package com.compass.domain.trip.entity;

import com.compass.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * 장소 마스터 데이터 엔티티
 * Tour API 크롤링 데이터 + Perplexity 실시간 검색 데이터 저장
 */
@Getter
@Entity
@Table(name = "places", indexes = {
    @Index(name = "idx_place_destination", columnList = "destination"),
    @Index(name = "idx_place_category", columnList = "category"),
    @Index(name = "idx_place_destination_category", columnList = "destination,category"),
    @Index(name = "idx_place_code", columnList = "place_code")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Place extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "place_code", unique = true, length = 100)
    private String placeCode;  // Tour API 코드

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "name_en", length = 200)
    private String nameEn;  // 영문명

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private PlaceCategory category;

    @Column(name = "sub_category", length = 50)
    private String subCategory;

    @Column(nullable = false, length = 50)
    private String destination;  // 지역 (제주, 부산, 서울 등)

    @Column(columnDefinition = "TEXT")
    private String address;

    private Double latitude;
    private Double longitude;

    @Column(length = 20)
    private String phone;

    @Column(length = 500)
    private String website;

    @Column(name = "business_hours", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String businessHours;  // JSON: {"mon": "09:00-18:00", "tue": "09:00-18:00", ...}

    @Column(name = "price_range")
    private Integer priceRange;  // 1-5 가격대

    private Double rating;  // 평점

    @Column(name = "review_count")
    private Integer reviewCount;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_urls", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String imageUrls;  // JSON Array

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String tags;  // JSON Array: ["SNS핫플", "오션뷰", "카페"]

    @Column(name = "is_trendy")
    @Builder.Default
    private Boolean isTrendy = false;  // Perplexity로 찾은 트렌디 장소

    @Column(name = "data_source", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DataSource dataSource = DataSource.TOUR_API;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    // 편의 메서드
    public void updateFromPerplexity() {
        this.isTrendy = true;
        this.dataSource = DataSource.PERPLEXITY;
        this.lastUpdated = LocalDateTime.now();
    }

    public void updateRating(Double newRating, Integer newReviewCount) {
        this.rating = newRating;
        this.reviewCount = newReviewCount;
        this.lastUpdated = LocalDateTime.now();
    }

    // Enum 정의
    public enum PlaceCategory {
        ATTRACTION,     // 관광지
        RESTAURANT,     // 맛집
        CAFE,          // 카페
        HOTEL,         // 숙소
        SHOPPING,      // 쇼핑
        ACTIVITY,      // 액티비티
        CULTURE,       // 문화시설
        NATURE,        // 자연
        NIGHTLIFE,     // 나이트라이프
        TRANSPORT      // 교통
    }

    public enum DataSource {
        TOUR_API,      // Tour API 크롤링
        PERPLEXITY,    // Perplexity 검색
        MANUAL,        // 수동 입력
        HYBRID         // 혼합 (업데이트된 경우)
    }
}