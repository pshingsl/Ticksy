package com.Ticksy.backend.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    /*
    * RedisTemplate은 자바 코드에서 Redis(인메모리 데이터베이스)에 데이터를 저장, 조회, 삭제할 수 있도록 돕는 스프링 전용 레디스 조작 도구
    * <String, String>의 의미는 키-값 형태로 저장하려고 약속
    * 주로 로그인 리프레시 토큰이나 이메일 인증번호를 임시 보관할 때 이 구조를 사용
    * connectionFactory - yml에 적어둔 주소(lcoalhost:6379)를 보고 실제 레디스 서버와 통신선을 열어주는 공장 객체
    * */
    @Bean
    public RedisTemplate<String, String> redisTemplate(
            RedisConnectionFactory connectionFactory) {

        // 1. 레디스를 조작할 템플릿 빈 객체 생성
        RedisTemplate<String, String> template = new RedisTemplate<>();

        // 2. 이 리모컨이 어떤 레디스 서버와 통신해야 하는지, 방금 주입받은 연결 공장을 셋팅
        template.setConnectionFactory(connectionFactory);

        /*
        * 직렬화
        * 자바의 변수나 객체 데이터는 메모리 주소 기반이라 레디스가 그대로 이해할 수 없다.
        * 그래서 자바 데이터를 레디스가 이해할 수 있는 평문 문자열 바이트로 "번역" 해주는 작업이 필요
        * 만약 이 설정을 안 하면 레디스에 데이터가 들어갈 때 \xac\xed\x00\x05t\x00 같은 정체불명의 외계어 문자가 섞여 들어옴
        */

        // 3. 일반적인 키-값 데이터를 저장할 때의 번역 규칙을 지정
        template.setKeySerializer(new StringRedisSerializer()); // 키값를 평문 문자열로 깨끗하게 저장
        template.setValueSerializer(new StringRedisSerializer()); // 실제 데이터를 평문 문자열로 깨끗하게 저장

        // 4. 레디스의 Hash라는 특수 구조(자바의 Map안에 Map이 들어간 형태)를 사용할 때 번역 규칙이다.
        template.setHashKeySerializer(new StringRedisSerializer()); // Hash 구조 안의 Key값 번역 규칙 설정
        template.setHashValueSerializer(new StringRedisSerializer()); // Hash 구조 안의 Value값 번역 규칙 설정

        // 5. 모든 직렬화/역직렬화 번역 규칙 셋팅이 완료된 완벽한 리모컨을 스프링 컨테이너에 반환(등록)합니다.
        return template;
    }
}
