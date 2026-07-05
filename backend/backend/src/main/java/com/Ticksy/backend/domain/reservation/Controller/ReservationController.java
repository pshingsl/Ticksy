package com.Ticksy.backend.domain.reservation.Controller;

import com.Ticksy.backend.domain.payment.DTO.Request.PaymentConfirmDto;
import com.Ticksy.backend.domain.payment.DTO.Request.PaymentRequestDto;
import com.Ticksy.backend.domain.payment.DTO.Response.PaymentConfirmResponse;
import com.Ticksy.backend.domain.payment.DTO.Response.PaymentReadyResponse;
import com.Ticksy.backend.domain.reservation.DTO.Request.ReservationCancelDto;
import com.Ticksy.backend.domain.reservation.DTO.Response.ReservationCancelResponse;
import com.Ticksy.backend.domain.reservation.DTO.Response.ReservationDetailResponse;
import com.Ticksy.backend.domain.reservation.DTO.Response.ReservationListResponse;
import com.Ticksy.backend.domain.reservation.DTO.Response.ReservationPreviewResponse;
import com.Ticksy.backend.domain.reservation.Service.ReservationService;
import com.Ticksy.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "예매/결제", description = "예매 정보 확인, 결제 요청/승인, 취소/환불 API")
@RestController
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    // 예매 정보 확인
    @Operation(summary = "예매 정보 확인", description = "선점된 좌석의 예매 정보를 확인합니다.")
    @GetMapping("/reservations/preview")
    public ResponseEntity<ApiResponse<ReservationPreviewResponse>> getPreview(
            @AuthenticationPrincipal Long userId,
            @RequestParam Long scheduleId,
            @RequestParam String seatIds
    ) {
        List<Long> seatIdList = Arrays.stream(seatIds.split(","))
                .map(Long::parseLong)
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                ApiResponse.success(
                        reservationService.getPreview(scheduleId, seatIdList, userId)
                )
        );
    }

    // 결제 요청
    @Operation(summary = "결제 요청", description = "Toss Payments 결제창 호출 전 서버 검증을 수행합니다.")
    @PostMapping("/payments/request")
    public ResponseEntity<ApiResponse<PaymentReadyResponse>> requestPayment(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid PaymentRequestDto request
    ) {
        return ResponseEntity.ok
                (ApiResponse.success(
                        reservationService.requestPayment(request, userId)
                ));
    }

    // 결제 승인 처리
    @Operation(summary = "결제 승인", description = "Toss Payments 결제 완료 후 서버 승인을 처리합니다.")
    @PostMapping("/payments/confirm")
    public ResponseEntity<ApiResponse<PaymentConfirmResponse>> confirmPayment(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid PaymentConfirmDto request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        reservationService.confirmPayment(request, userId)
                )
        );
    }

    // 예매 취소 + 환불 (PAY-05)
    @Operation(summary = "예매 취소/환불", description = "환불 정책에 따라 예매를 취소하고 Toss 환불을 처리합니다.")
    @DeleteMapping("/reservations/{reservationId}")
    public ResponseEntity<ApiResponse<ReservationCancelResponse>> cancelReservation(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long reservationId,
            @RequestBody(required = false) ReservationCancelDto request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        reservationService.cancelReservation(
                                reservationId, userId)
                )
        );
    }

    // 예매 내역 조회 (
    @Operation(summary = "예매 내역 조회", description = "로그인한 사용자의 전체 예매 내역을 조회합니다.")
    @GetMapping("/my/reservations")
    public ResponseEntity<ApiResponse<List<ReservationListResponse>>> getMyReservations(
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        reservationService.getMyReservations(userId)
                )
        );
    }

    // 예매 상세 조회
    @Operation(summary = "예매 상세 조회", description = "특정 예매의 상세 정보를 조회합니다.")
    @GetMapping("/my/reservations/{reservationId}")
    public ResponseEntity<ApiResponse<ReservationDetailResponse>> getReservationDetail(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long reservationId
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        reservationService.getReservationDetail(
                                reservationId, userId)
                )
        );
    }
}
