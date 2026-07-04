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
import com.Ticksy.backend.domain.payment.Service.OrderRedisService;
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
    private final OrderRedisService orderRedisService;

    // 예매 정보 확인
    public ReservationPreviewResponse getPreview(
            Long scheduleId, List<Long> seatIds, Long userId) {

        // 본인이 선점한 좌석인지 확인
        for (Long seatId : seatIds) {
            if (!seatHoldService.isHeldByUser(scheduleId, seatId, userId)) {
                throw new CustomException(ErrorCode.SEAT_HOLD_EXPIRED);
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
                .mapToInt(
                        ReservationPreviewResponse.SeatPreviewItem::getPrice
                )
                .sum();

        // 선점 만료 시각 조회 (첫 번째 좌석 기준)
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
    public PaymentReadyResponse requestPayment(
            PaymentRequestDto request, Long userId) {

        // 선점 재확인
        for (Long seatId : request.getSeatIds()) {
            if (!seatHoldService.isHeldByUser(
                    request.getScheduleId(), seatId, userId)) {
                throw new CustomException(ErrorCode.SEAT_HOLD_EXPIRED);
            }
        }

        EventScheduleEntity schedule = eventScheduleRepository
                .findById(request.getScheduleId())
                .orElseThrow(() ->
                        new CustomException(ErrorCode.NOT_FOUND_SCHEDULE));

        List<SeatEntity> seats =
                seatRepository.findBySeatIdIn(request.getSeatIds());

        // 금액 검증 (위변조 방지)
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

        // orderId 생성 (ORDER-날짜-순번)
        String today = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "ORDER-" + today + "-";
        Long count = reservationRepository
                .countByReservationCodePrefix(prefix);
        String orderId = prefix + String.format("%06d", count + 1);

        // 사용자 조회
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.NOT_FOUND_USER));

        // 주문명 생성
        String orderName = schedule.getConcert().getTitle();
        if (seats.size() > 1) {
            orderName += " 외 " + (seats.size() - 1) + "건";
        }

        // Redis에 orderId 정보 저장 (결제 승인 시 사용)
        orderRedisService.saveOrderInfo(
                orderId,
                request.getScheduleId(),
                request.getSeatIds()
        );

        log.info("결제 요청 생성: orderId={}, userId={}", orderId, userId);

        return PaymentReadyResponse.builder()
                .orderId(orderId)
                .orderName(orderName)
                .amount(request.getTotalPrice())
                .customerName(user.getName())
                .customerEmail(user.getEmail())
                .build();
    }

    // 결제 승인 처리 (PAY-03)
    @Transactional
    public PaymentConfirmResponse confirmPayment(
            PaymentConfirmDto request, Long userId) {

        // Redis에서 orderId → scheduleId, seatIds 조회
        String orderInfo =
                orderRedisService.getOrderInfo(request.getOrderId());
        String[] parts = orderInfo.split(":");
        Long scheduleId = Long.parseLong(parts[0]);
        List<Long> seatIds = Arrays.stream(parts[1].split(","))
                .map(Long::parseLong)
                .toList();

        // 선점 만료 재확인
        for (Long seatId : seatIds) {
            if (!seatHoldService.isHeldByUser(scheduleId, seatId, userId)) {
                throw new CustomException(ErrorCode.SEAT_HOLD_EXPIRED);
            }
        }

        // Toss 승인 API 호출
        Map<String, Object> tossResponse = tossPaymentService
                .confirmPayment(
                        request.getPaymentKey(),
                        request.getOrderId(),
                        request.getAmount()
                );

        // Toss 응답에서 승인 시각 파싱 (형식: "2024-08-15T12:34:56+09:00")
        String paidAtStr = (String) tossResponse.get("approvedAt");
        LocalDateTime paidAt = paidAtStr != null
                ? LocalDateTime.parse(
                paidAtStr.substring(0, 19),
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
        )
                : LocalDateTime.now();

        // 사용자, 회차, 좌석 조회
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.NOT_FOUND_USER));

        EventScheduleEntity schedule = eventScheduleRepository
                .findById(scheduleId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.NOT_FOUND_SCHEDULE));

        List<SeatEntity> seats = seatRepository.findBySeatIdIn(seatIds);

        // 예매번호 생성 (TK-날짜-순번)
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
        reservationRepository.save(reservation);

        // 예매 좌석 저장 + DB 좌석 상태 → RESERVED
        for (SeatEntity seat : seats) {
            ReservationSeatEntity reservationSeat =
                    ReservationSeatEntity.builder()
                            .reservation(reservation)
                            .seat(seat)
                            .price(seat.getSection().getPrice())
                            .build();
            reservationSeatRepository.save(reservationSeat);
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
        orderRedisService.deleteOrderInfo(request.getOrderId());

        log.info("결제 승인 완료: reservationCode={}, userId={}",
                reservationCode, userId);

        return PaymentConfirmResponse.builder()
                .reservationId(reservation.getReservationId())
                .reservationCode(reservationCode)
                .paidAt(paidAt)
                .build();
    }

    // 예매 취소 + 환불
    @Transactional
    public ReservationCancelResponse cancelReservation(
            Long reservationId, Long userId) {

        // 본인 예매 확인
        ReservationEntity reservation = reservationRepository
                .findByReservationIdAndUser_UserId(reservationId, userId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.NOT_FOUND_RESERVATION));

        // 이미 취소된 예매
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new CustomException(ErrorCode.ALREADY_CANCELLED);
        }

        // 환불 정책 계산 (공연일 기준)
        LocalDate eventDate = reservation.getSchedule().getEventDate();
        LocalDate today = LocalDate.now();
        long daysUntilEvent = today.until(
                eventDate, java.time.temporal.ChronoUnit.DAYS
        );

        // 3일 이내 취소 불가
        if (daysUntilEvent < 3) {
            throw new CustomException(ErrorCode.CANCEL_PERIOD_EXPIRED);
        }

        PaymentEntity payment = paymentRepository
                .findByReservation_ReservationId(reservationId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.NOT_FOUND_RESERVATION));

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

        // 예매 상태 → CANCELLED
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

    // 예매 내역 조회
    public List<ReservationListResponse> getMyReservations(Long userId) {
        return reservationRepository
                .findByUser_UserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(ReservationListResponse::from)
                .toList();
    }

    // 예매 상세 조회
    public ReservationDetailResponse getReservationDetail(
            Long reservationId, Long userId) {

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
                .concertTitle(
                        reservation.getSchedule().getConcert().getTitle()
                )
                .eventDate(reservation.getSchedule().getEventDate())
                .eventTime(reservation.getSchedule().getEventTime())
                .venueName(
                        reservation.getSchedule().getConcert()
                                .getVenue().getName()
                )
                .seats(seatItems)
                .totalPrice(reservation.getTotalPrice())
                .status(reservation.getStatus().name())
                .paidAt(payment != null ? payment.getPaidAt() : null)
                .build();
    }
}