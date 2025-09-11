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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;

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
            @RequestBody Map<String, Object> request) {
        
        String sessionId = String.valueOf(request.get("sessionId"));
        String userResponse = String.valueOf(request.get("response"));
        // userId 파라미터도 받을 수 있도록 처리 (선택적)
        Object userIdObj = request.get("userId");
        String userId = userIdObj != null ? String.valueOf(userIdObj) : null;
        
        if (sessionId == null || sessionId.isEmpty() || "null".equals(sessionId)) {
            return ResponseEntity.badRequest()
                    .body(FollowUpResponseDto.builder()
                            .message("세션 ID가 필요합니다.")
                            .build());
        }
        
        if (userResponse == null || userResponse.isEmpty() || "null".equals(userResponse)) {
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
    
    @Autowired
    private com.compass.domain.trip.service.TripService tripService;
    
    @Autowired
    private com.compass.domain.user.repository.UserRepository userRepository;
    
    @Autowired(required = false)
    private com.compass.domain.chat.service.PromptEngineeringService promptEngineeringService;
    
    /**
     * 여행 계획 JSON 저장
     * 수집된 여행 정보를 ChatThread의 travelPlanData 필드에 JSON으로 저장
     */
    @PostMapping("/save-travel-plan/{threadId}")
    @Operation(summary = "Save travel plan as JSON", 
               description = "Save collected travel information as JSON in ChatThread")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Travel plan JSON saved successfully"),
        @ApiResponse(responseCode = "404", description = "Thread or session not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> saveTravelPlanAsJson(
            @PathVariable String threadId,
            @RequestBody Map<String, Object> request) {
        
        String sessionId = (String) request.get("sessionId");
        
        log.info("Saving travel plan as JSON for thread: {}, session: {}", threadId, sessionId);
        
        try {
            // 세션에서 수집된 정보 가져오기
            FollowUpResponseDto sessionInfo = followUpService.getSessionStatus(sessionId);
            
            if (sessionInfo.isExpired()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "세션이 만료되었습니다."));
            }
            
            // 수집된 정보를 JSON으로 저장
            Map<String, Object> travelData = new HashMap<>();
            travelData.put("sessionId", sessionId);
            travelData.put("collectedInfo", sessionInfo.getCollectedInfo());
            travelData.put("canGeneratePlan", sessionInfo.isCanGeneratePlan());
            travelData.put("progressPercentage", sessionInfo.getProgressPercentage());
            travelData.put("savedAt", java.time.LocalDateTime.now().toString());
            
            // ChatThread에 JSON 저장
            boolean saved = followUpService.saveTravelPlanJson(threadId, travelData);
            
            if (saved) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "여행 계획 정보가 JSON으로 저장되었습니다.");
                response.put("threadId", threadId);
                response.put("sessionId", sessionId);
                
                log.info("Travel plan JSON saved successfully for thread: {}", threadId);
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Thread를 찾을 수 없습니다."));
            }
            
        } catch (Exception e) {
            log.error("Error saving travel plan JSON", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "여행 계획 JSON 저장 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 여행 계획 생성 및 DB 저장
     * 수집된 정보를 기반으로 여행 계획을 생성하고 저장
     */
    @PostMapping("/generate-plan")
    @Operation(summary = "Generate travel plan", 
               description = "Generate and save travel plan based on collected information")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Travel plan generated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or incomplete information"),
        @ApiResponse(responseCode = "404", description = "Session not found")
    })
    public ResponseEntity<Map<String, Object>> generateTravelPlan(
            @RequestBody Map<String, Object> request) {
        
        String sessionId = (String) request.get("sessionId");
        String userId = (String) request.get("userId");
        String threadId = (String) request.get("threadId");
        @SuppressWarnings("unchecked")
        java.util.List<String> travelStyles = (java.util.List<String>) request.get("travelStyles");
        
        log.info("Generating travel plan for session: {}, user: {}, styles: {}", 
                sessionId, userId, travelStyles);
        
        try {
            // 세션에서 수집된 정보 가져오기
            FollowUpResponseDto sessionInfo = followUpService.getSessionStatus(sessionId);
            
            if (sessionInfo.isExpired()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "세션이 만료되었습니다."));
            }
            
            // 여행 정보가 완전한지 확인
            if (!sessionInfo.isCanGeneratePlan()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "여행 정보가 아직 완전하지 않습니다."));
            }
            
            // 수집된 정보를 그대로 가져오기
            Map<String, Object> collectedInfo = sessionInfo.getCollectedInfo();
            
            // LLM을 통한 여행 계획 생성 시도 (PromptEngineeringService 사용)
            Map<String, Object> llmGeneratedPlan = null;
            if (promptEngineeringService != null && threadId != null) {
                try {
                    // ChatThread에서 저장된 JSON 가져오기
                    com.compass.domain.chat.entity.ChatThread chatThread = 
                        followUpService.getChatThreadById(threadId);
                    
                    if (chatThread != null && chatThread.getTravelPlanData() != null) {
                        // JSON 파싱
                        com.fasterxml.jackson.databind.ObjectMapper mapper = 
                            new com.fasterxml.jackson.databind.ObjectMapper();
                        Map<String, Object> savedTravelData = 
                            mapper.readValue(chatThread.getTravelPlanData(), Map.class);
                        
                        // PromptEngineeringService용 요청 데이터 준비
                        Map<String, Object> promptRequest = new HashMap<>();
                        Map<String, Object> info = (Map<String, Object>) savedTravelData.get("collectedInfo");
                        
                        // 필수 정보 매핑
                        promptRequest.put("destination", info.getOrDefault("목적지", "Seoul"));
                        promptRequest.put("nights", parseNights(info.get("숙박기간")));
                        promptRequest.put("travelDates", formatDates(info.get("출발일"), info.get("도착일")));
                        promptRequest.put("numberOfTravelers", parseNumber(info.get("인원")));
                        promptRequest.put("tripPurpose", "leisure travel");
                        promptRequest.put("userPreferences", buildPreferences(info));
                        promptRequest.put("travelStyle", determineTravelStyle(travelStyles));
                        promptRequest.put("budget", info.getOrDefault("예산", "moderate"));
                        promptRequest.put("groupType", info.getOrDefault("동행자", "solo"));
                        promptRequest.put("interests", travelStyles != null ? travelStyles : java.util.Arrays.asList("culture", "food"));
                        
                        // LLM을 통해 여행 계획 생성
                        log.info("Generating travel plan with PromptEngineeringService");
                        llmGeneratedPlan = promptEngineeringService.generateTravelPlan(promptRequest);
                        log.info("LLM generated plan: {}", llmGeneratedPlan != null);
                    }
                } catch (Exception e) {
                    log.error("Failed to generate plan with LLM, falling back to basic plan", e);
                }
            }
            
            // 목적지가 있으면 여행 제목 생성, 없으면 기본값
            String destination = (String) collectedInfo.getOrDefault("목적지", "미정");
            String title = destination + " 여행 계획";
            
            // 날짜 추출 - 달력에서 선택된 정제된 형식 (YYYY-MM-DD)
            String startDateStr = (String) collectedInfo.get("출발일");
            String endDateStr = (String) collectedInfo.get("도착일");
            
            java.time.LocalDate startDate;
            java.time.LocalDate endDate;
            
            try {
                // 달력에서 선택한 날짜는 이미 YYYY-MM-DD 형식
                startDate = startDateStr != null ? java.time.LocalDate.parse(startDateStr) : java.time.LocalDate.now().plusDays(7);
                endDate = endDateStr != null ? java.time.LocalDate.parse(endDateStr) : java.time.LocalDate.now().plusDays(10);
            } catch (Exception e) {
                // 파싱 실패 시 기본값 사용
                startDate = java.time.LocalDate.now().plusDays(7);
                endDate = java.time.LocalDate.now().plusDays(10);
            }
            
            // Trip 엔티티 생성을 위한 DTO 생성 - 모든 정보를 JSON으로 저장
            com.compass.domain.trip.dto.TripCreate.Request tripRequest = 
                new com.compass.domain.trip.dto.TripCreate.Request(
                    Long.parseLong(userId),  // userId
                    null,  // threadId (optional)
                    title,  // title
                    destination,  // destination
                    startDate,  // startDate (실제 선택된 날짜 또는 기본값)
                    endDate,  // endDate (실제 선택된 날짜 또는 기본값)
                    1,  // numberOfPeople (기본값)
                    null,  // totalBudget (null 허용)
                    null  // dailyPlans (LLM이 나중에 생성)
                );
            
            // Trip 생성 및 저장 - collectedInfo는 tripMetadata로 저장됨
            com.compass.domain.trip.dto.TripCreate.Response tripResponse = tripService.createTrip(tripRequest);
            
            log.info("Travel plan saved successfully - Trip ID: {}", tripResponse.id());
            
            // 응답 생성
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("message", "여행 계획이 성공적으로 생성되었습니다.");
            response.put("sessionId", sessionId);
            response.put("tripId", tripResponse.id());
            response.put("tripUuid", tripResponse.tripUuid());
            response.put("tripTitle", title);
            response.put("collectedInfo", sessionInfo.getCollectedInfo());
            response.put("travelStyles", travelStyles);
            
            // LLM 생성 계획이 있으면 추가
            if (llmGeneratedPlan != null) {
                response.put("generatedPlan", llmGeneratedPlan);
                response.put("planSource", "LLM_GENERATED");
            } else {
                response.put("planSource", "BASIC");
            }
            
            log.info("Travel plan generated successfully for session: {}", sessionId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error generating travel plan", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "여행 계획 생성 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    // Helper methods for generate-plan
    private Integer parseNights(Object value) {
        if (value == null) return 2; // 기본값
        String str = value.toString();
        // "2박" -> 2
        str = str.replaceAll("[^0-9]", "");
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return 2;
        }
    }
    
    private String formatDates(Object startDate, Object endDate) {
        if (startDate != null && endDate != null) {
            return startDate.toString() + " ~ " + endDate.toString();
        }
        return "next 3 days";
    }
    
    private Integer parseNumber(Object value) {
        if (value == null) return 1;
        String str = value.toString();
        // "2명" -> 2
        str = str.replaceAll("[^0-9]", "");
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return 1;
        }
    }
    
    private String buildPreferences(Map<String, Object> info) {
        StringBuilder prefs = new StringBuilder();
        
        Object style = info.get("여행스타일");
        if (style != null) {
            prefs.append(style.toString()).append(" style, ");
        }
        
        Object interests = info.get("관심사");
        if (interests != null) {
            prefs.append("interests: ").append(interests.toString());
        }
        
        return prefs.length() > 0 ? prefs.toString() : "relaxed style, interests: culture, food";
    }
    
    private String determineTravelStyle(java.util.List<String> travelStyles) {
        if (travelStyles != null && !travelStyles.isEmpty()) {
            return travelStyles.get(0);
        }
        return "relaxed";
    }
}