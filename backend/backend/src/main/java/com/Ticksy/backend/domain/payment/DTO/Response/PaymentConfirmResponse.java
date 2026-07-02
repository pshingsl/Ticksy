package com.Ticksy.backend.domain.payment.DTO.Response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PaymentConfirmResponse {

    private Long reservationId;
    private String reservationCode;
    private LocalDateTime paidAt;
}
