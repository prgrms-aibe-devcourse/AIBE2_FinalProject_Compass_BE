package com.compass.domain.chat.service;

import com.compass.domain.chat.dto.FollowUpQuestionDto;
import com.compass.domain.chat.entity.TravelInfoCollectionState;
import com.compass.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ClarificationQuestionGenerator 단위 테스트
 * REQ-FOLLOW-006: 재질문 로직 테스트
 */
@Tag("unit")
@DisplayName("재질문 생성 서비스 테스트")
class ClarificationQuestionGeneratorTest {
    
    private ClarificationQuestionGenerator generator;
    private TravelInfoCollectionState state;
    
    @BeforeEach
    void setUp() {
        generator = new ClarificationQuestionGenerator();
        
        // 테스트용 상태 생성 (User는 필수 필드가 많아서 null로 처리)
        state = TravelInfoCollectionState.builder()
                .sessionId("TEST_SESSION_001")
                .currentStep(TravelInfoCollectionState.CollectionStep.DESTINATION)
                .isCompleted(false)
                .build();
    }
    
    @Test
    @DisplayName("목적지 파싱 실패 시 첫 번째 재질문 생성")
    void generateDestinationClarification_FirstRetry() {
        // given
        String failedField = "destination";
        String originalResponse = "어디든 좋아요";
        int retryCount = 0;
        
        // when
        FollowUpQuestionDto question = generator.generateClarificationQuestion(
            state, failedField, originalResponse, retryCount);
        
        // then
        assertThat(question).isNotNull();
        assertThat(question.getSessionId()).isEqualTo("TEST_SESSION_001");
        assertThat(question.getPrimaryQuestion()).contains("목적지를 알려주세요");
        assertThat(question.isClarification()).isTrue();
        assertThat(question.getRetryCount()).isEqualTo(0);
        assertThat(question.getOriginalResponse()).isEqualTo(originalResponse);
        assertThat(question.getQuickOptions()).isNotNull();
        assertThat(question.getQuickOptions()).hasSize(6); // 서울, 부산, 제주, 강릉, 경주, 직접입력
    }
    
    @Test
    @DisplayName("날짜 파싱 실패 시 재질문 생성")
    void generateDatesClarification() {
        // given
        state.setCurrentStep(TravelInfoCollectionState.CollectionStep.DATES);
        String failedField = "dates";
        String originalResponse = "곧 가려고요";
        int retryCount = 0;
        
        // when
        FollowUpQuestionDto question = generator.generateClarificationQuestion(
            state, failedField, originalResponse, retryCount);
        
        // then
        assertThat(question).isNotNull();
        assertThat(question.getPrimaryQuestion()).contains("여행 날짜를 알려주세요");
        assertThat(question.getHelpText()).contains("여행 시작일과 종료일");
        assertThat(question.getExampleAnswers()).isNotNull();
        assertThat(question.getExampleAnswers()).contains("12월 25일 ~ 12월 27일");
    }
    
    @Test
    @DisplayName("동행자 파싱 실패 시 재질문 생성")
    void generateCompanionsClarification() {
        // given
        state.setCurrentStep(TravelInfoCollectionState.CollectionStep.COMPANIONS);
        String failedField = "companions";
        String originalResponse = "사람들이랑";
        int retryCount = 1; // 두 번째 시도
        
        // when
        FollowUpQuestionDto question = generator.generateClarificationQuestion(
            state, failedField, originalResponse, retryCount);
        
        // then
        assertThat(question).isNotNull();
        assertThat(question.getPrimaryQuestion()).contains("동행자를 다시 알려주세요");
        assertThat(question.getQuickOptions()).isNotNull();
        assertThat(question.getQuickOptions())
            .extracting("label")
            .contains("혼자", "연인/배우자", "가족", "친구");
    }
    
