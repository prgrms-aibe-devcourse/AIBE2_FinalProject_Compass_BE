package com.compass.domain.chat.stage3.model;

import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Stage3Response {
    private String status;
    private List<DailyItinerary> dailyItineraries;
    private List<Map<String, Object>> optimizedRoute;
    private Map<String, Object> transportInfo;
    private Integer estimatedCost;
    private String message;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyItinerary {
        private int day;
        private String date;
        private List<PlaceVisit> places;
        private Map<String, Object> dayInfo;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlaceVisit {
        private String placeId;
        private String placeName;
        private String category;
        private String arrivalTime;
        private String departureTime;
        private Integer duration; // 분 단위
        private Map<String, Object> placeInfo;
        private Map<String, Object> transport; // 다음 장소까지의 이동 정보
    }
}