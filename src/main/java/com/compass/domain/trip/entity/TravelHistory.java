package com.compass.domain.trip.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 사용자 여행 히스토리 엔티티
 * REQ-LLM-004: TravelHistory 테이블 - 과거 여행 경험 저장
 */
@Entity
@Table(name = "travel_histories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    /**
     * 여행 목적지
     */
    @Column(name = "destination", nullable = false, length = 100)
    private String destination;
    
    /**
     * 여행 시작일
     */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    
    /**
     * 여행 종료일
     */
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;
    
    /**
     * 여행 유형 (가족, 커플, 혼자, 친구, 비즈니스)
     */
    @Column(name = "travel_type", length = 50)
    private String travelType;
    
    /**
     * 동행 인원수
     */
    @Column(name = "companion_count")
    private Integer companionCount;
    
    /**
     * 총 여행 예산
     */
    @Column(name = "total_budget")
    private Integer totalBudget;
    
    /**
     * 실제 지출액
     */
    @Column(name = "actual_expense")
    private Integer actualExpense;
    
    /**
     * 여행 평점 (1-5)
     */
    @Column(name = "rating")
    private Integer rating;
    
    /**
     * 방문한 장소들 (JSONB)
     * 예: [{"name": "경복궁", "type": "attraction", "rating": 5}, ...]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "visited_places", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> visitedPlaces = new HashMap<>();
    
    /**
     * 선호한 활동들
     */
    @Column(name = "preferred_activities", columnDefinition = "TEXT")
    private String preferredActivities;
    
    /**
     * 여행 메모/피드백
     */
    @Column(name = "travel_notes", columnDefinition = "TEXT")
    private String travelNotes;
    
    /**
     * 여행 스타일 (휴양, 관광, 액티비티, 문화체험, 미식)
     */
    @Column(name = "travel_style", length = 100)
    private String travelStyle;
    
    /**
     * 숙박 유형 (호텔, 펜션, 게스트하우스, 에어비앤비, 리조트)
     */
    @Column(name = "accommodation_type", length = 50)
    private String accommodationType;
    
    /**
     * 주요 교통수단 (자가용, 대중교통, 렌터카, 도보)
     */
    @Column(name = "transportation_mode", length = 50)
    private String transportationMode;
    
    /**
     * 날씨 정보
     */
    @Column(name = "weather_condition", length = 50)
    private String weatherCondition;
    
    /**
     * 계절
     */
    @Column(name = "season", length = 20)
    private String season;
    
    /**
     * AI 생성 여행 계획 사용 여부
     */
    @Column(name = "used_ai_plan")
    private Boolean usedAiPlan;
    
    /**
     * AI 추천 만족도 (1-5)
     */
    @Column(name = "ai_satisfaction")
    private Integer aiSatisfaction;
    
    /**
     * 추가 메타데이터 (JSONB)
     * 예: {"photos_count": 150, "favorite_meal": "해물찜", ...}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    
    @Column(name = "created_at", nullable = false, updatable = false)
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
     * 여행 기간 계산 (일수)
     */
    public long getTripDuration() {
        if (startDate != null && endDate != null) {
            return java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
        }
        return 0;
    }
    
    /**
     * 예산 대비 실제 지출 비율
     */
    public double getExpenseRatio() {
        if (totalBudget != null && totalBudget > 0 && actualExpense != null) {
            return (double) actualExpense / totalBudget;
        }
        return 0.0;
    }
}