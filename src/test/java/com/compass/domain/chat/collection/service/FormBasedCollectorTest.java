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

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private TravelInfoValidator validator;

    private FormBasedCollector formBasedCollector;

    @BeforeEach
    void setUp() {
        formBasedCollector = new FormBasedCollector(objectMapper, validator);
    }

    @Test
    @DisplayName("collect - 유효한 JSON 문자열이 주어지면, TravelFormSubmitRequest 객체로 성공적으로 파싱한다")
    void collect_shouldParseValidJson_whenGivenValidInput() throws JsonProcessingException {
        // given
        String validJsonInput = """
                {
                  "userId": "user-123",
                  "destinations": ["제주도"],
                  "travelDates": { "startDate": "2024-10-26", "endDate": "2024-10-30" }
                }
                """;

        // when
        TravelFormSubmitRequest result = formBasedCollector.collect(validJsonInput, null);

        // then
        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo("user-123");
        assertThat(result.destinations()).containsExactly("제주도");
        assertThat(result.travelDates().startDate()).isEqualTo(LocalDate.of(2024, 10, 26));
    }

    @Test
    @DisplayName("collect - 유효하지 않은 JSON 문자열이 주어지면, 예외를 던진다")
    void collect_shouldThrowException_whenGivenInvalidJson() {
        // given
        String invalidJsonInput = "{ \"userId\": \"user-123\","; // Invalid JSON

        // when & then
        assertThatThrownBy(() -> formBasedCollector.collect(invalidJsonInput, null))
                .isInstanceOf(JsonProcessingException.class);
    }

    @Test
    @DisplayName("validate - TravelInfoValidator의 validate 메서드를 정확히 호출하여 책임을 위임한다")
    void validate_shouldDelegateToValidator() {
        // given
        // ✅ 수정: 생성자에 null 2개 추가
        var info = new TravelFormSubmitRequest("user-123", List.of("부산"), null, null, null, null, null, null, null, null);

        // when
        formBasedCollector.validate(info);

        // then
        verify(validator).validate(info);
    }
}