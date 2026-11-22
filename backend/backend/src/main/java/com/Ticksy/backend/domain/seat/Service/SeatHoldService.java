package com.Ticksy.backend.domain.seat.Service;


import com.Ticksy.backend.global.exception.CustomException;
import com.Ticksy.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatHoldService {

    private final RedissonClient redissonClient;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String SEAT_HOLD_PREFIX = "seat:hold:";
    private static final String LOCK_PREFIX = "lock:hold:";
    private static final long HOLD_TTL_MINUS = 5;
    private static final long LOCK_WAIT_SECONDS = 3;
    private static final long LOCK_LEASE_SECONDS = 3;

    // 좌석 선점
    public boolean tryHoldSeat(Long scheduleId, Long seatId, Long userId) {
        String lockKey = LOCK_PREFIX + scheduleId + ":" + seatId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 락 획득 시도(대기 3초, 점유 3초)
            boolean acquired = lock.tryLock(
                    LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS, TimeUnit.SECONDS
            );

            if (!acquired) {
                log.warn("락 획득 실패: schedule={}, seat={}", scheduleId, seatId);
                return false;
            }

            //  락 획득 성공 - 좌석 이미 선점 되어 있는지 확인
            String holdKey = SEAT_HOLD_PREFIX + scheduleId + ":" + seatId;
            Boolean isAlreadyHeld = redisTemplate.hasKey(holdKey);

            if (Boolean.TRUE.equals(isAlreadyHeld)) {
                return false; // 이미 선점 되면 리턴
            }

            // 선점 처리(TTL 5분)
            redisTemplate.opsForValue().set(
                    holdKey,
                    String.valueOf(userId),
                    HOLD_TTL_MINUS,
                    TimeUnit.MINUTES
            );

            return true;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        } finally {
            // 락 해제(현재 스레드가 보유한 경우에만)
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    // 좌석이 선점 중인지 확인
    public boolean isHolding(Long scheduleId, Long seatId) {
        String holdKey = SEAT_HOLD_PREFIX + scheduleId + ":" + seatId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(holdKey));
    }

    // 여러 좌석의 선점 여부를 한 번에 확인 (MGET: Redis 왕복 1회)
    // 좌석마다 hasKey를 호출하면 좌석 수만큼 왕복이 생겨서, 좌석 배치도 조회에서는 이 메서드를 사용
    public Map<Long, Boolean> getHoldingStatus(Long scheduleId, List<Long> seatIds) {
        Map<Long, Boolean> result = new HashMap<>();
        if (seatIds.isEmpty()) {
            return result;
        }

        List<String> keys = seatIds.stream()
                .map(seatId -> SEAT_HOLD_PREFIX + scheduleId + ":" + seatId)
                .toList();

        // 키 순서대로 값이 돌아오고, 없는 키는 null
        List<String> values = redisTemplate.opsForValue().multiGet(keys);

        for (int i = 0; i < seatIds.size(); i++) {
            boolean holding = values != null && values.get(i) != null;
            result.put(seatIds.get(i), holding);
        }
        return result;
    }

    // 본인이 선점한 좌석인지 확인
    public boolean isHeldByUser(Long scheduleId, Long seatId, Long userId) {
        String holdKey = SEAT_HOLD_PREFIX + scheduleId + ":" + seatId;
        String heldUserId = redisTemplate.opsForValue().get(holdKey);
        return heldUserId != null && heldUserId.equals(String.valueOf(userId));
    }

    // 좌석 선점 취소
    public void cancelHold(Long scheduleId, Long seatId) {
        String holdkey = SEAT_HOLD_PREFIX + scheduleId + ":" + seatId;
        redisTemplate.delete(holdkey);
    }

    // 좌석 선점 만료 시각 조회
    public LocalDateTime getExpiredAt(Long scheduleId, Long seatId) {
        String holdkey = SEAT_HOLD_PREFIX + scheduleId + ":" + seatId;
        Long ttlSeconds = redisTemplate.getExpire(holdkey, TimeUnit.SECONDS);
        if(ttlSeconds == null || ttlSeconds < 0) {
            return null;
        }
        return LocalDateTime.now().plusSeconds(ttlSeconds);
    }
}
