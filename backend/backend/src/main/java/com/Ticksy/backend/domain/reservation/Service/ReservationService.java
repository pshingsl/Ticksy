package com.Ticksy.backend.domain.reservation.Service;

import com.Ticksy.backend.domain.concert.Entity.EventScheduleEntity;
import com.Ticksy.backend.domain.concert.Entity.SectionEntity;
import com.Ticksy.backend.domain.concert.Repository.EventScheduleRepository;
import com.Ticksy.backend.domain.payment.DTO.Request.PaymentConfirmDto;
import com.Ticksy.backend.domain.payment.DTO.Request.PaymentRequestDto;
import com.Ticksy.backend.domain.payment.DTO.Response.PaymentConfirmResponse;
import com.Ticksy.backend.domain.payment.DTO.Response.PaymentReadyResponse;
import com.Ticksy.backend.domain.payment.Entity.PaymentEntity;
import com.Ticksy.backend.domain.payment.Repository.PaymentRepository;
import com.Ticksy.backend.domain.payment.Service.TossPaymentService;
import com.Ticksy.backend.domain.payment.enums.PaymentStatus;
import com.Ticksy.backend.domain.reservation.DTO.Response.ReservationCancelResponse;
import com.Ticksy.backend.domain.reservation.DTO.Response.ReservationDetailResponse;
import com.Ticksy.backend.domain.reservation.DTO.Response.ReservationListResponse;
import com.Ticksy.backend.domain.reservation.DTO.Response.ReservationPreviewResponse;
import com.Ticksy.backend.domain.reservation.Entity.ReservationEntity;
import com.Ticksy.backend.domain.reservation.Entity.ReservationSeatEntity;
import com.Ticksy.backend.domain.reservation.Repository.ReservationRepository;
import com.Ticksy.backend.domain.reservation.Repository.ReservationSeatRepository;
import com.Ticksy.backend.domain.reservation.enums.ReservationStatus;
import com.Ticksy.backend.domain.seat.Entity.SeatEntity;
import com.Ticksy.backend.domain.seat.Repository.SeatRepository;
import com.Ticksy.backend.domain.seat.Repository.SectionRepository;
import com.Ticksy.backend.domain.seat.Service.SeatHoldService;
import com.Ticksy.backend.domain.user.Entity.UserEntity;
import com.Ticksy.backend.domain.user.Repository.UserRepository;
import com.Ticksy.backend.global.exception.CustomException;
import com.Ticksy.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ReservationSeatRepository reservationSeatRepository;
    private final PaymentRepository paymentRepository;
    private final SeatRepository seatRepository;
    private final SectionRepository sectionRepository;
    private final EventScheduleRepository eventScheduleRepository;
    private final UserRepository userRepository;
    private final SeatHoldService seatHoldService;
    private final TossPaymentService tossPaymentService;

    // 예매 정보 확인
    public ReservationPreviewResponse getPreview(Long scheduleId, List<Long> seatIds, Long userId) {
        // 선점 여부
        for (Long seatId : seatIds) {
            if (!seatHoldService.isHeldByUser(scheduleId, seatId, userId)) {
                throw new CustomException((ErrorCode.SEAT_HOLD_EXPIRED));
            }
        }

        EventScheduleEntity schedule = eventScheduleRepository
                .findById(scheduleId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.NOT_FOUND_SCHEDULE));

        List<SeatEntity> seats = seatRepository.findBySeatIdIn(seatIds);

        List<ReservationPreviewResponse.SeatPreviewItem> seatItems =
                seats.stream().map(seat -> {
                    SectionEntity section = seat.getSection();
                    return ReservationPreviewResponse.SeatPreviewItem.builder()
                            .seatId(seat.getSeatId())
                            .sectionName(section.getName())
                            .grade(section.getGrade().name())
                            .rowNum(seat.getRowNum())
                            .colNum(seat.getColNum())
                            .price(section.getPrice())
                            .build();
                }).toList();

        Integer totalPrice = seatItems.stream()
                .mapToInt(ReservationPreviewResponse.SeatPreviewItem::getPrice)
                .sum();

        LocalDateTime holdExpiredAt =
                seatHoldService.getExpiredAt(scheduleId, seatIds.get(0));

        return ReservationPreviewResponse.builder()
                .scheduleId(scheduleId)
                .concertTitle(schedule.getConcert().getTitle())
                .eventDate(schedule.getEventDate())
                .eventTime(schedule.getEventTime())
                .venueName(schedule.getConcert().getVenue().getName())
                .seats(seatItems)
                .totalPrice(totalPrice)
                .holdExpiredAt(holdExpiredAt)
                .build();
    }

    // 결제 요청
    @Transactional
    public PaymentReadyResponse requestPayment(PaymentRequestDto request, Long userId) {
        // 선점 확인
        for (Long seatId : request.getSeatIds()) {
            if (!seatHoldService.isHeldByUser(request.getScheduleId(), seatId, userId)) {
                throw new CustomException(ErrorCode.SEAT_HOLD_EXPIRED);
            }
        }

        EventScheduleEntity schedule = eventScheduleRepository
                .findById(request.getScheduleId())
                .orElseThrow(() ->
                        new CustomException(ErrorCode.NOT_FOUND_SCHEDULE));

        List<SeatEntity> seats = seatRepository.findBySeatIdIn(request.getSeatIds());

        // 금액 검증
        Integer calculatedPrice = sectionRepository
                .findWithSeatsByScheduleId(request.getScheduleId())
                .stream()
                .flatMap(sec -> sec.getSeats().stream()
                        .filter(s -> request.getSeatIds()
                                .contains(s.getSeatId()))
                        .map(s -> sec.getPrice()))
                .mapToInt(Integer::intValue)
                .sum();

        if (!calculatedPrice.equals(request.getTotalPrice())) {
            throw new CustomException(ErrorCode.PRICE_MISMATCH);
        }

        // 주문생성 번호
        String today = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "ORDER-" + today + "-";
        Long count = reservationRepository
                .countByReservationCodePrefix(prefix);
        String orderId = prefix + String.format("%06d", count + 1);

        // 주문명 생성
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.NOT_FOUND_USER));

        String orderName = schedule.getConcert().getTitle();
        if (seats.size() > 1) {
            orderName += " 외 " + (seats.size() - 1) + "건";
        }

        return PaymentReadyResponse.builder()
                .orderId(orderId)
                .orderName(orderName)
                .amount(request.getTotalPrice())
                .customerName(user.getName())
                .customerEmail(user.getEmail())
                .build();
    }

    // 결제 승인 처리
    public PaymentConfirmResponse confirmPayment(PaymentConfirmDto request, Long userId) {

        // Toss 승인 API 호출
        Map<String, Object> tossResponse = tossPaymentService
                .confirmPayment(
                        request.getPaymentKey(),
                        request.getOrderId(),
                        request.getAmount()
                );

        // orderId에서 예매 정보 추출
        // orderId 형식: ORDER-날짜-순번
        // 실제 scheduleId, seatIds는 Redis에서 가져와야 하는데
        // 지금은 요청 시 저장한 정보를 활용하는 방식으로 처리
        // → 결제 요청 시 Redis에 orderId:info 저장하는 방식 사용

        String paitAtStr = (String) tossResponse.get("approvedAy");
        LocalDateTime paidAt = paitAtStr != null
                ? LocalDateTime.parse(paitAtStr.substring(0, 19))
                : LocalDateTime.now();

        // Redis에서 orderId에 해당하는 scheduleId, seatIds 조회
        // (결제 요청 시 저장해둔 것)
        String orderInfo = getOrderInfo(request.getOrderId());
        String[] parts = orderInfo.split(":");
        Long scheduleId = Long.parseLong(parts[0]);
        List<Long> seatIds = Arrays.stream(parts[1].split(","))
                .map(Long::parseLong)
                .toList();

        //선점 만료 재확인
        for (Long seatId : seatIds) {
            if (!seatHoldService.isHeldByUser(scheduleId, seatId, userId)) {
                throw new CustomException(ErrorCode.SEAT_HOLD_EXPIRED);
            }
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.NOT_FOUND_USER));

        EventScheduleEntity schedule = eventScheduleRepository.findById(scheduleId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.NOT_FOUND_SCHEDULE));

        List<SeatEntity> seats = seatRepository.findBySeatIdIn(seatIds);

        // 예매번호 생성
        String today = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "TK-" + today + "-";
        Long count = reservationRepository
                .countByReservationCodePrefix(prefix);
        String reservationCode = prefix + String.format("%06d", count + 1);

        // 총 금액 계산
        Integer totalPrice = seats.stream()
                .mapToInt(seat -> seat.getSection().getPrice())
                .sum();

        // 예매 저장
        ReservationEntity reservation = ReservationEntity.builder()
                .user(user)
                .schedule(schedule)
                .reservationCode(reservationCode)
                .totalPrice(totalPrice)
                .status(ReservationStatus.CONFIRMED)
                .build();

        // 예매 좌석 저장 + DB 좌석 상태 변경
        for (SeatEntity seat : seats) {
            ReservationSeatEntity reservationSeat =
                    ReservationSeatEntity.builder()
                            .reservation(reservation)
                            .seat(seat)
                            .price(seat.getSection().getPrice())
                            .build();
            reservationSeatRepository.save(reservationSeat);

            // DB 좌석 상태 → RESERVED
            seat.reserve();
        }

        // 결제 정보 저장
        PaymentEntity payment = PaymentEntity.builder()
                .reservation(reservation)
                .tossPaymentKey(request.getPaymentKey())
                .amount(request.getAmount())
                .status(PaymentStatus.DONE)
                .paidAt(paidAt)
                .build();
        paymentRepository.save(payment);

        // Redis 선점 삭제
        for (Long seatId : seatIds) {
            seatHoldService.cancelHold(scheduleId, seatId);
        }

        // Redis orderId 정보 삭제
        deleteOrderInfo(request.getOrderId());

        log.info("결제 승인 완료: reservationCode={}, userId={}",
                reservationCode, userId);

        return PaymentConfirmResponse.builder()
                .reservationId(reservation.getReservationId())
                .reservationCode(reservationCode)
                .paidAt(paidAt)
                .build();
    }

    // 예매 취소 + 환불 (PAY-05)
    @Transactional
    public ReservationCancelResponse cancelReservation(
            Long reservationId, Long userId
    ) {
        ReservationEntity reservation = reservationRepository
                .findByReservationIdAndUser_UserId(reservationId, userId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.NOT_FOUND_RESERVATION));

        // 본인 예매 확인
        if (!reservation.getUser().getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.RESERVATION_USER_MISMATCH);
        }

        // 이미 취소된 예매
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new CustomException(ErrorCode.ALREADY_CANCELLED);
        }

        // 환불 정책 계산 (공연일 기준)
        LocalDate eventDate = reservation.getSchedule().getEventDate();
        LocalDate today = LocalDate.now();
        long daysUntilEvent = today.until(eventDate,
                java.time.temporal.ChronoUnit.DAYS);

        if (daysUntilEvent < 3) {
            throw new CustomException(ErrorCode.CANCEL_PERIOD_EXPIRED);
        }

        PaymentEntity payment = paymentRepository
                .findByReservation_ReservationId(reservationId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.NOT_FOUND_RESERVATION));

        // 환불 금액 계산
        Integer refundAmount;
        String cancelReason;

        if (daysUntilEvent >= 7) {
            // 7일 전 이상 → 전액 환불
            refundAmount = payment.getAmount();
            cancelReason = "전액 환불";
            tossPaymentService.cancelPayment(
                    payment.getTossPaymentKey(), cancelReason, null
            );
            payment.cancel(LocalDateTime.now());
        } else {
            // 3일~7일 전 → 70% 환불
            refundAmount = (int) (payment.getAmount() * 0.7);
            cancelReason = "부분 환불 (70%)";
            tossPaymentService.cancelPayment(
                    payment.getTossPaymentKey(), cancelReason, refundAmount
            );
            payment.partialCancel(LocalDateTime.now());
        }

        // 예매 상태 취소
        reservation.cancel();

        // 좌석 상태 → AVAILABLE
        List<ReservationSeatEntity> reservationSeats =
                reservationSeatRepository
                        .findWithSeatByReservationId(reservationId);

        for (ReservationSeatEntity rs : reservationSeats) {
            rs.getSeat().release();
        }

        log.info("예매 취소 완료: reservationId={}, refund={}",
                reservationId, refundAmount);

        return ReservationCancelResponse.builder()
                .refundAmount(refundAmount)
                .cancelledAt(LocalDateTime.now())
                .build();
    }

    // 예매 내역 조회 (MP-01)
    public List<ReservationListResponse> getMyReservations(Long userId) {
        return reservationRepository
                .findByUser_UserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(ReservationListResponse::from)
                .toList();
    }

    // 예매 상세 조회
    public ReservationDetailResponse getReservationDetail(
            Long reservationId, Long userId
    ) {
        ReservationEntity reservation = reservationRepository
                .findByReservationIdAndUser_UserId(reservationId, userId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.NOT_FOUND_RESERVATION));

        List<ReservationSeatEntity> reservationSeats =
                reservationSeatRepository
                        .findWithSeatByReservationId(reservationId);

        List<ReservationPreviewResponse.SeatPreviewItem> seatItems =
                reservationSeats.stream().map(rs -> {
                    SeatEntity seat = rs.getSeat();
                    SectionEntity section = seat.getSection();
                    return ReservationPreviewResponse.SeatPreviewItem.builder()
                            .seatId(seat.getSeatId())
                            .sectionName(section.getName())
                            .grade(section.getGrade().name())
                            .rowNum(seat.getRowNum())
                            .colNum(seat.getColNum())
                            .price(rs.getPrice())
                            .build();
                }).toList();

        PaymentEntity payment = paymentRepository
                .findByReservation_ReservationId(reservationId)
                .orElse(null);

        return ReservationDetailResponse.builder()
                .reservationId(reservation.getReservationId())
                .reservationCode(reservation.getReservationCode())
                .concertTitle(reservation.getSchedule().getConcert().getTitle())
                .eventDate(reservation.getSchedule().getEventDate())
                .eventTime(reservation.getSchedule().getEventTime())
                .venueName(reservation.getSchedule().getConcert()
                        .getVenue().getName())
                .seats(seatItems)
                .totalPrice(reservation.getTotalPrice())
                .status(reservation.getStatus().name())
                .paidAt(payment != null ? payment.getPaidAt() : null)
                .build();
    }

    // Redis에 orderId:정보 저장/조회 헬퍼
    private final org.springframework.data.redis.core.RedisTemplate<String, String>
            redisTemplate = null; // 아래에서 생성자 주입으로 처리

    private String getOrderInfo(String orderId) {
        // 실제 구현은 아래 OrderRedisService에서 처리
        return "";
    }

    private void deleteOrderInfo(String orderId) {
        // 실제 구현은 아래 OrderRedisService에서 처리
    }
}
