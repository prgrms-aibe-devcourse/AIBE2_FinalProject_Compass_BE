package com.compass.domain.chat.model.dto;

import java.time.LocalDate;

public record HotelReservation(
        String hotelName,
        String address,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        String roomType,
        Integer numberOfGuests,
        String confirmationNumber,
        Double totalPrice,
        Double latitude,
        Double longitude
) {
    public HotelReservation {
        hotelName = hotelName == null ? "" : hotelName;
        address = address == null ? "" : address;
        roomType = roomType == null ? "" : roomType;
        confirmationNumber = confirmationNumber == null ? "" : confirmationNumber;
    }
}
