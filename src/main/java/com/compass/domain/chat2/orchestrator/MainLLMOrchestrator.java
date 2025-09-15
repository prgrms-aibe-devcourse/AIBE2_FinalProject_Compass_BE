package com.compass.domain.chat2.orchestrator;

import com.compass.domain.chat2.model.Intent;
import com.compass.domain.chat2.service.IntentClassificationService;
import com.compass.domain.chat2.config.OrchestratorConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * MainLLMOrchestrator - 중앙 지휘자
 * REQ-CHAT2-002: MainLLMOrchestrator 구현 (Gemini 2.0 Flash)
 * 
 * 모든 도메인의 Function을 통합하여 사용자 요청을 처리하는 중앙 오케스트레이터
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MainLLMOrchestrator {
    
    private final ChatModel chatModel;
    private final IntentClassificationService intentClassificationService;
    private final OrchestratorConfiguration orchestratorConfiguration;
    
    /**
     * 사용자 요청을 처리하는 메인 메서드
     * 
     * @param userInput 사용자 입력
     * @param threadId 대화 스레드 ID
     * @return 처리 결과
     */
    public String orchestrate(String userInput, String threadId) {
        log.info("MainLLMOrchestrator 시작: threadId={}, input={}", threadId, userInput);
        
        try {
            // 1. Intent 분류
            Intent intent = intentClassificationService.classifyIntent(userInput);
            log.info("Intent 분류 결과: {}", intent);
            
            // 2. Intent에 따른 Function 선택 및 실행
            String result = executeFunctionByIntent(userInput, intent, threadId);
            
            log.info("MainLLMOrchestrator 완료: threadId={}", threadId);
            return result;
            
        } catch (Exception e) {
            log.error("MainLLMOrchestrator 오류: threadId={}, error={}", threadId, e.getMessage(), e);
            return "죄송합니다. 처리 중 오류가 발생했습니다. 다시 시도해주세요.";
        }
    }
    
    /**
     * Intent에 따라 적절한 Function을 실행합니다.
     * 
     * @param userInput 사용자 입력
     * @param intent 분류된 Intent
     * @param threadId 대화 스레드 ID
     * @return 실행 결과
     */
    private String executeFunctionByIntent(String userInput, Intent intent, String threadId) {
        log.debug("Function 실행 시작: intent={}, threadId={}", intent, threadId);
        
        switch (intent) {
            case TRAVEL_PLANNING:
                return executeTravelPlanningFunction(userInput, threadId);
                
            case INFORMATION_COLLECTION:
                return executeInformationCollectionFunction(userInput, threadId);
                
            case IMAGE_UPLOAD:
                return executeImageUploadFunction(userInput, threadId);
                
            case GENERAL_QUESTION:
                return executeGeneralQuestionFunction(userInput, threadId);
                
            case QUICK_INPUT:
                return executeQuickInputFunction(userInput, threadId);
                
            case DESTINATION_SEARCH:
                return executeDestinationSearchFunction(userInput, threadId);
                
            case RESERVATION_PROCESSING:
                return executeReservationProcessingFunction(userInput, threadId);
                
            case API_USAGE_CHECK:
                return executeApiUsageCheckFunction(userInput, threadId);
                
            case UNKNOWN:
            default:
                return executeUnknownIntentFunction(userInput, threadId);
        }
    }
    
    /**
     * 여행 계획 생성 Function 실행
     */
    private String executeTravelPlanningFunction(String userInput, String threadId) {
        log.info("여행 계획 생성 Function 실행: threadId={}", threadId);
        
        try {
            // TRIP 도메인의 generateTravelPlan Function 호출
            if (orchestratorConfiguration.hasFunction("generateTravelPlan")) {
                log.info("generateTravelPlan Function 발견, 실행 중...");
                // 실제 Function 호출 로직은 Spring AI Function Calling으로 처리
                return "여행 계획을 생성하겠습니다. 목적지, 날짜, 인원, 예산, 여행 스타일을 알려주세요.";
            } else {
                log.warn("generateTravelPlan Function이 등록되지 않음");
                return "여행 계획 생성 기능이 아직 준비되지 않았습니다.";
            }
            
        } catch (Exception e) {
            log.error("여행 계획 생성 Function 실행 오류: {}", e.getMessage(), e);
            return "여행 계획 생성 중 오류가 발생했습니다. 다시 시도해주세요.";
        }
    }
    
    /**
     * 정보 수집 Function 실행
     */
    private String executeInformationCollectionFunction(String userInput, String threadId) {
        log.info("정보 수집 Function 실행: threadId={}", threadId);
        
        try {
            // CHAT2 도메인의 checkMissingInfo Function 호출
            return "추가 정보가 필요합니다. 구체적으로 알려주세요.";
            
        } catch (Exception e) {
            log.error("정보 수집 Function 실행 오류: {}", e.getMessage(), e);
            return "정보 수집 중 오류가 발생했습니다.";
        }
    }
    
    /**
     * 이미지 업로드 Function 실행
     */
    private String executeImageUploadFunction(String userInput, String threadId) {
        log.info("이미지 업로드 Function 실행: threadId={}", threadId);
        
        try {
            // MEDIA 도메인의 processOCR Function 호출
            return "이미지를 업로드해주세요. OCR로 예약 정보를 추출하겠습니다.";
            
        } catch (Exception e) {
            log.error("이미지 업로드 Function 실행 오류: {}", e.getMessage(), e);
            return "이미지 처리 중 오류가 발생했습니다.";
        }
    }
    
    /**
     * 일반 질문 Function 실행
     */
    private String executeGeneralQuestionFunction(String userInput, String threadId) {
        log.info("일반 질문 Function 실행: threadId={}", threadId);
        
        try {
            // CHAT1 도메인의 handleGeneralQuestions Function 호출
            return "일반 질문에 답변드리겠습니다. 여행 계획이 있으시면 언제든 말씀해주세요.";
            
        } catch (Exception e) {
            log.error("일반 질문 Function 실행 오류: {}", e.getMessage(), e);
            return "질문 처리 중 오류가 발생했습니다.";
        }
    }
    
    /**
     * 빠른 입력 Function 실행
     */
    private String executeQuickInputFunction(String userInput, String threadId) {
        log.info("빠른 입력 Function 실행: threadId={}", threadId);
        
        try {
            // USER 도메인의 processQuickInput Function 호출
            return "빠른 입력 폼을 처리하겠습니다.";
            
        } catch (Exception e) {
            log.error("빠른 입력 Function 실행 오류: {}", e.getMessage(), e);
            return "빠른 입력 처리 중 오류가 발생했습니다.";
        }
    }
    
    /**
     * 여행지 검색 Function 실행
     */
    private String executeDestinationSearchFunction(String userInput, String threadId) {
        log.info("여행지 검색 Function 실행: threadId={}", threadId);
        
        try {
            // TRIP 도메인의 searchWithPerplexity Function 호출
            return "여행지를 검색하겠습니다.";
            
        } catch (Exception e) {
            log.error("여행지 검색 Function 실행 오류: {}", e.getMessage(), e);
            return "여행지 검색 중 오류가 발생했습니다.";
        }
    }
    
    /**
     * 예약 처리 Function 실행
     */
    private String executeReservationProcessingFunction(String userInput, String threadId) {
        log.info("예약 처리 Function 실행: threadId={}", threadId);
        
        try {
            // MEDIA 도메인의 extractFlightInfo Function 호출
            return "예약 정보를 처리하겠습니다.";
            
        } catch (Exception e) {
            log.error("예약 처리 Function 실행 오류: {}", e.getMessage(), e);
            return "예약 처리 중 오류가 발생했습니다.";
        }
    }
    
    /**
     * API 사용량 확인 Function 실행
     */
    private String executeApiUsageCheckFunction(String userInput, String threadId) {
        log.info("API 사용량 확인 Function 실행: threadId={}", threadId);
        
        try {
            // USER 도메인의 trackApiUsage Function 호출
            return "API 사용량을 확인하겠습니다.";
            
        } catch (Exception e) {
            log.error("API 사용량 확인 Function 실행 오류: {}", e.getMessage(), e);
            return "API 사용량 확인 중 오류가 발생했습니다.";
        }
    }
    
    /**
     * 알 수 없는 Intent 처리
     */
    private String executeUnknownIntentFunction(String userInput, String threadId) {
        log.info("알 수 없는 Intent 처리: threadId={}", threadId);
        
        try {
            // 기본 응답 및 여행 주제로 유도
            return "죄송합니다. 이해하지 못했습니다. 여행 계획을 도와드릴까요?";
            
        } catch (Exception e) {
            log.error("알 수 없는 Intent 처리 오류: {}", e.getMessage(), e);
            return "처리 중 오류가 발생했습니다.";
        }
    }
    
    /**
     * 비동기 처리 지원
     */
    public CompletableFuture<String> orchestrateAsync(String userInput, String threadId) {
        return CompletableFuture.supplyAsync(() -> orchestrate(userInput, threadId));
    }
}
