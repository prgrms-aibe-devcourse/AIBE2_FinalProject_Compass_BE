package com.compass.domain.user.controller;

import com.compass.domain.common.dto.MessageResponse;
import com.compass.domain.user.dto.LoginRequest;
import com.compass.domain.user.dto.SignUpRequest;
import com.compass.domain.user.dto.TokenResponse;
import com.compass.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "사용자", description = "사용자 생성, 조회 등 사용자 관련 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "회원가입", description = "새로운 사용자를 생성합니다.")
    @PostMapping("/signup")
    public ResponseEntity<MessageResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        userService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new MessageResponse("회원가입 신청이 완료되었습니다. 이메일을 확인하여 계정을 활성화해주세요."));
    }
    @Operation(summary = "이메일 인증", description = "발급된 토큰으로 이메일 인증을 완료하고 계정을 활성화합니다.")
    @GetMapping("/verify")
    public ResponseEntity<MessageResponse> verifyEmail(@RequestParam String token) {
        userService.verifyEmail(token);
        return ResponseEntity.ok(new MessageResponse("이메일 인증이 완료되었습니다. 이제부터 서비스를 이용할 수 있습니다."));
    }

    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인하고 JWT를 발급받습니다.")
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse tokenResponse = userService.login(request);
        return ResponseEntity.ok(tokenResponse);
    }



}