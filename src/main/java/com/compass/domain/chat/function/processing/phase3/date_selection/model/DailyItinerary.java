package com.compass.domain.chat.function.processing.phase3.date_selection.model;

import java.time.LocalDate;
import java.util.List;

// 일별 여행 일정
public record DailyItinerary(
    int dayNumber,
    LocalDate date,
    List<String> regions,
    List<TourPlace> places,
    double totalDistance
) {
    // Builder 패턴
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int dayNumber;
        private LocalDate date;
        private List<String> regions;
        private List<TourPlace> places;
        private double totalDistance;

        public Builder dayNumber(int dayNumber) {
            this.dayNumber = dayNumber;
            return this;
        }

        public Builder date(LocalDate date) {
            this.date = date;
            return this;
        }

        public Builder regions(List<String> regions) {
            this.regions = regions;
            return this;
        }

        public Builder places(List<TourPlace> places) {
            this.places = places;
            return this;
        }

        public Builder totalDistance(double totalDistance) {
            this.totalDistance = totalDistance;
            return this;
        }

        public DailyItinerary build() {
            return new DailyItinerary(dayNumber, date, regions, places, totalDistance);
        }
    }


    // setter 대체 메서드 (불변 객체로 새 인스턴스 생성)
    public DailyItinerary withTotalDistance(double newDistance) {
        return new DailyItinerary(dayNumber, date, regions, places, newDistance);
    }

    public DailyItinerary withPlaces(List<TourPlace> newPlaces) {
        return new DailyItinerary(dayNumber, date, regions, newPlaces, totalDistance);
    }
}