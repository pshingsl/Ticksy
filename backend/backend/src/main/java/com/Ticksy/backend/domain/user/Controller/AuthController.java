package com.Ticksy.backend.domain.user.Controller;

import com.Ticksy.backend.domain.user.DTO.Request.EmailSendRequest;
import com.Ticksy.backend.domain.user.DTO.Request.EmailVerifyRequest;
import com.Ticksy.backend.domain.user.DTO.Request.LoginRequest;
import com.Ticksy.backend.domain.user.DTO.Request.SignupRequest;
import com.Ticksy.backend.domain.user.DTO.Response.*;
import com.Ticksy.backend.domain.user.Service.UserService;
import com.Ticksy.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "회원 인증", description = "회원가입, 로그인, 토큰 관련 API")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    // 이메일 중복 확인
    @Operation(summary = "이메일 중복 확인", description = "회원가입 전 이메일 중복 여부를 확인합니다.")
    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<EmailCheckResponse>> checkEmail(@RequestParam String email) {
        return ResponseEntity.ok(ApiResponse.success(userService.checkEmail(email)));
    }

    // 이메일 인증번호 발송
    @Operation(summary = "이메일 인증번호 발송", description = "입력한 이메일로 6자리 인증번호를 발송합니다. (TTL 5분)")
    @PostMapping("/email/send")
    public ResponseEntity<ApiResponse<Void>> sendVerificationCode(@RequestBody @Valid EmailSendRequest request) {
        userService.sendVerificationCode(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success());
    }

    // 이메일 인증번호 확인
    @Operation(summary = "회원가입", description = "이메일 인증 완료 후 회원가입을 진행합니다.")
    @PostMapping("/email/verify")
    public ResponseEntity<ApiResponse<EmailVerifyResponse>> verifyEmail(@RequestBody @Valid EmailVerifyRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userService.verifyEmail(request)));
    }

    // 회원가입
    @Operation(summary = "로그인", description = "이메일/비밀번호로 로그인하고 Access Token을 발급받습니다.")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@RequestBody @Valid SignupRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userService.signup(request)));
    }

    // 로그인
    @Operation(summary = "회원가입", description = "이메일 인증 완료 후 회원가입을 진행합니다.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userService.login(request)));
    }

    // Access 토큰 재발급
    @Operation(summary = "Access Token 재발급", description = "Refresh Token으로 Access Token을 재발급받습니다.")
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<ReissueResponse>> reissue(@RequestHeader("Refresh-Token") String refreshToken) {
        return ResponseEntity.ok(ApiResponse.success(userService.reissue(refreshToken)));
    }

    //로그아웃
    @Operation(summary = "로그아웃", description = "Redis에 저장된 Refresh Token을 삭제합니다.")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal Long userId) {
        userService.logout(userId);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
