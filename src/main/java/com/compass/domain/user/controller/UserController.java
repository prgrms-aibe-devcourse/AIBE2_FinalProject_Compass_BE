package com.compass.domain.user.controller;

import com.compass.domain.user.dto.UserDto;
import com.compass.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<UserDto.SignUpResponse> signUp(@Valid @RequestBody UserDto.SignUpRequest request) {
        return ResponseEntity.ok(userService.signUp(request));
    }

    @PostMapping("/login")
    public ResponseEntity<UserDto.LoginResponse> login(@Valid @RequestBody UserDto.LoginRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }

    @PostMapping("/logout") // ApiResponse를 사용하지 않는 버전
    public ResponseEntity<Map<String, String>> logout(@RequestHeader("Authorization") String authorizationHeader) {
        if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith("Bearer ")) {
            String accessToken = authorizationHeader.substring(7);
            userService.logout(accessToken);

            Map<String, String> response = new HashMap<>();
            response.put("message", "로그아웃 되었습니다.");
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().build(); // 간단하게 400 Bad Request만 반환
    }

}