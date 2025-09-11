package com.compass.domain.trip.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 관광지 정보 엔티티
 * REQ-CRAWL-002: Phase별 크롤링에서 수집된 데이터를 저장
 */
@Entity
@Table(name = "tour_places", indexes = {
    @Index(name = "idx_tour_places_content_id", columnList = "content_id"),
    @Index(name = "idx_tour_places_area_code", columnList = "area_code"),
    @Index(name = "idx_tour_places_category", columnList = "category"),
    @Index(name = "idx_tour_places_content_type_id", columnList = "content_type_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TourPlace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Tour API Content ID (고유 식별자)
     */
    @Column(name = "content_id", unique = true, nullable = false, length = 50)
    private String contentId;

    /**
     * 관광지명
     */
    @Column(nullable = false, length = 255)
    private String name;

    /**
     * 카테고리 (Seoul JSON 기준)
     */
    @Column(nullable = false, length = 100)
    private String category;

    /**
     * 지역 (구/군)
     */
    @Column(length = 100)
    private String district;

    /**
     * 상세 지역
     */
    @Column(length = 100)
    private String area;

    /**
     * 위도
     */
    @Column
    private Double latitude;

    /**
     * 경도
     */
    @Column
    private Double longitude;


    /**
     * 지역 코드 (1: 서울, 6: 부산, 39: 제주)
     */
    @Column(name = "area_code", nullable = false, length = 10)
    private String areaCode;

    /**
     * 컨텐츠 타입 ID (12: 관광지, 14: 문화시설, 39: 음식점, 38: 쇼핑, 28: 레포츠, 32: 숙박)
     */
    @Column(name = "content_type_id", nullable = false, length = 10)
    private String contentTypeId;

    /**
     * 주소
     */
    @Column(length = 500)
    private String address;

    /**
     * 대표 이미지 URL
     */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /**
     * 추가 상세 정보 (JSON)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details", columnDefinition = "jsonb")
    private JsonNode details;

    /**
     * 데이터 소스 (tour_api, seoul_json 등)
     */
    @Column(name = "data_source", length = 50)
    private String dataSource;

    /**
     * 크롤링 일시
     */
    @Column(name = "crawled_at")
    private java.time.LocalDateTime crawledAt;

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

