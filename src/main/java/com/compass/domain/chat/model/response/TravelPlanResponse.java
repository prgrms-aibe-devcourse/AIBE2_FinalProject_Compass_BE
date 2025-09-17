package com.compass.domain.chat.model.response;

import java.util.List;
import java.util.UUID;

// 여행 계획 생성 응답 DTO
public record TravelPlanResponse(
    String planId,                    // 계획 ID
    Object itinerary,                 // 일정 정보 (실제 일정 객체로 대체 예정)
    Integer totalCost,                // 총 비용 (원)
    String message,                   // 응답 메시지
    List<String> recommendations      // 추천 사항
) {
    // 검증 로직
    public TravelPlanResponse {
        if (planId == null || planId.isEmpty()) {
            planId = UUID.randomUUID().toString();
        }
        if (message == null || message.isEmpty()) {
            message = "여행 계획이 생성되었습니다";
        }
        if (totalCost != null && totalCost < 0) {
            throw new IllegalArgumentException("총 비용은 0 이상이어야 합니다");
        }
    }

    // 성공 응답 생성
    public static TravelPlanResponse success(String planId, Object itinerary, Integer totalCost) {
        return new TravelPlanResponse(
            planId,
            itinerary,
            totalCost,
            "여행 계획이 성공적으로 생성되었습니다!",
            null
        );
    }

    // 실패 응답 생성
    public static TravelPlanResponse error(String message) {
        return new TravelPlanResponse(
            "",
            null,
            0,
            message,
            null
        );
    }

    // 추천사항 포함 성공 응답 생성
    public static TravelPlanResponse successWithRecommendations(
            String planId, 
            Object itinerary, 
            Integer totalCost, 
            List<String> recommendations) {
        return new TravelPlanResponse(
            planId,
            itinerary,
            totalCost,
            "여행 계획이 성공적으로 생성되었습니다!",
            recommendations
        );
    }
}
