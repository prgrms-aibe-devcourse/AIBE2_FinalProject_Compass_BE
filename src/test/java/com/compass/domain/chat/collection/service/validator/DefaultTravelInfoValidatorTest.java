package com.compass.domain.chat.collection.service.validator;

import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultTravelInfoValidatorTest {

    @Mock
    private DateValidationRule dateRule;
    @Mock
    private DestinationValidationRule destinationRule;

    private DefaultTravelInfoValidator validator;

    @BeforeEach
    void setUp() {
        // Mock으로 만든 검증 규칙들을 주입하여 테스트
        validator = new DefaultTravelInfoValidator(List.of(dateRule, destinationRule));
    }

    @Test
    @DisplayName("validate - 주입된 모든 검증 규칙들을 실행한다")
    void validate_shouldExecuteAllInjectedRules() {
        // given
        var info = new TravelFormSubmitRequest(null, null, null, null, null, null, null, null);

        // when
        validator.validate(info);

        // then
        // 각 규칙의 apply 메서드가 정확히 1번씩 호출되었는지 검증
        verify(dateRule, times(1)).apply(info);
        verify(destinationRule, times(1)).apply(info);
    }

    @Test
    @DisplayName("getErrors - 각 규칙에서 발생한 모든 오류 메시지를 수집하여 반환한다")
    void getErrors_shouldCollectAllErrorMessagesFromRules() {
        // given
        var info = new TravelFormSubmitRequest(null, null, null, null, null, null, null, null);
        String dateError = "날짜를 입력해주세요.";
        String destError = "목적지를 입력해주세요.";

        // Mocking: 각 규칙이 예외를 던지도록 설정
        doThrow(new IllegalArgumentException(dateError)).when(dateRule).apply(info);
        doThrow(new IllegalArgumentException(destError)).when(destinationRule).apply(info);

        // when
        validator.validate(info);
        List<String> errors = validator.getErrors();

        // then
        assertThat(errors).hasSize(2)
                .containsExactlyInAnyOrder(dateError, destError);
    }

    @Test
    @DisplayName("getErrors - 오류가 없으면 빈 리스트를 반환한다")
    void getErrors_shouldReturnEmptyList_whenNoErrors() {
        // given
        var info = new TravelFormSubmitRequest(null, null, null, null, null, null, null, null);
        // Mocking: 아무 예외도 던지지 않도록 설정 (기본 동작)
        doNothing().when(dateRule).apply(info);
        doNothing().when(destinationRule).apply(info);

        // when
        validator.validate(info);
        List<String> errors = validator.getErrors();

        // then
        assertThat(errors).isEmpty();
    }
}