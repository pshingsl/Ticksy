package com.Ticksy.backend.domain.user.DTO.Response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
// 로그인 응답
public class LoginResponse {

    private String accessToken;
    private String tokenType;

    public static LoginResponse of(String accessToken) {
        return LoginResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .build();
    }
}
