package com.compass.domain.chat.collection.service.validator;

import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

// 목적지 입력 검증 규칙
@Component
public class DestinationValidationRule implements ValidationRule {
    @Override
    public void apply(TravelFormSubmitRequest info) {
        if (CollectionUtils.isEmpty(info.destinations())) {
            throw new IllegalArgumentException("여행 목적지를 입력해주세요.");
        }
    }
}
