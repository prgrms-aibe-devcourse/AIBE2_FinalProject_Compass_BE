package com.compass.domain.chat.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Stage 1에서 선별된 여행 장소 정보를 저장하는 엔티티
 */
@Entity
@Table(name = "travel_places", indexes = {
    @Index(name = "idx_travel_place_thread_id", columnList = "thread_id"),
    @Index(name = "idx_travel_place_destination", columnList = "destination"),
    @Index(name = "idx_travel_place_created_at", columnList = "created_at DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelPlaceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "thread_id", nullable = false, length = 36)
    private String threadId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "destination", nullable = false, length = 100)
    private String destination;

    @Column(name = "place_id", nullable = false, length = 100)
    private String placeId;

    @Column(name = "place_name", nullable = false, length = 200)
    private String placeName;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "rating")
    private Double rating;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "operating_hours", length = 500)
    private String operatingHours;

    @Column(name = "price_range", length = 50)
    private String priceRange;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", columnDefinition = "jsonb")
    private List<String> tags;

    @Column(name = "source", length = 100)
    private String source;

    @Column(name = "travel_style", length = 50)
    private String travelStyle;

    @Column(name = "budget", length = 50)
    private String budget;

    @Column(name = "trip_days")
    private Integer tripDays;

    @Column(name = "created_at", nullable = false)
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

    /**
     * PlaceDeduplicator.TourPlace에서 엔티티로 변환하는 정적 메서드
     */
    public static TravelPlaceEntity fromTourPlace(
            String threadId, 
            String userId, 
            String destination,
            com.compass.domain.chat.service.PlaceDeduplicator.TourPlace tourPlace,
            String travelStyle,
            String budget,
            Integer tripDays) {
        
        return TravelPlaceEntity.builder()
                .threadId(threadId)
                .userId(userId)
                .destination(destination)
                .placeId(tourPlace.id())
                .placeName(tourPlace.name())
                .address(tourPlace.address())
                .latitude(tourPlace.latitude())
                .longitude(tourPlace.longitude())
                .category(tourPlace.category())
                .rating(tourPlace.rating())
                .description(tourPlace.description())
                .operatingHours(tourPlace.operatingHours())
                .priceRange(tourPlace.priceRange())
                .tags(tourPlace.tags())
                .source(tourPlace.source())
                .travelStyle(travelStyle)
                .budget(budget)
                .tripDays(tripDays)
                .build();
    }
}

