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
        validateReservation(reservation);
        var entity = mapToEntity(threadId, userId, reservation);
        var saved = repository.save(entity);
        log.debug("호텔 예약 저장 완료 - id: {}", saved.getId());
        return saved;
    }

    private void validateReservation(HotelReservation reservation) {
        if (reservation == null) {
            throw new IllegalArgumentException("예약 정보가 존재하지 않습니다.");
        }
        var checkIn = reservation.checkInDate();
        if (checkIn == null) {
            throw new IllegalArgumentException("체크인 날짜는 필수입니다.");
        }
        var checkOut = reservation.checkOutDate();
        if (checkOut == null) {
            throw new IllegalArgumentException("체크아웃 날짜는 필수입니다.");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("체크아웃 날짜는 체크인 날짜 이후여야 합니다.");
        }
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
