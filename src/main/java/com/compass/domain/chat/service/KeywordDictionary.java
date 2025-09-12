package com.compass.domain.chat.service;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * REQ-INTENT-003: 키워드 사전 관리
 * HashMap을 기반으로 핵심 키워드와 의도를 매핑하여 관리합니다.
 * LLM 호출 전에 빠른 경로(Fast Path)를 제공하여 시스템 효율성을 높입니다.
 */
@Component
public class KeywordDictionary {

    private static final Map<String, IntentRouter.Intent> KEYWORD_MAP = new HashMap<>();

    // static 초기화 블록을 사용하여 키워드와 의도를 매핑합니다.
    static {
        // 여행 관련 키워드
        KEYWORD_MAP.put("여행", IntentRouter.Intent.TRIP);
        KEYWORD_MAP.put("일정", IntentRouter.Intent.TRIP);
        KEYWORD_MAP.put("계획", IntentRouter.Intent.TRIP);
        KEYWORD_MAP.put("가려", IntentRouter.Intent.TRIP);
        KEYWORD_MAP.put("갈까", IntentRouter.Intent.TRIP);

        // 추천 관련 키워드
        KEYWORD_MAP.put("추천", IntentRouter.Intent.RECOMMENDATION);
        KEYWORD_MAP.put("알려줘", IntentRouter.Intent.RECOMMENDATION);
        KEYWORD_MAP.put("맛집", IntentRouter.Intent.RECOMMENDATION);
        KEYWORD_MAP.put("명소", IntentRouter.Intent.RECOMMENDATION);
        KEYWORD_MAP.put("찾아줘", IntentRouter.Intent.RECOMMENDATION);
        KEYWORD_MAP.put("어때", IntentRouter.Intent.RECOMMENDATION);
    }

    /**
     * 사용자 입력에 키워드가 포함되어 있는지 확인하여 해당하는 의도를 반환합니다.
     * @param userInput 사용자 입력 문자열
     * @return Optional<Intent> 키워드가 존재하면 해당 의도를, 없으면 Optional.empty()를 반환
     */
    public Optional<IntentRouter.Intent> findIntent(String userInput) {
        String normalizedInput = userInput.toLowerCase().replaceAll("\\s+", "");
        for (Map.Entry<String, IntentRouter.Intent> entry : KEYWORD_MAP.entrySet()) {
            if (normalizedInput.contains(entry.getKey())) {
                return Optional.of(entry.getValue());
            }
        }
        return Optional.empty();
    }
}
