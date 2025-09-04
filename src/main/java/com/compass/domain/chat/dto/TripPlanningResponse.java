package com.compass.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO for comprehensive trip planning with function calling
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripPlanningResponse {
    
    private String tripId;
    
    private String destination;
    
    private LocalDate startDate;
    
    private LocalDate endDate;
    
    private String itinerary;
    
    private LocalDateTime generatedAt;
    
    private String model;
    
    private String[] functionsUsed;
    
    private Long processingTimeMs;
    
    private TripDetails details;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TripDetails {
        private FlightInfo flights;
        private HotelInfo hotels;
        private WeatherInfo weather;
        private AttractionInfo[] attractions;
        private Integer estimatedTotalCost;
        private String currency;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FlightInfo {
        private String outboundFlight;
        private String returnFlight;
        private Integer totalPrice;
        private String currency;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HotelInfo {
        private String hotelName;
        private Integer nightlyRate;
        private Integer totalCost;
        private String currency;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeatherInfo {
        private Double averageTemperature;
        private String generalConditions;
        private String packingRecommendations;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttractionInfo {
        private String name;
        private String category;
        private String description;
        private Integer estimatedCost;
    }
}