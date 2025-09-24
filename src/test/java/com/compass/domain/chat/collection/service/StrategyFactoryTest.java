package com.compass.domain.chat.collection.service;

import com.compass.domain.chat.collection.service.strategy.ClarificationStrategy;
import com.compass.domain.chat.collection.service.strategy.FollowUpStrategy;
import com.compass.domain.chat.collection.service.strategy.MissingFieldStrategy;
import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StrategyFactoryTest {

    @Mock
    private MissingFieldStrategy missingFieldStrategy;
    @Mock
    private ClarificationStrategy clarificationStrategy;

    @InjectMocks
    private StrategyFactory strategyFactory;

    @Test
    @DisplayName("getStrategy - 필수 정보가 누락되면 MissingFieldStrategy를 반환한다")
    void getStrategy_shouldReturnMissingFieldStrategy_whenFieldsAreMissing() {
        // given
        var info = new TravelFormSubmitRequest(null, null, null, null, null, null, null, null, null, null);
        when(missingFieldStrategy.findNextQuestion(info)).thenReturn(Optional.of("목적지 질문"));

        // when
        Optional<FollowUpStrategy> strategy = strategyFactory.getStrategy(info);

        // then
        assertThat(strategy).isPresent().contains(missingFieldStrategy);
    }

    @Test
    @DisplayName("getStrategy - 필수 정보는 있지만 정보가 모호하면 ClarificationStrategy를 반환한다")
    void getStrategy_shouldReturnClarificationStrategy_whenInfoIsAmbiguous() {
        // given
        var info = new TravelFormSubmitRequest(null, null, null, null, null, null, null, null, null, null);
        when(missingFieldStrategy.findNextQuestion(info)).thenReturn(Optional.empty());
        when(clarificationStrategy.findNextQuestion(info)).thenReturn(Optional.of("목적지 구체화 질문"));

        // when
        Optional<FollowUpStrategy> strategy = strategyFactory.getStrategy(info);

        // then
        assertThat(strategy).isPresent().contains(clarificationStrategy);
    }

    @Test
    @DisplayName("getStrategy - 적용할 전략이 없으면 비어있는 Optional을 반환한다")
    void getStrategy_shouldReturnEmpty_whenNoStrategyIsApplicable() {
        // given
        var info = new TravelFormSubmitRequest(null, null, null, null, null, null, null, null, null, null);
        when(missingFieldStrategy.findNextQuestion(info)).thenReturn(Optional.empty());
        when(clarificationStrategy.findNextQuestion(info)).thenReturn(Optional.empty());

        // when
        Optional<FollowUpStrategy> strategy = strategyFactory.getStrategy(info);

        // then
        assertThat(strategy).isNotPresent();
    }
}