package com.compass.domain.chat.function.model;

import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

/**
 * Response model for flight search function
 */
@Builder
public record FlightSearchResponse(
    String origin,
    String destination,
    LocalDate departureDate,
    List<Flight> flights
) {
    @Builder
    public record Flight(
        String airline,
        String flightNumber,
        String departureTime,
        String arrivalTime,
        Integer price,
        String currency,
        String aircraft,
        Integer availableSeats,
        List<String> amenities
    ) {}
}