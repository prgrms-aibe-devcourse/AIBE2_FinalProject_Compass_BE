package com.compass.domain.chat.function.model;

import lombok.Builder;

import java.util.List;

/**
 * Response model for cafe search function
 */
@Builder
public record CafeSearchResponse(
    String location,
    List<Cafe> cafes
) {
    @Builder
    public record Cafe(
        String name,
        String type,
        Double rating,
        String priceRange,
        String address,
        String description,
        List<String> atmosphere,
        List<String> amenities,
        String openingHours,
        String closingTime,
        List<String> specialties,
        List<String> images,
        Double distance,
        String distanceUnit,
        Integer reviewCount,
        String phoneNumber,
        String website,
        Boolean reservationAvailable,
        Integer averagePrice,
        String currency
    ) {}
}