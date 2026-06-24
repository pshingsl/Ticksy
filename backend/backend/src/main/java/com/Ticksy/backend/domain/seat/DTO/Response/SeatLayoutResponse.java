package com.Ticksy.backend.domain.seat.DTO.Response;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class SeatLayoutResponse {

    private Long scheduleId;
    private LocalDateTime bookingOpenAt;
    private List<SectionWithSeatsResponse> sections;
}
