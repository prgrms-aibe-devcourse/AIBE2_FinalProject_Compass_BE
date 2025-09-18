package com.compass.domain.chat.orchestrator;

import com.compass.domain.chat.model.context.TravelContext;
import com.compass.domain.chat.model.enums.Intent;
import com.compass.domain.chat.model.enums.TravelPhase;
import com.compass.domain.chat.model.request.ChatRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// ResponseGenerator 테스트
@ExtendWith(MockitoExtension.class)
class ResponseGeneratorTest {

    private ResponseGenerator responseGenerator;

    @Mock
    private PromptBuilder promptBuilder;

    @Mock
    private ChatModel chatModel;

    @Mock
    private ChatResponse mockChatResponse;

    @Mock
    private Generation mockGeneration;

    @BeforeEach
    void setUp() {
        responseGenerator = new ResponseGenerator(promptBuilder);
        // Reflection으로 ChatModel 주입
        var field = responseGenerator.getClass().getDeclaredFields();
        for (var f : field) {
            if (f.getType().equals(ChatModel.class)) {
                f.setAccessible(true);
                try {
                    f.set(responseGenerator, chatModel);
                } catch (Exception e) {
                    // 무시
                }
            }
        }
    }

    @Test
    @DisplayName("LLM을 통한 응답 생성")
    void testGenerateLLMResponse() {
        // given
        var request = createChatRequest("제주도 여행 계획 짜줘");
        var intent = Intent.INFORMATION_COLLECTION;
        var phase = TravelPhase.INITIALIZATION;

        List<Message> messages = mock(List.class);
        when(promptBuilder.buildPromptMessages(request, intent, phase)).thenReturn(messages);

        when(chatModel.call(any(Prompt.class))).thenReturn(mockChatResponse);
        when(mockChatResponse.getResult()).thenReturn(mockGeneration);
        var assistantMessage = mock(AssistantMessage.class);
        when(assistantMessage.getContent()).thenReturn("LLM 응답 내용");
        when(mockGeneration.getOutput()).thenReturn(assistantMessage);

        // when
        var response = responseGenerator.generateLLMResponse(request, intent, phase);

        // then
        assertThat(response).isEqualTo("LLM 응답 내용");
        verify(promptBuilder).buildPromptMessages(request, intent, phase);
        verify(chatModel).call(any(Prompt.class));
    }

    @Test
    @DisplayName("LLM 호출 실패 시 Mock 응답 반환")
    void testLLMFailureReturnsMockResponse() {
        // given
        var request = createChatRequest("제주도 여행");
        var intent = Intent.INFORMATION_COLLECTION;
        var phase = TravelPhase.INITIALIZATION;

        List<Message> messages = mock(List.class);
        when(promptBuilder.buildPromptMessages(request, intent, phase)).thenReturn(messages);
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("LLM 오류"));

        // when
        var response = responseGenerator.generateLLMResponse(request, intent, phase);

