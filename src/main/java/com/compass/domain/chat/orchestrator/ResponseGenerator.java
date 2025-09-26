package com.compass.domain.chat.orchestrator;

import com.compass.domain.chat.function.collection.ShowQuickInputFormFunction;
import com.compass.domain.chat.model.context.TravelContext;
import com.compass.domain.chat.model.request.ChatRequest;
import com.compass.domain.chat.model.response.ChatResponse;
import com.compass.domain.chat.model.enums.Intent;
import com.compass.domain.chat.model.enums.TravelPhase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

// 응답 생성기 - Intent와 Phase에 따른 적절한 응답 생성
@Slf4j
@Component
@RequiredArgsConstructor
public class ResponseGenerator {

    private final ShowQuickInputFormFunction showQuickInputFormFunction;
    private final com.compass.domain.chat.service.TravelPlanGenerationService travelPlanGenerationService;

    @Autowired(required = false)
    private ChatModel chatModel;  // OpenAI 모델 사용 (선택적)

    // 통합 응답 생성 (PromptBuilder 추가)
    public ChatResponse generateResponse(ChatRequest request, Intent intent, TravelPhase phase,
                                        TravelContext context, PromptBuilder promptBuilder) {
        MDC.put("intent", intent.name());
        MDC.put("phase", phase.name());

        try {
            log.info("응답 생성 시작");
            log.debug("Message: {}", request.getMessage());

        // 응답 타입 먼저 결정
        var responseType = determineResponseType(intent, phase, context);
        log.debug("ResponseType 결정: {}", responseType);

        // QUICK_FORM인 경우 간단한 폼 안내 메시지만 생성 (LLM 호출 안 함)
        String content;
        boolean requiresConfirmation = shouldAskForConfirmation(phase);

        if ("QUICK_FORM".equals(responseType)) {
            log.debug("QUICK_FORM 타입 - 폼 안내 메시지 반환");
            // QUICK_FORM인 경우 폼 작성 안내 메시지만
            content = "좋습니다! 아래 폼에 여행 정보를 입력해주세요. 빠르고 간편하게 맞춤형 여행 계획을 만들어드릴게요! 🎯";
        } else {
            log.debug("일반 응답 생성 - ResponseType: {}", responseType);
            // QUICK_FORM이 아닌 경우에만 LLM 응답 생성
            content = generateContent(request, intent, phase, context, promptBuilder);

            // 확인 프롬프트 추가
            if (requiresConfirmation && phase == TravelPhase.INITIALIZATION) {
                // INITIALIZATION 단계에서는 사용자 의사 확인 필요
                content += generateConfirmationPrompt(phase);
            }
        }

        // 응답 데이터 구성
        var responseData = buildResponseData(intent, phase, context);
        log.debug("ResponseData 존재: {}", responseData != null);

        // 다음 액션 결정
        var nextAction = determineNextAction(intent, phase);

        var response = ChatResponse.builder()
            .content(content)
            .type(responseType)
            .phase(phase.name())
            .data(responseData)
            .nextAction(nextAction)
            .requiresConfirmation(requiresConfirmation)
            .build();

        log.info("응답 생성 완료 - Type: {}", response.getType());

        return response;
        } finally {
            MDC.remove("intent");
            MDC.remove("phase");
        }
    }

    // 오버로드 메소드 (이전 버전 호환성)
    public ChatResponse generateResponse(ChatRequest request, Intent intent, TravelPhase phase, TravelContext context) {
        return generateResponse(request, intent, phase, context, null);
    }

