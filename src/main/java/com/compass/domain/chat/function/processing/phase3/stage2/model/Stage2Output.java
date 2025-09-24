package com.compass.domain.chat.function.processing.phase3.stage2.model;

import java.util.Map;

// Stage2 최종 출력 데이터
public record Stage2Output(
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
            this.dailyItineraries = dailyItineraries; return this;
        }
        public Builder summary(TravelSummary summary) {
            this.summary = summary; return this;
        }

        public Stage2Output build() {
            return new Stage2Output(dailyItineraries, summary);
        }
    }

    // 호환성을 위한 getter
    public Map<Integer, DailyItinerary> getDailyItineraries() { return dailyItineraries; }
    public TravelSummary getSummary() { return summary; }
}