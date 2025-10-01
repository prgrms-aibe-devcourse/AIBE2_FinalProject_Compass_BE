package com.compass.domain.chat.repository;

import com.compass.domain.chat.entity.HotelReservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HotelReservationRepository extends JpaRepository<HotelReservationEntity, Long> {

    // threadId로 호텔 예약 조회
    List<HotelReservationEntity> findByThreadId(String threadId);

    // userId와 threadId로 호텔 예약 조회
    List<HotelReservationEntity> findByUserIdAndThreadId(String userId, String threadId);
}
