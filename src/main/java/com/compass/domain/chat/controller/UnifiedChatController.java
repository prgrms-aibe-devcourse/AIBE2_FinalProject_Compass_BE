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
// jakarta.validation.Valid를 import 리스트에서 제거
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "통합 채팅 API")
public class UnifiedChatController {

    private final MainLLMOrchestrator mainLLMOrchestrator;
    private final com.compass.config.jwt.JwtTokenProvider jwtTokenProvider;

    @PostMapping("/unified")
    @Operation(summary = "통합 채팅 처리", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ChatResponse> processChat(
            // [수정] @Valid 어노테이션 제거
            @RequestBody ChatRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader(value = "Thread-Id", required = false) String threadId,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        if (threadId == null || threadId.trim().isEmpty()) {
            threadId = UUID.randomUUID().toString();
        }

        String userId = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                userId = jwtTokenProvider.getUserId(token);
            } catch (Exception e) {
                log.warn("JWT 파싱 실패: {}", e.getMessage());
            }
        }

        if (userId == null && userDetails != null) {
            // UserDetails에서 userId를 가져오는 로직이 필요하다면 여기에 추가
        }

        if (userId == null) {
            userId = "test-user"; // Fallback
        }

        // [수정] record의 불변성을 위해 setUserId와 setThreadId의 반환값을 다시 할당합니다.
        request = request.setUserId(userId);
        request = request.setThreadId(threadId);

        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ChatResponse.builder().content("메시지를 입력해주세요.").type("ERROR").build());
        }

        try {
            ChatResponse response = mainLLMOrchestrator.processChat(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("처리 중 오류: userId={}", userId, e);
            return ResponseEntity.internalServerError().body(ChatResponse.builder().content("처리 중 오류가 발생했습니다.").type("ERROR").build());
        }
    }

    @DeleteMapping("/unified/context")
    @Operation(summary = "대화 컨텍스트 초기화", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> resetContext(
            @RequestHeader("Thread-Id") String threadId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        try {
            mainLLMOrchestrator.resetContext(threadId, userDetails.getUsername());
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("컨텍스트 초기화 실패: threadId={}, error={}", threadId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/health")
    @Operation(summary = "헬스 체크")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}