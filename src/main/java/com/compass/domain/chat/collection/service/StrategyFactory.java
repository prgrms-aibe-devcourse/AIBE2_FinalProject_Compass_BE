package com.compass.domain.chat.collection.service;

import com.compass.domain.chat.collection.service.strategy.ClarificationStrategy;
import com.compass.domain.chat.collection.service.strategy.FollowUpStrategy;
import com.compass.domain.chat.collection.service.strategy.MissingFieldStrategy;
import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Optional;

// 컨텍스트를 분석하여 적절한 Follow-up 전략을 생성하는 팩토리
@Component
@RequiredArgsConstructor
public class StrategyFactory {

    private final MissingFieldStrategy missingFieldStrategy;
    private final ClarificationStrategy clarificationStrategy;

    // 현재 정보에 가장 적합한 전략을 찾아 반환
    public Optional<FollowUpStrategy> getStrategy(TravelFormSubmitRequest info) {
        // 우선순위 1: 필수 정보 누락 확인
        if (missingFieldStrategy.findNextQuestion(info).isPresent()) {
            return Optional.of(missingFieldStrategy);
        }
        // 우선순위 2: 정보 명확화 필요 확인
        if (clarificationStrategy.findNextQuestion(info).isPresent()) {
            return Optional.of(clarificationStrategy);
        }
        // 적용할 전략이 없음
        return Optional.empty();
    }
}
