package com.compass.domain.chat.model.dto;

import java.time.LocalDateTime;

public record FlightReservation(
        String flightNumber,
        String departure,
        String arrival,
        LocalDateTime departureDateTime,
        LocalDateTime arrivalDateTime,
        String passenger,
        String seatNumber,
        String bookingReference
) {
    public FlightReservation {
        flightNumber = flightNumber == null ? "" : flightNumber;
        departure = departure == null ? "" : departure;
        arrival = arrival == null ? "" : arrival;
        passenger = passenger == null ? "" : passenger;
        seatNumber = seatNumber == null ? "" : seatNumber;
        bookingReference = bookingReference == null ? "" : bookingReference;
    }
}
