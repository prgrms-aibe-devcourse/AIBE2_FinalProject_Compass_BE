package com.compass.domain.chat.function.processing.phase3.date_selection.model;

import java.util.Map;

// 날짜별 선별 최종 출력 데이터
public record DateSelectionOutput(
    Map<Integer, DailyItinerary> dailyItineraries,
    TravelSummary summary
) {
    // Builder 패턴
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Map<Integer, DailyItinerary> dailyItineraries;
        private TravelSummary summary;

        public Builder dailyItineraries(Map<Integer, DailyItinerary> dailyItineraries) {
            this.dailyItineraries = dailyItineraries;
            return this;
        }

        public Builder summary(TravelSummary summary) {
            this.summary = summary;
            return this;
        }

        public DateSelectionOutput build() {
            return new DateSelectionOutput(dailyItineraries, summary);
        }
    }

}