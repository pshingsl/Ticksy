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
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String EMAIL_VERIFY_PREFIX   = "email:verify:";
    private static final String EMAIL_VERIFIED_PREFIX = "email:verified:";
    private static final long VERIFY_TTL_MINUTES   = 5;
    private static final long VERIFIED_TTL_MINUTES = 30;

    // 인증번호 발송
    public void sendVerificationCode(String email) {
        String code = generateCode();

        // Redis에 인증번호 저장 (TTL 5분)
        redisTemplate.opsForValue().set(
                EMAIL_VERIFY_PREFIX + email,
                code,
                VERIFY_TTL_MINUTES,
                TimeUnit.MINUTES
        );

        // 이메일 발송
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[Ticksy] 이메일 인증번호");
        message.setText("인증번호: " + code
                + "\n\n인증번호는 5분간 유효합니다.");

        mailSender.send(message);
        log.info("인증번호 발송 완료: {}", email);
    }

    // 인증번호 확인
    public void verifyCode(String email, String code) {
        String savedCode = redisTemplate.opsForValue()
                .get(EMAIL_VERIFY_PREFIX + email);

        if (savedCode == null) {
            throw new CustomException(ErrorCode.EXPIRED_VERIFICATION_CODE);
        }

        if (!savedCode.equals(code)) {
            throw new CustomException(ErrorCode.INVALID_VERIFICATION_CODE);
        }

        // 인증번호 삭제
        redisTemplate.delete(EMAIL_VERIFY_PREFIX + email);

        // 인증 완료 상태 저장 (TTL 30분)
        redisTemplate.opsForValue().set(
                EMAIL_VERIFIED_PREFIX + email,
                "true",
                VERIFIED_TTL_MINUTES,
                TimeUnit.MINUTES
        );

        log.info("이메일 인증 완료: {}", email);
    }

    // 인증 완료 여부 확인
    public void checkVerified(String email) {
        String verified = redisTemplate.opsForValue()
                .get(EMAIL_VERIFIED_PREFIX + email);

        if (verified == null) {
            throw new CustomException(ErrorCode.INVALID_VERIFICATION);
        }
    }

    // 인증 완료 상태 삭제 (회원가입 성공 후)
    public void deleteVerified(String email) {
        redisTemplate.delete(EMAIL_VERIFIED_PREFIX + email);
    }

    // 6자리 랜덤 인증번호 생성
    private String generateCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }
}