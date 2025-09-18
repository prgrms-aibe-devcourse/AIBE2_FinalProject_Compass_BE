package com.compass.domain.chat.service;

import com.compass.domain.chat.entity.FlightReservationEntity;
import com.compass.domain.chat.model.dto.FlightReservation;
import com.compass.domain.chat.repository.FlightReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlightReservationService {

    private final FlightReservationRepository repository;

    @Transactional
    public FlightReservationEntity save(String threadId, String userId, FlightReservation reservation) {
        var entity = mapToEntity(threadId, userId, reservation);
        var saved = repository.save(entity);
        log.debug("항공권 예약 저장 완료 - id: {}", saved.getId());
        return saved;
    }

    private FlightReservationEntity mapToEntity(String threadId, String userId, FlightReservation reservation) {
        var entity = new FlightReservationEntity();
        entity.setThreadId(threadId);
        entity.setUserId(userId);
        entity.setFlightNumber(reservation.flightNumber());
        entity.setDepartureAirport(reservation.departure());
        entity.setArrivalAirport(reservation.arrival());
        entity.setDepartureDateTime(reservation.departureDateTime());
        entity.setArrivalDateTime(reservation.arrivalDateTime());
        entity.setPassengerName(reservation.passenger());
        entity.setSeatNumber(reservation.seatNumber());
        entity.setBookingReference(reservation.bookingReference());
        return entity;
    }
}
