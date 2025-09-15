package com.compass.domain.chat.controller;

import com.compass.domain.chat.dto.FollowUpQuestionDto;
import com.compass.domain.chat.dto.TravelInfoStatusDto;
import com.compass.domain.chat.dto.TripPlanningRequest;
import com.compass.domain.chat.dto.ValidationResult;
import com.compass.domain.chat.service.TravelInfoCollectionService;
import com.compass.domain.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 여행 정보 수집 컨트롤러
 * REQ-FOLLOW-002: 체계적인 여행 정보 수집을 위한 REST API
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "Travel Info Collection", description = "여행 정보 수집 API")
public class TravelInfoCollectionController {
    
    private final TravelInfoCollectionService collectionService;
    private final UserRepository userRepository;
    
    /**
     * 정보 수집 시작
     */
    @PostMapping("/collect-info")
    @Operation(summary = "여행 정보 수집 시작", description = "새로운 여행 정보 수집 세션을 시작합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "수집 시작 성공",
                content = @Content(schema = @Schema(implementation = FollowUpQuestionDto.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<FollowUpQuestionDto> startCollection(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid CollectionStartRequest request) {
        
        log.info("Starting info collection for user: {}", userDetails.getUsername());
        
        Long userId = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다"))
                .getId();
        
        FollowUpQuestionDto firstQuestion = collectionService.startInfoCollection(
                userId,
                request.getChatThreadId(),
                request.getInitialMessage()
        );
        
        return ResponseEntity.ok(firstQuestion);
    }
    
    /**
     * 후속 응답 처리
     */
    @PostMapping("/follow-up")
    @Operation(summary = "후속 응답 처리", description = "사용자의 후속 응답을 처리하고 다음 질문을 반환합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "응답 처리 성공",
                content = @Content(schema = @Schema(implementation = FollowUpQuestionDto.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음")
    })
    public ResponseEntity<FollowUpQuestionDto> processFollowUp(
            @RequestBody @Valid FollowUpResponseRequest request) {
        
        log.info("Processing follow-up for session: {}", request.getSessionId());
        
        FollowUpQuestionDto nextQuestion = collectionService.processFollowUpResponse(
                request.getSessionId(),
                request.getUserResponse()
        );
        
        return ResponseEntity.ok(nextQuestion);
    }
    
    /**
     * 수집 상태 조회
     */
    @GetMapping("/collection-status/{sessionId}")
    @Operation(summary = "수집 상태 조회", description = "현재 정보 수집 상태를 조회합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공",
                content = @Content(schema = @Schema(implementation = TravelInfoStatusDto.class))),
        @ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음")
    })
    public ResponseEntity<TravelInfoStatusDto> getCollectionStatus(
            @PathVariable String sessionId) {
        
        log.info("Getting collection status for session: {}", sessionId);
        
        TravelInfoStatusDto status = collectionService.getCollectionStatus(sessionId);
        return ResponseEntity.ok(status);
    }
    
    /**
     * 현재 사용자의 수집 상태 조회
     */
    @GetMapping("/my-collection-status")
    @Operation(summary = "내 수집 상태 조회", description = "현재 로그인한 사용자의 진행 중인 수집 상태를 조회합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공",
                content = @Content(schema = @Schema(implementation = TravelInfoStatusDto.class))),
        @ApiResponse(responseCode = "204", description = "진행 중인 수집이 없음"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<TravelInfoStatusDto> getMyCollectionStatus(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다"))
                .getId();
        
        return collectionService.getCurrentUserStatus(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
    
    /**
     * 특정 정보 업데이트
     */
    @PatchMapping("/collection-status/{sessionId}")
    @Operation(summary = "특정 정보 업데이트", description = "수집 중인 특정 정보를 직접 업데이트합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "업데이트 성공",
                content = @Content(schema = @Schema(implementation = TravelInfoStatusDto.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음")
    })
    public ResponseEntity<TravelInfoStatusDto> updateSpecificInfo(
            @PathVariable String sessionId,
            @RequestBody @Valid UpdateInfoRequest request) {
        
        log.info("Updating info for session: {}, field: {}", sessionId, request.getFieldName());
        
        TravelInfoStatusDto updatedStatus = collectionService.updateSpecificInfo(
                sessionId,
                request.getFieldName(),
                request.getValue()
        );
        
        return ResponseEntity.ok(updatedStatus);
    }
    
    /**
     * 수집 완료
     */
    @PostMapping("/complete-collection/{sessionId}")
    @Operation(summary = "정보 수집 완료", description = "모든 정보 수집을 완료하고 여행 계획 요청을 생성합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "수집 완료 성공",
                content = @Content(schema = @Schema(implementation = TripPlanningRequest.class))),
        @ApiResponse(responseCode = "400", description = "필수 정보가 부족함"),
        @ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음")
    })
    public ResponseEntity<TripPlanningRequest> completeCollection(
            @PathVariable String sessionId) {
        
        log.info("Completing collection for session: {}", sessionId);
        
        TripPlanningRequest tripRequest = collectionService.completeCollection(sessionId);
        return ResponseEntity.ok(tripRequest);
    }
    
    /**
     * 수집 취소
     */
    @DeleteMapping("/collection/{sessionId}")
    @Operation(summary = "정보 수집 취소", description = "진행 중인 정보 수집을 취소합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "취소 성공"),
        @ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음")
    })
    public ResponseEntity<Void> cancelCollection(
            @PathVariable String sessionId) {
        
        log.info("Cancelling collection for session: {}", sessionId);
        
        collectionService.cancelCollection(sessionId);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * 현재 수집 상태 검증
     * REQ-FOLLOW-005: 실시간 검증 API
     */
    @GetMapping("/validate-collection/{sessionId}")
    @Operation(summary = "수집 상태 검증", description = "현재까지 수집된 정보의 유효성을 검증합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "검증 결과 반환",
                content = @Content(schema = @Schema(implementation = ValidationResult.class))),
        @ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음")
    })
    public ResponseEntity<ValidationResult> validateCollection(
            @PathVariable String sessionId) {
        
        log.info("Validating collection for session: {}", sessionId);
        
        ValidationResult validationResult = collectionService.validateCurrentState(sessionId);
        return ResponseEntity.ok(validationResult);
    }
    
    /**
     * 여행 계획 데이터를 JSON으로 저장
     */
    @PostMapping("/save-travel-plan/{threadId}")
    @Operation(summary = "여행 계획 데이터 저장", description = "수집된 여행 정보를 JSON으로 변환하여 채팅 스레드에 저장합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "저장 성공",
                content = @Content(schema = @Schema(implementation = Map.class))),
        @ApiResponse(responseCode = "400", description = "활성화된 수집 세션이 없음"),
        @ApiResponse(responseCode = "404", description = "채팅 스레드를 찾을 수 없음")
    })
    public ResponseEntity<Map<String, Object>> saveTravelPlanAsJson(
            @PathVariable String threadId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("Saving travel plan as JSON for thread: {}", threadId);
        
        Long userId = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다"))
                .getId();
        
        String jsonData = collectionService.saveTravelPlanDataAsJson(threadId, userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "여행 계획 데이터가 성공적으로 저장되었습니다.");
        response.put("threadId", threadId);
        response.put("dataSize", jsonData.length());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 저장된 여행 계획 데이터 조회
     */
    @GetMapping("/travel-plan/{threadId}")
    @Operation(summary = "여행 계획 데이터 조회", description = "채팅 스레드에 저장된 여행 계획 JSON 데이터를 조회합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공",
                content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "204", description = "저장된 데이터가 없음"),
        @ApiResponse(responseCode = "404", description = "채팅 스레드를 찾을 수 없음")
    })
    public ResponseEntity<String> getTravelPlanData(@PathVariable String threadId) {
        
        log.info("Getting travel plan data for thread: {}", threadId);
        
        String jsonData = collectionService.getTravelPlanDataJson(threadId);
        
        if (jsonData == null || jsonData.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        
        return ResponseEntity.ok(jsonData);
    }
    
    // === Request/Response DTOs ===
    
    /**
     * 수집 시작 요청 DTO
     */
    @Schema(description = "정보 수집 시작 요청")
    public static class CollectionStartRequest {
        @Schema(description = "채팅 스레드 ID (선택사항)")
        private String chatThreadId;
        
        @Schema(description = "초기 사용자 메시지 (선택사항)", 
                example = "제주도 2박3일 여행 계획 짜줘")
        private String initialMessage;
        
        public String getChatThreadId() {
            return chatThreadId;
        }
        
        public void setChatThreadId(String chatThreadId) {
            this.chatThreadId = chatThreadId;
        }
        
        public String getInitialMessage() {
            return initialMessage;
        }
        
        public void setInitialMessage(String initialMessage) {
            this.initialMessage = initialMessage;
        }
    }
    
    /**
     * 후속 응답 요청 DTO
     */
    @Schema(description = "후속 응답 요청")
    public static class FollowUpResponseRequest {
        @Schema(description = "세션 ID", required = true, example = "TIC_A1B2C3D4")
        private String sessionId;
        
        @Schema(description = "사용자 응답", required = true, example = "제주도")
        private String userResponse;
        
        public String getSessionId() {
            return sessionId;
        }
        
        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }
        
        public String getUserResponse() {
            return userResponse;
        }
        
        public void setUserResponse(String userResponse) {
            this.userResponse = userResponse;
        }
    }
    
    /**
     * 정보 업데이트 요청 DTO
     */
    @Schema(description = "특정 정보 업데이트 요청")
    public static class UpdateInfoRequest {
        @Schema(description = "필드 이름", required = true, 
                example = "destination", 
                allowableValues = {"destination", "startDate", "endDate", "duration", "companions", "budget"})
        private String fieldName;
        
        @Schema(description = "필드 값", required = true)
        private Object value;
        
        public String getFieldName() {
            return fieldName;
        }
        
        public void setFieldName(String fieldName) {
            this.fieldName = fieldName;
        }
        
        public Object getValue() {
            return value;
        }
        
        public void setValue(Object value) {
            this.value = value;
        }
    }
    
    /**
     * 예외 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        Map<String, String> error = new HashMap<>();
        error.put("error", e.getMessage());
        return ResponseEntity.badRequest().body(error);
    }
    
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException e) {
        Map<String, String> error = new HashMap<>();
        error.put("error", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
}