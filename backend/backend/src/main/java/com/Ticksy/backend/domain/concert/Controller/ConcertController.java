package com.Ticksy.backend.domain.concert.Controller;

import com.Ticksy.backend.domain.concert.DTO.Response.ConcertDetailResponse;
import com.Ticksy.backend.domain.concert.DTO.Response.ConcertListPageResponse;
import com.Ticksy.backend.domain.concert.Service.ConcertService;
import com.Ticksy.backend.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/concerts")
@RequiredArgsConstructor
public class ConcertController {

    private final ConcertService concertService;

    // 공연 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponse<ConcertListPageResponse>> getConcertList(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String region,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(
                        concertService.getConcertList(date, region, pageable)
                )
        );
    }

    // 공연 검색
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<ConcertListPageResponse>> searchConcerts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                ApiResponse.success(concertService.searchConcert(keyword, pageable))
        );
    }

    // 공연 상세 조회
    @GetMapping("/{concertId}")
    public ResponseEntity<ApiResponse<ConcertDetailResponse>> getConcertDetail(
            @PathVariable Long concertId
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(concertService.getConcertDetail(concertId))
        );
    }
}
