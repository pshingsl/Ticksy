package com.Ticksy.backend.domain.user.DTO;

// 이메일 인증 확인 응답

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EmailVerifyResponse {

    private boolean isVerified;

    public static EmailVerifyResponse of(boolean isVerified) {
        return EmailVerifyResponse.builder()
                .isVerified(isVerified)
                .build();
    }
}
