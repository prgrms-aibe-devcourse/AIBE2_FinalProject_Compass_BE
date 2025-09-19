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
        validateReservation(threadId, userId, reservation);
        var entity = mapToEntity(threadId, userId, reservation);
        var saved = repository.save(entity);
        log.debug("항공권 예약 저장 완료 - id: {}", saved.getId());
        return saved;
    }

    private void validateReservation(String threadId, String userId, FlightReservation reservation) {
        if (threadId == null || threadId.isBlank()) {
            throw new IllegalArgumentException("threadId는 필수입니다.");
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId는 필수입니다.");
        }
        if (reservation == null) {
            throw new IllegalArgumentException("예약 정보가 존재하지 않습니다.");
        }
        if (reservation.flightNumber().isBlank()) {
            throw new IllegalArgumentException("항공편 번호는 필수입니다.");
        }
        if (reservation.departure().isBlank()) {
            throw new IllegalArgumentException("출발 공항은 필수입니다.");
        }
        if (reservation.arrival().isBlank()) {
            throw new IllegalArgumentException("도착 공항은 필수입니다.");
        }
        if (reservation.passenger().isBlank()) {
            throw new IllegalArgumentException("승객명은 필수입니다.");
        }
        if (reservation.seatNumber().isBlank()) {
            throw new IllegalArgumentException("좌석 번호는 필수입니다.");
        }
        if (reservation.bookingReference().isBlank()) {
            throw new IllegalArgumentException("예약 번호는 필수입니다.");
        }
        if (reservation.departureDateTime() == null) {
            throw new IllegalArgumentException("출발 시간이 필요합니다.");
        }
        if (reservation.arrivalDateTime() == null) {
            throw new IllegalArgumentException("도착 시간이 필요합니다.");
        }
    }

    private FlightReservationEntity mapToEntity(String threadId, String userId, FlightReservation reservation) {
        var entity = new FlightReservationEntity();
        entity.setThreadId(threadId);
        entity.setUserId(userId);
        entity.setFlightNumber(reservation.flightNumber());
        entity.setDepartureAirport(reservation.departure());
        entity.setArrivalAirport(reservation.arrival());
        entity.setDepartureTime(reservation.departureDateTime());
        entity.setArrivalTime(reservation.arrivalDateTime());
        entity.setPassengerName(reservation.passenger());
        entity.setSeatNumber(reservation.seatNumber());
        entity.setBookingReference(reservation.bookingReference());
        return entity;
    }
}
