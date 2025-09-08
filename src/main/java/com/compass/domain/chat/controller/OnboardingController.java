package com.compass.domain.chat.controller;

import com.compass.domain.chat.dto.OnboardingResponse;
import com.compass.domain.chat.dto.UserPreferenceDto;
import com.compass.domain.chat.service.UserOnboardingService;
import com.compass.config.jwt.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 신규 사용자 온보딩 컨트롤러
 * REQ-PERS-007: 콜드 스타트 해결을 위한 온보딩 엔드포인트
 */
@Slf4j
@RestController
@RequestMapping("/api/chat/onboarding")
@RequiredArgsConstructor
@Tag(name = "Onboarding", description = "신규 사용자 온보딩 API")
public class OnboardingController {
    
    private final UserOnboardingService onboardingService;
    private final JwtTokenProvider jwtTokenProvider;
    
    /**
     * 신규 사용자 확인
     */
    @GetMapping("/check")
    @Operation(summary = "신규 사용자 확인", description = "현재 사용자가 신규 사용자인지 확인합니다.")
    public ResponseEntity<Boolean> checkNewUser(HttpServletRequest request) {
        try {
            Long userId = getUserIdFromToken(request);
            boolean isNewUser = onboardingService.isNewUser(userId);
            
            log.info("Checked new user status for userId: {}, isNewUser: {}", userId, isNewUser);
            return ResponseEntity.ok(isNewUser);
            
        } catch (Exception e) {
            log.error("Error checking new user status: ", e);
            return ResponseEntity.badRequest().body(false);
        }
    }
    
    /**
     * 온보딩 정보 조회
     */
    @GetMapping
    @Operation(summary = "온보딩 정보 조회", description = "신규 사용자를 위한 온보딩 정보를 반환합니다.")
    public ResponseEntity<OnboardingResponse> getOnboardingInfo(HttpServletRequest request) {
        try {
            Long userId = getUserIdFromToken(request);
            OnboardingResponse response = onboardingService.createOnboardingResponse(userId);
            
            log.info("Generated onboarding response for userId: {}", userId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error generating onboarding response: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 환영 메시지 조회
     */
    @GetMapping("/welcome")
    @Operation(summary = "환영 메시지 조회", description = "사용자 이름을 포함한 환영 메시지를 반환합니다.")
    public ResponseEntity<String> getWelcomeMessage(
            @RequestParam(required = false) String userName) {
        try {
            String welcomeMessage = onboardingService.generateWelcomeMessage(userName);
            return ResponseEntity.ok(welcomeMessage);
            
        } catch (Exception e) {
            log.error("Error generating welcome message: ", e);
            return ResponseEntity.internalServerError().body("환영 메시지 생성 중 오류가 발생했습니다.");
        }
    }
    
    /**
     * 선호도 질문 조회
     */
    @GetMapping("/questions")
    @Operation(summary = "선호도 질문 조회", description = "사용자 선호도 수집을 위한 질문 목록을 반환합니다.")
    public ResponseEntity<?> getPreferenceQuestions() {
        try {
            return ResponseEntity.ok(onboardingService.generatePreferenceQuestions());
        } catch (Exception e) {
            log.error("Error generating preference questions: ", e);
            return ResponseEntity.internalServerError().body("질문 생성 중 오류가 발생했습니다.");
        }
    }
    
    /**
     * 예시 질문 조회
     */
    @GetMapping("/examples")
    @Operation(summary = "예시 질문 조회", description = "바로 시작할 수 있는 예시 질문들을 반환합니다.")
    public ResponseEntity<?> getExampleQuestions() {
        try {
            return ResponseEntity.ok(onboardingService.generateExampleQuestions());
        } catch (Exception e) {
            log.error("Error generating example questions: ", e);
            return ResponseEntity.internalServerError().body("예시 질문 생성 중 오류가 발생했습니다.");
        }
    }
    
    /**
     * 사용자 선호도 저장
     */
    @PostMapping("/preferences")
    @Operation(summary = "사용자 선호도 저장", description = "수집된 사용자 선호도 정보를 저장합니다.")
    public ResponseEntity<String> saveUserPreferences(
            HttpServletRequest request,
            @RequestBody UserPreferenceDto preferences) {
        try {
            Long userId = getUserIdFromToken(request);
            onboardingService.saveUserPreferences(userId, preferences);
            
            log.info("Saved preferences for userId: {}", userId);
            return ResponseEntity.ok("선호도가 성공적으로 저장되었습니다.");
            
        } catch (Exception e) {
            log.error("Error saving user preferences: ", e);
            return ResponseEntity.internalServerError().body("선호도 저장 중 오류가 발생했습니다.");
        }
    }
    
    /**
     * JWT 토큰에서 사용자 ID 추출
     */
    private Long getUserIdFromToken(HttpServletRequest request) {
        String token = jwtTokenProvider.resolveToken(request);
        if (token == null || !jwtTokenProvider.validateAccessToken(token)) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }
        // JWT에서 username을 가져온 후, UserRepository를 통해 userId를 조회해야 함
        String username = jwtTokenProvider.getUsername(token);
        // 임시로 username을 Long으로 변환 (실제로는 UserRepository 사용 필요)
        // TODO: UserRepository를 주입받아서 username으로 User 엔티티를 조회하고 ID를 반환
        return 1L; // 임시 반환값
    }
}