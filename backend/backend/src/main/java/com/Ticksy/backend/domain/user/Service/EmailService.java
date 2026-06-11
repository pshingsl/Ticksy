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

    /*
     * JavaMailSender Spring Boot가 제공하는 이메일 전송 객체
     * 역할: 이메일 SMTP 서버 연결 이메일 실제 발송
     *
     * RedisTemplate
     * Redis 데이터 저장, 조회 전용 객체
     */
    private final JavaMailSender mailSender;
    private final RedisTemplate<String, String> redisTemplate; // 메모리 기반 초고속 저장소와 대화하는 리모컨

    private static final String EMAIL_VERIFY_PREFIX   = "email:verify:"; // 인증번호 보관용 방
    private static final String EMAIL_VERIFIED_PREFIX = "email:verified:"; // 인증 통과 영수증 보관용 방
    private static final long VERIFY_TTL_MINUTES   = 5; // 인증번호는 5분 동안 생존
    private static final long VERIFIED_TTL_MINUTES = 30; // 인증 완료 영수증은 30분

    /*
     * 인증번호 발송 기능
     */
    public void sendVerificationCode(String email) {
        // 6자리의 임의 숫자 번호를 생성
        String code = generateCode();

        // Redis에 "인증번호 보관방"에 [이메일 주소 : 인증번호] 저장 (TTL 5분)
        redisTemplate.opsForValue().set(
                EMAIL_VERIFY_PREFIX + email, // Key(열쇠 이름)
                code,                             // 메일 제목
                VERIFY_TTL_MINUTES,               // 지속 시간 숫자(5)
                TimeUnit.MINUTES                  // 시간 단위(분)
        );

        // 유저에게 보낼 이메일 형식을 작성
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);                           // 받는 사람 이메일 주소
        message.setSubject("[Ticksy] 이메일 인증번호");   // 메일 제목
        message.setText("인증번호: " + code
                + "\n\n인증번호는 5분간 유효합니다.");      // 메일 본문 내용

        // 우체부 기계를 통해 실제로 이메일을 발송
        mailSender.send(message);

        // 서버 콘솔창에 기록을 남김
        log.info("인증번호 발송 완료: {}", email);
    }

    // 유저가 입력한 인증번호 확인 기능
    public void verifyCode(String email, String code) {
        // 레디스 금고방에서 이 이메일 주소로 저장된 인증번호 꺼내오기
        String savedCode = redisTemplate.opsForValue()
                .get(EMAIL_VERIFY_PREFIX + email);

        // 만약 꺼냈는데 아무것도 없다면? 5분이 지나서 레디스 자동으로 지워버린 것이다.
        if (savedCode == null) {
            throw new CustomException(ErrorCode.EXPIRED_VERIFICATION_CODE);
        }

        // 유저가 적어서 보낸 번호와 레디스 금고에 있던 진짜 번호가 맞는지 대조
        if (!savedCode.equals(code)) {
            throw new CustomException(ErrorCode.INVALID_VERIFICATION_CODE);
        }

        // 번호가 맞았으므로, 일회용이었던 5분째 인증번호는 금고에서 즉시 깔끔하게 삭제
        redisTemplate.delete(EMAIL_VERIFY_PREFIX + email);

        // 인증에 성공한 유저라고 알림 30분짜리 임시 통행증 새로 발행해서 레디스 저장
        redisTemplate.opsForValue().set(
                EMAIL_VERIFIED_PREFIX + email, // 새로운 영수증 방 이름
                "true",                             // 내용물은 그냥 성공했다는 의미로 "true" 문자열 저장
                VERIFIED_TTL_MINUTES,               // 30
                TimeUnit.MINUTES                    // 분
        );

        log.info("이메일 인증 완료: {}", email);
    }

    // 가입 직전, 진짜로 인증 완료 영수증을 가지고 있는지 최종 검사 가입
    public void checkVerified(String email) {
        // 레디스 영수증 방에서 이 이메일로 발행된 영수증이 있는지 검사
        String verified = redisTemplate.opsForValue()
                .get(EMAIL_VERIFIED_PREFIX + email);

        // 영수증 없거나 내용물이 true가 아니면 비정상적인 우회 접근
        if (verified == null) {
            throw new CustomException(ErrorCode.INVALID_VERIFICATION); // 인증되지 않은 이메일 에러를 터트려 가입을 막는다.
        }
        // 만약 영수증이 살아 있다면 에러가 터지지 않고 조용히 넘어가서 회원가입 코드가 계속 실행
    }

    // 회원가입 완벽하게 최종 성공했을 때, 레디스에 남아 있는 영수증을 청소하는 기능
    public void deleteVerified(String email) {
        // 회원가입이 완전히 끝났으므로 더 이상 필요 없어진 30분짜리 임시 영수증 데이터를 레디스에서 영구 삭제
        redisTemplate.delete(EMAIL_VERIFIED_PREFIX + email);
    }

    // 6자리 랜덤 인증번호 생성
    private String generateCode() {
        Random random = new Random();
        /*
         * 0~999999 사이의 무작위 숫자를 만든 뒤, 빈자리는 0으로 채워서 무조건 6글자로 포맷팅
         */
        return String.format("%06d", random.nextInt(1000000));
    }
}