        // then
        assertThat(response).contains("좋아요! 여행 계획을 시작해볼까요?");
    }

    @Test
    @DisplayName("Mock 응답 생성 - INITIALIZATION Phase")
    void testGenerateMockResponseInitialization() {
        // given
        var request = createChatRequest("안녕하세요");

        // when & then
        // GENERAL_CHAT
        var generalResponse = responseGenerator.generateMockResponse(
            request, Intent.GENERAL_QUESTION, TravelPhase.INITIALIZATION);
        assertThat(generalResponse).contains("안녕하세요! 오늘 기분은 어떠신가요?");

        // TRAVEL_QUESTION
        var questionResponse = responseGenerator.generateMockResponse(
            request, Intent.GENERAL_QUESTION, TravelPhase.INITIALIZATION);
        assertThat(questionResponse).contains("여행 관련 질문이시군요!");

        // TRAVEL_INFO_COLLECTION
        var collectionResponse = responseGenerator.generateMockResponse(
            request, Intent.INFORMATION_COLLECTION, TravelPhase.INITIALIZATION);
        assertThat(collectionResponse).contains("좋아요! 여행 계획을 시작해볼까요?");
    }

    @Test
    @DisplayName("Mock 응답 생성 - 다른 Phase들")
    void testGenerateMockResponseOtherPhases() {
        // given
        var request = createChatRequest("테스트");

        // when & then
        var infoResponse = responseGenerator.generateMockResponse(
            request, Intent.GENERAL_QUESTION, TravelPhase.INFORMATION_COLLECTION);
        assertThat(infoResponse).contains("여행 정보를 수집 중이에요!");

        var planResponse = responseGenerator.generateMockResponse(
            request, Intent.GENERAL_QUESTION, TravelPhase.PLAN_GENERATION);
        assertThat(planResponse).contains("여행 계획을 생성 중입니다");

        var feedbackResponse = responseGenerator.generateMockResponse(
            request, Intent.GENERAL_QUESTION, TravelPhase.FEEDBACK_REFINEMENT);
        assertThat(feedbackResponse).contains("피드백을 반영하여");

        var completionResponse = responseGenerator.generateMockResponse(
            request, Intent.GENERAL_QUESTION, TravelPhase.COMPLETION);
        assertThat(completionResponse).contains("완벽한 여행 계획이 완성되었습니다!");
    }

    @Test
    @DisplayName("응답 타입 결정")
    void testDetermineResponseType() {
        // when & then
        // PLAN_GENERATION Phase는 ITINERARY 타입
        var itineraryType = responseGenerator.determineResponseType(
            Intent.GENERAL_QUESTION, TravelPhase.PLAN_GENERATION);
        assertThat(itineraryType).isEqualTo("ITINERARY");

        // 나머지는 TEXT 타입
        var textType = responseGenerator.determineResponseType(
            Intent.GENERAL_QUESTION, TravelPhase.INITIALIZATION);
        assertThat(textType).isEqualTo("TEXT");
    }

    @Test
    @DisplayName("응답 데이터 구성")
    void testBuildResponseData() {
        // given
        var context = TravelContext.builder()
            .threadId("thread-1")
            .userId("user-1")
            .collectedInfo(Map.of("destination", "제주도"))
            .travelPlan(Map.of("day1", "한라산"))
            .build();

        // when & then
        // TRAVEL_INFO_COLLECTION은 collectedInfo 반환
        var collectedData = responseGenerator.buildResponseData(
            Intent.INFORMATION_COLLECTION, TravelPhase.INITIALIZATION, context);
        assertThat(collectedData).isEqualTo(Map.of("destination", "제주도"));

        // PLAN_GENERATION은 travelPlan 반환
        var planData = responseGenerator.buildResponseData(
            Intent.GENERAL_QUESTION, TravelPhase.PLAN_GENERATION, context);
        assertThat(planData).isEqualTo(Map.of("day1", "한라산"));

        // 나머지는 null
        var nullData = responseGenerator.buildResponseData(
            Intent.GENERAL_QUESTION, TravelPhase.INITIALIZATION, context);
        assertThat(nullData).isNull();
    }

    @Test
    @DisplayName("다음 액션 결정")
    void testDetermineNextAction() {
        // when & then
        assertThat(responseGenerator.determineNextAction(
            Intent.GENERAL_QUESTION, TravelPhase.INFORMATION_COLLECTION))
            .isEqualTo("COLLECT_MORE_INFO");

        assertThat(responseGenerator.determineNextAction(
            Intent.GENERAL_QUESTION, TravelPhase.FEEDBACK_REFINEMENT))
            .isEqualTo("REFINE_PLAN");

        assertThat(responseGenerator.determineNextAction(
            Intent.GENERAL_QUESTION, TravelPhase.COMPLETION))
            .isEqualTo("SAVE_OR_EXPORT");

        assertThat(responseGenerator.determineNextAction(
            Intent.GENERAL_QUESTION, TravelPhase.INITIALIZATION))
            .isEqualTo("CONTINUE");
    }

    @Test
    @DisplayName("Phase 진행 확인 프롬프트 추가 - INITIALIZATION")
    void testConfirmationPromptForInitialization() {
        // given
        var request = createChatRequest("여행 계획 짜고 싶어요");
        var intent = Intent.INFORMATION_COLLECTION;
        var phase = TravelPhase.INITIALIZATION;
        var context = TravelContext.builder()
            .threadId("thread-1")
            .userId("user-1")
            .build();

        // when
        var response = responseGenerator.generateResponse(request, intent, phase, context);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).contains("함께 멋진 여행 계획을 만들어볼까요?");
        assertThat(response.isRequiresConfirmation()).isTrue();
    }

    @Test
    @DisplayName("일반 대화 처리 + 여행 유도 - 인사말")
    void testGeneralChatWithTravelInductionGreeting() {
        // given
        var request = createChatRequest("안녕하세요!");
        var intent = Intent.GENERAL_QUESTION;
        var phase = TravelPhase.INITIALIZATION;
        var context = TravelContext.builder()
            .threadId("thread-1")
            .userId("user-1")
            .build();

        // when (ChatModel이 없어서 기본 응답 반환)
        var response = responseGenerator.generateResponse(request, intent, phase, context);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).contains("안녕하세요");
        assertThat(response.getContent()).contains("여행");
        assertThat(response.isRequiresConfirmation()).isTrue();
    }

    @Test
    @DisplayName("일반 대화 처리 + 여행 유도 - 날씨 질문")
    void testGeneralChatWithTravelInductionWeather() {
        // given
        var request = createChatRequest("오늘 날씨 어때?");
        var intent = Intent.GENERAL_QUESTION;
        var phase = TravelPhase.INITIALIZATION;
        var context = TravelContext.builder()
            .threadId("thread-1")
            .userId("user-1")
            .build();

        // when
        var response = responseGenerator.generateResponse(request, intent, phase, context);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).contains("날씨");
        assertThat(response.getContent()).contains("여행");
        assertThat(response.isRequiresConfirmation()).isTrue();
    }

    @Test
    @DisplayName("일반 대화 처리 + 여행 유도 - 심심함 표현")
    void testGeneralChatWithTravelInductionBored() {
        // given
        var request = createChatRequest("너무 심심해");
        var intent = Intent.GENERAL_QUESTION;
        var phase = TravelPhase.INITIALIZATION;
        var context = TravelContext.builder()
            .threadId("thread-1")
            .userId("user-1")
            .build();

        // when
        var response = responseGenerator.generateResponse(request, intent, phase, context);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).contains("기분전환");
        assertThat(response.getContent()).contains("여행");
        assertThat(response.isRequiresConfirmation()).isTrue();
    }

    @Test
    @DisplayName("Phase 진행 확인 프롬프트 추가 - INFORMATION_COLLECTION")
    void testConfirmationPromptForInformationCollection() {
        // given
        var request = createChatRequest("제주도 3박 4일");
        var intent = Intent.INFORMATION_COLLECTION;
        var phase = TravelPhase.INFORMATION_COLLECTION;
        var context = TravelContext.builder()
            .threadId("thread-1")
            .userId("user-1")
            .build();

        // when
        var response = responseGenerator.generateResponse(request, intent, phase, context);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).contains("충분한 정보가 모인 것 같네요!");
        assertThat(response.isRequiresConfirmation()).isTrue();
    }

    @Test
    @DisplayName("Phase 진행 확인 프롬프트 추가 - PLAN_GENERATION")
    void testConfirmationPromptForPlanGeneration() {
        // given
        var request = createChatRequest("계획 확인");
        var intent = Intent.INFORMATION_COLLECTION;
        var phase = TravelPhase.PLAN_GENERATION;
        var context = TravelContext.builder()
            .threadId("thread-1")
            .userId("user-1")
            .build();

        // when
        var response = responseGenerator.generateResponse(request, intent, phase, context);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).contains("어떠신가요? 이 일정으로 진행하시겠어요?");
        assertThat(response.isRequiresConfirmation()).isTrue();
    }

    @Test
    @DisplayName("Phase 진행 확인 프롬프트 추가 - FEEDBACK_REFINEMENT")
    void testConfirmationPromptForFeedbackRefinement() {
        // given
        var request = createChatRequest("수정사항 확인");
        var intent = Intent.INFORMATION_COLLECTION;
        var phase = TravelPhase.FEEDBACK_REFINEMENT;
        var context = TravelContext.builder()
            .threadId("thread-1")
            .userId("user-1")
            .build();

        // when
        var response = responseGenerator.generateResponse(request, intent, phase, context);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).contains("수정사항을 반영해드렸어요!");
        assertThat(response.isRequiresConfirmation()).isTrue();
    }

    @Test
    @DisplayName("COMPLETION Phase에서는 확인 프롬프트 없음")
    void testNoConfirmationPromptForCompletion() {
        // given
        var request = createChatRequest("여행 계획 완료");
        var intent = Intent.INFORMATION_COLLECTION;
        var phase = TravelPhase.COMPLETION;
        var context = TravelContext.builder()
            .threadId("thread-1")
            .userId("user-1")
            .build();

        // when
        var response = responseGenerator.generateResponse(request, intent, phase, context);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).doesNotContain("할까요?");
        assertThat(response.getContent()).doesNotContain("시겠어요?");
        assertThat(response.isRequiresConfirmation()).isFalse();
    }

    @Test
    @DisplayName("전체 응답 생성 - LLM 사용")
    void testGenerateResponseWithLLM() {
        // given
        var request = createChatRequest("제주도 여행");
        var intent = Intent.INFORMATION_COLLECTION;
        var phase = TravelPhase.INITIALIZATION;
        var context = TravelContext.builder()
            .threadId("thread-1")
            .userId("user-1")
            .build();

        List<Message> messages = mock(List.class);
        when(promptBuilder.buildPromptMessages(request, intent, phase)).thenReturn(messages);
        when(chatModel.call(any(Prompt.class))).thenReturn(mockChatResponse);
        when(mockChatResponse.getResult()).thenReturn(mockGeneration);
        var assistantMessage = mock(AssistantMessage.class);
        when(assistantMessage.getContent()).thenReturn("LLM 생성 응답");
        when(mockGeneration.getOutput()).thenReturn(assistantMessage);

        // when
        var response = responseGenerator.generateResponse(request, intent, phase, context);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).contains("LLM 생성 응답");
        assertThat(response.getContent()).contains("함께 멋진 여행 계획을 만들어볼까요?");
        assertThat(response.getType()).isEqualTo("TEXT");
        assertThat(response.getNextAction()).isEqualTo("CONTINUE");
        assertThat(response.isRequiresConfirmation()).isTrue();
    }

    @Test
    @DisplayName("전체 응답 생성 - ChatModel이 null일 때 Mock 사용")
    void testGenerateResponseWithoutChatModel() {
        // given
        responseGenerator = new ResponseGenerator(promptBuilder);  // ChatModel 없이 생성

        var request = createChatRequest("안녕하세요");
        var intent = Intent.GENERAL_QUESTION;
        var phase = TravelPhase.INITIALIZATION;
        var context = TravelContext.builder()
            .threadId("thread-1")
            .userId("user-1")
            .build();

        // when
        var response = responseGenerator.generateResponse(request, intent, phase, context);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).contains("안녕하세요! 오늘 기분은 어떠신가요?");
        assertThat(response.getContent()).contains("함께 멋진 여행 계획을 만들어볼까요?");
        assertThat(response.getType()).isEqualTo("TEXT");
        assertThat(response.getNextAction()).isEqualTo("CONTINUE");
        assertThat(response.isRequiresConfirmation()).isTrue();
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