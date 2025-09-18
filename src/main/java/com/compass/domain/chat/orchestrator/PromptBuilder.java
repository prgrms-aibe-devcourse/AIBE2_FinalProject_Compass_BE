package com.compass.domain.chat.orchestrator;

import com.compass.domain.chat.model.context.TravelContext;
import com.compass.domain.chat.model.enums.Intent;
import com.compass.domain.chat.model.enums.TravelPhase;
import com.compass.domain.chat.model.request.ChatRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

// 프롬프트 생성기
@Slf4j
@Component
@RequiredArgsConstructor
public class PromptBuilder {

    // 기본 시스템 프롬프트 생성
    public String buildSystemPrompt(Intent intent, TravelPhase phase, TravelContext context) {
        var basePrompt = buildBasePrompt();
        var intentPrompt = buildIntentSpecificPrompt(intent);
        var phasePrompt = buildPhaseSpecificPrompt(phase);
        var contextPrompt = buildContextPrompt(context);

        return String.format("""
            %s

            현재 Intent: %s
            현재 Phase: %s

            %s

            %s

            %s
            """, basePrompt, intent, phase, intentPrompt, phasePrompt, contextPrompt);
    }

    // 기본 프롬프트
    private String buildBasePrompt() {
        return """
            당신은 전문 여행 계획 도우미입니다.
            사용자의 여행 계획을 돕는 것이 주요 목표입니다.

            기본 원칙:
            1. 친절하고 전문적인 톤 유지
            2. 구체적이고 실용적인 정보 제공
            3. 사용자 선호도와 예산 고려
            4. 단계적으로 정보 수집
            5. 명확한 가이드 제공

            대화 전략:
            - 자연스럽게 여행 계획으로 대화 유도
            - 필수 정보를 체계적으로 수집
            - 사용자 피드백 적극 반영
            - 구체적인 일정과 예산 제시

            현재 활성화된 Functions:
            - travel_info_form: 여행 정보 입력 폼
            - flight_search: 항공권 검색
            - hotel_search: 호텔 검색
            - itinerary_generator: 여행 일정 생성
            - tour_guide: 관광지 정보 제공
            - restaurant_finder: 맛집 추천
            - activity_suggester: 액티비티 추천
            - budget_calculator: 예산 계산
            - weather_checker: 날씨 정보
            """;
    }

    // Intent별 프롬프트
    private String buildIntentSpecificPrompt(Intent intent) {
        return switch (intent) {
            case GENERAL_QUESTION -> """
                대화 전략:
                일반 대화에서도 여행 관련 화제로 자연스럽게 전환
                예: "안녕하세요! 오늘은 어떤 일로 찾아오셨나요? 혹시 여행 계획이 있으신가요?"
                """;

            case WEATHER_INQUIRY -> """
                대화 전략:
                날씨 질문에 답하면서 전체 여행 계획의 필요성 제안
                예: "파리 날씨 정보를 알려드렸는데, 파리 여행 계획을 함께 세워볼까요?"
                """;

            case TRAVEL_PLANNING -> """
                대화 전략:
                여행 계획 시작 의사 확인 및 정보 수집 준비
                예: "여행 계획을 시작해볼까요? 먼저 목적지와 날짜를 알려주세요."
                """;

            case INFORMATION_COLLECTION -> """
                대화 전략:
                본격적인 여행 정보 수집 시작
                예: "좋아요! 완벽한 여행 계획을 위해 몇 가지 정보가 필요합니다."
                """;

            case IMAGE_UPLOAD -> """
                대화 전략:
                업로드된 이미지 분석 및 여행 계획에 반영
                예: "이미지를 확인했습니다. 이를 바탕으로 여행 계획을 세워드릴게요."
                """;

            case DESTINATION_SEARCH -> """
                대화 전략:
                목적지 검색 및 추천
                예: "원하시는 목적지를 찾아드리겠습니다. 어떤 스타일의 여행을 선호하시나요?"
                """;

            case PLAN_MODIFICATION -> """
                대화 전략:
                생성된 일정을 사용자 피드백에 따라 수정
                예: "어떤 부분을 수정하고 싶으신가요? 구체적으로 알려주세요."
                """;

            case FEEDBACK -> """
                대화 전략:
                피드백을 반영하여 계획 개선
                예: "피드백 감사합니다. 말씀하신 내용을 반영하여 수정하겠습니다."
                """;

            case COMPLETION -> """
                대화 전략:
                최종 계획 확정 및 요약
                예: "여행 계획이 완성되었습니다! 최종 일정을 확인해주세요."
                """;

            default -> """
                대화 전략:
                사용자의 의도를 명확히 파악한 후 적절한 도움 제공
                예: "어떤 것을 도와드릴까요? 여행 계획이나 정보가 필요하신가요?"
                """;
        };
    }

