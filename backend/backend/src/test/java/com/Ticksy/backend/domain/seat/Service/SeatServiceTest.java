package com.Ticksy.backend.domain.seat.Service;

import com.Ticksy.backend.domain.concert.Entity.EventScheduleEntity;
import com.Ticksy.backend.domain.concert.Repository.EventScheduleRepository;
import com.Ticksy.backend.domain.concert.Repository.SectionRepository;
import com.Ticksy.backend.domain.seat.DTO.Response.SeatHoldResponse;
import com.Ticksy.backend.domain.seat.Entity.SeatEntity;
import com.Ticksy.backend.domain.seat.Repository.SeatRepository;
import com.Ticksy.backend.domain.seat.enums.SeatStatus;
import com.Ticksy.backend.global.exception.CustomException;
import com.Ticksy.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
public class SeatServiceTest {

    // SeatService가 의존하는 객체들을 가짜(Mock)로 생성
    @Mock
    private EventScheduleRepository eventScheduleRepository;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private SeatHoldService seatHoldService;

    // 진짜 SeatService를 만들고, 위의 Mock 객체들을 생성자에 주입
    @InjectMocks
    private SeatService seatService;

    private static final Long SCHEDULE_ID = 1L;
    private static final Long USER_ID = 100L;

    /*
     * [테스트 1] 1인당 최대 좌석 수 초과
     *
     * 검증 내용
     * - 5석을 요청하면 EXCEED_MAX_SEAT_COUNT 예외가 발생한다.
     * - 좌석 수 검증은 가장 먼저 일어나므로 DB 조회까지 가지 않는다.
     *
     * 왜 필요한가
     * - 1인당 4석 제한은 티켓 사재기를 막는 정책이다.
     * - 불필요한 DB 조회 없이 입력값 단계에서 바로 막히는지도 함께 확인한다.
     */
    @Test
    @DisplayName("1인당 최대 4석을 넘겨 선점하면 EXCEED_MAX_SEAT_COUNT 예외가 발생한다")
    void holdSeats_exceedMaxSeatCount() {
        // given: 5개 좌석 선점 요청
        List<Long> seatIds = List.of(1L, 2L, 3L, 4L, 5L);

        // when & then: 예외가 발생해야 한다
        assertThatThrownBy(() -> seatService.holdSeats(SCHEDULE_ID, seatIds, USER_ID))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EXCEED_MAX_SEAT_COUNT);

