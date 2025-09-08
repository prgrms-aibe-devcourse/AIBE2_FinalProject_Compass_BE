package com.compass.domain.chat.function.model;

import lombok.Builder;

import java.util.List;

/**
 * Response model for cultural experience search function
 */
@Builder
public record CulturalExperienceResponse(
    String location,
    List<Experience> experiences
) {
    @Builder
    public record Experience(
        String name,
        String type,
        List<String> culturalFocus,
        Double rating,
        String description,
        String historicalBackground,
        String duration,
        String participationLevel,
        String authenticityLevel,
        List<String> languagesAvailable,
        String groupType,
        Integer minParticipants,
        Integer maxParticipants,
        String venue,
        String address,
        String schedule,
        Integer price,
        String currency,
        String priceIncludes,
        Boolean souvenirsIncluded,
        List<String> takeaways,
        Boolean photoAllowed,
        Boolean traditionalClothingProvided,
        List<String> requirements,
        List<String> restrictions,
        String instructorProfile,
        Boolean certificateProvided,
        String bookingPolicy,
        String cancellationPolicy,
        List<String> images,
        List<String> reviews,
        Double distance,
        String distanceUnit,
        Integer reviewCount,
        String phoneNumber,
        String website,
        String bestTimeToVisit,
        List<String> culturalNotes
    ) {}
}