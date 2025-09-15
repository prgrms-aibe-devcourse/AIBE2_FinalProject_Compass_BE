package com.compass.domain.chat2;

import com.compass.domain.chat2.controller.UnifiedChatController;
import com.compass.domain.chat2.orchestrator.MainLLMOrchestrator;
import com.compass.domain.chat2.service.IntentClassificationService;
import com.compass.domain.chat2.model.Intent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * UnifiedChatController 테스트
 * REQ-TRIP-001: MainLLMOrchestrator 연동 테스트
 */
class UnifiedChatControllerTest {

    @Mock
    private MainLLMOrchestrator mainLLMOrchestrator;

    @Mock
    private IntentClassificationService intentClassificationService;

    private UnifiedChatController unifiedChatController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        unifiedChatController = new UnifiedChatController(mainLLMOrchestrator);
    }

    @Test
    void testUnifiedChat_TravelPlanning() {
        // Given
        String userInput = "서울로 여행 계획을 만들어줘";
        String threadId = "test-thread-001";
        String expectedResponse = "여행 계획을 생성하겠습니다. 목적지, 날짜, 인원, 예산, 여행 스타일을 알려주세요.";
        
        when(mainLLMOrchestrator.orchestrate(userInput, threadId))
                .thenReturn(expectedResponse);

        UnifiedChatController.ChatRequest request = new UnifiedChatController.ChatRequest(userInput, threadId);

        // When
        ResponseEntity<Map<String, Object>> response = unifiedChatController.unifiedChat(request);

        // Then
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        
        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody);
        assertTrue((Boolean) responseBody.get("success"));
        assertEquals(threadId, responseBody.get("threadId"));
        assertEquals(expectedResponse, responseBody.get("response"));
        
        verify(mainLLMOrchestrator, times(1)).orchestrate(userInput, threadId);
    }

    @Test
    void testUnifiedChat_GeneralQuestion() {
        // Given
        String userInput = "오늘 날씨 어때?";
        String threadId = "test-thread-002";
        String expectedResponse = "일반 질문에 답변드리겠습니다. 여행 계획이 있으시면 언제든 말씀해주세요.";
        
        when(mainLLMOrchestrator.orchestrate(userInput, threadId))
                .thenReturn(expectedResponse);

        UnifiedChatController.ChatRequest request = new UnifiedChatController.ChatRequest(userInput, threadId);

        // When
        ResponseEntity<Map<String, Object>> response = unifiedChatController.unifiedChat(request);

        // Then
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        
        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody);
        assertTrue((Boolean) responseBody.get("success"));
        assertEquals(expectedResponse, responseBody.get("response"));
    }

    @Test
    void testUnifiedChat_ErrorHandling() {
        // Given
        String userInput = "테스트 입력";
        String threadId = "test-thread-003";
        
        when(mainLLMOrchestrator.orchestrate(userInput, threadId))
                .thenThrow(new RuntimeException("테스트 오류"));

        UnifiedChatController.ChatRequest request = new UnifiedChatController.ChatRequest(userInput, threadId);

        // When
        ResponseEntity<Map<String, Object>> response = unifiedChatController.unifiedChat(request);

        // Then
        assertNotNull(response);
        assertEquals(500, response.getStatusCode().value());
        
        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody);
        assertFalse((Boolean) responseBody.get("success"));
        assertEquals("처리 중 오류가 발생했습니다.", responseBody.get("error"));
    }
}
