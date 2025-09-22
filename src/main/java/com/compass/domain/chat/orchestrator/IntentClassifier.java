package com.compass.domain.chat.orchestrator;

import com.compass.domain.chat.model.enums.Intent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

// Intent 분류기
@Slf4j
@Component
public class IntentClassifier {

    private final ChatModel chatModel;  // 필수 의존성

    public IntentClassifier(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    // 삭제: 키워드 맵은 더 이상 필요없음 (LLM만 사용)

    // 메시지로부터 Intent 분류 (LLM 기반)
    public Intent classify(String message) {
        log.debug("Intent 분류 시작: {}", message);

        // 빈 메시지 처리
        if (message == null || message.isEmpty()) {
            return Intent.UNKNOWN;
        }

        try {
            // LLM 기반 Intent 분류
            var intent = classifyByLLM(message);
            log.info("Intent 분류 완료 - 메시지: '{}', 결과: {}",
                message.length() > 50 ? message.substring(0, 50) + "..." : message,
                intent);
            return intent;
        } catch (Exception e) {
            log.error("Intent 분류 실패 - 메시지: '{}', 에러: {}", message, e.getMessage());
            // LLM 분류 실패 시 기본값 반환
            return Intent.GENERAL_CHAT;
        }
    }


    // LLM 기반 Intent 분류
    private Intent classifyByLLM(String message) {
        // 프롬프트 생성
        var systemPrompt = createSystemPrompt();
        var userPrompt = createUserPrompt(message);

        // LLM 호출
        var prompt = new Prompt(List.of(
            new SystemMessage(systemPrompt),
            new UserMessage(userPrompt)
        ));

        var response = chatModel.call(prompt);
        var result = response.getResult().getOutput().getContent();

        log.debug("LLM 응답: {}", result);

        // Intent 파싱
        return parseIntentFromResponse(result);
    }

    // 시스템 프롬프트 생성
    private String createSystemPrompt() {
        return """
            당신은 사용자 메시지의 의도를 정확히 분류하는 전문가입니다.

            사용자 메시지를 분석하여 다음 3가지 중 하나로 분류하세요:

            1. TRAVEL_INFO_COLLECTION: 사용자가 여행 계획을 짜달라고 명시적으로 요청
               예: "여행 계획 짜줘", "일정 짜줘", "3박 4일 제주도 여행 계획", "여행 가고 싶어"

            2. TRAVEL_QUESTION: 여행 관련 정보를 묻는 질문
               예: "파리 날씨 어때?", "제주도 맛집 추천", "비자 필요해?", "호텔 추천"

            3. GENERAL_CHAT: 여행과 무관한 일반 대화
               예: "안녕하세요", "고마워", "날씨 좋네", "뭐해?"

            반드시 위 3가지 중 하나만 응답하세요.
            """;
    }

    // 사용자 프롬프트 생성
    private String createUserPrompt(String message) {
        return String.format("""
            다음 사용자 메시지의 의도를 분류하세요:

            메시지: %s

            의도:
            """, message);
    }

    // LLM 응답에서 Intent 파싱
    private Intent parseIntentFromResponse(String response) {
        var cleanResponse = response.toUpperCase().trim();

        try {
            // 응답에서 Intent enum 값 추출
            for (var intent : Intent.values()) {
                if (cleanResponse.contains(intent.name())) {
                    log.debug("LLM 분류 결과: {}", intent);
                    return intent;
                }
            }
        } catch (Exception e) {
            log.error("Intent 파싱 실패: {}", e.getMessage());
        }

        return Intent.GENERAL_CHAT;
    }
}