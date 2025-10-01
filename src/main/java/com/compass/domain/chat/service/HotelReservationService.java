package com.compass.domain.chat.service;

import com.compass.domain.chat.entity.HotelReservationEntity;
import com.compass.domain.chat.model.dto.ConfirmedSchedule;
import com.compass.domain.chat.model.dto.HotelReservation;
import com.compass.domain.chat.repository.HotelReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

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

    // threadId로 호텔 예약 조회
    @Transactional(readOnly = true)
    public List<HotelReservationEntity> findByThreadId(String threadId) {
        var reservations = repository.findByThreadId(threadId);
        log.debug("호텔 예약 조회 완료 - threadId: {}, count: {}", threadId, reservations.size());
        return reservations;
    }

    // DB의 호텔 예약을 ConfirmedSchedule로 변환
    @Transactional(readOnly = true)
    public List<ConfirmedSchedule> findConfirmedSchedulesByThreadId(String threadId) {
        var reservations = repository.findByThreadId(threadId);
        log.info("호텔 예약 {} 개를 ConfirmedSchedule로 변환", reservations.size());

        return reservations.stream()
                .map(this::convertToConfirmedSchedule)
                .collect(Collectors.toList());
    }

    // HotelReservationEntity → ConfirmedSchedule 변환
    private ConfirmedSchedule convertToConfirmedSchedule(HotelReservationEntity entity) {
        // 체크인 시간이 없으면 기본값 15:00 사용
        var checkInTime = entity.getCheckInTime() != null
                ? entity.getCheckInTime()
                : LocalTime.of(15, 0);

        // 체크아웃 시간이 없으면 기본값 11:00 사용
        var checkOutTime = entity.getCheckOutTime() != null
                ? entity.getCheckOutTime()
                : LocalTime.of(11, 0);

        // LocalDate + LocalTime → LocalDateTime
        var checkInDateTime = LocalDateTime.of(entity.getCheckInDate(), checkInTime);
        var checkOutDateTime = LocalDateTime.of(entity.getCheckOutDate(), checkOutTime);

        return ConfirmedSchedule.hotel(
                checkInDateTime,
                checkOutDateTime,
                entity.getHotelName(),
                entity.getAddress(),
                "OCR-" + entity.getConfirmationNumber(),  // originalText로 확인번호 사용
                null  // imageUrl (현재는 없음)
        );
    }

    private HotelReservationEntity mapToEntity(String threadId, String userId, HotelReservation reservation) {
        var entity = new HotelReservationEntity();
        entity.setThreadId(threadId);
        entity.setUserId(userId);
        entity.setHotelName(reservation.hotelName());
        entity.setAddress(reservation.address());
        entity.setCheckInDate(reservation.checkInDate());
        entity.setCheckInTime(reservation.checkInTime());
        entity.setCheckOutDate(reservation.checkOutDate());
        entity.setCheckOutTime(reservation.checkOutTime());
        entity.setRoomType(reservation.roomType());
        entity.setNumberOfGuests(reservation.numberOfGuests());
        entity.setConfirmationNumber(reservation.confirmationNumber());
        entity.setTotalPrice(reservation.totalPrice());
        entity.setNights(reservation.nights());
        entity.setLatitude(reservation.latitude());
        entity.setLongitude(reservation.longitude());
        entity.setGuestName(reservation.guestName());
        entity.setPhone(reservation.phone());
        return entity;
    }
}
