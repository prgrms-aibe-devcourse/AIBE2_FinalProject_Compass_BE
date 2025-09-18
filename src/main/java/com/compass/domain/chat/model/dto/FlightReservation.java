package com.compass.domain.chat.model.dto;

import java.time.LocalDate;

public record FlightReservation(
        String flightNumber,
        String departure,
        String arrival,
        LocalDate departureDate,
        LocalDate arrivalDate,
        String passenger
) {
    public FlightReservation {
        flightNumber = flightNumber == null ? "" : flightNumber;
        departure = departure == null ? "" : departure;
        arrival = arrival == null ? "" : arrival;
        passenger = passenger == null ? "" : passenger;
    }
}
