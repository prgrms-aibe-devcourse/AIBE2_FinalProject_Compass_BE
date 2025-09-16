package com.compass.domain.auth.dto;

import com.compass.domain.auth.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String email;
    private String nickname;
    private String profileImageUrl;
    private String provider;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserDto from(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .provider(user.getProvider())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    // Nested classes for other DTOs
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

    @Getter
    @NoArgsConstructor
    public static class ProfileUpdateRequest {
        private String nickname;
        private String profileImageUrl;
    }
    
    
}