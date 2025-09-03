package com.compass.domain.chat.function.model;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/**
 * Request model for attraction search function
 */
@JsonClassDescription("Search for tourist attractions and points of interest in a specific location")
public record AttractionSearchRequest(
    @JsonProperty(required = true)
    @JsonPropertyDescription("The city or area to search for attractions (e.g., 'Seoul', 'Paris', 'Tokyo')")
    String location,
    
    @JsonPropertyDescription("Categories of attractions to search for (e.g., ['Museum', 'Historical Site', 'Park', 'Restaurant', 'Shopping'])")
    List<String> categories,
    
    @JsonPropertyDescription("Maximum distance from city center in kilometers")
    Double maxDistance,
    
    @JsonPropertyDescription("Minimum rating (1-5)")
    Double minRating,
    
    @JsonPropertyDescription("Maximum number of results to return (default: 10)")
    Integer maxResults,
    
    @JsonPropertyDescription("Sort by: 'rating', 'distance', 'popularity' (default: 'rating')")
    String sortBy
) {
    public AttractionSearchRequest {
        // Default values
        if (maxResults == null) maxResults = 10;
        if (sortBy == null) sortBy = "rating";
    }
}