    // 콘텐츠 생성 (PromptBuilder 활용)
    private String generateContent(ChatRequest request, Intent intent, TravelPhase phase,
                                  TravelContext context, PromptBuilder promptBuilder) {
        // QUICK_FORM 타입인 경우 간단한 안내 메시지만 반환
        String responseType = determineResponseType(intent, phase, context);
        log.debug("Content 생성 - ResponseType: {}", responseType);
        if ("QUICK_FORM".equals(responseType)) {
            log.debug("QUICK_FORM 타입 - 폼 안내 메시지 반환");
            return "좋습니다! 아래 폼에 여행 정보를 입력해주세요. 빠르고 간편하게 맞춤형 여행 계획을 만들어드릴게요! 🎯";
        }

        // INITIALIZATION 단계에서 여행 계획 확인 대기중인 경우
        // GENERAL_QUESTION이나 다른 Intent가 왔다는 것은 사용자가 확인을 거부하거나 다른 주제로 넘어간 것
        if (phase == TravelPhase.INITIALIZATION &&
            context != null && context.isWaitingForTravelConfirmation() &&
            intent != Intent.GENERAL_QUESTION) {
            return generateIntentResponse(intent, phase);
        }

        // ChatModel이 설정되어 있으면 LLM 사용, 아니면 Mock 응답
        if (chatModel != null) {
            return generateLLMResponse(request, intent, phase, context, promptBuilder);
        } else {
            log.debug("ChatModel 없음 - Mock 응답 반환");
            return generateMockResponse(request, intent, phase);
        }
    }

    // 오버로드 메소드 (이전 버전 호환성)
    private String generateContent(ChatRequest request, Intent intent, TravelPhase phase, TravelContext context) {
        return generateContent(request, intent, phase, context, null);
    }

    // LLM을 통한 응답 생성 (PromptBuilder 활용)
    public String generateLLMResponse(ChatRequest request, Intent intent, TravelPhase phase,
                                     TravelContext context, PromptBuilder promptBuilder) {
        try {
            // PromptBuilder를 사용한 정교한 프롬프트 생성
            String prompt;
            if (promptBuilder != null) {
                // PromptBuilder가 있으면 활용
                var systemPrompt = promptBuilder.buildSystemPrompt(intent, phase, context);
                var userPrompt = promptBuilder.buildUserPrompt(request.getMessage(), context);
                prompt = systemPrompt + "\n\n" + userPrompt;
            } else {
                // PromptBuilder가 없으면 기존 방식
                prompt = buildPrompt(request, intent, phase, context);
            }

            // LLM 응답 요청
            log.debug("LLM 응답 요청 - Intent: {}, Phase: {}", intent, phase);
            String llmResponse = getBasicLLMResponse(prompt);
            return llmResponse;
        } catch (Exception e) {
            log.error("LLM 응답 생성 실패", e);
            return generateMockResponse(request, intent, phase);
        }
    }

    // 오버로드 메소드 (이전 버전 호환성)
    public String generateLLMResponse(ChatRequest request, Intent intent, TravelPhase phase, TravelContext context) {
        return generateLLMResponse(request, intent, phase, context, null);
    }

    // LLM 프롬프트 구성 (PromptBuilder가 없을 때의 기본 프롬프트)
    private String buildPrompt(ChatRequest request, Intent intent, TravelPhase phase, TravelContext context) {
        return String.format("""
            당신은 친근하고 도움이 되는 여행 계획 도우미입니다.

            현재 대화 상태:
            - Intent: %s
            - Phase: %s
            - 사용자 메시지: %s
            - 여행 확인 대기 상태: %s

            역할:
            1. 사용자의 의도를 파악하여 적절한 응답 제공
            2. Phase에 맞는 안내 제공
            3. 자연스러운 대화체 사용

            중요:
            - INITIALIZATION 단계에서 TRAVEL_PLANNING Intent가 감지되면 "여행 계획을 세워드릴까요?"와 같은 확인 질문을 해야 합니다.
            - 사용자가 여행 의도를 보였지만 아직 확정하지 않았다면, 부드럽게 확인을 요청하세요.

            응답 가이드라인:
            - 간결하고 친근한 톤 사용
            - 여행 계획에 도움이 되는 정보 제공
            - 다음 단계로의 자연스러운 유도

            응답:
            """, intent, phase, request.getMessage(),
            context != null ? context.isWaitingForTravelConfirmation() : false);
    }

    // 기본 LLM 호출
    private String getBasicLLMResponse(String prompt) {
        try {
            if (chatModel == null) {
                log.warn("ChatModel이 설정되지 않음");
                return "죄송합니다, 지금은 응답할 수 없습니다.";
            }

            Prompt springPrompt = new Prompt(prompt);
            String response = chatModel.call(springPrompt).getResult().getOutput().getContent();
            log.debug("LLM 응답: {}", response);
            return response;
        } catch (Exception e) {
            log.error("LLM 호출 실패", e);
            return "죄송합니다, 일시적인 오류가 발생했습니다.";
        }
    }

