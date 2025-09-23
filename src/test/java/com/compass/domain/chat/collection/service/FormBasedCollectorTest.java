package com.compass.domain.chat.collection.service;

import com.compass.domain.chat.collection.service.validator.TravelInfoValidator;
import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FormBasedCollectorTest {

    // 실제 ObjectMapper를 사용하여 JSON 파싱 로직을 직접 테스트.
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private TravelInfoValidator validator;

    private FormBasedCollector formBasedCollector;

    @BeforeEach
    void setUp() {
        // 테스트 대상 객체를 수동으로 생성하여 의존성을 명확히 하기.
        formBasedCollector = new FormBasedCollector(objectMapper, validator);
    }

    @Test
    @DisplayName("collect - 유효한 JSON 문자열이 주어지면, TravelFormSubmitRequest 객체로 성공적으로 파싱한다")
    void collect_shouldParseValidJson_whenGivenValidInput() throws JsonProcessingException {
        // given: 유효한 JSON 형태의 사용자 입력
        String validJsonInput = """
                {
                  "userId": "user-123",
                  "destinations": ["제주도"],
                  "travelDates": { "startDate": "2024-10-26", "endDate": "2024-10-30" }
                }
                """;

        // when: collect 메서드 호출
        TravelFormSubmitRequest result = formBasedCollector.collect(validJsonInput, null);

        // then: 결과 검증
        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo("user-123");
        assertThat(result.destinations()).containsExactly("제주도");
        assertThat(result.travelDates().startDate()).isEqualTo(LocalDate.of(2024, 10, 26));
    }

    @Test
    @DisplayName("collect - 유효하지 않은 JSON 문자열이 주어지면, 예외를 던진다")
    void collect_shouldThrowException_whenGivenInvalidJson() {
        // given: 유효하지 않은 JSON 형태의 사용자 입력
        String invalidJsonInput = """
                {
                  "userId": "user-123",
                  "destinations": ["제주도"],
                  "travelDates": { "startDate": "2024-10-26", "endDate": "2024-10-30" }
                """; // 닫는 중괄호가 없음

        // when & then: collect 메서드 호출 시 JsonProcessingException 또는 그 상위 예외가 발생하는지 검증
        assertThatThrownBy(() -> formBasedCollector.collect(invalidJsonInput, null))
                .isInstanceOf(JsonProcessingException.class);
    }

    @Test
    @DisplayName("validate - TravelInfoValidator의 validate 메서드를 정확히 호출하여 책임을 위임한다")
    void validate_shouldDelegateToValidator() {
        // given: 검증할 여행 정보 객체
        var info = new TravelFormSubmitRequest("user-123", List.of("부산"), null, null, null, null, null, null);

        // when: validate 메서드 호출
        formBasedCollector.validate(info);

        // then: validator.validate()가 정확히 1번, 올바른 인자와 함께 호출되었는지 검증
        verify(validator).validate(info);
    }
}