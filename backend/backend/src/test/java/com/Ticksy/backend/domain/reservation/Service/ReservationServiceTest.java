package com.Ticksy.backend.domain.reservation.Service;

import com.Ticksy.backend.domain.concert.Entity.ConcertEntity;
import com.Ticksy.backend.domain.concert.Entity.EventScheduleEntity;
import com.Ticksy.backend.domain.concert.Entity.SectionEntity;
import com.Ticksy.backend.domain.concert.Repository.EventScheduleRepository;
import com.Ticksy.backend.domain.concert.Repository.SectionRepository;
import com.Ticksy.backend.domain.payment.DTO.Request.PaymentConfirmDto;
import com.Ticksy.backend.domain.payment.DTO.Request.PaymentRequestDto;
import com.Ticksy.backend.domain.payment.DTO.Response.PaymentConfirmResponse;
import com.Ticksy.backend.domain.payment.DTO.Response.PaymentReadyResponse;
import com.Ticksy.backend.domain.payment.Entity.PaymentEntity;
import com.Ticksy.backend.domain.payment.Repository.PaymentRepository;
import com.Ticksy.backend.domain.payment.Service.OrderRedisService;
import com.Ticksy.backend.domain.payment.Service.TossPaymentService;
import com.Ticksy.backend.domain.reservation.Entity.ReservationEntity;
import com.Ticksy.backend.domain.reservation.Entity.ReservationSeatEntity;
import com.Ticksy.backend.domain.reservation.Repository.ReservationRepository;
import com.Ticksy.backend.domain.reservation.Repository.ReservationSeatRepository;
import com.Ticksy.backend.domain.seat.Entity.SeatEntity;
import com.Ticksy.backend.domain.seat.Repository.SeatRepository;
import com.Ticksy.backend.domain.seat.Service.SeatHoldService;
import com.Ticksy.backend.domain.seat.enums.SeatStatus;
import com.Ticksy.backend.domain.user.Entity.UserEntity;
import com.Ticksy.backend.domain.user.Repository.UserRepository;
import com.Ticksy.backend.global.exception.CustomException;
import com.Ticksy.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
public class ReservationServiceTest {

    @Mock private ReservationRepository reservationRepository;
    @Mock private ReservationSeatRepository reservationSeatRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private SeatRepository seatRepository;
    @Mock private SectionRepository sectionRepository;
    @Mock private EventScheduleRepository eventScheduleRepository;
    @Mock private UserRepository userRepository;
    @Mock private SeatHoldService seatHoldService;
    @Mock private TossPaymentService tossPaymentService;
    @Mock private OrderRedisService orderRedisService;

    @InjectMocks
    private ReservationService reservationService;

    private static final Long SCHEDULE_ID = 1L;
    private static final Long USER_ID = 100L;
    private static final String ORDER_ID = "ORDER-TEST00000001";
    private static final String PAYMENT_KEY = "test_payment_key";
    private static final int SEAT_PRICE = 15000;
    private static final int TOTAL_PRICE = SEAT_PRICE * 2;   // 좌석 2개 = 30,000원

    // Redis에 저장된 주문 정보 (형식: "scheduleId:좌석들:금액")
    private static final String ORDER_INFO = SCHEDULE_ID + ":10,20:" + TOTAL_PRICE;

    // =====================================================================
    //  결제 승인 (confirmPayment)
    // =====================================================================

    /*
     * [테스트 1] 결제 승인 금액 위변조 차단
     *
     * 상황
     * - 결제 요청 시 서버가 계산한 금액은 30,000원이다 (Redis에 저장됨).
     * - 승인 요청에서 금액을 1,000원으로 바꿔서 보냈다.
     *
     * 검증 내용
     * - PRICE_MISMATCH 예외가 발생한다.
     * - Toss 승인 API는 호출되지 않는다 (실제 결제가 일어나지 않음).
     *
     * 왜 필요한가
     * - 기존 코드는 승인 단계에서 금액을 검증하지 않아, 금액을 바꿔 보내면 그대로 결제가 진행될 수 있었다.
     */
    @Test
    @DisplayName("결제 승인 금액이 서버에 저장된 금액과 다르면 PRICE_MISMATCH 예외가 발생하고 Toss 승인을 호출하지 않는다")
    void confirmPayment_priceMismatch() {
        // given: 서버에 저장된 금액은 30,000원
        given(orderRedisService.getOrderInfo(ORDER_ID)).willReturn(ORDER_INFO);

        // 승인 요청 금액을 1,000원으로 위조
        PaymentConfirmDto request = confirmRequest(1000);

        // when & then
        assertThatThrownBy(() -> reservationService.confirmPayment(request, USER_ID))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRICE_MISMATCH);

