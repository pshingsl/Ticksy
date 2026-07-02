package com.Ticksy.backend.domain.payment.DTO.Response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentReadyResponse {

    private String orderId;
    private String orderName;
    private Integer amount;
    private String customerName;
    private String customerEmail;
}
