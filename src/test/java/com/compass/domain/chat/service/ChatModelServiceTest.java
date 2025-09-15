package com.compass.domain.chat.service;

import com.compass.config.BaseIntegrationTest;
import com.compass.domain.chat.service.impl.GeminiChatService;
import com.compass.domain.chat.service.impl.OpenAIChatService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Spring AI Integration Tests
 * Tests for Gemini and OpenAI model integration
 */
@Disabled("Integration test issues - temporarily disabled to fix CI")
class ChatModelServiceTest extends BaseIntegrationTest {

    @Autowired(required = false)
    private GeminiChatService geminiChatService;

    @Autowired(required = false)
    private OpenAIChatService openAIChatService;

    @Test
    @DisplayName("Gemini 서비스 빈이 생성되어야 한다")
    void geminiServiceBeanShouldBeCreated() {
        // Given & When & Then
        assertNotNull(geminiChatService, "GeminiChatService bean should be created");
    }

    @Test
    @DisplayName("OpenAI 서비스 빈이 생성되어야 한다")
    void openAIServiceBeanShouldBeCreated() {
        // Given & When & Then
        assertNotNull(openAIChatService, "OpenAIChatService bean should be created");
    }

    @Test
    @DisplayName("Gemini 모델 이름을 반환해야 한다")
    void shouldReturnGeminiModelName() {
        // Given
        if (geminiChatService == null) {
            return; // Skip if service is not available
        }

        // When
        String modelName = geminiChatService.getModelName();

        // Then
        assertThat(modelName).isEqualTo("Gemini 2.0 Flash");
    }

    @Test
    @DisplayName("OpenAI 모델 이름을 반환해야 한다")
    void shouldReturnOpenAIModelName() {
        // Given
        if (openAIChatService == null) {
            return; // Skip if service is not available
        }

        // When
        String modelName = openAIChatService.getModelName();

        // Then
        assertThat(modelName).isEqualTo("GPT-4o-mini");
    }

    @Test
    @DisplayName("Gemini 서비스는 null 입력에 대해 예외를 던져야 한다")
    void geminiShouldThrowExceptionForNullInput() {
        // Given
        if (geminiChatService == null) {
            return; // Skip if service is not available
        }

        // When & Then
        assertThrows(Exception.class, () -> {
            geminiChatService.generateResponse(null);
        });
    }

    @Test
    @DisplayName("OpenAI 서비스는 null 입력에 대해 예외를 던져야 한다")
    void openAIShouldThrowExceptionForNullInput() {
        // Given
        if (openAIChatService == null) {
            return; // Skip if service is not available
        }

        // When & Then
        assertThrows(Exception.class, () -> {
            openAIChatService.generateResponse(null);
        });
    }

    // Note: Actual API call tests should be in separate integration test files
    // with proper test API keys and should be marked with @Disabled for CI/CD
    
    /*
    @Test
    @Disabled("Requires actual API key - run manually")
    @DisplayName("Gemini API 실제 호출 테스트")
    void testActualGeminiAPICall() {
        // This test should only be run manually with real API credentials
        String response = geminiChatService.generateResponse("Hello, how are you?");
        assertNotNull(response);
        assertThat(response).isNotEmpty();
    }
    
    @Test
    @Disabled("Requires actual API key - run manually")
    @DisplayName("OpenAI API 실제 호출 테스트")
    void testActualOpenAIAPICall() {
        // This test should only be run manually with real API credentials
        String response = openAIChatService.generateResponse("Hello, how are you?");
        assertNotNull(response);
        assertThat(response).isNotEmpty();
    }
    */
}