package com.Ticksy.backend.domain.reservation.DTO.Response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

// 예매 상세 조회 응답
@Getter
@Builder
public class ReservationDetailResponse {
    private Long reservationId;
    private String reservationCode;
    private String concertTitle;
    private LocalDate eventDate;
    private LocalTime eventTime;
    private String venueName;
    private List<ReservationPreviewResponse.SeatPreviewItem> seats;
    private Integer totalPrice;
    private String status;
    private LocalDateTime paidAt;
}
