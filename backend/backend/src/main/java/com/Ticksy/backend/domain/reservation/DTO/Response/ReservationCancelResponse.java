package com.Ticksy.backend.domain.reservation.DTO.Response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ReservationCancelResponse {

    private Integer refundAmount;
    private LocalDateTime cancelledAt;
}
