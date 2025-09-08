package com.compass.domain.chat.function.model;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.time.LocalDate;
import java.util.List;

/**
 * Request model for exhibition and event search function
 */
@JsonClassDescription("Search for exhibitions, art shows, and cultural events in a specific location")
public record ExhibitionSearchRequest(
    @JsonProperty(required = true)
    @JsonPropertyDescription("The city or area to search for exhibitions (e.g., 'Seoul', 'Tokyo', 'Paris')")
    String location,
    
    @JsonPropertyDescription("Type of exhibition: 'art', 'museum', 'science', 'history', 'technology' (default: 'all')")
    String exhibitionType,
    
    @JsonPropertyDescription("Entry fee range: 'free', 'budget', 'moderate', 'premium' (default: 'moderate')")
    String entryFeeRange
) {
    public ExhibitionSearchRequest {
        // Default values
        if (exhibitionType == null) exhibitionType = "all";
        if (entryFeeRange == null) entryFeeRange = "moderate";
    }
}