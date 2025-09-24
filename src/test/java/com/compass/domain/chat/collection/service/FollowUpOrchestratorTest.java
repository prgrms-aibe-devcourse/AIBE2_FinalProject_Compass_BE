package com.compass.domain.chat.collection.service;

import com.compass.domain.chat.collection.service.strategy.ClarificationStrategy;
import com.compass.domain.chat.collection.service.strategy.MissingFieldStrategy;
import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FollowUpOrchestratorTest {

    @Mock
    private MissingFieldStrategy missingFieldStrategy;
    @Mock
    private ClarificationStrategy clarificationStrategy;

    private FollowUpOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        // 전략의 순서가 중요: 누락 필드 확인 -> 모호한 정보 확인
        orchestrator = new FollowUpOrchestrator(List.of(missingFieldStrategy, clarificationStrategy));
    }

    @Test
    @DisplayName("첫 번째 전략(MissingField)이 질문을 찾으면, 그 질문을 즉시 반환하고 다음 전략은 실행하지 않는다")
    void determineNextQuestion_shouldReturnFirstQuestion_whenFirstStrategyFindsOne() {
        // given
        var info = new TravelFormSubmitRequest(null, null, null, null, null, null, null, null, null, null);
        String missingFieldQuestion = "목적지가 어디인가요?";

        when(missingFieldStrategy.findNextQuestion(info)).thenReturn(Optional.of(missingFieldQuestion));
        // clarificationStrategy.findNextQuestion()는 호출되지 않아야 함

        // when
        Optional<String> result = orchestrator.determineNextQuestion(info);

        // then
        assertThat(result).isPresent().contains(missingFieldQuestion);
        verify(missingFieldStrategy, times(1)).findNextQuestion(info);
        verify(clarificationStrategy, never()).findNextQuestion(info); // 두 번째 전략은 호출되지 않음
    }

    @Test
    @DisplayName("첫 번째 전략이 질문을 못 찾으면, 두 번째 전략(Clarification)을 실행하여 질문을 찾는다")
    void determineNextQuestion_shouldUseSecondStrategy_whenFirstFindsNothing() {
        // given
        var info = new TravelFormSubmitRequest(null, null, null, null, null, null, null, null, null, null);
        String clarificationQuestion = "목적지를 더 구체적으로 알려주세요.";

        when(missingFieldStrategy.findNextQuestion(info)).thenReturn(Optional.empty());
        when(clarificationStrategy.findNextQuestion(info)).thenReturn(Optional.of(clarificationQuestion));

        // when
        Optional<String> result = orchestrator.determineNextQuestion(info);

        // then
        assertThat(result).isPresent().contains(clarificationQuestion);
        verify(missingFieldStrategy, times(1)).findNextQuestion(info);
        verify(clarificationStrategy, times(1)).findNextQuestion(info);
    }

    @Test
    @DisplayName("모든 전략이 질문을 찾지 못하면, 비어있는 Optional을 반환한다")
    void determineNextQuestion_shouldReturnEmpty_whenNoStrategyFindsQuestion() {
        // given
        var info = new TravelFormSubmitRequest(null, null, null, null, null, null, null, null, null, null);

        when(missingFieldStrategy.findNextQuestion(info)).thenReturn(Optional.empty());
        when(clarificationStrategy.findNextQuestion(info)).thenReturn(Optional.empty());

        // when
        Optional<String> result = orchestrator.determineNextQuestion(info);

        // then
        assertThat(result).isNotPresent();
    }
}