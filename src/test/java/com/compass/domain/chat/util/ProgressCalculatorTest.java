package com.compass.domain.chat.util;

import com.compass.domain.chat.entity.TravelInfoCollectionState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ProgressCalculator 유틸리티 클래스 테스트
 */
@Tag("unit")
class ProgressCalculatorTest {
    
    @Test
    @DisplayName("모든 필드가 비어있을 때 진행률은 0%이다")
    void calculateProgress_whenAllFieldsEmpty_returnsZero() {
        // given
        TravelInfoCollectionState state = TravelInfoCollectionState.builder()
                .sessionId("test-session")
                .build();
        
        // when
        int progress = ProgressCalculator.calculateProgress(state);
        
        // then
        assertThat(progress).isEqualTo(0);
    }
    
    @Test
    @DisplayName("모든 필드가 채워졌을 때 진행률은 100%이다")
    void calculateProgress_whenAllFieldsComplete_returnsHundred() {
        // given
        TravelInfoCollectionState state = TravelInfoCollectionState.builder()
                .sessionId("test-session")
                .originCollected(true)
                .destinationCollected(true)
                .datesCollected(true)
                .durationCollected(true)
                .companionsCollected(true)
                .budgetCollected(true)
                .build();
        
        // when
        int progress = ProgressCalculator.calculateProgress(state);
        
        // then
        assertThat(progress).isEqualTo(100);
    }
    
    @Test
    @DisplayName("절반의 필드가 채워졌을 때 진행률은 50%이다")
    void calculateProgress_whenHalfFieldsComplete_returnsFifty() {
        // given
        TravelInfoCollectionState state = TravelInfoCollectionState.builder()
                .sessionId("test-session")
                .destinationCollected(true)
                .datesCollected(true)
                .companionsCollected(true)
                .build();
        
        // when
        int progress = ProgressCalculator.calculateProgress(state);
        
        // then
        assertThat(progress).isEqualTo(50);
    }
    
    @Test
    @DisplayName("null 상태에 대해 진행률은 0%이다")
    void calculateProgress_whenStateIsNull_returnsZero() {
        // when
        int progress = ProgressCalculator.calculateProgress(null);
        
        // then
        assertThat(progress).isEqualTo(0);
    }
    
    @Test
    @DisplayName("필수 정보가 모두 있으면 여행 계획 생성이 가능하다")
    void canGenerateTravelPlan_whenRequiredFieldsPresent_returnsTrue() {
        // given
        TravelInfoCollectionState state = TravelInfoCollectionState.builder()
                .sessionId("test-session")
                .originCollected(true) // 출발지도 필수
                .destinationCollected(true)
                .datesCollected(true)
                .durationCollected(true) // 기간도 필수
                .companionsCollected(true)
                .budgetCollected(true)
                .build();

        // when
        boolean canGenerate = ProgressCalculator.canGenerateTravelPlan(state);

        // then
        assertThat(canGenerate).isTrue();
    }
    
    @Test
    @DisplayName("목적지가 없으면 여행 계획 생성이 불가능하다")
    void canGenerateTravelPlan_whenDestinationMissing_returnsFalse() {
        // given
        TravelInfoCollectionState state = TravelInfoCollectionState.builder()
                .sessionId("test-session")
                .datesCollected(true)
                .companionsCollected(true)
                .budgetCollected(true)
                .build();
        
        // when
        boolean canGenerate = ProgressCalculator.canGenerateTravelPlan(state);
        
        // then
        assertThat(canGenerate).isFalse();
    }
    
    @Test
    @DisplayName("날짜 정보가 없으면 여행 계획 생성이 불가능하다")
    void canGenerateTravelPlan_whenTimeInfoMissing_returnsFalse() {
        // given
        TravelInfoCollectionState state = TravelInfoCollectionState.builder()
                .sessionId("test-session")
                .destinationCollected(true)
                .companionsCollected(true)
                .budgetCollected(true)
                .build();
        
        // when
        boolean canGenerate = ProgressCalculator.canGenerateTravelPlan(state);
        
        // then
        assertThat(canGenerate).isFalse();
    }
    
    @Test
    @DisplayName("기간 정보만 있어도 시간 정보로 인정된다")
    void canGenerateTravelPlan_whenOnlyDurationPresent_returnsTrue() {
        // given
        TravelInfoCollectionState state = TravelInfoCollectionState.builder()
                .sessionId("test-session")
                .originCollected(true) // 출발지도 필수
                .destinationCollected(true)
                .durationCollected(true) // 날짜 대신 기간만 있음
                .companionsCollected(true)
                .budgetCollected(true)
                .build();

        // when
        boolean canGenerate = ProgressCalculator.canGenerateTravelPlan(state);

        // then
        assertThat(canGenerate).isTrue();
    }
    
    @Test
    @DisplayName("다음 필요한 단계를 올바르게 결정한다")
    void getNextRequiredStep_returnsCorrectStep() {
        // given
        TravelInfoCollectionState state = TravelInfoCollectionState.builder()
                .sessionId("test-session")
                .originCollected(true)
                .destinationCollected(true)
                .build();
        
        // when
        TravelInfoCollectionState.CollectionStep nextStep = 
            ProgressCalculator.getNextRequiredStep(state);
        
        // then
        assertThat(nextStep).isEqualTo(TravelInfoCollectionState.CollectionStep.DATES);
    }
    
    @Test
    @DisplayName("모든 정보가 수집되면 확인 단계를 반환한다")
    void getNextRequiredStep_whenAllCollected_returnsConfirmation() {
        // given
        TravelInfoCollectionState state = TravelInfoCollectionState.builder()
                .sessionId("test-session")
                .originCollected(true)
                .destinationCollected(true)
                .datesCollected(true)
                .durationCollected(true)
                .companionsCollected(true)
                .budgetCollected(true)
                .build();
        
        // when
        TravelInfoCollectionState.CollectionStep nextStep = 
            ProgressCalculator.getNextRequiredStep(state);
        
        // then
        assertThat(nextStep).isEqualTo(TravelInfoCollectionState.CollectionStep.CONFIRMATION);
    }
}