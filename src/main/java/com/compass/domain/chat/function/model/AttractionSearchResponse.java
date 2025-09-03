package com.compass.domain.chat.function.model;

import lombok.Builder;

import java.util.List;

/**
 * Response model for attraction search function
 */
@Builder
public record AttractionSearchResponse(
    String location,
    List<Attraction> attractions
) {
    @Builder
    public record Attraction(
        String name,
        String category,
        Double rating,
        String description,
        String address,
        String openingHours,
        Integer ticketPrice,
        String currency,
        String estimatedDuration,
        List<String> tags,
        Double latitude,
        Double longitude,
        String website,
        String phoneNumber,
        List<String> images,
        Integer reviewCount
    ) {}
}