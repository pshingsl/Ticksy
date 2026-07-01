package com.Ticksy.backend.domain.reservation.DTO.Response;

import com.Ticksy.backend.domain.reservation.Entity.ReservationEntity;
import com.Ticksy.backend.domain.reservation.enums.ReservationStatus;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

// 예매 내역 조회 응답
@Getter
@Builder
public class ReservationListResponse {
    private Long reservationId;
    private String reservationCode;
    private String concertTitle;
    private LocalDate eventDate;
    private LocalTime eventTime;
    private String venueName;
    private Integer totalPrice;
    private ReservationStatus status;
    private LocalDateTime createdAt;

    public static ReservationListResponse from(ReservationEntity r) {
        return ReservationListResponse.builder()
                .reservationId(r.getReservationId())
                .reservationCode(r.getReservationCode())
                .concertTitle(r.getSchedule().getConcert().getTitle())
                .eventDate(r.getSchedule().getEventDate())
                .eventTime(r.getSchedule().getEventTime())
                .venueName(r.getSchedule().getConcert().getVenue().getName())
                .totalPrice(r.getTotalPrice())
                .status(r.getStatus())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
