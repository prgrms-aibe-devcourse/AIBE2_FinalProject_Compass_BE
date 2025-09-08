package com.compass.domain.trip.repository;

import com.compass.domain.trip.entity.TravelHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * TravelHistory Repository
 * REQ-LLM-004: 여행 히스토리 데이터 접근
 */
@Repository
public interface TravelHistoryRepository extends JpaRepository<TravelHistory, Long> {
    
    /**
     * 사용자의 여행 히스토리 페이징 조회
     */
    Page<TravelHistory> findByUserId(Long userId, Pageable pageable);
    
    /**
     * 사용자의 최근 여행 히스토리 조회
     */
    List<TravelHistory> findTop5ByUserIdOrderByStartDateDesc(Long userId);
    
    /**
     * 특정 목적지로의 여행 히스토리 조회
     */
    List<TravelHistory> findByUserIdAndDestination(Long userId, String destination);
    
    /**
     * 날짜 범위 내 여행 히스토리 조회
     */
    @Query("SELECT th FROM TravelHistory th WHERE th.userId = :userId " +
           "AND th.startDate >= :startDate AND th.endDate <= :endDate " +
           "ORDER BY th.startDate DESC")
    List<TravelHistory> findByUserIdAndDateRange(@Param("userId") Long userId,
                                                  @Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate);
    
    /**
     * 여행 유형별 히스토리 조회
     */
    List<TravelHistory> findByUserIdAndTravelType(Long userId, String travelType);
    
    /**
     * 평점이 높은 여행 조회
     */
    @Query("SELECT th FROM TravelHistory th WHERE th.userId = :userId AND th.rating >= :minRating " +
           "ORDER BY th.rating DESC, th.startDate DESC")
    List<TravelHistory> findHighRatedTrips(@Param("userId") Long userId, @Param("minRating") Integer minRating);
    
    /**
     * AI 계획을 사용한 여행 조회
     */
    List<TravelHistory> findByUserIdAndUsedAiPlanTrue(Long userId);
    
    /**
     * 여행 스타일별 히스토리 조회
     */
    List<TravelHistory> findByUserIdAndTravelStyle(Long userId, String travelStyle);
    
    /**
     * 계절별 여행 히스토리 조회
     */
    List<TravelHistory> findByUserIdAndSeason(Long userId, String season);
    
    /**
     * 사용자의 평균 여행 예산 계산
     */
    @Query("SELECT AVG(th.actualExpense) FROM TravelHistory th WHERE th.userId = :userId AND th.actualExpense > 0")
    Optional<Double> getAverageExpenseByUserId(@Param("userId") Long userId);
    
    /**
     * 사용자의 평균 여행 기간 계산
     */
    @Query("SELECT AVG(CAST(th.endDate - th.startDate AS integer) + 1) FROM TravelHistory th WHERE th.userId = :userId")
    Optional<Double> getAverageTripDurationByUserId(@Param("userId") Long userId);
    
    /**
     * 사용자의 선호 여행 유형 통계
     */
    @Query("SELECT th.travelType, COUNT(th) FROM TravelHistory th " +
           "WHERE th.userId = :userId GROUP BY th.travelType ORDER BY COUNT(th) DESC")
    List<Object[]> getTravelTypeStatsByUserId(@Param("userId") Long userId);
    
    /**
     * 사용자의 선호 목적지 통계
     */
    @Query("SELECT th.destination, COUNT(th), AVG(th.rating) FROM TravelHistory th " +
           "WHERE th.userId = :userId GROUP BY th.destination " +
           "HAVING COUNT(th) > 0 ORDER BY COUNT(th) DESC, AVG(th.rating) DESC")
    List<Object[]> getDestinationStatsByUserId(@Param("userId") Long userId);
    
    /**
     * 특정 연도의 여행 히스토리 조회
     */
    @Query("SELECT th FROM TravelHistory th WHERE th.userId = :userId " +
           "AND EXTRACT(YEAR FROM th.startDate) = :year ORDER BY th.startDate DESC")
    List<TravelHistory> findByUserIdAndYear(@Param("userId") Long userId, @Param("year") Integer year);
    
    /**
     * 사용자의 총 여행 횟수
     */
    Long countByUserId(Long userId);
    
    /**
     * AI 만족도가 높은 여행 조회
     */
    @Query("SELECT th FROM TravelHistory th WHERE th.userId = :userId " +
           "AND th.aiSatisfaction >= :minSatisfaction ORDER BY th.aiSatisfaction DESC")
    List<TravelHistory> findHighAiSatisfactionTrips(@Param("userId") Long userId, 
                                                    @Param("minSatisfaction") Integer minSatisfaction);
}