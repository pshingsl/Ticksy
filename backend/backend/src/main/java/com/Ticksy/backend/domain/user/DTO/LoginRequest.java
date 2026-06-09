package com.Ticksy.backend.domain.user.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

// 로그인 요청
@Getter
public class LoginRequest {

    @NotBlank(message = "이메일을 입력해주세요.")
    private String email;

    @NotBlank(message = "비밀번호를 입력해주새요.")
    private String password;
}
