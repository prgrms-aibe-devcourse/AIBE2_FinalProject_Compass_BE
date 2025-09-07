package com.compass.domain.chat.service;

import com.compass.domain.chat.context.ConversationContextManager;
import com.compass.domain.chat.service.impl.GeminiChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Gemini Chat Service 단위 테스트
 * REQ-LLM-006: Includes conversation context management tests
 */
class GeminiChatServiceTest {

    @Mock
    private VertexAiGeminiChatModel geminiChatModel;
    
    @Mock
    private ConversationContextManager contextManager;

    private GeminiChatService geminiChatService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        geminiChatService = new GeminiChatService(geminiChatModel, contextManager);
    }

    @Test
    @DisplayName("단순 메시지 응답 생성 테스트")
    void testGenerateResponse_SimpleMessage() {
        // Given
        String userMessage = "안녕하세요";
        String expectedResponse = "안녕하세요! 무엇을 도와드릴까요?";
        
        AssistantMessage assistantMessage = new AssistantMessage(expectedResponse);
        Generation generation = new Generation(assistantMessage);
        
        ChatResponse chatResponse = new ChatResponse(java.util.List.of(generation));
        
        when(geminiChatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        // When
        String actualResponse = geminiChatService.generateResponse(userMessage);

        // Then
        assertNotNull(actualResponse);
        verify(geminiChatModel, times(1)).call(any(Prompt.class));
    }

    @Test
    @DisplayName("시스템 프롬프트와 함께 응답 생성 테스트")
    void testGenerateResponse_WithSystemPrompt() {
        // Given
        String systemPrompt = "당신은 여행 전문가입니다.";
        String userMessage = "서울에서 가볼만한 곳 추천해주세요";
        String expectedResponse = "서울의 주요 관광지를 추천드리겠습니다...";
        
        AssistantMessage assistantMessage = new AssistantMessage(expectedResponse);
        Generation generation = new Generation(assistantMessage);
        
        ChatResponse chatResponse = new ChatResponse(java.util.List.of(generation));
        
        when(geminiChatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        // When
        String actualResponse = geminiChatService.generateResponse(systemPrompt, userMessage);

        // Then
        assertNotNull(actualResponse);
        assertEquals(expectedResponse, actualResponse);
        verify(geminiChatModel, times(1)).call(any(Prompt.class));
    }

    @Test
    @DisplayName("모델 이름 반환 테스트")
    void testGetModelName() {
        // When
        String modelName = geminiChatService.getModelName();

        // Then
        assertEquals("Gemini 2.0 Flash", modelName);
    }

    @Test
    @DisplayName("예외 처리 테스트 - 단순 메시지")
    void testGenerateResponse_ExceptionHandling() {
        // Given
        String userMessage = "테스트 메시지";
        when(geminiChatModel.call(any(Prompt.class)))
            .thenThrow(new RuntimeException("API 호출 실패"));

        // When & Then
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> geminiChatService.generateResponse("test", userMessage)
        );
        
        assertTrue(exception.getMessage().contains("Failed to generate response with Gemini"));
        verify(geminiChatModel, times(1)).call(any(Prompt.class));
    }

    @Test
    @DisplayName("빈 메시지 처리 테스트")
    void testGenerateResponse_EmptyMessage() {
        // Given
        String userMessage = "";

        // When & Then - 빈 메시지는 예외가 발생해야 함
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> geminiChatService.generateResponse(userMessage)
        );
        
        assertTrue(exception.getMessage().contains("Failed to generate response with Gemini"));
        verify(geminiChatModel, never()).call(any(Prompt.class));
    }

    @Test
    @DisplayName("긴 메시지 처리 테스트")
    void testGenerateResponse_LongMessage() {
        // Given
        StringBuilder longMessage = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longMessage.append("이것은 매우 긴 메시지입니다. ");
        }
        String userMessage = longMessage.toString();
        String expectedResponse = "긴 메시지에 대한 응답입니다.";
        
        AssistantMessage assistantMessage = new AssistantMessage(expectedResponse);
        Generation generation = new Generation(assistantMessage);
        
        ChatResponse chatResponse = new ChatResponse(java.util.List.of(generation));
        
        when(geminiChatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        // When
        String actualResponse = geminiChatService.generateResponse(userMessage);

        // Then
        assertNotNull(actualResponse);
        assertEquals(expectedResponse, actualResponse);
        verify(geminiChatModel, times(1)).call(any(Prompt.class));
    }
}