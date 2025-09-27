package com.compass.domain.chat.repository;

import com.compass.domain.chat.entity.TravelCandidate;
import com.compass.domain.chat.entity.TravelCandidate.TimeBlock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 여행 후보지 Repository
 * Pre-Stage에서 수집된 여행 후보지 정보 관리
 */
@Repository
public interface TravelCandidateRepository extends JpaRepository<TravelCandidate, Long>, TravelCandidateQueryRepository {

    // Place ID로 조회
    Optional<TravelCandidate> findByPlaceIdAndRegion(String placeId, String region);

    Optional<TravelCandidate> findFirstByPlaceId(String placeId);

    // 지역별 모든 데이터 조회
    List<TravelCandidate> findByRegion(String region);

    // 지역별 조회 (품질점수 높은 순)
    @Query("SELECT tc FROM TravelCandidate tc WHERE tc.region = :region AND tc.isActive = true " +
           "ORDER BY tc.qualityScore DESC")
    List<TravelCandidate> findByRegionOrderByQualityScore(@Param("region") String region);

    // 지역 + 시간블럭별 조회 (리뷰 많은 순)
    @Query("SELECT tc FROM TravelCandidate tc WHERE tc.region = :region " +
           "AND tc.timeBlock = :timeBlock AND tc.isActive = true " +
           "ORDER BY tc.reviewCount DESC, tc.rating DESC")
    List<TravelCandidate> findByRegionAndTimeBlock(@Param("region") String region,
                                               @Param("timeBlock") TimeBlock timeBlock);

    // 지역 + 카테고리별 조회 (인기순)
    @Query("SELECT tc FROM TravelCandidate tc WHERE tc.region = :region " +
           "AND tc.category = :category AND tc.isActive = true " +
           "ORDER BY tc.reviewCount DESC, tc.rating DESC")
    List<TravelCandidate> findByRegionAndCategory(@Param("region") String region,
                                              @Param("category") String category);

    // 지역 + 최소 평점 이상 조회
    @Query("SELECT tc FROM TravelCandidate tc WHERE tc.region = :region " +
           "AND tc.rating >= :minRating AND tc.isActive = true " +
           "ORDER BY tc.reviewCount DESC")
    List<TravelCandidate> findByRegionAndMinRating(@Param("region") String region,
                                               @Param("minRating") Double minRating);

    // 지역 + 신뢰도 레벨별 조회
    @Query("SELECT tc FROM TravelCandidate tc WHERE tc.region = :region " +
           "AND tc.reliabilityLevel = :level AND tc.isActive = true " +
           "ORDER BY tc.qualityScore DESC")
    List<TravelCandidate> findByRegionAndReliabilityLevel(@Param("region") String region,
                                                      @Param("level") String level);

    // 지역 + 가격대별 조회
    @Query("SELECT tc FROM TravelCandidate tc WHERE tc.region = :region " +
           "AND tc.priceLevel <= :maxPriceLevel AND tc.priceLevel >= 0 " +
           "AND tc.isActive = true ORDER BY tc.qualityScore DESC")
    List<TravelCandidate> findByRegionAndPriceLevel(@Param("region") String region,
                                                @Param("maxPriceLevel") Integer maxPriceLevel);

    // 복합 조건 조회 (지역 + 시간블럭 + 최소평점)
    @Query("SELECT tc FROM TravelCandidate tc WHERE tc.region = :region " +
           "AND tc.timeBlock = :timeBlock AND tc.rating >= :minRating " +
           "AND tc.isActive = true ORDER BY tc.reviewCount DESC, tc.rating DESC")
    List<TravelCandidate> findByRegionTimeBlockAndMinRating(@Param("region") String region,
                                                        @Param("timeBlock") TimeBlock timeBlock,
                                                        @Param("minRating") Double minRating);

    // 현재 영업중인 장소만 조회
    @Query("SELECT tc FROM TravelCandidate tc WHERE tc.region = :region " +
           "AND tc.openNow = true AND tc.isActive = true " +
           "ORDER BY tc.qualityScore DESC")
    List<TravelCandidate> findOpenPlacesByRegion(@Param("region") String region);

    // 페이징 처리된 지역별 조회
    Page<TravelCandidate> findByRegionAndIsActiveOrderByQualityScoreDesc(String region,
                                                                     Boolean isActive,
                                                                     Pageable pageable);

    // 수집 시간 기준 오래된 데이터 조회
    @Query("SELECT tc FROM TravelCandidate tc WHERE tc.collectedAt < :beforeDate " +
           "AND tc.isActive = true")
    List<TravelCandidate> findOldData(@Param("beforeDate") LocalDateTime beforeDate);

    // 중복 체크
    boolean existsByPlaceIdAndRegion(String placeId, String region);

    // Soft Delete
    @Modifying
    @Query("UPDATE TravelCandidate tc SET tc.isActive = false WHERE tc.id = :id")
    void softDelete(@Param("id") Long id);

