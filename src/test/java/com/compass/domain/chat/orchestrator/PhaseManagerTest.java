package com.compass.domain.chat.orchestrator;

import com.compass.domain.chat.model.context.TravelContext;
import com.compass.domain.chat.model.enums.Intent;
import com.compass.domain.chat.model.enums.TravelPhase;
import com.compass.domain.chat.orchestrator.cache.PhaseCache;
import com.compass.domain.chat.orchestrator.persistence.PhasePersistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhaseManagerTest {

    private PhaseManager phaseManager;

    @Mock
    private PhaseCache phaseCache;

    @Mock
    private PhasePersistence phasePersistence;

    @Mock
    private ContextManager contextManager;

    private static final String TEST_THREAD_ID = "test-thread-123";

    @BeforeEach
    void setUp() {
        phaseManager = new PhaseManager(phaseCache, phasePersistence, contextManager);
    }

    @Test
    @DisplayName("새로운 Thread는 INITIALIZATION Phase로 시작한다")
    void testNewThreadStartsWithInitializationPhase() {
        // given
        when(phaseCache.get(TEST_THREAD_ID)).thenReturn(Optional.empty());
        when(phasePersistence.findByThreadId(TEST_THREAD_ID)).thenReturn(Optional.empty());

        // when
        TravelPhase phase = phaseManager.getCurrentPhase(TEST_THREAD_ID);

        // then
        assertThat(phase).isEqualTo(TravelPhase.INITIALIZATION);
        verify(phaseCache).put(TEST_THREAD_ID, TravelPhase.INITIALIZATION);
    }

    @Test
    @DisplayName("Cache에 저장된 Phase를 정상적으로 조회한다")
    void testGetCurrentPhaseFromCache() {
        // given
        when(phaseCache.get(TEST_THREAD_ID)).thenReturn(Optional.of(TravelPhase.INFORMATION_COLLECTION));

        // when
        TravelPhase phase = phaseManager.getCurrentPhase(TEST_THREAD_ID);

        // then
        assertThat(phase).isEqualTo(TravelPhase.INFORMATION_COLLECTION);
        verify(phaseCache).get(TEST_THREAD_ID);
        verify(phasePersistence, never()).findByThreadId(any());
    }

    @Test
    @DisplayName("Phase를 저장한다")
    void testSavePhase() {
        // when
        phaseManager.savePhase(TEST_THREAD_ID, TravelPhase.PLAN_GENERATION);

        // then
        verify(phaseCache).put(TEST_THREAD_ID, TravelPhase.PLAN_GENERATION);
        verify(phasePersistence).save(TEST_THREAD_ID, TravelPhase.PLAN_GENERATION);
    }

    @Test
    @DisplayName("INITIALIZATION에서 정보수집 Intent시 INFORMATION_COLLECTION으로 전환")
    void testTransitionFromInitializationToInfoCollection() {
        // given
        when(phaseCache.get(TEST_THREAD_ID)).thenReturn(Optional.empty());
        when(phasePersistence.findByThreadId(TEST_THREAD_ID)).thenReturn(Optional.empty());

        TravelContext context = TravelContext.builder()
            .threadId(TEST_THREAD_ID)
            .userId("user-1")
            .conversationCount(0)
            .waitingForTravelConfirmation(true)  // 확인 대기 상태로 설정
            .build();

        // when
        TravelPhase nextPhase = phaseManager.transitionPhase(
            TEST_THREAD_ID,
            Intent.CONFIRMATION,  // CONFIRMATION Intent로 변경
            context
        );

        // then
        assertThat(nextPhase).isEqualTo(TravelPhase.INFORMATION_COLLECTION);
        verify(phaseCache, times(2)).put(eq(TEST_THREAD_ID), any());
    }

    @Test
    @DisplayName("정보수집 완료시 PLAN_GENERATION으로 전환")
    void testTransitionFromInfoCollectionToPlanGeneration() {
        // given
        when(phaseCache.get(TEST_THREAD_ID)).thenReturn(Optional.of(TravelPhase.INFORMATION_COLLECTION));

        var collectedInfo = new java.util.HashMap<String, Object>();
        collectedInfo.put("destination", "제주도");
        collectedInfo.put("startDate", "2024-03-01");
        collectedInfo.put("endDate", "2024-03-03");
        collectedInfo.put("budget", 1000000);

        TravelContext context = TravelContext.builder()
            .threadId(TEST_THREAD_ID)
            .userId("user-1")
            .collectedInfo(collectedInfo)
            .build();

        // when
        TravelPhase nextPhase = phaseManager.transitionPhase(
            TEST_THREAD_ID,
            Intent.DESTINATION_SEARCH,
            context
        );

        // then
        assertThat(nextPhase).isEqualTo(TravelPhase.PLAN_GENERATION);
        verify(phaseCache).put(TEST_THREAD_ID, TravelPhase.PLAN_GENERATION);
    }

    @Test
    @DisplayName("계획 생성 완료시 FEEDBACK_REFINEMENT로 전환")
    void testTransitionFromPlanGenerationToFeedback() {
        // given
        when(phaseCache.get(TEST_THREAD_ID)).thenReturn(Optional.of(TravelPhase.PLAN_GENERATION));

        var travelPlan = new java.util.HashMap<String, Object>();
        travelPlan.put("itinerary", "day1...");

        TravelContext context = TravelContext.builder()
            .threadId(TEST_THREAD_ID)
            .userId("user-1")
            .travelPlan(travelPlan)
            .build();

        // when
        TravelPhase nextPhase = phaseManager.transitionPhase(
            TEST_THREAD_ID,
            Intent.PLAN_MODIFICATION,
            context
        );

        // then
        assertThat(nextPhase).isEqualTo(TravelPhase.FEEDBACK_REFINEMENT);
        verify(phaseCache).put(TEST_THREAD_ID, TravelPhase.FEEDBACK_REFINEMENT);
    }

    @Test
    @DisplayName("사용자 만족시 COMPLETION으로 전환")
    void testTransitionFromFeedbackToCompletion() {
        // given
        when(phaseCache.get(TEST_THREAD_ID)).thenReturn(Optional.of(TravelPhase.FEEDBACK_REFINEMENT));

        TravelContext context = TravelContext.builder()
            .threadId(TEST_THREAD_ID)
            .userId("user-1")
            .build();

        // when
        TravelPhase nextPhase = phaseManager.transitionPhase(
            TEST_THREAD_ID,
            Intent.COMPLETION,
            context
        );

        // then
        assertThat(nextPhase).isEqualTo(TravelPhase.COMPLETION);
        verify(phaseCache).put(TEST_THREAD_ID, TravelPhase.COMPLETION);
    }

    @Test
    @DisplayName("Phase별 유효한 Intent 검증")
    void testValidIntentForPhase() {
        // Mock 설정 제거 (static 메서드라 Mock 필요 없음)

        // INITIALIZATION - 모든 Intent 허용
        assertThat(phaseManager.isValidIntentForPhase(
            TravelPhase.INITIALIZATION, Intent.GENERAL_QUESTION
        )).isTrue();

        // INFORMATION_COLLECTION
        assertThat(phaseManager.isValidIntentForPhase(
            TravelPhase.INFORMATION_COLLECTION, Intent.INFORMATION_COLLECTION
        )).isTrue();

        assertThat(phaseManager.isValidIntentForPhase(
            TravelPhase.INFORMATION_COLLECTION, Intent.DESTINATION_SEARCH
        )).isFalse();

        // PLAN_GENERATION
        assertThat(phaseManager.isValidIntentForPhase(
            TravelPhase.PLAN_GENERATION, Intent.DESTINATION_SEARCH
        )).isTrue();

        // FEEDBACK_REFINEMENT
        assertThat(phaseManager.isValidIntentForPhase(
            TravelPhase.FEEDBACK_REFINEMENT, Intent.PLAN_MODIFICATION
        )).isTrue();

        // COMPLETION
        assertThat(phaseManager.isValidIntentForPhase(
            TravelPhase.COMPLETION, Intent.COMPLETION
        )).isTrue();
    }

    @Test
    @DisplayName("Phase 초기화 테스트")
    void testResetPhase() {
        // when
        phaseManager.resetPhase(TEST_THREAD_ID);

        // then
        verify(phaseCache).put(TEST_THREAD_ID, TravelPhase.INITIALIZATION);
        verify(phasePersistence).save(TEST_THREAD_ID, TravelPhase.INITIALIZATION);
    }

    @Test
    @DisplayName("여행 질문 반복시 정보수집으로 전환")
    void testRepeatedTravelQuestionTransition() {
        // given
        when(phaseCache.get(TEST_THREAD_ID)).thenReturn(Optional.empty());
        when(phasePersistence.findByThreadId(TEST_THREAD_ID)).thenReturn(Optional.empty());

        TravelContext context = TravelContext.builder()
            .threadId(TEST_THREAD_ID)
            .userId("user-1")
            .conversationCount(3) // 반복된 대화
            .waitingForTravelConfirmation(true)  // 확인 대기 상태로 설정
            .build();

        // when
        TravelPhase nextPhase = phaseManager.transitionPhase(
            TEST_THREAD_ID,
            Intent.CONFIRMATION,  // 여행을 시작하려는 확인 Intent
            context
        );

        // then
        assertThat(nextPhase).isEqualTo(TravelPhase.INFORMATION_COLLECTION);
        verify(phaseCache, times(2)).put(eq(TEST_THREAD_ID), any());
    }
}