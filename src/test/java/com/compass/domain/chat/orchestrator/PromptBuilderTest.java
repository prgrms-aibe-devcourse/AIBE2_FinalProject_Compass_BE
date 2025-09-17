package com.compass.domain.chat.orchestrator;

import com.compass.domain.chat.model.enums.Intent;
import com.compass.domain.chat.model.enums.TravelPhase;
import com.compass.domain.chat.model.request.ChatRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import static org.assertj.core.api.Assertions.assertThat;

// 프롬프트 빌더 테스트
class PromptBuilderTest {

    private PromptBuilder promptBuilder;

    @BeforeEach
    void setUp() {
        promptBuilder = new PromptBuilder();
    }

    @Test
    @DisplayName("기본 프롬프트 메시지 생성")
    void testBuildBasicPromptMessages() {
        // given
        var request = new ChatRequest();
        request.setMessage("제주도 여행 계획 짜줘");
        request.setThreadId("thread-1");
        request.setUserId("user-1");

        Intent intent = Intent.TRAVEL_INFO_COLLECTION;
        TravelPhase phase = TravelPhase.INITIALIZATION;

        // when
        var messages = promptBuilder.buildPromptMessages(request, intent, phase);

        // then
        assertThat(messages).hasSize(2); // 시스템 메시지 + 사용자 메시지
        assertThat(messages.get(0)).isInstanceOf(SystemMessage.class);
        assertThat(messages.get(1)).isInstanceOf(UserMessage.class);
        assertThat(messages.get(1).getContent()).isEqualTo("제주도 여행 계획 짜줘");
    }

    @Test
    @DisplayName("시스템 프롬프트 구성 - INITIALIZATION Phase")
    void testBuildSystemPromptInitialization() {
        // given
        Intent intent = Intent.GENERAL_CHAT;
        TravelPhase phase = TravelPhase.INITIALIZATION;

        // when
        var systemPrompt = promptBuilder.buildSystemPrompt(intent, phase);

        // then
        assertThat(systemPrompt).contains("친근하고 열정적인 여행 계획 전문가");
        assertThat(systemPrompt).contains("INITIALIZATION");
        assertThat(systemPrompt).contains("GENERAL_CHAT");
        assertThat(systemPrompt).contains("친근한 인사로 시작");
    }

    @Test
    @DisplayName("시스템 프롬프트 구성 - INFORMATION_COLLECTION Phase")
    void testBuildSystemPromptInfoCollection() {
        // given
        Intent intent = Intent.TRAVEL_INFO_COLLECTION;
        TravelPhase phase = TravelPhase.INFORMATION_COLLECTION;

        // when
        var systemPrompt = promptBuilder.buildSystemPrompt(intent, phase);

        // then
        assertThat(systemPrompt).contains("필수 정보 체계적으로 수집");
        assertThat(systemPrompt).contains("목적지, 날짜, 예산, 동행자");
    }

    @Test
    @DisplayName("각 Intent별 프롬프트 확인")
    void testIntentSpecificPrompts() {
        // given
        TravelPhase phase = TravelPhase.INITIALIZATION;

        // when & then
        // GENERAL_CHAT
        var generalPrompt = promptBuilder.buildSystemPrompt(Intent.GENERAL_CHAT, phase);
        assertThat(generalPrompt).contains("여행 관련 화제로 자연스럽게 전환");

        // TRAVEL_QUESTION
        var questionPrompt = promptBuilder.buildSystemPrompt(Intent.TRAVEL_QUESTION, phase);
        assertThat(questionPrompt).contains("여행 질문에 답하면서 전체 여행 계획의 필요성 제안");

        // TRAVEL_INFO_COLLECTION
        var collectionPrompt = promptBuilder.buildSystemPrompt(Intent.TRAVEL_INFO_COLLECTION, phase);
        assertThat(collectionPrompt).contains("본격적인 여행 정보 수집 시작");

        // UNKNOWN
        var unknownPrompt = promptBuilder.buildSystemPrompt(Intent.UNKNOWN, phase);
        assertThat(unknownPrompt).contains("사용자의 의도를 명확히 파악");
    }

    @Test
    @DisplayName("각 Phase별 프롬프트 확인")
    void testPhaseSpecificPrompts() {
        // given
        Intent intent = Intent.TRAVEL_INFO_COLLECTION;

        // when & then
        // PLAN_GENERATION
        var planPrompt = promptBuilder.buildSystemPrompt(intent, TravelPhase.PLAN_GENERATION);
        assertThat(planPrompt).contains("수집된 정보 기반 최적 일정 생성");
        assertThat(planPrompt).contains("일자별 세부 계획 제시");

        // FEEDBACK_REFINEMENT
        var feedbackPrompt = promptBuilder.buildSystemPrompt(intent, TravelPhase.FEEDBACK_REFINEMENT);
        assertThat(feedbackPrompt).contains("사용자 피드백 적극 수용");

        // COMPLETION
        var completionPrompt = promptBuilder.buildSystemPrompt(intent, TravelPhase.COMPLETION);
        assertThat(completionPrompt).contains("최종 계획 요약 제시");
    }

    @Test
    @DisplayName("사용자 프롬프트 구성")
    void testBuildUserPrompt() {
        // given
        String message = "제주도 3박 4일 여행";

        // when
        var userPrompt = promptBuilder.buildUserPrompt(message);

        // then
        assertThat(userPrompt).isEqualTo(message);
    }

    @Test
    @DisplayName("간단한 프롬프트 생성")
    void testBuildSimplePrompt() {
        // given
        String template = "목적지: %s, 기간: %s";
        String destination = "제주도";
        String duration = "3박4일";

        // when
        var result = promptBuilder.buildSimplePrompt(template, destination, duration);

        // then
        assertThat(result).isEqualTo("목적지: 제주도, 기간: 3박4일");
    }

    @Test
    @DisplayName("빈 메시지 처리")
    void testBuildPromptWithEmptyMessage() {
        // given
        var request = new ChatRequest();
        request.setMessage("");
        request.setThreadId("thread-1");
        request.setUserId("user-1");

        // when
        var messages = promptBuilder.buildPromptMessages(request, Intent.GENERAL_CHAT, TravelPhase.INITIALIZATION);

        // then
        assertThat(messages).hasSize(2);
        assertThat(messages.get(1).getContent()).isEmpty();
    }

    @Test
    @DisplayName("모든 Intent와 Phase 조합 테스트")
    void testAllIntentPhaseCombinations() {
        // given
        var request = new ChatRequest();
        request.setMessage("테스트 메시지");

        // when & then
        for (Intent intent : Intent.values()) {
            for (TravelPhase phase : TravelPhase.values()) {
                var messages = promptBuilder.buildPromptMessages(request, intent, phase);

                // 모든 조합에서 메시지 생성 확인
                assertThat(messages).hasSize(2);
                assertThat(messages.get(0)).isInstanceOf(SystemMessage.class);
                assertThat(messages.get(1)).isInstanceOf(UserMessage.class);

                // 시스템 프롬프트에 Intent와 Phase 정보 포함 확인
                var systemPrompt = ((SystemMessage) messages.get(0)).getContent();
                assertThat(systemPrompt).contains(intent.toString());
                assertThat(systemPrompt).contains(phase.toString());
            }
        }
    }
}