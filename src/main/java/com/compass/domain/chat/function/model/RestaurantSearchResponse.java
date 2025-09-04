package com.compass.domain.chat.function.model;

import lombok.Builder;

import java.util.List;

/**
 * Response model for restaurant search function
 */
@Builder
public record RestaurantSearchResponse(
    String location,
    List<Restaurant> restaurants
) {
    @Builder
    public record Restaurant(
        String name,
        List<String> cuisineTypes,
        Double rating,
        String priceRange,
        String address,
        String description,
        List<String> specialties,
        List<String> dietaryOptions,
        List<String> features,
        String atmosphere,
        String openingHours,
        String closingTime,
        Boolean reservationRequired,
        Boolean reservationAvailable,
        Integer averagePrice,
        String currency,
        Integer michelinStars,
        List<String> awards,
        String chefName,
        List<String> images,
        Double distance,
        String distanceUnit,
        Integer reviewCount,
        String phoneNumber,
        String website,
        Integer waitTime,
        String dressCode
    ) {}
}