package com.compass.domain.chat.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 여행지 후보 엔티티
 * Pre-Stage에서 수집되어 DB에 저장되는 여행 후보지 정보
 */
@Entity
@Table(name = "travel_candidates",
    indexes = {
        @Index(name = "idx_place_id", columnList = "place_id"),
        @Index(name = "idx_region", columnList = "region"),
        @Index(name = "idx_category", columnList = "category"),
        @Index(name = "idx_time_block", columnList = "time_block"),
        @Index(name = "idx_rating", columnList = "rating DESC"),
        @Index(name = "idx_review_count", columnList = "review_count DESC"),
        @Index(name = "idx_quality_score", columnList = "quality_score DESC")
    },
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"place_id", "region"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"photoUrl", "description"})
public class TravelCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Google Place ID (고유 식별자)
    @Column(name = "place_id", nullable = false, length = 255)
    private String placeId;

    // 장소명
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    // 지역 (서울, 부산 등)
    @Column(name = "region", nullable = false, length = 50)
    private String region;

    // 세부 지역 (홍대/신촌, 강남/서초 등)
    @Column(name = "sub_region", length = 100)
    private String subRegion;

    // 카테고리 (관광지, 맛집, 카페 등)
    @Column(name = "category", nullable = false, length = 50)
    private String category;

    // 시간 블럭 (BREAKFAST, MORNING_ACTIVITY, LUNCH 등)
    @Column(name = "time_block", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private TimeBlock timeBlock;

    // 위도
    @Column(name = "latitude", nullable = false)
    private Double latitude;

    // 경도
    @Column(name = "longitude", nullable = false)
    private Double longitude;

    // 주소
    @Column(name = "address", length = 500)
    private String address;

    // 평점 (1.0 ~ 5.0)
    @Column(name = "rating")
    private Double rating;

    // 리뷰 수
    @Column(name = "review_count")
    private Integer reviewCount;

    // 가격대 (0 ~ 4, -1은 정보없음)
    @Column(name = "price_level")
    private Integer priceLevel;

    // 대표 사진 URL
    @Column(name = "photo_url", length = 1000)
    private String photoUrl;

    // 현재 영업 여부
    @Column(name = "open_now")
    private Boolean openNow;

    // 전화번호
    @Column(name = "phone_number", length = 50)
    private String phoneNumber;

    // 웹사이트
    @Column(name = "website", length = 500)
    private String website;

    // 설명 (평점, 리뷰 수 등 요약 정보)
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // 품질 점수 (평점 70% + 리뷰수 30% 가중치)
    @Column(name = "quality_score")
    private Double qualityScore;

    // 신뢰도 레벨 (매우높음, 높음, 보통, 낮음)
    @Column(name = "reliability_level", length = 20)
    private String reliabilityLevel;

    // Google Places 타입 (JSON 배열 형태로 저장)
    @Column(name = "google_types", length = 500)
    private String googleTypes;

    // 수집 일시
    @CreationTimestamp
    @Column(name = "collected_at", nullable = false, updatable = false)
    private LocalDateTime collectedAt;

    // 최종 수정 일시
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 활성화 여부 (soft delete용)
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // === Tour API로 보강되는 추가 필드 ===

    // 반려동물 동반 가능 여부
    @Column(name = "pet_friendly")
    private Boolean petFriendly;

    // 주차 가능 여부
    @Column(name = "parking_available")
    private Boolean parkingAvailable;

    // 휠체어 접근 가능 여부
    @Column(name = "wheelchair_accessible")
    private Boolean wheelchairAccessible;

    // 와이파이 제공 여부
    @Column(name = "wifi_available")
    private Boolean wifiAvailable;

    // 영업 시간 (JSON 형태)
    @Column(name = "business_hours", columnDefinition = "TEXT")
    private String businessHours;

    // 이용 요금 정보
    @Column(name = "admission_fee", length = 500)
    private String admissionFee;

    // === Perplexity로 보강되는 추가 필드 ===

    // 추천 방문 시간
    @Column(name = "recommended_duration", length = 100)
    private String recommendedDuration;

    // 주요 특징 및 하이라이트
    @Column(name = "highlights", columnDefinition = "TEXT")
    private String highlights;

    // 팁 및 주의사항
    @Column(name = "tips", columnDefinition = "TEXT")
    private String tips;

    // 근처 명소
    @Column(name = "nearby_attractions", columnDefinition = "TEXT")
    private String nearbyAttractions;

    // === API 식별자 필드 추가 ===

    // Google Places ID
    @Column(name = "google_place_id", length = 100)
    private String googlePlaceId;

    // Tour API Content ID
    @Column(name = "tour_api_content_id", length = 100)
    private String tourApiContentId;

    // Kakao Place ID
    @Column(name = "kakao_place_id", length = 100)
    private String kakaoPlaceId;

    // 상세 주소
    @Column(name = "detailed_address", length = 500)
    private String detailedAddress;

    // AI 보강 여부
    @Column(name = "ai_enriched")
    private Boolean aiEnriched;

    // 수용 인원
    @Column(name = "capacity")
    private Integer capacity;

    // 휴무일 (Tour API)
    @Column(name = "closed_days", length = 200)
    private String closedDays;

    // 휴무일 (Tour API - restdate)
    @Column(name = "rest_day", length = 200)
    private String restDay;

    // 우편번호
    @Column(name = "postal_code", length = 20)
    private String postalCode;

    // 특별 이벤트
    @Column(name = "special_events", columnDefinition = "TEXT")
    private String specialEvents;

    // 데이터 보강 상태 (0: 미완료, 1: Tour API 완료, 2: Perplexity 완료)
    @Column(name = "enrichment_status")
    @Builder.Default
    private Integer enrichmentStatus = 0;

    // 시간 블럭 Enum
    public enum TimeBlock {
        BREAKFAST("아침식사", 7, 9),
        MORNING_ACTIVITY("오전일과", 9, 12),
        LUNCH("점심식사", 12, 14),
        AFTERNOON_ACTIVITY("오후일과", 14, 18),
        DINNER("저녁식사", 18, 20),
        EVENING_ACTIVITY("저녁일과", 20, 23);

        private final String koreanName;
        private final int startHour;
        private final int endHour;

        TimeBlock(String koreanName, int startHour, int endHour) {
            this.koreanName = koreanName;
            this.startHour = startHour;
            this.endHour = endHour;
        }

        public String getKoreanName() {
            return koreanName;
        }

        public int getStartHour() {
            return startHour;
        }

        public int getEndHour() {
            return endHour;
        }
    }

    // 품질 점수 계산 메서드
    @PrePersist
    @PreUpdate
    public void calculateScores() {
        // 품질 점수 계산
        if (rating != null && reviewCount != null) {
            double normalizedRating = rating / 5.0;
            double normalizedReviews = Math.log10(reviewCount + 1) / 4.0;
            normalizedReviews = Math.min(normalizedReviews, 1.0);
            this.qualityScore = (normalizedRating * 0.7) + (normalizedReviews * 0.3);
        } else {
            this.qualityScore = 0.0;
        }

        // 신뢰도 레벨 계산
        if (reviewCount == null || reviewCount == 0) {
            this.reliabilityLevel = "정보없음";
        } else if (reviewCount >= 1000 && rating >= 4.0) {
            this.reliabilityLevel = "매우높음";
        } else if (reviewCount >= 500 && rating >= 3.5) {
            this.reliabilityLevel = "높음";
        } else if (reviewCount >= 100 && rating >= 3.0) {
            this.reliabilityLevel = "보통";
        } else {
            this.reliabilityLevel = "낮음";
        }
    }

    // TravelPlace로 변환 (Stage 2 연동용)
    public com.compass.domain.chat.model.TravelPlace toTravelPlace() {
        return com.compass.domain.chat.model.TravelPlace.builder()
            .placeId(this.placeId)
            .name(this.name)
            .category(this.category)
            .latitude(this.latitude)
            .longitude(this.longitude)
            .address(this.address)
            .description(this.description)
            .rating(this.rating)
            .reviewCount(this.reviewCount)
            .priceLevel(this.priceLevel)
            .photoUrl(this.photoUrl)
            .openNow(this.openNow)
            .phoneNumber(this.phoneNumber)
            .website(this.website)
            .build();
    }
}
