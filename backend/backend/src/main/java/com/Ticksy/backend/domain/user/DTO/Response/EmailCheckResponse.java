package com.Ticksy.backend.domain.user.DTO.Response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
// 이메일 중복 확인 응답
public class EmailCheckResponse {

    private boolean isDuplicate;

    public static EmailCheckResponse of(boolean isDuplicate) {
        return EmailCheckResponse.builder()
                .isDuplicate(isDuplicate)
                .build();
    }
}
