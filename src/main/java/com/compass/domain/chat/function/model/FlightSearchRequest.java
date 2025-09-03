package com.compass.domain.chat.function.model;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.time.LocalDate;

/**
 * Request model for flight search function
 * This record is used by Spring AI to generate function schema for LLM
 */
@JsonClassDescription("Search for available flights between two locations")
public record FlightSearchRequest(
    @JsonProperty(required = true)
    @JsonPropertyDescription("The departure airport or city (e.g., 'Seoul', 'ICN', 'New York')")
    String origin,
    
    @JsonProperty(required = true)
    @JsonPropertyDescription("The arrival airport or city (e.g., 'Tokyo', 'NRT', 'Paris')")
    String destination,
    
    @JsonProperty(required = true)
    @JsonPropertyDescription("The departure date in ISO format (YYYY-MM-DD)")
    LocalDate departureDate,
    
    @JsonPropertyDescription("The return date for round-trip flights in ISO format (YYYY-MM-DD)")
    LocalDate returnDate,
    
    @JsonPropertyDescription("Number of passengers (default: 1)")
    Integer passengers,
    
    @JsonPropertyDescription("Travel class: 'economy', 'business', or 'first' (default: 'economy')")
    String travelClass
) {
    public FlightSearchRequest {
        // Default values
        if (passengers == null) passengers = 1;
        if (travelClass == null) travelClass = "economy";
    }
}