        // then: 좌석 수 검증에서 바로 막혀야 하므로 DB 조회는 일어나지 않아야 한다
        verify(eventScheduleRepository, never()).findById(anyLong());
    }

    /*
     * [테스트 2] 예매 오픈 전 선점 차단
     *
     * 검증 내용
     * - 예매 오픈 시각 이전에 선점하면 BOOKING_NOT_OPEN_YET 예외가 발생한다.
     * - Redis 선점(tryHoldSeat)은 한 번도 시도하지 않는다.
     *
     * 왜 필요한가
     * - 기존 코드에서 throw가 빠져 있어 예매 오픈 전에도 선점이 되던 버그가 있었다.
     * - 이 테스트로 같은 버그가 다시 생기는 것을 막는다.
     */
    @Test
    @DisplayName("예매 오픈 전에 선점하면 BOOKING_NOT_OPEN_YET 예외가 발생하고 선점을 시도하지 않는다")
    void holdSeats_bookingNotOpenYet() {
        // given: 예매 오픈이 내일인 회차
        EventScheduleEntity schedule = EventScheduleEntity.builder()
                .scheduleId(SCHEDULE_ID)
                .bookingOpenAt(LocalDateTime.now().plusDays(1))
                .build();

        given(eventScheduleRepository.findById(SCHEDULE_ID))
                .willReturn(Optional.of(schedule));

        // when & then
        assertThatThrownBy(() -> seatService.holdSeats(SCHEDULE_ID, List.of(10L), USER_ID))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BOOKING_NOT_OPEN_YET);

        // then: 오픈 전이므로 Redis 선점 시도는 절대 일어나면 안 된다
        verify(seatHoldService, never()).tryHoldSeat(anyLong(), anyLong(), anyLong());
    }

    /*
     * [테스트 3] 존재하지 않는 회차
     *
     * 검증 내용
     * - DB에 없는 회차로 선점하면 NOT_FOUND_SCHEDULE 예외가 발생한다.
     *
     * 왜 필요한가
     * - 기존 코드는 회차가 없을 때 NOT_FOUND_USER(사용자 없음)를 던지고 있었다.
     * - 잘못된 에러 코드는 프론트엔드에 엉뚱한 안내 메시지를 보여주게 되므로,
     *   올바른 에러 코드가 나가는지 확인한다.
     */
    @Test
    @DisplayName("존재하지 않는 회차로 선점하면 NOT_FOUND_SCHEDULE 예외가 발생한다")
    void holdSeats_scheduleNotFound() {
        // given: DB에 회차가 없음
        given(eventScheduleRepository.findById(SCHEDULE_ID))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> seatService.holdSeats(SCHEDULE_ID, List.of(10L), USER_ID))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_FOUND_SCHEDULE);
    }

    /*
     * [테스트 4] 이미 예매 완료된 좌석
     *
     * 검증 내용
     * - DB 상태가 RESERVED(예매 완료)인 좌석을 선점하면 ALREADY_RESERVED_SEAT 예외가 발생한다.
     * - Redis 선점(tryHoldSeat)은 시도하지 않는다.
     *
     * 왜 필요한가
     * - 결제까지 끝난 좌석은 Redis에 선점 키가 없다(결제 완료 시 삭제됨).
     * - 따라서 DB 상태를 먼저 확인하지 않으면 이미 팔린 좌석이 다시 선점될 수 있다.
     */
    @Test
    @DisplayName("이미 예매 완료된 좌석을 선점하면 ALREADY_RESERVED_SEAT 예외가 발생한다")
    void holdSeats_alreadyReservedSeat() {
        // given: 예매가 열린 회차 + DB상 이미 예매 완료(RESERVED)된 좌석
        List<Long> seatIds = List.of(10L);

        given(eventScheduleRepository.findById(SCHEDULE_ID))
                .willReturn(Optional.of(openSchedule()));
        given(seatRepository.findBySeatIdIn(seatIds))
                .willReturn(List.of(seat(10L, SeatStatus.RESERVED)));

        // when & then
        assertThatThrownBy(() -> seatService.holdSeats(SCHEDULE_ID, seatIds, USER_ID))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ALREADY_RESERVED_SEAT);

        // then: DB에서 이미 예매된 좌석이면 Redis 선점은 시도하지 않는다
        verify(seatHoldService, never()).tryHoldSeat(anyLong(), anyLong(), anyLong());
    }

    /*
     * [테스트 5] 일부 좌석 선점 실패 시 롤백
     *
     * 상황
     * - 사용자가 10번, 20번 좌석을 함께 선점한다.
     * - 10번은 성공, 20번은 이미 다른 사용자가 선점해서 실패한다.
     *
     * 검증 내용
     * - ALREADY_HELD_SEAT 예외가 발생한다.
     * - 먼저 선점한 10번 좌석은 선점이 취소(롤백)된다.
     * - 실패한 20번 좌석은 취소하지 않는다.
     *
     * 왜 필요한가
     * - 롤백하지 않으면 사용자는 실패 메시지를 받았는데 10번 좌석은 TTL(5분) 동안 묶여 있게 된다.
     * - 반대로 20번까지 취소하면 다른 사용자의 선점을 풀어버리는 더 큰 버그가 된다.
     */
    @Test
    @DisplayName("여러 좌석 중 하나라도 선점에 실패하면 앞서 선점한 좌석을 롤백한다")
    void holdSeats_rollbackWhenPartialFailure() {
        // given: 좌석 10번, 20번 선점 요청
        List<Long> seatIds = List.of(10L, 20L);

        given(eventScheduleRepository.findById(SCHEDULE_ID))
                .willReturn(Optional.of(openSchedule()));
        given(seatRepository.findBySeatIdIn(seatIds))
                .willReturn(List.of(
                        seat(10L, SeatStatus.AVAILABLE),
                        seat(20L, SeatStatus.AVAILABLE)
                ));

        // 10번은 선점 성공, 20번은 이미 다른 사람이 선점해서 실패
        given(seatHoldService.tryHoldSeat(SCHEDULE_ID, 10L, USER_ID)).willReturn(true);
        given(seatHoldService.tryHoldSeat(SCHEDULE_ID, 20L, USER_ID)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> seatService.holdSeats(SCHEDULE_ID, seatIds, USER_ID))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ALREADY_HELD_SEAT);

        // then: 먼저 선점한 10번은 롤백(선점 취소)되어야 한다
        verify(seatHoldService, times(1)).cancelHold(SCHEDULE_ID, 10L);

        // then: 선점에 실패한 20번은 내 선점이 아니므로 취소하면 안 된다
        verify(seatHoldService, never()).cancelHold(SCHEDULE_ID, 20L);
    }

    /*
     * [테스트 6] 모든 좌석 선점 성공
     *
     * 검증 내용
     * - 선점한 좌석 목록이 요청한 순서대로 응답에 담긴다.
     * - 만료 시각이 응답에 포함된다.
     * - 성공했으므로 롤백(cancelHold)은 일어나지 않는다.
     *
     * 왜 필요한가
     * - 실패 상황만 검증하면 정상 흐름이 깨져도 알 수 없다.
     * - 프론트엔드는 응답의 좌석 목록과 만료 시각으로 타이머를 표시하므로 응답 내용도 확인한다.
     */
    @Test
    @DisplayName("모든 좌석 선점에 성공하면 선점한 좌석 목록을 반환하고 롤백하지 않는다")
    void holdSeats_success() {
        // given
        List<Long> seatIds = List.of(10L, 20L);

        given(eventScheduleRepository.findById(SCHEDULE_ID))
                .willReturn(Optional.of(openSchedule()));
        given(seatRepository.findBySeatIdIn(seatIds))
                .willReturn(List.of(
                        seat(10L, SeatStatus.AVAILABLE),
                        seat(20L, SeatStatus.AVAILABLE)
                ));
        given(seatHoldService.tryHoldSeat(SCHEDULE_ID, 10L, USER_ID)).willReturn(true);
        given(seatHoldService.tryHoldSeat(SCHEDULE_ID, 20L, USER_ID)).willReturn(true);

        // when
        SeatHoldResponse response = seatService.holdSeats(SCHEDULE_ID, seatIds, USER_ID);

        // then: 두 좌석 모두 선점 목록에 있어야 한다
        assertThat(response.getHeldSeatId()).containsExactly(10L, 20L);
        assertThat(response.getExpiredAt()).isNotNull();

        // then: 성공했으므로 롤백은 일어나지 않아야 한다
        verify(seatHoldService, never()).cancelHold(anyLong(), anyLong());
    }


    // 예매가 이미 열린 회차 (어제 오픈)
    private EventScheduleEntity openSchedule() {
        return EventScheduleEntity.builder()
                .scheduleId(SCHEDULE_ID)
                .bookingOpenAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    // 지정한 상태의 좌석 (AVAILABLE 또는 RESERVED)
    private SeatEntity seat(Long seatId, SeatStatus status) {
        return SeatEntity.builder()
                .seatId(seatId)
                .status(status)
                .build();
    }
}