package com.compass.domain.chat2.orchestrator;

import com.compass.domain.chat2.model.Intent;
import com.compass.domain.chat2.model.TravelPhase;
import com.compass.domain.chat2.service.IntentClassificationService;
import com.compass.domain.chat2.service.TravelPhaseManager;
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
    private final TravelPhaseManager phaseManager;
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
     * 오케스트레이션 컨텍스트 구축 (Phase 인식)
     */
    private OrchestrationContext buildContext(String userInput, String threadId, String userId) {
        // 1. Intent 분류
        Intent intent = classifyUserIntent(userInput);

        // 2. 현재 Phase 결정
        TravelPhase currentPhase = determinePhase(threadId, intent);
        log.info("현재 Phase: {} (threadId: {})", currentPhase.getKoreanName(), threadId);

        // 3. Phase에 맞는 Function 선택
        List<String> functions = selectFunctionsForPhase(currentPhase, intent);

        // 4. Phase 데이터 준비
        Map<String, Object> phaseData = preparePhaseData(threadId, currentPhase);

        return OrchestrationContext.builder()
            .userInput(userInput)
            .threadId(threadId)
            .userId(userId)
            .intent(intent)
            .currentPhase(currentPhase)
            .phaseData(phaseData)
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
     * 응답 처리 (Phase 전환 포함)
     */
    private String processResponse(ChatResponse response, String threadId) {
        if (!isValidResponse(response)) {
            return ERROR_NULL_RESPONSE;
        }

        String content = extractContent(response);

        // Phase 전환 체크
        checkAndTransitionPhase(threadId, content);

        // 진행률 표시
        int progress = phaseManager.calculatePhaseProgress(threadId);
        TravelPhase currentPhase = phaseManager.getCurrentPhase(threadId);

        log.info("[{}] {} - 진행률: {}% (threadId: {})",
            currentPhase.name(),
            currentPhase.getKoreanName(),
            progress,
            threadId);

        log.info(LOG_ORCHESTRATION_COMPLETE, threadId);
        return postProcessResponse(content);
    }

    /**
     * Phase 전환 체크 및 실행
     */
    private void checkAndTransitionPhase(String threadId, String responseContent) {
        // 응답 내용에서 Phase 전환 신호 감지
        if (responseContent.contains("정보 수집 완료") ||
            responseContent.contains("계획 생성 시작")) {
            phaseManager.transitionPhase(
                threadId,
                TravelPhase.PLAN_GENERATION
            );
        } else if (responseContent.contains("계획 생성 완료")) {
            phaseManager.transitionPhase(
                threadId,
                TravelPhase.FEEDBACK_REFINEMENT
            );
        } else if (responseContent.contains("최종 확정")) {
            phaseManager.transitionPhase(
                threadId,
                TravelPhase.COMPLETION
            );
        }
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
     * 현재 Phase 결정
     */
    private TravelPhase determinePhase(String threadId, Intent intent) {
        // 현재 Phase 조회
        TravelPhase currentPhase = phaseManager.getCurrentPhase(threadId);

        // 초기 상태면 Intent 기반으로 Phase 설정
        if (currentPhase == TravelPhase.INITIALIZATION) {
            TravelPhase newPhase = phaseManager.determineInitialPhase(intent);
            phaseManager.transitionPhase(threadId, newPhase);
            return newPhase;
        }

        // Phase 완료 체크 및 자동 전환
        if (phaseManager.isPhaseComplete(threadId)) {
            TravelPhase nextPhase = getNextPhase(currentPhase);
            if (phaseManager.transitionPhase(threadId, nextPhase)) {
                return nextPhase;
            }
        }

        return currentPhase;
    }

    /**
     * 다음 Phase 결정
     */
    private TravelPhase getNextPhase(TravelPhase currentPhase) {
        return switch (currentPhase) {
            case INITIALIZATION -> TravelPhase.INFORMATION_COLLECTION;
            case INFORMATION_COLLECTION -> TravelPhase.PLAN_GENERATION;
            case PLAN_GENERATION -> TravelPhase.FEEDBACK_REFINEMENT;
            case FEEDBACK_REFINEMENT -> TravelPhase.COMPLETION;
            case COMPLETION -> TravelPhase.COMPLETION;
        };
    }

    /**
     * Phase에 맞는 Function 선택
     */
    private List<String> selectFunctionsForPhase(TravelPhase phase, Intent intent) {
        // Phase별 기본 Function
        List<String> phaseFunctions = phaseManager.getPhaseAppropriateFunctions(phase);

        // Intent에 따른 추가 조정
        if (phase == TravelPhase.INFORMATION_COLLECTION) {
            // 정보 수집 단계에서는 빠른 입력 폼 우선
            if (intent == Intent.TRAVEL_PLANNING) {
                return List.of(
                    "showQuickInputForm",  // 최우선
                    "analyzeUserInput",
                    "startFollowUp"
                );
            }
        }

        return phaseFunctions;
    }

    /**
     * Phase 데이터 준비
     */
    private Map<String, Object> preparePhaseData(String threadId, TravelPhase phase) {
        Map<String, Object> data = new HashMap<>();

        // Phase별 필요 데이터 로드
        switch (phase) {
            case INFORMATION_COLLECTION -> {
                // 이미 수집된 정보 로드
                data.put("collectedInfo", loadCollectedInfo(threadId));
                data.put("progress", phaseManager.calculatePhaseProgress(threadId));
            }
            case PLAN_GENERATION -> {
                // 수집 완료된 정보 로드
                data.put("travelInfo", loadCompleteTravelInfo(threadId));
            }
            case FEEDBACK_REFINEMENT -> {
                // 생성된 계획 로드
                data.put("generatedPlan", loadGeneratedPlan(threadId));
            }
        }

        return data;
    }

    /**
     * Intent에 따라 필요한 Function들을 선택 (레거시 - Phase 도입 전)
     * @deprecated Phase 기반 선택으로 대체됨
     */
    @Deprecated
    private List<String> selectFunctionsForIntent(Intent intent) {
        // Phase 기반 선택으로 대체
        return List.of();
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

    /**
     * 수집된 정보 로드 (헬퍼 메소드)
     */
    private Map<String, Object> loadCollectedInfo(String threadId) {
        // TODO: DB에서 수집된 정보 로드
        return new HashMap<>();
    }

    /**
     * 완전한 여행 정보 로드 (헬퍼 메소드)
     */
    private Map<String, Object> loadCompleteTravelInfo(String threadId) {
        // TODO: DB에서 완전한 여행 정보 로드
        return new HashMap<>();
    }

    /**
     * 생성된 계획 로드 (헬퍼 메소드)
     */
    private Map<String, Object> loadGeneratedPlan(String threadId) {
        // TODO: DB에서 생성된 계획 로드
        return new HashMap<>();
    }
}