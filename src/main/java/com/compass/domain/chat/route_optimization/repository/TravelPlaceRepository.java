package com.compass.domain.chat.route_optimization.repository;

import com.compass.domain.chat.route_optimization.entity.TravelPlace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TravelPlaceRepository extends JpaRepository<TravelPlace, Long> {

    // 일정별 선택된 장소 조회 (방문 순서대로)
    @Query("SELECT tp FROM TravelPlace tp WHERE tp.itinerary.id = :itineraryId AND tp.isSelected = true ORDER BY tp.dayNumber, tp.visitOrder")
    List<TravelPlace> findSelectedPlacesByItineraryId(@Param("itineraryId") Long itineraryId);

    // 일정별 특정 날짜의 장소 조회
    @Query("SELECT tp FROM TravelPlace tp WHERE tp.itinerary.id = :itineraryId AND tp.dayNumber = :dayNumber ORDER BY tp.visitOrder")
    List<TravelPlace> findByItineraryIdAndDayNumber(
        @Param("itineraryId") Long itineraryId,
        @Param("dayNumber") Integer dayNumber
    );

    // OCR 확정 일정만 조회
    @Query("SELECT tp FROM TravelPlace tp WHERE tp.itinerary.id = :itineraryId AND tp.isFixed = true ORDER BY tp.dayNumber, tp.visitOrder")
    List<TravelPlace> findFixedPlacesByItineraryId(@Param("itineraryId") Long itineraryId);

    // 카테고리별 장소 조회
    @Query("SELECT tp FROM TravelPlace tp WHERE tp.itinerary.id = :itineraryId AND tp.category = :category")
    List<TravelPlace> findByItineraryIdAndCategory(
        @Param("itineraryId") Long itineraryId,
        @Param("category") String category
    );

    // 시간대별 장소 조회
    @Query("SELECT tp FROM TravelPlace tp WHERE tp.itinerary.id = :itineraryId AND tp.timeBlock = :timeBlock ORDER BY tp.dayNumber")
    List<TravelPlace> findByItineraryIdAndTimeBlock(
        @Param("itineraryId") Long itineraryId,
        @Param("timeBlock") String timeBlock
    );

    // 특정 일정의 모든 장소 삭제
    void deleteByItineraryId(Long itineraryId);
}