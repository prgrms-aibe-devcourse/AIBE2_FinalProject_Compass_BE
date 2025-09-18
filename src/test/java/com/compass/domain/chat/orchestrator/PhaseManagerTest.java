package com.compass.domain.chat.orchestrator;

import com.compass.domain.chat.model.context.TravelContext;
import com.compass.domain.chat.model.enums.Intent;
import com.compass.domain.chat.model.enums.TravelPhase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhaseManagerTest {

    @InjectMocks
    private PhaseManager phaseManager;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private static final String TEST_THREAD_ID = "test-thread-123";

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("새로운 Thread는 INITIALIZATION Phase로 시작한다")
    void testNewThreadStartsWithInitializationPhase() {
        // given
        when(valueOperations.get(anyString())).thenReturn(null);

        // when
        TravelPhase phase = phaseManager.getCurrentPhase(TEST_THREAD_ID);

        // then
        assertThat(phase).isEqualTo(TravelPhase.INITIALIZATION);
    }

    @Test
    @DisplayName("Redis에 저장된 Phase를 정상적으로 조회한다")
    void testGetCurrentPhaseFromRedis() {
        // given
        Map<String, Object> phaseData = new HashMap<>();
        phaseData.put("phase", "INFORMATION_COLLECTION");
        when(valueOperations.get(anyString())).thenReturn(phaseData);

        // when
        TravelPhase phase = phaseManager.getCurrentPhase(TEST_THREAD_ID);

        // then
        assertThat(phase).isEqualTo(TravelPhase.INFORMATION_COLLECTION);
    }

    @Test
    @DisplayName("Phase를 Redis에 저장한다")
    void testSavePhaseToRedis() {
        // when
        phaseManager.savePhase(TEST_THREAD_ID, TravelPhase.PLAN_GENERATION);

        // then
        verify(valueOperations).set(
            eq("phase:thread:" + TEST_THREAD_ID),
            argThat(map -> {
                Map<String, Object> data = (Map<String, Object>) map;
                return "PLAN_GENERATION".equals(data.get("phase"));
            }),
            eq(Duration.ofHours(24))
        );
    }

    @Test
    @DisplayName("INITIALIZATION에서 정보수집 Intent시 INFORMATION_COLLECTION으로 전환")
    void testTransitionFromInitializationToInfoCollection() {
        // given
        when(valueOperations.get(anyString())).thenReturn(null);
        TravelContext context = TravelContext.builder()
            .threadId(TEST_THREAD_ID)
            .conversationCount(0)
            .build();

        // when
        TravelPhase nextPhase = phaseManager.transitionPhase(
            TEST_THREAD_ID,
            Intent.TRAVEL_PLANNING,
            context
        );

        // then
        assertThat(nextPhase).isEqualTo(TravelPhase.INFORMATION_COLLECTION);
        verify(valueOperations).set(anyString(), any(), any(Duration.class));
    }

    @Test
    @DisplayName("정보수집 완료시 PLAN_GENERATION으로 전환")
    void testTransitionFromInfoCollectionToPlanGeneration() {
        // given
        Map<String, Object> phaseData = new HashMap<>();
        phaseData.put("phase", "INFORMATION_COLLECTION");
        when(valueOperations.get(anyString())).thenReturn(phaseData);

        Map<String, Object> collectedInfo = new HashMap<>();
        collectedInfo.put("destination", "제주도");
        collectedInfo.put("startDate", "2024-03-01");
        collectedInfo.put("endDate", "2024-03-03");
        collectedInfo.put("budget", 1000000);

        TravelContext context = TravelContext.builder()
            .threadId(TEST_THREAD_ID)
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
    }

    @Test
    @DisplayName("계획 생성 완료시 FEEDBACK_REFINEMENT로 전환")
    void testTransitionFromPlanGenerationToFeedback() {
        // given
        Map<String, Object> phaseData = new HashMap<>();
        phaseData.put("phase", "PLAN_GENERATION");
        when(valueOperations.get(anyString())).thenReturn(phaseData);

        Map<String, Object> travelPlan = new HashMap<>();
        travelPlan.put("itinerary", "day1...");

        TravelContext context = TravelContext.builder()
            .threadId(TEST_THREAD_ID)
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
    }

    @Test
    @DisplayName("사용자 만족시 COMPLETION으로 전환")
    void testTransitionFromFeedbackToCompletion() {
        // given
        Map<String, Object> phaseData = new HashMap<>();
        phaseData.put("phase", "FEEDBACK_REFINEMENT");
        when(valueOperations.get(anyString())).thenReturn(phaseData);

        TravelContext context = TravelContext.builder()
            .threadId(TEST_THREAD_ID)
            .build();

        // when
        TravelPhase nextPhase = phaseManager.transitionPhase(
            TEST_THREAD_ID,
            Intent.COMPLETION,
            context
        );

        // then
        assertThat(nextPhase).isEqualTo(TravelPhase.COMPLETION);
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
        verify(valueOperations).set(
            eq("phase:thread:" + TEST_THREAD_ID),
            argThat(map -> {
                Map<String, Object> data = (Map<String, Object>) map;
                return "INITIALIZATION".equals(data.get("phase"));
            }),
            eq(Duration.ofHours(24))
        );
    }

    @Test
    @DisplayName("여행 질문 반복시 정보수집으로 전환")
    void testRepeatedTravelQuestionTransition() {
        // given
        when(valueOperations.get(anyString())).thenReturn(null);
        TravelContext context = TravelContext.builder()
            .threadId(TEST_THREAD_ID)
            .conversationCount(3) // 반복된 대화
            .build();

        // when
        TravelPhase nextPhase = phaseManager.transitionPhase(
            TEST_THREAD_ID,
            Intent.GENERAL_QUESTION,
            context
        );

        // then
        assertThat(nextPhase).isEqualTo(TravelPhase.INFORMATION_COLLECTION);
    }
}