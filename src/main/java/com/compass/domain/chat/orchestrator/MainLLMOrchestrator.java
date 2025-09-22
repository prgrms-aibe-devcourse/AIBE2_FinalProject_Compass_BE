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
    private final com.compass.domain.chat.collection.service.FormDataConverter formDataConverter;


    // 채팅 요청 처리
    public ChatResponse processChat(ChatRequest request) {
        log.info("╔══════════════════════════════════════════════════════════════");
        log.info("║ 채팅 요청 처리 시작");
        log.info("║ Thread ID: {}", request.getThreadId());
        log.info("║ User ID: {}", request.getUserId());
        log.info("║ Message: {}", request.getMessage());
        log.info("╚══════════════════════════════════════════════════════════════");

        // 0. 빠른입력폼 데이터 처리 체크
        if (request.getMetadata() != null && request.getMetadata() instanceof java.util.Map) {
            @SuppressWarnings("unchecked")
            var metadata = (java.util.Map<String, Object>) request.getMetadata();
            var type = metadata.get("type");

            if ("TRAVEL_FORM_SUBMIT".equals(type) && metadata.get("formData") != null) {
                log.info("╔══════════════════════════════════════════════════════════════");
                log.info("║ 🎯 빠른입력폼 제출 감지 - 여행 정보 수집 완료");
                log.info("║ FormData: {}", metadata.get("formData"));
                log.info("╚══════════════════════════════════════════════════════════════");

                try {
                    // 컨텍스트 조회 또는 생성
                    var context = contextManager.getOrCreateContext(request);

                    // 폼 데이터를 Map으로 가져오기
                    @SuppressWarnings("unchecked")
                    var formDataMap = (java.util.Map<String, Object>) metadata.get("formData");

                    // FormDataConverter를 사용하여 폼 데이터 변환
                    var travelFormRequest = formDataConverter.convertFromFrontend(
                        request.getUserId(), formDataMap);

                    // updateFromFormSubmit 메서드를 사용하여 한 번에 모든 정보 업데이트
                    context.updateFromFormSubmit(travelFormRequest);

                    context.setWaitingForTravelConfirmation(false);

                    // Phase를 PLAN_GENERATION으로 전환
                    context.setCurrentPhase(TravelPhase.PLAN_GENERATION.name());
                    contextManager.updateContext(context, context.getUserId());
                    phaseManager.savePhase(request.getThreadId(), TravelPhase.PLAN_GENERATION);

                    // 메시지 저장
                    ensureChatThreadExists(request);
                    saveUserMessage(request);

                    // 수집된 정보 요약 - TravelFormSubmitRequest에서 가져오기
                    StringBuilder summary = new StringBuilder("수집된 여행 정보:\n");
                    summary.append("- 목적지: ").append(travelFormRequest.destinations()).append("\n");
                    summary.append("- 출발지: ").append(travelFormRequest.departureLocation()).append("\n");
                    if (travelFormRequest.travelDates() != null) {
                        summary.append("- 여행 기간: ").append(travelFormRequest.travelDates().startDate())
                               .append(" ~ ").append(travelFormRequest.travelDates().endDate()).append("\n");
                    }
                    summary.append("- 예산: ").append(travelFormRequest.budget()).append("\n");
                    summary.append("- 여행 스타일: ").append(travelFormRequest.travelStyle()).append("\n");
                    summary.append("- 동반자: ").append(travelFormRequest.companions()).append("\n");

                    log.info("║ {}", summary.toString().replace("\n", "\n║ "));

                    // 실제 계획 생성을 위해 ResponseGenerator 호출
                    // 폼 제출 확인 메시지 먼저 저장
                    String confirmMessage = "여행 정보를 모두 입력해주셔서 감사합니다! 🎉\n\n" +
                            summary.toString() + "\n" +
                            "입력하신 정보를 바탕으로 맞춤형 여행 계획을 생성하고 있습니다...\n" +
                            "잠시만 기다려주세요! ⏳";
                    saveSystemMessage(request.getThreadId(), confirmMessage);

                    // 실제 계획 생성 (ResponseGenerator를 통해 LLM 호출)
                    var planResponse = responseGenerator.generateResponse(
                        request,
                        Intent.CONFIRMATION,  // 계획 생성을 위한 Intent
                        TravelPhase.PLAN_GENERATION,
                        context,
                        promptBuilder
                    );

                    // 계획 생성 응답 저장
                    saveSystemMessage(request.getThreadId(), planResponse.getContent());

                    return planResponse;

                } catch (Exception e) {
                    log.error("폼 데이터 처리 중 오류 발생: {}", e.getMessage(), e);
                    return ChatResponse.builder()
                        .content("폼 데이터 처리 중 오류가 발생했습니다. 다시 시도해주세요.")
                        .type("ERROR")
                        .phase(TravelPhase.INFORMATION_COLLECTION.name())
                        .requiresConfirmation(false)
                        .build();
                }
            }
        }

        // 1. ChatThread 생성 또는 확인 (가장 먼저!)
        ensureChatThreadExists(request);

        // 2. 사용자 메시지 저장
        saveUserMessage(request);

        // 3. 컨텍스트 조회 또는 생성
        var context = contextManager.getOrCreateContext(request);

        // 3. 대화 횟수 증가
        context.incrementConversation();

        // 4. 대화 히스토리 로드 (최근 10개)
        var history = chatThreadService.getHistory(request.getThreadId());
        log.info("║ 대화 히스토리: {}개 메시지", history.size());

        // 5. 현재 Phase 확인 (먼저 확인)
        var currentPhase = TravelPhase.valueOf(context.getCurrentPhase());
        log.info("║ 현재 Phase: {}", currentPhase);

        // 5-1. 구체적인 여행 질문 감지 (LLM 기반) - 일시적으로 비활성화
        // 일반 인사를 여행 질문으로 잘못 판단하는 문제 때문에 비활성화
        // TODO: IntentClassifier의 정확도 개선 후 재활성화
        /*
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
        */

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

    // ChatThread 존재 확인 및 생성
    private void ensureChatThreadExists(ChatRequest request) {
        try {
            // ChatThreadService에서 Thread 존재 여부 확인하고 없으면 생성
            chatThreadService.ensureThreadExists(request.getThreadId(), request.getUserId());
            log.debug("ChatThread 확인/생성 완료: threadId={}", request.getThreadId());
        } catch (Exception e) {
            log.error("ChatThread 생성 실패: {}", e.getMessage());
            throw new RuntimeException("대화 스레드 생성에 실패했습니다.", e);
        }
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