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
            @Parameter(description = "대화 스레드 ID (없으면 자동 생성)") String threadId
    ) {
        // 1. Thread-Id 처리 (없으면 생성)
        if (threadId == null || threadId.trim().isEmpty()) {
            threadId = UUID.randomUUID().toString();
            log.debug("새 Thread-Id 생성: {}", threadId);
        }

        // 2. 사용자 정보 설정
        request.setUserId(userDetails.getUsername());
        request.setThreadId(threadId);

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
            log.error("처리 중 오류: userId={}", userDetails.getUsername(), e);
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
        log.info("컨텍스트 초기화: userId={}, threadId={}",
            userDetails.getUsername(), threadId);

        try {
            mainLLMOrchestrator.resetContext(threadId);
            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            log.error("컨텍스트 초기화 실패: threadId={}", threadId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}