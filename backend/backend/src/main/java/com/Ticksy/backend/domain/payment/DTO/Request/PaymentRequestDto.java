package com.Ticksy.backend.domain.payment.DTO.Request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.List;

@Getter
public class PaymentRequestDto {
    @NotNull(message = "회차 ID는 필수입니다.")
    private Long scheduleId;

    @NotEmpty(message = "좌석을 선택해주세요.")
    private List<Long> seatIds;

    @NotNull(message = "결제 금액은 필수입니다.")
    private Integer totalPrice;
}
