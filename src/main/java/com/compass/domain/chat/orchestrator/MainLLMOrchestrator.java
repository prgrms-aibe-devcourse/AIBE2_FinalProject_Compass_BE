package com.compass.domain.chat.orchestrator;

import com.compass.domain.chat.function.collection.AnalyzeUserInputFunction;
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
import com.compass.domain.chat.service.TravelInfoService;
import com.compass.domain.chat.collection.service.FormDataConverter; // ◀◀ [수정 2] FormDataConverter Import 경로 변경
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
    private final TravelInfoService travelInfoService;


    private final SubmitTravelFormFunction submitTravelFormFunction;
    private final StartFollowUpFunction startFollowUpFunction;
    private final RecommendDestinationsFunction recommendDestinationsFunction;
    private final ContinueFollowUpFunction continueFollowUpFunction;
    // AnalyzeUserInputFunction은 현재 직접 사용되지 않으므로, 필요시 주입
    // private final AnalyzeUserInputFunction analyzeUserInputFunction;



     // 모든 채팅 요청의 메인 진입점.
     // 요청에 폼 데이터가 포함되어 있는지 여부에 따라 처리를 분기합니다.
    public ChatResponse processChat(ChatRequest request) {
        log.info("╔══════════════════════════════════════════════════════════════");
        log.info("║ 채팅 요청 처리 시작: Thread ID = {}, User ID = {}, Message = '{}'",
                request.getThreadId(), request.getUserId(), request.getMessage());
        log.info("╚══════════════════════════════════════════════════════════════");

        // 모든 요청이 시작될 때 스레드 존재를 보장하고 사용자 메시지를 기록
        // 이렇게 하면 폼 제출 시에도 DB에 스레드가 생성되어, 다음 요청에서 상태가 유실되지 않습니다.
        ensureChatThreadExists(request);
        saveUserMessage(request);

        // 1. 폼 데이터가 포함된 특별 요청인지 확인
        if (request.getMetadata() != null && request.getMetadata() instanceof java.util.Map) {
            var metadata = (java.util.Map<String, Object>) request.getMetadata();
            if ("TRAVEL_FORM_SUBMIT".equals(metadata.get("type"))) {
                return handleFormSubmission(request, metadata);
            }
        }

        // 2. 폼 데이터가 없는 일반 대화 요청 처리
        return handleGeneralChatMessage(request);
    }

    /**
     * 빠른 입력 폼 제출 요청을 전문적으로 처리하는 메서드.
     * 모든 검증과 분기 처리는 Function에 위임하고, Orchestrator는 그 결과(nextAction)에 따라 후속 조치만 담당합니다.
     */
    private ChatResponse handleFormSubmission(ChatRequest request, java.util.Map<String, Object> metadata) {
        log.info("🎯 빠른입력폼 제출 감지 -> SubmitTravelFormFunction으로 모든 처리 위임");
        try {
            var context = contextManager.getOrCreateContext(request);
            var formDataMap = (java.util.Map<String, Object>) metadata.get("formData");
            var travelFormRequest = formDataConverter.convertFromFrontend(request.getUserId(), formDataMap);

            // 컨텍스트에 사용자가 제출한 최신 정보 우선 반영
            context.updateFromFormSubmit(travelFormRequest);
            contextManager.updateContext(context, context.getUserId());

            // 폼 제출에 대한 모든 검증과 분기 처리는 SubmitTravelFormFunction에 위임
            ChatResponse validationResponse = submitTravelFormFunction.apply(travelFormRequest);
            saveSystemMessage(request.getThreadId(), validationResponse.getContent());

            //  폼에서 받은 정보를 DB에 즉시 저장/업데이트
            // 이렇게 하면 대화가 중간에 끊겨도 정보가 보존되며, DB가 항상 최신 상태를 유지
            travelInfoService.saveTravelInfo(request.getThreadId(), travelFormRequest);

            // SubmitTravelFormFunction의 판단(nextAction)에 따라 후속 Function을 호출
            String nextAction = validationResponse.getNextAction();

            if ("RECOMMEND_DESTINATIONS".equals(nextAction)) {
                log.info("✅ '목적지 미정' 시나리오 -> RecommendDestinationsFunction 호출");
                DestinationRecommendationDto recommendations = recommendDestinationsFunction.apply(travelFormRequest);
                // 응답 데이터에 추천 목록을 추가하여 프론트엔드에 전달
                validationResponse.setData(recommendations);
                validationResponse.setType("DESTINATION_RECOMMENDATION");

            } else if ("START_FOLLOW_UP".equals(nextAction)) {
                log.info("✅ 정보 부족 시나리오 -> StartFollowUpFunction 호출");
                // StartFollowUpFunction을 호출하여 첫 번째 후속 질문을 생성
                ChatResponse followUpQuestionResponse = startFollowUpFunction.apply(travelFormRequest);
                saveSystemMessage(request.getThreadId(), followUpQuestionResponse.getContent());
                return followUpQuestionResponse; // 후속 질문 응답을 바로 반환

            } else if ("TRIGGER_PLAN_GENERATION".equals(nextAction)) {
                log.info("✅ 정보 수집 완료 시나리오 -> PLAN_GENERATION으로 전환"); 
                // DB 저장은 이미 위에서 완료되었으므로, 여기서는 Phase 전환만 담당합니다.
                phaseManager.savePhase(request.getThreadId(), TravelPhase.PLAN_GENERATION);
                context.setCurrentPhase(TravelPhase.PLAN_GENERATION.name());
                contextManager.updateContext(context, context.getUserId());
            }

            return validationResponse; // 최종 응답 반환

        } catch (Exception e) {
            log.error("폼 데이터 처리 중 심각한 오류 발생: {}", e.getMessage(), e);
            return createErrorResponse("폼 데이터를 처리하는 중 오류가 발생했습니다.");
        }
    }

    /**
     * 일반적인 대화 메시지를 처리하는 메서드.
     * 정보 수집 단계에서는 ContinueFollowUpFunction을 호출하여 대화형 정보 수집을 수행합니다.
     */
    private ChatResponse handleGeneralChatMessage(ChatRequest request) {
        var context = contextManager.getOrCreateContext(request);
        context.incrementConversation();

        var currentPhase = TravelPhase.valueOf(context.getCurrentPhase());
        var intent = intentClassifier.classify(request.getMessage(), context.isWaitingForTravelConfirmation());

        log.info("║ 현재 Phase: {} ║ 분류된 Intent: {}", currentPhase, intent);

        // 시나리오 3: 정보 수집 단계에서의 대화형 정보 제공 -> ContinueFollowUpFunction으로 위임
        if (currentPhase == TravelPhase.INFORMATION_COLLECTION &&
                (intent == Intent.INFORMATION_COLLECTION || intent == Intent.DESTINATION_SEARCH)) {

            log.info("정보 수집 단계의 사용자 입력 감지 -> ContinueFollowUpFunction으로 처리 위임");

            // Function에 전달할 요청 객체 생성 (threadId 포함)
            var followUpRequest = new FollowUpResponse(
                    request.getThreadId(),
                    request.getMessage()
            );

            // Function 호출
            ChatResponse functionResponse = continueFollowUpFunction.apply(followUpRequest);

            // Function이 Phase 전환까지 처리했으므로, 오케스트레이터는 컨텍스트의 Phase만 동기화
            if (TravelPhase.PLAN_GENERATION.name().equals(functionResponse.getPhase())) {
                context.setCurrentPhase(TravelPhase.PLAN_GENERATION.name());
                contextManager.updateContext(context, context.getUserId());
            }

            // Function이 생성한 최종 응답을 저장하고 반환
            saveSystemMessage(request.getThreadId(), functionResponse.getContent());
            return functionResponse;
        }

        // 그 외 일반적인 대화 처리
        var nextPhase = handlePhaseTransition(currentPhase, intent, context);
        handleConfirmationStatus(intent, context);

        log.info("╔══════════════════════════════════════════════════════════════");
        log.info("║ 일반 응답 생성 시작: Intent = {}, Phase = {}", intent, nextPhase);
        log.info("╚══════════════════════════════════════════════════════════════");
        var response = responseGenerator.generateResponse(request, intent, nextPhase, context, promptBuilder);
        saveSystemMessage(request.getThreadId(), response.getContent());
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
            log.info("╔══════════════════════════════════════════════════════════════");
            log.info("║ 🔄 Phase 전환 감지: {} → {}", currentPhase, nextPhase);
            log.info("╚══════════════════════════════════════════════════════════════");
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