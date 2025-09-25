package com.compass.domain.chat.route_optimization.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "travel_places", indexes = {
    @Index(name = "idx_travel_place_itinerary_day", columnList = "itinerary_id, day_number"),
    @Index(name = "idx_travel_place_is_selected", columnList = "is_selected")
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelPlace {

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
    private String timeBlock;  // BREAKFAST, LUNCH, DINNER, MORNING_ACTIVITY, etc.

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
    private String priceLevel;  // $, $$, $$$, $$$$

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
    private Integer dayNumber;  // 여행 며칠째

    @Column(name = "visit_order")
    private Integer visitOrder;  // 방문 순서

    @Column(name = "scheduled_time")
    private LocalDateTime scheduledTime;  // 예정 방문 시간

    @Column(name = "duration_minutes")
    @Builder.Default
    private Integer durationMinutes = 60;  // 예상 체류 시간

    @Column(name = "is_selected", nullable = false)
    @Builder.Default
    private Boolean isSelected = true;  // AI 추천 선택 여부

    @Column(name = "is_fixed", nullable = false)
    @Builder.Default
    private Boolean isFixed = false;  // OCR 확정 일정 여부

    @Column(name = "is_from_ocr", nullable = false)
    @Builder.Default
    private Boolean isFromOcr = false;  // OCR로 추가된 일정

    @Column(name = "ocr_document_type", length = 50)
    private String ocrDocumentType;  // FLIGHT_RESERVATION, HOTEL_RESERVATION 등

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String notes;  // 사용자 메모

    // 경로 정보
    @Column(name = "distance_from_previous")
    private Double distanceFromPrevious;  // 이전 장소로부터 거리 (km)

    @Column(name = "duration_from_previous")
    private Integer durationFromPrevious;  // 이전 장소로부터 이동 시간 (분)

    @Column(name = "transport_mode", length = 50)
    private String transportMode;  // 이전 장소에서 이동 수단

    // 연관 메서드
    public boolean hasTimeConflict(TravelPlace other) {
        if (other == null || this.scheduledTime == null || other.scheduledTime == null) {
            return false;
        }

        LocalDateTime thisEnd = this.scheduledTime.plusMinutes(this.durationMinutes);
        LocalDateTime otherEnd = other.scheduledTime.plusMinutes(other.durationMinutes);

        return !thisEnd.isBefore(other.scheduledTime) && !this.scheduledTime.isAfter(otherEnd);
    }

    public void updateVisitOrder(int order) {
        this.visitOrder = order;
    }

    public void markAsFixed() {
        this.isFixed = true;
        this.isSelected = true;
    }
}