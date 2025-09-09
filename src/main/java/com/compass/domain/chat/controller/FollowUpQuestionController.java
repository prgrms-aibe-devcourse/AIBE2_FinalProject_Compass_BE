package com.compass.domain.chat.controller;

import com.compass.domain.chat.dto.FollowUpResponseDto;
import com.compass.domain.chat.dto.ValidationResult;
import com.compass.domain.chat.service.FollowUpQuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 꼬리질문 플로우 컨트롤러
 * REQ-FOLLOW-001 ~ REQ-FOLLOW-006 API 엔드포인트
 */
@Slf4j
@RestController
@RequestMapping("/api/chat/follow-up")
@RequiredArgsConstructor
@Tag(name = "Follow-up Questions", description = "Travel information collection through follow-up questions")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class FollowUpQuestionController {
    
    private final FollowUpQuestionService followUpService;
    
    /**
     * 꼬리질문 세션 시작
     * REQ-FOLLOW-001: 질문 플로우 엔진 시작
     */
    @PostMapping("/start")
    @Operation(summary = "Start follow-up question session", 
               description = "Initialize a new follow-up question session for travel planning")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Session started successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<FollowUpResponseDto> startSession(
            @Parameter(description = "Initial message and user context")
            @RequestBody Map<String, Object> request) {
        
        String userId = (String) request.get("userId");
        String initialMessage = (String) request.get("message");
        
        log.info("Starting follow-up session for user: {} with message: {}", userId, initialMessage);
        
        try {
            FollowUpResponseDto response = followUpService.startSession(userId, initialMessage);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error starting follow-up session", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(FollowUpResponseDto.builder()
                            .message("세션 시작 중 오류가 발생했습니다.")
                            .build());
        }
    }
    
    /**
     * 사용자 응답 처리
     * REQ-FOLLOW-002: 정보 수집
     * REQ-FOLLOW-003: LLM 파싱
     * REQ-FOLLOW-006: 재질문 처리
     */
    @PostMapping("/respond")
    @Operation(summary = "Process user response", 
               description = "Process user's answer and generate next question or complete the flow")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Response processed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "404", description = "Session not found")
    })
    public ResponseEntity<FollowUpResponseDto> processResponse(
            @Parameter(description = "Session ID and user response")
            @RequestBody Map<String, String> request) {
        
        String sessionId = request.get("sessionId");
        String userResponse = request.get("response");
        
        if (sessionId == null || sessionId.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(FollowUpResponseDto.builder()
                            .message("세션 ID가 필요합니다.")
                            .build());
        }
        
        if (userResponse == null || userResponse.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(FollowUpResponseDto.builder()
                            .sessionId(sessionId)
                            .message("응답을 입력해주세요.")
                            .build());
        }
        
        log.info("Processing response for session: {} - Response: {}", sessionId, userResponse);
        
        try {
            FollowUpResponseDto response = followUpService.processUserResponse(sessionId, userResponse);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing user response", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(FollowUpResponseDto.builder()
                            .sessionId(sessionId)
                            .message("응답 처리 중 오류가 발생했습니다.")
                            .build());
        }
    }
    
    /**
     * 세션 상태 조회
     * REQ-FOLLOW-004: Redis 세션 조회
     */
    @GetMapping("/status/{sessionId}")
    @Operation(summary = "Get session status", 
               description = "Retrieve current status and collected information of a session")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Status retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Session not found or expired")
    })
    public ResponseEntity<FollowUpResponseDto> getSessionStatus(
            @Parameter(description = "Session ID")
            @PathVariable String sessionId) {
        
        log.info("Getting status for session: {}", sessionId);
        
        try {
            FollowUpResponseDto response = followUpService.getSessionStatus(sessionId);
            
            if (response.isExpired()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting session status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(FollowUpResponseDto.builder()
                            .sessionId(sessionId)
                            .message("상태 조회 중 오류가 발생했습니다.")
                            .build());
        }
    }
    
    /**
     * 세션 검증
     * REQ-FOLLOW-005: 완성도 검증
     */
    @GetMapping("/validate/{sessionId}")
    @Operation(summary = "Validate session information", 
               description = "Check if collected information is complete and valid")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Validation result returned"),
        @ApiResponse(responseCode = "404", description = "Session not found")
    })
    public ResponseEntity<ValidationResult> validateSession(
            @Parameter(description = "Session ID")
            @PathVariable String sessionId) {
        
        log.info("Validating session: {}", sessionId);
        
        try {
            ValidationResult result = followUpService.validateSession(sessionId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error validating session", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ValidationResult.builder()
                            .valid(false)
                            .userFriendlyMessage("검증 중 오류가 발생했습니다.")
                            .build());
        }
    }
    
    /**
     * 건강 체크 엔드포인트
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if follow-up service is running")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "OK",
            "service", "FollowUpQuestionService",
            "version", "1.0.0"
        ));
    }
}