        // then: 금액이 틀리면 Toss 승인 API는 절대 호출되면 안 된다
        verify(tossPaymentService, never()).confirmPayment(anyString(), anyString(), any());
    }

    /*
     * [테스트 2] 좌석 선점 만료 후 결제 승인 차단
     *
     * 상황
     * - 사용자가 결제창에서 5분 넘게 머물러 좌석 선점이 만료됐다.
     *
     * 검증 내용
     * - SEAT_HOLD_EXPIRED 예외가 발생하고 Toss 승인 API는 호출되지 않는다.
     *
     * 왜 필요한가
     * - 선점이 만료된 좌석은 이미 다른 사용자가 선점했을 수 있다.
     *   그대로 결제하면 한 좌석이 두 명에게 팔릴 수 있다.
     */
    @Test
    @DisplayName("좌석 선점이 만료된 상태로 결제 승인을 요청하면 SEAT_HOLD_EXPIRED 예외가 발생하고 Toss 승인을 호출하지 않는다")
    void confirmPayment_seatHoldExpired() {
        // given: 금액은 정상이지만 10번 좌석의 선점이 만료됨
        given(orderRedisService.getOrderInfo(ORDER_ID)).willReturn(ORDER_INFO);
        given(seatHoldService.isHeldByUser(SCHEDULE_ID, 10L, USER_ID)).willReturn(false);

        PaymentConfirmDto request = confirmRequest(TOTAL_PRICE);

        // when & then
        assertThatThrownBy(() -> reservationService.confirmPayment(request, USER_ID))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SEAT_HOLD_EXPIRED);

        verify(tossPaymentService, never()).confirmPayment(anyString(), anyString(), any());
    }

    /*
     * [테스트 3] 주문 정보 만료 후 결제 승인 차단
     *
     * 상황
     * - Redis의 주문 정보(TTL 10분)가 만료되어 orderId로 조회할 수 없다.
     *
     * 검증 내용
     * - SEAT_HOLD_EXPIRED 예외가 발생하고 Toss 승인 API는 호출되지 않는다.
     *
     * 왜 필요한가
     * - 주문 정보가 없으면 어떤 좌석을 얼마에 결제하는지 서버가 확인할 수 없다.
     */
    @Test
    @DisplayName("주문 정보가 만료된 상태로 결제 승인을 요청하면 Toss 승인을 호출하지 않는다")
    void confirmPayment_orderInfoExpired() {
        // given: Redis에 주문 정보가 없어서 예외 발생
        given(orderRedisService.getOrderInfo(ORDER_ID))
                .willThrow(new CustomException(ErrorCode.SEAT_HOLD_EXPIRED));

        PaymentConfirmDto request = confirmRequest(TOTAL_PRICE);

        // when & then
        assertThatThrownBy(() -> reservationService.confirmPayment(request, USER_ID))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SEAT_HOLD_EXPIRED);

        verify(tossPaymentService, never()).confirmPayment(anyString(), anyString(), any());
    }

    /*
     * [테스트 4] 결제 승인 성공
     *
     * 검증 내용
     * - Toss 승인 API가 정확한 값으로 한 번 호출된다.
     * - 예매, 예매 좌석, 결제 정보가 DB에 저장된다.
     * - 좌석 상태가 RESERVED(예매 완료)로 바뀐다.
     * - Redis의 좌석 선점과 주문 정보가 삭제된다.
     *
     * 왜 필요한가
     * - 실패 케이스만 검증하면 정상 결제 흐름이 깨져도 알 수 없다.
     * - 결제 후 선점을 지우지 않으면 좌석이 5분간 불필요하게 묶이고,
     *   좌석 상태를 바꾸지 않으면 이미 팔린 좌석이 다시 선점될 수 있다.
     */
    @Test
    @DisplayName("검증을 모두 통과하면 Toss 승인 후 예매를 저장하고 좌석을 예매 완료 처리한다")
    void confirmPayment_success() {
        // given: 주문 정보와 선점 상태 모두 정상
        given(orderRedisService.getOrderInfo(ORDER_ID)).willReturn(ORDER_INFO);
        given(seatHoldService.isHeldByUser(SCHEDULE_ID, 10L, USER_ID)).willReturn(true);
        given(seatHoldService.isHeldByUser(SCHEDULE_ID, 20L, USER_ID)).willReturn(true);

        // Toss 승인 API 응답 (가짜)
        Map<String, Object> tossResponse = Map.of("approvedAt", "2026-11-26T23:50:00+09:00");
        given(tossPaymentService.confirmPayment(PAYMENT_KEY, ORDER_ID, TOTAL_PRICE))
                .willReturn(tossResponse);

        // DB 조회 결과 (가짜)
        SectionEntity section = section();
        SeatEntity seat10 = seat(10L, section);
        SeatEntity seat20 = seat(20L, section);

        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user()));
        given(eventScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule()));
        given(seatRepository.findBySeatIdIn(List.of(10L, 20L))).willReturn(List.of(seat10, seat20));
        given(reservationRepository.countByReservationCodePrefix(anyString())).willReturn(0L);

        // when
        PaymentConfirmResponse response =
                reservationService.confirmPayment(confirmRequest(TOTAL_PRICE), USER_ID);

        // then: Toss 승인은 정확한 값으로 한 번만 호출
        verify(tossPaymentService, times(1)).confirmPayment(PAYMENT_KEY, ORDER_ID, TOTAL_PRICE);

        // then: 예매 1건, 예매 좌석 2건, 결제 정보 1건 저장
        verify(reservationRepository, times(1)).save(any(ReservationEntity.class));
        verify(reservationSeatRepository, times(2)).save(any(ReservationSeatEntity.class));
        verify(paymentRepository, times(1)).save(any(PaymentEntity.class));

        // then: 좌석 상태가 예매 완료로 변경
        assertThat(seat10.getStatus()).isEqualTo(SeatStatus.RESERVED);
        assertThat(seat20.getStatus()).isEqualTo(SeatStatus.RESERVED);

        // then: Redis 선점과 주문 정보 삭제
        verify(seatHoldService).cancelHold(SCHEDULE_ID, 10L);
        verify(seatHoldService).cancelHold(SCHEDULE_ID, 20L);
        verify(orderRedisService).deleteOrderInfo(ORDER_ID);

        // then: 예매번호 형식 확인 (TK-날짜-순번)
        assertThat(response.getReservationCode()).startsWith("TK-");
    }

    // =====================================================================
    //  결제 요청 (requestPayment)
    // =====================================================================

    /*
     * [테스트 5] 결제 요청 금액 위변조 차단
     *
     * 상황
     * - DB 기준 좌석 2개 가격은 30,000원인데, 클라이언트가 1,000원으로 결제를 요청했다.
     *
     * 검증 내용
     * - PRICE_MISMATCH 예외가 발생한다.
     * - Redis에 주문 정보가 저장되지 않는다 (다음 단계인 결제 승인으로 넘어갈 수 없음).
     */
    @Test
    @DisplayName("결제 요청 금액이 DB 가격과 다르면 PRICE_MISMATCH 예외가 발생하고 주문 정보를 저장하지 않는다")
    void requestPayment_priceMismatch() {
        // given
        List<Long> seatIds = List.of(10L, 20L);
        given(seatHoldService.isHeldByUser(SCHEDULE_ID, 10L, USER_ID)).willReturn(true);
        given(seatHoldService.isHeldByUser(SCHEDULE_ID, 20L, USER_ID)).willReturn(true);
        given(eventScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule()));
        given(sectionRepository.findWithSeatsByScheduleId(SCHEDULE_ID))
                .willReturn(List.of(sectionWithSeats(10L, 20L)));

        // 30,000원짜리를 1,000원으로 요청
        PaymentRequestDto request = paymentRequest(seatIds, 1000);

        // when & then
        assertThatThrownBy(() -> reservationService.requestPayment(request, USER_ID))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRICE_MISMATCH);

        // then: 주문 정보가 저장되지 않아야 결제 승인 단계로 넘어갈 수 없다
        verify(orderRedisService, never()).saveOrderInfo(anyString(), anyLong(), any(), any());
    }

    /*
     * [테스트 6] 결제 요청 시 서버가 계산한 금액을 저장
     *
     * 검증 내용
     * - Redis 주문 정보에 서버가 DB 가격으로 계산한 금액(30,000원)이 저장된다.
     * - 응답에 orderId가 서버에서 생성되어 담긴다.
     *
     * 왜 필요한가
     * - 결제 승인 시 이 금액을 기준으로 위변조를 검증하므로(테스트 1),
     *   여기서 올바른 금액이 저장되는 것이 금액 검증 전체의 전제 조건이다.
     */
    @Test
    @DisplayName("결제 요청이 정상이면 서버가 계산한 금액으로 주문 정보를 저장하고 orderId를 발급한다")
    void requestPayment_success() {
        // given
        List<Long> seatIds = List.of(10L, 20L);
        given(seatHoldService.isHeldByUser(SCHEDULE_ID, 10L, USER_ID)).willReturn(true);
        given(seatHoldService.isHeldByUser(SCHEDULE_ID, 20L, USER_ID)).willReturn(true);
        given(eventScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule()));

        SectionEntity section = sectionWithSeats(10L, 20L);
        given(sectionRepository.findWithSeatsByScheduleId(SCHEDULE_ID)).willReturn(List.of(section));
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user()));
        given(seatRepository.findBySeatIdIn(seatIds)).willReturn(section.getSeats());

        PaymentRequestDto request = paymentRequest(seatIds, TOTAL_PRICE);

        // when
        PaymentReadyResponse response = reservationService.requestPayment(request, USER_ID);

        // then: 서버가 계산한 30,000원이 주문 정보에 저장된다
        verify(orderRedisService).saveOrderInfo(
                anyString(), eq(SCHEDULE_ID), eq(seatIds), eq(TOTAL_PRICE)
        );

        // then: orderId는 서버에서 생성된다
        assertThat(response.getOrderId()).startsWith("ORDER-");
    }

    // =====================================================================
    //  테스트용 객체 생성 헬퍼
    // =====================================================================

    // 결제 승인 요청 DTO (setter가 없어서 ReflectionTestUtils로 필드에 값을 넣음)
    private PaymentConfirmDto confirmRequest(int amount) {
        PaymentConfirmDto dto = new PaymentConfirmDto();
        ReflectionTestUtils.setField(dto, "paymentKey", PAYMENT_KEY);
        ReflectionTestUtils.setField(dto, "orderId", ORDER_ID);
        ReflectionTestUtils.setField(dto, "amount", amount);
        return dto;
    }

    // 결제 요청 DTO
    private PaymentRequestDto paymentRequest(List<Long> seatIds, int totalPrice) {
        PaymentRequestDto dto = new PaymentRequestDto();
        ReflectionTestUtils.setField(dto, "scheduleId", SCHEDULE_ID);
        ReflectionTestUtils.setField(dto, "seatIds", seatIds);
        ReflectionTestUtils.setField(dto, "totalPrice", totalPrice);
        return dto;
    }

    // 예매가 열린 회차 (공연 제목 포함)
    private EventScheduleEntity schedule() {
        ConcertEntity concert = ConcertEntity.builder()
                .title("테스트 공연")
                .build();

        return EventScheduleEntity.builder()
                .scheduleId(SCHEDULE_ID)
                .concert(concert)
                .bookingOpenAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    private UserEntity user() {
        return UserEntity.builder()
                .userId(USER_ID)
                .name("테스트유저")
                .email("test@ticksy.com")
                .build();
    }

    // 좌석 1개당 15,000원인 구역
    private SectionEntity section() {
        return SectionEntity.builder()
                .price(SEAT_PRICE)
                .build();
    }

    // 지정한 좌석들을 포함한 구역 (결제 요청 시 금액 계산용)
    private SectionEntity sectionWithSeats(Long... seatIds) {
        SectionEntity section = section();
        for (Long seatId : seatIds) {
            section.getSeats().add(seat(seatId, section));
        }
        return section;
    }

    // 예매 가능 상태의 좌석
    private SeatEntity seat(Long seatId, SectionEntity section) {
        return SeatEntity.builder()
                .seatId(seatId)
                .section(section)
                .status(SeatStatus.AVAILABLE)
                .build();
    }
}