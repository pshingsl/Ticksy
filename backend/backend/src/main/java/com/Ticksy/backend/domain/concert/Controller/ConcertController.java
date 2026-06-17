package com.Ticksy.backend.domain.concert.Controller;

import com.Ticksy.backend.domain.concert.DTO.Response.ConcertDetailResponse;
import com.Ticksy.backend.domain.concert.DTO.Response.ConcertListPageResponse;
import com.Ticksy.backend.domain.concert.Service.ConcertService;
import com.Ticksy.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "공연", description = "공연 목록/검색/상세 조회 API")
@RestController
@RequestMapping("/concerts")
@RequiredArgsConstructor
public class ConcertController {

    private final ConcertService concertService;

    // 공연 목록 조회
    @Operation(summary = "공연 목록 조회", description = "날짜/지역 필터와 페이지네이션을 지원합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<ConcertListPageResponse>> getConcertList(
            @Parameter(description = "공연 날짜 (yyyy-MM-dd)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "공연장 지역 키워드")
            @RequestParam(required = false) String region,
            @Parameter(description = "페이지 번호 (0부터 시작)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기")
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(
                        concertService.getConcertList(date, region, pageable)
                )
        );
    }

    // 공연 검색
    @Operation(summary = "공연 검색", description = "공연명 또는 출연진 이름으로 검색합니다.")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<ConcertListPageResponse>> searchConcerts(
            @Parameter(description = "검색 키워드", required = true)
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
    @Operation(summary = "공연 상세 조회", description = "회차 목록, 등급별 가격 등 상세 정보를 반환합니다.")
    @GetMapping("/{concertId}")
    public ResponseEntity<ApiResponse<ConcertDetailResponse>> getConcertDetail(
            @Parameter(description = "공연 ID", required = true)
            @PathVariable Long concertId
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(concertService.getConcertDetail(concertId))
        );
    }
}
