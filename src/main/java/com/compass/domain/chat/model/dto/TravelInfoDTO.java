package com.compass.domain.chat.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import java.time.LocalDate;
import java.util.List;

// Task 2.5.2: 수집된 여행 정보를 전달하는 핵심 데이터 객체 (DTO)

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TravelInfoDTO(
        String userId,
        List<String> destinations,
        String departureLocation,
        DateRange travelDates,
        String companions,
        Integer budget,
        List<String> travelStyle,
        String reservationDocument
) {
    public record DateRange(
            LocalDate startDate,
            LocalDate endDate
    ) {}

}