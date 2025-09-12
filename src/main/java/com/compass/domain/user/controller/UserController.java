package com.compass.domain.user.controller;

import com.compass.domain.user.dto.UserDto;
import com.compass.domain.user.dto.UserFeedbackDto;
import com.compass.domain.user.dto.UserPreferenceDto;
import com.compass.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    }// 삭제 예정

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인하고, 성공 시 Access Token과 Refresh Token을 발급합니다.")
    public ResponseEntity<UserDto.LoginResponse> login(@Valid @RequestBody UserDto.LoginRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }// 삭제 예정

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
    } // 삭제 예정1

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

    @PatchMapping("/profile")
    @Operation(summary = "내 프로필 수정", description = "현재 로그인된 사용자의 프로필 정보(닉네임, 프로필 이미지)를 수정합니다.")
    public ResponseEntity<UserDto> updateMyProfile(
            Authentication authentication,
            @RequestBody UserDto.ProfileUpdateRequest request) {
        if (authentication == null || !authentication.isAuthenticated()) {
            // This is a safeguard, though the filter chain should prevent this.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserDto updatedUser = userService.updateUserProfileByEmail(authentication.getName(), request);
        return ResponseEntity.ok(updatedUser);
    }

    @PutMapping("/preferences")
    @Operation(summary = "여행 스타일 선호도 수정 및 저장", description = "사용자의 여행 스타일(휴양, 관광, 액티비티 등) 선호도 가중치를 수정합니다.")
    public ResponseEntity<List<UserPreferenceDto.Response>> updateTravelStylePreferences(
            Authentication authentication,
            @Valid @RequestBody UserPreferenceDto.UpdateRequest request) {
        if (authentication == null || !authentication.isAuthenticated()) {
            // 인증 정보가 없으면 401 Unauthorized를 반환하는 방어 코드
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<UserPreferenceDto.Response> responses = userService.updateUserTravelStyle(authentication.getName(), request);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/preferences/budget-level")
    @Operation(summary = "예산 수준 설정", description = "사용자의 여행 예산 수준(BUDGET, STANDARD, LUXURY)을 설정합니다.")
    public ResponseEntity<UserPreferenceDto.Response> updateBudgetLevel(
            Authentication authentication,
            @Valid @RequestBody UserPreferenceDto.BudgetUpdateRequest request) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserPreferenceDto.Response response = userService.updateBudgetLevel(authentication.getName(), request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/preferences/analyze")
    @Operation(summary = "사용자 선호도 분석 실행", description = "사용자의 여행 기록을 바탕으로 여행 스타일을 자동으로 분석하고 저장합니다.")
    public ResponseEntity<?> analyzeMyPreferences(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Optional<List<UserPreferenceDto.Response>> optionalResponse = userService.analyzeAndSavePreferences(userDetails.getUsername());

        return optionalResponse.<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/feedback")
    @Operation(summary = "피드백 제출", description = "서비스에 대한 만족도 및 의견을 제출합니다.")
    public ResponseEntity<UserFeedbackDto.Response> submitFeedback(
            Authentication authentication,
            @Valid @RequestBody UserFeedbackDto.CreateRequest request) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserFeedbackDto.Response response = userService.saveFeedback(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}