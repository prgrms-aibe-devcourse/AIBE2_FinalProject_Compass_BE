package com.compass.domain.chat.function.model;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/**
 * Request model for restaurant search function
 */
@JsonClassDescription("Search for restaurants in a specific location with cuisine and dietary preferences")
public record RestaurantSearchRequest(
    @JsonProperty(required = true)
    @JsonPropertyDescription("The city or area to search for restaurants (e.g., 'Seoul', 'Gangnam', 'Tokyo')")
    String location,
    
    @JsonPropertyDescription("Cuisine type: 'korean', 'japanese', 'chinese', 'italian', 'western', 'asian' (default: 'all')")
    String cuisineType,
    
    @JsonPropertyDescription("Price range: 'budget', 'moderate', 'fine_dining' (default: 'moderate')")
    String priceRange,
    
    @JsonPropertyDescription("Meal type: 'breakfast', 'lunch', 'dinner' (default: 'all')")
    String mealType
) {
    public RestaurantSearchRequest {
        // Default values
        if (cuisineType == null) cuisineType = "all";
        if (priceRange == null) priceRange = "moderate";
        if (mealType == null) mealType = "all";
    }
}