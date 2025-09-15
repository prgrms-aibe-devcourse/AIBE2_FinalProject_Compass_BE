package com.compass.domain.trip.repository;

import com.compass.domain.trip.entity.CrawlStatus;
import com.compass.domain.trip.enums.CrawlStatusType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 크롤링 상태 저장소
 * REQ-CRAWL-002: Phase별 크롤링 진행 상황 관리
 */
@Repository
public interface CrawlStatusRepository extends JpaRepository<CrawlStatus, Long> {

    /**
     * 지역 코드로 크롤링 상태 조회
     */
    List<CrawlStatus> findByAreaCode(String areaCode);

    /**
     * 지역 코드와 컨텐츠 타입 ID로 크롤링 상태 조회
     */
    Optional<CrawlStatus> findByAreaCodeAndContentTypeId(String areaCode, String contentTypeId);

    /**
     * 상태로 크롤링 상태 목록 조회
     */
    List<CrawlStatus> findByStatus(CrawlStatusType status);

    /**
     * 지역 코드와 상태로 크롤링 상태 목록 조회
     */
    List<CrawlStatus> findByAreaCodeAndStatus(String areaCode, CrawlStatusType status);

    /**
     * 진행 중인 크롤링 상태 조회
     */
    @Query("SELECT c FROM CrawlStatus c WHERE c.status = 'IN_PROGRESS' ORDER BY c.startedAt ASC")
    List<CrawlStatus> findInProgressCrawls();

    /**
     * 완료된 크롤링 상태 조회
     */
    @Query("SELECT c FROM CrawlStatus c WHERE c.status = 'COMPLETED' ORDER BY c.completedAt DESC")
    List<CrawlStatus> findCompletedCrawls();

    /**
     * 실패한 크롤링 상태 조회
     */
    @Query("SELECT c FROM CrawlStatus c WHERE c.status = 'FAILED' ORDER BY c.updatedAt DESC")
    List<CrawlStatus> findFailedCrawls();

    /**
     * 특정 지역의 최근 크롤링 상태 조회
     */
    @Query("SELECT c FROM CrawlStatus c WHERE c.areaCode = :areaCode ORDER BY c.updatedAt DESC")
    List<CrawlStatus> findRecentCrawlsByAreaCode(@Param("areaCode") String areaCode);

    /**
     * 지역별 크롤링 통계 조회
     */
    @Query("SELECT c.areaCode, c.areaName, COUNT(c), " +
           "SUM(CASE WHEN c.status = 'COMPLETED' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN c.status = 'FAILED' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN c.status = 'IN_PROGRESS' THEN 1 ELSE 0 END) " +
           "FROM CrawlStatus c GROUP BY c.areaCode, c.areaName")
    List<Object[]> getCrawlStatisticsByArea();

    /**
     * 컨텐츠 타입별 크롤링 통계 조회
     */
    @Query("SELECT c.contentTypeId, c.contentTypeName, COUNT(c), " +
           "SUM(CASE WHEN c.status = 'COMPLETED' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN c.status = 'FAILED' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN c.status = 'IN_PROGRESS' THEN 1 ELSE 0 END) " +
           "FROM CrawlStatus c GROUP BY c.contentTypeId, c.contentTypeName")
    List<Object[]> getCrawlStatisticsByContentType();

    /**
     * 전체 크롤링 통계 조회
     */
    @Query("SELECT COUNT(c), " +
           "SUM(CASE WHEN c.status = 'COMPLETED' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN c.status = 'FAILED' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN c.status = 'IN_PROGRESS' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN c.status = 'PENDING' THEN 1 ELSE 0 END) " +
           "FROM CrawlStatus c")
    Object[] getOverallCrawlStatistics();

    /**
     * 특정 지역의 완료된 크롤링 개수 조회
     */
    @Query("SELECT COUNT(c) FROM CrawlStatus c WHERE c.areaCode = :areaCode AND c.status = 'COMPLETED'")
    Long countCompletedCrawlsByAreaCode(@Param("areaCode") String areaCode);

    /**
     * 특정 지역의 전체 크롤링 개수 조회
     */
    @Query("SELECT COUNT(c) FROM CrawlStatus c WHERE c.areaCode = :areaCode")
    Long countTotalCrawlsByAreaCode(@Param("areaCode") String areaCode);
}

