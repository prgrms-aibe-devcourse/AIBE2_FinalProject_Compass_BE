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

        // 주입된 모든 규칙들을 순회하며, 적용 대상인 경우에만 검증을 실행합니다.
        for (var rule : rules) {
            if (rule.appliesTo(info)) {
                // validate 메서드가 반환한 오류 메시지(Optional)가 존재할 경우에만 리스트에 추가합니다.
                rule.validate(info).ifPresent(errors::add);
            }
        }
    }

    @Override
    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }
}