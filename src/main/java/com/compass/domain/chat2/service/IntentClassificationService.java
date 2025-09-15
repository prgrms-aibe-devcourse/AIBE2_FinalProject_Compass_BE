package com.compass.domain.chat2.service;

import com.compass.domain.chat2.model.Intent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

import static com.compass.domain.chat2.service.IntentClassificationConstants.*;

/**
 * IntentClassificationService - 사용자 의도 분류 서비스
 *
 * REQ-CHAT2-004: Intent 분류 및 라우팅
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntentClassificationService {

    private final ChatModel chatModel;

    /**
     * 사용자 입력에서 Intent를 분류 (LLM 직접 사용)
     *
     * @param userInput 사용자 입력
     * @return 분류된 Intent
     */
    public Intent classifyIntent(String userInput) {
        log.debug(LOG_INTENT_START, userInput);

        try {
            String llmResponse = callLLMForClassification(userInput);
            Intent intent = parseIntentFromResponse(llmResponse);
            log.info(LOG_LLM_BASED_CLASSIFIED, intent);
            return intent;
        } catch (Exception e) {
            log.error(LOG_LLM_CLASSIFICATION_FAILED, e);
            return Intent.UNKNOWN;
        }
    }

    /**
     * LLM 호출하여 분류 결과 받기
     */
    private String callLLMForClassification(String userInput) {
        Prompt prompt = createClassificationPrompt(userInput);
        return chatModel.call(prompt)
            .getResult()
            .getOutput()
            .getContent();
    }

    /**
     * 분류용 프롬프트 생성
     */
    private Prompt createClassificationPrompt(String userInput) {
        Map<String, Object> variables = Map.of("userInput", userInput);
        PromptTemplate promptTemplate = new PromptTemplate(INTENT_CLASSIFICATION_PROMPT, variables);
        return promptTemplate.create();
    }

    /**
     * LLM 응답에서 Intent 파싱
     */
    private Intent parseIntentFromResponse(String response) {
        String cleanResponse = response.trim().toUpperCase();

        try {
            return Intent.valueOf(cleanResponse);
        } catch (IllegalArgumentException e) {
            log.warn(LOG_UNKNOWN_INTENT, cleanResponse);
            return Intent.UNKNOWN;
        }
    }

    /**
     * Intent에 대한 설명 반환
     */
    public String getIntentDescription(Intent intent) {
        return switch (intent) {
            case TRAVEL_PLANNING -> "여행 계획 생성";
            case INFORMATION_COLLECTION -> "여행 정보 수집";
            case IMAGE_UPLOAD -> "이미지/문서 처리";
            case GENERAL_QUESTION -> "일반 질문";
            case QUICK_INPUT -> "빠른 입력";
            case DESTINATION_SEARCH -> "여행지 검색";
            case RESERVATION_PROCESSING -> "예약 처리";
            case API_USAGE_CHECK -> "API 사용량 확인";
            case UNKNOWN -> "알 수 없음";
        };
    }
}