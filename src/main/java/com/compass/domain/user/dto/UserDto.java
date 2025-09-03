package com.compass.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class UserDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SignUpRequest {
        @Email(message = "잘못된 이메일 형식입니다.")
        @NotBlank(message = "Email is required.")
        private String email;

        @NotBlank(message = "Password is required.")
        private String password;

        @NotBlank(message = "Nickname is required.")
        private String nickname;
    }

    @Getter
    @Builder
    public static class SignUpResponse {
        private Long id;
        private String email;
        private String nickname;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        @NotBlank(message = "Email is required.")
        private String email;

        @NotBlank(message = "Password is required.")
        private String password;
    }

    @Getter
    @Builder
    public static class LoginResponse {
        private String accessToken;
        private String refreshToken;
    }
}