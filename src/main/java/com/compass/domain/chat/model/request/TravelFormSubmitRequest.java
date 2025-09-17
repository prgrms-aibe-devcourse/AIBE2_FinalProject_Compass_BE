package com.compass.domain.chat.model.request;

import java.util.List;
import java.util.Map;

// 빠른 입력 폼에서 제출된 데이터를 담는 DTO
// 필독.md 규칙에 따라 record로 구현하여 불변성을 보장합니다.
public record TravelFormSubmitRequest(
    String userId,
    List<String> destinations,
    String departureLocation,
    Map<String, String> travelDates, // "startDate", "endDate"
    String companions,
    Long budget,
    List<String> travelStyle,
    String reservationDocument // 파일 ID 또는 URL
) {}