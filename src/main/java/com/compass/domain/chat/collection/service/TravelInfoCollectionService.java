package com.compass.domain.chat.collection.service;

import com.compass.domain.chat.collection.service.calculator.CompletionCalculator;
import com.compass.domain.chat.collection.service.notifier.ProgressNotifier;
import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import com.compass.domain.chat.service.TravelInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

// 정보 수집 시스템의 전체 파이프라인을 조율하는 통합 서비스 (Facade 패턴)
@Service
@RequiredArgsConstructor
public class TravelInfoCollectionService {

    private final Map<String, TravelInfoCollector> collectors; // "formBasedCollector", "conversationBasedCollector"
    private final TravelInfoService travelInfoService; // DB 서비스를 직접 사용
    private final FollowUpOrchestrator followUpOrchestrator;
    private final CompletionCalculator calculator;
    private final ProgressNotifier notifier;

    // 정보 수집 전체 프로세스를 담당하는 메인 메서드
    public CollectionResult collectInfo(String threadId, String userInput, String collectorType) {
        // 1. DB에서 이전 정보 로드
        TravelFormSubmitRequest currentInfo = travelInfoService.loadTravelInfo(threadId);

        // 2. 적절한 Collector(폼/대화)로 정보 수집(업데이트)
        TravelInfoCollector collector = collectors.get(collectorType);
        if (collector == null) {
            throw new IllegalArgumentException("지원하지 않는 수집기 타입입니다: " + collectorType);
        }
        TravelFormSubmitRequest updatedInfo = collector.collect(userInput, currentInfo);

        // 3. 수집된 정보 검증
        collector.validate(updatedInfo);

        // 4. 업데이트된 정보를 DB에 저장
        travelInfoService.saveTravelInfo(threadId, updatedInfo);

        // 5. 진행률 계산 및 알림
        int progress = calculator.calculate(updatedInfo);
        notifier.notify(threadId, progress);

        // 6. 다음 Follow-up 질문 결정
        Optional<String> nextQuestion = followUpOrchestrator.determineNextQuestion(updatedInfo);

        // 7. 최종 결과 반환
        return new CollectionResult(updatedInfo, nextQuestion, progress);
    }

    // 정보 수집 결과를 담는 내부 Record
    public record CollectionResult(
            TravelFormSubmitRequest collectedInfo,
            Optional<String> nextQuestion,
            int progress
    ) {}
}
