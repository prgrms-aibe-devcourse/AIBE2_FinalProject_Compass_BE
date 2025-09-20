package com.compass.domain.chat.collection.service;

import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import com.compass.domain.chat.collection.service.validator.TravelInfoValidator;
import com.compass.domain.chat.parser.service.TravelInfoParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// 대화 기반 정보 수집 구현체
@Component("conversationBasedCollector")
@RequiredArgsConstructor
public class ConversationBasedCollector implements TravelInfoCollector {

    private final TravelInfoParser parser;
    private final TravelInfoValidator validator;

    @Override
    public TravelFormSubmitRequest collect(String userInput, TravelFormSubmitRequest currentInfo) {
        // 정보 수집(파싱)의 실제 작업은 TravelInfoParser에게 위임
        return parser.parse(userInput, currentInfo);
    }

    @Override
    public void validate(TravelFormSubmitRequest info) {
        // 검증 책임은 TravelInfoValidator에게 위임
        validator.validate(info);
    }
}