package com.compass.domain.chat.collection.service.validator;

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
class DefaultTravelInfoValidatorTest {

    @Mock
    private DateValidationRule dateRule;
    @Mock
    private DestinationValidationRule destinationRule;

    private DefaultTravelInfoValidator validator;

    @BeforeEach
    void setUp() {
        validator = new DefaultTravelInfoValidator(List.of(dateRule, destinationRule));
    }

    @Test
    @DisplayName("validate - 주입된 모든 검증 규칙들을 실행하고, 조건이 맞으면 validate도 실행한다")
    void validate_shouldExecuteAllInjectedRules() {
        // given
        var info = new TravelFormSubmitRequest(null, null, null, null, null, null, null, null, null, null);

        // Mocking: 모든 규칙이 적용 대상(true)이라고 설정
        when(dateRule.appliesTo(info)).thenReturn(true);
        when(destinationRule.appliesTo(info)).thenReturn(true);

        // when
        validator.validate(info);

        // then
        // ✅ 수정: 각 규칙의 appliesTo와 validate 메서드가 1번씩 호출되었는지 검증
        verify(dateRule).appliesTo(info);
        verify(dateRule).validate(info);
        verify(destinationRule).appliesTo(info);
        verify(destinationRule).validate(info);
    }

    @Test
    @DisplayName("getErrors - 각 규칙에서 발생한 모든 오류 메시지를 수집하여 반환한다")
    void getErrors_shouldCollectAllErrorMessagesFromRules() {
        // given
        var info = new TravelFormSubmitRequest(null, null, null, null, null, null, null, null, null, null);
        String dateError = "날짜를 입력해주세요.";
        String destError = "목적지를 입력해주세요.";

        // Mocking: 각 규칙이 적용 대상이며, 오류 메시지를 담은 Optional을 반환하도록 설정
        when(dateRule.appliesTo(info)).thenReturn(true);
        when(destinationRule.appliesTo(info)).thenReturn(true);
        when(dateRule.validate(info)).thenReturn(Optional.of(dateError));
        when(destinationRule.validate(info)).thenReturn(Optional.of(destError));

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
        var info = new TravelFormSubmitRequest(null, null, null, null, null, null, null, null, null, null);

        // Mocking: 오류가 없는 상황(빈 Optional 반환) 설정
        when(dateRule.appliesTo(info)).thenReturn(true);
        when(destinationRule.appliesTo(info)).thenReturn(true);
        when(dateRule.validate(info)).thenReturn(Optional.empty());
        when(destinationRule.validate(info)).thenReturn(Optional.empty());

        // when
        validator.validate(info);
        List<String> errors = validator.getErrors();

        // then
        assertThat(errors).isEmpty();
    }
}