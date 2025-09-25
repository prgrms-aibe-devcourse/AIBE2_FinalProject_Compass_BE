package com.compass.domain.chat.function.processing.phase3.date_selection.model;

import java.util.Map;

// 여행 요약 정보
public record TravelSummary(
    int totalDays,
    int totalPlaces,
    int totalRegions,
    double averageRegionsPerDay,
    Map<String, Integer> categoryDistribution,
    double estimatedTotalDistance,
    boolean llmReviewApplied
) {
    // Builder 패턴
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int totalDays;
        private int totalPlaces;
        private int totalRegions;
        private double averageRegionsPerDay;
        private Map<String, Integer> categoryDistribution;
        private double estimatedTotalDistance;
        private boolean llmReviewApplied;

        public Builder totalDays(int totalDays) { this.totalDays = totalDays; return this; }
        public Builder totalPlaces(int totalPlaces) { this.totalPlaces = totalPlaces; return this; }
        public Builder totalRegions(int totalRegions) { this.totalRegions = totalRegions; return this; }
        public Builder averageRegionsPerDay(double averageRegionsPerDay) {
            this.averageRegionsPerDay = averageRegionsPerDay; return this;
        }
        public Builder categoryDistribution(Map<String, Integer> categoryDistribution) {
            this.categoryDistribution = categoryDistribution; return this;
        }
        public Builder estimatedTotalDistance(double estimatedTotalDistance) {
            this.estimatedTotalDistance = estimatedTotalDistance; return this;
        }
        public Builder llmReviewApplied(boolean llmReviewApplied) {
            this.llmReviewApplied = llmReviewApplied; return this;
        }

        public TravelSummary build() {
            return new TravelSummary(totalDays, totalPlaces, totalRegions, averageRegionsPerDay,
                categoryDistribution, estimatedTotalDistance, llmReviewApplied);
        }
    }
}