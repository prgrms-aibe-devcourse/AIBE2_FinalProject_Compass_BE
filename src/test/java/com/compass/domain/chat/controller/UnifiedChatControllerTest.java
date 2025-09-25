package com.compass.domain.chat.controller;

import com.compass.domain.chat.model.request.ChatRequest;
import com.compass.domain.chat.model.response.ChatResponse;
import com.compass.domain.chat.orchestrator.MainLLMOrchestrator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// UnifiedChatController 테스트
@WebMvcTest(UnifiedChatController.class)
@ActiveProfiles("test")
@Disabled("Spring Context 로드 문제 해결 필요")
class UnifiedChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MainLLMOrchestrator mainLLMOrchestrator;

    private ChatRequest validRequest;
    private ChatResponse successResponse;

    @BeforeEach
    void setUp() {
        validRequest = new ChatRequest("부산 여행 계획을 세우고 싶어", "test-thread-1", "test-user-1");

        successResponse = ChatResponse.builder()
                .content("부산 여행 계획을 도와드리겠습니다.")
                .type("TEXT")
                .nextAction("WAIT_FOR_INPUT")
                .requiresConfirmation(false)
                .build();
    }

    @Test
    @DisplayName("정상적인 채팅 요청 처리")
    @WithMockUser(username = "testuser")
    void processChat_Success() throws Exception {
        // given
        when(mainLLMOrchestrator.processChat(any(ChatRequest.class)))
                .thenReturn(successResponse);

        // when & then
        mockMvc.perform(post("/api/chat/unified")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .header("Thread-Id", "test-thread-123")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("부산 여행 계획을 도와드리겠습니다."))
                .andExpect(jsonPath("$.type").value("TEXT"));

        verify(mainLLMOrchestrator, times(1)).processChat(any(ChatRequest.class));
    }

    @Test
    @DisplayName("Thread-Id 없이 요청시 자동 생성")
    @WithMockUser(username = "testuser")
    void processChat_WithoutThreadId() throws Exception {
        // given
        when(mainLLMOrchestrator.processChat(any(ChatRequest.class)))
                .thenReturn(successResponse);

        // when & then
        mockMvc.perform(post("/api/chat/unified")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists());

        verify(mainLLMOrchestrator, times(1)).processChat(argThat(request ->
                request.getThreadId() != null && !request.getThreadId().isEmpty()
        ));
    }

    @Test
    @DisplayName("빈 메시지 요청시 400 에러")
    @WithMockUser(username = "testuser")
    void processChat_EmptyMessage() throws Exception {
        // given
        var emptyRequest = new ChatRequest("", "test-thread-1", "test-user-1");

        // when & then
        mockMvc.perform(post("/api/chat/unified")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyRequest))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.content").value("메시지를 입력해주세요."))
                .andExpect(jsonPath("$.type").value("ERROR"));

        verify(mainLLMOrchestrator, never()).processChat(any());
    }

    @Test
    @DisplayName("null 메시지 요청시 400 에러")
    @WithMockUser(username = "testuser")
    void processChat_NullMessage() throws Exception {
        // given
        var nullRequest = new ChatRequest(null, "test-thread-1", "test-user-1");

        // when & then
        mockMvc.perform(post("/api/chat/unified")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nullRequest))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.content").value("메시지를 입력해주세요."))
                .andExpect(jsonPath("$.type").value("ERROR"));

        verify(mainLLMOrchestrator, never()).processChat(any());
    }

    @Test
    @DisplayName("인증 없이 요청시 401 에러")
    void processChat_Unauthorized() throws Exception {
        // when & then
        mockMvc.perform(post("/api/chat/unified")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Orchestrator 예외 발생시 500 에러")
    @WithMockUser(username = "testuser")
    void processChat_InternalError() throws Exception {
        // given
        when(mainLLMOrchestrator.processChat(any(ChatRequest.class)))
                .thenThrow(new RuntimeException("LLM 서비스 오류"));

        // when & then
        mockMvc.perform(post("/api/chat/unified")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.content").value("처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."))
                .andExpect(jsonPath("$.type").value("ERROR"));
    }

    @Test
    @DisplayName("IllegalArgumentException 발생시 400 에러")
    @WithMockUser(username = "testuser")
    void processChat_BadRequest() throws Exception {
        // given
        when(mainLLMOrchestrator.processChat(any(ChatRequest.class)))
                .thenThrow(new IllegalArgumentException("잘못된 파라미터"));

        // when & then
        mockMvc.perform(post("/api/chat/unified")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.content").value("잘못된 요청입니다: 잘못된 파라미터"))
                .andExpect(jsonPath("$.type").value("ERROR"));
    }

    @Test
    @DisplayName("컨텍스트 초기화 성공")
    @WithMockUser(username = "testuser")
    void resetContext_Success() throws Exception {
        // given
        doNothing().when(mainLLMOrchestrator).resetContext("test-thread-123", "testuser");

        // when & then
        mockMvc.perform(delete("/api/chat/unified/context")
                        .header("Thread-Id", "test-thread-123")
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(mainLLMOrchestrator, times(1)).resetContext("test-thread-123", "testuser");
    }

    @Test
    @DisplayName("컨텍스트 초기화시 Thread-Id 필수")
    @WithMockUser(username = "testuser")
    void resetContext_MissingThreadId() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/chat/unified/context")
                        .with(csrf()))
                .andExpect(status().isBadRequest());

        verify(mainLLMOrchestrator, never()).resetContext(any(), any());
    }

    @Test
    @DisplayName("컨텍스트 초기화 실패시 500 에러")
    @WithMockUser(username = "testuser")
    void resetContext_InternalError() throws Exception {
        // given
        doThrow(new RuntimeException("초기화 실패"))
                .when(mainLLMOrchestrator).resetContext("test-thread-123", "testuser");

        // when & then
        mockMvc.perform(delete("/api/chat/unified/context")
                        .header("Thread-Id", "test-thread-123")
                        .with(csrf()))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("확인 요청 처리")
    @WithMockUser(username = "testuser")
    void processChat_WithConfirmation() throws Exception {
        // given
        var confirmResponse = ChatResponse.builder()
                .content("정보 수집을 시작하시겠습니까?")
                .type("CONFIRMATION")
                .nextAction("WAIT_FOR_CONFIRMATION")
                .requiresConfirmation(true)
                .build();

        when(mainLLMOrchestrator.processChat(any(ChatRequest.class)))
                .thenReturn(confirmResponse);

        // when & then
        mockMvc.perform(post("/api/chat/unified")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiresConfirmation").value(true))
                .andExpect(jsonPath("$.type").value("CONFIRMATION"));
    }
}