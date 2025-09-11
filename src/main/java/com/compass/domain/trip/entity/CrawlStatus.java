package com.compass.domain.trip.entity;

import java.time.LocalDateTime;
import com.compass.domain.trip.enums.CrawlStatusType;
import jakarta.persistence.*;
import lombok.*;

/**
 * 크롤링 상태 관리 엔티티
 * REQ-CRAWL-002: Phase별 크롤링 진행 상황을 추적
 */
@Entity
@Table(name = "crawl_status", indexes = {
    @Index(name = "idx_crawl_status_area_code", columnList = "area_code"),
    @Index(name = "idx_crawl_status_content_type_id", columnList = "content_type_id"),
    @Index(name = "idx_crawl_status_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrawlStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 지역 코드 (1: 서울, 6: 부산, 39: 제주)
     */
    @Column(name = "area_code", nullable = false, length = 10)
    private String areaCode;

    /**
     * 지역명
     */
    @Column(name = "area_name", nullable = false, length = 50)
    private String areaName;

    /**
     * 컨텐츠 타입 ID (12: 관광지, 14: 문화시설, 39: 음식점, 38: 쇼핑, 28: 레포츠, 32: 숙박)
     */
    @Column(name = "content_type_id", length = 10)
    private String contentTypeId;

    /**
     * 컨텐츠 타입명
     */
    @Column(name = "content_type_name", length = 50)
    private String contentTypeName;

    /**
     * 크롤링 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CrawlStatusType status;

    /**
     * 전체 페이지 수
     */
    @Column(name = "total_pages")
    private Integer totalPages;

    /**
     * 현재 페이지
     */
    @Column(name = "current_page")
    private Integer currentPage;

    /**
     * 수집된 데이터 개수
     */
    @Column(name = "collected_count")
    private Integer collectedCount;

    /**
     * 오류 메시지
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 크롤링 시작 시간
     */
    @Column(name = "started_at")
    private java.time.LocalDateTime startedAt;

    /**
     * 크롤링 완료 시간
     */
    @Column(name = "completed_at")
    private java.time.LocalDateTime completedAt;

    /**
     * 예상 수집 개수
     */
    @Column(name = "expected_count")
    private Integer expectedCount;

    /**
     * 진행률 (0-100)
     */
    @Column(name = "progress_percentage")
    private Integer progressPercentage;

    /**
     * 크롤링 소요 시간 (초)
     */
    @Column(name = "duration_seconds")
    private Long durationSeconds;

    /**
     * 생성 일시
     */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 수정 일시
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
