package com.compass.domain.chat.collection.service.strategy;

import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MissingFieldStrategyTest {

    private MissingFieldStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new MissingFieldStrategy();
    }

    @Test
    @DisplayName("가장 우선순위가 높은 목적지 정보가 없으면, 목적지 질문을 반환한다")
    void findNextQuestion_shouldAskForDestination_whenItIsMissing() {
        // given: 목적지 정보가 없는 요청
        var info = new TravelFormSubmitRequest(null, null, null, null, null, null, null, null);

        // when: 다음 질문 찾기
        Optional<String> question = strategy.findNextQuestion(info);

        // then: 목적지 질문이 반환되어야 함
        assertThat(question).isPresent().contains("어디로 여행을 떠나고 싶으신가요? 도시 이름을 알려주세요.");
    }

    @Test
    @DisplayName("목적지가 '목적지 미정'일 때, 목적지 질문을 반환한다")
    void findNextQuestion_shouldAskForDestination_whenItIsUndecided() {
        // given: 목적지가 "목적지 미정"인 요청
        var info = new TravelFormSubmitRequest(null, List.of("목적지 미정"), null, null, null, null, null, null);

        // when: 다음 질문 찾기
        Optional<String> question = strategy.findNextQuestion(info);

        // then: 목적지 질문이 반환되어야 함
        assertThat(question).isPresent().contains("어디로 여행을 떠나고 싶으신가요? 도시 이름을 알려주세요.");
    }

    @Test
    @DisplayName("목적지는 있지만 날짜 정보가 없으면, 날짜 질문을 반환한다")
    void findNextQuestion_shouldAskForDates_whenDestinationIsPresentButDatesAreMissing() {
        // given: 목적지는 있지만 날짜가 없는 요청
        var info = new TravelFormSubmitRequest(null, List.of("부산"), null, null, null, null, null, null);

        // when
        Optional<String> question = strategy.findNextQuestion(info);

        // then: 날짜 질문이 반환되어야 함
        assertThat(question).isPresent().contains("여행은 언제부터 언제까지 계획하고 계신가요?");
    }

    @Test
    @DisplayName("목적지와 날짜는 있지만 출발지가 없으면, 출발지 질문을 반환한다")
    void findNextQuestion_shouldAskForDeparture_whenDatesArePresentButDepartureIsMissing() {
        // given: 목적지와 날짜는 있지만 출발지가 없는 요청
        var dateRange = new TravelFormSubmitRequest.DateRange(LocalDate.now(), LocalDate.now().plusDays(1));
        var info = new TravelFormSubmitRequest(null, List.of("서울"), null, dateRange, null, null, null, null);

        // when
        Optional<String> question = strategy.findNextQuestion(info);

        // then: 출발지 질문이 반환되어야 함
        assertThat(question).isPresent().contains("어디서 출발하시나요?");
    }

    @Test
    @DisplayName("모든 필수 정보가 있으면, 질문을 반환하지 않는다")
    void findNextQuestion_shouldReturnEmpty_whenAllRequiredInfoIsPresent() {
        // given: 모든 필수 정보가 있는 요청
        var dateRange = new TravelFormSubmitRequest.DateRange(LocalDate.now(), LocalDate.now().plusDays(1));
        var info = new TravelFormSubmitRequest(null, List.of("제주"), "서울", dateRange, null, null, null, null);

        // when
        Optional<String> question = strategy.findNextQuestion(info);

        // then: 질문이 없어야 함
        assertThat(question).isNotPresent();
    }
}