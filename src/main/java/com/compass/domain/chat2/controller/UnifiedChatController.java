package com.compass.domain.chat2.controller;

import com.compass.domain.chat2.dto.UnifiedChatRequest;
import com.compass.domain.chat2.dto.UnifiedChatResponse;
import com.compass.domain.chat2.orchestrator.MainLLMOrchestrator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * UnifiedChatController - 모든 채팅 요청의 단일 진입점
 *
 * REQ-CHAT2-001: MainLLMOrchestrator를 통한 통합 엔드포인트
 * REQ-CHAT2-002: 모든 도메인 요청 처리
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "Unified Chat", description = "통합 채팅 API")
public class UnifiedChatController {

    private final MainLLMOrchestrator mainLLMOrchestrator;

    /**
     * 통합 채팅 엔드포인트 - 모든 사용자 요청의 진입점
     *
     * @param request 사용자 요청
     * @param userDetails 인증된 사용자 정보
     * @return 처리된 응답
     */
    @PostMapping("/unified")
    @Operation(summary = "통합 채팅 요청", description = "모든 채팅 요청을 처리하는 단일 엔드포인트")
    public ResponseEntity<UnifiedChatResponse> handleUnifiedChat(
            @Valid @RequestBody UnifiedChatRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        String userId = userDetails != null ? userDetails.getUsername() : "anonymous";
        String threadId = request.getThreadId() != null ? request.getThreadId() : generateThreadId();

        log.info("📨 통합 채팅 요청 수신 - ThreadId: {}, UserId: {}", threadId, userId);

        try {
            // MainLLMOrchestrator를 통한 요청 처리
            String response = mainLLMOrchestrator.orchestrate(
                request.getMessage(),
                threadId,
                userId
            );

            // 응답 생성
            UnifiedChatResponse chatResponse = UnifiedChatResponse.builder()
                .threadId(threadId)
                .message(response)
                .timestamp(LocalDateTime.now())
                .status("SUCCESS")
                .build();

            log.info("✅ 통합 채팅 응답 완료 - ThreadId: {}", threadId);
            return ResponseEntity.ok(chatResponse);

        } catch (Exception e) {
            log.error("❌ 통합 채팅 처리 실패 - ThreadId: {}", threadId, e);

            UnifiedChatResponse errorResponse = UnifiedChatResponse.builder()
                .threadId(threadId)
                .message("요청을 처리하는 중 오류가 발생했습니다.")
                .timestamp(LocalDateTime.now())
                .status("ERROR")
                .errorCode("CHAT2_001")
                .build();

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 사용 가능한 Function 목록 조회
     */
    @GetMapping("/functions")
    @Operation(summary = "사용 가능한 Function 목록", description = "현재 시스템에서 사용 가능한 Function 목록 조회")
    public ResponseEntity<?> getAvailableFunctions() {
        return ResponseEntity.ok(mainLLMOrchestrator.getAvailableFunctions());
    }

    /**
     * 시스템 상태 확인 (Health Check)
     */
    @GetMapping("/health")
    @Operation(summary = "시스템 상태 확인", description = "MainLLMOrchestrator 및 관련 서비스 상태 확인")
    public ResponseEntity<?> healthCheck() {
        mainLLMOrchestrator.logFunctionStatus();

        return ResponseEntity.ok(Map.of(
            "status", "healthy",
            "timestamp", LocalDateTime.now(),
            "orchestrator", "active",
            "functions", mainLLMOrchestrator.getAvailableFunctions().size()
        ));
    }

    /**
     * Thread ID 생성
     */
    private String generateThreadId() {
        return "thread-" + UUID.randomUUID().toString();
    }
}