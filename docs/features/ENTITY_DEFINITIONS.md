# 📋 엔티티 정의서 (Entity Definitions)

이 문서는 Compass 프로젝트에서 사용하는 JPA 엔티티의 전체 코드를 관리합니다.

---

## 🗺️ Trip Domain

### `TourPlace.java` (최적화됨)

```java
// src/main/java/com/compass/domain/trip/entity/TourPlace.java

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
 * 관광지 정보 엔티티 (최적화됨)
 * - 불필요한 null 필드들 제거
 * - 핵심 필드만 유지하여 효율성 향상
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
     * Tour API 고유 식별자
     */
    @Column(name = "content_id", unique = true, nullable = false, length = 50)
    private String contentId;

    /**
     * 관광지명
     */
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * 카테고리 (관광지, 문화시설, 음식점, 쇼핑, 레포츠, 숙박)
     */
    @Column(nullable = false, length = 50)
    private String category;

    /**
     * 지역 (구/군)
     */
    @Column(length = 50)
    private String district;

    /**
     * 상세 지역
     */
    @Column(length = 200)
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
     * 데이터 소스 (tour_api)
     */
    @Column(name = "data_source", length = 50)
    private String dataSource;

    /**
     * 크롤링 일시
     */
    @Column(name = "crawled_at")
    private LocalDateTime crawledAt;

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
```

### `CrawlStatus.java` (최적화됨)

```java
// src/main/java/com/compass/domain/trip/entity/CrawlStatus.java

package com.compass.domain.trip.entity;

import java.time.LocalDateTime;

import com.compass.domain.trip.enums.CrawlStatusType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * 크롤링 상태 추적 엔티티 (최적화됨)
 * - BaseEntity 상속 제거하고 직접 필드 정의
 * - 크롤링 진행 상황을 실시간으로 추적
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
     * 컨텐츠 타입 ID
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
     * 총 페이지 수
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
    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    /**
     * 시작 일시
     */
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /**
     * 완료 일시
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

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
```

---

## 🗺️ Trip Domain

### `Trip.java`

```java
// src/main/java/com/compass/domain/trip/Trip.java

package com.compass.domain.trip;

import com.compass.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Entity
@Table(name = "trips")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE trips SET deleted_at = NOW() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class Trip extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID tripUuid = UUID.randomUUID();

    // TODO: 추후 User 엔티티와 연관관계 설정
    private Long userId;

    // TODO: 추후 ChatThread 엔티티와 연관관계 설정
    private Long threadId;
    
    private String title;

    private String destination;

    private LocalDate startDate;

    private LocalDate endDate;
    
    private Integer numberOfPeople;

    private Integer totalBudget;

    private String status = "PLANNING";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String tripMetadata;

    @Version
    private Integer version;

    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TripDetail> details = new ArrayList<>();

    @Builder
    public Trip(Long userId, Long threadId, String title, String destination, LocalDate startDate, LocalDate endDate, Integer numberOfPeople, Integer totalBudget, String status, String tripMetadata) {
        this.userId = userId;
        this.threadId = threadId;
        this.title = title;
        this.destination = destination;
        this.startDate = startDate;
        this.endDate = endDate;
        this.numberOfPeople = numberOfPeople;
        this.totalBudget = totalBudget;
        this.status = (status != null) ? status : "PLANNING";
        this.tripMetadata = tripMetadata;
    }

    public void addDetail(TripDetail detail) {
        details.add(detail);
        detail.setTrip(this);
    }
}
```

### `TripDetail.java`

```java
// src/main/java/com/compass/domain/trip/TripDetail.java

package com.compass.domain.trip;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Entity
@Table(name = "trip_details")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TripDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    private Integer dayNumber;

    private LocalDate activityDate;
    
    private LocalTime activityTime;

    private String placeName;

    private String category;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Integer estimatedCost;

    private String address;
    
    private Double latitude;
    
    private Double longitude;

    @Column(columnDefinition = "TEXT")
    private String tips;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String additionalInfo;
    
    private Integer displayOrder;

    @Builder
    public TripDetail(Integer dayNumber, LocalDate activityDate, LocalTime activityTime, String placeName, String category, String description, Integer estimatedCost, String address, Double latitude, Double longitude, String tips, String additionalInfo, Integer displayOrder) {
        this.dayNumber = dayNumber;
        this.activityDate = activityDate;
        this.activityTime = activityTime;
        this.placeName = placeName;
        this.category = category;
        this.description = description;
        this.estimatedCost = estimatedCost;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.tips = tips;
        this.additionalInfo = additionalInfo;
        this.displayOrder = displayOrder;
    }
}
```