    // 지역별 카테고리 통계
    @Query("SELECT tc.category, COUNT(tc) FROM TravelCandidate tc " +
           "WHERE tc.region = :region AND tc.isActive = true " +
           "GROUP BY tc.category ORDER BY COUNT(tc) DESC")
    List<Object[]> getCategoryStatsByRegion(@Param("region") String region);

    // 지역별 시간블럭 통계
    @Query("SELECT tc.timeBlock, COUNT(tc) FROM TravelCandidate tc " +
           "WHERE tc.region = :region AND tc.isActive = true " +
           "GROUP BY tc.timeBlock ORDER BY tc.timeBlock")
    List<Object[]> getTimeBlockStatsByRegion(@Param("region") String region);

    // 지역별 평균 평점 및 리뷰
    @Query("SELECT AVG(tc.rating), AVG(tc.reviewCount) FROM TravelCandidate tc " +
           "WHERE tc.region = :region AND tc.isActive = true")
    Object[] getAverageStatsForRegion(@Param("region") String region);

    // 상위 N개 인기 장소 조회
    @Query(value = "SELECT * FROM travel_candidates WHERE region = :region " +
                   "AND is_active = true ORDER BY review_count DESC, rating DESC " +
                   "LIMIT :limit",
           nativeQuery = true)
    List<TravelCandidate> findTopPopularPlaces(@Param("region") String region,
                                           @Param("limit") int limit);

    // 배치 저장을 위한 중복 제거 조회
    @Query("SELECT tc.placeId FROM TravelCandidate tc WHERE tc.region = :region")
    List<String> findExistingPlaceIds(@Param("region") String region);

    // 모든 데이터 삭제 (CSV 임포트 전 사용)
    @Modifying
    @Query("DELETE FROM TravelCandidate")
    void deleteAllData();

    // 주소가 있는 데이터 카운트
    @Query("SELECT COUNT(tc) FROM TravelCandidate tc WHERE tc.address IS NOT NULL AND tc.address != ''")
    long countByAddressIsNotNull();

    // 주소가 없는 데이터 조회
    @Query("SELECT tc FROM TravelCandidate tc WHERE (tc.address IS NULL OR tc.address = '') AND tc.isActive = true")
    List<TravelCandidate> findCandidatesWithoutAddress();

    // 특정 지역의 주소가 없는 데이터 조회
    @Query("SELECT tc FROM TravelCandidate tc WHERE tc.region = :region AND (tc.address IS NULL OR tc.address = '') AND tc.isActive = true")
    List<TravelCandidate> findCandidatesWithoutAddressByRegion(@Param("region") String region);

    // 주요 필드가 비어 있는 후보 조회 (Google/Gemini 보강 대상)
    @Query("SELECT tc FROM TravelCandidate tc WHERE tc.isActive = true AND (" +
           "tc.address IS NULL OR tc.address = '' OR " +
           "tc.phoneNumber IS NULL OR tc.phoneNumber = '' OR " +
           "tc.website IS NULL OR tc.website = '' OR " +
           "tc.photoUrl IS NULL OR tc.photoUrl = '' OR " +
           "tc.businessHours IS NULL OR tc.businessHours = '' OR " +
           "tc.googleTypes IS NULL OR tc.googleTypes = '' OR " +
           "tc.rating IS NULL OR tc.reviewCount IS NULL OR " +
           "tc.description IS NULL OR tc.description = '')")
    List<TravelCandidate> findCandidatesNeedingEnrichment();

    // Google Place ID가 존재하는 활성 데이터 조회
    List<TravelCandidate> findByGooglePlaceIdIsNotNullAndIsActiveTrue();

    // 보강 관련 통계 메서드들
    long countByLatitudeIsNotNullAndLongitudeIsNotNull();
    long countByRatingIsNotNull();
    long countByPhotoUrlIsNotNull();
    long countByCategoryIsNotNull();
    long countByPhoneNumberIsNotNull();
    long countByCategoryContaining(String keyword);
    long countByParkingAvailableIsNotNull();
    long countByWifiAvailableIsNotNullOrPetFriendlyIsNotNull();
    long countByAiEnrichedTrue();
    long countByDescriptionIsNotNull();
    long countByTipsIsNotNull();

    // 상위 평점 장소 조회
    @Query("SELECT tc FROM TravelCandidate tc WHERE tc.rating IS NOT NULL " +
           "ORDER BY tc.rating DESC, tc.reviewCount DESC")
    List<TravelCandidate> findTopRatedPlaces(Pageable pageable);

    // Stage 3에서 사용할 메서드들
    List<TravelCandidate> findByRegionAndIsActiveTrue(String region);

    List<TravelCandidate> findByRegionAndCategoryAndIsActiveTrue(String region, String category);

    List<TravelCandidate> findByRegionAndTimeBlockAndIsActiveTrue(String region, TimeBlock timeBlock);
}
