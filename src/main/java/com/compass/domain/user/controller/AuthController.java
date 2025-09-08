package com.compass.domain.user.controller;

import com.compass.domain.user.dto.JwtDto;
import com.compass.domain.user.dto.LoginRequestDto;
import com.compass.domain.user.dto.SignupRequestDto;
import com.compass.domain.user.dto.UserDto;
import com.compass.domain.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/refresh")
    public ResponseEntity<JwtDto> refresh(@RequestHeader("Authorization") String refreshToken) {
        log.info("Token refresh request received");
        // Remove "Bearer " prefix if present
        String token = refreshToken.startsWith("Bearer ") ? refreshToken.substring(7) : refreshToken;
        JwtDto jwtDto = authService.refreshToken(token);
        return ResponseEntity.ok(jwtDto);
    }
}