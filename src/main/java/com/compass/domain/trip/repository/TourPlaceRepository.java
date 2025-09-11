package com.compass.domain.trip.repository;

import com.compass.domain.trip.entity.TourPlace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 관광지 데이터 저장소
 * REQ-CRAWL-002: Phase별 크롤링에서 수집된 데이터 관리
 */
@Repository
public interface TourPlaceRepository extends JpaRepository<TourPlace, Long> {

    /**
     * Content ID로 관광지 조회
     */
    Optional<TourPlace> findByContentId(String contentId);

    /**
     * 지역 코드로 관광지 목록 조회
     */
    List<TourPlace> findByAreaCode(String areaCode);

    /**
     * 카테고리로 관광지 목록 조회
     */
    List<TourPlace> findByCategory(String category);

    /**
     * 컨텐츠 타입 ID로 관광지 목록 조회
     */
    List<TourPlace> findByContentTypeId(String contentTypeId);

    /**
     * 지역 코드와 컨텐츠 타입 ID로 관광지 목록 조회
     */
    List<TourPlace> findByAreaCodeAndContentTypeId(String areaCode, String contentTypeId);

    /**
     * 지역 코드와 카테고리로 관광지 목록 조회
     */
    List<TourPlace> findByAreaCodeAndCategory(String areaCode, String category);

    /**
     * 이름으로 관광지 검색 (LIKE 검색)
     */
    @Query(value = "SELECT * FROM tour_places WHERE name ILIKE CONCAT('%', :name, '%')", 
           nativeQuery = true)
    List<TourPlace> searchByName(@Param("name") String name);

    /**
     * 키워드로 관광지 검색 (JSONB 검색)
     */
    @Query(value = "SELECT * FROM tour_places WHERE keywords @> CAST(:keyword AS jsonb)", 
           nativeQuery = true)
    List<TourPlace> findByKeyword(@Param("keyword") String keyword);

    /**
     * 근처 관광지 검색 (거리 기반)
     */
    @Query(value = """
        SELECT *,
               (6371 * acos(cos(radians(:lat)) * cos(radians(latitude))
               * cos(radians(longitude) - radians(:lng))
               + sin(radians(:lat)) * sin(radians(latitude)))) AS distance
        FROM tour_places
        WHERE latitude IS NOT NULL AND longitude IS NOT NULL
        AND (6371 * acos(cos(radians(:lat)) * cos(radians(latitude))
             * cos(radians(longitude) - radians(:lng))
             + sin(radians(:lat)) * sin(radians(latitude)))) < :radiusKm
        ORDER BY distance
        """, nativeQuery = true)
    List<TourPlace> findNearbyPlaces(@Param("lat") Double latitude,
                                   @Param("lng") Double longitude,
                                   @Param("radiusKm") Double radiusKm);

    /**
     * 지역별 관광지 개수 조회
     */
    @Query("SELECT t.areaCode, COUNT(t) FROM TourPlace t GROUP BY t.areaCode")
    List<Object[]> countByAreaCode();

    /**
     * 카테고리별 관광지 개수 조회
     */
    @Query("SELECT t.category, COUNT(t) FROM TourPlace t GROUP BY t.category")
    List<Object[]> countByCategory();

    /**
     * 컨텐츠 타입별 관광지 개수 조회
     */
    @Query("SELECT t.contentTypeId, COUNT(t) FROM TourPlace t GROUP BY t.contentTypeId")
    List<Object[]> countByContentTypeId();

    /**
     * 데이터 소스별 관광지 개수 조회
     */
    @Query("SELECT t.dataSource, COUNT(t) FROM TourPlace t GROUP BY t.dataSource")
    List<Object[]> countByDataSource();

    /**
     * 최근 크롤링된 관광지 조회
     */
    @Query("SELECT t FROM TourPlace t WHERE t.crawledAt IS NOT NULL ORDER BY t.crawledAt DESC")
    List<TourPlace> findRecentlyCrawled();

    /**
     * 특정 지역의 최근 크롤링된 관광지 조회
     */
    @Query("SELECT t FROM TourPlace t WHERE t.areaCode = :areaCode AND t.crawledAt IS NOT NULL ORDER BY t.crawledAt DESC")
    List<TourPlace> findRecentlyCrawledByAreaCode(@Param("areaCode") String areaCode);
}

