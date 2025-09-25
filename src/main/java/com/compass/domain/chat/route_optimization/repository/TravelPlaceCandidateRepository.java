package com.compass.domain.chat.route_optimization.repository;

import com.compass.domain.chat.route_optimization.entity.TravelPlaceCandidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TravelPlaceCandidateRepository extends JpaRepository<TravelPlaceCandidate, Long> {

    // 일정별 모든 후보 장소 조회
    List<TravelPlaceCandidate> findByItineraryId(Long itineraryId);

    // 일정별 특정 날짜의 후보 장소 조회
    @Query("SELECT tpc FROM TravelPlaceCandidate tpc WHERE tpc.itinerary.id = :itineraryId AND tpc.dayNumber = :dayNumber ORDER BY tpc.matchScore DESC")
    List<TravelPlaceCandidate> findByItineraryIdAndDayNumber(
        @Param("itineraryId") Long itineraryId,
        @Param("dayNumber") Integer dayNumber
    );

    // 매칭 점수 상위 후보 조회
    @Query("SELECT tpc FROM TravelPlaceCandidate tpc WHERE tpc.itinerary.id = :itineraryId ORDER BY tpc.matchScore DESC")
    List<TravelPlaceCandidate> findTopCandidatesByItineraryId(@Param("itineraryId") Long itineraryId);

    // 카테고리별 후보 조회
    @Query("SELECT tpc FROM TravelPlaceCandidate tpc WHERE tpc.itinerary.id = :itineraryId AND tpc.category = :category")
    List<TravelPlaceCandidate> findByItineraryIdAndCategory(
        @Param("itineraryId") Long itineraryId,
        @Param("category") String category
    );

    // 특정 일정의 모든 후보 삭제
    void deleteByItineraryId(Long itineraryId);
}