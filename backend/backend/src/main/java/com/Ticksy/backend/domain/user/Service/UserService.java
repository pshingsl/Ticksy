package com.Ticksy.backend.domain.user.Service;

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

    private final UserRepository userRepository;                // MySQL 데이터베이스 장부와 직접 통신하는 기계
    private final PasswordEncoder passwordEncoder;              // 비밀번호 암호화
    private final JwtProvider jwtProvider;                      // 유저 확인용 토큰(Access/Refresh Token)을 찍어내는 공장
    private final EmailService emailService;                    // 이메일 인증 영수증 관리
    private final RedisTemplate<String, String> redisTemplate;  // 레디스 초고속 금고 리모컨

    private static final String REFRESH_TOKEN_PREFIX = "refresh:token:"; // 레디스에 보관할 리프레시 토큰방 이름
    private static final long REFRESH_TTL_DAYS = 7;                      // 리프레시 토큰은 7일 동안만 유지

    /*
     * 이메일 중복 확인
     * 입력한 이메일을 쓰는 사람이 이미 장부에 있는지 체크
     */
    public EmailCheckResponse checkEmail(String email) {
        // DB 장부에 해당 이메일이 이미 존재하는지 참/거짓으로 확인
        boolean isDuplicate = userRepository.existsByEmail(email);
        // 결과를 DTO 붕어빵 틀에 DTO 값으로 반환
        return EmailCheckResponse.of(isDuplicate);
    }

    /*
     * 이메일 인증번호 확인
     * 가입 전, 이미 가입된 메일인지 체크하고 통과하면 인증번호를 메일을 날린다.
     */
    public void sendVerificationCode(String email) {
        // 유저의 이메일 있는지 검사
        if (userRepository.existsByEmail(email)) {
            throw new CustomException(ErrorCode.ALREADY_REGISTERED_EMAIL);
        }
        // 유저의 이메일이 없다면 이메일 서비스에 이메일을 인증번호 발송
        emailService.sendVerificationCode(email);
    }

    /*
     * 이메일 인증번호 확인
     * 유저가 폰으로 받은 번호를 입력했을 때 진짜 맞는지 대조
     */
    public EmailVerifyResponse verifyEmail(EmailVerifyRequest request) {
        // 이메일 서비스에서 이메일주소와 유저가 입력한 번호를 넘겨서 맞는지 검증
        emailService.verifyCode(request.getEmail(), request.getCode());
        // 검증 도중 에러가 안 터지고 무사히 내려왔다면 성공 영수증 발급된 것이므로 true를 응답
        return EmailVerifyResponse.of(true);
    }

    /*
     * 회원가입 최종 처리
     * @Transactional: 전부 성공하든지, 하나라도 삐끗하면 가입 전으로 시간을 되돌려라는 안정 장치
     */
    @Transactional
    public SignupResponse signup(SignupRequest request) {
        // 이메일 중복체크를 우회하고 가입 요청을 날렸을지 모르니 한 번 더 이메일 중복을 체크
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        // 유저가 이메일 인증 영수증을 진짜 가지고 있는지 확인
        emailService.checkVerified(request.getEmail());

        // 빌더를 이용해서 새 회원 정보를 이쁘게 제조
        UserEntity user = UserEntity.builder()
                .email(request.getEmail())
                // 🔥 유저가 입력한 날것의 비밀번호(password)를 암호화 기계로 꽁꽁 숨겨서 세팅
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .role(UserRole.USER)
                .isDeleted(false) // 정상 회원(탈퇴 안 됨) 표기
                .build();

        // 회원가입 된 회원 정보를 MySQL DB에 저장
        userRepository.save(user);

        // 회원가입이 안전하게 성공했으므로, 이제 목적을 달성한 일회용 이메일 인증 영수증을 레디스에서 지워즌다.
        emailService.deleteVerified(request.getEmail());

        log.info("회원가입 완료: {}", user.getEmail());

        // 가입 완료된 유저 정보를 가지고 응답 객체를 만들어 프론트엔드에게 반환
        return SignupResponse.from(user);
    }

    /*
     * 로그인 처리
     * readOnly = true: 조회만 하는 메소드이므로 성능 최적화를 위해 읽기모드 변경
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        // 회원이 존재하는지 검사
        UserEntity user = userRepository
                .findByEmailAndIsDeletedFalse(request.getEmail())
                .orElseThrow(() ->
                        new CustomException(ErrorCode.INVALID_CREDENTIALS));

        // 탈퇴 회원 확인
        if (user.isDeleted()) {
            throw new CustomException(ErrorCode.DELETED_USER);
        }

        // 유저가 입력한 비밀번호와 DB에 저장된 암호화된 비밀번호가 '동일한 해시값'을 가졌는지 비교 기계로 매칭
        if (!passwordEncoder.matches(
                request.getPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS); // 비밀번호가 달라도 계정 정보 오류 에러
        }

        /*
         * 통과 했으므로 토큰 두 종류를 찍어낸다.
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

        // 로그인 유지 및 검증을 위해, 새로 만든 7일짜리 리프레쉬 토큰을 레디스 디비에 안전하게 백업 보관
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + user.getUserId(), // Key
                refreshToken,                                 // value
                REFRESH_TTL_DAYS,                             // 7
                TimeUnit.DAYS                                 // 일
        );

        // 프론트에게는 API 요청할 때마다 헤더에 매달고 올 자유이용권만 쥐어진다.
        return LoginResponse.of(accessToken);
    }

    /*
     * Access Token 재발급 (로그인 수명 연장 기계)
     * 역할: 30분짜리 자유이용권이 만료되었을 때, 7일짜리 리프레시 토큰을 내밀면 새 자유이용권을 끊어줍니다.
     */
    public ReissueResponse reissue(String refreshToken) {
        // 가져온 리프레시 토큰이 유효한 형태인지 변조 여부를 1차 검사
        jwtProvider.validateRefreshToken(refreshToken);

        // 리프레시 토큰의 내부 알맹이에서 유저의 고유 번호를 추출
        Long userId = jwtProvider.getUserId(refreshToken);

        // Redis에 이전에 보관한 리프레시 토큰을 꺼낸다.
        String savedToken = redisTemplate.opsForValue()
                .get(REFRESH_TOKEN_PREFIX + userId);

        // 토큰이 없거나, 유저가 들고 온 토큰이랑 금고 원본이 다르면 위조 토큰으로 간주하고 차단
        if (savedToken == null || !savedToken.equals(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 유저가 존재하는지 검사
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        // 30분 동안 새로 사용할 수 있는 신선한 '새 Access Token'을 발급
        String newAccessToken = jwtProvider.createAccessToken(
                user.getUserId(),
                user.getRole().name()
        );

        // 새로운 토큰 발급
        return ReissueResponse.of(newAccessToken);
    }

    // 로그아웃
    public void logout(Long userId) {
        // 레디스에 있는 유저의 리프레시 토큰 제거 -> 나중에 말료된 액세스 토큰을 들고 와서 재발급 요청해도 레디스이 원본 재발급 차단
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);
        log.info("로그아웃 완료: userId={}", userId);
    }

    // 비밀번호 변경 (MP-02)
    @Transactional
    public void changePassword(Long userId, PasswordChangeRequest request) {
        // 유저 존재하는지 검사
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        // 현재 비밀번호 디비에 있는 비밀번호가 같은지 검사
        if (!passwordEncoder.matches(
                request.getCurrentPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_CURRENT_PASSWORD);
        }

        // 검증이 성공적으로 끝낸다면 새로운 비밀번호로 업데이트
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
        // 유저가 존재하는지 검사
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        // 이미 탈퇴가 유저의 상택가 참이라면 이중 처리 탈퇴 막기
        if(user.isDeleted()) {
            throw new CustomException(ErrorCode.DELETED_USER);
        }

        // 확정된 예매 내역 확인은 ReservationService에서 처리 예정
        // 여기서는 소프트 삭제만
        user.delete();

        // 탈퇴 처리가 정상 접수되었으므로 이 유저가 쓰던 로그인 통행증(Refresh Token)을 레디스 금고에서 소각
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);
        log.info("회원 탈퇴 완료: userId={}", userId);
    }
}