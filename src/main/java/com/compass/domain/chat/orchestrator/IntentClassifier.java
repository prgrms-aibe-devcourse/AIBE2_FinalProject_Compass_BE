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

    public IntentClassifier(@Qualifier("vertexAiGeminiChat") ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    // 삭제: 키워드 맵은 더 이상 필요없음 (LLM만 사용)

    // 메시지로부터 Intent 분류 (LLM 기반)
    public Intent classify(String message) {
        log.debug("Intent 분류 시작: {}", message);

        // 빈 메시지 처리
        if (message == null || message.isEmpty()) {
            return Intent.GENERAL_QUESTION;
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
            return Intent.GENERAL_QUESTION;
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

            사용자 메시지를 분석하여 다음 9가지 중 하나로 분류하세요:

            1. TRAVEL_PLANNING: 새로운 여행 계획 시작 요청
               예: "여행 계획 짜줘", "일정 짜줘", "3박 4일 제주도 여행 계획", "여행 가고 싶어"

            2. INFORMATION_COLLECTION: 여행 정보 입력 및 수집
               예: "2월에 가고 싶어", "예산은 100만원", "3명이서 갈거야", "5박 6일로"

            3. IMAGE_UPLOAD: 이미지 업로드 및 OCR 처리
               예: "사진 보낼게", "이미지 업로드", "사진에서 정보 추출해줘"

            4. PLAN_MODIFICATION: 기존 여행 계획 수정 요청
               예: "일정 변경해줘", "호텔 바꿔줘", "일차 수정", "다른 곳으로 변경"

            5. GENERAL_QUESTION: 여행 관련 일반적인 질문
               예: "비자 필요해?", "환율 알려줘", "추천 관광지", "맛집 추천"

            6. WEATHER_INQUIRY: 날씨 관련 문의
               예: "파리 날씨 어때?", "비 오나요?", "기온은?", "날씨 정보"

            7. DESTINATION_SEARCH: 목적지 검색 및 추천
               예: "제주도 관광지", "파리 명소", "도쿄 어디가 좋아?", "추천 여행지"

            8. FEEDBACK: 사용자 피드백 및 개선 요청
               예: "마음에 안들어", "다시 짜줘", "더 좋은 방법 없어?", "개선해줘"

            9. COMPLETION: 여행 계획 완료 및 저장
               예: "완료", "저장해줘", "이대로 확정", "끝", "마무리"

            반드시 위 9가지 중 하나만 응답하세요.
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

        return Intent.GENERAL_QUESTION;
    }
}