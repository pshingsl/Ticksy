package com.Ticksy.backend.domain.user.DTO;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
// 이메일 인증 확인 응답
public class EmailVerifyResponse {

    private boolean isVerified;

    public static EmailVerifyResponse of(boolean isVerified) {
        return EmailVerifyResponse.builder()
                .isVerified(isVerified)
                .build();
    }
}
