package com.compass.domain.chat2.controller;

import com.compass.domain.chat2.dto.UnifiedChatRequest;
import com.compass.domain.chat2.orchestrator.MainLLMOrchestrator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * UnifiedChatController 테스트
 *
 * REQ-CHAT2-002: 통합 엔드포인트 테스트
 */
@WebMvcTest(UnifiedChatController.class)
@Tag("unit")
@DisplayName("UnifiedChatController 테스트")
class UnifiedChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MainLLMOrchestrator mainLLMOrchestrator;

    private UnifiedChatRequest testRequest;

    @BeforeEach
    void setUp() {
        testRequest = UnifiedChatRequest.builder()
            .message("제주도 여행 계획 세워줘")
            .threadId("test-thread-123")
            .build();
    }

    @Test
    @DisplayName("인증된 사용자가 통합 채팅 요청을 보낼 수 있다")
    @WithMockUser(username = "testuser")
    void testUnifiedChatWithAuthenticatedUser() throws Exception {
        // Given
        String expectedResponse = "제주도 여행 계획을 생성했습니다.";
        when(mainLLMOrchestrator.orchestrate(anyString(), anyString(), anyString()))
            .thenReturn(expectedResponse);

        // When & Then
        mockMvc.perform(post("/api/chat/unified")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value(expectedResponse))
            .andExpect(jsonPath("$.threadId").value("test-thread-123"))
            .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(mainLLMOrchestrator).orchestrate(
            eq("제주도 여행 계획 세워줘"),
            eq("test-thread-123"),
            eq("testuser")
        );
    }

    @Test
    @DisplayName("threadId가 없으면 새로 생성한다")
    @WithMockUser(username = "testuser")
    void testUnifiedChatWithoutThreadId() throws Exception {
        // Given
        UnifiedChatRequest requestWithoutThreadId = UnifiedChatRequest.builder()
            .message("여행 가고 싶어")
            .build();

        String expectedResponse = "어디로 여행을 가고 싶으신가요?";
        when(mainLLMOrchestrator.orchestrate(anyString(), anyString(), anyString()))
            .thenReturn(expectedResponse);

        // When & Then
        mockMvc.perform(post("/api/chat/unified")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestWithoutThreadId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value(expectedResponse))
            .andExpect(jsonPath("$.threadId").exists())
            .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(mainLLMOrchestrator).orchestrate(
            eq("여행 가고 싶어"),
            anyString(),
            eq("testuser")
        );
    }

    @Test
    @DisplayName("오케스트레이터 에러 시 에러 응답을 반환한다")
    @WithMockUser(username = "testuser")
    void testUnifiedChatWithOrchestratorError() throws Exception {
        // Given
        when(mainLLMOrchestrator.orchestrate(anyString(), anyString(), anyString()))
            .thenThrow(new RuntimeException("오케스트레이션 실패"));

        // When & Then
        mockMvc.perform(post("/api/chat/unified")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest)))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.message").value("요청을 처리하는 중 오류가 발생했습니다."))
            .andExpect(jsonPath("$.status").value("ERROR"))
            .andExpect(jsonPath("$.errorCode").value("CHAT2_001"));
    }

    @Test
    @DisplayName("사용 가능한 Function 목록을 조회할 수 있다")
    @WithMockUser
    void testGetAvailableFunctions() throws Exception {
        // Given
        Set<String> functions = Set.of(
            "analyzeUserInput",
            "startFollowUp",
            "generateTravelPlan"
        );
        when(mainLLMOrchestrator.getAvailableFunctions()).thenReturn(functions);

        // When & Then
        mockMvc.perform(get("/api/chat/functions")
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0]").exists());

        verify(mainLLMOrchestrator).getAvailableFunctions();
    }

    @Test
    @DisplayName("시스템 상태를 확인할 수 있다")
    @WithMockUser
    void testHealthCheck() throws Exception {
        // Given
        Set<String> functions = Set.of("function1", "function2", "function3");
        when(mainLLMOrchestrator.getAvailableFunctions()).thenReturn(functions);

        // When & Then
        mockMvc.perform(get("/api/chat/health")
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("healthy"))
            .andExpect(jsonPath("$.orchestrator").value("active"))
            .andExpect(jsonPath("$.functions").value(3));

        verify(mainLLMOrchestrator).logFunctionStatus();
        verify(mainLLMOrchestrator).getAvailableFunctions();
    }

    @Test
    @DisplayName("메시지가 비어있으면 유효성 검증에 실패한다")
    @WithMockUser
    void testUnifiedChatWithEmptyMessage() throws Exception {
        // Given
        UnifiedChatRequest invalidRequest = UnifiedChatRequest.builder()
            .message("")
            .build();

        // When & Then
        mockMvc.perform(post("/api/chat/unified")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(mainLLMOrchestrator);
    }
}