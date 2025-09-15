package com.compass.domain.chat.service;

import com.compass.domain.chat.util.TravelParsingUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * NaturalLanguageParsingService 단위 테스트
 * LLM 파서와 Fallback 메커니즘 테스트
 */
@Tag("unit")
@DisplayName("자연어 파싱 서비스 테스트")
class NaturalLanguageParsingServiceTest {
    
    private NaturalLanguageParsingService parsingService;
    private VertexAiGeminiChatModel mockGeminiModel;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        mockGeminiModel = mock(VertexAiGeminiChatModel.class);
        objectMapper = new ObjectMapper();
        parsingService = new NaturalLanguageParsingService(mockGeminiModel, objectMapper);
    }
    
    @Test
    @DisplayName("LLM을 통한 목적지 파싱 성공")
    void parseDestination_WithLLM_Success() {
        // given
        String userInput = "제주도로 여행 가고 싶어요";
        ChatResponse mockResponse = mock(ChatResponse.class);
        Generation mockGeneration = mock(Generation.class);
        org.springframework.ai.chat.messages.AssistantMessage mockMessage = 
            mock(org.springframework.ai.chat.messages.AssistantMessage.class);
        
        when(mockMessage.getContent()).thenReturn("제주도");
        when(mockGeneration.getOutput()).thenReturn(mockMessage);
        when(mockResponse.getResult()).thenReturn(mockGeneration);
        when(mockGeminiModel.call(any(Prompt.class))).thenReturn(mockResponse);
        
        // when
        String destination = parsingService.parseDestination(userInput);
        
        // then
        assertThat(destination).isEqualTo("제주도");
        verify(mockGeminiModel, times(1)).call(any(Prompt.class));
    }
    
    @Test
    @DisplayName("LLM 실패 시 정규식 Fallback")
    void parseDestination_LLMFails_FallbackToRegex() {
        // given
        String userInput = "부산으로 여행 가고 싶어요";
        when(mockGeminiModel.call(any(Prompt.class)))
            .thenThrow(new RuntimeException("LLM API Error"));
        
        // when
        String destination = parsingService.parseDestination(userInput);
        
        // then
        assertThat(destination).isEqualTo("부산");
        verify(mockGeminiModel, times(1)).call(any(Prompt.class));
    }
    
    @Test
    @DisplayName("LLM이 UNKNOWN 반환 시 null 반환")
    void parseDestination_LLMReturnsUnknown_ReturnsNull() {
        // given
        String userInput = "어디든 좋아요";
        ChatResponse mockResponse = mock(ChatResponse.class);
        Generation mockGeneration = mock(Generation.class);
        org.springframework.ai.chat.messages.AssistantMessage mockMessage = 
            mock(org.springframework.ai.chat.messages.AssistantMessage.class);
        
        when(mockMessage.getContent()).thenReturn("UNKNOWN");
        when(mockGeneration.getOutput()).thenReturn(mockMessage);
        when(mockResponse.getResult()).thenReturn(mockGeneration);
        when(mockGeminiModel.call(any(Prompt.class))).thenReturn(mockResponse);
        
        // when
        String destination = parsingService.parseDestination(userInput);
        
        // then
        assertThat(destination).isNull();
    }
    
    @Test
    @DisplayName("LLM 없이 정규식만 사용")
    void parseDestination_NoLLM_UsesRegexOnly() {
        // given
        parsingService = new NaturalLanguageParsingService(null, new ObjectMapper());
        String userInput = "서울로 가고 싶어요";
        
        // when
        String destination = parsingService.parseDestination(userInput);
        
        // then
        assertThat(destination).isEqualTo("서울");
    }
    
    @Test
    @DisplayName("동행자 정보 LLM 파싱")
    void parseCompanions_WithLLM_Success() {
        // given
        String userInput = "친구들 3명이랑 같이 갈 거예요";
        ChatResponse mockResponse = mock(ChatResponse.class);
        Generation mockGeneration = mock(Generation.class);
        org.springframework.ai.chat.messages.AssistantMessage mockMessage = 
            mock(org.springframework.ai.chat.messages.AssistantMessage.class);
        
        when(mockMessage.getContent()).thenReturn("""
            {
                "numberOfTravelers": 4,
                "groupType": "friends"
            }
            """);
        when(mockGeneration.getOutput()).thenReturn(mockMessage);
        when(mockResponse.getResult()).thenReturn(mockGeneration);
        when(mockGeminiModel.call(any(Prompt.class))).thenReturn(mockResponse);
        
        // when
        java.util.Map<String, Object> companions = parsingService.parseCompanions(userInput);
        
        // then
        assertThat(companions).containsEntry("numberOfTravelers", 4);
        assertThat(companions).containsEntry("groupType", "friends");
    }
    
    @Test
    @DisplayName("예산 정보 LLM 파싱")
    void parseBudget_WithLLM_Success() {
        // given
        String userInput = "1인당 50만원 정도 생각하고 있어요";
        ChatResponse mockResponse = mock(ChatResponse.class);
        Generation mockGeneration = mock(Generation.class);
        org.springframework.ai.chat.messages.AssistantMessage mockMessage = 
            mock(org.springframework.ai.chat.messages.AssistantMessage.class);
        
        when(mockMessage.getContent()).thenReturn("""
            {
                "budgetLevel": "moderate",
                "budgetPerPerson": 500000,
                "currency": "KRW"
            }
            """);
        when(mockGeneration.getOutput()).thenReturn(mockMessage);
        when(mockResponse.getResult()).thenReturn(mockGeneration);
        when(mockGeminiModel.call(any(Prompt.class))).thenReturn(mockResponse);
        
        // when
        java.util.Map<String, Object> budget = parsingService.parseBudget(userInput);
        
        // then
        assertThat(budget).containsEntry("budgetLevel", "moderate");
        assertThat(budget).containsEntry("budgetPerPerson", 500000);
        assertThat(budget).containsEntry("currency", "KRW");
    }
    
    @Test
    @DisplayName("날짜 범위 LLM 파싱")
    void parseDates_WithLLM_Success() {
        // given
        String userInput = "12월 25일부터 27일까지 가려고 해요";
        ChatResponse mockResponse = mock(ChatResponse.class);
        Generation mockGeneration = mock(Generation.class);
        org.springframework.ai.chat.messages.AssistantMessage mockMessage = 
            mock(org.springframework.ai.chat.messages.AssistantMessage.class);
        
        when(mockMessage.getContent()).thenReturn("2024-12-25 ~ 2024-12-27");
        when(mockGeneration.getOutput()).thenReturn(mockMessage);
        when(mockResponse.getResult()).thenReturn(mockGeneration);
        when(mockGeminiModel.call(any(Prompt.class))).thenReturn(mockResponse);
        
        // when
        TravelParsingUtils.DateRange dateRange = parsingService.parseDates(userInput);
        
        // then
        assertThat(dateRange).isNotNull();
        // DateRange 파싱 로직은 TravelParsingUtils.parseDateRangeFromString에 의존
    }
}