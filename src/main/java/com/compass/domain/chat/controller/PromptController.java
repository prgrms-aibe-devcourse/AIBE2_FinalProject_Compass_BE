package com.compass.domain.chat.controller;

import com.compass.domain.chat.service.PromptEngineeringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Prompt Controller
 * AI 프롬프트 템플릿 기반 여행 계획 생성 API
 */
@Slf4j
@RestController
@RequestMapping("/api/chat/prompt")
@Tag(name = "프롬프트 엔진", description = "AI 프롬프트 템플릿 기반 여행 계획 생성")
@RequiredArgsConstructor
public class PromptController {
    
    private final PromptEngineeringService promptEngineeringService;
    
    @Operation(
        summary = "AI 여행 계획 생성", 
        description = "사용자 메시지를 분석하여 프롬프트 템플릿 기반으로 개인화된 여행 계획을 생성합니다"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "여행 계획 생성 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/travel-plan")
    public ResponseEntity<Map<String, Object>> generateTravelPlan(
            @Parameter(description = "여행 계획 요청", required = true)
            @RequestBody Map<String, Object> request) {
        
        try {
            log.info("Travel plan generation request received: {}", request);
            
            // 사용자 메시지와 추가 정보 추출
            String userMessage = (String) request.get("message");
            if (userMessage == null || userMessage.isBlank()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "메시지가 필요합니다");
                error.put("suggestion", "예: '부산 2박3일 여행 계획 짜줘'");
                return ResponseEntity.badRequest().body(error);
            }
            
            // 프롬프트 엔지니어링 서비스로 여행 계획 생성
            Map<String, Object> travelPlan = promptEngineeringService.generateTravelPlan(request);
            
            // 응답 구성
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("plan", travelPlan.get("plan"));
            response.put("templateUsed", travelPlan.get("templateId"));
            response.put("promptType", travelPlan.get("promptType"));
            response.put("metadata", travelPlan.get("metadata"));
            
            // 꼬리질문이 있으면 추가
            if (travelPlan.containsKey("followUpQuestions")) {
                response.put("followUpQuestions", travelPlan.get("followUpQuestions"));
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to generate travel plan: ", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "여행 계획 생성 실패");
            error.put("details", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    @Operation(
        summary = "사용자 입력 분석",
        description = "사용자의 메시지를 분석하여 여행 관련 파라미터를 추출합니다"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "분석 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeUserInput(
            @Parameter(description = "분석할 메시지", required = true)
            @RequestBody Map<String, String> request) {
        
        try {
            String message = request.get("message");
            if (message == null || message.isBlank()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "분석할 메시지가 필요합니다");
                return ResponseEntity.badRequest().body(error);
            }
            
            // 메시지 분석 (NaturalLanguageParsingService 활용)
            Map<String, Object> analysis = new HashMap<>();
            
            // 간단한 파싱 로직 (실제로는 NLP 서비스 사용)
            analysis.put("message", message);
            
            // 키워드 추출
            Map<String, Object> extracted = new HashMap<>();
            if (message.contains("부산")) extracted.put("destination", "부산");
            if (message.contains("서울")) extracted.put("destination", "서울");
            if (message.contains("제주")) extracted.put("destination", "제주");
            
            if (message.contains("당일치기")) extracted.put("duration", "당일치기");
            if (message.contains("1박2일")) extracted.put("duration", "1박2일");
            if (message.contains("2박3일")) extracted.put("duration", "2박3일");
            if (message.contains("3박4일")) extracted.put("duration", "3박4일");
            
            if (message.contains("가족")) extracted.put("travelType", "가족여행");
            if (message.contains("커플") || message.contains("연인")) extracted.put("travelType", "커플여행");
            if (message.contains("혼자")) extracted.put("travelType", "나홀로여행");
            
            analysis.put("extractedParameters", extracted);
            
            // 누락된 정보 확인
            List<String> missingInfo = new java.util.ArrayList<>();
            if (!extracted.containsKey("destination")) missingInfo.add("목적지");
            if (!extracted.containsKey("duration")) missingInfo.add("여행 기간");
            if (!extracted.containsKey("travelType")) missingInfo.add("여행 유형");
            
            analysis.put("missingInformation", missingInfo);
            
            // 추천 질문 생성
            if (!missingInfo.isEmpty()) {
                List<String> followUpQuestions = new java.util.ArrayList<>();
                if (missingInfo.contains("목적지")) {
                    followUpQuestions.add("어디로 여행을 가시나요?");
                }
                if (missingInfo.contains("여행 기간")) {
                    followUpQuestions.add("몇 박 며칠 여행을 계획하시나요?");
                }
                if (missingInfo.contains("여행 유형")) {
                    followUpQuestions.add("누구와 함께 여행하시나요?");
                }
                analysis.put("suggestedQuestions", followUpQuestions);
            }
            
            return ResponseEntity.ok(analysis);
            
        } catch (Exception e) {
            log.error("Failed to analyze user input: ", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "메시지 분석 실패");
            error.put("details", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    @Operation(
        summary = "사용 가능한 템플릿 목록",
        description = "시스템에서 사용 가능한 프롬프트 템플릿 목록을 반환합니다"
    )
    @GetMapping("/templates")
    public ResponseEntity<Map<String, Object>> getAvailableTemplates() {
        try {
            Map<String, Object> response = new HashMap<>();
            
            // 프롬프트 템플릿 목록
            Set<String> promptTemplates = promptEngineeringService.getAvailablePromptTemplates();
            response.put("promptTemplates", promptTemplates);
            
            // 여행 템플릿 목록
            List<Map<String, Object>> travelTemplates = promptEngineeringService.getAvailableTravelTemplates();
            response.put("travelTemplates", travelTemplates);
            
            // 템플릿 설명
            Map<String, String> templateDescriptions = new HashMap<>();
            templateDescriptions.put("travel_planning", "종합적인 여행 계획 생성");
            templateDescriptions.put("daily_itinerary", "일차별 상세 일정 생성");
            templateDescriptions.put("budget_optimization", "예산 최적화 여행 계획");
            templateDescriptions.put("local_experience", "현지 체험 중심 여행");
            templateDescriptions.put("destination_discovery", "목적지 탐색 및 추천");
            templateDescriptions.put("travel_recommendation", "개인화 여행 추천");
            response.put("descriptions", templateDescriptions);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to get templates: ", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "템플릿 목록 조회 실패");
            error.put("details", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    @Operation(
        summary = "꼬리질문 생성",
        description = "현재 대화 컨텍스트를 기반으로 추가 정보를 수집하기 위한 꼬리질문을 생성합니다"
    )
    @PostMapping("/follow-up")
    public ResponseEntity<Map<String, Object>> generateFollowUpQuestions(
            @Parameter(description = "현재 컨텍스트", required = true)
            @RequestBody Map<String, Object> context) {
        
        try {
            Map<String, Object> response = new HashMap<>();
            List<String> questions = new java.util.ArrayList<>();
            
            // 컨텍스트 분석
            Map<String, Object> currentInfo = (Map<String, Object>) context.getOrDefault("currentInfo", new HashMap<>());
            
            // 정보 수집을 위한 꼬리질문 생성
            if (!currentInfo.containsKey("destination")) {
                questions.add("어떤 도시나 지역으로 여행을 가고 싶으신가요?");
            }
            
            if (!currentInfo.containsKey("duration")) {
                questions.add("몇 박 며칠 정도 여행을 계획하고 계신가요?");
            }
            
            if (!currentInfo.containsKey("budget")) {
                questions.add("여행 예산은 대략 어느 정도로 생각하고 계신가요?");
            }
            
            if (!currentInfo.containsKey("travelStyle")) {
                questions.add("선호하시는 여행 스타일이 있으신가요? (예: 휴양, 관광, 액티비티)");
            }
            
            if (!currentInfo.containsKey("companions")) {
                questions.add("누구와 함께 여행하시나요? (예: 가족, 친구, 연인, 혼자)");
            }
            
            if (!currentInfo.containsKey("interests")) {
                questions.add("특별히 관심있는 활동이나 테마가 있으신가요? (예: 맛집, 역사, 자연, 쇼핑)");
            }
            
            // 개인화를 위한 추가 질문
            if (currentInfo.containsKey("destination") && currentInfo.containsKey("duration")) {
                questions.add("숙박은 호텔과 펜션 중 어느 것을 선호하시나요?");
                questions.add("하루에 몇 개 정도의 관광지를 방문하고 싶으신가요?");
                questions.add("특별한 음식 제한사항이나 알레르기가 있으신가요?");
            }
            
            response.put("followUpQuestions", questions);
            response.put("totalQuestions", questions.size());
            response.put("context", currentInfo);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to generate follow-up questions: ", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "꼬리질문 생성 실패");
            error.put("details", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}