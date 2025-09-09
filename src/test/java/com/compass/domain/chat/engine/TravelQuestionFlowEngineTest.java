package com.compass.domain.chat.engine;

import com.compass.domain.chat.dto.FollowUpQuestionDto;
import com.compass.domain.chat.entity.TravelInfoCollectionState;
import com.compass.domain.chat.service.FollowUpQuestionGenerator;
import com.compass.domain.user.entity.User;
import com.compass.domain.user.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * TravelQuestionFlowEngine 단위 테스트
 * REQ-FOLLOW-001: 5개 필수 질문 순차 진행 테스트
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("TravelQuestionFlowEngine 테스트")
class TravelQuestionFlowEngineTest {
    
    @Mock
    private FollowUpQuestionGenerator questionGenerator;
    
    @InjectMocks
    private TravelQuestionFlowEngine flowEngine;
    
    private TravelInfoCollectionState testState;
    private User testUser;
    
    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@test.com")
                .nickname("testuser")
                .role(Role.USER)
                .build();
        ReflectionTestUtils.setField(testUser, "id", 1L);
        
        testState = TravelInfoCollectionState.builder()
                .user(testUser)
                .sessionId("TIC_TEST1234")
                .currentStep(TravelInfoCollectionState.CollectionStep.INITIAL)
                .originCollected(false)
                .destinationCollected(false)
                .datesCollected(false)
                .durationCollected(false)
                .companionsCollected(false)
                .budgetCollected(false)
                .build();
    }
    
    @Test
    @DisplayName("필수 질문 개수가 5개인지 확인한다")
    void testRequiredQuestionCount() {
        // When
        int count = flowEngine.getRequiredQuestionCount();
        
        // Then
        assertThat(count).isEqualTo(5); // 출발지 제외 시 5개
    }
    
    @Test
    @DisplayName("목적지 응답을 처리한다")
    void testProcessDestinationResponse() {
        // Given
        testState.setOriginCollected(true);
        testState.setCurrentStep(TravelInfoCollectionState.CollectionStep.DESTINATION);
        
        // When
        TravelInfoCollectionState result = flowEngine.processResponse(testState, "제주도");
        
        // Then
        assertThat(result.getDestination()).isEqualTo("제주도");
        assertThat(result.isDestinationCollected()).isTrue();
    }
    
    @Test
    @DisplayName("날짜 응답을 처리하고 기간을 자동 계산한다")
    void testProcessDateResponse() {
        // Given
        testState.setCurrentStep(TravelInfoCollectionState.CollectionStep.DATES);
        
        // When
        TravelInfoCollectionState result = flowEngine.processResponse(testState, "2024-03-15 ~ 2024-03-17");
        
        // Then
        assertThat(result.isDatesCollected()).isTrue();
        assertThat(result.isDurationCollected()).isTrue();
        assertThat(result.getDurationNights()).isEqualTo(2);
    }
    
    @Test
    @DisplayName("동행자 응답을 처리한다")
    void testProcessCompanionResponse() {
        // Given
        testState.setCurrentStep(TravelInfoCollectionState.CollectionStep.COMPANIONS);
        
        // When
        TravelInfoCollectionState result = flowEngine.processResponse(testState, "가족 4명");
        
        // Then
        assertThat(result.isCompanionsCollected()).isTrue();
        assertThat(result.getCompanionType()).isEqualTo("family");
        assertThat(result.getNumberOfTravelers()).isEqualTo(4);
    }
    
    @Test
    @DisplayName("예산 응답을 처리한다")
    void testProcessBudgetResponse() {
        // Given
        testState.setCurrentStep(TravelInfoCollectionState.CollectionStep.BUDGET);
        
        // When
        TravelInfoCollectionState result = flowEngine.processResponse(testState, "100만원");
        
        // Then
        assertThat(result.isBudgetCollected()).isTrue();
        assertThat(result.getBudgetPerPerson()).isEqualTo(1000000);
        assertThat(result.getBudgetCurrency()).isEqualTo("KRW");
    }
    
    @Test
    @DisplayName("예산 단계는 건너뛸 수 있다")
    void testCanSkipBudgetStep() {
        // Given
        testState.setCurrentStep(TravelInfoCollectionState.CollectionStep.BUDGET);
        
        // When
        boolean canSkip = flowEngine.canSkipCurrentStep(testState);
        
        // Then
        assertThat(canSkip).isTrue();
    }
    
    @Test
    @DisplayName("필수 정보가 모두 수집되면 플로우가 완료된다")
    void testFlowComplete() {
        // Given
        testState.setDestinationCollected(true);
        testState.setDatesCollected(true);
        testState.setDurationCollected(true);
        testState.setCompanionsCollected(true);
        testState.setBudgetCollected(true);
        
        // When
        boolean isComplete = flowEngine.isFlowComplete(testState);
        
        // Then
        assertThat(isComplete).isTrue();
    }
    
    @Test
    @DisplayName("목적지가 없으면 플로우 검증이 실패한다")
    void testFlowValidationFailsWithoutDestination() {
        // Given
        testState.setDatesCollected(true);
        testState.setCompanionsCollected(true);
        
        // When
        boolean isValid = flowEngine.validateFlow(testState);
        
        // Then
        assertThat(isValid).isFalse();
    }
    
    @Test
    @DisplayName("현재 단계 번호를 올바르게 반환한다")
    void testGetCurrentStepNumber() {
        // Given
        testState.setOriginCollected(true);
        testState.setDestinationCollected(true);
        testState.setDatesCollected(true);
        
        // When
        int stepNumber = flowEngine.getCurrentStepNumber(testState);
        
        // Then
        assertThat(stepNumber).isEqualTo(4); // 3개 완료, 다음은 4번째
    }
    
    @Test
    @DisplayName("다음 질문을 생성한다")
    void testGenerateNextQuestion() {
        // Given
        FollowUpQuestionDto mockQuestion = FollowUpQuestionDto.builder()
                .sessionId("TIC_TEST1234")
                .currentStep(TravelInfoCollectionState.CollectionStep.DESTINATION)
                .primaryQuestion("어디로 여행을 가시나요?")
                .build();
        
        when(questionGenerator.generateNextQuestion(any())).thenReturn(mockQuestion);
        
        // When
        FollowUpQuestionDto result = flowEngine.generateNextQuestion(testState);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPrimaryQuestion()).contains("어디로 여행");
    }
}