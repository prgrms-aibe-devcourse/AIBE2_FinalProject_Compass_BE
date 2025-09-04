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
    
    @JsonPropertyDescription("Type of cafe: 'coffee_shop', 'dessert_cafe', 'brunch_cafe', 'study_cafe', 'pet_cafe' (default: 'coffee_shop')")
    String cafeType,
    
    @JsonPropertyDescription("Atmosphere preferences: ['quiet', 'lively', 'cozy', 'modern', 'vintage', 'instagrammable']")
    List<String> atmosphere,
    
    @JsonPropertyDescription("Available amenities: ['wifi', 'power_outlets', 'parking', 'outdoor_seating', 'pet_friendly']")
    List<String> amenities,
    
    @JsonPropertyDescription("Price range: 'budget', 'moderate', 'premium' (default: 'moderate')")
    String priceRange,
    
    @JsonPropertyDescription("Maximum distance from location center in kilometers")
    Double maxDistance,
    
    @JsonPropertyDescription("Minimum rating (1-5)")
    Double minRating,
    
    @JsonPropertyDescription("Maximum number of results to return (default: 10)")
    Integer maxResults,
    
    @JsonPropertyDescription("Sort by: 'rating', 'distance', 'popularity', 'price' (default: 'rating')")
    String sortBy
) {
    public CafeSearchRequest {
        // Default values
        if (cafeType == null) cafeType = "coffee_shop";
        if (priceRange == null) priceRange = "moderate";
        if (maxResults == null) maxResults = 10;
        if (sortBy == null) sortBy = "rating";
    }
}