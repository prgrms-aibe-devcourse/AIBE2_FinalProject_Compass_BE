package com.compass.domain.chat2.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.HashMap;
import java.util.Map;

/**
 * OrchestratorConfiguration - MainLLMOrchestrator를 위한 Function 통합 설정
 *
 * 모든 도메인의 Function을 수집하여 MainLLMOrchestrator에 제공
 */
@Slf4j
@Configuration
public class OrchestratorConfiguration {

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * 모든 도메인의 Function을 수집하여 Map으로 제공
     *
     * @return Function 이름과 FunctionCallback의 Map
     */
    @Bean
    @Primary
    public Map<String, FunctionCallback> allFunctions() {
        Map<String, FunctionCallback> allFunctions = new HashMap<>();

        // Spring 컨텍스트에서 모든 FunctionCallback 빈 수집
        Map<String, FunctionCallback> functionBeans = applicationContext.getBeansOfType(FunctionCallback.class);

        functionBeans.forEach((beanName, callback) -> {
            // 로거나 더미 콜백은 제외
            if (!beanName.contains("Logger") && !beanName.contains("dummy")) {
                allFunctions.put(beanName, callback);
                log.info("📌 Function 등록: {} - {}", beanName, callback.getDescription());
            }
        });

        // Function 타입 빈들도 FunctionCallback으로 래핑
        Map<String, java.util.function.Function> functionTypeBeans =
            applicationContext.getBeansOfType(java.util.function.Function.class);

        functionTypeBeans.forEach((beanName, function) -> {
            if (!allFunctions.containsKey(beanName) && !beanName.contains("Logger")) {
                // Function을 FunctionCallback으로 래핑
                FunctionCallback callback = FunctionCallback.builder()
                    .function(beanName, function)
                    .description(getDescriptionForFunction(beanName))
                    .build();

                allFunctions.put(beanName, callback);
                log.info("📌 Function 래핑 및 등록: {}", beanName);
            }
        });

        log.info("✅ 총 {}개의 Function이 MainLLMOrchestrator에 등록되었습니다", allFunctions.size());

        return allFunctions;
    }

    /**
     * Function 이름으로부터 설명 생성
     */
    private String getDescriptionForFunction(String functionName) {
        return switch (functionName) {
            // CHAT2 Functions
            case "analyzeUserInput" -> "사용자 입력에서 여행 정보를 추출합니다";
            case "startFollowUp" -> "누락된 정보 수집을 위한 Follow-up을 시작합니다";
            case "classifyIntent" -> "사용자 입력의 의도를 분류합니다";
            case "generateFinalResponse" -> "최종 응답을 생성합니다";

            // TRIP Functions (예상)
            case "generateTravelPlan" -> "여행 계획을 생성합니다";
            case "searchWithPerplexity" -> "Perplexity API로 트렌디한 장소를 검색합니다";
            case "getWeatherInfo" -> "날씨 정보를 조회합니다";

            // MEDIA Functions (예상)
            case "uploadToS3AndOCR" -> "이미지를 S3에 업로드하고 OCR을 수행합니다";
            case "extractFlightInfo" -> "항공권 정보를 추출합니다";
            case "extractHotelInfo" -> "호텔 예약 정보를 추출합니다";

            // USER Functions (예상)
            case "processQuickInput" -> "빠른 입력 폼을 처리합니다";
            case "trackApiUsage" -> "API 사용량을 추적합니다";
            case "getUserPreferences" -> "사용자 선호도를 조회합니다";

            // CHAT1 Functions (예상)
            case "handleGeneralQuestions" -> "일반적인 질문에 답변합니다";
            case "redirectToTravel" -> "대화를 여행 주제로 유도합니다";

            default -> functionName + " Function";
        };
    }
}