package com.compass.domain.collection.service;

import com.compass.domain.collection.dto.DateRange;
import com.compass.domain.collection.dto.TravelInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

// FormBasedCollector 단위 테스트
@DisplayName("FormBasedCollector 단위 테스트")
class FormBasedCollectorTest {

    private FormBasedCollector formBasedCollector;

    @BeforeEach
    void setUp() {
        // 각 테스트 전에 새로운 인스턴스를 생성하여 테스트 간 독립성을 보장합니다.
        formBasedCollector = new FormBasedCollector();
    }

    @Test
    @DisplayName("성공: 유효한 FormData를 TravelInfo로 정상 변환")
    void collect_success() {
        // given: 테스트를 위한 준비
        var travelDates = new DateRange(LocalDate.of(2024, 8, 1), LocalDate.of(2024, 8, 5));
        var formData = new FormBasedCollector.FormData(
                List.of("서울", "부산"),
                travelDates,
                1000000,
                "친구",
                "맛집 탐방"
        );

        // when: 실제 테스트 대상 메서드 호출
        var result = formBasedCollector.collect(formData);

        // then: 결과 검증
        assertThat(result).isNotNull();
        assertThat(result.destinations()).isEqualTo(formData.destinations());
        assertThat(result.travelDates()).isEqualTo(formData.travelDates());
        assertThat(result.budget()).isEqualTo(formData.budget());
        assertThat(result.companions()).isEqualTo(formData.companions());
        assertThat(result.travelStyle()).isEqualTo(formData.travelStyle());
    }

    @Test
    @DisplayName("실패: 잘못된 타입의 입력이 들어올 경우 예외 발생")
    void collect_withInvalidInputType_shouldThrowException() {
        // given: 잘못된 타입의 입력 데이터
        var invalidInput = "이것은 잘못된 입력입니다.";

        // when & then: 예외가 발생하는지 검증
        var exception = assertThrows(IllegalArgumentException.class, () -> {
            formBasedCollector.collect(invalidInput);
        });

        // 예외 메시지까지 확인하여 더 견고한 테스트를 만듭니다.
        assertThat(exception.getMessage()).isEqualTo("Input for FormBasedCollector must be of type FormData");
    }

    @Test
    @DisplayName("엣지 케이스: FormData의 필드가 null일 경우, TravelInfo에도 null로 매핑")
    void collect_withNullFields() {
        // given: 필드가 비어있는 FormData
        var formData = new FormBasedCollector.FormData(
                null,
                null,
                null,
                null,
                null
        );

        // when: 메서드 호출
        var result = formBasedCollector.collect(formData);

        // then: 결과 검증
        assertThat(result).isNotNull();
        assertThat(result.destinations()).isNull();
        assertThat(result.travelDates()).isNull();
        assertThat(result.budget()).isNull();
        assertThat(result.companions()).isNull();
        assertThat(result.travelStyle()).isNull();
    }
    
    @Test
    @DisplayName("유효성 검증: 시작일이 종료일보다 늦은 DateRange로 FormData 생성 시 예외 발생")
    void createFormData_withInvalidDateRange_shouldThrowException() {
        // given: 잘못된 날짜 범위
        var invalidStartDate = LocalDate.of(2024, 8, 10);
        var invalidEndDate = LocalDate.of(2024, 8, 5);

        // when & then: FormData 생성 시점에서 예외가 발생하는지 검증
        // 이 테스트는 사실상 DateRange의 유효성 검증을 확인하는 것이지만,
        // FormBasedCollector의 입력 데이터 생성 과정의 안정성을 보장하기 위해 포함합니다.
        var exception = assertThrows(IllegalArgumentException.class, () -> {
            new FormBasedCollector.FormData(
                    List.of("제주"),
                    new DateRange(invalidStartDate, invalidEndDate),
                    500000,
                    "가족",
                    "휴양"
            );
        });

        assertThat(exception.getMessage()).isEqualTo("시작일은 종료일보다 늦을 수 없습니다.");
    }
}