    // Mock 응답 생성 (ChatModel 없을 때)
    private String generateMockResponse(ChatRequest request, Intent intent, TravelPhase phase) {
        log.debug("Mock 응답 생성 - Intent: {}, Phase: {}", intent, phase);
        // Intent와 Phase 조합으로 적절한 Mock 응답 반환
        return generateIntentResponse(intent, phase);
    }

    // Intent별 응답 생성 (Intent 중심)
    private String generateIntentResponse(Intent intent, TravelPhase phase) {
        return switch (intent) {
            case TRAVEL_PLANNING -> """
                멋진 여행을 계획하시는군요! 🌍
                제가 완벽한 여행 계획을 세워드릴 수 있어요.

                여행 계획을 시작해볼까요? 목적지, 날짜, 예산 등을 편하게 입력하실 수 있는 폼을 준비해드릴게요!
                """;
            case CONFIRMATION -> "좋아요! 바로 시작해보겠습니다. 🎉";
            case INFORMATION_COLLECTION -> "여행 정보를 입력해주세요. 목적지, 날짜, 예산을 알려주세요.";
            case DESTINATION_SEARCH -> "원하시는 목적지를 찾아드리겠습니다.";
            case PLAN_MODIFICATION -> "여행 계획을 수정해드리겠습니다.";
            case FEEDBACK -> "피드백 감사합니다. 계획을 개선해드리겠습니다.";
            case COMPLETION -> "여행 계획이 완성되었습니다! 즐거운 여행 되세요!";
            case GENERAL_QUESTION -> "무엇이 궁금하신가요? 도와드리겠습니다.";
            case WEATHER_INQUIRY -> switch (phase) {
                case INITIALIZATION -> "날씨 정보를 확인해드리겠습니다.";
                default -> "해당 지역의 날씨를 조회하고 있습니다.";
            };
            case IMAGE_UPLOAD -> "이미지를 확인했습니다. 내용을 분석하고 있습니다.";
            default -> "무엇을 도와드릴까요?";
        };
    }

    // 응답 타입 결정
    public String determineResponseType(Intent intent, TravelPhase phase, TravelContext context) {
        log.debug("응답 타입 결정 - Context: {}, CollectedInfo: {}",
            context != null, context != null && context.getCollectedInfo() != null);

        // Phase에 따른 응답 타입 결정
        if (phase == TravelPhase.PLAN_GENERATION) {
            log.debug("PLAN_GENERATION 단계 - ITINERARY 반환");
            return "ITINERARY";
        }

        // INFORMATION_COLLECTION 단계에서는 Intent와 컨텍스트를 확인하여 결정
        if (phase == TravelPhase.INFORMATION_COLLECTION) {
            log.debug("INFORMATION_COLLECTION 단계 진입");

            // 이미 폼 데이터가 저장되어 있고 충분한 정보가 있으면 TEXT로 다음 액션 유도
            if (context != null && context.getCollectedInfo() != null) {
                Map<String, Object> info = (Map<String, Object>) context.getCollectedInfo();
                log.debug("CollectedInfo 크기: {}", info.size());

                // CollectedInfo가 빈 Map인 경우에도 QUICK_FORM 표시 필요
                if (info.isEmpty()) {
                    log.debug("CollectedInfo 비어있음 - Intent: {}", intent);
                    // Intent가 TRAVEL_PLANNING 또는 CONFIRMATION일 때만 QUICK_FORM 표시
                    if (intent == Intent.TRAVEL_PLANNING || intent == Intent.CONFIRMATION) {
                        log.debug("QUICK_FORM 반환 - 빈 CollectedInfo + TRAVEL_PLANNING Intent");
                        return "QUICK_FORM";
                    }
                }

                // 필수 정보가 모두 있으면 여행 계획 생성 준비
                if (hasRequiredTravelInfo(info)) {
                    log.debug("필수 정보 모두 수집됨 - TEXT 반환");
                    return "TEXT";  // 계획 생성 안내 메시지
                }
                // 정보가 부족하면 추가 수집 필요
                log.debug("정보 부족 - TEXT 반환");
                return "TEXT";  // 추가 질문 메시지
            }

            log.debug("CollectedInfo null - Intent: {}", intent);

            // Intent가 TRAVEL_PLANNING 또는 CONFIRMATION일 때만 QUICK_FORM 표시
            // 일반 대화(GENERAL_QUESTION)에서는 폼을 표시하지 않음
            if (intent == Intent.TRAVEL_PLANNING || intent == Intent.CONFIRMATION) {
                log.debug("QUICK_FORM 반환 - TRAVEL_PLANNING/CONFIRMATION Intent");
                return "QUICK_FORM";
            }
            // DESTINATION_SEARCH나 구체적인 여행 관련 Intent도 폼 표시
            if (intent == Intent.DESTINATION_SEARCH || intent == Intent.INFORMATION_COLLECTION) {
                log.debug("QUICK_FORM 반환 - 여행 관련 Intent");
                return "QUICK_FORM";
            }
            // 그 외의 경우는 TEXT 응답
            log.debug("TEXT 반환 - Intent: {}", intent);
            return "TEXT";
        }

        // INITIALIZATION 단계에서 여행 확인 대기중이면 TEXT로 확인 질문
        if (phase == TravelPhase.INITIALIZATION &&
            context != null && context.isWaitingForTravelConfirmation()) {
            return "TEXT";
        }

        // Intent에 따른 특별한 타입이 필요한 경우 여기 추가

        // 기본값
        return "TEXT";
    }

