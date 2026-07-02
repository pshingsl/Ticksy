package com.Ticksy.backend.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class TossPaymentConfig {

    @Value("${toss.secret-key}")
    private String secretKey;

    @Value("${toss.confirm-url}")
    private String confirmUrl;

    @Value("${toss.cancel-url}")
    private String cancelUrl;

    public String getSecretKey() {
        return secretKey;
    }

    public String getConfirmUrl() {
        return confirmUrl;
    }

    public String getCancelUrl() {
        return cancelUrl;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
