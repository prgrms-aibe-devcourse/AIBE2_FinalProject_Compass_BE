package com.compass.domain.chat.collection.service.strategy;

import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Optional;

//모호한 정보를 구체화하는 질문을 하는 전략
@Component("clarificationStrategy")
public class ClarificationStrategy implements FollowUpStrategy {

    private static final List<String> BROAD_CITIES = List.of("서울", "부산", "제주");

    @Override
    public Optional<String> findNextQuestion(TravelFormSubmitRequest info) {
        // 목적지가 모호한지 확인
        if (isDestinationAmbiguous(info)) {
            var destination = info.destinations().get(0);
            return Optional.of(String.format("'%s' 전체를 구경하실 건가요, 아니면 특정 지역을 중심으로 다니실 건가요?", destination));
        }

        // 예산이 모호한지 확인
        if (isBudgetAmbiguous(info)) {
            return Optional.of("대략적인 예산은 얼마정도 생각하시나요? (예: 50만원, 100만원)");
        }

        return Optional.empty();
    }

    // 목적지가 구체화 필요한지 판단
    private boolean isDestinationAmbiguous(TravelFormSubmitRequest info) {
        return !CollectionUtils.isEmpty(info.destinations()) &&
                info.destinations().size() == 1 &&
                BROAD_CITIES.contains(info.destinations().get(0));
    }

    // 예산이 구체화 필요한지 판단
    private boolean isBudgetAmbiguous(TravelFormSubmitRequest info) {
        return info.budget() == null &&
                !CollectionUtils.isEmpty(info.travelStyle()) &&
                info.travelStyle().stream().anyMatch(style -> style.contains("저렴") || style.contains("가성비"));
    }
}