package com.compass.domain.chat.orchestrator;

import com.compass.domain.chat.model.context.TravelContext;
import com.compass.domain.chat.model.enums.Intent;
import com.compass.domain.chat.model.enums.TravelPhase;
import com.compass.domain.chat.model.request.ChatRequest;
import com.compass.domain.chat.model.response.ChatResponse;
import com.compass.domain.chat.service.ChatThreadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired(required = false)
    private ChatModel chatModel;  // Spring AI ChatModel 인터페이스

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
        String content;

        // ChatModel이 설정되어 있으면 LLM 사용, 아니면 Mock 응답
        if (chatModel != null) {
            content = generateLLMResponse(request, intent, phase);
        } else {
            content = generateMockResponse(request, intent, phase);
        }

        // 응답 타입 결정
        var responseType = determineResponseType(intent, phase);

        return ChatResponse.builder()
            .content(content)
            .type(responseType)
            .data(buildResponseData(intent, phase, context))
            .nextAction(determineNextAction(intent, phase))
            .build();
    }

    // LLM 응답 생성
    private String generateLLMResponse(ChatRequest request, Intent intent, TravelPhase phase) {
        try {
            // 프롬프트 메시지 구성
            var messages = buildPromptMessages(request, intent, phase);
            var prompt = new Prompt(messages);

            // LLM 호출
            var response = chatModel.call(prompt);
            return response.getResult().getOutput().getContent();
        } catch (Exception e) {
            log.error("LLM 호출 실패: {}", e.getMessage());
            // 실패 시 Mock 응답 반환
            return generateMockResponse(request, intent, phase);
        }
    }

    // 프롬프트 메시지 구성
    private List<Message> buildPromptMessages(ChatRequest request, Intent intent, TravelPhase phase) {
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

    // 임시 응답 생성 메서드
    private String generateMockResponse(ChatRequest request, Intent intent, TravelPhase phase) {
        // 개발 초기 단계에서 사용할 임시 응답
        return switch (phase) {
            case INITIALIZATION -> "안녕하세요! 여행 계획을 도와드리겠습니다. 어디로 여행을 가고 싶으신가요?";
            case INFORMATION_COLLECTION -> "여행 정보를 수집하고 있습니다. 추가 정보가 필요합니다.";
            case PLAN_GENERATION -> "여행 계획을 생성 중입니다...";
            case FEEDBACK_REFINEMENT -> "피드백을 반영하여 계획을 수정하고 있습니다.";
            case COMPLETION -> "여행 계획이 완성되었습니다!";
        };
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