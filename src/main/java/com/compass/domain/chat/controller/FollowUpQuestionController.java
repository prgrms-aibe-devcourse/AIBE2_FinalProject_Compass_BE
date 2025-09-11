package com.compass.domain.chat.controller;

import com.compass.domain.chat.dto.FollowUpResponseDto;
import com.compass.domain.chat.dto.ValidationResult;
import com.compass.domain.chat.service.FollowUpQuestionService;
import com.compass.domain.chat.exception.FollowUpException;
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
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "409", description = "Duplicate request detected")
    })
    public ResponseEntity<FollowUpResponseDto> startSession(
            @RequestHeader(value = "X-User-ID", required = false) String userId,
            @Parameter(description = "Initial message and user context")
            @RequestBody Map<String, Object> request) {
        
        // Request body에서 userId가 없으면 헤더에서 사용
        if (userId == null) {
            Object userIdObj = request.get("userId");
            if (userIdObj != null) {
                userId = String.valueOf(userIdObj);
            }
        }
        String initialMessage = (String) request.get("message");
        String clientRequestId = (String) request.get("requestId"); // 클라이언트가 제공하는 요청 ID (중복 방지용)
        
        // 중복 요청 체크를 위한 키 생성
        String dedupeKey = userId + ":" + (clientRequestId != null ? clientRequestId : initialMessage);
        
        log.info("Starting follow-up session for user: {} with message: {} (requestId: {}, dedupeKey: {})", 
                userId, initialMessage, clientRequestId, dedupeKey);
        
        try {
            // ThreadId가 제공된 경우, 기존 세션 재사용 여부 확인
            String threadId = (String) request.get("threadId");
            
            // 중복 요청 방지를 위한 체크 (같은 메시지로 5초 이내 재요청 차단)
            if (followUpService.isDuplicateRequest(dedupeKey)) {
                log.warn("Duplicate request detected for key: {}", dedupeKey);
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(FollowUpResponseDto.builder()
                                .message("중복 요청이 감지되었습니다. 잠시 후 다시 시도해주세요.")
                                .build());
            }
            
            FollowUpResponseDto response = followUpService.startSession(userId, initialMessage, threadId);
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
     * 사용자 응답 처리 (프론트엔드 호환 엔드포인트)
     * 캘린더 UI와 기타 프론트엔드 컴포넌트에서 사용
     */
    @PostMapping("/answer")
    @Operation(summary = "Process user answer (Frontend compatible)", 
               description = "Process user's answer from frontend components like calendar UI")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Answer processed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "404", description = "Session not found")
    })
    public ResponseEntity<FollowUpResponseDto> processAnswer(
            @Parameter(description = "Session ID, answer and optional metadata")
            @RequestBody Map<String, Object> request) {
        
        String sessionId = (String) request.get("sessionId");
        String answer = (String) request.get("answer");
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) request.get("metadata");
        
        if (sessionId == null || sessionId.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(FollowUpResponseDto.builder()
                            .message("세션 ID가 필요합니다.")
                            .build());
        }
        
        // 메타데이터 처리 (날짜 정보 등)
        if (metadata != null) {
            log.info("Processing answer with metadata for session: {} - Answer: {}, Metadata: {}", 
                    sessionId, answer, metadata);
            
            // 날짜 정보가 메타데이터에 있는 경우 처리
            if (metadata.containsKey("startDate") && metadata.containsKey("endDate")) {
                String dateRange = metadata.get("startDate") + " ~ " + metadata.get("endDate");
                if (answer == null || answer.isEmpty()) {
                    answer = dateRange;
                }
            }
            
            // 스킵 요청 처리
            if (Boolean.TRUE.equals(metadata.get("skipped"))) {
                answer = "나중에 결정할게요";
            }
        }
        
        if (answer == null || answer.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(FollowUpResponseDto.builder()
                            .sessionId(sessionId)
                            .message("응답을 입력해주세요.")
                            .build());
        }
        
        log.info("Processing answer for session: {} - Answer: {}", sessionId, answer);
        
        try {
            FollowUpResponseDto response = followUpService.processUserResponse(sessionId, answer);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing user answer", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(FollowUpResponseDto.builder()
                            .sessionId(sessionId)
                            .message("응답 처리 중 오류가 발생했습니다.")
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