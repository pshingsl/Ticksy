package com.Ticksy.backend.domain.seat.DTO.Response;

import com.Ticksy.backend.domain.concert.Entity.SectionEntity;
import com.Ticksy.backend.domain.concert.enums.SeatGrade;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SectionTabResponse {
    private Long sectionId;
    private String name;
    private SeatGrade grade;
    private Integer price;

    public static SectionTabResponse of(SectionEntity section){
        return SectionTabResponse.builder()
                .sectionId(section.getSectionId())
                .name(section.getName())
                .grade(section.getGrade())
                .price(section.getPrice())
                .build();
    }
}
