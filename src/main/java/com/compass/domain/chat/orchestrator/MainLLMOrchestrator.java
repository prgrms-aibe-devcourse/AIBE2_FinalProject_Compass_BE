package com.compass.domain.chat.orchestrator;

import com.compass.domain.chat.model.context.TravelContext;
import com.compass.domain.chat.model.enums.Intent;
import com.compass.domain.chat.model.enums.TravelPhase;
import com.compass.domain.chat.model.request.ChatRequest;
import com.compass.domain.chat.model.response.ChatResponse;
import com.compass.domain.chat.service.ChatThreadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// 메인 오케스트레이터 서비스
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


    // 채팅 요청 처리
    public ChatResponse processChat(ChatRequest request) {
        log.info("╔══════════════════════════════════════════════════════════════");
        log.info("║ 채팅 요청 처리 시작");
        log.info("║ Thread ID: {}", request.getThreadId());
        log.info("║ User ID: {}", request.getUserId());
        log.info("║ Message: {}", request.getMessage());
        log.info("╚══════════════════════════════════════════════════════════════");

        // 1. 사용자 메시지 저장
        saveUserMessage(request);

        // 2. 컨텍스트 조회 또는 생성
        var context = contextManager.getOrCreateContext(request);

        // 3. 대화 횟수 증가
        context.incrementConversation();

        // 4. 대화 히스토리 로드 (최근 10개)
        var history = chatThreadService.getHistory(request.getThreadId());
        log.info("║ 대화 히스토리: {}개 메시지", history.size());

        // 5. 현재 Phase 확인 (먼저 확인)
        var currentPhase = TravelPhase.valueOf(context.getCurrentPhase());
        log.info("║ 현재 Phase: {}", currentPhase);

        // 5-1. 구체적인 여행 질문 감지 (LLM 기반)
        boolean isSpecificTravelQuery = intentClassifier.isSpecificTravelQuery(request.getMessage());
        if (isSpecificTravelQuery && currentPhase == TravelPhase.INITIALIZATION) {
            log.info("║ 🎯 구체적인 여행 질문 감지 - 바로 INFORMATION_COLLECTION으로 전환");
            context.setWaitingForTravelConfirmation(false);
            // Intent를 CONFIRMATION으로 설정하여 바로 전환되도록
            var intent = Intent.CONFIRMATION;
            context.setWaitingForTravelConfirmation(true); // 일시적으로 true 설정
            contextManager.updateContext(context, context.getUserId());
            var nextPhase = phaseManager.transitionPhase(context.getThreadId(), intent, context);
            context.setCurrentPhase(nextPhase.name());
            contextManager.updateContext(context, context.getUserId());
            var response = responseGenerator.generateResponse(request, intent, nextPhase, context, promptBuilder);
            saveSystemMessage(request.getThreadId(), response.getContent());
            return response;
        }

        // 6. Intent 분류 (맥락 정보와 함께 LLM으로 분류)
        var intent = intentClassifier.classify(
            request.getMessage(),
            context.isWaitingForTravelConfirmation()
        );
        log.info("║ 분류된 Intent: {}", intent);
        log.info("║ 여행 확인 대기 상태: {}", context.isWaitingForTravelConfirmation());

        // 7. Phase 전환 처리 (waitingForTravelConfirmation 플래그를 유지한 상태로)
        var nextPhase = handlePhaseTransition(currentPhase, intent, context);

        // 8. 여행 확인 대기 상태 처리
        if (context.isWaitingForTravelConfirmation()) {
            if (intent == Intent.CONFIRMATION) {
                // 사용자가 확인한 경우 - Phase 전환 후 플래그 리셋
                log.info("║ 여행 계획 시작 확인 응답 감지 - Phase 전환 후 플래그 리셋");
                context.setWaitingForTravelConfirmation(false);
                contextManager.updateContext(context, context.getUserId());
            } else if (intent != Intent.TRAVEL_PLANNING) {
                // 사용자가 다른 의도를 보인 경우 (거부 또는 주제 변경) - 확인 대기 상태 해제
                log.info("║ 사용자가 다른 의도를 보임 (Intent: {}) - 확인 대기 상태 해제", intent);
                context.setWaitingForTravelConfirmation(false);
                contextManager.updateContext(context, context.getUserId());
            }
            // TRAVEL_PLANNING인 경우는 계속 대기 상태 유지
        }

        // 9. 응답 생성 - ResponseGenerator에 PromptBuilder 전달
        log.info("╔══════════════════════════════════════════════════════════════");
        log.info("║ 응답 생성 시작");
        log.info("║ Intent: {}, Phase: {}", intent, nextPhase);
        log.info("╚══════════════════════════════════════════════════════════════");

        var response = responseGenerator.generateResponse(request, intent, nextPhase, context, promptBuilder);

        // 10. 시스템 응답 저장
        saveSystemMessage(request.getThreadId(), response.getContent());

        return response;
    }

    // 사용자 메시지 저장
    private void saveUserMessage(ChatRequest request) {
        try {
            var saveRequest = new ChatThreadService.MessageSaveRequest(
                request.getThreadId(),
                "user",
                request.getMessage()
            );
            chatThreadService.saveMessage(saveRequest);
            log.debug("사용자 메시지 저장 완료");
        } catch (Exception e) {
            log.error("사용자 메시지 저장 실패: {}", e.getMessage());
        }
    }

    // 시스템 메시지 저장
    private void saveSystemMessage(String threadId, String content) {
        try {
            var saveRequest = new ChatThreadService.MessageSaveRequest(
                threadId,
                "assistant",
                content
            );
            chatThreadService.saveMessage(saveRequest);
            log.debug("시스템 메시지 저장 완료");
        } catch (Exception e) {
            log.error("시스템 메시지 저장 실패: {}", e.getMessage());
        }
    }

    // Phase 전환 처리
    private TravelPhase handlePhaseTransition(TravelPhase currentPhase, Intent intent,
                                              TravelContext context) {
        // PhaseManager의 transitionPhase 메서드 사용
        var nextPhase = phaseManager.transitionPhase(context.getThreadId(), intent, context);

        if (nextPhase != currentPhase) {
            log.info("╔══════════════════════════════════════════════════════════════");
            log.info("║ 🔄 Phase 전환 감지!");
            log.info("║ 이전 Phase: {}", currentPhase);
            log.info("║ 새로운 Phase: {}", nextPhase);
            log.info("╚══════════════════════════════════════════════════════════════");
            context.setCurrentPhase(nextPhase.name());
            contextManager.updateContext(context, context.getUserId());
        }

        return nextPhase;
    }

    // 컨텍스트 초기화
    public void resetContext(String threadId, String userId) {
        contextManager.resetContext(threadId, userId);
    }
}