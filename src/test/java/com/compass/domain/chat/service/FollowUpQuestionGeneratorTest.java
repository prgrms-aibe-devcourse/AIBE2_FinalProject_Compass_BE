package com.compass.domain.chat.service;

import com.compass.domain.chat.dto.FollowUpQuestionDto;
import com.compass.domain.chat.entity.TravelInfoCollectionState;
import com.compass.domain.user.entity.User;
import com.compass.domain.user.enums.Role;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FollowUpQuestionGenerator 단위 테스트
 * REQ-FOLLOW-002: 후속 질문 생성 테스트
 */
@Tag("unit")
@DisplayName("FollowUpQuestionGenerator 테스트")
class FollowUpQuestionGeneratorTest {
    
    private FollowUpQuestionGenerator generator;
    private TravelInfoCollectionState testState;
    private User testUser;
    
    @BeforeEach
    void setUp() {
        generator = new FollowUpQuestionGenerator();
        
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
                .destinationCollected(false)
                .datesCollected(false)
                .durationCollected(false)
                .companionsCollected(false)
                .budgetCollected(false)
                .build();
    }
    
    @Test
    @DisplayName("목적지가 없을 때 목적지 질문을 생성한다")
    void testGenerateDestinationQuestion() {
        // When
        FollowUpQuestionDto question = generator.generateNextQuestion(testState);
        
        // Then
        assertThat(question).isNotNull();
        assertThat(question.getCurrentStep()).isEqualTo(TravelInfoCollectionState.CollectionStep.DESTINATION);
        assertThat(question.getPrimaryQuestion()).contains("어디로 여행");
        assertThat(question.getQuickOptions()).isNotEmpty();
        assertThat(question.isRequired()).isTrue();
        assertThat(question.isCanSkip()).isFalse();
    }
    
    @Test
    @DisplayName("목적지 수집 후 날짜 질문을 생성한다")
    void testGenerateDateQuestion() {
        // Given
        testState.setDestination("제주도");
        testState.setDestinationCollected(true);
        
        // When
        FollowUpQuestionDto question = generator.generateNextQuestion(testState);
        
        // Then
        assertThat(question).isNotNull();
        assertThat(question.getCurrentStep()).isEqualTo(TravelInfoCollectionState.CollectionStep.DATES);
        assertThat(question.getPrimaryQuestion()).contains("제주도").contains("언제");
        assertThat(question.getInputType()).isEqualTo("date-range");
    }
    
    @Test
    @DisplayName("날짜 수집 후 기간 질문을 생성한다")
    void testGenerateDurationQuestion() {
        // Given
        testState.setDestination("제주도");
        testState.setDestinationCollected(true);
        testState.setStartDate(LocalDate.now().plusDays(7));
        testState.setEndDate(LocalDate.now().plusDays(9));
        testState.setDatesCollected(true);
        
        // When
        FollowUpQuestionDto question = generator.generateNextQuestion(testState);
        
        // Then
        assertThat(question).isNotNull();
        assertThat(question.getCurrentStep()).isEqualTo(TravelInfoCollectionState.CollectionStep.DURATION);
        assertThat(question.getPrimaryQuestion()).contains("2박 3일");
        assertThat(question.getQuickOptions()).hasSize(5); // 당일치기 ~ 4박 이상
    }
    
    @Test
    @DisplayName("기간 수집 후 동행자 질문을 생성한다")
    void testGenerateCompanionQuestion() {
        // Given
        testState.setDestination("부산");
        testState.setDestinationCollected(true);
        testState.setDatesCollected(true);
        testState.setDurationNights(2);
        testState.setDurationCollected(true);
        
        // When
        FollowUpQuestionDto question = generator.generateNextQuestion(testState);
        
        // Then
        assertThat(question).isNotNull();
        assertThat(question.getCurrentStep()).isEqualTo(TravelInfoCollectionState.CollectionStep.COMPANIONS);
        assertThat(question.getPrimaryQuestion()).contains("누구와");
        assertThat(question.getQuickOptions()).hasSize(5); // 혼자, 연인, 가족, 친구, 비즈니스
        assertThat(question.getQuickOptions().get(0).getIcon()).isNotNull();
    }
    
    @Test
    @DisplayName("동행자 수집 후 예산 질문을 생성한다")
    void testGenerateBudgetQuestion() {
        // Given
        testState.setDestination("서울");
        testState.setDestinationCollected(true);
        testState.setDatesCollected(true);
        testState.setDurationNights(1);
        testState.setDurationCollected(true);
        testState.setNumberOfTravelers(2);
        testState.setCompanionType("couple");
        testState.setCompanionsCollected(true);
        
        // When
        FollowUpQuestionDto question = generator.generateNextQuestion(testState);
        
        // Then
        assertThat(question).isNotNull();
        assertThat(question.getCurrentStep()).isEqualTo(TravelInfoCollectionState.CollectionStep.BUDGET);
        assertThat(question.getPrimaryQuestion()).contains("예산");
        assertThat(question.isCanSkip()).isTrue(); // 예산은 선택사항
        assertThat(question.getExampleAnswers()).isNotEmpty();
    }
    
