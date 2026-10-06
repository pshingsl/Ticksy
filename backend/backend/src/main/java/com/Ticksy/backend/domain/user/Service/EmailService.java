package com.Ticksy.backend.domain.user.Service;

import com.Ticksy.backend.global.exception.CustomException;
import com.Ticksy.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service // 스프링에서 이 클래스는 비즈니스 로직을 담당하는 서비스"
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String EMAIL_VERIFY_PREFIX   = "email:verify:";
    private static final String EMAIL_VERIFIED_PREFIX = "email:verified:";
    private static final long VERIFY_TTL_MINUTES   = 5;
    private static final long VERIFIED_TTL_MINUTES = 30;

    // 인증번호 발송 기능
    public void sendVerificationCode(String email) {
        // 6자리의 임의 숫자 번호를 생성
        String code = generateCode();

        redisTemplate.opsForValue().set(
                EMAIL_VERIFY_PREFIX + email,
                code,
                VERIFY_TTL_MINUTES,
                TimeUnit.MINUTES
        );

        // 유저에게 보낼 이메일 형식을 작성
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[Ticksy] 이메일 인증번호");
        message.setText("인증번호: " + code
                + "\n\n인증번호는 5분간 유효합니다.");

        // 이메일을 발송
        mailSender.send(message);

        log.info("인증번호 발송 완료: {}", email);
    }

    // 유저가 입력한 인증번호 확인 기능
    public void verifyCode(String email, String code) {

        String savedCode = redisTemplate.opsForValue()
                .get(EMAIL_VERIFY_PREFIX + email);

        if (savedCode == null) {
            throw new CustomException(ErrorCode.EXPIRED_VERIFICATION_CODE);
        }

        if (!savedCode.equals(code)) {
            throw new CustomException(ErrorCode.INVALID_VERIFICATION_CODE);
        }

        redisTemplate.delete(EMAIL_VERIFY_PREFIX + email);

        redisTemplate.opsForValue().set(
                EMAIL_VERIFIED_PREFIX + email,
                "true",
                VERIFIED_TTL_MINUTES,
                TimeUnit.MINUTES
        );

        log.info("이메일 인증 완료: {}", email);
    }

    public void checkVerified(String email) {

        String verified = redisTemplate.opsForValue()
                .get(EMAIL_VERIFIED_PREFIX + email);

        if (verified == null) {
            throw new CustomException(ErrorCode.INVALID_VERIFICATION);
        }
    }

    // 회원가입 완벽하게 최종 성공
    public void deleteVerified(String email) {

        redisTemplate.delete(EMAIL_VERIFIED_PREFIX + email);
    }

    // 6자리 랜덤 인증번호 생성
    private String generateCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }
}