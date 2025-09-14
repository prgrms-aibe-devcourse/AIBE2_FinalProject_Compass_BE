package com.compass.domain.trip.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * 관광지 정보 엔티티
 * REQ-SEARCH-001: RDS 검색 시스템을 위한 관광지 데이터 저장
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
    
    @Column(name = "content_id", unique = true, nullable = false, length = 50)
    private String contentId;
    
    @Column(nullable = false, length = 200)
    private String name;
    
    @Column(nullable = false, length = 50)
    private String category;
    
    @Column(length = 50)
    private String district;
    
    @Column(length = 200)
    private String area;
    
    @Column
    private Double latitude;
    
    @Column
    private Double longitude;
    
    @Column(name = "area_code", nullable = false, length = 10)
    private String areaCode;
    
    @Column(name = "content_type_id", nullable = false, length = 10)
    private String contentTypeId;
    
    @Column(length = 500)
    private String address;
    
    @Column(name = "image_url", length = 500)
    private String imageUrl;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details", columnDefinition = "jsonb")
    private JsonNode details;
    
    @Column(name = "data_source", length = 50)
    private String dataSource;
    
    @Column(name = "crawled_at")
    private LocalDateTime crawledAt;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
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

