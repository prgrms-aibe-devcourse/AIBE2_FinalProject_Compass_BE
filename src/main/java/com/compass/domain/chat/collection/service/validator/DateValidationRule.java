package com.compass.domain.chat.collection.service.validator;

import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;

// 여행 날짜 검증 규칙
@Component
public class DateValidationRule implements ValidationRule {
    @Override
    public boolean appliesTo(TravelFormSubmitRequest request) {
        // 날짜 정보가 존재할 때만 이 규칙을 적용합니다.
        return request.travelDates() != null;
    }

    @Override
    public Optional<String> validate(TravelFormSubmitRequest request) {
        if (request.travelDates().startDate() == null || request.travelDates().endDate() == null) {
            return Optional.of("여행 날짜를 입력해주세요.");
        }
        if (request.travelDates().startDate().isAfter(request.travelDates().endDate())) {
            return Optional.of("출발일이 도착일보다 늦을 수 없습니다.");
        }
        return Optional.empty();
    }
}