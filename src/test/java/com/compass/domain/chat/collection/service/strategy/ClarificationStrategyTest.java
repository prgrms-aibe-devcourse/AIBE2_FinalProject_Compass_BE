package com.compass.domain.chat.collection.service.strategy;

import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ClarificationStrategyTest {

    private ClarificationStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new ClarificationStrategy();
    }

    @Test
    @DisplayName("목적지가 '서울'처럼 광범위하면, 구체화 질문을 반환한다")
    void findNextQuestion_shouldAskForClarification_whenDestinationIsBroad() {
        // given: 광범위한 목적지 "서울"
        var info = new TravelFormSubmitRequest(null, List.of("서울"), null, null, null, null, null, null);

        // when
        Optional<String> question = strategy.findNextQuestion(info);

        // then
        assertThat(question).isPresent().contains("'서울' 전체를 구경하실 건가요, 아니면 특정 지역을 중심으로 다니실 건가요?");
    }

    @Test
    @DisplayName("목적지가 '강남'처럼 구체적이면, 목적지 구체화 질문을 하지 않는다")
    void findNextQuestion_shouldNotAsk_whenDestinationIsSpecific() {
        // given: 구체적인 목적지 "강남"
        var info = new TravelFormSubmitRequest(null, List.of("강남"), null, null, null, null, null, null);

        // when
        Optional<String> question = strategy.findNextQuestion(info);

        // then
        assertThat(question).isEmpty();
    }

    @Test
    @DisplayName("예산은 없지만 '가성비' 스타일이 있으면, 예산 구체화 질문을 반환한다")
    void findNextQuestion_shouldAskForBudget_whenStyleIsBudgetFriendly() {
        // given: 예산은 없고, 여행 스타일이 "가성비"인 경우
        var info = new TravelFormSubmitRequest(null, List.of("강릉"), null, null, null, null, List.of("맛집", "가성비"), null);

        // when
        Optional<String> question = strategy.findNextQuestion(info);

        // then
        assertThat(question).isPresent().contains("대략적인 예산은 얼마정도 생각하시나요? (예: 50만원, 100만원)");
    }

    @Test
    @DisplayName("예산이 이미 있거나 '가성비' 스타일이 없으면, 예산 질문을 하지 않는다")
    void findNextQuestion_shouldNotAskForBudget_whenBudgetIsPresent() {
        // given: 예산이 이미 있거나 관련 스타일이 없는 경우
        var infoWithBudget = new TravelFormSubmitRequest(null, List.of("강릉"), null, null, null, 500000L, List.of("맛집", "가성비"), null);
        var infoWithoutStyle = new TravelFormSubmitRequest(null, List.of("강릉"), null, null, null, null, List.of("맛집", "럭셔리"), null);

        // when
        Optional<String> question1 = strategy.findNextQuestion(infoWithBudget);
        Optional<String> question2 = strategy.findNextQuestion(infoWithoutStyle);

        // then
        assertThat(question1).isEmpty();
        assertThat(question2).isEmpty();
    }

    @Test
    @DisplayName("모호한 정보가 없으면, 질문을 반환하지 않는다")
    void findNextQuestion_shouldReturnEmpty_whenNoAmbiguousInfo() {
        // given: 모든 정보가 구체적인 요청
        var dateRange = new TravelFormSubmitRequest.DateRange(LocalDate.now(), LocalDate.now().plusDays(1));
        var info = new TravelFormSubmitRequest(null, List.of("속초"), "서울", dateRange, null, 1000000L, List.of("휴양"), null);

        // when
        Optional<String> question = strategy.findNextQuestion(info);

        // then
        assertThat(question).isNotPresent();
    }
}