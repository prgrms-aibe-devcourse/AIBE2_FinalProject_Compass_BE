package com.compass.domain.chat.orchestrator;

import com.compass.domain.chat.collection.service.FormDataConverter;
import com.compass.domain.chat.function.collection.ContinueFollowUpFunction;
import com.compass.domain.chat.function.collection.StartFollowUpFunction;
import com.compass.domain.chat.function.collection.SubmitTravelFormFunction;
import com.compass.domain.chat.function.planning.RecommendDestinationsFunction;
import com.compass.domain.chat.model.context.TravelContext;
import com.compass.domain.chat.model.dto.DestinationRecommendationDto;
import com.compass.domain.chat.model.enums.Intent;
import com.compass.domain.chat.model.enums.TravelPhase;
import com.compass.domain.chat.model.request.ChatRequest;
import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import com.compass.domain.chat.model.response.ChatResponse;
import com.compass.domain.chat.model.response.FollowUpResponse;
import com.compass.domain.chat.service.ChatThreadService;
// ❌ 사용되지 않는 의존성 (테스트 실패의 원인)
// import com.compass.domain.chat.service.TravelInfoService; 
import com.compass.domain.chat.service.TravelFormWorkflowService; // ✅ 실제 사용되는 서비스
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MainLLMOrchestratorTest {

    @InjectMocks
    private MainLLMOrchestrator orchestrator;

    @Mock private IntentClassifier intentClassifier;
    @Mock private PhaseManager phaseManager;
    @Mock private ContextManager contextManager;
    @Mock private ResponseGenerator responseGenerator;
    @Mock private ChatThreadService chatThreadService;
    @Mock private PromptBuilder promptBuilder;
    @Mock private FormDataConverter formDataConverter;
    // @Mock private TravelInfoService travelInfoService; // ❌ 사용되지 않음
    @Mock private TravelFormWorkflowService travelFormWorkflowService; // ✅ Mock 객체 추가
    @Mock private SubmitTravelFormFunction submitTravelFormFunction;
    @Mock private StartFollowUpFunction startFollowUpFunction;
    @Mock private RecommendDestinationsFunction recommendDestinationsFunction;
    @Mock private ContinueFollowUpFunction continueFollowUpFunction;

    private TravelContext context;
    private ChatRequest chatRequest;
    private TravelFormSubmitRequest travelFormRequest;

    @BeforeEach
    void setup() {
        context = TravelContext.builder()
                .threadId("thread-1")
                .userId("user-1")
                .currentPhase(TravelPhase.INITIALIZATION.name())
                .build();
    }

    @Test
    @DisplayName("폼 제출 - 모든 정보 완료 시, 계획 생성 단계로 전환")
    void handleFormSubmission_triggersPlanGeneration_whenFormIsComplete() {
        // given
        setupFormSubmissionRequest();
        when(submitTravelFormFunction.apply(any(TravelFormSubmitRequest.class)))
                .thenReturn(ChatResponse.builder().nextAction("TRIGGER_PLAN_GENERATION").build());

        // when
        ChatResponse response = orchestrator.processChat(chatRequest);

        // then
        // ✅ 실제 호출되는 travelFormWorkflowService.persistFormData를 검증
        boolean shouldTransition = true;
        verify(travelFormWorkflowService).persistFormData(context, chatRequest.getThreadId(), travelFormRequest, shouldTransition);
        // ❌ phaseManager.savePhase는 호출되지 않으므로 검증 제거
        // verify(phaseManager).savePhase(chatRequest.getThreadId(), TravelPhase.PLAN_GENERATION);

        // ✅ Orchestrator가 context를 직접 변경하므로, 그 결과를 검증
        assertThat(context.getCurrentPhase()).isEqualTo(TravelPhase.PLAN_GENERATION.name());
        assertThat(response.getNextAction()).isEqualTo("TRIGGER_PLAN_GENERATION");
    }

    @Test
    @DisplayName("폼 제출 - 정보 부족 시, 후속 질문 단계로 전환")
    void handleFormSubmission_startsFollowUp_whenFormIsIncomplete() {
        // given
        setupFormSubmissionRequest();
        when(submitTravelFormFunction.apply(any(TravelFormSubmitRequest.class)))
                .thenReturn(ChatResponse.builder().nextAction("START_FOLLOW_UP").content("초기 응답").build());
        when(startFollowUpFunction.apply(any(TravelFormSubmitRequest.class)))
                .thenReturn(ChatResponse.builder().content("후속 질문입니다.").build());

        // when
        ChatResponse response = orchestrator.processChat(chatRequest);

        // then
        // ✅ 실제 구현과 동일하게 persistFormData가 호출되지 않음을 검증
        verify(travelFormWorkflowService, never()).persistFormData(any(), any(), any(), anyBoolean());
        verify(startFollowUpFunction).apply(travelFormRequest);
        verify(chatThreadService, times(3)).saveMessage(any());
        assertThat(response.getContent()).isEqualTo("후속 질문입니다.");
    }

    @Test
    @DisplayName("폼 제출 - 목적지 미정 시, 목적지 추천 단계로 전환")
    void handleFormSubmission_recommendsDestinations_whenDestinationIsUndecided() {
        // given
        setupFormSubmissionRequest();
        when(submitTravelFormFunction.apply(any(TravelFormSubmitRequest.class)))
                .thenReturn(ChatResponse.builder().nextAction("RECOMMEND_DESTINATIONS").build());
        when(recommendDestinationsFunction.apply(any(TravelFormSubmitRequest.class)))
                .thenReturn(new DestinationRecommendationDto("추천", "설명", null, null));

        // when
        ChatResponse response = orchestrator.processChat(chatRequest);

        // then
        // ✅ 실제 호출되는 travelFormWorkflowService.persistFormData를 검증
        boolean shouldTransition = false;
        verify(travelFormWorkflowService).persistFormData(context, chatRequest.getThreadId(), travelFormRequest, shouldTransition);
        verify(recommendDestinationsFunction).apply(travelFormRequest);
        assertThat(response.getType()).isEqualTo("DESTINATION_RECOMMENDATION");
        assertThat(response.getData()).isInstanceOf(DestinationRecommendationDto.class);
    }

    @Test
    @DisplayName("일반 대화 - Phase 전환이 일어나는 경우")
    void handleGeneralChatMessage_transitionsPhase_whenNeeded() {
        // given
        chatRequest = createChatRequest("여행 계획 세워줘");
        when(contextManager.getOrCreateContext(chatRequest)).thenReturn(context);
        when(intentClassifier.classify(anyString(), anyBoolean())).thenReturn(Intent.TRAVEL_PLANNING);
        when(phaseManager.transitionPhase(any(), any(), any())).thenReturn(TravelPhase.INFORMATION_COLLECTION);
        when(responseGenerator.generateResponse(any(), any(), eq(TravelPhase.INFORMATION_COLLECTION), any(), any()))
                .thenReturn(ChatResponse.builder().content("여행 계획을 시작합니다.").build());

        // when
        ChatResponse response = orchestrator.processChat(chatRequest);

        // then
        assertThat(response.getContent()).isEqualTo("여행 계획을 시작합니다.");
        verify(contextManager).updateContext(context, "user-1");
        assertThat(context.getCurrentPhase()).isEqualTo(TravelPhase.INFORMATION_COLLECTION.name());
    }

    @Test
    @DisplayName("정보 수집 단계에서 대화 - 후속 질문으로 이어짐")
    void handleGeneralChatMessage_continuesFollowUp_inInfoCollectionPhase() {
        // given
        chatRequest = createChatRequest("네 혼자 가요");
        context.setCurrentPhase(TravelPhase.INFORMATION_COLLECTION.name());
        when(contextManager.getOrCreateContext(chatRequest)).thenReturn(context);
        when(intentClassifier.classify(anyString(), anyBoolean())).thenReturn(Intent.INFORMATION_COLLECTION);
        when(continueFollowUpFunction.apply(any(FollowUpResponse.class)))
                .thenReturn(ChatResponse.builder().content("다음 질문입니다.").build());

        // when
        ChatResponse response = orchestrator.processChat(chatRequest);

        // then
        assertThat(response.getContent()).isEqualTo("다음 질문입니다.");
        verify(continueFollowUpFunction).apply(any(FollowUpResponse.class));
        verify(phaseManager, never()).transitionPhase(any(), any(), any());
    }

    // --- 헬퍼 메서드 ---

    private void setupFormSubmissionRequest() {
        var formData = Map.<String, Object>of("destination", "제주");
        var metadata = Map.<String, Object>of("type", "TRAVEL_FORM_SUBMIT", "formData", formData);
        chatRequest = new ChatRequest("폼 제출", "thread-1", "user-1", metadata);
        travelFormRequest = new TravelFormSubmitRequest("user-1", List.of("제주"), null, null, null, null, null, null, null, null);

        when(contextManager.getOrCreateContext(chatRequest)).thenReturn(context);
        when(formDataConverter.convertFromFrontend(anyString(), anyMap())).thenReturn(travelFormRequest);
    }

    private ChatRequest createChatRequest(String message) {
        return new ChatRequest(message, "thread-1", "user-1", Collections.emptyMap());
    }
}