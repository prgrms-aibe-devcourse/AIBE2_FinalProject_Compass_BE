package com.compass.domain.chat.function.model;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.time.LocalDate;
import java.util.List;

/**
 * Request model for hotel search function
 */
@JsonClassDescription("Search for available hotels in a specific location")
public record HotelSearchRequest(
    @JsonProperty(required = true)
    @JsonPropertyDescription("The city or area to search for hotels (e.g., 'Seoul', 'Gangnam District', 'Tokyo')")
    String location,
    
    @JsonProperty(required = true)
    @JsonPropertyDescription("Check-in date in ISO format (YYYY-MM-DD)")
    LocalDate checkIn,
    
    @JsonProperty(required = true)
    @JsonPropertyDescription("Check-out date in ISO format (YYYY-MM-DD)")
    LocalDate checkOut,
    
    @JsonPropertyDescription("Number of guests (default: 2)")
    Integer guests,
    
    @JsonPropertyDescription("Number of rooms (default: 1)")
    Integer rooms,
    
    @JsonPropertyDescription("Minimum price per night in local currency")
    Integer minPrice,
    
    @JsonPropertyDescription("Maximum price per night in local currency")
    Integer maxPrice,
    
    @JsonPropertyDescription("Minimum hotel star rating (1-5)")
    Integer minRating,
    
    @JsonPropertyDescription("Required amenities (e.g., ['WiFi', 'Parking', 'Pool', 'Gym'])")
    List<String> amenities
) {
    public HotelSearchRequest {
        // Default values
        if (guests == null) guests = 2;
        if (rooms == null) rooms = 1;
    }
}