package com.compass.domain.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * REQ-INTENT-001: LLM 기반 의도 분류기
 * 사용자 입력을 '여행', '추천', '일반' 세 가지 의도로 분류합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntentRouter {

    private final ChatModelService chatModelService;

    // LLM에게 의도 분류를 요청하는 시스템 프롬프트
    private static final String INTENT_CLASSIFICATION_PROMPT = 
            "You are an intent classifier. Classify the user's message into one of the following categories: " +
            "'여행' (Trip), '추천' (Recommendation), or '일반' (General). " +
            "Respond with only the single Korean word for the category. Do not add any other text or punctuation.";

    public enum Intent {
        TRIP("여행"),
        RECOMMENDATION("추천"),
        GENERAL("일반");

        private final String description;

        Intent(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        /**
         * 문자열을 해당 Intent Enum으로 변환합니다.
         * @param text "여행", "추천", "일반" 중 하나
         * @return 해당 Intent, 일치하는 것이 없으면 GENERAL
         */
        public static Intent fromString(String text) {
            for (Intent intent : Intent.values()) {
                if (intent.description.equalsIgnoreCase(text)) {
                    return intent;
                }
            }
            log.warn("Unknown intent received from LLM: {}. Falling back to GENERAL.", text);
            return GENERAL; // 알 수 없는 응답일 경우 GENERAL로 처리
        }
    }

    /**
     * LLM을 사용하여 사용자 입력의 의도를 파악합니다.
     * @param userInput 사용자 입력 문자열
     * @return 분류된 의도 (TRIP, RECOMMENDATION, GENERAL)
     */
    public Intent route(String userInput) {
        try {
            log.info("Routing intent for user input: '{}'", userInput);
            String intentResponse = chatModelService.generateResponse(INTENT_CLASSIFICATION_PROMPT, userInput);
            
            // LLM 응답에서 불필요한 문자(따옴표, 마침표 등)를 제거
            String cleanedResponse = intentResponse.trim().replaceAll("[\"'.]", "");
            
            log.info("LLM classified intent as: '{}'", cleanedResponse);
            return Intent.fromString(cleanedResponse);
        } catch (Exception e) {
            log.error("Error routing intent with LLM for input: '{}'. Falling back to GENERAL.", userInput, e);
            // LLM 호출 실패 시 GENERAL로 폴백
            return Intent.GENERAL;
        }
    }
}
