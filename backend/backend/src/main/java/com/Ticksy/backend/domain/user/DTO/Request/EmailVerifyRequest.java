package com.Ticksy.backend.domain.user.DTO.Request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
// 이메일 인증번호 확인 요청
public class EmailVerifyRequest {

    @NotBlank(message = "이메일을 입력 해주세요.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;

    @NotBlank(message = "인증버호를 입력해주세요.")
    private String code;
}
