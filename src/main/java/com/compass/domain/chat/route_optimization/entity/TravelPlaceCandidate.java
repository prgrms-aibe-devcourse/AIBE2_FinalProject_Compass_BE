package com.compass.domain.chat.route_optimization.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "travel_place_candidates", indexes = {
    @Index(name = "idx_candidate_itinerary_day", columnList = "itinerary_id, day_number")
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelPlaceCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itinerary_id", nullable = false)
    private TravelItinerary itinerary;

    @Column(name = "place_id", length = 100)
    private String placeId;  // 외부 API ID

    @Column(nullable = false)
    private String name;

    @Column(name = "time_block", length = 50)
    private String timeBlock;

    @Column(length = 100)
    private String category;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column
    private Double rating;

    @Column(name = "price_level", length = 10)
    private String priceLevel;

    @Column(name = "is_trendy")
    @Builder.Default
    private Boolean isTrendy = false;

    @Column(name = "pet_allowed")
    @Builder.Default
    private Boolean petAllowed = false;

    @Column(name = "parking_available")
    @Builder.Default
    private Boolean parkingAvailable = false;

    @Column(name = "day_number", nullable = false)
    private Integer dayNumber;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "rejection_reason")
    private String rejectionReason;  // AI가 선택하지 않은 이유

    @Column(name = "match_score")
    private Double matchScore;  // 사용자 선호도 매칭 점수

    // 편의 메서드
    public TravelPlace toTravelPlace() {
        return TravelPlace.builder()
            .placeId(this.placeId)
            .name(this.name)
            .timeBlock(this.timeBlock)
            .category(this.category)
            .address(this.address)
            .latitude(this.latitude)
            .longitude(this.longitude)
            .rating(this.rating)
            .priceLevel(this.priceLevel)
            .isTrendy(this.isTrendy)
            .petAllowed(this.petAllowed)
            .parkingAvailable(this.parkingAvailable)
            .dayNumber(this.dayNumber)
            .description(this.description)
            .imageUrl(this.imageUrl)
            .isSelected(false)
            .build();
    }
}