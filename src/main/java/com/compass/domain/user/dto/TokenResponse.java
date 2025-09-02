package com.compass.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "JWT 토큰 응답 DTO")
public class TokenResponse {
    @Schema(description = "Access Token")
    private final String accessToken;
}