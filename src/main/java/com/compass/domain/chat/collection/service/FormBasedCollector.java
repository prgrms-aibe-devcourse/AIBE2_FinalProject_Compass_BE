package com.compass.domain.chat.collection.service;

import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import com.compass.domain.chat.collection.service.validator.TravelInfoValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

// 폼 기반 정보 수집 구현체
@Component("formBasedCollector")
@RequiredArgsConstructor
public class FormBasedCollector implements TravelInfoCollector {

    private final ObjectMapper objectMapper;
    private final TravelInfoValidator validator;

    @Override
    @SneakyThrows
    public TravelFormSubmitRequest collect(String userInput, TravelFormSubmitRequest currentInfo) {
        // userInput이 JSON 형태의 폼 데이터라고 가정하고 DTO로 파싱
        return objectMapper.readValue(userInput, TravelFormSubmitRequest.class);
    }

    @Override
    public void validate(TravelFormSubmitRequest info) {
        // 검증 책임은 TravelInfoValidator에게 위임
        validator.validate(info);
    }
}