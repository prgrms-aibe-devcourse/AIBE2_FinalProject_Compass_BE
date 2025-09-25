package com.compass.domain.chat.route_optimization.model;

import java.util.List;
import java.util.Map;

/**
 * 사용자 선택 기반 동선 최적화 요청
 */
public record UserSelectionRequest(
    Long sessionId,
    Map<Integer, List<String>> selectedPlaceIds,  // 일차별 선택된 장소 ID 리스트
    String transportMode,
    String optimizationStrategy
) {

    /**
     * 사용자 선택 예시
     * {
     *   "sessionId": 12345,
     *   "selectedPlaceIds": {
     *     1: ["place_1_2", "place_1_5", "place_1_8"],  // Day 1: 3개 선택
     *     2: ["place_2_1", "place_2_4"],               // Day 2: 2개 선택
     *     3: ["place_3_3", "place_3_6", "place_3_7"]   // Day 3: 3개 선택
     *   },
     *   "transportMode": "CAR",
     *   "optimizationStrategy": "BALANCED"
     * }
     */

    public static UserSelectionRequest of(
        Long sessionId,
        Map<Integer, List<String>> selections
    ) {
        return new UserSelectionRequest(
            sessionId,
            selections,
            "CAR",
            "BALANCED"
        );
    }
}