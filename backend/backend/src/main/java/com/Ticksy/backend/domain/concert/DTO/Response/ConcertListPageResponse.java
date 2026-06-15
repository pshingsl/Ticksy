package com.Ticksy.backend.domain.concert.DTO.Response;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Builder
public class ConcertListPageResponse {

    private List<ConcertListResponse> content;
    private long totalElements;
    private int totalPages;
    private int currentPages;

    public static ConcertListPageResponse of(
            Page<ConcertListResponse> page
    ) {
        return ConcertListPageResponse.builder()
                .content(page.getContent())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .currentPages(page.getNumber())
                .build();
    }
}
