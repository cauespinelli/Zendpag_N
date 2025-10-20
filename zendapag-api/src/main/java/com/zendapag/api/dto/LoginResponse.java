package com.zendapag.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private String token;
    private String type;
    private UserResponse user;

    public static LoginResponse of(String token, UserResponse user) {
        return LoginResponse.builder()
                .token(token)
                .type("Bearer")
                .user(user)
                .build();
    }
}