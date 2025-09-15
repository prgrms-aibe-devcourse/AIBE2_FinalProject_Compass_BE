package com.compass.domain.chat2.orchestrator;

import com.compass.domain.chat2.model.Intent;
import com.compass.domain.chat2.service.IntentClassificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.model.function.FunctionCallingOptions;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * MainLLMOrchestrator 통합 테스트
 *
 * REQ-CHAT2-001: MainLLMOrchestrator 구현 테스트
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("MainLLMOrchestrator 테스트")
class MainLLMOrchestratorTest {

    @Mock
    private ChatModel chatModel;

    @Mock
    private IntentClassificationService intentClassificationService;

    @Mock
    private Map<String, FunctionCallback> allFunctions;

    @InjectMocks
    private MainLLMOrchestrator orchestrator;

    private String testThreadId;
    private String testUserId;

    @BeforeEach
    void setUp() {
        testThreadId = "test-thread-123";
        testUserId = "test-user-456";
    }

    @Test
    @DisplayName("여행 계획 요청을 정상적으로 처리한다")
    void testOrchestrateWithTravelPlanningIntent() {
        // Given
        String userInput = "제주도 3박4일 여행 계획 세워줘";
        Intent expectedIntent = Intent.TRAVEL_PLANNING;

        when(intentClassificationService.classifyIntent(userInput))
            .thenReturn(expectedIntent);

        // Mock Functions
        Map<String, FunctionCallback> mockFunctions = new HashMap<>();
        mockFunctions.put("analyzeUserInput", mock(FunctionCallback.class));
        mockFunctions.put("generateTravelPlan", mock(FunctionCallback.class));
        mockFunctions.put("searchWithPerplexity", mock(FunctionCallback.class));

        when(allFunctions.get(anyString())).thenAnswer(invocation -> {
            String functionName = invocation.getArgument(0);
            return mockFunctions.get(functionName);
        });

        // Mock ChatModel response
        AssistantMessage assistantMessage = new AssistantMessage("제주도 여행 계획을 생성했습니다.");
        Generation generation = new Generation(assistantMessage);
        ChatResponse mockResponse = new ChatResponse(generation);

        when(chatModel.call(any(Prompt.class), any(FunctionCallingOptions.class)))
            .thenReturn(mockResponse);

        // When
        String result = orchestrator.orchestrate(userInput, testThreadId, testUserId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).contains("제주도 여행 계획");

        verify(intentClassificationService).classifyIntent(userInput);
        verify(chatModel).call(any(Prompt.class), any(FunctionCallingOptions.class));
    }

    @Test
    @DisplayName("정보 수집이 필요한 경우 Follow-up을 시작한다")
    void testOrchestrateWithInformationCollection() {
        // Given
        String userInput = "여행 가고 싶어";
        Intent expectedIntent = Intent.INFORMATION_COLLECTION;

        when(intentClassificationService.classifyIntent(userInput))
            .thenReturn(expectedIntent);

        // Mock Functions
        Map<String, FunctionCallback> mockFunctions = new HashMap<>();
        mockFunctions.put("analyzeUserInput", mock(FunctionCallback.class));
        mockFunctions.put("startFollowUp", mock(FunctionCallback.class));

        when(allFunctions.get(anyString())).thenAnswer(invocation -> {
            String functionName = invocation.getArgument(0);
            return mockFunctions.get(functionName);
        });

        // Mock ChatModel response
        AssistantMessage assistantMessage = new AssistantMessage("어디로 여행을 가고 싶으신가요?");
        Generation generation = new Generation(assistantMessage);
        ChatResponse mockResponse = new ChatResponse(generation);

        when(chatModel.call(any(Prompt.class), any(FunctionCallingOptions.class)))
            .thenReturn(mockResponse);

        // When
        String result = orchestrator.orchestrate(userInput, testThreadId, testUserId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).contains("어디로");

        verify(intentClassificationService).classifyIntent(userInput);
        verify(chatModel).call(any(Prompt.class), any(FunctionCallingOptions.class));
    }

    @Test
    @DisplayName("에러 발생 시 적절한 에러 메시지를 반환한다")
    void testOrchestrateWithError() {
        // Given
        String userInput = "테스트 입력";

        when(intentClassificationService.classifyIntent(userInput))
            .thenThrow(new RuntimeException("Intent 분류 실패"));

        // When
        String result = orchestrator.orchestrate(userInput, testThreadId, testUserId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).contains("요청을 처리하는 중 오류가 발생했습니다");

        verify(intentClassificationService).classifyIntent(userInput);
        verifyNoInteractions(chatModel);
    }

    @Test
    @DisplayName("사용 가능한 Function 목록을 조회한다")
    void testGetAvailableFunctions() {
        // Given
        Map<String, FunctionCallback> mockFunctions = new HashMap<>();
        mockFunctions.put("function1", mock(FunctionCallback.class));
        mockFunctions.put("function2", mock(FunctionCallback.class));
        mockFunctions.put("function3", mock(FunctionCallback.class));

        when(allFunctions.keySet()).thenReturn(mockFunctions.keySet());

        // When
        var availableFunctions = orchestrator.getAvailableFunctions();

        // Then
        assertThat(availableFunctions).hasSize(3);
        assertThat(availableFunctions).contains("function1", "function2", "function3");
    }

    @Test
    @DisplayName("알 수 없는 Intent에 대해 기본 Function을 사용한다")
    void testOrchestrateWithUnknownIntent() {
        // Given
        String userInput = "알 수 없는 요청";
        Intent expectedIntent = Intent.UNKNOWN;

        when(intentClassificationService.classifyIntent(userInput))
            .thenReturn(expectedIntent);

        // Mock Functions
        Map<String, FunctionCallback> mockFunctions = new HashMap<>();
        mockFunctions.put("analyzeUserInput", mock(FunctionCallback.class));

        when(allFunctions.get("analyzeUserInput"))
            .thenReturn(mockFunctions.get("analyzeUserInput"));

        // Mock ChatModel response
        AssistantMessage assistantMessage = new AssistantMessage("무엇을 도와드릴까요?");
        Generation generation = new Generation(assistantMessage);
        ChatResponse mockResponse = new ChatResponse(generation);

        when(chatModel.call(any(Prompt.class), any(FunctionCallingOptions.class)))
            .thenReturn(mockResponse);

        // When
        String result = orchestrator.orchestrate(userInput, testThreadId, testUserId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).contains("무엇을 도와드릴까요");

        verify(intentClassificationService).classifyIntent(userInput);
        verify(chatModel).call(any(Prompt.class), any(FunctionCallingOptions.class));
    }
}