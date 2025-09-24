package com.compass.domain.chat.collection.service;

import com.compass.domain.chat.collection.service.calculator.CompletionCalculator;
import com.compass.domain.chat.collection.service.notifier.ProgressNotifier;
import com.compass.domain.chat.collection.service.storage.TravelInfoStorage;
import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
    @Mock
    private TravelInfoStorage storage;
    @Mock
    private FollowUpOrchestrator followUpOrchestrator;
    @Mock
    private CompletionCalculator calculator;
    @Mock
    private ProgressNotifier notifier;

    private TravelInfoCollectionService collectionService;

    @BeforeEach
    void setUp() {
        // Mock 객체들을 Map에 담아 주입
        Map<String, TravelInfoCollector> collectors = Map.of("formBasedCollector", formBasedCollector);
        collectionService = new TravelInfoCollectionService(collectors, storage, followUpOrchestrator, calculator, notifier);
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

        when(storage.load(threadId)).thenReturn(initialInfo);
        when(formBasedCollector.collect(userInput, initialInfo)).thenReturn(updatedInfo);
        when(calculator.calculate(updatedInfo)).thenReturn(30);
        when(followUpOrchestrator.determineNextQuestion(updatedInfo)).thenReturn(Optional.of("다음 질문"));

        // when
        var result = collectionService.collectInfo(threadId, userInput, collectorType);

        // then
        // 각 컴포넌트가 올바른 순서와 값으로 호출되었는지 검증
        verify(storage).load(threadId);
        verify(formBasedCollector).collect(userInput, initialInfo);
        verify(formBasedCollector).validate(updatedInfo);
        verify(storage).save(threadId, updatedInfo);
        verify(calculator).calculate(updatedInfo);
        verify(notifier).notify(threadId, 30);
        verify(followUpOrchestrator).determineNextQuestion(updatedInfo);

        assertThat(result.progress()).isEqualTo(30);
        assertThat(result.nextQuestion()).isPresent().contains("다음 질문");
    }
}