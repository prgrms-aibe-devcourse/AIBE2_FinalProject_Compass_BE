package com.compass.domain.chat.service;

import com.compass.domain.chat.entity.HotelReservationEntity;
import com.compass.domain.chat.model.dto.HotelReservation;
import com.compass.domain.chat.repository.HotelReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotelReservationService {

    private final HotelReservationRepository repository;

    @Transactional
    public HotelReservationEntity save(String threadId, String userId, HotelReservation reservation) {
        var entity = mapToEntity(threadId, userId, reservation);
        var saved = repository.save(entity);
        log.debug("호텔 예약 저장 완료 - id: {}", saved.getId());
        return saved;
    }

    private HotelReservationEntity mapToEntity(String threadId, String userId, HotelReservation reservation) {
        var entity = new HotelReservationEntity();
        entity.setThreadId(threadId);
        entity.setUserId(userId);
        entity.setHotelName(reservation.hotelName());
        entity.setAddress(reservation.address());
        entity.setCheckInDate(reservation.checkInDate());
        entity.setCheckOutDate(reservation.checkOutDate());
        entity.setRoomType(reservation.roomType());
        entity.setNumberOfGuests(reservation.numberOfGuests());
        entity.setConfirmationNumber(reservation.confirmationNumber());
        entity.setTotalPrice(reservation.totalPrice());
        entity.setNights(reservation.nights());
        entity.setLatitude(reservation.latitude());
        entity.setLongitude(reservation.longitude());
        return entity;
    }
}
