package com.compass.domain.chat.parser.service;

import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

//  LLM을 이용해 자연어 입력을 분석하는 파서 구현체
@Slf4j
@Service
public class NaturalLanguageParser implements TravelInfoParser {

    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper;

    public NaturalLanguageParser(@Lazy ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper) {
        this.chatClientBuilder = chatClientBuilder;
        this.objectMapper = objectMapper;
    }

    // LLM 역할 정의 프롬프트 (한글 버전)
    private static final String PROMPT_TEMPLATE = """
            You are a factual information extractor. Your task is to update a JSON object based on new user input.
            - ONLY extract explicit facts from the user input.
            - DO NOT infer, guess, or interpret ambiguous information. (e.g., If the user says "next week", do not calculate the date. If they say "a cheap trip", do not estimate a budget.)
            - The "destinations" key MUST always be an array of strings, for example: ["Seoul"] or ["Seoul", "Busan"]. Even for a single destination, it must be in an array.
            - Your output MUST be a pure JSON object without any explanations or markdown.

            Current Date: {currentDate}
            Existing JSON:
            {currentInfo}

            User Input:
            "{userInput}"

            Updated JSON:
            """;

    @Override
    public TravelFormSubmitRequest parse(String userInput, TravelFormSubmitRequest currentInfo) {
        try {
            var currentInfoJson = objectMapper.writeValueAsString(currentInfo);
            var chatClient = chatClientBuilder.build();

            var llmResponseString = chatClient.prompt()
                    .user(p -> p.text(PROMPT_TEMPLATE)
                            .param("currentDate", LocalDate.now().toString())
                            .param("currentInfo", currentInfoJson)
                            .param("userInput", userInput))
                    .call()
                    .content();

            String cleanedJson = cleanJsonString(llmResponseString);

            return objectMapper.readValue(cleanedJson, TravelFormSubmitRequest.class);

        } catch (Exception e) {
            // LLM 파싱 실패 시, 대화 흐름이 끊기지 않도록 원본 정보 반환
            log.error("LLM 파싱 오류: {}", e.getMessage(), e);
            return currentInfo;
        }
    }

    private String cleanJsonString(String response) {
        int firstBrace = response.indexOf('{');
        int lastBrace = response.lastIndexOf('}');
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            return response.substring(firstBrace, lastBrace + 1);
        }
        return response;
    }
}