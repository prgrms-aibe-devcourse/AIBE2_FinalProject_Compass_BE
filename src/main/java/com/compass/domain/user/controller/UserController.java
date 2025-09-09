package com.compass.domain.user.controller;

import com.compass.domain.user.dto.UserDto;
import com.compass.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "사용자", description = "사용자 인증 및 프로필 관리 API")
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "이메일, 비밀번호, 닉네임을 사용하여 새로운 사용자를 등록합니다.")
    public ResponseEntity<UserDto.SignUpResponse> signUp(@Valid @RequestBody UserDto.SignUpRequest request) {
        return ResponseEntity.ok(userService.signUp(request));
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인하고, 성공 시 Access Token과 Refresh Token을 발급합니다.")
    public ResponseEntity<UserDto.LoginResponse> login(@Valid @RequestBody UserDto.LoginRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }

    @PostMapping("/logout") // ApiResponse를 사용하지 않는 버전
    @Operation(summary = "로그아웃", description = "현재 로그인된 사용자를 로그아웃 처리하고, Access Token을 무효화합니다.")
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

    @GetMapping("/profile")
    @Operation(summary = "내 프로필 조회", description = "현재 로그인된 사용자의 프로필 정보를 조회합니다.")
    public ResponseEntity<UserDto> getMyProfile(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            // Spring Security 필터 체인에서 처리되지만, 안전장치로 추가합니다.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        log.info("Fetching profile for user: {}", authentication.getName());
        UserDto userDto = userService.getUserProfileByEmail(authentication.getName());
        return ResponseEntity.ok(userDto);
    }

}