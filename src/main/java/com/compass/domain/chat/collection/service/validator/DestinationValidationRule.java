package com.compass.domain.chat.collection.service.validator;

import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Optional;

// 목적지 입력 검증 규칙
@Component
public class DestinationValidationRule implements ValidationRule {
    @Override
    public boolean appliesTo(TravelFormSubmitRequest request) {
        return true; // 목적지는 항상 검사해야 합니다.
    }

    @Override
    public Optional<String> validate(TravelFormSubmitRequest request) {
        if (CollectionUtils.isEmpty(request.destinations()) || request.destinations().stream().allMatch(String::isBlank)) {
            return Optional.of("여행 목적지를 입력해주세요.");
        }
        return Optional.empty();
    }
}
