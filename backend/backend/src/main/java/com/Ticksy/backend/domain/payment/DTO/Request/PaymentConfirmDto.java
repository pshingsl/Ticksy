package com.Ticksy.backend.domain.payment.DTO.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class PaymentConfirmDto {

    @NotBlank(message = "paymentKey는 필수입니다.")
    private String paymentKey;

    @NotBlank(message = "orderId는 필수입니다.")
    private String orderId;

    @NotNull(message = "결제 금액은 필수입니다.")
    private Integer amount;
}
