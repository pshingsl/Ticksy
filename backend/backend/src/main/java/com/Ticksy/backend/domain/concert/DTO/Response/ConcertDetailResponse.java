package com.Ticksy.backend.domain.concert.DTO.Response;

import com.Ticksy.backend.domain.concert.Entity.ConcertEntity;
import lombok.Builder;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Getter
@Builder
public class ConcertDetailResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    // 응답시 가져올 데이터 정의
    private Long concertId;
    private String title;
    private String cast;
    private String ageLimit;
    private Integer runTime;
    private String description;
    private String posterUrl;
    private String venueName;
    private String venueAddress;
    private List<ScheduleResponse> schedules;
    private List<GradeResponse> grades;

    public static ConcertDetailResponse of(
            ConcertEntity concert,
            List<ScheduleResponse> schedules,
            List<GradeResponse> grades) {
        return  ConcertDetailResponse.builder()
                .concertId(concert.getConcertId())
                .title(concert.getTitle())
                .cast(concert.getCast())
                .ageLimit(concert.getAgeLimit())
                .runTime(concert.getRunTime())
                .description(concert.getDescription())
                .posterUrl(concert.getPosterUrl())
                .venueName(concert.getVenue().getName())
                .venueAddress(concert.getVenue().getAddress())
                .schedules(schedules)
                .grades(grades)
                .build();
    }
}
