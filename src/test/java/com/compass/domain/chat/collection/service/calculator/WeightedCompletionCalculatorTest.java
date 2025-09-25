package com.compass.domain.chat.collection.service.calculator;

import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WeightedCompletionCalculatorTest {

    private WeightedCompletionCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new WeightedCompletionCalculator();
    }

    @Test
    @DisplayName("모든 정보가 입력되면 진행률은 100%가 되어야 한다")
    void calculate_shouldReturn100_whenAllInfoIsProvided() {
        // given
        // ✅ 수정: 생성자에 LocalTime.now() 2개 추가
        var completeInfo = new TravelFormSubmitRequest(
                "user-1",
                List.of("서울", "부산"),
                "인천",
                new TravelFormSubmitRequest.DateRange(LocalDate.now(), LocalDate.now().plusDays(3)),
                LocalTime.now(),
                LocalTime.now().plusHours(1),
                "친구와",
                1000000L,
                List.of("맛집", "관광"),
                null
        );

        // when
        int progress = calculator.calculate(completeInfo);
        List<String> missingFields = calculator.getRequiredFields(completeInfo);

        // then
        assertThat(progress).isEqualTo(100);
        assertThat(missingFields).isEmpty();
    }

    @Test
    @DisplayName("목적지와 날짜만 입력되면 진행률은 60%가 되어야 한다")
    void calculate_shouldReturn60_whenOnlyDestinationAndDatesAreProvided() {
        // given
        // ✅ 수정: 생성자에 null 2개 추가
        var partialInfo = new TravelFormSubmitRequest(
                "user-1",
                List.of("제주도"),
                null,
                new TravelFormSubmitRequest.DateRange(LocalDate.now(), LocalDate.now().plusDays(2)),
                null,
                null,
                null,
                null,
                null,
                null
        );

        // when
        int progress = calculator.calculate(partialInfo);
        List<String> missingFields = calculator.getRequiredFields(partialInfo);

        // then
        assertThat(progress).isEqualTo(60); // 30 (destinations) + 30 (travelDates)
        assertThat(missingFields).containsExactly("budget", "companions", "travelStyle");
    }

    @Test
    @DisplayName("정보가 전혀 없으면 진행률은 0%가 되어야 한다")
    void calculate_shouldReturn0_whenNoInfoIsProvided() {
        // given
        // ✅ 수정: 생성자에 null 2개 추가
        var emptyInfo = new TravelFormSubmitRequest(null, null, null, null, null, null, null, null, null, null);

        // when
        int progress = calculator.calculate(emptyInfo);
        List<String> missingFields = calculator.getRequiredFields(emptyInfo);

        // then
        assertThat(progress).isEqualTo(0);
        assertThat(missingFields).hasSize(5);
    }

    @Test
    @DisplayName("null 입력 시 진행률은 0%를 반환하고 모든 필드를 누락 처리한다")
    void calculate_shouldHandleNullInputGracefully() {
        // when
        int progress = calculator.calculate(null);
        List<String> missingFields = calculator.getRequiredFields(null);

        // then
        assertThat(progress).isEqualTo(0);
        assertThat(missingFields).containsExactly("destinations", "travelDates", "budget", "companions", "travelStyle");
    }
}