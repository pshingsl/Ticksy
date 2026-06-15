package com.Ticksy.backend.domain.user.DTO.Response;

import com.Ticksy.backend.domain.user.Entity.UserEntity;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
// 회원가입 응답
public class SignupResponse {

    private Long userId;
    private String email;
    private String name;

    public static SignupResponse from(UserEntity user) {
        return SignupResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .name(user.getName())
                .build();
    }
}
