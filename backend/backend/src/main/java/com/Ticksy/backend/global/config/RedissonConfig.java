package com.Ticksy.backend.global.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private String redisPort;

    @Bean
    public RedissonClient redissonClient() {

        // Redisson 서버 연결 세팅 정보를 담을 빈 객체 생성
        Config config = new Config();

        // 고가용성 구조가 아닌 , 단 한 대의 독립된 Redis 서버와 연결
        // 고가용성: 서버가 24시간 365일 동안 절대 죽지않고 정삭적으로서비스를 제공하는 것
        // setAddress() Redis 서버의 네트워크주소를 문자 형태로 전달
        config.useSingleServer()
                .setAddress("redis://" + redisHost + ":" + redisPort);
        return Redisson.create(config);
    }
}
