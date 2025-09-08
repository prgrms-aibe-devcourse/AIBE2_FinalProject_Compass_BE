package com.compass.domain.chat.function.model;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/**
 * Request model for cultural experience search function
 */
@JsonClassDescription("Search for cultural experiences and traditional activities in a specific location")
public record CulturalExperienceRequest(
    @JsonProperty(required = true)
    @JsonPropertyDescription("The city or area to search for cultural experiences (e.g., 'Seoul', 'Kyoto', 'Bangkok')")
    String location,
    
    @JsonPropertyDescription("Type of experience: 'traditional', 'modern', 'culinary', 'artistic', 'religious' (default: 'traditional')")
    String experienceType,
    
    @JsonPropertyDescription("Duration: 'short' (under 3 hours), 'half-day', 'full-day' (default: 'half-day')")
    String duration,
    
    @JsonPropertyDescription("Price range: 'budget', 'moderate', 'premium' (default: 'moderate')")
    String priceRange
) {
    public CulturalExperienceRequest {
        // Default values
        if (experienceType == null) experienceType = "traditional";
        if (duration == null) duration = "half-day";
        if (priceRange == null) priceRange = "moderate";
    }
}