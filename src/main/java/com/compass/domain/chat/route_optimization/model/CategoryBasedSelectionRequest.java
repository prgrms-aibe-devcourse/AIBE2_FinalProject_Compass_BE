package com.compass.domain.chat.route_optimization.model;

import java.util.List;
import java.util.Map;

/**
 * 카테고리 기반 필수 장소 선택 요청 DTO
 */
public record CategoryBasedSelectionRequest(
    String threadId,
    Long sessionId,
    int tripDays,  // 여행 일수
    List<PlaceCandidate> allPlaces,  // Stage 1에서 선택된 전체 장소들
    Map<String, RequiredPlaces> requiredByCategory,  // 카테고리별 필수 장소
    TripConstraints constraints  // 여행 제약 조건
) {

    /**
     * 장소 후보 정보
     */
    public record PlaceCandidate(
        String id,
        String name,
        String category,  // 관광지, 맛집, 쇼핑, 체험, 문화 등
        double latitude,
        double longitude,
        String timeBlock,  // MORNING_ACTIVITY, LUNCH, AFTERNOON_ACTIVITY, DINNER, EVENING_ACTIVITY
        int priority,  // 1: 필수, 2: 선호, 3: 가능
        int estimatedMinutes  // 예상 체류 시간
    ) {}

    /**
     * 카테고리별 필수 장소 정보
     */
    public record RequiredPlaces(
        List<String> placeIds,  // 필수로 포함해야 할 장소 ID들
        int minPerDay,  // 일당 최소 개수
        int maxPerDay   // 일당 최대 개수
    ) {}

    /**
     * 여행 제약 조건
     */
    public record TripConstraints(
        Map<String, Integer> dailyLimits,  // 일당 장소 제한 (min, max)
        Map<String, CategoryConstraint> categoryBalance,  // 카테고리별 균형
        boolean preferSameAreaClustering,  // 같은 지역 클러스터링 선호
        String transportMode,  // CAR, PUBLIC_TRANSPORT, WALKING
        String optimizationStrategy  // DISTANCE, TIME, BALANCED
    ) {}

    /**
     * 카테고리 제약 조건
     */
    public record CategoryConstraint(
        int minPerDay,
        int maxPerDay,
        List<String> preferredTimeBlocks  // 선호 시간대
    ) {}

    // 헬퍼 메서드: 필수 장소만 추출
    public List<PlaceCandidate> getRequiredPlaces() {
        return allPlaces.stream()
            .filter(place -> place.priority == 1)
            .toList();
    }

    // 헬퍼 메서드: 카테고리별 장소 그룹화
    public Map<String, List<PlaceCandidate>> getPlacesByCategory() {
        return allPlaces.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                PlaceCandidate::category
            ));
    }

    // 헬퍼 메서드: 일당 필수 장소 개수 계산
    public int getRequiredPlacesPerDay() {
        if (tripDays == 0) return 2;  // 당일치기
        return Math.max(2, 4 / tripDays);  // 1박2일 이상
    }
}