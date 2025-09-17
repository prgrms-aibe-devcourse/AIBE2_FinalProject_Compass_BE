package com.compass.domain.chat.orchestrator;

import com.compass.domain.chat.model.context.TravelContext;
import com.compass.domain.chat.model.enums.Intent;
import com.compass.domain.chat.model.enums.TravelPhase;
import com.compass.domain.chat.model.request.ChatRequest;
import com.compass.domain.chat.model.response.ChatResponse;
import com.compass.domain.chat.service.ChatThreadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

// 메인 오케스트레이터 서비스
@Slf4j
@Service
@RequiredArgsConstructor
public class MainLLMOrchestrator {

    private final IntentClassifier intentClassifier;
    private final PhaseManager phaseManager;
    private final ChatThreadService chatThreadService;
    private final ContextManager contextManager;
    private final PromptBuilder promptBuilder;

    @Autowired(required = false)
    private ChatModel chatModel;  // Spring AI ChatModel 인터페이스


    // 채팅 요청 처리
    public ChatResponse processChat(ChatRequest request) {
        log.debug("채팅 요청 처리 시작: threadId={}, userId={}",
                request.getThreadId(), request.getUserId());

        // 컨텍스트 조회 또는 생성
        var context = contextManager.getOrCreateContext(request);

        // 대화 횟수 증가
        context.incrementConversation();

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
            contextManager.updateContext(context);
        }

        return nextPhase;
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
            // 프롬프트 메시지 구성 - PromptBuilder 사용
            var messages = promptBuilder.buildPromptMessages(request, intent, phase);
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

    // 임시 응답 생성 메서드 - 여행 계획으로 유도하는 응답
    private String generateMockResponse(ChatRequest request, Intent intent, TravelPhase phase) {
        // Intent와 Phase를 고려한 전략적 응답
        if (phase == TravelPhase.INITIALIZATION) {
            return switch (intent) {
                case GENERAL_CHAT -> """
                    안녕하세요! 오늘 기분은 어떠신가요? 😊
                    요즘 날씨가 정말 좋은데, 어디론가 떠나고 싶지 않으신가요?
                    제가 멋진 여행 계획을 도와드릴 수 있어요!
                    """;
                case TRAVEL_QUESTION -> """
                    네, 여행 관련 질문이시군요! 기꺼이 도와드리겠습니다.
                    그런데 혹시 구체적인 여행 계획을 세우는 데도 관심이 있으신가요?
                    완벽한 여행 일정을 함께 만들어볼 수 있어요!
                    """;
                case TRAVEL_INFO_COLLECTION -> """
                    좋아요! 여행 계획을 시작해볼까요? 🎉
                    완벽한 여행을 위해 몇 가지 정보를 알려주세요.
                    어디로 가고 싶으신지, 언제쯤 떠나실 예정인지 궁금해요!
                    """;
                default -> "무엇을 도와드릴까요? 여행 계획이 있으신가요?";
            };
        }

        // 다른 Phase들의 기본 응답
        return switch (phase) {
            case INITIALIZATION -> "이미 처리됨";
            case INFORMATION_COLLECTION -> """
                여행 정보를 수집 중이에요! 🗺️
                목적지, 날짜, 예산, 동행자 정보를 알려주시면
                맞춤형 여행 일정을 만들어드릴게요.
                """;
            case PLAN_GENERATION -> "여행 계획을 생성 중입니다... ✈️";
            case FEEDBACK_REFINEMENT -> "피드백을 반영하여 계획을 수정하고 있습니다. 🔧";
            case COMPLETION -> "완벽한 여행 계획이 완성되었습니다! 🎊";
        };
    }

    // 응답 타입 결정
    private String determineResponseType(Intent intent, TravelPhase phase) {
        // 단순화된 Intent로 기본 TEXT 타입만 사용
        if (phase == TravelPhase.PLAN_GENERATION) {
            return "ITINERARY";
        }
        return "TEXT";
    }

    // 응답 데이터 구성
    private Object buildResponseData(Intent intent, TravelPhase phase, TravelContext context) {
        // 필요한 경우 추가 데이터 반환
        if (intent == Intent.TRAVEL_INFO_COLLECTION) {
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
        contextManager.resetContext(threadId);
    }
}