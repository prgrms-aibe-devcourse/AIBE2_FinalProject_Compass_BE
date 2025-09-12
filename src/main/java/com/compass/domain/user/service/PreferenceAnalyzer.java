package com.compass.domain.user.service;

import com.compass.domain.user.enums.TravelTemplateType;
import com.compass.domain.trip.entity.TravelHistory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PreferenceAnalyzer {

    // AI에게 전달할 프롬프트의 '틀'을 정의합니다. %s 부분은 Enum의 이름 목록으로 동적으로 채워집니다.
    private static final String SYSTEM_PROMPT_TEMPLATE = """
             You are a travel preference analyzer.
             Your task is to analyze the user's past travel history and suggest up to 9 relevant travel styles from the predefined list below.
             The output must be a comma-separated list of strings, exactly matching the names from the provided list, in lowercase and with underscores, in order of relevance (most relevant first).
             Do not add any extra text, just the comma-separated list.
 
             Available travel styles:
             [%s]

            Analyze the provided JSON data of travel histories and return only the single most fitting travel style name from the list above.
            """;

    // 프로젝트의 다른 서비스와 동일하게, @Qualifier를 사용하여 Gemini 모델을 명시적으로 주입받습니다.
    @Qualifier("geminiChatClient")
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    /**
     * 사용자의 여행 기록을 바탕으로, Spring AI(Gemini)를 활용하여 여행 스타일을 지능적으로 분석/분류합니다.
     * @return 분석된 여행 템플릿 이름 리스트 (최대 9개)
     */
    public List<String> analyzeTravelStyleWithAi(List<TravelHistory> histories) {
        if (histories == null || histories.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            String travelHistoriesJson = objectMapper.writeValueAsString(
                    histories.stream().map(this::toSimplifiedHistory).collect(Collectors.toList())
            );

            // 1. Enum을 통해 정의된 모든 여행 템플릿 이름 목록을 가져옵니다.
            List<String> templateNames = TravelTemplateType.getNames();
            String availableTemplates = String.join(", ", templateNames);

            // 2. 템플릿 목록을 포함하여 AI에게 전달할 최종 프롬프트를 생성합니다.
            String systemPrompt = String.format(SYSTEM_PROMPT_TEMPLATE, availableTemplates);

            String analysisResult = chatClient.prompt()
                    .system(systemPrompt)
                    .user(travelHistoriesJson)
                    .call()
                    .content();

            // 3. AI의 답변(콤마로 구분된 문자열)을 파싱하고 검증합니다.
            List<String> analyzedStyles = Arrays.stream(analysisResult.split(","))
                    .map(style -> style.trim().replaceAll("\\s+", "_").toLowerCase())
                    .filter(templateNames::contains) // 우리가 준 목록에 있는 것만 필터링
                    .distinct() // 중복 제거
                    .limit(9)   // 최대 9개로 제한
                    .toList();

            if (!analyzedStyles.isEmpty()) {
                log.info("AI Analysis Result: {}", analyzedStyles);
                return analyzedStyles; // 소문자, 언더스코어 형태의 리스트를 반환
            }

            // 4. AI가 유효한 답변을 하나도 주지 못했다면, 규칙 기반으로 대체합니다.
            log.warn("AI did not return any valid template names from the response: '{}'. Falling back to rule-based analysis.", analysisResult);
            return List.of(analyzeTravelStyleWithRules(histories));

        } catch (Exception e) {
            log.error("AI-based analysis failed, falling back to rule-based analysis.", e);
            return List.of(analyzeTravelStyleWithRules(histories)); // 규칙 기반 결과도 리스트로 감싸서 반환
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