package com.Ticksy.backend.domain.user.DTO;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
// Access Token 재발급 응답
public class ReissueResponse {

    private String accessToken;

    public static ReissueResponse of(String accessToken) {
        return ReissueResponse.builder()
                .accessToken(accessToken)
                .build();
    }
}
