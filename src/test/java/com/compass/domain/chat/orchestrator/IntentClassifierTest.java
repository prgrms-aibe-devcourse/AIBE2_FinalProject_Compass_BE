package com.compass.domain.chat.orchestrator;

import com.compass.domain.chat.model.enums.Intent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

// Intent 분류기 테스트 (단순화된 3가지 Intent - 분류만 담당)
@ExtendWith(MockitoExtension.class)
class IntentClassifierTest {

    private IntentClassifier intentClassifier;

    @Mock
    private ChatModel chatModel;

    @Mock
    private ChatResponse chatResponse;

    @Mock
    private Generation generation;

    @BeforeEach
    void setUp() {
        // 생성자로 ChatModel 주입
        intentClassifier = new IntentClassifier(chatModel);
    }

    @Test
    @DisplayName("빈 메시지는 GENERAL_QUESTION으로 분류")
    void testClassifyEmptyMessage() {
        // given
        String emptyMessage = "";

        // when
        Intent result = intentClassifier.classify(emptyMessage, false);

        // then
        assertThat(result).isEqualTo(Intent.GENERAL_QUESTION);
        verify(chatModel, never()).call(any(Prompt.class));
    }

    @Test
    @DisplayName("null 메시지는 GENERAL_QUESTION으로 분류")
    void testClassifyNullMessage() {
        // when
        Intent result = intentClassifier.classify(null, false);

        // then
        assertThat(result).isEqualTo(Intent.GENERAL_QUESTION);
        verify(chatModel, never()).call(any(Prompt.class));
    }

    @Test
    @DisplayName("여행 계획 요청 → TRAVEL_PLANNING")
    void testClassifyTravelPlanningRequest() {
        // given
        String message = "제주도 3박 4일 여행 계획 짜줘";
        // 키워드 매칭으로 처리되므로 LLM Mock 불필요

        // when
        Intent result = intentClassifier.classify(message, false);

        // then
        assertThat(result).isEqualTo(Intent.TRAVEL_PLANNING);
        // 키워드 매칭으로 처리되므로 LLM 호출 안됨
        verify(chatModel, never()).call(any(Prompt.class));
    }

    @Test
    @DisplayName("날씨 관련 질문 → WEATHER_INQUIRY")
    void testClassifyTravelQuestion() {
        // given
        String message = "파리 날씨 어때?";
        // 키워드 매칭으로 날씨 문의 처리

        // when
        Intent result = intentClassifier.classify(message, false);

        // then
        assertThat(result).isEqualTo(Intent.WEATHER_INQUIRY);
        // 키워드 매칭으로 처리되므로 LLM 호출 안됨
        verify(chatModel, never()).call(any(Prompt.class));
    }

    @Test
    @DisplayName("일반 대화 → GENERAL_QUESTION")
    void testClassifyGeneralChat() {
        // given
        String message = "안녕하세요";
        // 키워드 매칭으로 일반 인사 처리

        // when
        Intent result = intentClassifier.classify(message, false);

        // then
        assertThat(result).isEqualTo(Intent.GENERAL_QUESTION);
        // 키워드 매칭으로 처리되므로 LLM 호출 안됨
        verify(chatModel, never()).call(any(Prompt.class));
    }

    @Test
    @DisplayName("여행 계획 생성 요청 다양한 표현 테스트")
    void testVariousTravelPlanningRequests() {
        // given
        String[] messages = {
            "여행 계획 짜줘",
            "일정 짜줘", 
            "휴가 계획 세워줘"
        };

        for (String message : messages) {
            // 키워드 매칭으로 처리됨

            // when
            Intent result = intentClassifier.classify(message, false);

            // then
            assertThat(result).isEqualTo(Intent.TRAVEL_PLANNING);
        }
    }

    @Test
    @DisplayName("LLM으로 분류되는 복잡한 질문들")
    void testVariousTravelQuestions() {
        // given - 키워드 매칭에 걸리지 않는 메시지들로 변경
        String[] messages = {
            "맛집 추천해줘",
            "비자 필요해?", 
            "숙소 추천해줘",
            "교통편 알려줘"
        };

        for (String message : messages) {
            setupLLMResponse("GENERAL_QUESTION");

            // when
            Intent result = intentClassifier.classify(message, false);

            // then
            assertThat(result).isEqualTo(Intent.GENERAL_QUESTION);
        }
    }

    @Test
    @DisplayName("LLM 응답이 알 수 없는 형식일 때 GENERAL_QUESTION 반환")
    void testInvalidLLMResponse() {
        // given
        String message = "테스트 메시지";
        setupLLMResponse("알 수 없는 응답");

        // when
        Intent result = intentClassifier.classify(message, false);

        // then
        assertThat(result).isEqualTo(Intent.GENERAL_QUESTION);
        verify(chatModel, times(1)).call(any(Prompt.class));
    }

    @Test
    @DisplayName("LLM 호출 실패 시 GENERAL_QUESTION 반환")
    void testLLMFailure() {
        // given
        String message = "테스트 메시지";
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("LLM 호출 실패"));

        // when
        Intent result = intentClassifier.classify(message, false);

        // then
        assertThat(result).isEqualTo(Intent.GENERAL_QUESTION);
        verify(chatModel, times(1)).call(any(Prompt.class));
    }

    @Test
    @DisplayName("애매한 메시지도 LLM이 적절히 분류")
    void testAmbiguousMessage() {
        // given
        String message = "다음 주 가족 휴가 가려고 하는데";
        setupLLMResponse("INFORMATION_COLLECTION");

        // when
        Intent result = intentClassifier.classify(message, false);

        // then
        assertThat(result).isEqualTo(Intent.INFORMATION_COLLECTION);
        verify(chatModel, times(1)).call(any(Prompt.class));
    }

    @Test
    @DisplayName("확인 대기 상태에서 긍정 응답 처리")
    void testClassifyConfirmationWhileWaiting() {
        // given
        String message = "네, 좋아요";
        // 키워드 매칭으로 확인 처리

        // when
        Intent result = intentClassifier.classify(message, true);

        // then
        assertThat(result).isEqualTo(Intent.CONFIRMATION);
        // 키워드 매칭으로 처리되므로 LLM 호출 안됨
        verify(chatModel, never()).call(any(Prompt.class));
    }

    @Test
    @DisplayName("확인 대기 상태에서 부정 응답 처리")
    void testClassifyRejectionWhileWaiting() {
        // given
        String message = "아니요";
        setupLLMResponse("GENERAL_QUESTION");

        // when
        Intent result = intentClassifier.classify(message, true);

        // then
        assertThat(result).isEqualTo(Intent.GENERAL_QUESTION);
        verify(chatModel, times(1)).call(any(Prompt.class));
    }

    // 헬퍼 메서드: LLM 응답 설정
    private void setupLLMResponse(String intentName) {
        AssistantMessage assistantMessage = mock(AssistantMessage.class);
        lenient().when(assistantMessage.getContent()).thenReturn(intentName);
        lenient().when(generation.getOutput()).thenReturn(assistantMessage);
        lenient().when(chatResponse.getResult()).thenReturn(generation);
        lenient().when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);
    }
}