    @Test
    @DisplayName("예산 파싱 실패 시 재질문 생성")
    void generateBudgetClarification() {
        // given
        state.setCurrentStep(TravelInfoCollectionState.CollectionStep.BUDGET);
        String failedField = "budget";
        String originalResponse = "적당히";
        int retryCount = 0;
        
        // when
        FollowUpQuestionDto question = generator.generateClarificationQuestion(
            state, failedField, originalResponse, retryCount);
        
        // then
        assertThat(question).isNotNull();
        assertThat(question.getPrimaryQuestion()).contains("예산을 알려주세요");
        assertThat(question.getQuickOptions()).isNotNull();
        assertThat(question.getQuickOptions())
            .extracting("value")
            .contains("budget", "moderate", "luxury");
    }
    
    @Test
    @DisplayName("최대 재시도 횟수 초과 시 기본값 제안")
    void generateDefaultSuggestion_AfterMaxRetries() {
        // given
        String failedField = "destination";
        String originalResponse = "모르겠어요";
        int retryCount = 3; // MAX_RETRY_COUNT
        
        // when
        FollowUpQuestionDto question = generator.generateClarificationQuestion(
            state, failedField, originalResponse, retryCount);
        
        // then
        assertThat(question).isNotNull();
        assertThat(question.getPrimaryQuestion()).contains("기본값");
        assertThat(question.getPrimaryQuestion()).contains("서울");
        assertThat(question.getQuickOptions()).hasSize(2);
        assertThat(question.getQuickOptions())
            .extracting("value")
            .contains("서울", "retry");
        assertThat(question.isCanSkip()).isTrue();
    }
    
    @Test
    @DisplayName("재시도 횟수에 따라 다른 질문 생성")
    void generateDifferentQuestions_BasedOnRetryCount() {
        // given
        String failedField = "destination";
        String originalResponse = "그냥";
        
        // when & then - 첫 번째 시도
        FollowUpQuestionDto question1 = generator.generateClarificationQuestion(
            state, failedField, originalResponse, 0);
        assertThat(question1.getPrimaryQuestion()).contains("목적지를 알려주세요");
        
        // when & then - 두 번째 시도
        FollowUpQuestionDto question2 = generator.generateClarificationQuestion(
            state, failedField, originalResponse, 1);
        assertThat(question2.getPrimaryQuestion()).contains("여행 목적지를 다시 입력해주세요");
        
        // when & then - 세 번째 시도
        FollowUpQuestionDto question3 = generator.generateClarificationQuestion(
            state, failedField, originalResponse, 2);
        assertThat(question3.getPrimaryQuestion()).contains("정확한 도시명을 입력해주세요");
    }
    
    @Test
    @DisplayName("출발지 재질문 생성")
    void generateOriginClarification() {
        // given
        state.setCurrentStep(TravelInfoCollectionState.CollectionStep.ORIGIN);
        String failedField = "origin";
        String originalResponse = "집에서";
        int retryCount = 0;
        
        // when
        FollowUpQuestionDto question = generator.generateClarificationQuestion(
            state, failedField, originalResponse, retryCount);
        
        // then
        assertThat(question).isNotNull();
        assertThat(question.getPrimaryQuestion()).contains("출발지를 알려주세요");
        assertThat(question.getQuickOptions()).isNotNull();
        assertThat(question.getQuickOptions()).hasSize(6); // 5개 도시 + 직접입력
    }
    
    @Test
    @DisplayName("알 수 없는 필드에 대한 일반 재질문 생성")
    void generateGenericClarification() {
        // given
        String failedField = "unknown";
        String originalResponse = "잘 모르겠어요";
        int retryCount = 0;
        
        // when
        FollowUpQuestionDto question = generator.generateClarificationQuestion(
            state, failedField, originalResponse, retryCount);
        
        // then
        assertThat(question).isNotNull();
        assertThat(question.getPrimaryQuestion()).contains("unknown 정보를 알려주세요");
        assertThat(question.getInputType()).isEqualTo("text");
        assertThat(question.isClarification()).isTrue();
    }
}