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
        return classify(message, false);
    }

    // 맥락을 고려한 Intent 분류
    public Intent classify(String message, boolean isWaitingForConfirmation) {
        log.debug("Intent 분류 시작: {}", message);

        // 빈 메시지 처리
        if (message == null || message.isEmpty()) {
            return Intent.GENERAL_QUESTION;
        }

        try {
            // LLM 기반 Intent 분류 (맥락 정보 포함)
            var intent = classifyByLLM(message, isWaitingForConfirmation);
            log.info("Intent 분류 완료 - 메시지: '{}', 확인대기: {}, 결과: {}",
                message.length() > 50 ? message.substring(0, 50) + "..." : message,
                isWaitingForConfirmation,
                intent);
            return intent;
        } catch (Exception e) {
            log.error("Intent 분류 실패 - 메시지: '{}', 에러: {}", message, e.getMessage());
            // LLM 분류 실패 시 기본값 반환
            return Intent.GENERAL_QUESTION;
        }
    }

    // 구체적인 여행 질문 여부를 LLM으로 판단
    public boolean isSpecificTravelQuery(String message) {
        if (message == null || message.isEmpty()) {
            return false;
        }

        try {
            var systemPrompt = """
                사용자의 메시지가 구체적인 여행 질문인지 판단하세요.

                구체적인 여행 질문 기준:
                1. 특정 목적지 추천 요청 (예: "갈만한 곳", "추천해줘")
                2. 구체적인 여행 일정 문의 (예: "당일치기", "1박2일")
                3. 명확한 여행 계획 생성 요청 (예: "계획 짜줘", "일정 만들어줘")

                응답 형식:
                {
                    "isSpecific": true/false,
                    "reason": "판단 이유"
                }

                JSON 형식으로만 응답하세요.
                """;

            var userPrompt = "메시지: " + message;

            var prompt = new Prompt(List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(userPrompt)
            ));

            var response = chatModel.call(prompt);
            var result = response.getResult().getOutput().getContent();

            // JSON 파싱
            return result.contains("\"isSpecific\": true") || result.contains("\"isSpecific\":true");

        } catch (Exception e) {
            log.error("구체적 여행 질문 판단 실패: {}", e.getMessage());
            return false;
        }
    }


    // LLM 기반 Intent 분류 (기존 메소드)
    private Intent classifyByLLM(String message) {
        return classifyByLLM(message, false);
    }

    // LLM 기반 Intent 분류 (맥락 포함)
    private Intent classifyByLLM(String message, boolean isWaitingForConfirmation) {
        // 프롬프트 생성
        var systemPrompt = createSystemPrompt();
        var userPrompt = createUserPrompt(message, isWaitingForConfirmation);

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

            중요: 대화의 맥락과 상황을 고려하여 분류하세요.
            - 단순한 "네", "좋아" 같은 응답도 맥락에 따라 의도가 달라집니다
            - 직전 대화에서 여행 계획 제안이 있었다면 CONFIRMATION으로 분류
            - 일반 대화 중 단순 동의라면 GENERAL_QUESTION으로 분류

            사용자 메시지를 분석하여 다음 10가지 중 하나로 분류하세요:

            1. TRAVEL_PLANNING: 새로운 여행 계획 시작 요청
               예: "여행 계획 짜줘", "일정 짜줘", "3박 4일 제주도 여행 계획", "여행 가고 싶어"

            2. CONFIRMATION: 여행 계획 시작에 대한 명확한 동의 (중요: 여행 계획 제안 후의 응답만 해당)
               - 조건: 이전에 "여행 계획을 세워드릴까요?" 같은 제안이 있었을 때
               예: "네, 시작할게요", "좋아, 여행 계획 짜줘", "응, 부탁해"
               - 주의: 일반 대화의 "네", "좋아"는 해당 안됨

            3. INFORMATION_COLLECTION: 여행 정보 입력 및 수집
               예: "2월에 가고 싶어", "예산은 100만원", "3명이서 갈거야", "5박 6일로"

            4. IMAGE_UPLOAD: 이미지 업로드 및 OCR 처리
               예: "사진 보낼게", "이미지 업로드", "사진에서 정보 추출해줘"

            5. PLAN_MODIFICATION: 기존 여행 계획 수정 요청
               예: "일정 변경해줘", "호텔 바꿔줘", "일차 수정", "다른 곳으로 변경"

            6. GENERAL_QUESTION: 여행 관련 일반적인 질문 또는 명확하지 않은 응답
               예: "비자 필요해?", "환율 알려줘", "추천 관광지", "맛집 추천"
               - 맥락이 불분명한 단순 응답도 포함: "네", "좋아", "알겠어" (여행 계획 시작과 무관한 경우)

            7. WEATHER_INQUIRY: 날씨 관련 문의
               예: "파리 날씨 어때?", "비 오나요?", "기온은?", "날씨 정보"

            8. DESTINATION_SEARCH: 목적지 검색 및 추천
               예: "제주도 관광지", "파리 명소", "도쿄 어디가 좋아?", "추천 여행지"

            9. FEEDBACK: 사용자 피드백 및 개선 요청
               예: "마음에 안들어", "다시 짜줘", "더 좋은 방법 없어?", "개선해줘"

            10. COMPLETION: 여행 계획 완료 및 저장
               예: "완료", "저장해줘", "이대로 확정", "끝", "마무리"

            분류 원칙:
            - 여행과 직접 관련된 명확한 의도가 있어야 해당 Intent로 분류
            - 맥락이 불분명하면 GENERAL_QUESTION으로 분류
            - CONFIRMATION은 여행 계획 시작 동의가 명확한 경우만

            반드시 위 10가지 중 하나만 응답하세요.
            """;
    }

    // 사용자 프롬프트 생성 (기존)
    private String createUserPrompt(String message) {
        return createUserPrompt(message, false);
    }

    // 사용자 프롬프트 생성 (맥락 포함)
    private String createUserPrompt(String message, boolean isWaitingForConfirmation) {
        if (isWaitingForConfirmation) {
            return String.format("""
                다음 사용자 메시지의 의도를 분류하세요:

                맥락: 시스템이 방금 "여행 계획을 세워드릴까요?" 같은 여행 시작 제안을 했습니다.
                메시지: %s

                분석: 이 메시지가 여행 계획 시작에 대한 동의라면 CONFIRMATION,
                      그렇지 않다면 적절한 다른 Intent로 분류하세요.

                의도:
                """, message);
        } else {
            return String.format("""
                다음 사용자 메시지의 의도를 분류하세요:

                메시지: %s

                의도:
                """, message);
        }
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