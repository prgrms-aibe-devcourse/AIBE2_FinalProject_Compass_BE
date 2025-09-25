package com.compass.domain.chat.collection.service;

import com.compass.domain.chat.collection.service.calculator.CompletionCalculator;
import com.compass.domain.chat.collection.service.notifier.ProgressNotifier;
import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import com.compass.domain.chat.service.TravelInfoService; // ✅ TravelInfoService를 import
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TravelInfoCollectionServiceTest {

    @Mock
    private TravelInfoCollector formBasedCollector;

    // ✅ 수정: 타입을 TravelInfoService로 변경하고, 변수명도 역할에 맞게 변경
    @Mock
    private TravelInfoService travelInfoService;

    @Mock
    private FollowUpOrchestrator followUpOrchestrator;
    @Mock
    private CompletionCalculator calculator;
    @Mock
    private ProgressNotifier notifier;

    private TravelInfoCollectionService collectionService;

    @BeforeEach
    void setUp() {
        Map<String, TravelInfoCollector> collectors = Map.of("formBasedCollector", formBasedCollector);
        // ✅ 수정: 생성자에 travelInfoService를 전달
        collectionService = new TravelInfoCollectionService(collectors, travelInfoService, followUpOrchestrator, calculator, notifier);
    }

    @Test
    @DisplayName("collectInfo - 정보 수집의 전체 파이프라인을 올바른 순서로 실행한다")
    void collectInfo_shouldExecuteFullPipelineInOrder() {
        // given
        String threadId = "thread-1";
        String userInput = "{\"destinations\":[\"부산\"]}";
        String collectorType = "formBasedCollector";

        var initialInfo = new TravelFormSubmitRequest(null, null, null, null, null, null, null, null, null, null);
        var updatedInfo = new TravelFormSubmitRequest(null, java.util.List.of("부산"), null, null, null, null, null, null, null, null);

        // ✅ 수정: storage.load() -> travelInfoService.loadTravelInfo()
        when(travelInfoService.loadTravelInfo(threadId)).thenReturn(initialInfo);
        when(formBasedCollector.collect(userInput, initialInfo)).thenReturn(updatedInfo);
        when(calculator.calculate(updatedInfo)).thenReturn(30);
        when(followUpOrchestrator.determineNextQuestion(updatedInfo)).thenReturn(Optional.of("다음 질문"));

        // when
        var result = collectionService.collectInfo(threadId, userInput, collectorType);

        // then
        // ✅ 수정: storage -> travelInfoService로 검증 대상 변경
        verify(travelInfoService).loadTravelInfo(threadId);
        verify(formBasedCollector).collect(userInput, initialInfo);
        verify(formBasedCollector).validate(updatedInfo);
        verify(travelInfoService).saveTravelInfo(threadId, updatedInfo);
        verify(calculator).calculate(updatedInfo);
        verify(notifier).notify(threadId, 30);
        verify(followUpOrchestrator).determineNextQuestion(updatedInfo);

        assertThat(result.progress()).isEqualTo(30);
        assertThat(result.nextQuestion()).isPresent().contains("다음 질문");
    }
}