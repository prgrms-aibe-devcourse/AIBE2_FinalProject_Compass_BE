package com.compass.domain.chat.repository;

import com.compass.domain.chat.entity.TravelInfo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface TravelInfoRepository extends JpaRepository<TravelInfo, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TravelInfo> findByThreadId(String threadId);
}
