package com.compass.domain.chat.orchestrator;

import com.compass.domain.chat.model.context.TravelContext;
import com.compass.domain.chat.model.enums.Intent;
import com.compass.domain.chat.model.enums.TravelPhase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// 프롬프트 빌더 테스트
class PromptBuilderTest {

    private PromptBuilder promptBuilder;
    private TravelContext context;

    @BeforeEach
    void setUp() {
        promptBuilder = new PromptBuilder();

        // 기본 TravelContext 설정
        context = TravelContext.builder()
            .threadId("thread-1")
            .userId("user-1")
            .currentPhase(TravelPhase.INITIALIZATION.name())
            .collectedInfo(new HashMap<>())
            .build();
    }

    @Test
    @DisplayName("시스템 프롬프트 생성 테스트")
    void testBuildSystemPrompt() {
        // given
        Intent intent = Intent.INFORMATION_COLLECTION;
        TravelPhase phase = TravelPhase.INITIALIZATION;

        // when
        var systemPrompt = promptBuilder.buildSystemPrompt(intent, phase, context);

        // then
        assertThat(systemPrompt).isNotNull();
        assertThat(systemPrompt).contains("현재 Intent: " + intent);
        assertThat(systemPrompt).contains("현재 Phase: " + phase);
        assertThat(systemPrompt).contains("당신은 전문 여행 계획 도우미입니다");
    }

    @Test
    @DisplayName("시스템 프롬프트 구성 - INITIALIZATION Phase")
    void testBuildSystemPromptInitialization() {
        // given
        Intent intent = Intent.GENERAL_QUESTION;
        TravelPhase phase = TravelPhase.INITIALIZATION;

        // when
        var systemPrompt = promptBuilder.buildSystemPrompt(intent, phase, context);

        // then
        assertThat(systemPrompt).contains("전문 여행 계획 도우미");
        assertThat(systemPrompt).contains("INITIALIZATION");
        assertThat(systemPrompt).contains("GENERAL_QUESTION");
        assertThat(systemPrompt).contains("친절하고 전문적인 톤 유지");
    }

    @Test
    @DisplayName("시스템 프롬프트 구성 - INFORMATION_COLLECTION Phase")
    void testBuildSystemPromptInfoCollection() {
        // given
        Intent intent = Intent.INFORMATION_COLLECTION;
        TravelPhase phase = TravelPhase.INFORMATION_COLLECTION;
        context.updatePhase(phase);

        // when
        var systemPrompt = promptBuilder.buildSystemPrompt(intent, phase, context);

        // then
        assertThat(systemPrompt).contains("본격적인 여행 정보 수집 시작");
    }

    @Test
    @DisplayName("각 Intent별 프롬프트 확인")
    void testIntentSpecificPrompts() {
        // given
        TravelPhase phase = TravelPhase.INITIALIZATION;

        // when & then
        // GENERAL_QUESTION
        var generalPrompt = promptBuilder.buildSystemPrompt(Intent.GENERAL_QUESTION, phase, context);
        assertThat(generalPrompt).contains("대화 전략");
        assertThat(generalPrompt).contains("여행 관련 화제로 자연스럽게 전환");

        // INFORMATION_COLLECTION
        var collectionPrompt = promptBuilder.buildSystemPrompt(Intent.INFORMATION_COLLECTION, phase, context);
        assertThat(collectionPrompt).contains("본격적인 여행 정보 수집 시작");

        // WEATHER_INQUIRY
        var weatherPrompt = promptBuilder.buildSystemPrompt(Intent.WEATHER_INQUIRY, phase, context);
        assertThat(weatherPrompt).contains("날씨 질문에 답하면서 전체 여행 계획의 필요성 제안");
    }

    @Test
    @DisplayName("각 Phase별 프롬프트 확인")
    void testPhaseSpecificPrompts() {
        // given
        Intent intent = Intent.INFORMATION_COLLECTION;

        // when & then
        // PLAN_GENERATION
        context.updatePhase(TravelPhase.PLAN_GENERATION);
        var planPrompt = promptBuilder.buildSystemPrompt(intent, TravelPhase.PLAN_GENERATION, context);
        assertThat(planPrompt).contains("수집된 정보 기반");
        assertThat(planPrompt).contains("계획");

        // FEEDBACK_REFINEMENT
        context.updatePhase(TravelPhase.FEEDBACK_REFINEMENT);
        var feedbackPrompt = promptBuilder.buildSystemPrompt(intent, TravelPhase.FEEDBACK_REFINEMENT, context);
        assertThat(feedbackPrompt).contains("사용자 피드백 적극 수용");

        // COMPLETION
        context.updatePhase(TravelPhase.COMPLETION);
        var completionPrompt = promptBuilder.buildSystemPrompt(intent, TravelPhase.COMPLETION, context);
        assertThat(completionPrompt).contains("최종 계획 요약 제시");
    }

    @Test
    @DisplayName("사용자 프롬프트 구성")
    void testBuildUserPrompt() {
        // given
        String message = "제주도 3박 4일 여행";

        // when
        var userPrompt = promptBuilder.buildUserPrompt(message, context);

        // then
        assertThat(userPrompt).contains(message);
        assertThat(userPrompt).contains("대화 히스토리");
        assertThat(userPrompt).contains("현재 여행 계획 진행률");
    }

    @Test
    @DisplayName("컨텍스트와 함께 프롬프트 생성")
    void testBuildPromptWithContext() {
        // given
        Intent intent = Intent.INFORMATION_COLLECTION;
        TravelPhase phase = TravelPhase.INFORMATION_COLLECTION;

        // 컨텍스트에 정보 추가
        context.updateCollectedInfo("destination", "제주도");
        context.updateCollectedInfo("budget", "100만원");

        // when
        var systemPrompt = promptBuilder.buildSystemPrompt(intent, phase, context);

        // then
        assertThat(systemPrompt).contains("제주도");
        assertThat(systemPrompt).contains("100만원");
        assertThat(systemPrompt).contains("추가로 필요한 정보");
    }

    @Test
    @DisplayName("빈 메시지 처리")
    void testBuildPromptWithEmptyMessage() {
        // given
        String message = "";

        // when
        var userPrompt = promptBuilder.buildUserPrompt(message, context);

        // then
        assertThat(userPrompt).contains("사용자 메시지: ");
        assertThat(userPrompt).contains("대화 히스토리: 0개");
    }

    @Test
    @DisplayName("모든 Intent와 Phase 조합 테스트")
    void testAllIntentPhaseCombinations() {
        // when & then
        for (Intent intent : Intent.values()) {
            for (TravelPhase phase : TravelPhase.values()) {
                context.updatePhase(phase);
                var systemPrompt = promptBuilder.buildSystemPrompt(intent, phase, context);

                // 모든 조합에서 프롬프트 생성 확인
                assertThat(systemPrompt).isNotNull();
                assertThat(systemPrompt).isNotEmpty();

                // 시스템 프롬프트에 Intent와 Phase 정보 포함 확인
                assertThat(systemPrompt).contains(intent.toString());
                assertThat(systemPrompt).contains(phase.toString());
            }
        }
    }
}