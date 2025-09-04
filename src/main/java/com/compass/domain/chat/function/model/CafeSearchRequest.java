package com.compass.domain.chat.function.model;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/**
 * Request model for cafe search function
 */
@JsonClassDescription("Search for cafes in a specific location with various preferences")
public record CafeSearchRequest(
    @JsonProperty(required = true)
    @JsonPropertyDescription("The city or area to search for cafes (e.g., 'Seoul', 'Gangnam', 'Tokyo')")
    String location,
    
    @JsonPropertyDescription("Type of cafe: 'coffee', 'dessert', 'brunch', 'study' (default: 'coffee')")
    String cafeType,
    
    @JsonPropertyDescription("Price range: 'budget', 'moderate', 'premium' (default: 'moderate')")
    String priceRange
) {
    public CafeSearchRequest {
        // Default values
        if (cafeType == null) cafeType = "coffee";
        if (priceRange == null) priceRange = "moderate";
    }
}