    // Phase별 프롬프트
    private String buildPhaseSpecificPrompt(TravelPhase phase) {
        return switch (phase) {
            case INITIALIZATION -> """
                Phase 목표: 사용자와의 관계 형성 및 여행 계획 시작 유도
                주요 작업:
                - 친근한 인사와 소개
                - 여행 계획 의사 확인
                - 기본 정보 수집 준비
                """;

            case INFORMATION_COLLECTION -> """
                Phase 목표: 여행 계획에 필요한 모든 정보 수집
                주요 작업:
                - 목적지, 날짜, 인원, 예산 확인
                - 여행 스타일과 선호도 파악
                - 특별 요구사항 확인
                """;

            case PLAN_GENERATION -> """
                Phase 목표: 수집된 정보로 맞춤형 여행 계획 생성
                주요 작업:
                - 일정표 작성
                - 숙소와 교통 추천
                - 관광지와 맛집 선정
                """;

            case FEEDBACK_REFINEMENT -> """
                Phase 목표: 사용자 피드백 반영하여 계획 개선
                주요 작업:
                - 수정 요청사항 확인
                - 대안 제시
                - 세부 조정
                """;

            case COMPLETION -> """
                Phase 목표: 최종 계획 확정 및 전달
                주요 작업:
                - 최종 일정 요약
                - 예약 정보 정리
                - 추가 팁 제공
                """;
        };
    }

    // Context 기반 프롬프트
    private String buildContextPrompt(TravelContext context) {
        if (context == null) {
            return "컨텍스트 정보 없음";
        }

        var collectInfo = context.getCollectedInfo();
        if (collectInfo == null || (collectInfo instanceof java.util.Map && ((java.util.Map)collectInfo).isEmpty())) {
            return "아직 수집된 정보가 없습니다.";
        }

        var prompt = new StringBuilder("현재까지 수집된 정보:\n");
        if (collectInfo instanceof java.util.Map) {
            ((java.util.Map<String, Object>)collectInfo).forEach((key, value) ->
                prompt.append(String.format("- %s: %s\n", key, value))
            );
        }

        // 부족한 정보 체크
        var missingInfo = collectInfo instanceof java.util.Map ?
            checkMissingInfo((java.util.Map<String, Object>)collectInfo) :
            java.util.Collections.emptyList();
        if (!missingInfo.isEmpty()) {
            prompt.append("\n추가로 필요한 정보:\n");
            missingInfo.forEach(info ->
                prompt.append(String.format("- %s\n", info))
            );
        }

        return prompt.toString();
    }

    // 부족한 정보 체크
    private List<String> checkMissingInfo(java.util.Map<String, Object> collectInfo) {
        var required = List.of("destination", "startDate", "endDate", "travelers", "budget");
        return required.stream()
            .filter(key -> !collectInfo.containsKey(key))
            .toList();
    }

    // 사용자 프롬프트 빌드
    public String buildUserPrompt(String message, TravelContext context) {
        return String.format("""
            사용자 메시지: %s

            대화 히스토리: %d개의 이전 대화
            현재 여행 계획 진행률: %d%%
            """,
            message,
            context != null ? context.getConversationCount() : 0,
            calculateProgress(context)
        );
    }

    // 진행률 계산
    private int calculateProgress(TravelContext context) {
        if (context == null) return 0;

        var phaseString = context.getCurrentPhase();
        if (phaseString == null) return 0;

        try {
            var phase = TravelPhase.valueOf(phaseString);
            return switch (phase) {
                case INITIALIZATION -> 0;
                case INFORMATION_COLLECTION -> 25;
                case PLAN_GENERATION -> 50;
                case FEEDBACK_REFINEMENT -> 75;
                case COMPLETION -> 100;
            };
        } catch (IllegalArgumentException e) {
            log.warn("Unknown phase: {}", phaseString);
            return 0;
        }
    }

    // LLM 프롬프트 메시지 생성 메서드
    public List<Message> buildPromptMessages(ChatRequest request, Intent intent, TravelPhase phase) {
        var messages = new ArrayList<Message>();

        // 시스템 메시지 추가
        var systemPrompt = buildSystemPrompt(intent, phase, null);
        messages.add(new SystemMessage(systemPrompt));

        // 사용자 메시지 추가
        var userPrompt = buildUserPrompt(request.getMessage(), null);
        messages.add(new UserMessage(userPrompt));

        return messages;
    }
}