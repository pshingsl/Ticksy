package com.Ticksy.backend.domain.seat.Controller;

import com.Ticksy.backend.domain.seat.DTO.Request.SeatHoldCancelRequest;
import com.Ticksy.backend.domain.seat.DTO.Request.SeatHoldRequest;
import com.Ticksy.backend.domain.seat.DTO.Response.SeatHoldResponse;
import com.Ticksy.backend.domain.seat.DTO.Response.SeatLayoutResponse;
import com.Ticksy.backend.domain.seat.Service.SeatService;
import com.Ticksy.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "좌석", description = "좌석 배치도 조회 및 선점/선점취소 API")
@RestController
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    // 좌석 배치도 조회
    @Operation(summary = "좌석 배치도 조회", description = "DB 상태(예매완료) + Redis 상태(선점중)를 조합하여 좌석 상태를 반환합니다. 로그인 필요.")
    @GetMapping("/concerts/{concertId}/schedules/{scheduleId}/seats")
    public ResponseEntity<ApiResponse<SeatLayoutResponse>> getSeatLayout (
            @PathVariable Long concertId,
            @PathVariable Long scheduleId
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        seatService.getSeatLayout(concertId, scheduleId)
                )
        );
    }
    // 좌석 선점 요청
    @Operation(summary = "좌석 선점 요청", description = "Redisson 분산 락을 통해 좌석을 선점합니다. 1인당 최대 4석, TTL 5분.")
    @PostMapping("/seats/hold")
    public ResponseEntity<ApiResponse<SeatHoldResponse>> holdSeats(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid SeatHoldRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        seatService.holdSeats(
                                request.getScheduleId(),
                                request.getSeatIds(),
                                userId
                        )
                )
        );
    }

    // 좌석 선점 취소
    @Operation(summary = "좌석 선점 취소", description = "본인이 선점한 좌석의 선점 상태를 즉시 해제합니다.")
    @DeleteMapping("/seats/hold")
    public ResponseEntity<ApiResponse<Void>> cancelHold(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid SeatHoldCancelRequest request
    ) {
        seatService.cancelHold(
                request.getScheduleId(),
                request.getSeatIds(),
                userId
        );
        return ResponseEntity.ok(ApiResponse.success());
    }
}
