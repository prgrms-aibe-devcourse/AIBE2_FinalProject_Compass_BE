package com.compass.domain.chat.controller;

import com.compass.domain.chat.model.request.ChatRequest;
import com.compass.domain.chat.model.response.ChatResponse;
import com.compass.domain.chat.orchestrator.MainLLMOrchestrator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

// 통합 채팅 컨트롤러 - 심플하게 요구사항만 구현
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "통합 채팅 API")
public class UnifiedChatController {

    private final MainLLMOrchestrator mainLLMOrchestrator;
    private final com.compass.config.jwt.JwtTokenProvider jwtTokenProvider;

    // 통합 채팅 엔드포인트 - 요구사항대로만
    @PostMapping("/unified")
    @Operation(
        summary = "통합 채팅 처리",
        description = "여행 계획, 정보 수집, 일반 대화 등 모든 채팅을 처리하는 통합 엔드포인트",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ChatResponse> processChat(
            @Valid @RequestBody ChatRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader(value = "Thread-Id", required = false)
            @Parameter(description = "대화 스레드 ID (없으면 자동 생성)") String threadId,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        // 1. Thread-Id 처리 (없으면 생성)
        if (threadId == null || threadId.trim().isEmpty()) {
            threadId = UUID.randomUUID().toString();
            log.debug("새 Thread-Id 생성: {}", threadId);
        }

        // 2. 사용자 정보 설정 - JWT에서 userId 추출
        String userId = null;
        String userEmail = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                userId = jwtTokenProvider.getUserId(token);
                userEmail = jwtTokenProvider.getUsername(token);
                log.debug("JWT에서 추출 - userId: {}, email: {}", userId, userEmail);
            } catch (Exception e) {
                log.warn("JWT 파싱 실패: {}", e.getMessage());
            }
        }

        // Fallback: UserDetails 사용
        if (userId == null && userDetails != null) {
            userEmail = userDetails.getUsername();
            userId = userEmail; // 임시로 이메일을 userId로 사용
        }

        // 테스트 사용자
        if (userId == null) {
            userId = "test-user";
            userEmail = "test-user@test.com";
        }

        request.setUserId(userId);
        request.setThreadId(threadId);

        log.info("채팅 요청 처리 - userId: {}, email: {}, threadId: {}", userId, userEmail, threadId);

        // 3. 요청 검증 (간단하게)
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            log.warn("빈 메시지 요청: userId={}", request.getUserId());
            return ResponseEntity.badRequest()
                .body(ChatResponse.builder()
                    .content("메시지를 입력해주세요.")
                    .type("ERROR")
                    .build());
        }

        // 4. 로깅
        log.info("채팅 요청: userId={}, threadId={}, message={}",
            request.getUserId(),
            request.getThreadId(),
            request.getMessage());

        try {
            // 5. Orchestrator로 전달 (핵심!)
            ChatResponse response = mainLLMOrchestrator.processChat(request);

            // 6. 응답 로깅
            log.info("채팅 응답: threadId={}, type={}",
                request.getThreadId(),
                response.getType());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            // 400 - 잘못된 요청
            log.error("잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ChatResponse.builder()
                    .content("잘못된 요청입니다: " + e.getMessage())
                    .type("ERROR")
                    .build());

        } catch (Exception e) {
            // 500 - 서버 오류
            String username = userDetails != null ? userDetails.getUsername() : "unknown";
            log.error("처리 중 오류: userId={}", username, e);
            return ResponseEntity.internalServerError()
                .body(ChatResponse.builder()
                    .content("처리 중 오류가 발생했습니다.")
                    .type("ERROR")
                    .build());
        }
    }

    // 컨텍스트 초기화 엔드포인트
    @DeleteMapping("/unified/context")
    @Operation(
        summary = "대화 컨텍스트 초기화",
        description = "특정 스레드의 대화 컨텍스트를 초기화",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "초기화 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Void> resetContext(
            @RequestHeader("Thread-Id") String threadId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        // userDetails null 체크 추가
        String username = userDetails != null ? userDetails.getUsername() : "unknown";
        log.info("컨텍스트 초기화 요청: username={}, threadId={}", username, threadId);

        if (userDetails == null) {
            log.warn("UserDetails is null - 인증 정보 없음");
            return ResponseEntity.status(401).build();  // Unauthorized
        }

        try {
            mainLLMOrchestrator.resetContext(threadId, userDetails.getUsername());
            log.info("✅ 컨텍스트 초기화 성공: threadId={}", threadId);
            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            log.error("❌ 컨텍스트 초기화 실패: threadId={}, error={}", threadId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // 헬스 체크 엔드포인트
    @GetMapping("/health")
    @Operation(
        summary = "헬스 체크",
        description = "서비스 상태 확인"
    )
    @ApiResponse(responseCode = "200", description = "서비스 정상")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}