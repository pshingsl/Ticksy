package com.Ticksy.backend.domain.seat.DTO.Request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.List;

@Getter
public class SeatHoldCancelRequest {

    @NotNull(message = "회차 ID는 필수입니다.")
    private Long scheduleId;

    @NotEmpty(message = "좌석을 선택해주세요.")
    private List<Long> seatIds;
}