    @Test
    @DisplayName("모든 정보 수집 후 확인 질문을 생성한다")
    void testGenerateConfirmationQuestion() {
        // Given
        testState.setDestination("경주");
        testState.setDestinationCollected(true);
        testState.setStartDate(LocalDate.now().plusDays(10));
        testState.setEndDate(LocalDate.now().plusDays(12));
        testState.setDatesCollected(true);
        testState.setDurationNights(2);
        testState.setDurationCollected(true);
        testState.setNumberOfTravelers(4);
        testState.setCompanionType("family");
        testState.setCompanionsCollected(true);
        testState.setBudgetLevel("moderate");
        testState.setBudgetCollected(true);
        
        // When
        FollowUpQuestionDto question = generator.generateNextQuestion(testState);
        
        // Then
        assertThat(question).isNotNull();
        assertThat(question.getCurrentStep()).isEqualTo(TravelInfoCollectionState.CollectionStep.CONFIRMATION);
        assertThat(question.getPrimaryQuestion()).contains("맞나요");
        assertThat(question.getHelpText()).contains("경주");
        assertThat(question.getProgressPercentage()).isEqualTo(100);
        assertThat(question.getRemainingQuestions()).isEqualTo(0);
    }
    
    @Test
    @DisplayName("날짜가 설정된 경우 기간 질문을 건너뛸 수 있다")
    void testSkipDurationWhenDatesProvided() {
        // Given
        LocalDate startDate = LocalDate.now().plusDays(5);
        LocalDate endDate = LocalDate.now().plusDays(7);
        
        testState.setDestination("강릉");
        testState.setDestinationCollected(true);
        testState.setStartDate(startDate);
        testState.setEndDate(endDate);
        testState.setDatesCollected(true);
        
        // When
        FollowUpQuestionDto question = generator.generateNextQuestion(testState);
        
        // Then
        assertThat(question).isNotNull();
        assertThat(question.getCurrentStep()).isEqualTo(TravelInfoCollectionState.CollectionStep.DURATION);
        assertThat(question.getPrimaryQuestion()).contains("2박 3일");
        assertThat(question.isCanSkip()).isTrue();
    }
    
    @Test
    @DisplayName("컨텍스트 기반 질문에 수집된 정보가 포함된다")
    void testContextualQuestionGeneration() {
        // Given
        testState.setDestination("제주도");
        testState.setDestinationCollected(true);
        testState.setDurationNights(3);
        testState.setDurationCollected(true);
        
        // When
        FollowUpQuestionDto question = generator.generateNextQuestion(testState);
        
        // Then
        assertThat(question).isNotNull();
        assertThat(question.getCurrentStep()).isEqualTo(TravelInfoCollectionState.CollectionStep.DATES);
        assertThat(question.getCollectedInfo()).containsKey("destination");
        assertThat(question.getCollectedInfo().get("destination")).isEqualTo("제주도");
        assertThat(question.getCollectedInfo()).containsKey("durationNights");
    }
    
    @Test
    @DisplayName("진행률이 올바르게 계산된다")
    void testProgressCalculation() {
        // Given - 0% 완료
        FollowUpQuestionDto q1 = generator.generateNextQuestion(testState);
        assertThat(q1.getProgressPercentage()).isEqualTo(0);
        
        // 20% 완료 (1/5)
        testState.setDestinationCollected(true);
        FollowUpQuestionDto q2 = generator.generateNextQuestion(testState);
        assertThat(q2.getProgressPercentage()).isEqualTo(20);
        
        // 40% 완료 (2/5)
        testState.setDatesCollected(true);
        FollowUpQuestionDto q3 = generator.generateNextQuestion(testState);
        assertThat(q3.getProgressPercentage()).isEqualTo(40);
        
        // 60% 완료 (3/5)
        testState.setDurationCollected(true);
        FollowUpQuestionDto q4 = generator.generateNextQuestion(testState);
        assertThat(q4.getProgressPercentage()).isEqualTo(60);
        
        // 80% 완료 (4/5)
        testState.setCompanionsCollected(true);
        FollowUpQuestionDto q5 = generator.generateNextQuestion(testState);
        assertThat(q5.getProgressPercentage()).isEqualTo(80);
        
        // 100% 완료 (5/5) - 확인 질문에 필요한 실제 데이터 설정
        testState.setDestination("제주도");
        testState.setStartDate(LocalDate.now().plusDays(7));
        testState.setEndDate(LocalDate.now().plusDays(9));
        testState.setDurationNights(2);
        testState.setNumberOfTravelers(2);
        testState.setCompanionType("couple");
        testState.setBudgetLevel("moderate");
        testState.setBudgetCollected(true);
        FollowUpQuestionDto q6 = generator.generateNextQuestion(testState);
        assertThat(q6.getProgressPercentage()).isEqualTo(100);
    }
}