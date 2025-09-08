package com.compass.domain.chat.controller;

import com.compass.domain.chat.service.PromptEngineeringService;
import com.compass.domain.chat.service.NaturalLanguageParsingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Prompt Engineering Controller
 * 프롬프트 엔지니어링 기반 여행 계획 생성 API
 */
@RestController
@RequestMapping("/api/chat/prompt")
@Tag(name = "Prompt Engineering", description = "프롬프트 엔지니어링 기반 여행 계획 API")
public class PromptEngineeringController {
    
    private static final Logger logger = LoggerFactory.getLogger(PromptEngineeringController.class);
    
    private final PromptEngineeringService promptService;
    private final NaturalLanguageParsingService parsingService;
    
    @Autowired
    public PromptEngineeringController(PromptEngineeringService promptService,
                                      NaturalLanguageParsingService parsingService) {
        this.promptService = promptService;
        this.parsingService = parsingService;
    }
    
    /**
     * Generate travel plan using prompt engineering
     * 프롬프트 엔지니어링을 사용한 여행 계획 생성
     */
    @PostMapping("/generate")
    @Operation(summary = "Generate travel plan", description = "프롬프트 엔지니어링을 사용하여 여행 계획 생성")
    public ResponseEntity<Map<String, Object>> generateTravelPlan(@RequestBody Map<String, Object> request) {
        logger.info("Generating travel plan with prompt engineering: {}", request);
        
        try {
            // Validate required parameters
            if (!request.containsKey("destination")) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "destination is required",
                    "message", "목적지를 지정해주세요"
                ));
            }
            
            // Generate travel plan
            Map<String, Object> result = promptService.generateTravelPlan(request);
            
            logger.info("Successfully generated travel plan for {}", request.get("destination"));
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Failed to generate travel plan: ", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to generate travel plan",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Get available prompt templates
     * 사용 가능한 프롬프트 템플릿 목록 조회
     */
    @GetMapping("/templates/prompt")
    @Operation(summary = "Get prompt templates", description = "사용 가능한 프롬프트 템플릿 목록 조회")
    public ResponseEntity<Map<String, Object>> getPromptTemplates() {
        try {
            Set<String> templates = promptService.getAvailablePromptTemplates();
            
            Map<String, Object> response = new HashMap<>();
            response.put("count", templates.size());
            response.put("templates", templates);
            response.put("description", Map.of(
                "TravelPlanning", "종합적인 여행 계획 생성",
                "TravelRecommendation", "여행지 추천 및 제안",
                "DailyItinerary", "일별 상세 일정 계획",
                "BudgetOptimization", "예산 최적화 여행 계획",
                "DestinationDiscovery", "목적지 탐색 및 발견",
                "LocalExperience", "현지 경험 중심 계획"
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to get prompt templates: ", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to retrieve prompt templates",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Get available travel templates
     * 사용 가능한 여행 템플릿 목록 조회
     */
    @GetMapping("/templates/travel")
    @Operation(summary = "Get travel templates", description = "사용 가능한 여행 템플릿 목록 조회")
    public ResponseEntity<Map<String, Object>> getTravelTemplates() {
        try {
            List<Map<String, Object>> templates = promptService.getAvailableTravelTemplates();
            
            Map<String, Object> response = new HashMap<>();
            response.put("count", templates.size());
            response.put("templates", templates);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to get travel templates: ", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to retrieve travel templates",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Generate travel plan with specific template
     * 특정 템플릿을 사용한 여행 계획 생성
     */
    @PostMapping("/generate/{templateType}")
    @Operation(summary = "Generate with specific template", description = "특정 템플릿을 사용하여 여행 계획 생성")
    public ResponseEntity<Map<String, Object>> generateWithTemplate(
            @PathVariable String templateType,
            @RequestBody Map<String, Object> request) {
        
        logger.info("Generating travel plan with template: {}", templateType);
        
        try {
            // Set template type flag
            switch (templateType.toLowerCase()) {
                case "daily":
                    request.put("dailyDetail", true);
                    break;
                case "budget":
                    request.put("budgetFocus", true);
                    break;
                case "local":
                    request.put("localExperience", true);
                    break;
                case "discovery":
                    request.put("discovery", true);
                    break;
                case "recommendation":
                    request.put("recommendation", true);
                    break;
                default:
                    return ResponseEntity.badRequest().body(Map.of(
                        "error", "Invalid template type",
                        "message", "유효하지 않은 템플릿 타입입니다",
                        "validTypes", List.of("daily", "budget", "local", "discovery", "recommendation")
                    ));
            }
            
            // Generate travel plan
            Map<String, Object> result = promptService.generateTravelPlan(request);
            
            logger.info("Successfully generated {} travel plan", templateType);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Failed to generate travel plan with template: ", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to generate travel plan",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Generate travel plan from natural language input
     * 자연어 입력으로부터 여행 계획 생성
     */
    @PostMapping("/chat")
    @Operation(summary = "Chat-based travel planning", description = "자연어 입력을 통한 여행 계획 생성")
    public ResponseEntity<Map<String, Object>> chatTravelPlan(@RequestBody Map<String, Object> request) {
        logger.info("Processing natural language travel request");
        
        try {
            // Extract message from request
            String message = (String) request.get("message");
            if (message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Message is required",
                    "message", "메시지를 입력해주세요"
                ));
            }
            
            logger.info("User message: {}", message);
            
            // Step 1: Parse natural language to structured format
            Map<String, Object> parsedRequest = parsingService.parseNaturalLanguageRequest(message);
            logger.info("Parsed request: {}", parsedRequest);
            
            // Add any additional parameters from original request
            for (Map.Entry<String, Object> entry : request.entrySet()) {
                if (!"message".equals(entry.getKey())) {
                    parsedRequest.putIfAbsent(entry.getKey(), entry.getValue());
                }
            }
            
            // Step 2: Generate travel plan with parsed request
            Map<String, Object> result = promptService.generateTravelPlan(parsedRequest);
            
            // Add parsing info to response
            result.put("parsedInput", parsedRequest);
            result.put("originalMessage", message);
            
            logger.info("Successfully generated travel plan from natural language");
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Failed to process natural language request: ", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to process request",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Health check for prompt engineering service
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "프롬프트 엔지니어링 서비스 상태 확인")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "PromptEngineeringService");
        health.put("timestamp", System.currentTimeMillis());
        
        try {
            Set<String> promptTemplates = promptService.getAvailablePromptTemplates();
            List<Map<String, Object>> travelTemplates = promptService.getAvailableTravelTemplates();
            
            health.put("promptTemplates", promptTemplates.size());
            health.put("travelTemplates", travelTemplates.size());
            health.put("ready", true);
            
        } catch (Exception e) {
            health.put("ready", false);
            health.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(health);
    }
}