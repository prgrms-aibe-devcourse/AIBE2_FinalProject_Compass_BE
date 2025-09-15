package com.compass.domain.chat2.orchestrator;

import com.compass.domain.chat2.model.Intent;
import com.compass.domain.chat2.service.IntentClassificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.model.function.FunctionCallingOptions;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.compass.domain.chat2.orchestrator.OrchestratorConstants.*;

/**
 * MainLLMOrchestrator - Compass 시스템의 중앙 지휘자
 *
 * REQ-CHAT2-001: 모든 사용자 요청을 받아 적절한 Function을 선택하고 실행
 * REQ-CHAT2-004: Intent 분류 및 라우팅
 * REQ-CHAT2-006: Function Calling으로 도메인 통합
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MainLLMOrchestrator {

    private final ChatModel chatModel;
    private final IntentClassificationService intentClassificationService;
    private final Map<String, FunctionCallback> allFunctions;

    /**
     * 사용자 요청을 오케스트레이션하는 메인 메서드
     *
     * @param userInput 사용자 입력
     * @param threadId 대화 스레드 ID
     * @param userId 사용자 ID
     * @return 최종 응답
     */
    public String orchestrate(String userInput, String threadId, String userId) {
        log.info(LOG_ORCHESTRATION_START, threadId, userId);

        try {
            OrchestrationContext context = buildContext(userInput, threadId, userId);
            ChatResponse response = executeWithContext(context);
            return processResponse(response, threadId);
        } catch (Exception e) {
            log.error(LOG_ORCHESTRATION_ERROR, threadId, e);
            return handleError(e);
        }
    }

    /**
     * 오케스트레이션 컨텍스트 구축
     */
    private OrchestrationContext buildContext(String userInput, String threadId, String userId) {
        Intent intent = classifyUserIntent(userInput);
        List<String> functions = selectFunctionsForIntent(intent);

        return OrchestrationContext.builder()
            .userInput(userInput)
            .threadId(threadId)
            .userId(userId)
            .intent(intent)
            .selectedFunctions(functions)
            .functionOptions(buildFunctionOptions(functions))
            .prompt(buildPrompt(userInput, threadId, userId))
            .build();
    }

    /**
     * 사용자 의도 분류
     */
    private Intent classifyUserIntent(String userInput) {
        Intent intent = intentClassificationService.classifyIntent(userInput);
        log.info(LOG_INTENT_CLASSIFIED, intent);
        return intent;
    }

    /**
     * 컨텍스트 기반 LLM 실행
     */
    private ChatResponse executeWithContext(OrchestrationContext context) {
        return chatModel.call(context.getPrompt(), context.getFunctionOptions());
    }

    /**
     * 응답 처리
     */
    private String processResponse(ChatResponse response, String threadId) {
        if (!isValidResponse(response)) {
            return ERROR_NULL_RESPONSE;
        }

        String content = extractContent(response);
        log.info(LOG_ORCHESTRATION_COMPLETE, threadId);
        return postProcessResponse(content);
    }

    /**
     * 응답 유효성 검증
     */
    private boolean isValidResponse(ChatResponse response) {
        return response != null && response.getResult() != null;
    }

    /**
     * 응답 컨텐츠 추출
     */
    private String extractContent(ChatResponse response) {
        return response.getResult().getOutput().getContent();
    }

    /**
     * Intent에 따라 필요한 Function들을 선택
     */
    private List<String> selectFunctionsForIntent(Intent intent) {
        return switch (intent) {
            case TRAVEL_PLANNING -> List.of(
                "analyzeUserInput",
                "generateTravelPlan",
                "searchWithPerplexity"
            );
            case INFORMATION_COLLECTION -> List.of(
                "analyzeUserInput",
                "startFollowUp"
            );
            case IMAGE_UPLOAD -> List.of(
                "uploadToS3AndOCR",
                "extractFlightInfo",
                "extractHotelInfo"
            );
            case GENERAL_QUESTION -> List.of(
                "handleGeneralQuestions",
                "redirectToTravel"
            );
            case QUICK_INPUT -> List.of(
                "processQuickInput",
                "generateTravelPlan"
            );
            default -> List.of("analyzeUserInput");
        };
    }

    /**
     * Function 옵션 구축
     */
    private FunctionCallingOptions buildFunctionOptions(List<String> functionNames) {
        Set<FunctionCallback> callbacks = resolveFunctionCallbacks(functionNames);
        log.info(LOG_FUNCTIONS_SELECTED, functionNames);

        return FunctionCallingOptions.builder()
            .functions(callbacks)
            .build();
    }

    /**
     * Function 콜백 해결
     */
    private Set<FunctionCallback> resolveFunctionCallbacks(List<String> functionNames) {
        return functionNames.stream()
            .map(allFunctions::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    /**
     * 프롬프트 구축
     */
    private Prompt buildPrompt(String userInput, String threadId, String userId) {
        Map<String, Object> variables = Map.of(
            "userInput", userInput,
            "threadId", threadId,
            "userId", userId,
            "systemPrompt", SYSTEM_PROMPT
        );

        return new PromptTemplate(PROMPT_TEMPLATE, variables).create();
    }


    /**
     * 응답 후처리
     */
    private String postProcessResponse(String content) {
        // 필요한 경우 응답 포맷팅, 정제 등 수행
        return content;
    }

    /**
     * 에러 처리
     */
    private String handleError(Exception e) {
        log.error("오케스트레이션 중 에러 발생", e);

        if (e.getMessage() != null && e.getMessage().contains("API")) {
            return ERROR_API_LIMIT_MESSAGE;
        }

        return ERROR_GENERAL_MESSAGE;
    }

    /**
     * 사용 가능한 Function 목록 조회
     */
    public Set<String> getAvailableFunctions() {
        return allFunctions.keySet();
    }

    /**
     * Function 등록 상태 확인
     */
    public void logFunctionStatus() {
        log.info(LOG_FUNCTION_STATUS);
        allFunctions.forEach((name, callback) -> {
            log.info(LOG_FUNCTION_ITEM, name, callback.getDescription());
        });
    }
}