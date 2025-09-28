package com.compass.domain.chat.stage3.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stage3Response {
    private List<DailyItinerary> dailyItineraries;
    private List<OptimizedRoute> optimizedRoutes;
    private double totalDistance;
    private long totalDuration;
    private LocalDateTime generatedAt;
    private TravelStatistics statistics;

    // Use Stage3Output as base for the response
    public static Stage3Response from(Stage3Output output) {
        return Stage3Response.builder()
            .dailyItineraries(output.getDailyItineraries())
            .optimizedRoutes(output.getOptimizedRoutes())
            .totalDistance(output.getTotalDistance())
            .totalDuration(output.getTotalDuration())
            .generatedAt(output.getGeneratedAt())
            .statistics(TravelStatistics.from(output.getStatistics()))
            .build();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TravelStatistics {
        private int totalPlaces;
        private int userSelectedCount;
        private int aiRecommendedCount;
        private double averageRating;
        private int totalReviews;
        private Map<String, Integer> categoryDistribution;

        public static TravelStatistics from(Stage3Output.TravelStatistics stats) {
            if (stats == null) return null;
            return TravelStatistics.builder()
                .totalPlaces(stats.getTotalPlaces())
                .userSelectedCount(stats.getUserSelectedCount())
                .aiRecommendedCount(stats.getAiRecommendedCount())
                .averageRating(stats.getAverageRating())
                .totalReviews(stats.getTotalReviews())
                .categoryDistribution(stats.getCategoryDistribution())
                .build();
        }
    }
}