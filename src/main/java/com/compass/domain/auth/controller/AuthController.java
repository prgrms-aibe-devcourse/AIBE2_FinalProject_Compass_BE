package com.compass.domain.auth.controller;

import com.compass.domain.auth.dto.JwtDto;
import com.compass.domain.auth.dto.LoginRequestDto;
import com.compass.domain.auth.dto.RefreshTokenRequestDto;
import com.compass.domain.auth.dto.SignupRequestDto;
import com.compass.domain.auth.dto.UserDto;
import com.compass.domain.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<UserDto> signup(@Valid @RequestBody SignupRequestDto signupRequest) {
        log.info("Signup request received for email: {}", signupRequest.getEmail());
        UserDto userDto = authService.signup(signupRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(userDto);
    }

    @PostMapping("/login")
    public ResponseEntity<JwtDto> login(@Valid @RequestBody LoginRequestDto loginRequest) {
        log.info("Login request received for email: {}", loginRequest.getEmail());
        JwtDto jwtDto = authService.login(loginRequest);
        return ResponseEntity.ok(jwtDto);
    }

    @PostMapping("/logout") // ApiResponse를 사용하지 않는 버전
    public ResponseEntity<Map<String, String>> logout(@RequestHeader("Authorization") String authorizationHeader) {
        if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith("Bearer ")) {
            String accessToken = authorizationHeader.substring(7);
            authService.logout(accessToken);

            Map<String, String> response = new HashMap<>();
            response.put("message", "로그아웃 되었습니다.");
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().build(); // 간단하게 400 Bad Request만 반환
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtDto> refresh(@Valid @RequestBody RefreshTokenRequestDto refreshTokenRequest) {
        log.info("Token refresh request received");
        JwtDto jwtDto = authService.refreshToken(refreshTokenRequest.getRefreshToken());
        return ResponseEntity.ok(jwtDto);
    }
}