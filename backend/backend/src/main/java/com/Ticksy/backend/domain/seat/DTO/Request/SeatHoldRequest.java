package com.Ticksy.backend.domain.seat.DTO.Request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.List;

@Getter
@Schema(description = "좌석 선점 요청")
public class SeatHoldRequest {

    @Schema(description = "회차 ID", example = "1")
    @NotNull(message = "회차 ID는 필수입니다.")
    private Long scheduleId;

    @Schema(description = "선점할 좌석 ID 목록 (최대 4개)", example = "[3, 4]")
    @NotEmpty(message = "좌석을 선택해주세요.")
    private List<Long> seatIds;
}
