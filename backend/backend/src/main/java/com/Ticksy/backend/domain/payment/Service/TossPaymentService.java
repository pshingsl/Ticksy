package com.Ticksy.backend.domain.payment.Service;

import com.Ticksy.backend.global.config.TossPaymentConfig;
import com.Ticksy.backend.global.exception.CustomException;
import com.Ticksy.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TossPaymentService {

    private final RestTemplate restTemplate;
    private final TossPaymentConfig tossPaymentConfig;

    // Toss 결제 승인 API 호출
    public Map<String, Object> confirmPayment(String paymentKey, String orderId, Integer amount) {
        HttpHeaders headers = createHeaders();
        Map<String, Object> body = new HashMap<>();
        body.put("paymentKey", paymentKey);
        body.put("orderId", orderId);
        body.put("amount", amount);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    tossPaymentConfig.getConfirmUrl(),
                    request,
                    Map.class
            );

            log.info("Toss 결제 승인 성공: orderId={}", orderId);
            return response.getBody();

        } catch (HttpClientErrorException e) {
            log.error("Toss 결제 승인 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.PAYMENT_CONFIRM_FAILED);
        }
    }

    // Toss 환불 API 호출
    public void cancelPayments(String paymentKey, String cancelReason, Integer cancelAmount) {
        HttpHeaders headers = createHeaders();
        Map<String, Object> body = new HashMap<>();
        body.put("cancelReason", cancelReason);

        if (cancelAmount != null) {
            body.put("cancelAmount", cancelAmount);
        }

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        String url = tossPaymentConfig.getCancelUrl()
                .replace("{paymentKey}", paymentKey);


        try {
            restTemplate.postForEntity(url, request, Map.class);
            log.info("Toss 환불 처리 성공: paymentKey={}", paymentKey);

        } catch (HttpClientErrorException e) {
            log.error("Toss 환불 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.PAYMENT_CANCEL_FAILED);
        }
    }

    // Basic Auth 헤더 생성 (Toss 인증 방식)
    private HttpHeaders createHeaders() {
        String credentials = tossPaymentConfig.getSecretKey() + ":";
        String encode = Base64.getEncoder().encodeToString(
                credentials.getBytes(StandardCharsets.UTF_8)
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic " + encode);
        return headers;
    }
}
