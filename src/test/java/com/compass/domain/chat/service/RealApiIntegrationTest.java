package com.compass.domain.chat.service;

import com.compass.domain.chat.service.impl.GeminiChatService;
import com.compass.domain.chat.service.impl.OpenAIChatService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Real API Integration Tests
 * These tests make actual API calls to Gemini and OpenAI
 * Run with actual API keys set in environment variables
 */
@SpringBootTest(properties = {
    "spring.datasource.url=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"
})
@ActiveProfiles("test")
@DisplayName("실제 API 통합 테스트")
public class RealApiIntegrationTest {

    @Autowired(required = false)
    private GeminiChatService geminiChatService;

    @Autowired(required = false)
    private OpenAIChatService openAIChatService;

    @Test
    @DisplayName("Gemini API 실제 호출 테스트 - 간단한 질문")
    void testGeminiApiCall_SimpleQuestion() {
        // Given
        assertNotNull(geminiChatService, "GeminiChatService should be available");
        String userMessage = "Say 'Hello from Gemini' and nothing else.";
        
        // When
        System.out.println("Calling Gemini API with message: " + userMessage);
        String response = geminiChatService.generateResponse(userMessage);
        
        // Then
        System.out.println("Gemini response: " + response);
        assertNotNull(response, "Response should not be null");
        assertFalse(response.isEmpty(), "Response should not be empty");
        assertTrue(response.toLowerCase().contains("hello") || response.contains("Gemini"), 
                   "Response should contain expected content");
    }

    @Test
    @DisplayName("Gemini API 실제 호출 테스트 - 시스템 프롬프트 포함")
    @EnabledIfEnvironmentVariable(named = "GOOGLE_CLOUD_PROJECT_ID", matches = ".+")
    void testGeminiApiCall_WithSystemPrompt() {
        // Given
        assertNotNull(geminiChatService, "GeminiChatService should be available");
        String systemPrompt = "You are a helpful travel assistant. Be concise.";
        String userMessage = "What is the capital of South Korea?";
        
        // When
        System.out.println("Calling Gemini API with system prompt");
        String response = geminiChatService.generateResponse(systemPrompt, userMessage);
        
        // Then
        System.out.println("Gemini response with system prompt: " + response);
        assertNotNull(response, "Response should not be null");
        assertTrue(response.toLowerCase().contains("seoul"), 
                   "Response should mention Seoul");
    }

    @Test
    @DisplayName("OpenAI API 실제 호출 테스트 - 간단한 질문")
    @EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = "sk-.+")
    void testOpenAIApiCall_SimpleQuestion() {
        // Given
        assertNotNull(openAIChatService, "OpenAIChatService should be available");
        String userMessage = "Say 'Hello from OpenAI' and nothing else.";
        
        // When
        System.out.println("Calling OpenAI API with message: " + userMessage);
        String response = openAIChatService.generateResponse(userMessage);
        
        // Then
        System.out.println("OpenAI response: " + response);
        assertNotNull(response, "Response should not be null");
        assertFalse(response.isEmpty(), "Response should not be empty");
        assertTrue(response.toLowerCase().contains("hello") || response.toLowerCase().contains("openai"), 
                   "Response should contain expected content");
    }

    @Test
    @DisplayName("OpenAI API 실제 호출 테스트 - 시스템 프롬프트 포함")
    @EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = "sk-.+")
    void testOpenAIApiCall_WithSystemPrompt() {
        // Given
        assertNotNull(openAIChatService, "OpenAIChatService should be available");
        String systemPrompt = "You are a helpful travel assistant. Be concise.";
        String userMessage = "What is the capital of Japan?";
        
        // When
        System.out.println("Calling OpenAI API with system prompt");
        String response = openAIChatService.generateResponse(systemPrompt, userMessage);
        
        // Then
        System.out.println("OpenAI response with system prompt: " + response);
        assertNotNull(response, "Response should not be null");
        assertTrue(response.toLowerCase().contains("tokyo"), 
                   "Response should mention Tokyo");
    }

    @Test
    @DisplayName("Gemini API 에러 처리 테스트")
    @EnabledIfEnvironmentVariable(named = "GOOGLE_CLOUD_PROJECT_ID", matches = ".+")
    void testGeminiApiCall_ErrorHandling() {
        // Given
        assertNotNull(geminiChatService, "GeminiChatService should be available");
        
        // When & Then
        assertThrows(RuntimeException.class, () -> {
            geminiChatService.generateResponse(null);
        }, "Should throw exception for null input");
        
        assertThrows(RuntimeException.class, () -> {
            geminiChatService.generateResponse("");
        }, "Should throw exception for empty input");
    }

    @Test
    @DisplayName("OpenAI API 에러 처리 테스트")
    @EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = "sk-.+")
    void testOpenAIApiCall_ErrorHandling() {
        // Given
        assertNotNull(openAIChatService, "OpenAIChatService should be available");
        
        // When & Then
        assertThrows(RuntimeException.class, () -> {
            openAIChatService.generateResponse(null);
        }, "Should throw exception for null input");
        
        assertThrows(RuntimeException.class, () -> {
            openAIChatService.generateResponse("");
        }, "Should throw exception for empty input");
    }

    @Test
    @DisplayName("Gemini API 응답 시간 측정")
    @EnabledIfEnvironmentVariable(named = "GOOGLE_CLOUD_PROJECT_ID", matches = ".+")
    void testGeminiApiCall_ResponseTime() {
        // Given
        assertNotNull(geminiChatService, "GeminiChatService should be available");
        String userMessage = "What is 2+2?";
        
        // When
        long startTime = System.currentTimeMillis();
        String response = geminiChatService.generateResponse(userMessage);
        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;
        
        // Then
        System.out.println("Gemini response time: " + responseTime + " ms");
        assertNotNull(response);
        assertTrue(responseTime < 10000, "Response should be received within 10 seconds");
        assertTrue(response.contains("4"), "Response should contain the answer 4");
    }

    @Test
    @DisplayName("OpenAI API 응답 시간 측정")
    @EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = "sk-.+")
    void testOpenAIApiCall_ResponseTime() {
        // Given
        assertNotNull(openAIChatService, "OpenAIChatService should be available");
        String userMessage = "What is 3+3?";
        
        // When
        long startTime = System.currentTimeMillis();
        String response = openAIChatService.generateResponse(userMessage);
        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;
        
        // Then
        System.out.println("OpenAI response time: " + responseTime + " ms");
        assertNotNull(response);
        assertTrue(responseTime < 10000, "Response should be received within 10 seconds");
        assertTrue(response.contains("6"), "Response should contain the answer 6");
    }
}