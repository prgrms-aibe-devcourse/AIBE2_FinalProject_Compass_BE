package com.compass.domain.chat.function.model;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/**
 * Request model for leisure activity search function
 */
@JsonClassDescription("Search for leisure activities and recreational options in a specific location")
public record LeisureActivityRequest(
    @JsonProperty(required = true)
    @JsonPropertyDescription("The city or area to search for activities (e.g., 'Seoul', 'Tokyo', 'Bangkok')")
    String location,
    
    @JsonPropertyDescription("Type of activity: 'sports', 'wellness', 'entertainment', 'outdoor', 'cultural' (default: 'all')")
    String activityType,
    
    @JsonPropertyDescription("Price range: 'budget', 'moderate', 'premium' (default: 'moderate')")
    String priceRange
) {
    public LeisureActivityRequest {
        // Default values
        if (activityType == null) activityType = "all";
        if (priceRange == null) priceRange = "moderate";
    }
}