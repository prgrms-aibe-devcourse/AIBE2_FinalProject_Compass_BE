package com.compass.domain.chat.collection.service.validator;

import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Component
public class DateValidationRule implements ValidationRule {
    @Override
    public boolean appliesTo(TravelFormSubmitRequest request) {
        return request.travelDates() != null;
    }

    @Override
    public Optional<String> validate(TravelFormSubmitRequest request) {
        if (request.travelDates().startDate() == null || request.travelDates().endDate() == null ||
                request.departureTime() == null || request.endTime() == null) {
            return Optional.of("여행 날짜와 시간을 모두 입력해주세요.");
        }
        if (request.travelDates().startDate().atTime(request.departureTime())
                .isAfter(request.travelDates().endDate().atTime(request.endTime()))) {
            return Optional.of("출발 시간이 도착 시간보다 늦을 수 없습니다.");
        }

        long daysBetween = ChronoUnit.DAYS.between(
                request.travelDates().startDate(),
                request.travelDates().endDate()
        );
        if (daysBetween > 2) {
            return Optional.of("여행 기간은 최대 2박 3일까지 설정할 수 있습니다.");
        }
        return Optional.empty();
    }
}