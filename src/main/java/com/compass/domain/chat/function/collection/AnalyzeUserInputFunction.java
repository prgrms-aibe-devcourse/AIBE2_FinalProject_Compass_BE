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
            You are a travel information extractor. Your task is to update a JSON object with new information provided by a user.
            The current date is {currentDate}. Use this to resolve relative dates like "next Friday" or "this weekend".

            Here is the current travel information we have:
            ---
            {currentInfo}
            ---
            The user just provided this new input: "{userInput}"

            Analyze the user's input and update the current travel information.
            - Only update the fields that are clearly mentioned in the user's input.
            - Do not make up information for fields that are not mentioned.
            - If the user's input is unclear or doesn't seem to answer a travel-related question, return the original information without any changes.

            Respond with ONLY the updated JSON object. The JSON structure must be as follows:
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