package com.compass.domain.chat2.controller;

import com.compass.domain.chat2.orchestrator.MainLLMOrchestrator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * UnifiedChatController - 통합 채팅 API
 * REQ-CHAT2-008: 전체 시스템 통합 테스트
 * 
 * 모든 도메인의 Function을 통합하여 처리하는 단일 엔드포인트
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Unified Chat", description = "통합 채팅 API")
public class UnifiedChatController {
    
    private final MainLLMOrchestrator mainLLMOrchestrator;
    
    /**
     * 통합 채팅 API - 동기 처리
     * 
     * @param request 채팅 요청
     * @return 채팅 응답
     */
    @PostMapping("/unified")
    @Operation(summary = "통합 채팅", description = "모든 도메인의 Function을 통합하여 처리")
    public ResponseEntity<Map<String, Object>> unifiedChat(
            @Parameter(description = "채팅 요청", required = true)
            @RequestBody ChatRequest request) {
        
        log.info("통합 채팅 API 호출: threadId={}, input={}", 
                request.getThreadId(), request.getUserInput());
        
        try {
            // MainLLMOrchestrator를 통해 요청 처리
            String response = mainLLMOrchestrator.orchestrate(
                    request.getUserInput(), 
                    request.getThreadId()
            );
            
            // 응답 생성
            Map<String, Object> responseBody = Map.of(
                    "success", true,
                    "threadId", request.getThreadId(),
                    "response", response,
                    "timestamp", System.currentTimeMillis()
            );
            
            log.info("통합 채팅 API 완료: threadId={}", request.getThreadId());
            return ResponseEntity.ok(responseBody);
            
        } catch (Exception e) {
            log.error("통합 채팅 API 오류: threadId={}, error={}", 
                    request.getThreadId(), e.getMessage(), e);
            
            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "threadId", request.getThreadId(),
                    "error", "처리 중 오류가 발생했습니다.",
                    "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 통합 채팅 API - 비동기 처리
     * 
     * @param request 채팅 요청
     * @return 비동기 채팅 응답
     */
    @PostMapping("/unified/async")
    @Operation(summary = "비동기 통합 채팅", description = "비동기로 처리하는 통합 채팅")
    public ResponseEntity<Map<String, Object>> unifiedChatAsync(
            @Parameter(description = "채팅 요청", required = true)
            @RequestBody ChatRequest request) {
        
        log.info("비동기 통합 채팅 API 호출: threadId={}, input={}", 
                request.getThreadId(), request.getUserInput());
        
        try {
            // 비동기 처리 시작
            CompletableFuture<String> future = mainLLMOrchestrator.orchestrateAsync(
                    request.getUserInput(), 
                    request.getThreadId()
            );
            
            // 즉시 응답 반환 (처리 중 상태)
            Map<String, Object> responseBody = Map.of(
                    "success", true,
                    "threadId", request.getThreadId(),
                    "status", "processing",
                    "message", "요청이 처리 중입니다.",
                    "timestamp", System.currentTimeMillis()
            );
            
            log.info("비동기 통합 채팅 API 시작: threadId={}", request.getThreadId());
            return ResponseEntity.ok(responseBody);
            
        } catch (Exception e) {
            log.error("비동기 통합 채팅 API 오류: threadId={}, error={}", 
                    request.getThreadId(), e.getMessage(), e);
            
            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "threadId", request.getThreadId(),
                    "error", "비동기 처리 중 오류가 발생했습니다.",
                    "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 채팅 요청 DTO
     */
    public static class ChatRequest {
        private String userInput;
        private String threadId;
        
        public ChatRequest() {}
        
        public ChatRequest(String userInput, String threadId) {
            this.userInput = userInput;
            this.threadId = threadId;
        }
        
        public String getUserInput() {
            return userInput;
        }
        
        public void setUserInput(String userInput) {
            this.userInput = userInput;
        }
        
        public String getThreadId() {
            return threadId;
        }
        
        public void setThreadId(String threadId) {
            this.threadId = threadId;
        }
    }
}
