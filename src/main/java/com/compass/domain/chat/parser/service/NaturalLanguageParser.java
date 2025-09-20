package com.compass.domain.chat.parser.service;

import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

//  LLM을 이용해 자연어 입력을 분석하는 파서 구현체
@Slf4j
@Service
@RequiredArgsConstructor
public class NaturalLanguageParser implements TravelInfoParser {

    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper;

    // LLM 역할 정의 프롬프트 (한글 버전)
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
            - 최종 결과는 반드시 JSON 객체 형식으로만 반환해야 하며, 다른 부가 설명은 절대 추가하지 마세요.
            """;

    @Override
    public TravelFormSubmitRequest parse(String userInput, TravelFormSubmitRequest currentInfo) {
        try {
            var currentInfoJson = objectMapper.writeValueAsString(currentInfo);
            var chatClient = chatClientBuilder.build();

            var llmResponse = chatClient.prompt()
                    .user(p -> p.text(PROMPT_TEMPLATE)
                            .param("currentDate", LocalDate.now().toString())
                            .param("currentInfo", currentInfoJson)
                            .param("userInput", userInput))
                    .call()
                    .content();

            return objectMapper.readValue(llmResponse, TravelFormSubmitRequest.class);

        } catch (Exception e) {
            // LLM 파싱 실패 시, 대화 흐름이 끊기지 않도록 원본 정보 반환
            log.error("LLM 파싱 오류: {}", e.getMessage());
            return currentInfo;
        }
    }
}