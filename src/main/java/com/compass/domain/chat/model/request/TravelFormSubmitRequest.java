package com.compass.domain.chat.model.request;

import java.time.LocalDate;
import java.util.List;

// 빠른 입력 폼에서 제출된 데이터를 담는 DTO
public record TravelFormSubmitRequest(
    String userId,
    List<String> destinations,
    String departureLocation,
    DateRange travelDates,
    String companions,
    Long budget,
    List<String> travelStyle,
    String reservationDocument // 파일 ID 또는 URL
) {
    // 여행 기간을 명확하게 표현하는 내부 record
    public record DateRange(
        LocalDate startDate,
        LocalDate endDate
    ) {}
}