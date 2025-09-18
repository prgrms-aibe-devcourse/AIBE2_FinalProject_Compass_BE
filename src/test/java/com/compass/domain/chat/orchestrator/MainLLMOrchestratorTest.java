package com.compass.domain.chat.orchestrator;

import com.compass.domain.chat.model.context.TravelContext;
import com.compass.domain.chat.model.enums.Intent;
import com.compass.domain.chat.model.enums.TravelPhase;
import com.compass.domain.chat.model.request.ChatRequest;
import com.compass.domain.chat.model.response.ChatResponse;
import com.compass.domain.chat.service.ChatThreadService;
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

// MainLLMOrchestrator 테스트
@ExtendWith(MockitoExtension.class)
class MainLLMOrchestratorTest {

    private MainLLMOrchestrator orchestrator;

    @Mock
    private IntentClassifier intentClassifier;

    @Mock
    private PhaseManager phaseManager;

    @Mock
    private ChatThreadService chatThreadService;

    @Mock
    private ContextManager contextManager;

    @Mock
    private PromptBuilder promptBuilder;

    @Mock
    private ResponseGenerator responseGenerator;

    @BeforeEach
    void setUp() {
        orchestrator = new MainLLMOrchestrator(
            intentClassifier,
            phaseManager,
            chatThreadService,
            contextManager,
            promptBuilder,
            responseGenerator
        );
    }

    @Test
    @DisplayName("긍정적 자연어 응답 처리 - 다음 Phase로 전환")
    void testHandlePositiveNaturalLanguageResponse() {
        // given
        var request = createChatRequest("네, 시작할게요!");
        var context = TravelContext.builder()
            .threadId("thread-1")
            .userId("user-1")
            .currentPhase(TravelPhase.INITIALIZATION.name())
            .build();

        when(contextManager.getOrCreateContext(request)).thenReturn(context);

        var nextPhaseResponse = ChatResponse.builder()
            .content("여행 정보를 수집합니다.")
            .type("TEXT")
            .nextAction("COLLECT_MORE_INFO")
            .requiresConfirmation(true)
            .build();

        when(responseGenerator.generateResponse(
            eq(request),
            eq(Intent.TRAVEL_INFO_COLLECTION),
            eq(TravelPhase.INFORMATION_COLLECTION),
            eq(context)
        )).thenReturn(nextPhaseResponse);

        // when
        var response = orchestrator.processChat(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).isEqualTo("여행 정보를 수집합니다.");
        assertThat(context.getCurrentPhase()).isEqualTo(TravelPhase.INFORMATION_COLLECTION.name());
        verify(contextManager).updateContext(context);
        verify(responseGenerator).generateResponse(
            eq(request),
            eq(Intent.TRAVEL_INFO_COLLECTION),
            eq(TravelPhase.INFORMATION_COLLECTION),
            eq(context)
        );
    }

    @Test
    @DisplayName("다양한 긍정 표현 처리")
    void testHandleVariousPositiveExpressions() {
        // given
        var request = createChatRequest("좋아요, 진행해주세요");
        var context = TravelContext.builder()
            .threadId("thread-1")
            .userId("user-1")
            .currentPhase(TravelPhase.INFORMATION_COLLECTION.name())
            .build();

        when(contextManager.getOrCreateContext(request)).thenReturn(context);

        var nextPhaseResponse = ChatResponse.builder()
            .content("계획을 생성합니다.")
            .type("TEXT")
            .nextAction("GENERATE_PLAN")
            .requiresConfirmation(true)
            .build();

        when(responseGenerator.generateResponse(
            eq(request),
            eq(Intent.TRAVEL_INFO_COLLECTION),
            eq(TravelPhase.PLAN_GENERATION),
            eq(context)
        )).thenReturn(nextPhaseResponse);

        // when
        var response = orchestrator.processChat(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).isEqualTo("계획을 생성합니다.");
        assertThat(context.getCurrentPhase()).isEqualTo(TravelPhase.PLAN_GENERATION.name());
        verify(contextManager).updateContext(context);
    }

