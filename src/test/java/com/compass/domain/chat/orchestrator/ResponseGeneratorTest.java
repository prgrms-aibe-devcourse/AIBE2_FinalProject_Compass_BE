package com.compass.domain.chat.orchestrator;

import com.compass.domain.chat.function.collection.ShowQuickInputFormFunction;
import com.compass.domain.chat.model.context.TravelContext;
import com.compass.domain.chat.model.dto.QuickInputFormDto;
import com.compass.domain.chat.model.request.ChatRequest;
import com.compass.domain.chat.model.response.ChatResponse;
import com.compass.domain.chat.model.enums.Intent;
import com.compass.domain.chat.model.enums.TravelPhase;
import com.compass.domain.travel_plan.service.TravelPlanGenerationService; // ⬅️ 추가됨
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class ResponseGeneratorTest {

    private ResponseGenerator responseGenerator;

    @Mock
    private ChatModel chatModel;

    @Mock
    private ShowQuickInputFormFunction showQuickInputFormFunction;

    @Mock // ⬅️ 추가됨
    private TravelPlanGenerationService travelPlanGenerationService;

    @Mock
    private PromptBuilder promptBuilder;

    @Mock
    private org.springframework.ai.chat.model.ChatResponse mockChatResponse;

    @Mock
    private Generation mockGeneration;

    @Mock
    private TravelContext travelContext;

    @BeforeEach
    void setUp() {
        // ⬅️ 수정됨: 누락되었던 travelPlanGenerationService 인자 추가
        responseGenerator = new ResponseGenerator(showQuickInputFormFunction, travelPlanGenerationService);
        // ChatModel은 @Autowired(required = false)로 주입되므로 리플렉션으로 설정
        ReflectionTestUtils.setField(responseGenerator, "chatModel", chatModel);
    }

    @Test
    @DisplayName("LLM을 통한 응답 생성 - generateLLMResponse 메서드")
    void testGenerateLLMResponse() {
        // given
        var request = createChatRequest("제주도 여행 계획 짜줘");
        var intent = Intent.INFORMATION_COLLECTION;
        var phase = TravelPhase.INITIALIZATION;

        when(promptBuilder.buildSystemPrompt(any(), any(), any())).thenReturn("시스템 프롬프트");
        when(promptBuilder.buildUserPrompt(any(), any())).thenReturn("사용자 프롬프트");

        when(chatModel.call(any(Prompt.class))).thenReturn(mockChatResponse);
        when(mockChatResponse.getResult()).thenReturn(mockGeneration);
        var assistantMessage = mock(AssistantMessage.class);
        when(assistantMessage.getContent()).thenReturn("LLM 응답 내용");
        when(mockGeneration.getOutput()).thenReturn(assistantMessage);

        // when
        var response = responseGenerator.generateLLMResponse(request, intent, phase, travelContext, promptBuilder);

        // then
        assertThat(response).isEqualTo("LLM 응답 내용");
        verify(promptBuilder).buildSystemPrompt(intent, phase, travelContext);
        verify(promptBuilder).buildUserPrompt(request.getMessage(), travelContext);
        verify(chatModel).call(any(Prompt.class));
    }

    @Test
    @DisplayName("ChatResponse 생성 - generateResponse 메서드")
    void testGenerateResponse() {
        // given
        var request = createChatRequest("여행 계획 세워줘");
        var intent = Intent.TRAVEL_PLANNING;
        var phase = TravelPhase.INFORMATION_COLLECTION;

        lenient().when(travelContext.isWaitingForTravelConfirmation()).thenReturn(false);
        // CollectedInfo가 null이 아닌 빈 Map을 반환하도록 설정
        lenient().when(travelContext.getCollectedInfo()).thenReturn(Map.of());
        

        // ShowQuickInputFormFunction 모킹
        var quickForm = new QuickInputFormDto(
            "travel_form",
            List.of(),
            Map.of()
        );
        lenient().when(showQuickInputFormFunction.apply(any())).thenReturn(quickForm);

        // when
        var response = responseGenerator.generateResponse(request, intent, phase, travelContext, promptBuilder);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getType()).isEqualTo("QUICK_FORM");
        assertThat(response.getData()).isNotNull();
    }

    @Test
    @DisplayName("확인 대기 상태일 때 응답 생성")
    void testGenerateResponse_WaitingForConfirmation() {
        // given
        var request = createChatRequest("네, 여행 계획 세워주세요");
        var intent = Intent.CONFIRMATION;
        var phase = TravelPhase.INITIALIZATION;

        when(travelContext.isWaitingForTravelConfirmation()).thenReturn(true);

        // when
        var response = responseGenerator.generateResponse(request, intent, phase, travelContext, promptBuilder);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getType()).isEqualTo("TEXT");
        assertThat(response.getContent()).contains("좋");
    }

    @Test
    @DisplayName("LLM 응답 없이 generateResponse 호출")
    void testGenerateResponse_WithoutLLM() {
        // given
        var request = createChatRequest("여행 계획");
        var intent = Intent.GENERAL_QUESTION;
        var phase = TravelPhase.INITIALIZATION;

        when(travelContext.isWaitingForTravelConfirmation()).thenReturn(false);

        // LLM 응답 모킹
        when(promptBuilder.buildSystemPrompt(any(), any(), any())).thenReturn("시스템 프롬프트");
        when(promptBuilder.buildUserPrompt(any(), any())).thenReturn("사용자 프롬프트");

        when(chatModel.call(any(Prompt.class))).thenReturn(mockChatResponse);
        when(mockChatResponse.getResult()).thenReturn(mockGeneration);
        var assistantMessage = mock(AssistantMessage.class);
        when(assistantMessage.getContent()).thenReturn("여행 계획을 도와드리겠습니다.");
        when(mockGeneration.getOutput()).thenReturn(assistantMessage);

        // when
        var response = responseGenerator.generateResponse(request, intent, phase, travelContext, promptBuilder);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).isNotNull();
    }

    private ChatRequest createChatRequest(String message) {
        return new ChatRequest(message, "test-thread-123", "test-user-123");
    }
}