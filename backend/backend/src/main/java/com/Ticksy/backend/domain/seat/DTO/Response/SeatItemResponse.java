package com.Ticksy.backend.domain.seat.DTO.Response;

import com.Ticksy.backend.domain.seat.Entity.SeatEntity;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SeatItemResponse {

    private Long seatId;
    private Integer rowNum;
    private Integer colNum;
    private String status; // AVAILABLE / HOLDING / RESERVED

    public static SeatItemResponse of(SeatEntity seat, boolean isHolding) {
        String status;
        if (seat.getStatus().name().equals("RESERVED")) {
            status = "RESERVED";
        } else if (isHolding) {
            status = "HOLDING";
        } else {
            status = "AVAILABLE";
        }

        return SeatItemResponse.builder()
                .seatId(seat.getSeatId())
                .rowNum(seat.getRowNum())
                .colNum(seat.getColNum())
                .status(status)
                .build();
    }
}