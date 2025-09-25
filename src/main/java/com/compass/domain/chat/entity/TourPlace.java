package com.compass.domain.chat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tour_places")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TourPlace {
    
    // 기본 식별자
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // 외부 ID (source별 고유 ID)
    @Column(name = "external_id")
    private String externalId;
    
    // 필수 필드
    @Column(name = "name", nullable = false)
    private String name;            // 장소명
    
    @Column(name = "latitude")
    private Double latitude;        // 위도
    
    @Column(name = "longitude")
    private Double longitude;       // 경도
    
    @Column(name = "address")
    private String address;         // 주소
    
    @Column(name = "category")
    private String category;        // 맛집|카페|관광지|쇼핑|문화시설
    
    // 시간대 블록 정보 (핵심)
    @Column(name = "time_block")
    private String timeBlock;       // BREAKFAST|MORNING_ACTIVITY|LUNCH|CAFE|AFTERNOON_ACTIVITY|DINNER|EVENING_ACTIVITY
    
    @Column(name = "day")
    private Integer day;            // 여행 일차 (1, 2, 3)
    
    @Column(name = "recommend_time")
    private String recommendTime;   // 추천 방문 시간 (예: "10:00-11:30")
    
    // Tour API 상세 정보
    @Column(name = "operating_hours", columnDefinition = "TEXT")
    private String operatingHours;  // 운영 시간 (예: "09:00-18:00")
    
    @Column(name = "closed_days")
    private String closedDays;      // 휴무일 (예: "매주 월요일")
    
    @Column(name = "pet_allowed")
    private Boolean petAllowed;     // 반려동물 동반 가능
    
    @Column(name = "parking_available")
    private Boolean parkingAvailable; // 주차 가능 여부
    
    @Column(name = "usage_tip", columnDefinition = "TEXT")
    private String usageTip;        // 이용 팁
    
    @Column(name = "tel")
    private String tel;             // 전화번호
    
    @Column(name = "homepage")
    private String homepage;        // 홈페이지
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;     // 간단 설명
    
    @Column(name = "overview", columnDefinition = "TEXT")
    private String overview;        // 상세 설명
    
    // Google Places 정보
    @Column(name = "photo_url")
    private String photoUrl;        // 대표 사진 URL
    
    @Column(name = "rating")
    private Double rating;          // 평점 (1-5)
    
    @Column(name = "review_count")
    private Integer reviewCount;    // 리뷰 수
    
    @Column(name = "price_level")
    private String priceLevel;      // 가격 정보 ($~$$$$)
    
    // 클러스터 정보 (새로 추가)
    @Column(name = "cluster_name")
    private String clusterName;     // 클러스터명 (hongdae, gangnam, sungsu 등)
    
    @Column(name = "match_score")
    private Double matchScore;      // 스타일 매칭 점수 (0.0-1.0)
    
    // 메타 정보
    @Column(name = "source")
    private String source;          // Perplexity(기본)|TourAPI|Kakao|Google
    
    @Column(name = "is_trendy")
    private Boolean isTrendy;       // Perplexity 추천 여부 (트렌드)
    
    @Column(name = "thread_id")
    private String threadId;        // 스레드 ID
    
    // 생성/수정 시간
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // 편의 메서드
    public boolean hasLocation() {
        return latitude != null && longitude != null;
    }
    
    public boolean hasCompleteInfo() {
        return name != null && address != null && hasLocation() && timeBlock != null;
    }
    
    public String getFullAddress() {
        if (address == null) return "";
        return address;
    }
    
    public String getDisplayName() {
        return name != null ? name : "알 수 없는 장소";
    }
}
