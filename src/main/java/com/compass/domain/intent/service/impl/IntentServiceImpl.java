package com.compass.domain.intent.service.impl;

import com.compass.domain.intent.Intent;
import com.compass.domain.intent.service.IntentService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class IntentServiceImpl implements IntentService {

    /**
     * REQ-INTENT-003: 키워드 사전 관리
     * HashMap을 사용하여 의도별 키워드를 관리합니다.
     */
    private final Map<Intent, List<String>> keywordDictionary = new HashMap<>();

    @PostConstruct
    public void init() {
        keywordDictionary.put(Intent.TRAVEL, Arrays.asList("계획", "일정", "코스", "짜줘", "만들어줘", "여행"));
        keywordDictionary.put(Intent.RECOMMENDATION, Arrays.asList("추천", "알려줘", "어디가", "맛집", "볼거리", "쇼핑"));
        keywordDictionary.put(Intent.GENERAL, Arrays.asList("날씨", "환율", "정보", "비자", "어때", "뭐야", "누구야"));
    }

    /**
     * REQ-INTENT-001: 키워드 분류기
     * REQ-INTENT-004: 로깅 시스템
     */
    @Override
    public Intent classifyIntent(String message) {
        String lowerCaseMessage = message.toLowerCase();

        for (Map.Entry<Intent, List<String>> entry : keywordDictionary.entrySet()) {
            Intent intent = entry.getKey();
            List<String> keywords = entry.getValue();

            if (keywords.stream().anyMatch(lowerCaseMessage::contains)) {
                log.info("[Intent Classification] Message: '{}' -> Intent: {}", message, intent);
                return intent;
            }
        }

        log.info("[Intent Classification] Message: '{}' -> Intent: {}", message, Intent.UNKNOWN);
        return Intent.UNKNOWN;
    }
}
