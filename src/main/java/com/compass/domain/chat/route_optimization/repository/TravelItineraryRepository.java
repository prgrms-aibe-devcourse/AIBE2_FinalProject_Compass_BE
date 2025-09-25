package com.compass.domain.chat.route_optimization.repository;

import com.compass.domain.chat.route_optimization.entity.TravelItinerary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TravelItineraryRepository extends JpaRepository<TravelItinerary, Long> {

    // 세션 ID로 활성 일정 조회
    Optional<TravelItinerary> findBySessionIdAndIsActiveTrue(Long sessionId);

    // 스레드 ID로 모든 일정 조회
    @Query("SELECT ti FROM TravelItinerary ti WHERE ti.thread.id = :threadId ORDER BY ti.createdAt DESC")
    List<TravelItinerary> findByThreadId(@Param("threadId") String threadId);

    // 스레드 ID로 최신 활성 일정 조회
    @Query("SELECT ti FROM TravelItinerary ti WHERE ti.thread.id = :threadId AND ti.isActive = true ORDER BY ti.createdAt DESC")
    Optional<TravelItinerary> findLatestActiveByThreadId(@Param("threadId") String threadId);

    // 날짜 범위로 일정 검색
    @Query("SELECT ti FROM TravelItinerary ti WHERE ti.startDate <= :endDate AND ti.endDate >= :startDate AND ti.isActive = true")
    List<TravelItinerary> findByDateRange(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    // 확정된 일정 조회
    List<TravelItinerary> findByIsFinalTrueAndIsActiveTrue();

    // 세션별 일정 히스토리
    @Query("SELECT ti FROM TravelItinerary ti WHERE ti.sessionId = :sessionId ORDER BY ti.createdAt DESC")
    List<TravelItinerary> findHistoryBySessionId(@Param("sessionId") Long sessionId);
}