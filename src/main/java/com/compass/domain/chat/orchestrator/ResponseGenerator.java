package com.compass.domain.chat.orchestrator;

import com.compass.domain.chat.model.context.TravelContext;
import com.compass.domain.chat.model.enums.Intent;
import com.compass.domain.chat.model.enums.TravelPhase;
import com.compass.domain.chat.model.request.ChatRequest;
import com.compass.domain.chat.model.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

// 응답 생성 전담 컴포넌트
@Slf4j
@Component
@RequiredArgsConstructor
public class ResponseGenerator {

    private final PromptBuilder promptBuilder;

    @Autowired(required = false)
    @Qualifier("vertexAiGeminiChat")
    private ChatModel chatModel;  // Vertex AI Gemini 모델 사용

    // 메인 응답 생성 메서드
    public ChatResponse generateResponse(ChatRequest request, Intent intent,
                                        TravelPhase phase, TravelContext context) {
        log.debug("응답 생성 시작: Intent={}, Phase={}", intent, phase);

        // 콘텐츠 생성
        var content = generateContent(request, intent, phase);

        // Phase 진행 확인 프롬프트 추가
        var requiresConfirmation = shouldAskForConfirmation(phase);
        if (requiresConfirmation) {
            content += generateConfirmationPrompt(phase);
        }

        // 응답 타입 결정
        var responseType = determineResponseType(intent, phase);

        // 응답 데이터 구성
        var responseData = buildResponseData(intent, phase, context);

        // 다음 액션 결정
        var nextAction = determineNextAction(intent, phase);

        return ChatResponse.builder()
            .content(content)
            .type(responseType)
            .data(responseData)
            .nextAction(nextAction)
            .requiresConfirmation(requiresConfirmation)
            .build();
    }

    // 콘텐츠 생성 (LLM 또는 Mock)
    private String generateContent(ChatRequest request, Intent intent, TravelPhase phase) {
        // ChatModel이 설정되어 있으면 LLM 사용, 아니면 Mock 응답
        if (chatModel != null) {
            return generateLLMResponse(request, intent, phase);
        } else {
            log.debug("ChatModel 없음 - Mock 응답 반환");
            return generateMockResponse(request, intent, phase);
        }
    }

    // LLM을 통한 응답 생성
    public String generateLLMResponse(ChatRequest request, Intent intent, TravelPhase phase) {
        try {
            log.debug("LLM 응답 생성 시작 - Intent: {}, Phase: {}", intent, phase);

            // 일반 대화 + INITIALIZATION Phase인 경우 특별 처리
            if (intent == Intent.GENERAL_QUESTION && phase == TravelPhase.INITIALIZATION) {
                return generateGeneralChatWithTravelInduction(request);
            }

            // 기존 프롬프트 메시지 구성
            var messages = promptBuilder.buildPromptMessages(request, intent, phase);
            var prompt = new Prompt(messages);

            // LLM 호출
            var response = chatModel.call(prompt);
            var content = response.getResult().getOutput().getContent();

            log.debug("LLM 응답 생성 완료");
            return content;
        } catch (Exception e) {
            log.error("LLM 호출 실패: {}", e.getMessage());
            // 실패 시 Mock 응답 반환
            return generateMockResponse(request, intent, phase);
        }
    }

    // Mock 응답 생성 (개발용)
    public String generateMockResponse(ChatRequest request, Intent intent, TravelPhase phase) {
        log.debug("Mock 응답 생성: Intent={}, Phase={}", intent, phase);

        // Intent와 Phase를 고려한 전략적 응답
        if (phase == TravelPhase.INITIALIZATION) {
            return generateInitializationResponse(intent);
        }

        // 다른 Phase들의 기본 응답
        return generatePhaseResponse(phase);
    }

    // INITIALIZATION Phase 응답 생성
    private String generateInitializationResponse(Intent intent) {
        return switch (intent) {
            case GENERAL_QUESTION -> """
                안녕하세요! 오늘 기분은 어떠신가요? 😊
                요즘 날씨가 정말 좋은데, 어디론가 떠나고 싶지 않으신가요?
                제가 멋진 여행 계획을 도와드릴 수 있어요!
                """;
            case WEATHER_INQUIRY -> """
                네, 여행 관련 질문이시군요! 기꺼이 도와드리겠습니다.
                그런데 혹시 구체적인 여행 계획을 세우는 데도 관심이 있으신가요?
                완벽한 여행 일정을 함께 만들어볼 수 있어요!
                """;
            case INFORMATION_COLLECTION -> """
                좋아요! 여행 계획을 시작해볼까요? 🎉
                완벽한 여행을 위해 몇 가지 정보를 알려주세요.
                어디로 가고 싶으신지, 언제쯤 떠나실 예정인지 궁금해요!
                """;
            default -> "무엇을 도와드릴까요? 여행 계획이 있으신가요?";
        };
    }

