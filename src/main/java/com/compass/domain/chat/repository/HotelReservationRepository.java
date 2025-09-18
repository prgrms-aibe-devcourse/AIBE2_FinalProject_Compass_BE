package com.compass.domain.chat.repository;

import com.compass.domain.chat.entity.HotelReservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HotelReservationRepository extends JpaRepository<HotelReservationEntity, Long> {
}
