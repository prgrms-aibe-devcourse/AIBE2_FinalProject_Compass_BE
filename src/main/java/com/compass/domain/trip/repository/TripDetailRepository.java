package com.compass.domain.trip.repository;

import com.compass.domain.trip.TripDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripDetailRepository extends JpaRepository<TripDetail, Long> {
}
