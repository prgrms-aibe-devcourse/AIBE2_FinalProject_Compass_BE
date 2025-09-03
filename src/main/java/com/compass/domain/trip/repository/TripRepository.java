package com.compass.domain.trip.repository;

import com.compass.domain.trip.Trip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long> {
    
    @Query("SELECT t FROM Trip t LEFT JOIN FETCH t.details WHERE t.id = :tripId")
    Optional<Trip> findByIdWithDetails(@Param("tripId") Long tripId);
    
    @Query("SELECT t FROM Trip t WHERE t.user.id = :userId ORDER BY t.createdAt DESC")
    Page<Trip> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);
}
