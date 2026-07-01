package com.Ticksy.backend.domain.reservation.DTO.Request;

import lombok.Getter;

// 예매 취소
@Getter
public class ReservationCancelDto {
    private String cancelReason;
}
