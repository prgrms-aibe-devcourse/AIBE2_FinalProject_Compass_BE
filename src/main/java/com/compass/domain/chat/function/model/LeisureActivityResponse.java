package com.compass.domain.chat.function.model;

import lombok.Builder;

import java.util.List;

/**
 * Response model for leisure activity search function
 */
@Builder
public record LeisureActivityResponse(
    String location,
    List<Activity> activities
) {
    @Builder
    public record Activity(
        String name,
        String type,
        List<String> specificActivities,
        Double rating,
        String description,
        String difficultyLevel,
        String duration,
        String ageGroup,
        String groupSize,
        String environment,
        String address,
        String operatingHours,
        Integer price,
        String currency,
        String priceDetails,
        Boolean equipmentProvided,
        List<String> equipmentIncluded,
        Boolean instructorAvailable,
        List<String> languages,
        Boolean bookingRequired,
        String cancellationPolicy,
        List<String> safetyMeasures,
        List<String> requirements,
        List<String> restrictions,
        List<String> images,
        Double distance,
        String distanceUnit,
        Integer reviewCount,
        String phoneNumber,
        String website,
        Integer maxParticipants,
        String bestSeason
    ) {}
}