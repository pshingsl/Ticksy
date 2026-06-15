package com.Ticksy.backend.domain.concert.DTO.Response;

import com.Ticksy.backend.domain.concert.enums.SeatGrade;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GradeResponse {

    private SeatGrade grade;
    private Integer price;

    public static GradeResponse of(SeatGrade grade, Integer price) {
        return GradeResponse.builder()
                .grade(grade)
                .price(price)
                .build();
    }
}
