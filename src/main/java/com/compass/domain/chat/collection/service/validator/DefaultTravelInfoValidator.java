package com.compass.domain.chat.collection.service.validator;

import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

// ValidationRule들을 모아 실행하는 기본 검증기
@Component
@RequiredArgsConstructor
public class DefaultTravelInfoValidator implements TravelInfoValidator {

    // Spring이 등록된 모든 ValidationRule Bean을 주입
    private final List<ValidationRule> rules;
    private final List<String> errors = new ArrayList<>();

    @Override
    public void validate(TravelFormSubmitRequest info) {
        errors.clear();

        // 주입된 모든 규칙들을 순회하며 검증
        for (var rule : rules) {
            try {
                rule.apply(info);
            } catch (IllegalArgumentException e) {
                // 규칙 위반 시, 오류 메시지를 리스트에 추가
                errors.add(e.getMessage());
            }
        }
    }

    @Override
    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }
}