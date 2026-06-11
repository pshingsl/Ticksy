package com.Ticksy.backend.domain.user.Controller;

import com.Ticksy.backend.domain.user.DTO.PasswordChangeRequest;
import com.Ticksy.backend.domain.user.Service.UserService;
import com.Ticksy.backend.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/my")
@RequiredArgsConstructor
public class MyPageController {

    private final UserService userService;

    // 비밀번호 변경
    @PatchMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid PasswordChangeRequest request) {
        userService.changePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    // 회원탈퇴
    @DeleteMapping("/account")
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @AuthenticationPrincipal Long userId) {
        userService.withdraw(userId);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
