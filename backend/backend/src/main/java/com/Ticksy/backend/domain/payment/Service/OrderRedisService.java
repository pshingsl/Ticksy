package com.Ticksy.backend.domain.payment.Service;

import com.Ticksy.backend.global.exception.CustomException;
import com.Ticksy.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderRedisService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String ORDER_PREFIX = "order:info:";
    private static final long ORDER_TTL_MINUTES = 10;

    // OrderId -> scheduleId:seatId1,seatId2" 형태로 저장
    public void saveOrderInfo(
            String orderId, Long scheduleId, List<Long> seatIds
    ) {
        String value = scheduleId + ":" +
                seatIds.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(","));

        redisTemplate.opsForValue().set(
                ORDER_PREFIX + orderId,
                value,
                ORDER_TTL_MINUTES,
                TimeUnit.MINUTES
        );
    }

    // orderId로 정보 조회
    public String getOrderInfo(String orderId) {
        String value = redisTemplate.opsForValue()
                .get(ORDER_PREFIX + orderId);

        if(value == null) {
            throw new CustomException(ErrorCode.SEAT_HOLD_EXPIRED);
        }
        return value;
    }

    public void deleteOrderInfo(String orderId) {
        redisTemplate.delete(ORDER_PREFIX + orderId);
    }
}
