package com.Ticksy.backend.domain.seat.DTO.Response;

import com.Ticksy.backend.domain.concert.enums.SeatGrade;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SectionWithSeatsResponse {

    private Long sectionId;
    private String name;
    private SeatGrade grade;
    private Integer price;
    private List<SeatItemResponse> seats;
}
