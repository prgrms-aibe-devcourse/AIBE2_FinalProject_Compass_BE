package com.compass.domain.chat.service;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * REQ-INTENT-001: 키워드 기반 의도 분류기
 * 사용자 입력을 '여행', '추천', '일반' 세 가지 의도로 분류합니다.
 */
@Service
public class IntentRouter {

    // '여행' 관련 키워드
    private static final List<String> TRIP_KEYWORDS = Arrays.asList(
            "여행", "가고 싶어", "계획", "일정", "가자", "갈래", "어때"
    );

    // '추천' 관련 키워드
    private static final List<String> RECOMMENDATION_KEYWORDS = Arrays.asList(
            "추천", "알려줘", "뭐가 좋을까", "어디가 좋아", "찾아줘"
    );

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
    }

    /**
     * 사용자 입력을 분석하여 의도를 파악합니다.
     * @param userInput 사용자 입력 문자열
     * @return 분류된 의도 (TRIP, RECOMMENDATION, GENERAL)
     */
    public Intent route(String userInput) {
        String normalizedInput = userInput.toLowerCase().replaceAll("\s+", "");

        if (containsKeyword(normalizedInput, TRIP_KEYWORDS)) {
            return Intent.TRIP;
        }
        if (containsKeyword(normalizedInput, RECOMMENDATION_KEYWORDS)) {
            return Intent.RECOMMENDATION;
        }
        return Intent.GENERAL;
    }

    private boolean containsKeyword(String input, List<String> keywords) {
        return keywords.stream().anyMatch(input::contains);
    }
}
