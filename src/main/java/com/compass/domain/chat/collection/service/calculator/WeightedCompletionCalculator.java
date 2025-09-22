package com.compass.domain.chat.collection.service.calculator;

import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

// 각 정보의 중요도에 따라 가중치를 부여하여 정보 수집 진행률을 계산하는 구현체
@Component("weightedCompletionCalculator")
public class WeightedCompletionCalculator implements CompletionCalculator {

    // 각 필드와 완료 조건을 정의한 내부 레코드
    private record FieldWeight(String fieldName, int weight, Predicate<TravelFormSubmitRequest> completionCondition) {}

    // 필드별 가중치 및 완료 조건 목록 (총합 100)
    private static final List<FieldWeight> WEIGHTS = List.of(
            new FieldWeight("destinations", 30, info -> !CollectionUtils.isEmpty(info.destinations()) && info.destinations().stream().noneMatch("목적지 미정"::equalsIgnoreCase)),
            new FieldWeight("travelDates", 30, info -> info.travelDates() != null && info.travelDates().startDate() != null && info.travelDates().endDate() != null),
            new FieldWeight("budget", 20, info -> info.budget() != null && info.budget() > 0),
            new FieldWeight("companions", 10, info -> StringUtils.hasText(info.companions())),
            new FieldWeight("travelStyle", 10, info -> !CollectionUtils.isEmpty(info.travelStyle()))
    );

    @Override
    public int calculate(TravelFormSubmitRequest info) {
        if (info == null) {
            return 0;
        }
        // 완료된 필드들의 가중치를 합산하여 진행률 계산
        return WEIGHTS.stream()
                .filter(field -> field.completionCondition.test(info))
                .mapToInt(FieldWeight::weight)
                .sum();
    }

    @Override
    public List<String> getRequiredFields(TravelFormSubmitRequest info) {
        if (info == null) {
            return List.of("destinations", "travelDates", "budget", "companions", "travelStyle");
        }
        var missingFields = new ArrayList<String>();
        // 완료되지 않은 필드들의 이름을 리스트에 추가
        WEIGHTS.forEach(field -> {
            if (!field.completionCondition.test(info)) {
                missingFields.add(field.fieldName);
            }
        });
        return missingFields;
    }
}
