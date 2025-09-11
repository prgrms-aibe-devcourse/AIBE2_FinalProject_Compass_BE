package com.compass.domain.user.service;

import com.compass.domain.trip.entity.TravelHistory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PreferenceAnalyzer {

    // 프로젝트의 다른 서비스와 동일하게, @Qualifier를 사용하여 Gemini 모델을 명시적으로 주입받습니다.
    @Qualifier("geminiChatClient")
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    /**
     * 사용자의 여행 기록을 바탕으로, Spring AI(Gemini)를 활용하여 여행 스타일을 지능적으로 분석/분류합니다.
     * 프롬프트는 추후 더 개선해볼 예정입니다.
     */
    public String analyzeTravelStyleWithAi(List<TravelHistory> histories) {
        if (histories == null || histories.isEmpty()) {
            return "NEW_TRAVELER";
        }

        try {
            String travelHistoriesJson = objectMapper.writeValueAsString(
                    histories.stream().map(this::toSimplifiedHistory).collect(Collectors.toList())
            );

            String systemPrompt = """
                     You are a travel preference analyzer. Your task is to analyze the user's past travel history and classify them into a single, representative traveler type.
                     The output must be a single, concise string in English, using uppercase letters and underscores.
                     Example types: BUDGET_ACTIVITY_SEEKER, LUXURY_RELAXATION_TRAVELER, FAMILY_SIGHTSEEING_EXPLORER, SOLO_CULTURAL_ADVENTURER.
                     Analyze the provided JSON data of travel histories and return only the most fitting traveler type.
                     """;

            String analysisResult = chatClient.prompt()
                    .system(systemPrompt)
                    .user(travelHistoriesJson)
                    .call()
                    .content();

            log.info("AI Analysis Result: {}", analysisResult);
            return analysisResult.trim().replaceAll("\\s+", "_").toUpperCase();

        } catch (Exception e) {
            log.error("AI-based analysis failed, falling back to rule-based analysis.", e);
            return analyzeTravelStyleWithRules(histories);
        }
    }

    /**
     * AI 분석 실패 시를 대비한 규칙 기반 폴백(Fallback) 분석기
     */
    private String analyzeTravelStyleWithRules(List<TravelHistory> histories) {
        log.warn("Falling back to rule-based preference analysis.");
        return histories.stream()
                .map(TravelHistory::getTravelStyle)
                .filter(item -> item != null && !item.isBlank())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("UNKNOWN");
    }

    // AI 프롬프트에 넣기 위해 여행 기록을 간소화된 Map 형태로 변환
    private Map<String, Object> toSimplifiedHistory(TravelHistory history) {
        Map<String, Object> map = new HashMap<>();
        if (history.getDestination() != null) {
            map.put("destination", history.getDestination());
        }
        if (history.getTravelStyle() != null) {
            map.put("travelStyle", history.getTravelStyle());
        }
        if (history.getTravelType() != null) {
            map.put("companionType", history.getTravelType());
        }
        if (history.getPreferredActivities() != null) {
            map.put("activities", history.getPreferredActivities());
        }
        return map;
    }
}