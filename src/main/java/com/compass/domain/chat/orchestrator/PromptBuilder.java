package com.compass.domain.chat.orchestrator;

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

// 프롬프트 빌더
@Slf4j
@Component
@RequiredArgsConstructor
public class PromptBuilder {

    // 프롬프트 메시지 리스트 생성
    public List<Message> buildPromptMessages(ChatRequest request, Intent intent, TravelPhase phase) {
        var messages = new ArrayList<Message>();

        // 시스템 메시지 추가
        messages.add(new SystemMessage(buildSystemPrompt(intent, phase)));

        // 사용자 메시지 추가
        messages.add(new UserMessage(request.getMessage()));

        log.debug("프롬프트 메시지 구성 완료: Intent={}, Phase={}", intent, phase);
        return messages;
    }

    // 시스템 프롬프트 구성
    public String buildSystemPrompt(Intent intent, TravelPhase phase) {
        var basePrompt = buildBasePrompt();
        var phasePrompt = buildPhaseSpecificPrompt(phase);
        var intentPrompt = buildIntentSpecificPrompt(intent);

        return String.format("""
            %s

            현재 Intent: %s
            현재 Phase: %s

            %s

            %s
            """,
            basePrompt,
            intent,
            phase,
            phasePrompt,
            intentPrompt
        );
    }

    // 기본 프롬프트
    private String buildBasePrompt() {
        return """
            당신은 친근하고 열정적인 여행 계획 전문가입니다.
            목표: 자연스럽게 대화를 이끌어 사용자가 여행 계획을 세우도록 도와주기

            항상 긍정적이고 도움이 되는 톤을 유지하며,
            사용자가 여행에 관심을 갖도록 자연스럽게 유도하세요.
            """;
    }

    // Intent별 프롬프트
    private String buildIntentSpecificPrompt(Intent intent) {
        return switch (intent) {
            case GENERAL_CHAT -> """
                대화 전략:
                일반 대화에서도 여행 관련 화제로 자연스럽게 전환
                예: "안녕하세요! 오늘은 어떤 일로 찾아오셨나요? 혹시 여행 계획이 있으신가요?"
                """;

            case TRAVEL_QUESTION -> """
                대화 전략:
                여행 질문에 답하면서 전체 여행 계획의 필요성 제안
                예: "파리 날씨 정보를 알려드렸는데, 파리 여행 계획을 함께 세워볼까요?"
                """;

            case TRAVEL_INFO_COLLECTION -> """
                대화 전략:
                본격적인 여행 정보 수집 시작
                예: "좋아요! 완벽한 여행 계획을 위해 몇 가지 정보가 필요합니다."
                """;

            case UNKNOWN -> """
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
                Phase 가이드:
                - 친근한 인사로 시작
                - 여행에 대한 관심 유도
                - 자연스럽게 여행 계획 제안
                """;

            case INFORMATION_COLLECTION -> """
                Phase 가이드:
                - 필수 정보 체계적으로 수집 (목적지, 날짜, 예산, 동행자)
                - 누락된 정보 확인
                - 추가 선호사항 파악
                """;

            case PLAN_GENERATION -> """
                Phase 가이드:
                - 수집된 정보 기반 최적 일정 생성
                - 일자별 세부 계획 제시
                - 동선 최적화 고려
                """;

            case FEEDBACK_REFINEMENT -> """
                Phase 가이드:
                - 사용자 피드백 적극 수용
                - 수정 사항 명확히 확인
                - 개선된 계획 제시
                """;

            case COMPLETION -> """
                Phase 가이드:
                - 최종 계획 요약 제시
                - 추가 도움 제안
                - 즐거운 여행 기원
                """;
        };
    }

    // 사용자 프롬프트 구성 (필요시 사용)
    public String buildUserPrompt(String message) {
        return message;
    }

    // 간단한 프롬프트 생성 (유틸리티)
    public String buildSimplePrompt(String template, Object... args) {
        return String.format(template, args);
    }
}