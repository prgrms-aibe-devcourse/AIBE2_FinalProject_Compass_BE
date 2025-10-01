package com.compass.domain.chat.model.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record HotelReservation(
        String hotelName,
        String address,
        LocalDate checkInDate,
        LocalTime checkInTime,
        LocalDate checkOutDate,
        LocalTime checkOutTime,
        String roomType,
        Integer numberOfGuests,
        String confirmationNumber,
        Double totalPrice,
        Integer nights,
        Double latitude,
        Double longitude,
        String guestName,
        String phone
) {
    public HotelReservation {
        hotelName = hotelName == null ? "" : hotelName;
        address = address == null ? "" : address;
        roomType = roomType == null ? "" : roomType;
        confirmationNumber = confirmationNumber == null ? "" : confirmationNumber;
        guestName = guestName == null ? "" : guestName;
        phone = phone == null ? "" : phone;
    }
}
