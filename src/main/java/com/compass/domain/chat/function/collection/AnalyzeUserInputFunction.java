package com.compass.domain.chat.function.collection;

import com.compass.domain.chat.model.request.AnalyzeUserInputRequest;
import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.function.Function;

// 사용자의 자연어 입력을 분석하여 여행 정보를 추출하고 업데이트하는 Function
@Slf4j
@Component("analyzeUserInput")
@RequiredArgsConstructor
public class AnalyzeUserInputFunction implements Function<AnalyzeUserInputRequest, TravelFormSubmitRequest> {

    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper;

    // LLM에게 역할을 지시하는 프롬프트 템플릿
    private static final String PROMPT_TEMPLATE = """
            당신은 여행 정보 추출 전문가입니다. 사용자가 제공한 새로운 정보를 바탕으로 기존 JSON 객체를 업데이트하는 것이 당신의 임무입니다.
            현재 날짜는 {currentDate}입니다. "다음 주 금요일"이나 "이번 주말" 같은 상대적인 날짜를 해석할 때 이 정보를 사용하세요.

            현재까지 수집된 여행 정보는 다음과 같습니다:
            ---
            {currentInfo}
            ---
            사용자가 방금 다음과 같은 새로운 정보를 입력했습니다: "{userInput}"

            사용자의 입력을 분석하여 현재 여행 정보를 업데이트하세요.
            - 사용자의 입력에 명확하게 언급된 필드만 업데이트하세요.
            - 언급되지 않은 정보는 임의로 만들지 마세요.
            - 사용자의 입력이 불분명하거나 여행 관련 질문에 대한 답변이 아닌 것 같으면, 아무것도 변경하지 말고 원래 정보를 그대로 반환하세요.

            오직 업데이트된 JSON 객체만 응답해야 합니다. JSON 구조는 반드시 다음 형식을 따라야 합니다:
            {
              "userId": "string",
              "destinations": ["string"],
              "departureLocation": "string",
              "travelDates": { "startDate": "YYYY-MM-DD", "endDate": "YYYY-MM-DD" },
              "companions": "string",
              "budget": 0,
              "travelStyle": ["string"],
              "reservationDocument": "string"
            }
            """;

    @Override
    public TravelFormSubmitRequest apply(AnalyzeUserInputRequest request) {
        log.info("사용자 입력 분석을 시작합니다. userId: {}, input: '{}'", request.userId(), request.userInput());

        try {
            String currentInfoJson = objectMapper.writeValueAsString(request.currentInfo());

            // 필요할 때마다 ChatClient를 직접 생성해서 사용
            ChatClient chatClient = chatClientBuilder.build();

            String llmResponse = chatClient.prompt()
                    .user(p -> p.text(PROMPT_TEMPLATE)
                            .param("currentDate", LocalDate.now().toString())
                            .param("currentInfo", currentInfoJson)
                            .param("userInput", request.userInput()))
                    .call()
                    .content(); // LLM의 응답을 순수 문자열로 받음

            // 응답 문자열을 DTO 객체로 직접 파싱
            return objectMapper.readValue(llmResponse, TravelFormSubmitRequest.class);

        } catch (Exception e) {
            log.error("LLM 호출 또는 응답 파싱 중 오류 발생. 원본 정보를 반환합니다. userId: {}", request.userId(), e);
            // 오류 발생 시, 기존 정보를 그대로 반환하여 안정성 확보
            return request.currentInfo();
        }
    }
}