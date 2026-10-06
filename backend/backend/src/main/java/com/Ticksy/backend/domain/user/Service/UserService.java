package com.Ticksy.backend.domain.user.Service;

import com.Ticksy.backend.domain.reservation.Repository.ReservationRepository;
import com.Ticksy.backend.domain.reservation.enums.ReservationStatus;
import com.Ticksy.backend.domain.user.DTO.Request.EmailVerifyRequest;
import com.Ticksy.backend.domain.user.DTO.Request.LoginRequest;
import com.Ticksy.backend.domain.user.DTO.Request.PasswordChangeRequest;
import com.Ticksy.backend.domain.user.DTO.Request.SignupRequest;
import com.Ticksy.backend.domain.user.DTO.Response.*;
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
    private final ReservationRepository reservationRepository;

    private static final String REFRESH_TOKEN_PREFIX = "refresh:token:";
    private static final long REFRESH_TTL_DAYS = 7;

    // 이메일 중복 확인
    public EmailCheckResponse checkEmail(String email) {

        boolean isDuplicate = userRepository.existsByEmail(email);

        return EmailCheckResponse.of(isDuplicate);
    }

    // 이메일 인증번호 확인
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

    // 회원가입 최종 처리
    @Transactional
    public SignupResponse signup(SignupRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        emailService.checkVerified(request.getEmail());

        UserEntity user = UserEntity.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .role(UserRole.USER)
                .isDeleted(false)
                .build();

        userRepository.save(user);

        emailService.deleteVerified(request.getEmail());

        log.info("회원가입 완료: {}", user.getEmail());

        return SignupResponse.from(user);
    }

    // 로그인 처리
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        UserEntity user = userRepository
                .findByEmailAndIsDeletedFalse(request.getEmail())
                .orElseThrow(() ->
                        new CustomException(ErrorCode.INVALID_CREDENTIALS));

        if (user.isDeleted()) {
            throw new CustomException(ErrorCode.DELETED_USER);
        }

        if (!passwordEncoder.matches(
                request.getPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        /*
         * - Access Token: 30분~1시간짜리 이용
         * - Refresh Token: 7일짜리 로그인 재발급용
         */
        String accessToken = jwtProvider.createAccessToken(
                user.getUserId(),
                user.getRole().name()
        );
        String refreshToken = jwtProvider.createRefreshToken(
                user.getUserId()
        );


        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + user.getUserId(),
                refreshToken,
                REFRESH_TTL_DAYS,
                TimeUnit.DAYS
        );

        return LoginResponse.of(accessToken);
    }

    /*
     * Access Token 재발급 (
     * 역할: 30분짜리 자유이용권이 만료되었을 때, 7일짜리 리프레시 토큰을
     */
    public ReissueResponse reissue(String refreshToken) {

        jwtProvider.validateRefreshToken(refreshToken);

        Long userId = jwtProvider.getUserId(refreshToken);

        String savedToken = redisTemplate.opsForValue()
                .get(REFRESH_TOKEN_PREFIX + userId);

        if (savedToken == null || !savedToken.equals(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

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

    // 비밀번호 변경
    @Transactional
    public void changePassword(Long userId, PasswordChangeRequest request) {

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        if (!passwordEncoder.matches(
                request.getCurrentPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_CURRENT_PASSWORD);
        }

        user.updatePassword(
                passwordEncoder.encode(request.getNewPassword())
        );

        redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);
        log.info("비밀번호 변경 완료: userId={}", userId);
    }

    // 회원 탈퇴
    @Transactional
    public void withdraw(Long userId) {

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        if (user.isDeleted()) {
            throw new CustomException(ErrorCode.DELETED_USER);
        }

        boolean hasActiveReservation = reservationRepository.existsByUser_UserIdAndStatus(
                userId, ReservationStatus.CONFIRMED);

        if (hasActiveReservation) {
            throw new CustomException(ErrorCode.HAS_ACTIVE_RESERVATION);
        }

        user.delete();

        redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);
        log.info("회원 탈퇴 완료: userId={}", userId);
    }
}