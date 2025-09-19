package com.compass.domain.chat.collection.dto;

import java.util.List;

// 수집된 여행 정보를 담는 불변 데이터 객체(DTO)
public record TravelInfo(
    List<String> destinations,
    DateRange travelDates, // startDate, endDate를 DateRange 객체로 통합
    Integer budget,
    String companions,
    String travelStyle
) {
    // 날짜 유효성 검증은 DateRange 객체가 담당하므로 여기서는 제거합니다.
}
