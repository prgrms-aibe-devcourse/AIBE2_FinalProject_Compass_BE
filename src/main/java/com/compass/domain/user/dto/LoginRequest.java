package com.compass.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "로그인 요청 DTO")
public class LoginRequest {
    @Schema(description = "이메일", example = "user@example.com")
    @NotBlank @Email private final String email;
    @Schema(description = "비밀번호", example = "password123!")
    @NotBlank private final String password;
}