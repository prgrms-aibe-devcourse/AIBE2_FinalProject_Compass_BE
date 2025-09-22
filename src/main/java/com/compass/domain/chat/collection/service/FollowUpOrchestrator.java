package com.compass.domain.chat.collection.service;

import com.compass.domain.chat.collection.service.strategy.FollowUpStrategy;
import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

// Follow-up 프로세스를 조정하는 오케스트레이터
@Service
@RequiredArgsConstructor
public class FollowUpOrchestrator {

    // Spring이 등록된 모든 FollowUpStrategy Bean을 주입받아 우선순위대로 사용
    private final List<FollowUpStrategy> strategies;

    // 다음에 할 Follow-up 질문을 결정하여 반환
    public Optional<String> determineNextQuestion(TravelFormSubmitRequest currentInfo) {
        // 주입된 전략들을 순서대로 실행하여 첫 번째로 발견되는 질문을 반환
        return strategies.stream()
                .map(strategy -> strategy.findNextQuestion(currentInfo))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }
}