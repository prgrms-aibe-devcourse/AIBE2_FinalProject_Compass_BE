package com.compass.domain.completion.service;

import com.compass.domain.collection.dto.DateRange;
import com.compass.domain.collection.dto.TravelInfo;
import com.compass.domain.completion.dto.CompletionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// WeightedCompletionCalculator 단위 테스트
@DisplayName("WeightedCompletionCalculator 단위 테스트")
class WeightedCompletionCalculatorTest {

    private WeightedCompletionCalculator calculator;

    @BeforeEach
    void setUp() {
        // 각 테스트 전에 새로운 인스턴스를 생성하여 테스트 간 독립성을 보장합니다.
        calculator = new WeightedCompletionCalculator();
    }

    @Test
    @DisplayName("초기 상태: TravelInfo가 null일 경우 0%와 시작 메시지 반환")
    void calculate_withNullTravelInfo_shouldReturnZeroAndStartMessage() {
        // given: null 입력
        TravelInfo nullTravelInfo = null;

        // when: 계산 메서드 호출
        var result = calculator.calculate(nullTravelInfo);

        // then: 결과 검증
        assertThat(result.progressPercentage()).isZero();
        assertThat(result.message()).isEqualTo("여행 계획을 시작하려면 정보를 입력해주세요.");
    }

    @Test
    @DisplayName("핵심 정보 완료: 목적지, 날짜, 예산 입력 시 80%와 추가 정보 유도 메시지 반환")
    void calculate_withCoreInfo_shouldReturn80AndClarificationMessage() {
        // given: 목적지, 날짜, 예산만 입력된 정보
        var travelInfo = new TravelInfo(
                List.of("제주"),
                new DateRange(LocalDate.now(), LocalDate.now().plusDays(3)),
                500000,
                null, // 동행자 정보 없음
                null  // 스타일 정보 없음
        );

        // when: 계산 메서드 호출
        var result = calculator.calculate(travelInfo);

        // then: 결과 검증
        assertThat(result.progressPercentage()).isEqualTo(80);
        assertThat(result.message()).isEqualTo("필수 정보가 모두 입력되었어요! 더 정확한 추천을 위해 동행자나 여행 스타일에 대해서도 알려주시겠어요?");
    }

    @Test
    @DisplayName("모든 정보 완료: 모든 정보 입력 시 100%와 완료 메시지 반환")
    void calculate_withAllInfo_shouldReturn100AndCompletionMessage() {
        // given: 모든 정보가 입력된 상태
        var travelInfo = new TravelInfo(
                List.of("서울"),
                new DateRange(LocalDate.now(), LocalDate.now().plusDays(2)),
                1000000,
                "친구",
                "맛집 탐방"
        );

        // when: 계산 메서드 호출
        var result = calculator.calculate(travelInfo);

        // then: 결과 검증
        assertThat(result.progressPercentage()).isEqualTo(100);
        assertThat(result.message()).isEqualTo("모든 정보가 입력되었습니다! 이제 여행 계획을 생성할 수 있습니다.");
    }

    @Test
    @DisplayName("부분 정보 입력: 목적지와 날짜만 입력 시 60%와 진행률 메시지 반환")
    void calculate_withPartialInfo_shouldReturn60AndProgressMessage() {
        // given: 목적지와 날짜만 입력된 정보
        var travelInfo = new TravelInfo(
                List.of("부산"),
                new DateRange(LocalDate.now(), LocalDate.now().plusDays(5)),
                null,
                null,
                null
        );

        // when: 계산 메서드 호출
        var result = calculator.calculate(travelInfo);

        // then: 결과 검증
        assertThat(result.progressPercentage()).isEqualTo(60);
        assertThat(result.message()).isEqualTo("여행 정보의 60%가 입력되었습니다.");
    }

    @Test
    @DisplayName("정보 없음: 빈 TravelInfo 객체 입력 시 0%와 진행률 메시지 반환")
    void calculate_withEmptyTravelInfo_shouldReturnZeroAndProgressMessage() {
        // given: 모든 필드가 null인 정보
        var travelInfo = new TravelInfo(null, null, null, null, null);

        // when: 계산 메서드 호출
        var result = calculator.calculate(travelInfo);

        // then: 결과 검증
        assertThat(result.progressPercentage()).isZero();
        assertThat(result.message()).isEqualTo("여행 정보의 0%가 입력되었습니다.");
    }
}
