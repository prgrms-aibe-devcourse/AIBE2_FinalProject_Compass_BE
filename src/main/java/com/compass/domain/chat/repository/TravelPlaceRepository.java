package com.compass.domain.chat.repository;

import com.compass.domain.chat.entity.TravelPlaceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 여행 장소 정보 Repository
 */
@Repository
public interface TravelPlaceRepository extends JpaRepository<TravelPlaceEntity, Long> {

    /**
     * 스레드 ID로 여행 장소 조회
     */
    List<TravelPlaceEntity> findByThreadIdOrderByCreatedAtDesc(String threadId);

    /**
     * 사용자 ID로 여행 장소 조회
     */
    List<TravelPlaceEntity> findByUserIdOrderByCreatedAtDesc(String userId);

    /**
     * 목적지별 여행 장소 조회
     */
    List<TravelPlaceEntity> findByDestinationOrderByRatingDesc(String destination);

    /**
     * 스레드 ID와 목적지로 여행 장소 조회
     */
    List<TravelPlaceEntity> findByThreadIdAndDestinationOrderByCreatedAtDesc(String threadId, String destination);

    /**
     * 스레드 ID로 여행 장소 삭제
     */
    void deleteByThreadId(String threadId);

    /**
     * 스레드 ID로 여행 장소 개수 조회
     */
    long countByThreadId(String threadId);

    /**
     * 카테고리별 장소 통계 조회
     */
    @Query("SELECT tp.category, COUNT(tp) FROM TravelPlaceEntity tp WHERE tp.threadId = :threadId GROUP BY tp.category")
    List<Object[]> findCategoryStatsByThreadId(@Param("threadId") String threadId);

    /**
     * 평점 평균 조회
     */
    @Query("SELECT AVG(tp.rating) FROM TravelPlaceEntity tp WHERE tp.threadId = :threadId AND tp.rating IS NOT NULL")
    Double findAverageRatingByThreadId(@Param("threadId") String threadId);
}

