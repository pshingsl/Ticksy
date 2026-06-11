package com.Ticksy.backend.domain.user.Service;

import com.Ticksy.backend.domain.user.DTO.*;
import com.Ticksy.backend.domain.user.Entity.UserEntity;
import com.Ticksy.backend.domain.user.Repository.UserRepository;
import com.Ticksy.backend.domain.user.enums.UserRole;
import com.Ticksy.backend.global.auth.jwt.JwtProvider;
import com.Ticksy.backend.global.exception.CustomException;
import com.Ticksy.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final EmailService emailService;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String REFRESH_TOKEN_PREFIX = "refresh:token:";
    private static final long REFRESH_TTL_DAYS = 7;

    // 이메일 중복 확인
    public EmailCheckResponse checkEmail(String email) {
        boolean isDuplicate = userRepository.existsByEmail(email);
        return EmailCheckResponse.of(isDuplicate);
    }

    // 이메일 인증번호 발송
    public void sendVerificationCode(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new CustomException(ErrorCode.ALREADY_REGISTERED_EMAIL);
        }
        emailService.sendVerificationCode(email);
    }

    // 이메일 인증번호 확인
    public EmailVerifyResponse verifyEmail(EmailVerifyRequest request) {
        emailService.verifyCode(request.getEmail(), request.getCode());
        return EmailVerifyResponse.of(true);
    }

    // 회원가입
    @Transactional
    public SignupResponse signup(SignupRequest request) {
        // 이메일 중복 확인
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        // 이메일 인증 확인
        emailService.checkVerified(request.getEmail());

        // 비밀번호 암호화 후 저장
        UserEntity user = UserEntity.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .role(UserRole.USER)
                .isDeleted(false)
                .build();

        userRepository.save(user);

        // 인증 완료 상태 삭제
        emailService.deleteVerified(request.getEmail());

        log.info("회원가입 완료: {}", user.getEmail());

        return SignupResponse.from(user);
    }

    // 로그인
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        // 회원 조회
        UserEntity user = userRepository
                .findByEmailAndIsDeletedFalse(request.getEmail())
                .orElseThrow(() ->
                        new CustomException(ErrorCode.INVALID_CREDENTIALS));

        // 탈퇴 회원 확인
        if (user.isDeleted()) {
            throw new CustomException(ErrorCode.DELETED_USER);
        }

        // 비밀번호 확인
        if (!passwordEncoder.matches(
                request.getPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 토큰 발급
        String accessToken = jwtProvider.createAccessToken(
                user.getUserId(),
                user.getRole().name()
        );
        String refreshToken = jwtProvider.createRefreshToken(
                user.getUserId()
        );

        // Refresh Token Redis 저장
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + user.getUserId(),
                refreshToken,
                REFRESH_TTL_DAYS,
                TimeUnit.DAYS
        );

        return LoginResponse.of(accessToken);
    }

    // Access Token 재발급
    public ReissueResponse reissue(String refreshToken) {
        // Refresh Token 검증
        jwtProvider.validateRefreshToken(refreshToken);

        Long userId = jwtProvider.getUserId(refreshToken);

        // Redis에서 Refresh Token 확인
        String savedToken = redisTemplate.opsForValue()
                .get(REFRESH_TOKEN_PREFIX + userId);

        if (savedToken == null || !savedToken.equals(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 회원 조회
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        // 새 Access Token 발급
        String newAccessToken = jwtProvider.createAccessToken(
                user.getUserId(),
                user.getRole().name()
        );

        return ReissueResponse.of(newAccessToken);
    }

    // 로그아웃
    public void logout(Long userId) {
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);
        log.info("로그아웃 완료: userId={}", userId);
    }

    // 비밀번호 변경 (MP-02)
    @Transactional
    public void changePassword(Long userId, PasswordChangeRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(
                request.getCurrentPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_CURRENT_PASSWORD);
        }

        // 새 비밀번호 저장
        user.updatePassword(
                passwordEncoder.encode(request.getNewPassword())
        );

        // 보안을 위해 Refresh Token 삭제 (자동 로그아웃)
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);
        log.info("비밀번호 변경 완료: userId={}", userId);
    }

    // 회원 탈퇴
    @Transactional
    public void withdraw(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        if(user.isDeleted()) {
            throw new CustomException(ErrorCode.DELETED_USER);
        }

        // 확정된 예매 내역 확인은 ReservationService에서 처리
        // 여기서는 소프트 삭제만
        user.delete();

        // Refresh Token 삭제
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);
        log.info("회원 탈퇴 완료: userId={}", userId);
    }
}