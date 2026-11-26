package com.Ticksy.backend.domain.seat.Service;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class SeatHoldServiceTest {

    private static final Long SCHEDULE_ID = 1L;
    private static final Long SEAT_ID = 10L;
    private static final String HOLD_KEY = "seat:hold:" + SCHEDULE_ID + ":" + SEAT_ID;

    private static GenericContainer<?> redis;
    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redisTemplate;
    private static RedissonClient redissonClient;
    private static SeatHoldService seatHoldService;

    /*
     * 테스트 클래스 시작 시 한 번 실행
     * 1. Docker로 Redis 컨테이너를 띄운다.
     * 2. 그 Redis에 연결하는 RedisTemplate과 RedissonClient를 만든다.
     * 3. 두 객체를 넣어서 진짜 SeatHoldService를 만든다.
     */
    @BeforeAll
    static void setUp() {
        // 1. Redis 컨테이너 실행 (처음 한 번은 이미지를 내려받느라 시간이 걸림)
        redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379);
        redis.start();

        // 컨테이너의 6379 포트는 PC의 임의 포트로 연결되므로, 실제 주소를 꺼내서 사용
        String host = redis.getHost();
        int port = redis.getMappedPort(6379);

        // 2-1. RedisTemplate (선점 키 저장, 조회용)
        connectionFactory = new LettuceConnectionFactory(host, port);
        connectionFactory.afterPropertiesSet();
        connectionFactory.start();
        redisTemplate = new StringRedisTemplate(connectionFactory);

        // 2-2. RedissonClient (분산 락용)
        Config config = new Config();
        config.useSingleServer().setAddress("redis://" + host + ":" + port);
        redissonClient = Redisson.create(config);

        // 3. 실제 SeatHoldService 생성
        seatHoldService = new SeatHoldService(redissonClient, redisTemplate);
    }

    // 테스트 클래스가 끝나면 연결을 닫고 컨테이너를 정리
    @AfterAll
    static void tearDown() {
        redissonClient.shutdown();
        connectionFactory.destroy();
        redis.stop();
    }

    // 테스트끼리 영향을 주지 않도록 매 테스트가 끝날 때마다 Redis를 비움
    @AfterEach
    void clearRedis() {
        Set<String> keys = redisTemplate.keys("*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    /*
     * [테스트 1] 같은 좌석에 100명이 동시에 선점 요청
     *
     * 검증 내용
     * - 100건 중 정확히 1건만 선점에 성공하고 99건은 실패한다.
     * - Redis에는 성공한 사용자 1명의 선점 정보만 남는다.
     *
     * 왜 필요한가
     * - "빈 좌석 확인 → 선점 저장" 사이에 다른 요청이 끼어들면 한 좌석이 여러 명에게 선점된다.
     * - Redisson 분산 락이 이 구간을 한 번에 한 요청만 지나가도록 막는지 실제 Redis로 검증한다.
     * - 포트폴리오의 k6 테스트(서버 2대, 100건 → 1건 성공) 결과를 코드로 재현한 테스트다.
     */
    @Test
    @DisplayName("같은 좌석에 100명이 동시에 선점을 요청하면 정확히 1명만 성공한다")
    void tryHoldSeat_concurrentRequests_onlyOneSucceeds() throws InterruptedException {
        // given
        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        CountDownLatch startLatch = new CountDownLatch(1);           // "준비, 땅!" 신호
        CountDownLatch doneLatch = new CountDownLatch(threadCount);  // 100개가 다 끝났는지 확인

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        // 100명의 사용자(스레드)가 출발선에서 대기
        for (int i = 0; i < threadCount; i++) {
            long userId = i + 1;
            executor.submit(() -> {
                try {
                    startLatch.await();   // 신호가 떨어질 때까지 대기

                    boolean success = seatHoldService.tryHoldSeat(SCHEDULE_ID, SEAT_ID, userId);
                    if (success) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();   // 이 스레드는 끝났다고 표시
                }
            });
        }

        // when: 100개 스레드 동시 출발
        startLatch.countDown();

        // 100개가 모두 끝날 때까지 최대 30초 대기
        boolean finished = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // then
        assertThat(finished).isTrue();
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(99);

        // then: Redis에는 성공한 사용자 1명의 선점 정보만 남아 있어야 한다
        String holderUserId = redisTemplate.opsForValue().get(HOLD_KEY);
        assertThat(holderUserId).isNotNull();
    }

    /*
     * [테스트 2] 선점 키 TTL 설정
     *
     * 검증 내용
     * - 선점에 성공하면 선점 키에 5분(300초) 이하의 만료 시간이 설정된다.
     *
     * 왜 필요한가
     * - TTL이 없으면 결제하지 않고 떠난 사용자의 선점이 영원히 남아 좌석을 팔 수 없게 된다.
     */
    @Test
    @DisplayName("선점에 성공하면 선점 키에 5분 이하의 TTL이 설정된다")
    void tryHoldSeat_setsTtl() {
        // when
        boolean success = seatHoldService.tryHoldSeat(SCHEDULE_ID, SEAT_ID, 1L);

        // then
        assertThat(success).isTrue();

        Long ttlSeconds = redisTemplate.getExpire(HOLD_KEY, TimeUnit.SECONDS);
        assertThat(ttlSeconds).isGreaterThan(0L).isLessThanOrEqualTo(300L);
    }

    /*
     * [테스트 3] 이미 선점된 좌석에 다른 사용자가 요청
     *
     * 검증 내용
     * - 두 번째 사용자는 선점에 실패한다.
     * - 선점 주인은 첫 번째 사용자로 그대로 유지된다 (덮어쓰지 않는다).
     */
    @Test
    @DisplayName("이미 선점된 좌석은 다른 사용자가 선점할 수 없고 기존 선점이 유지된다")
    void tryHoldSeat_alreadyHeld() {
        // given: 사용자 1이 먼저 선점
        seatHoldService.tryHoldSeat(SCHEDULE_ID, SEAT_ID, 1L);

        // when: 사용자 2가 같은 좌석 선점 시도
        boolean secondResult = seatHoldService.tryHoldSeat(SCHEDULE_ID, SEAT_ID, 2L);

        // then
        assertThat(secondResult).isFalse();
        assertThat(seatHoldService.isHeldByUser(SCHEDULE_ID, SEAT_ID, 1L)).isTrue();
        assertThat(seatHoldService.isHeldByUser(SCHEDULE_ID, SEAT_ID, 2L)).isFalse();
    }

    /*
     * [테스트 4] 여러 좌석 선점 상태 일괄 조회 (MGET)
     *
     * 검증 내용
     * - 선점된 좌석은 true, 선점되지 않은 좌석은 false로 정확히 구분된다.
     * - 빈 목록을 조회하면 빈 결과를 반환한다.
     *
     * 왜 필요한가
     * - 좌석 배치도 조회 성능을 개선하며 좌석마다 hasKey를 호출하던 방식을 MGET 한 번으로 바꿨다.
     * - 성능만 좋아지고 결과가 틀리면 안 되므로, 기존 방식과 같은 결과가 나오는지 확인한다.
     */
    @Test
    @DisplayName("MGET으로 여러 좌석의 선점 상태를 한 번에 정확히 조회한다")
    void getHoldingStatus_withMget() {
        // given: 10번, 30번만 선점 (20번은 빈 좌석)
        seatHoldService.tryHoldSeat(SCHEDULE_ID, 10L, 1L);
        seatHoldService.tryHoldSeat(SCHEDULE_ID, 30L, 2L);

        // when
        Map<Long, Boolean> status =
                seatHoldService.getHoldingStatus(SCHEDULE_ID, List.of(10L, 20L, 30L));

        // then
        assertThat(status)
                .containsEntry(10L, true)
                .containsEntry(20L, false)
                .containsEntry(30L, true);

        // then: 빈 목록이면 Redis를 조회하지 않고 빈 결과를 반환
        assertThat(seatHoldService.getHoldingStatus(SCHEDULE_ID, List.of())).isEmpty();
    }
}