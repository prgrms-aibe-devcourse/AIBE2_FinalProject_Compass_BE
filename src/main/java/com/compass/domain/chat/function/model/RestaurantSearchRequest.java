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
    
    @JsonPropertyDescription("Cuisine types: ['korean', 'japanese', 'chinese', 'italian', 'french', 'american', 'thai', 'vietnamese', 'indian', 'mexican']")
    List<String> cuisineTypes,
    
    @JsonPropertyDescription("Meal type: 'breakfast', 'lunch', 'dinner', 'brunch', 'late_night' (default: any)")
    String mealType,
    
    @JsonPropertyDescription("Price range: 'budget', 'moderate', 'fine_dining', 'luxury' (default: 'moderate')")
    String priceRange,
    
    @JsonPropertyDescription("Dietary restrictions: ['vegetarian', 'vegan', 'halal', 'kosher', 'gluten_free', 'dairy_free']")
    List<String> dietaryRestrictions,
    
    @JsonPropertyDescription("Restaurant features: ['outdoor_seating', 'private_room', 'bar', 'live_music', 'view', 'parking']")
    List<String> features,
    
    @JsonPropertyDescription("Atmosphere: ['romantic', 'business', 'casual', 'family_friendly', 'trendy', 'traditional']")
    List<String> atmosphere,
    
    @JsonPropertyDescription("Michelin stars: 0 (no preference), 1, 2, or 3")
    Integer michelinStars,
    
    @JsonPropertyDescription("Maximum distance from location center in kilometers")
    Double maxDistance,
    
    @JsonPropertyDescription("Minimum rating (1-5)")
    Double minRating,
    
    @JsonPropertyDescription("Reservation required: true, false, or null (no preference)")
    Boolean reservationRequired,
    
    @JsonPropertyDescription("Maximum number of results to return (default: 10)")
    Integer maxResults,
    
    @JsonPropertyDescription("Sort by: 'rating', 'distance', 'price', 'popularity' (default: 'rating')")
    String sortBy
) {
    public RestaurantSearchRequest {
        // Default values
        if (priceRange == null) priceRange = "moderate";
        if (maxResults == null) maxResults = 10;
        if (sortBy == null) sortBy = "rating";
    }
}