package com.compass.domain.chat.orchestrator;

import com.compass.domain.chat.model.context.TravelContext;
import com.compass.domain.chat.model.enums.Intent;
import com.compass.domain.chat.model.enums.TravelPhase;
import com.compass.domain.chat.model.request.ChatRequest;
import com.compass.domain.chat.model.response.ChatResponse;
import com.compass.domain.chat.service.ChatThreadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatResponse as AiChatResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// 메인 오케스트레이터 서비스
@Slf4j
@Service
@RequiredArgsConstructor
public class MainLLMOrchestrator {

    private final IntentClassifier intentClassifier;
    private final PhaseManager phaseManager;
    private final ChatThreadService chatThreadService;
    private final VertexAiGeminiChatClient geminiChatClient;

    // 스레드별 컨텍스트 저장소
    private final Map<String, TravelContext> contextStore = new ConcurrentHashMap<>();

    // 채팅 요청 처리
    public ChatResponse processChat(ChatRequest request) {
        log.debug("채팅 요청 처리 시작: threadId={}, userId={}",
                request.getThreadId(), request.getUserId());

        // 컨텍스트 조회 또는 생성
        var context = getOrCreateContext(request);

        // Intent 분류
        var intent = intentClassifier.classify(request.getMessage());
        log.debug("분류된 Intent: {}", intent);

        // 현재 Phase 확인
        var currentPhase = TravelPhase.valueOf(context.getCurrentPhase());
        log.debug("현재 Phase: {}", currentPhase);

        // Phase 전환 처리
        var nextPhase = handlePhaseTransition(currentPhase, intent, context);

        // 응답 생성
        return generateResponse(request, intent, nextPhase, context);
    }

    // Phase 전환 처리
    private TravelPhase handlePhaseTransition(TravelPhase currentPhase, Intent intent,
                                              TravelContext context) {
        var nextPhase = phaseManager.determineNextPhase(currentPhase, intent, context);

        if (nextPhase != currentPhase) {
            log.info("Phase 전환: {} -> {}", currentPhase, nextPhase);
            context.setCurrentPhase(nextPhase.name());
            updateContext(context);
        }

        return nextPhase;
    }

    // 컨텍스트 조회 또는 생성
    private TravelContext getOrCreateContext(ChatRequest request) {
        return contextStore.computeIfAbsent(request.getThreadId(), k ->
            TravelContext.builder()
                .threadId(request.getThreadId())
                .userId(request.getUserId())
                .currentPhase(TravelPhase.INITIALIZATION.name())
                .build()
        );
    }

    // 컨텍스트 업데이트
    private void updateContext(TravelContext context) {
        contextStore.put(context.getThreadId(), context);
    }

    // 응답 생성
    private ChatResponse generateResponse(ChatRequest request, Intent intent,
                                         TravelPhase phase, TravelContext context) {
        // LLM 프롬프트 구성
        var messages = buildPromptMessages(request, intent, phase, context);
        var prompt = new Prompt(messages);

        // LLM 호출
        AiChatResponse aiResponse = geminiChatClient.call(prompt);
        var content = aiResponse.getResult().getOutput().getContent();

        // 응답 타입 결정
        var responseType = determineResponseType(intent, phase);

        return ChatResponse.builder()
            .content(content)
            .type(responseType)
            .data(buildResponseData(intent, phase, context))
            .nextAction(determineNextAction(intent, phase))
            .build();
    }

    // 프롬프트 메시지 구성
    private List<Message> buildPromptMessages(ChatRequest request, Intent intent,
                                             TravelPhase phase, TravelContext context) {
        // List.of 사용으로 불변 리스트 생성
        return List.of(
            new SystemMessage(buildSystemPrompt(intent, phase)),
            new UserMessage(request.getMessage())
        );
    }

    // 시스템 프롬프트 구성
    private String buildSystemPrompt(Intent intent, TravelPhase phase) {
        return String.format("""
            당신은 여행 계획을 도와주는 AI 어시스턴트입니다.
            현재 Intent: %s
            현재 Phase: %s

            사용자의 요청에 맞춰 적절한 응답을 제공해주세요.
            """, intent, phase);
    }

    // 응답 타입 결정
    private String determineResponseType(Intent intent, TravelPhase phase) {
        // Switch Expression 활용
        return switch (intent) {
            case QUICK_INPUT -> "FORM";
            case DESTINATION_SEARCH, RESERVATION_PROCESSING -> "CARD";
            default -> phase == TravelPhase.PLAN_GENERATION ? "ITINERARY" : "TEXT";
        };
    }

    // 응답 데이터 구성
    private Object buildResponseData(Intent intent, TravelPhase phase, TravelContext context) {
        // 필요한 경우 추가 데이터 반환
        if (intent == Intent.INFORMATION_COLLECTION) {
            return context.getCollectedInfo();
        } else if (phase == TravelPhase.PLAN_GENERATION) {
            return context.getTravelPlan();
        }
        return null;
    }

    // 다음 액션 결정
    private String determineNextAction(Intent intent, TravelPhase phase) {
        // Switch Expression 활용
        return switch (phase) {
            case INFORMATION_COLLECTION -> "COLLECT_MORE_INFO";
            case FEEDBACK_REFINEMENT -> "REFINE_PLAN";
            case COMPLETION -> "SAVE_OR_EXPORT";
            default -> "CONTINUE";
        };
    }

    // 컨텍스트 초기화
    public void resetContext(String threadId) {
        log.info("컨텍스트 초기화: threadId={}", threadId);
        contextStore.remove(threadId);
    }
}