    @Test
    @DisplayName("부정적 자연어 응답 처리 - 현재 Phase 유지")
    void testHandleNegativeNaturalLanguageResponse() {
        // given
        var request = createChatRequest("아니요, 다시 생각해볼게요");
        var context = TravelContext.builder()
            .threadId("thread-1")
            .userId("user-1")
            .currentPhase(TravelPhase.INITIALIZATION.name())
            .build();

        when(contextManager.getOrCreateContext(request)).thenReturn(context);

        // when
        var response = orchestrator.processChat(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).contains("알겠습니다. 다른 도움이 필요하시면");
        assertThat(response.isRequiresConfirmation()).isFalse();
        assertThat(context.getCurrentPhase()).isEqualTo(TravelPhase.INITIALIZATION.name());
        // Phase 변경이 없으므로 updateContext 호출되지 않음
        verify(contextManager, never()).updateContext(any());
    }

    @Test
    @DisplayName("다양한 부정 표현 처리")
    void testHandleVariousNegativeExpressions() {
        // given
        var request = createChatRequest("그만할래요");
        var context = TravelContext.builder()
            .threadId("thread-1")
            .userId("user-1")
            .currentPhase(TravelPhase.PLAN_GENERATION.name())
            .build();

        when(contextManager.getOrCreateContext(request)).thenReturn(context);

        // when
        var response = orchestrator.processChat(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).contains("계획을 다시 검토해보시겠어요?");
        assertThat(response.isRequiresConfirmation()).isFalse();
        assertThat(context.getCurrentPhase()).isEqualTo(TravelPhase.PLAN_GENERATION.name());
    }

    @Test
    @DisplayName("일반 메시지 처리 - Intent 분류 및 Phase 전환")
    void testProcessNormalMessage() {
        // given
        var request = createChatRequest("제주도 여행 계획 짜줘");
        var context = TravelContext.builder()
            .threadId("thread-1")
            .userId("user-1")
            .currentPhase(TravelPhase.INITIALIZATION.name())
            .conversationCount(0)
            .build();

        when(contextManager.getOrCreateContext(request)).thenReturn(context);
        when(intentClassifier.classify("제주도 여행 계획 짜줘"))
            .thenReturn(Intent.TRAVEL_INFO_COLLECTION);
        when(phaseManager.transitionPhase(
            "thread-123",
            Intent.TRAVEL_INFO_COLLECTION,
            context
        )).thenReturn(TravelPhase.INFORMATION_COLLECTION);

        var mockResponse = ChatResponse.builder()
            .content("여행 정보를 알려주세요.")
            .type("TEXT")
            .nextAction("COLLECT_MORE_INFO")
            .requiresConfirmation(true)
            .build();

        when(responseGenerator.generateResponse(
            eq(request),
            eq(Intent.TRAVEL_INFO_COLLECTION),
            eq(TravelPhase.INFORMATION_COLLECTION),
            eq(context)
        )).thenReturn(mockResponse);

        // when
        var response = orchestrator.processChat(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).isEqualTo("여행 정보를 알려주세요.");
        assertThat(context.getConversationCount()).isEqualTo(1);
        assertThat(context.getCurrentPhase()).isEqualTo(TravelPhase.INFORMATION_COLLECTION.name());

        verify(intentClassifier).classify("제주도 여행 계획 짜줘");
        verify(phaseManager).transitionPhase(
            "thread-123",
            Intent.TRAVEL_INFO_COLLECTION,
            context
        );
        verify(contextManager).updateContext(context);
    }