    // 필수 여행 정보 확인 (헬퍼 메서드)
    private boolean hasRequiredTravelInfo(Map<String, Object> info) {
        if (info == null) return false;

        boolean hasDestination = info.containsKey("destination") &&
                                info.get("destination") != null &&
                                !info.get("destination").toString().isEmpty();
        boolean hasDates = (info.containsKey("startDate") && info.get("startDate") != null) &&
                          (info.containsKey("endDate") && info.get("endDate") != null);
        boolean hasBudget = info.containsKey("budget") && info.get("budget") != null;

        return hasDestination && hasDates && hasBudget;
    }

    // 응답 데이터 구성
    public Object buildResponseData(Intent intent, TravelPhase phase, TravelContext context) {
        log.info("🔍 buildResponseData 호출 - Intent: {}, Phase: {}, Context null?: {}",
            intent, phase, context == null);

        // INFORMATION_COLLECTION 단계에서는 컨텍스트 확인 후 처리
        if (phase == TravelPhase.INFORMATION_COLLECTION) {
            // 이미 정보가 수집되었는지 확인
            if (context != null && context.getCollectedInfo() != null) {
                Map<String, Object> info = (Map<String, Object>) context.getCollectedInfo();
                // 필수 정보가 있으면 다음 단계 준비
                if (hasRequiredTravelInfo(info)) {
                    // 여행 계획 생성 function 호출을 위해 수집된 정보 반환
                    return context.getCollectedInfo();
                }
                // 정보가 부족하면 추가 질문 또는 폼 다시 표시
                log.debug("정보 부족 - 추가 수집 필요");
                return context.getCollectedInfo(); // 현재까지 수집된 정보 반환
            } else {
                // 처음 INFORMATION_COLLECTION에 들어왔을 때 폼 표시
                log.debug("INFORMATION_COLLECTION 단계 - 빠른 입력 폼 생성");
                var request = new ShowQuickInputFormFunction.Request();
                return showQuickInputFormFunction.apply(request);
            }
        }

        // 필요한 경우 컨텍스트에서 추가 데이터 반환
        if (intent == Intent.INFORMATION_COLLECTION && context != null) {
            return context.getCollectedInfo();
        } else if (phase == TravelPhase.PLAN_GENERATION && context != null) {
            log.info("🎯 PLAN_GENERATION 단계 진입!");
            // 여행 계획이 없으면 생성
            if (context.getTravelPlan() == null ||
                (context.getTravelPlan() instanceof Map && ((Map<?,?>)context.getTravelPlan()).isEmpty())) {
                log.info("📍 여행 계획이 없어서 새로 생성합니다.");
                try {
                    Map<String, Object> travelPlan = travelPlanGenerationService.generateTravelPlan(context);
                    log.info("✅ 여행 계획 생성 완료. 계획 null?: {}", travelPlan == null);
                    if (travelPlan != null) {
                        log.info("📋 생성된 계획 키들: {}", travelPlan.keySet());
                    }
                    context.setTravelPlan(travelPlan);
                    return travelPlan;
                } catch (Exception e) {
                    log.error("❌ 여행 계획 생성 중 오류 발생", e);
                    return null;
                }
            }
            log.info("📦 이미 존재하는 여행 계획 반환");
            return context.getTravelPlan();
        }

        log.info("⚠️ buildResponseData - 해당하는 조건 없음, null 반환");
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
        // INITIALIZATION 단계에서는 사용자 확인 필요
        // COMPLETION을 제외한 다른 Phase에서는 상황에 따라
        return phase == TravelPhase.INITIALIZATION;
    }

