package com.compass.domain.chat.repository;

import com.compass.domain.chat.entity.TravelInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TravelInfoRepository extends JpaRepository<TravelInfo, Long> {
    Optional<TravelInfo> findByThreadId(String threadId);
}