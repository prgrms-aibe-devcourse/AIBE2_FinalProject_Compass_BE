package com.compass.domain.chat.function.collection;

import com.compass.domain.chat.model.request.AnalyzeUserInputRequest;
import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import com.compass.domain.chat.parser.service.NaturalLanguageParser; // ◀◀◀ 1. NaturalLanguageParser를 import 합니다.
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Function;

// 사용자의 자연어 입력을 분석하여 여행 정보를 추출하고 업데이트하는 Function
@Slf4j
@Component("analyzeUserInput")
@RequiredArgsConstructor
public class AnalyzeUserInputFunction implements Function<AnalyzeUserInputRequest, TravelFormSubmitRequest> {

    private final NaturalLanguageParser naturalLanguageParser;

    @Override
    public TravelFormSubmitRequest apply(AnalyzeUserInputRequest request) {
        log.info("사용자 입력 분석 시작 -> NaturalLanguageParser 위임. userId: {}, input: '{}'", request.userId(), request.userInput());

        return naturalLanguageParser.parse(request.userInput(), request.currentInfo());
    }
}