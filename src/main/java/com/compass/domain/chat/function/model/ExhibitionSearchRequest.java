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
    
    @JsonPropertyDescription("Exhibition types: ['art', 'museum', 'gallery', 'science', 'history', 'technology', 'fashion', 'photography', 'design']")
    List<String> exhibitionTypes,
    
    @JsonPropertyDescription("Art styles: ['contemporary', 'modern', 'classical', 'traditional', 'digital', 'interactive', 'experimental']")
    List<String> artStyles,
    
    @JsonPropertyDescription("Event types: ['permanent', 'temporary', 'special', 'opening', 'workshop', 'talk', 'guided_tour']")
    List<String> eventTypes,
    
    @JsonPropertyDescription("Start date for exhibition search (YYYY-MM-DD)")
    LocalDate startDate,
    
    @JsonPropertyDescription("End date for exhibition search (YYYY-MM-DD)")
    LocalDate endDate,
    
    @JsonPropertyDescription("Venue types: ['museum', 'gallery', 'cultural_center', 'convention_center', 'outdoor', 'virtual']")
    List<String> venueTypes,
    
    @JsonPropertyDescription("Target audience: ['general', 'adults', 'children', 'students', 'professionals', 'art_enthusiasts']")
    String targetAudience,
    
    @JsonPropertyDescription("Famous artists or specific themes to search for")
    List<String> artistsOrThemes,
    
    @JsonPropertyDescription("Language support: ['english', 'korean', 'japanese', 'chinese', 'audio_guide']")
    List<String> languageSupport,
    
    @JsonPropertyDescription("Accessibility features: ['wheelchair_access', 'audio_description', 'sign_language', 'braille']")
    List<String> accessibilityFeatures,
    
    @JsonPropertyDescription("Interactive elements: true, false, or null (no preference)")
    Boolean interactiveElements,
    
    @JsonPropertyDescription("Photography allowed: true, false, or null (no preference)")
    Boolean photographyAllowed,
    
    @JsonPropertyDescription("Entry fee range: 'free', 'budget', 'moderate', 'premium' (default: 'moderate')")
    String entryFeeRange,
    
    @JsonPropertyDescription("Include workshops or educational programs: true, false, or null")
    Boolean includePrograms,
    
    @JsonPropertyDescription("Maximum distance from location center in kilometers")
    Double maxDistance,
    
    @JsonPropertyDescription("Minimum rating (1-5)")
    Double minRating,
    
    @JsonPropertyDescription("Maximum number of results to return (default: 10)")
    Integer maxResults,
    
    @JsonPropertyDescription("Sort by: 'popularity', 'date', 'rating', 'price' (default: 'popularity')")
    String sortBy
) {
    public ExhibitionSearchRequest {
        // Default values
        if (entryFeeRange == null) entryFeeRange = "moderate";
        if (maxResults == null) maxResults = 10;
        if (sortBy == null) sortBy = "popularity";
    }
}