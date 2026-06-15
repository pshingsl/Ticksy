package com.Ticksy.backend.domain.concert.DTO.Response;

import com.Ticksy.backend.domain.concert.Entity.ConcertEntity;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDate;

@Getter
@Builder
public class ConcertListResponse {

    private Long concertId;
    private String title;
    private String posterUrl;
    private String venueName;
    private LocalDate eventDate;
    private Integer minPrice;
    private Integer maxPrice;
    private boolean hasAvailableSeat;

    public static ConcertListResponse of(
            ConcertEntity concert,
            LocalDate eventDate,
            Integer minPrice,
            Integer maxPrice,
            boolean hasAvailableSeat
    ) {
        return ConcertListResponse.builder()
                .concertId(concert.getConcertId())
                .title(concert.getTitle())
                .posterUrl(concert.getPosterUrl())
                .venueName(concert.getVenue().getName())
                .eventDate(eventDate)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .hasAvailableSeat(hasAvailableSeat)
                .build();
    }
}
