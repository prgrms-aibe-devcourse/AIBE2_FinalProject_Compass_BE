package com.compass.domain.intent.service.impl;

import com.compass.domain.intent.Intent;
import com.compass.domain.intent.IntentClassification;
import com.compass.domain.intent.service.IntentService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class IntentServiceImpl implements IntentService {

    /**
     * CHAT1: 키워드 사전 및 가중치 관리
     * 각 키워드에 가중치를 부여하여 신뢰도 점수 계산에 사용합니다.
     */
    private final Map<Intent, Map<String, Double>> keywordRegistry = new HashMap<>();
    private static final double MAX_SCORE_CAP = 1.0; // 점수 상한

    @PostConstruct
    public void init() {
        // 여행 계획 관련 키워드 (가중치 높음: 구체적 동사, 낮음: 일반 명사)
        Map<String, Double> travelKeywords = new HashMap<>();
        travelKeywords.put("짜줘", 0.9);
        travelKeywords.put("만들어줘", 0.9);
        travelKeywords.put("계획", 0.7);
        travelKeywords.put("일정", 0.6);
        travelKeywords.put("코스", 0.6);
        travelKeywords.put("여행", 0.4);
        keywordRegistry.put(Intent.TRAVEL, travelKeywords);

        // 추천 관련 키워드
        Map<String, Double> recommendationKeywords = new HashMap<>();
        recommendationKeywords.put("추천", 0.9);
        recommendationKeywords.put("알려줘", 0.7);
        recommendationKeywords.put("어디가", 0.6);
        recommendationKeywords.put("맛집", 0.5);
        recommendationKeywords.put("볼거리", 0.5);
        recommendationKeywords.put("쇼핑", 0.5);
        keywordRegistry.put(Intent.RECOMMENDATION, recommendationKeywords);

        // 일반 정보 관련 키워드
        Map<String, Double> generalKeywords = new HashMap<>();
        generalKeywords.put("날씨", 0.9);
        generalKeywords.put("환율", 0.9);
        generalKeywords.put("정보", 0.6);
        generalKeywords.put("비자", 0.8);
        generalKeywords.put("어때", 0.5);
        keywordRegistry.put(Intent.GENERAL, generalKeywords);
    }

    /**
     * CHAT1: 의도 분류 및 신뢰도 점수 계산
     */
    @Override
    public IntentClassification classifyIntent(String message) {
        String lowerCaseMessage = message.toLowerCase();

        // 각 의도별로 점수를 계산
        Map<Intent, Double> intentScores = new HashMap<>();
        for (Map.Entry<Intent, Map<String, Double>> registryEntry : keywordRegistry.entrySet()) {
            Intent intent = registryEntry.getKey();
            Map<String, Double> keywords = registryEntry.getValue();

            double score = keywords.entrySet().stream()
                    .filter(keywordEntry -> lowerCaseMessage.contains(keywordEntry.getKey()))
                    .mapToDouble(Map.Entry::getValue)
                    .sum();

            if (score > 0) {
                intentScores.put(intent, Math.min(score, MAX_SCORE_CAP));
            }
        }

        // 가장 높은 점수를 받은 의도를 선택
        if (intentScores.isEmpty()) {
            log.info("[Intent Classification] Message: '{}' -> Intent: {}, Score: 0.0", message, Intent.UNKNOWN);
            return new IntentClassification(Intent.UNKNOWN, 0.0);
        }

        IntentClassification result = intentScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> new IntentClassification(entry.getKey(), entry.getValue()))
                .orElse(new IntentClassification(Intent.UNKNOWN, 0.0));

        log.info("[Intent Classification] Message: '{}' -> Intent: {}, Score: {}", message, result.intent(), String.format("%.2f", result.confidenceScore()));
        return result;
    }
}
