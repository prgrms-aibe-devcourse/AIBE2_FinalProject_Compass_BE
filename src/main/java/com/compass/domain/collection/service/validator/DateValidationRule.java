package com.compass.domain.collection.service.validator;

import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import org.springframework.stereotype.Component;

// 여행 날짜 검증 규칙
@Component
public class DateValidationRule implements ValidationRule {
    @Override
    public void apply(TravelFormSubmitRequest info) {
        // 날짜 객체 또는 내부 필드가 null인지 확인
        if (info.travelDates() == null || info.travelDates().startDate() == null || info.travelDates().endDate() == null) {
            throw new IllegalArgumentException("여행 날짜를 입력해주세요.");
        }
        // 시작일이 종료일보다 늦는지 확인
        if (info.travelDates().startDate().isAfter(info.travelDates().endDate())) {
            throw new IllegalArgumentException("출발일이 도착일보다 늦을 수 없습니다.");
        }
    }
}