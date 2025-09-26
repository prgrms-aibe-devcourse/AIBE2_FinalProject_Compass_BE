package com.compass.domain.chat.orchestrator;

import com.compass.domain.chat.function.collection.ContinueFollowUpFunction;
import com.compass.domain.chat.function.collection.StartFollowUpFunction;
import com.compass.domain.chat.function.collection.SubmitTravelFormFunction;
import com.compass.domain.chat.function.planning.RecommendDestinationsFunction;
import com.compass.domain.chat.model.context.TravelContext;
import com.compass.domain.chat.model.dto.DestinationRecommendationDto;
import com.compass.domain.chat.model.enums.Intent;
import com.compass.domain.chat.model.enums.TravelPhase;
import com.compass.domain.chat.model.request.ChatRequest;
import com.compass.domain.chat.model.response.FollowUpResponse;
import com.compass.domain.chat.model.response.ChatResponse;
import com.compass.domain.chat.service.ChatThreadService;
import com.compass.domain.chat.service.TravelFormWorkflowService;
import com.compass.domain.chat.service.TravelPlanGenerationService;
import com.compass.domain.chat.collection.service.FormDataConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MainLLMOrchestrator {

    private final IntentClassifier intentClassifier;
    private final PhaseManager phaseManager;
    private final ContextManager contextManager;
    private final ResponseGenerator responseGenerator;
    private final ChatThreadService chatThreadService;
    private final PromptBuilder promptBuilder;
    private final FormDataConverter formDataConverter;
    private final TravelFormWorkflowService travelFormWorkflowService;
    private final TravelPlanGenerationService travelPlanGenerationService;

    private final SubmitTravelFormFunction submitTravelFormFunction;
    private final StartFollowUpFunction startFollowUpFunction;
    private final RecommendDestinationsFunction recommendDestinationsFunction;
    private final ContinueFollowUpFunction continueFollowUpFunction;

    /**
     * 모든 채팅 요청의 메인 진입점.
     * 요청에 폼 데이터가 포함되어 있는지 여부에 따라 처리를 분기합니다.
     */
    public ChatResponse processChat(ChatRequest request) {
        // MDC를 사용하여 로그에 컨텍스트 정보(threadId, userId)를 추가합니다.
        MDC.put("threadId", request.getThreadId());
        MDC.put("userId", request.getUserId());

        try {
            log.info("╔══════════════════════════════════════════════════════════════");
            log.info("║ 채팅 요청 처리 시작: Message = '{}'", request.getMessage());
            log.info("╚══════════════════════════════════════════════════════════════");

            // 모든 요청이 시작될 때 스레드 존재를 보장하고 사용자 메시지를 기록합니다.
            ensureChatThreadExists(request);
            saveUserMessage(request);

            // 1. 폼 데이터가 포함된 특별 요청인지 확인합니다.
            if (isFormSubmission(request)) {
                return handleFormSubmission(request);
            }

            // 2. 폼 데이터가 없는 일반 대화 요청을 처리합니다.
            return handleGeneralChatMessage(request);

        } finally {
            // 요청 처리가 끝나면 MDC에서 정보를 제거합니다.
            MDC.clear();
        }
    }

    /**
     * 요청이 폼 제출인지 확인하는 헬퍼 메소드입니다.
     */
    private boolean isFormSubmission(ChatRequest request) {
        if (request.getMetadata() instanceof java.util.Map) {
            var metadata = (java.util.Map<String, Object>) request.getMetadata();
            return "TRAVEL_FORM_SUBMIT".equals(metadata.get("type"));
        }
        return false;
    }

    /**
     * 빠른 입력 폼 제출 요청을 전문적으로 처리하는 메서드입니다.
     */
    private ChatResponse handleFormSubmission(ChatRequest request) {
        log.info("🎯 빠른입력폼 제출 감지 -> 처리를 시작합니다.");
        log.info("📍 [FORM] ThreadId: {}, UserId: {}", request.getThreadId(), request.getUserId());
        try {
            var context = contextManager.getOrCreateContext(request);
            log.info("📍 [CONTEXT] Current Phase: {}", context.getCurrentPhase());

            var metadata = (java.util.Map<String, Object>) request.getMetadata();
            log.info("📍 [METADATA] Raw metadata: {}", metadata);

            var formDataMap = (java.util.Map<String, Object>) metadata.get("formData");
            log.info("📍 [FORMDATA] Form data map: {}", formDataMap);

            var travelFormRequest = formDataConverter.convertFromFrontend(request.getUserId(), formDataMap);
            log.info("📍 [CONVERTED] TravelFormRequest: {}", travelFormRequest);

            context.updateFromFormSubmit(travelFormRequest);
            log.info("📍 [UPDATED] Context updated with form data");

            log.info("📍 [FUNCTION] Calling submitTravelFormFunction");
            ChatResponse validationResponse = submitTravelFormFunction.apply(travelFormRequest);
            log.info("📍 [RESPONSE] Validation response type: {}, phase: {}",
                validationResponse.getType(), validationResponse.getPhase());
            saveSystemMessage(request.getThreadId(), validationResponse.getContent());
            log.info("📍 [SAVED] System message saved");;

            String nextAction = validationResponse.getNextAction();

            boolean shouldPersist = !"START_FOLLOW_UP".equals(nextAction);
            boolean shouldTransition = "TRIGGER_PLAN_GENERATION".equals(nextAction);

            if (shouldTransition) {
                context.setCurrentPhase(TravelPhase.PLAN_GENERATION.name());
            }

            if (shouldPersist) {
                log.info("유효성 검사 통과 또는 목적지 미정 확인. 폼 데이터를 저장합니다.");
                travelFormWorkflowService.persistFormData(
                    context,
                    request.getThreadId(),
                    travelFormRequest,
                    shouldTransition
                );
            } else {
                log.warn("유효성 검사 실패. DB에 정보를 저장하지 않습니다.");
                contextManager.updateContext(context, context.getUserId());
            }

            return switch (nextAction) {
                case "RECOMMEND_DESTINATIONS" -> {
                    log.info("✅ '목적지 미정' 시나리오 -> RecommendDestinationsFunction 호출");
                    DestinationRecommendationDto recommendations = recommendDestinationsFunction.apply(travelFormRequest);
                    validationResponse.setData(recommendations);
                    validationResponse.setType("DESTINATION_RECOMMENDATION");
                    validationResponse.setPhase(context.getCurrentPhase());
                    validationResponse.setThreadId(request.getThreadId());
                    validationResponse.setCurrentPhase(context.getCurrentPhase());
                    yield validationResponse;
                }
                case "START_FOLLOW_UP" -> {
                    log.info("✅ 정보 부족 시나리오 -> StartFollowUpFunction 호출");
                    ChatResponse followUpQuestionResponse = startFollowUpFunction.apply(travelFormRequest);
                    saveSystemMessage(request.getThreadId(), followUpQuestionResponse.getContent());
                    followUpQuestionResponse.setPhase(context.getCurrentPhase());
                    followUpQuestionResponse.setThreadId(request.getThreadId());
                    followUpQuestionResponse.setCurrentPhase(context.getCurrentPhase());
                    yield followUpQuestionResponse;
                }
                case "TRIGGER_PLAN_GENERATION" -> {
                    log.info("✅ 정보 수집 완료 시나리오 -> PLAN_GENERATION으로 전환");
                    log.info("📍 [PHASE_TRANSITION] INFORMATION_COLLECTION -> PLAN_GENERATION");
                    log.info("📍 [PHASE_TRANSITION] ThreadId: {}, UserId: {}",
                        request.getThreadId(), context.getUserId());

                    // 🔥 Stage 3를 즉시 실행!
                    log.info("🚀 Stage 3 여행 계획 생성을 즉시 시작합니다!");
                    try {
                        Map<String, Object> travelPlan = travelPlanGenerationService.generateTravelPlan(context);
                        if (travelPlan != null && !travelPlan.isEmpty()) {
                            log.info("✅ Stage 3 여행 계획 생성 성공! 데이터를 응답에 포함합니다.");
                            log.info("📊 생성된 계획 키: {}", travelPlan.keySet());
                            validationResponse.setData(travelPlan);
                            validationResponse.setType("TRAVEL_PLAN_GENERATED");
                        } else {
                            log.warn("⚠️ Stage 3 실행했지만 여행 계획이 비어있습니다.");
                        }
                    } catch (Exception e) {
                        log.error("❌ Stage 3 실행 중 오류 발생: {}", e.getMessage(), e);
                        // 오류가 발생해도 Phase 전환은 진행
                    }

                    validationResponse.setPhase(TravelPhase.PLAN_GENERATION.name());
                    validationResponse.setThreadId(request.getThreadId());
                    validationResponse.setCurrentPhase(TravelPhase.PLAN_GENERATION.name());

                    log.info("📍 [PHASE_COMPLETE] Phase transition complete. New phase: {}",
                        TravelPhase.PLAN_GENERATION.name());
                    log.info("📍 [RESPONSE_READY] Response ready with phase: {}, threadId: {}, hasData: {}",
                        validationResponse.getPhase(), validationResponse.getThreadId(),
                        validationResponse.getData() != null);

                    yield validationResponse;
                }
                default -> {
                    validationResponse.setPhase(context.getCurrentPhase());
                    validationResponse.setThreadId(request.getThreadId());
                    validationResponse.setCurrentPhase(context.getCurrentPhase());
                    yield validationResponse;
                }
            };

        } catch (Exception e) {
            log.error("폼 데이터 처리 중 심각한 오류 발생: {}", e.getMessage(), e);
            return createErrorResponse("폼 데이터를 처리하는 중 오류가 발생했습니다.");
        }
    }

    /**
     * 일반적인 대화 메시지를 처리하는 메서드입니다.
     */
    private ChatResponse handleGeneralChatMessage(ChatRequest request) {
        var context = contextManager.getOrCreateContext(request);
        context.incrementConversation();

        var currentPhase = TravelPhase.valueOf(context.getCurrentPhase());
        var intent = intentClassifier.classify(request.getMessage(), context.isWaitingForTravelConfirmation());

        MDC.put("phase", currentPhase.name());
        MDC.put("intent", intent.name());
        log.info("║ 현재 Phase: {} ║ 분류된 Intent: {}", currentPhase, intent);

        // 정보 수집 단계에서는 ContinueFollowUpFunction을 통해 대화형으로 정보를 수집합니다.
        if (currentPhase == TravelPhase.INFORMATION_COLLECTION &&
                (intent == Intent.INFORMATION_COLLECTION || intent == Intent.DESTINATION_SEARCH)) {

            log.info("정보 수집 단계의 사용자 입력 감지 -> ContinueFollowUpFunction으로 처리 위임");
            var followUpRequest = new FollowUpResponse(request.getThreadId(), request.getMessage());
            ChatResponse functionResponse = continueFollowUpFunction.apply(followUpRequest);

            if (TravelPhase.PLAN_GENERATION.name().equals(functionResponse.getPhase())) {
                context.setCurrentPhase(TravelPhase.PLAN_GENERATION.name());
                contextManager.updateContext(context, context.getUserId());
            }

            saveSystemMessage(request.getThreadId(), functionResponse.getContent());
            // Phase 정보 추가
            functionResponse.setPhase(context.getCurrentPhase());
            functionResponse.setThreadId(request.getThreadId());
            functionResponse.setCurrentPhase(context.getCurrentPhase());
            return functionResponse;
        }

        // 그 외 일반적인 대화 처리
        var nextPhase = handlePhaseTransition(currentPhase, intent, context);
        handleConfirmationStatus(intent, context);

        var response = responseGenerator.generateResponse(request, intent, nextPhase, context, promptBuilder);
        saveSystemMessage(request.getThreadId(), response.getContent());
        // Phase와 ThreadId 정보 추가
        response.setPhase(nextPhase.name());
        response.setThreadId(request.getThreadId());
        response.setCurrentPhase(nextPhase.name());
        return response;
    }

    private void handleConfirmationStatus(Intent intent, TravelContext context) {
        if (context.isWaitingForTravelConfirmation()) {
            if (intent == Intent.CONFIRMATION) {
                log.info("║ 여행 계획 시작 확인 응답 감지 - 확인 대기 상태 해제");
                context.setWaitingForTravelConfirmation(false);
            } else if (intent != Intent.TRAVEL_PLANNING) {
                log.info("║ 다른 의도 감지 (Intent: {}) - 확인 대기 상태 해제", intent);
                context.setWaitingForTravelConfirmation(false);
            }
            contextManager.updateContext(context, context.getUserId());
        }
    }

    private TravelPhase handlePhaseTransition(TravelPhase currentPhase, Intent intent, TravelContext context) {
        var nextPhase = phaseManager.transitionPhase(context.getThreadId(), intent, context);
        if (nextPhase != currentPhase) {
            log.info("║ 🔄 Phase 전환 감지: {} → {}", currentPhase, nextPhase);
            context.setCurrentPhase(nextPhase.name());
            contextManager.updateContext(context, context.getUserId());
        }
        return nextPhase;
    }

    private void ensureChatThreadExists(ChatRequest request) {
        try {
            chatThreadService.ensureThreadExists(request.getThreadId(), request.getUserId());
        } catch (Exception e) {
            log.error("ChatThread 확인/생성 실패: {}", e.getMessage());
            throw new RuntimeException("대화 스레드 생성에 실패했습니다.", e);
        }
    }

    private void saveUserMessage(ChatRequest request) {
        chatThreadService.saveMessage(new ChatThreadService.MessageSaveRequest(
                request.getThreadId(), "user", request.getMessage()
        ));
    }

    private void saveSystemMessage(String threadId, String content) {
        if (content != null && !content.isBlank()) {
            chatThreadService.saveMessage(new ChatThreadService.MessageSaveRequest(
                    threadId, "assistant", content
            ));
        }
    }

    private ChatResponse createErrorResponse(String message) {
        return ChatResponse.builder().content(message).type("ERROR").build();
    }

    public void resetContext(String threadId, String userId) {
        contextManager.resetContext(threadId, userId);
    }
}