    // Phase별 응답 생성
    private String generatePhaseResponse(TravelPhase phase) {
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
    public String determineResponseType(Intent intent, TravelPhase phase) {
        // Phase에 따른 응답 타입 결정
        if (phase == TravelPhase.PLAN_GENERATION) {
            return "ITINERARY";
        }

        // Intent에 따른 특별한 타입이 필요한 경우 여기 추가

        // 기본값
        return "TEXT";
    }

    // 응답 데이터 구성
    public Object buildResponseData(Intent intent, TravelPhase phase, TravelContext context) {
        // 필요한 경우 컨텍스트에서 추가 데이터 반환
        if (intent == Intent.INFORMATION_COLLECTION && context != null) {
            return context.getCollectedInfo();
        } else if (phase == TravelPhase.PLAN_GENERATION && context != null) {
            return context.getTravelPlan();
        }
        return null;
    }

    // 다음 액션 결정
    public String determineNextAction(Intent intent, TravelPhase phase) {
        // Phase 기반 다음 액션 결정
        return switch (phase) {
            case INFORMATION_COLLECTION -> "COLLECT_MORE_INFO";
            case FEEDBACK_REFINEMENT -> "REFINE_PLAN";
            case COMPLETION -> "SAVE_OR_EXPORT";
            default -> "CONTINUE";
        };
    }

    // Phase 진행 확인이 필요한지 판단
    private boolean shouldAskForConfirmation(TravelPhase phase) {
        // COMPLETION을 제외한 모든 Phase에서 확인 필요
        return phase != TravelPhase.COMPLETION;
    }

    // 확인 프롬프트 생성 - 자연스러운 대화 형태
    private String generateConfirmationPrompt(TravelPhase phase) {
        return switch (phase) {
            case INITIALIZATION -> "\n\n✨ 함께 멋진 여행 계획을 만들어볼까요? 시작하고 싶으시면 말씀해주세요!";
            case INFORMATION_COLLECTION -> "\n\n📝 충분한 정보가 모인 것 같네요! 이제 여행 일정을 만들어드릴까요?";
            case PLAN_GENERATION -> "\n\n🎯 어떠신가요? 이 일정으로 진행하시겠어요? 아니면 수정이 필요하신가요?";
            case FEEDBACK_REFINEMENT -> "\n\n✏️ 수정사항을 반영해드렸어요! 이대로 진행할까요?";
            case COMPLETION -> "";  // COMPLETION은 확인 불필요
        };
    }

    // 일반 대화 처리 + 여행 유도 (LLM 기반)
    private String generateGeneralChatWithTravelInduction(ChatRequest request) {
        log.debug("일반 대화 + 여행 유도 응답 생성");

        var systemPrompt = """
            당신은 친절하고 재미있는 여행 계획 도우미 '컴패스'입니다.
            사용자와 자연스럽게 대화하면서, 적절한 타이밍에 여행 계획으로 대화를 유도합니다.

            대화 전략:
            1. 먼저 사용자의 말에 공감하고 적절히 응답하세요
            2. 대화 내용과 연결하여 자연스럽게 여행을 언급하세요
            3. 부담스럽지 않게, 제안하는 톤으로 여행 계획을 권유하세요

            예시:
            - 날씨 언급 → "날씨가 좋은 곳으로 여행 떠나보시는 건 어떠세요?"
            - 스트레스/피로 → "잠시 일상을 벗어나 여행으로 리프레시하시는 건 어떨까요?"
            - 취미/관심사 → "그 취미를 즐길 수 있는 여행지를 소개해드릴까요?"
            - 일반 인사 → "요즘 여행 가고 싶은 곳이 있으신가요?"

            이모지를 적절히 사용하여 친근한 분위기를 만드세요.
            """;

        var userPrompt = String.format("""
            사용자: %s

            위 메시지에 먼저 친근하게 응답한 후,
            자연스럽게 여행 계획을 제안해주세요.
            """, request.getMessage());

        try {
            var prompt = new Prompt(List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(userPrompt)
            ));

            var response = chatModel.call(prompt);
            return response.getResult().getOutput().getContent();
        } catch (Exception e) {
            log.error("일반 대화 LLM 호출 실패: {}", e.getMessage());
            return generateDefaultGeneralChatResponse(request.getMessage());
        }
    }

    // 일반 대화 기본 응답 (LLM 실패 시 폴백)
    private String generateDefaultGeneralChatResponse(String message) {
        var lowerMessage = message.toLowerCase();

        if (lowerMessage.contains("안녕") || lowerMessage.contains("hello")) {
            return """
                안녕하세요! 반가워요 😊
                오늘 기분은 어떠신가요?
                혹시 요즘 여행 가고 싶은 곳이 있으신가요?
                제가 멋진 여행 계획을 도와드릴 수 있어요! ✈️
                """;
        }

        if (lowerMessage.contains("날씨")) {
            return """
                날씨 정보가 궁금하시군요! ☀️
                요즘 날씨가 참 좋죠? 이런 날씨에 여행 떠나기 딱 좋은데...
                혹시 날씨 좋은 여행지로 계획 세워보실래요? 🏖️
                """;
        }

        if (lowerMessage.contains("심심") || lowerMessage.contains("지루")) {
            return """
                일상이 좀 지루하신가 봐요 😔
                이럴 때 여행만큼 좋은 기분전환이 없죠!
                가까운 곳이라도 여행 계획 세워볼까요? 제가 도와드릴게요! 🗺️
                """;
        }

        // 기본 응답
        return """
            네, 무엇을 도와드릴까요? 😊
            혹시 여행 계획이 필요하시면 언제든 말씀해주세요!
            국내든 해외든 완벽한 일정을 만들어드릴 수 있어요 ✨
            """;
    }
}