    // 확인 프롬프트 생성 - 자연스러운 대화 형태
    private String generateConfirmationPrompt(TravelPhase phase) {
        return switch (phase) {
            case INITIALIZATION -> "\n\n✨ 함께 멋진 여행 계획을 만들어볼까요? 시작하고 싶으시면 말씀해주세요!" +
                "\n\n💡 Tip: \"여행 계획을 생성해줘!\"라고 말씀하시면 바로 빠른 입력폼을 제시해드릴게요!";
            case INFORMATION_COLLECTION -> "";  // 빠른 입력 폼과 함께 제공되므로 별도 프롬프트 불필요
            case PLAN_GENERATION -> "\n\n🎯 어떠신가요? 이 일정으로 진행하시겠어요? 아니면 수정이 필요하신가요?";
            case FEEDBACK_REFINEMENT -> "\n\n✏️ 수정사항을 반영해드렸어요! 이대로 진행할까요?";
            case COMPLETION -> "";  // COMPLETION은 확인 불필요
        };
    }

    // QUICK_FORM 메시지 생성
    private String generateQuickFormMessage(String userMessage) {
        if (chatModel == null) {
            // ChatModel이 없으면 기본 메시지
            return "좋습니다! 빠른 입력폼에 정보를 입력해주시면, 맞춤형 여행 계획을 세워드릴게요! 🎯";
        }

        try {
            var systemPrompt = """
                사용자가 여행 질문을 했습니다. 빠른 입력폼을 제시하는 짧고 친근한 응답을 생성하세요.

                응답 형식:
                {
                    "message": "자연스러운 한국어 응답 (1-2문장)"
                }

                예시:
                - 사용자: "당일치기로 갈만한곳 있을까?" → "좋은 질문이네요! 빠른 입력폼에 정보를 입력해주시면, 딱 맞는 당일치기 여행지를 추천해드릴게요! 🎯"
                - 사용자: "여행 계획 짜줘" → "좋습니다! 여행 정보를 입력해주시면 완벽한 일정을 만들어드릴게요!"

                JSON 형식으로만 응답하세요.
                """;

            var userPrompt = "사용자 메시지: " + userMessage;

            var prompt = new Prompt(List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(userPrompt)
            ));

            var response = chatModel.call(prompt);
            var result = response.getResult().getOutput().getContent();

            // JSON에서 message 추출
            var messageStart = result.indexOf("\"message\"");
            if (messageStart != -1) {
                var start = result.indexOf("\"", messageStart + 10) + 1;
                var end = result.indexOf("\"", start);
                if (start > 0 && end > start) {
                    return result.substring(start, end);
                }
            }

            return "좋습니다! 빠른 입력폼에 정보를 입력해주시면, 맞춤형 여행 계획을 세워드릴게요! 🎯";

        } catch (Exception e) {
            log.error("QUICK_FORM 메시지 생성 실패: {}", e.getMessage());
            return "좋습니다! 빠른 입력폼에 정보를 입력해주시면, 맞춤형 여행 계획을 세워드릴게요! 🎯";
        }
    }
}