    @Test
    @DisplayName("Phase 전환이 없는 경우 - 컨텍스트 업데이트 안함")
    void testNoPhaseTransition() {
        // given
        var request = createChatRequest("날씨 어때?");
        var context = TravelContext.builder()
            .threadId("thread-1")
            .userId("user-1")
            .currentPhase(TravelPhase.INFORMATION_COLLECTION.name())
            .conversationCount(5)
            .build();

        when(contextManager.getOrCreateContext(request)).thenReturn(context);
        when(intentClassifier.classify("날씨 어때?"))
            .thenReturn(Intent.TRAVEL_QUESTION);
        when(phaseManager.transitionPhase(
            "thread-1",
            Intent.TRAVEL_QUESTION,
            context
        )).thenReturn(TravelPhase.INFORMATION_COLLECTION);  // 같은 Phase 유지

        var mockResponse = ChatResponse.builder()
            .content("제주도 날씨는...")
            .type("TEXT")
            .nextAction("CONTINUE")
            .requiresConfirmation(false)
            .build();

        when(responseGenerator.generateResponse(
            eq(request),
            eq(Intent.TRAVEL_QUESTION),
            eq(TravelPhase.INFORMATION_COLLECTION),
            eq(context)
        )).thenReturn(mockResponse);

        // when
        var response = orchestrator.processChat(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).isEqualTo("제주도 날씨는...");
        assertThat(context.getConversationCount()).isEqualTo(6);
        assertThat(context.getCurrentPhase()).isEqualTo(TravelPhase.INFORMATION_COLLECTION.name());

        // Phase 변경이 없으므로 updateContext 호출되지 않음
        verify(contextManager, never()).updateContext(any());
    }

    @Test
    @DisplayName("COMPLETION Phase에서 긍정 응답 - Phase 유지")
    void testPositiveResponseInCompletionPhase() {
        // given
        var request = createChatRequest("네, 확정할게요");
        var context = TravelContext.builder()
            .threadId("thread-1")
            .userId("user-1")
            .currentPhase(TravelPhase.COMPLETION.name())
            .build();

        when(contextManager.getOrCreateContext(request)).thenReturn(context);

        var completionResponse = ChatResponse.builder()
            .content("여행 계획이 완료되었습니다.")
            .type("TEXT")
            .nextAction("SAVE_OR_EXPORT")
            .requiresConfirmation(false)
            .build();

        when(responseGenerator.generateResponse(
            eq(request),
            eq(Intent.TRAVEL_INFO_COLLECTION),
            eq(TravelPhase.COMPLETION),
            eq(context)
        )).thenReturn(completionResponse);

        // when
        var response = orchestrator.processChat(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).isEqualTo("여행 계획이 완료되었습니다.");
        assertThat(context.getCurrentPhase()).isEqualTo(TravelPhase.COMPLETION.name());
        // COMPLETION -> COMPLETION이므로 updateContext 호출 안됨
        verify(contextManager, never()).updateContext(any());
    }

    @Test
    @DisplayName("컨텍스트 초기화")
    void testResetContext() {
        // given
        var threadId = "thread-1";

        // when
        orchestrator.resetContext(threadId);

        // then
        verify(contextManager).resetContext(threadId);
    }

    @Test
    @DisplayName("'Y' 응답도 여전히 처리 가능")
    void testLegacyYResponse() {
        // given
        var request = createChatRequest("y");
        var context = TravelContext.builder()
            .threadId("thread-1")
            .userId("user-1")
            .currentPhase(TravelPhase.INITIALIZATION.name())
            .build();

        when(contextManager.getOrCreateContext(request)).thenReturn(context);

        var nextPhaseResponse = ChatResponse.builder()
            .content("여행 정보를 수집합니다.")
            .type("TEXT")
            .nextAction("COLLECT_MORE_INFO")
            .requiresConfirmation(true)
            .build();

        when(responseGenerator.generateResponse(
            eq(request),
            eq(Intent.TRAVEL_INFO_COLLECTION),
            eq(TravelPhase.INFORMATION_COLLECTION),
            eq(context)
        )).thenReturn(nextPhaseResponse);

        // when
        var response = orchestrator.processChat(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).isEqualTo("여행 정보를 수집합니다.");
        assertThat(context.getCurrentPhase()).isEqualTo(TravelPhase.INFORMATION_COLLECTION.name());
        verify(contextManager).updateContext(context);
    }

    // 헬퍼 메서드
    private ChatRequest createChatRequest(String message) {
        var request = new ChatRequest();
        request.setMessage(message);
        request.setThreadId("thread-1");
        request.setUserId("user-1");
        return request;
    }
}