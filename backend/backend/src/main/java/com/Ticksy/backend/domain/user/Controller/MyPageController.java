package com.Ticksy.backend.domain.user.Controller;

import com.Ticksy.backend.domain.user.DTO.Request.PasswordChangeRequest;
import com.Ticksy.backend.domain.user.Service.UserService;
import com.Ticksy.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "마이페이지", description = "비밀번호 변경, 회원 탈퇴 API")
@RestController
@RequestMapping("/my")
@RequiredArgsConstructor
public class MyPageController {

    private final UserService userService;

    // 비밀번호 변경
    @Operation(summary = "비밀번호 변경", description = "현재 비밀번호 확인 후 새 비밀번호로 변경합니다. 변경 시 자동 로그아웃됩니다.")
    @PatchMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid PasswordChangeRequest request) {
        userService.changePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    // 회원탈퇴

    @Operation(summary = "회원 탈퇴", description = "소프트 삭제로 처리됩니다. 확정된 예매 내역이 있으면 탈퇴할 수 없습니다.")
    @DeleteMapping("/account")
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @AuthenticationPrincipal Long userId) {
        userService.withdraw(userId);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
