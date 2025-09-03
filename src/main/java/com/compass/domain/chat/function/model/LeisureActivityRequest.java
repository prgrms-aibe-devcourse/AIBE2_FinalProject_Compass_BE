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
    
    @JsonPropertyDescription("Activity types: ['adventure', 'sports', 'wellness', 'entertainment', 'nature', 'water_sports', 'winter_sports', 'gaming']")
    List<String> activityTypes,
    
    @JsonPropertyDescription("Specific activities: ['hiking', 'cycling', 'surfing', 'skiing', 'spa', 'massage', 'yoga', 'theme_park', 'escape_room', 'karaoke', 'bowling']")
    List<String> specificActivities,
    
    @JsonPropertyDescription("Difficulty level: 'beginner', 'intermediate', 'advanced', 'all_levels' (default: 'all_levels')")
    String difficultyLevel,
    
    @JsonPropertyDescription("Duration in hours: 'under_1', '1_3', '3_6', 'half_day', 'full_day', 'multi_day'")
    String duration,
    
    @JsonPropertyDescription("Age group: 'kids', 'teens', 'adults', 'seniors', 'family' (default: 'adults')")
    String ageGroup,
    
    @JsonPropertyDescription("Group size: 'solo', 'couple', 'small_group', 'large_group' (default: 'couple')")
    String groupSize,
    
    @JsonPropertyDescription("Indoor or outdoor: 'indoor', 'outdoor', 'both' (default: 'both')")
    String environment,
    
    @JsonPropertyDescription("Price range: 'free', 'budget', 'moderate', 'premium' (default: 'moderate')")
    String priceRange,
    
    @JsonPropertyDescription("Equipment provided: true, false, or null (no preference)")
    Boolean equipmentProvided,
    
    @JsonPropertyDescription("Instructor/guide available: true, false, or null (no preference)")
    Boolean instructorAvailable,
    
    @JsonPropertyDescription("Maximum distance from location center in kilometers")
    Double maxDistance,
    
    @JsonPropertyDescription("Minimum rating (1-5)")
    Double minRating,
    
    @JsonPropertyDescription("Maximum number of results to return (default: 10)")
    Integer maxResults,
    
    @JsonPropertyDescription("Sort by: 'rating', 'distance', 'price', 'popularity' (default: 'rating')")
    String sortBy
) {
    public LeisureActivityRequest {
        // Default values
        if (difficultyLevel == null) difficultyLevel = "all_levels";
        if (ageGroup == null) ageGroup = "adults";
        if (groupSize == null) groupSize = "couple";
        if (environment == null) environment = "both";
        if (priceRange == null) priceRange = "moderate";
        if (maxResults == null) maxResults = 10;
        if (sortBy == null) sortBy = "rating";
    }
}