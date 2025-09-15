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
 * REQ-SEARCH-001: RDS 검색 시스템을 위한 관광지 데이터 관리
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

    // ===== REQ-SEARCH-001: RDS 검색 시스템 =====

    /**
     * PostgreSQL 전문검색 - 관광지명, 주소, 카테고리 통합 검색
     */
    @Query(value = """
        SELECT *, 
               ts_rank(to_tsvector('korean', name || ' ' || COALESCE(address, '') || ' ' || category), 
                       plainto_tsquery('korean', :query)) AS rank
        FROM tour_places 
        WHERE to_tsvector('korean', name || ' ' || COALESCE(address, '') || ' ' || category) 
              @@ plainto_tsquery('korean', :query)
        ORDER BY rank DESC, name
        """, nativeQuery = true)
    List<TourPlace> fullTextSearch(@Param("query") String query);

    /**
     * PostgreSQL 전문검색 - 페이징 지원 (간단한 LIKE 검색으로 대체)
     */
    @Query(value = """
        SELECT * 
        FROM tour_places 
        WHERE name ILIKE CONCAT('%', :query, '%')
           OR address ILIKE CONCAT('%', :query, '%')
           OR category ILIKE CONCAT('%', :query, '%')
        ORDER BY name
        LIMIT :size OFFSET :offset
        """, nativeQuery = true)
    List<TourPlace> fullTextSearchWithPaging(@Param("query") String query, 
                                           @Param("offset") int offset, 
                                           @Param("size") int size);

    /**
     * PostgreSQL 전문검색 - 카운트 (간단한 LIKE 검색으로 대체)
     */
    @Query(value = """
        SELECT COUNT(*)
        FROM tour_places 
        WHERE name ILIKE CONCAT('%', :query, '%')
           OR address ILIKE CONCAT('%', :query, '%')
           OR category ILIKE CONCAT('%', :query, '%')
        """, nativeQuery = true)
    long countFullTextSearch(@Param("query") String query);

    /**
     * 복합 검색 - 전문검색 + 카테고리 필터 (간단한 LIKE 검색으로 대체)
     */
    @Query(value = """
        SELECT * 
        FROM tour_places 
        WHERE (name ILIKE CONCAT('%', :query, '%')
           OR address ILIKE CONCAT('%', :query, '%')
           OR category ILIKE CONCAT('%', :query, '%'))
        AND (:category IS NULL OR category = :category)
        ORDER BY name
        LIMIT :size OFFSET :offset
        """, nativeQuery = true)
    List<TourPlace> fullTextSearchWithCategory(@Param("query") String query,
                                             @Param("category") String category,
                                             @Param("offset") int offset,
                                             @Param("size") int size);

    /**
     * 복합 검색 - 전문검색 + 지역 필터 (간단한 LIKE 검색으로 대체)
     */
    @Query(value = """
        SELECT * 
        FROM tour_places 
        WHERE (name ILIKE CONCAT('%', :query, '%')
           OR address ILIKE CONCAT('%', :query, '%')
           OR category ILIKE CONCAT('%', :query, '%'))
        AND (:areaCode IS NULL OR area_code = :areaCode)
        ORDER BY name
        LIMIT :size OFFSET :offset
        """, nativeQuery = true)
    List<TourPlace> fullTextSearchWithArea(@Param("query") String query,
                                         @Param("areaCode") String areaCode,
                                         @Param("offset") int offset,
                                         @Param("size") int size);

    /**
     * 복합 검색 - 전문검색 + 카테고리 + 지역 필터 (간단한 LIKE 검색으로 대체)
     */
    @Query(value = """
        SELECT * 
        FROM tour_places 
        WHERE (name ILIKE CONCAT('%', :query, '%')
           OR address ILIKE CONCAT('%', :query, '%')
           OR category ILIKE CONCAT('%', :query, '%'))
        AND (:category IS NULL OR category = :category)
        AND (:areaCode IS NULL OR area_code = :areaCode)
        ORDER BY name
        LIMIT :size OFFSET :offset
        """, nativeQuery = true)
    List<TourPlace> fullTextSearchWithFilters(@Param("query") String query,
                                            @Param("category") String category,
                                            @Param("areaCode") String areaCode,
                                            @Param("offset") int offset,
                                            @Param("size") int size);

    /**
     * 근거리 검색 - PostgreSQL earthdistance 확장 활용
     */
    @Query(value = """
        SELECT *, 
               earth_distance(ll_to_earth(latitude, longitude), 
                              ll_to_earth(:lat, :lng)) AS distance
        FROM tour_places 
        WHERE latitude IS NOT NULL AND longitude IS NOT NULL
        AND earth_box(ll_to_earth(:lat, :lng), :radiusKm * 1000) @> ll_to_earth(latitude, longitude)
        ORDER BY distance
        LIMIT :size OFFSET :offset
        """, nativeQuery = true)
    List<TourPlace> findNearbyPlacesWithDistance(@Param("lat") Double latitude,
                                               @Param("lng") Double longitude,
                                               @Param("radiusKm") Double radiusKm,
                                               @Param("offset") int offset,
                                               @Param("size") int size);

    /**
     * 검색 통계 - 인기 카테고리
     */
    @Query("SELECT t.category, COUNT(t) FROM TourPlace t GROUP BY t.category ORDER BY COUNT(t) DESC")
    List<Object[]> getPopularCategories();

    /**
     * 검색 통계 - 지역별 관광지 분포
     */
    @Query("SELECT t.areaCode, COUNT(t) FROM TourPlace t GROUP BY t.areaCode ORDER BY COUNT(t) DESC")
    List<Object[]> getAreaDistribution();

    // ===== SearchService에서 사용하는 추가 메서드들 =====

    /**
     * 카테고리 필터링된 전문검색 결과 개수 (간단한 LIKE 검색으로 대체)
     */
    @Query(value = """
        SELECT COUNT(*)
        FROM tour_places 
        WHERE (name ILIKE CONCAT('%', :query, '%')
           OR address ILIKE CONCAT('%', :query, '%')
           OR category ILIKE CONCAT('%', :query, '%'))
        AND (:category IS NULL OR category = :category)
        """, nativeQuery = true)
    long countFullTextSearchWithCategory(@Param("query") String query, @Param("category") String category);

    /**
     * 지역 필터링된 전문검색 결과 개수 (간단한 LIKE 검색으로 대체)
     */
    @Query(value = """
        SELECT COUNT(*)
        FROM tour_places 
        WHERE (name ILIKE CONCAT('%', :query, '%')
           OR address ILIKE CONCAT('%', :query, '%')
           OR category ILIKE CONCAT('%', :query, '%'))
        AND (:areaCode IS NULL OR area_code = :areaCode)
        """, nativeQuery = true)
    long countFullTextSearchWithArea(@Param("query") String query, @Param("areaCode") String areaCode);

    /**
     * 복합 필터링된 전문검색 결과 개수 (간단한 LIKE 검색으로 대체)
     */
    @Query(value = """
        SELECT COUNT(*)
        FROM tour_places 
        WHERE (name ILIKE CONCAT('%', :query, '%')
           OR address ILIKE CONCAT('%', :query, '%')
           OR category ILIKE CONCAT('%', :query, '%'))
        AND (:category IS NULL OR category = :category)
        AND (:areaCode IS NULL OR area_code = :areaCode)
        """, nativeQuery = true)
    long countFullTextSearchWithFilters(@Param("query") String query, 
                                      @Param("category") String category, 
                                      @Param("areaCode") String areaCode);

    /**
     * 근거리 검색 결과 개수
     */
    @Query(value = """
        SELECT COUNT(*)
        FROM tour_places 
        WHERE latitude IS NOT NULL AND longitude IS NOT NULL
        AND earth_box(ll_to_earth(:lat, :lng), :radiusKm * 1000) @> ll_to_earth(latitude, longitude)
        """, nativeQuery = true)
    long countNearbyPlaces(@Param("lat") Double latitude, 
                         @Param("lng") Double longitude, 
                         @Param("radiusKm") Double radiusKm);
}

