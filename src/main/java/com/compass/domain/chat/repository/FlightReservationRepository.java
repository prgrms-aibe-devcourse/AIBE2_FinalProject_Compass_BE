package com.compass.domain.chat.repository;

import com.compass.domain.chat.entity.FlightReservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FlightReservationRepository extends JpaRepository<FlightReservationEntity, Long> {
}
