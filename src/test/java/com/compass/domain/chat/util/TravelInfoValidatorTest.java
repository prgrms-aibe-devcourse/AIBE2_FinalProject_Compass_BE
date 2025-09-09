package com.compass.domain.chat.util;

import com.compass.domain.chat.dto.ValidationResult;
import com.compass.domain.chat.entity.TravelInfoCollectionState;
import com.compass.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * REQ-FOLLOW-005: 필수 필드 입력 검증 테스트
 */
@Tag("unit")
@DisplayName("TravelInfoValidator 테스트")
class TravelInfoValidatorTest {
    
    private TravelInfoValidator validator;
    private TravelInfoCollectionState validState;
    private User mockUser;
    
    @BeforeEach
    void setUp() {
        validator = new TravelInfoValidator();
        mockUser = mock(User.class);
        
        // 유효한 상태 생성
        validState = TravelInfoCollectionState.builder()
                .user(mockUser)
                .sessionId("TEST_SESSION_001")
                .origin("서울")
                .originCollected(true)
                .destination("부산")
                .destinationCollected(true)
                .startDate(LocalDate.now().plusDays(7))
                .endDate(LocalDate.now().plusDays(9))
                .datesCollected(true)
                .durationNights(2)
                .durationCollected(true)
                .numberOfTravelers(2)
                .companionType("couple")
                .companionsCollected(true)
                .budgetPerPerson(500000)
                .budgetLevel("moderate")
                .budgetCurrency("KRW")
                .budgetCollected(true)
                .build();
    }
    
    @Test
    @DisplayName("모든 필드가 유효한 경우 검증 성공")
    void validateAllFieldsValid() {
        // when
        ValidationResult result = validator.validate(validState);
        
        // then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getFieldErrors()).isEmpty();
        assertThat(result.getCompletionPercentage()).isEqualTo(100);
    }
    
    @Test
    @DisplayName("필수 필드 누락시 검증 실패")
    void validateMissingRequiredFields() {
        // given
        TravelInfoCollectionState incompleteState = TravelInfoCollectionState.builder()
                .user(mockUser)
                .sessionId("TEST_SESSION_002")
                .destination("제주도")
                .destinationCollected(true)
                .build();
        
        // when
        ValidationResult result = validator.validate(incompleteState);
        
        // then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getFieldErrors()).isNotEmpty();
        assertThat(result.getIncompleteFields()).contains("origin", "dates", "duration", "companions", "budget");
        assertThat(result.getCompletionPercentage()).isLessThan(100);
    }
    
    @Test
    @DisplayName("빈 문자열 필드 검증 실패")
    void validateEmptyStringFields() {
        // given
        validState.setDestination("");
        
        // when
        ValidationResult result = validator.validate(validState);
        
        // then
        assertThat(result.isValid()).isFalse();
        assertThat(result.hasFieldError("destination")).isTrue();
        assertThat(result.getFieldErrors().get("destination")).contains("입력해주세요");
    }
    
    @Test
    @DisplayName("날짜 순서 검증 - 도착일이 출발일보다 빠른 경우")
    void validateDateOrder() {
        // given
        validState.setStartDate(LocalDate.now().plusDays(10));
        validState.setEndDate(LocalDate.now().plusDays(5));
        
        // when
        ValidationResult result = validator.validate(validState);
        
        // then
        assertThat(result.isValid()).isFalse();
        assertThat(result.hasFieldError("dates")).isTrue();
        assertThat(result.getFieldErrors().get("dates")).contains("도착일이 출발일보다 빠릅니다");
    }
    
    @Test
    @DisplayName("과거 날짜 검증 실패")
    void validatePastDates() {
        // given
        validState.setStartDate(LocalDate.now().minusDays(1));
        validState.setEndDate(LocalDate.now().plusDays(1));
        
        // when
        ValidationResult result = validator.validate(validState);
        
        // then
        assertThat(result.isValid()).isFalse();
        assertThat(result.hasFieldError("startDate")).isTrue();
        assertThat(result.getFieldErrors().get("startDate")).contains("오늘 이후여야 합니다");
    }
    
    @Test
    @DisplayName("여행 기간 범위 검증")
    void validateDurationRange() {
        // given - 너무 긴 기간
        validState.setDurationNights(400);
        
        // when
        ValidationResult result = validator.validate(validState);
        
        // then
        assertThat(result.isValid()).isFalse();
        assertThat(result.hasFieldError("duration")).isTrue();
        assertThat(result.getFieldErrors().get("duration")).contains("최대 365일까지");
    }
    
    @Test
    @DisplayName("동행자 인원 범위 검증")
    void validateTravelerCount() {
        // given - 너무 많은 인원
        validState.setNumberOfTravelers(25);
        
        // when
        ValidationResult result = validator.validate(validState);
        
        // then
        assertThat(result.isValid()).isFalse();
        assertThat(result.hasFieldError("companions")).isTrue();
        assertThat(result.getFieldErrors().get("companions")).contains("최대 20명까지");
    }
    
