package com.compass.domain.chat.function.model;

import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

/**
 * Response model for hotel search function
 */
@Builder
public record HotelSearchResponse(
    String location,
    LocalDate checkIn,
    LocalDate checkOut,
    List<Hotel> hotels
) {
    @Builder
    public record Hotel(
        String name,
        Double rating,
        Integer pricePerNight,
        String currency,
        List<String> amenities,
        String address,
        String description,
        Double distance,
        String distanceUnit,
        List<String> images,
        Boolean breakfastIncluded,
        Boolean freeCancellation
    ) {}
}