package com.Ticksy.backend.domain.user.Controller;

import com.Ticksy.backend.domain.user.DTO.*;
import com.Ticksy.backend.domain.user.Service.UserService;
import com.Ticksy.backend.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    // 이메일 중복 확인
    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<EmailCheckResponse>> checkEmail(@RequestParam String email) {
        return ResponseEntity.ok(ApiResponse.success(userService.checkEmail(email)));
    }

    // 이메일 인증번호 발송
    @PostMapping("/email/send")
    public ResponseEntity<ApiResponse<Void>> sendVerificationCode(@RequestBody @Valid EmailSendRequest request) {
        userService.sendVerificationCode(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success());
    }

    // 이메일 인증번호 확인
    @PostMapping("/email/verify")
    public ResponseEntity<ApiResponse<EmailVerifyResponse>> verifyEmail(@RequestBody @Valid EmailVerifyRequest  request) {
        return ResponseEntity.ok(ApiResponse.success(userService.verifyEmail(request)));
    }

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@RequestBody @Valid SignupRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userService.signup(request)));
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userService.login(request)));
    }

    // Access 토큰 재발급
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<ReissueResponse>> reissue(@RequestHeader("Refresh-Token") String refreshToken) {
        return ResponseEntity.ok(ApiResponse.success(userService.reissue(refreshToken)));
    }

    //로그아웃
    @DeleteMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal Long userId) {
        userService.logout(userId);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