    @Test
    @DisplayName("예산 범위 검증")
    void validateBudgetRange() {
        // given - 너무 적은 예산
        validState.setBudgetPerPerson(5000);
        
        // when
        ValidationResult result = validator.validate(validState);
        
        // then
        assertThat(result.isValid()).isFalse();
        assertThat(result.hasFieldError("budget")).isTrue();
        assertThat(result.getFieldErrors().get("budget")).contains("최소");
    }
    
    @Test
    @DisplayName("STRICT 레벨 - 출발지와 목적지 동일 검증")
    void validateStrictLevelSameOriginDestination() {
        // given
        validState.setOrigin("서울");
        validState.setDestination("서울");
        
        // when
        ValidationResult result = validator.validate(validState, ValidationResult.ValidationLevel.STRICT);
        
        // then
        assertThat(result.isValid()).isFalse();
        assertThat(result.hasFieldError("destination")).isTrue();
        assertThat(result.getFieldErrors().get("destination")).contains("출발지와 목적지가 동일");
    }
    
    @Test
    @DisplayName("STRICT 레벨 - 동행자 타입과 인원 일치성 검증")
    void validateStrictCompanionConsistency() {
        // given - solo인데 2명
        validState.setCompanionType("solo");
        validState.setNumberOfTravelers(2);
        
        // when
        ValidationResult result = validator.validate(validState, ValidationResult.ValidationLevel.STRICT);
        
        // then
        assertThat(result.isValid()).isFalse();
        assertThat(result.hasFieldError("companions")).isTrue();
        assertThat(result.getFieldErrors().get("companions")).contains("혼자 여행시 인원은 1명");
    }
    
    @Test
    @DisplayName("BASIC 레벨 - 기본 검증만 수행")
    void validateBasicLevel() {
        // given - 짧은 목적지명 (BASIC에서는 통과)
        validState.setDestination("A");
        
        // when
        ValidationResult result = validator.validate(validState, ValidationResult.ValidationLevel.BASIC);
        
        // then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getFieldErrors()).isEmpty();
    }
    
    @Test
    @DisplayName("개선 제안 생성 검증")
    void validateSuggestions() {
        // given - 예산 정보가 없는 상태로 만들기
        validState.setBudgetPerPerson(null);
        validState.setBudgetLevel("luxury");
        validState.setBudgetCollected(false); // 예산이 수집되지 않은 상태로 변경
        
        // when
        ValidationResult result = validator.validate(validState);
        
        // then
        // 검증 실패시 제안이 생성되어야 함
        assertThat(result.isValid()).isFalse();
        assertThat(result.getSuggestions()).isNotEmpty();
        // 예산 수준에 대한 제안이 포함되어야 함
        boolean hasBudgetSuggestion = result.getSuggestions().stream()
                .anyMatch(s -> s.contains("럭셔리") || s.contains("예산") || s.contains("100만원"));
        assertThat(hasBudgetSuggestion).isTrue();
    }
    
    @Test
    @DisplayName("사용자 친화적 메시지 생성")
    void validateUserFriendlyMessage() {
        // given
        validState.setDestinationCollected(false);
        validState.setDatesCollected(false);
        
        // when
        ValidationResult result = validator.validate(validState);
        String message = result.getUserFriendlyMessage();
        
        // then
        assertThat(message).contains("다음 정보를 확인해 주세요");
        assertThat(message).contains("목적지");
        assertThat(message).contains("여행 날짜");
    }
    
    @Test
    @DisplayName("단일 필드 검증 - 유효한 값")
    void validateSingleFieldValid() {
        // when & then
        assertThat(validator.validateField("destination", "파리")).isTrue();
        assertThat(validator.validateField("duration", 5)).isTrue();
        assertThat(validator.validateField("budget", 300000)).isTrue();
    }
    
    @Test
    @DisplayName("단일 필드 검증 - 유효하지 않은 값")
    void validateSingleFieldInvalid() {
        // when & then
        assertThat(validator.validateField("destination", "")).isFalse();
        assertThat(validator.validateField("duration", 500)).isFalse();
        assertThat(validator.validateField("budget", 100)).isFalse();
        assertThat(validator.validateField("companions", 0)).isFalse();
    }
    
    @Test
    @DisplayName("날짜와 기간 일치성 검증")
    void validateDateDurationConsistency() {
        // given
        validState.setStartDate(LocalDate.now().plusDays(1));
        validState.setEndDate(LocalDate.now().plusDays(5));
        validState.setDurationNights(2); // 실제는 4박이어야 함
        
        // when
        ValidationResult result = validator.validate(validState);
        
        // then
        assertThat(result.isValid()).isFalse();
        assertThat(result.hasFieldError("duration")).isTrue();
        assertThat(result.getFieldErrors().get("duration")).contains("일치하지 않습니다");
    }
}