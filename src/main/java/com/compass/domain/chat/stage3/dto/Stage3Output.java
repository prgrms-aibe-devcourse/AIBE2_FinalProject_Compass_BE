package com.compass.domain.chat.stage3.dto;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

// Stage 3 출력 데이터 (최종 여행 계획)
@Getter
@Builder
public class Stage3Output {
    private final List<DailyItinerary> dailyItineraries;  // 일별 여행 일정
    private final List<OptimizedRoute> optimizedRoutes;   // 최적화된 경로들
    private final double totalDistance;                   // 전체 이동 거리 (km)
    private final long totalDuration;                     // 전체 소요 시간 (분)
    private final LocalDateTime generatedAt;              // 생성 시각
    private final TravelStatistics statistics;            // 여행 통계 정보

    // 통계 정보 추가
    @Getter
    @Builder
    public static class TravelStatistics {
        private final int totalPlaces;          // 전체 방문 장소 수
        private final int userSelectedCount;    // 사용자 선택 장소 수
        private final int aiRecommendedCount;   // AI 추천 장소 수
        private final double averageRating;     // 평균 평점
        private final int totalReviews;         // 전체 리뷰 수
        private final Map<String, Integer> categoryDistribution;  // 카테고리별 분포
    }

    // 빌더에 통계 자동 계산 추가
    public static class Stage3OutputBuilder {
        public Stage3Output build() {
            // 통계 정보 자동 계산
            if (statistics == null && dailyItineraries != null) {
                statistics = calculateStatistics(dailyItineraries);
            }

            return new Stage3Output(
                dailyItineraries,
                optimizedRoutes,
                totalDistance,
                totalDuration,
                generatedAt != null ? generatedAt : LocalDateTime.now(),
                statistics
            );
        }

        private TravelStatistics calculateStatistics(List<DailyItinerary> itineraries) {
            int totalPlaces = 0;
            int userSelected = 0;
            double totalRating = 0;
            int totalReviews = 0;
            Map<String, Integer> categories = new HashMap<>();

            for (DailyItinerary itinerary : itineraries) {
                for (var place : itinerary.getPlaces()) {
                    totalPlaces++;
                    if (place.getIsUserSelected() != null && place.getIsUserSelected()) {
                        userSelected++;
                    }
                    if (place.getRating() != null) {
                        totalRating += place.getRating();
                    }
                    if (place.getReviewCount() != null) {
                        totalReviews += place.getReviewCount();
                    }
                    categories.merge(place.getCategory(), 1, Integer::sum);
                }
            }

            return TravelStatistics.builder()
                .totalPlaces(totalPlaces)
                .userSelectedCount(userSelected)
                .aiRecommendedCount(totalPlaces - userSelected)
                .averageRating(totalPlaces > 0 ? totalRating / totalPlaces : 0)
                .totalReviews(totalReviews)
                .categoryDistribution(categories)
                .build();
        }
    }
}