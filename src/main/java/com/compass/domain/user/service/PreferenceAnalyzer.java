package com.compass.domain.user.service;

import com.compass.domain.trip.entity.TravelHistory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class PreferenceAnalyzer {

    /**
     * 사용자의 여행 기록 리스트를 받아, 가장 지배적인 여행 스타일을 분석하여 문자열로 반환합니다.
     * (규칙 기반 초기 버전)
     */
    public String analyzeTravelStyle(List<TravelHistory> histories) {
        if (histories == null || histories.isEmpty()) {
            return "NEW_TRAVELER"; // 분석할 기록이 없는 신규 사용자
        }

        // 가장 빈번하게 나타난 여행 스타일(travelStyle)을 찾습니다.
        String dominantStyle = findMostFrequent(histories, TravelHistory::getTravelStyle);

        // TODO: 향후 예산, 동행인 등 다른 요소를 결합하여 분석 로직을 고도화할 수 있습니다.
        // 예: "BUDGET_ACTIVITY_TRAVELER", "LUXURY_RELAXATION_TRAVELER"

        log.info("Analyzed dominant travel style: {}", dominantStyle);
        return dominantStyle;
    }

    private <T> String findMostFrequent(List<T> items, Function<T, String> classifier) {
        return items.stream()
                .map(classifier)
                .filter(item -> item != null && !item.isBlank())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("UNKNOWN"); // 분석 결과가 없을 경우
    }
}