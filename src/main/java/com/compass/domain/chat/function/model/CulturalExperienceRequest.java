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
    
    @JsonPropertyDescription("Experience types: ['traditional', 'modern', 'religious', 'artistic', 'culinary', 'craft', 'performance', 'festival']")
    List<String> experienceTypes,
    
    @JsonPropertyDescription("Specific experiences: ['tea_ceremony', 'cooking_class', 'temple_stay', 'hanbok_rental', 'calligraphy', 'pottery', 'dance', 'music', 'martial_arts']")
    List<String> specificExperiences,
    
    @JsonPropertyDescription("Cultural focus: ['local_traditions', 'history', 'art', 'religion', 'cuisine', 'crafts', 'customs']")
    List<String> culturalFocus,
    
    @JsonPropertyDescription("Duration: 'under_1_hour', '1_3_hours', 'half_day', 'full_day', 'multi_day' (default: '1_3_hours')")
    String duration,
    
    @JsonPropertyDescription("Participation level: 'observation', 'hands_on', 'immersive' (default: 'hands_on')")
    String participationLevel,
    
    @JsonPropertyDescription("Language support: ['english', 'korean', 'japanese', 'chinese', 'spanish', 'french']")
    List<String> languageSupport,
    
    @JsonPropertyDescription("Group type: 'private', 'small_group', 'large_group', 'public' (default: 'small_group')")
    String groupType,
    
    @JsonPropertyDescription("Authenticity level: 'highly_authentic', 'authentic', 'modern_interpretation' (default: 'authentic')")
    String authenticityLevel,
    
    @JsonPropertyDescription("Include souvenirs/takeaways: true, false, or null (no preference)")
    Boolean includeSouvenirs,
    
    @JsonPropertyDescription("Photo opportunities: true, false, or null (no preference)")
    Boolean photoOpportunities,
    
    @JsonPropertyDescription("Price range: 'free', 'budget', 'moderate', 'premium' (default: 'moderate')")
    String priceRange,
    
    @JsonPropertyDescription("Suitable for children: true, false, or null (no preference)")
    Boolean familyFriendly,
    
    @JsonPropertyDescription("Maximum distance from location center in kilometers")
    Double maxDistance,
    
    @JsonPropertyDescription("Minimum rating (1-5)")
    Double minRating,
    
    @JsonPropertyDescription("Maximum number of results to return (default: 10)")
    Integer maxResults,
    
    @JsonPropertyDescription("Sort by: 'rating', 'authenticity', 'price', 'popularity' (default: 'rating')")
    String sortBy
) {
    public CulturalExperienceRequest {
        // Default values
        if (duration == null) duration = "1_3_hours";
        if (participationLevel == null) participationLevel = "hands_on";
        if (groupType == null) groupType = "small_group";
        if (authenticityLevel == null) authenticityLevel = "authentic";
        if (priceRange == null) priceRange = "moderate";
        if (maxResults == null) maxResults = 10;
        if (sortBy == null) sortBy = "rating";
    }
}