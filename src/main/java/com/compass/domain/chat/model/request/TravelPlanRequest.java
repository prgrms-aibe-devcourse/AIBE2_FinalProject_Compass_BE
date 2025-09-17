package com.compass.domain.chat.model.request;

import java.time.LocalDate;
import java.util.List;

// 여행 계획 생성 요청 DTO
public record TravelPlanRequest(
    List<String> destinations,        // 목적지 목록
    LocalDate startDate,             // 출발일
    LocalDate endDate,               // 도착일
    Integer budget,                  // 예산 (원)
    List<String> travelStyle,        // 여행 스타일 (문화, 자연, 액티비티 등)
    List<String> companions,         // 동행자 (혼자, 커플, 가족, 친구 등)
    String departureLocation,        // 출발지
    List<String> interests,          // 관심사 (음식, 쇼핑, 역사 등)
    String accommodationType,        // 숙박 타입 (호텔, 게스트하우스, 펜션 등)
    String transportationType        // 교통수단 (대중교통, 렌터카, 도보 등)
) {
    // 검증 로직
    public TravelPlanRequest {
        if (destinations == null || destinations.isEmpty()) {
            throw new IllegalArgumentException("목적지는 필수입니다");
        }
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("출발일과 도착일은 필수입니다");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("출발일이 도착일보다 늦을 수 없습니다");
        }
        if (budget != null && budget < 0) {
            throw new IllegalArgumentException("예산은 0 이상이어야 합니다");
        }
    }
}
