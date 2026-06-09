package com.Ticksy.backend.domain.user.DTO;

import lombok.Builder;
import lombok.Getter;

// 이메일 중복 확인 응답
@Getter
@Builder
public class EmailCheckResponse {

    private boolean isDuplicate;

    public static EmailCheckResponse of(boolean isDuplicate) {
        return EmailCheckResponse.builder()
                .isDuplicate(isDuplicate)
                .build();
    }
}
