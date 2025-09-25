package com.compass.domain.chat.repository;

import com.compass.domain.chat.entity.TourPlace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TourPlaceRepository extends JpaRepository<TourPlace, Long> {
    
    // 스레드별 장소 조회
    List<TourPlace> findByThreadId(String threadId);
    
    // 스레드별 일차별 장소 조회
    List<TourPlace> findByThreadIdAndDay(String threadId, Integer day);
    
    // 스레드별 시간대별 장소 조회
    List<TourPlace> findByThreadIdAndTimeBlock(String threadId, String timeBlock);
    
    // 스레드별 클러스터별 장소 조회
    List<TourPlace> findByThreadIdAndClusterName(String threadId, String clusterName);
    
    // 스레드별 완전한 정보를 가진 장소만 조회
    @Query("SELECT tp FROM TourPlace tp WHERE tp.threadId = :threadId AND tp.name IS NOT NULL AND tp.address IS NOT NULL AND tp.latitude IS NOT NULL AND tp.longitude IS NOT NULL")
    List<TourPlace> findCompletePlacesByThreadId(@Param("threadId") String threadId);
    
    // 스레드별 매칭 점수 높은 순으로 조회
    @Query("SELECT tp FROM TourPlace tp WHERE tp.threadId = :threadId ORDER BY tp.matchScore DESC")
    List<TourPlace> findByThreadIdOrderByMatchScoreDesc(@Param("threadId") String threadId);
    
    // 클러스터별 통계 조회
    @Query("SELECT tp.clusterName, COUNT(tp) FROM TourPlace tp WHERE tp.threadId = :threadId GROUP BY tp.clusterName")
    List<Object[]> getClusterDistributionByThreadId(@Param("threadId") String threadId);
    
    // 외부 ID로 중복 확인
    Optional<TourPlace> findByExternalIdAndThreadId(String externalId, String threadId);
    
    // 스레드별 장소 수 조회
    @Query("SELECT COUNT(tp) FROM TourPlace tp WHERE tp.threadId = :threadId")
    Long countByThreadId(@Param("threadId") String threadId);
    
    // 스레드별 일차별 장소 수 조회
    @Query("SELECT COUNT(tp) FROM TourPlace tp WHERE tp.threadId = :threadId AND tp.day = :day")
    Long countByThreadIdAndDay(@Param("threadId") String threadId, @Param("day") Integer day);
}


