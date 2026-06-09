package com.Ticksy.backend.domain.user.DTO;

import lombok.Builder;
import lombok.Getter;

// Access Token 재발급 응답
@Getter
@Builder
public class ReissueResponse {

    private String accessToken;

    public static ReissueResponse of(String accessToken) {
        return ReissueResponse.builder()
                .accessToken(accessToken)
                .build();
    }
}
