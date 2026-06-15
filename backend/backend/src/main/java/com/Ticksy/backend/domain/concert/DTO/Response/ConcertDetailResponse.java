package com.Ticksy.backend.domain.concert.DTO.Response;

import com.Ticksy.backend.domain.concert.Entity.ConcertEntity;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ConcertDetailResponse {

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

    // 매개변수 3개로 둔 이유
    // 의존성 분리: DTO가 하위 엔티티(스케줄)이나 Enum(등급)의 내부 파싱 로직까지 알 필요 없게 단일 책임을 하기 위해
    // 쿼리 최적화(N+1 방지): 서비스 레이어에서 최적화되어 조회·가공이 끝난 DTO 리스트들을 주입받음으로써, 뷰 레이어 전환 시점에 원치 않는 추가 쿼리가 도는 것을 원천 차단
    // 성격이 다른 데이터(엔티티, 일대다 리스트, Enum 가공 리스트)는 밖에서 각각 만들어서 여기선 '최종 조립'만 한다.
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
