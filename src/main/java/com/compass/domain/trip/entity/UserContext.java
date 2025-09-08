package com.compass.domain.trip.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 사용자 컨텍스트 정보 엔티티
 * REQ-DB-002: UserContext 테이블 - 사용자 컨텍스트 정보
 */
@Entity
@Table(name = "user_contexts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserContext {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;
    
    /**
     * 나이대 (20대, 30대, 40대, 50대+)
     */
    @Column(name = "age_group", length = 20)
    private String ageGroup;
    
    /**
     * 동행 유형 (혼자, 커플, 가족, 친구, 단체)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "travel_companion", length = 50)
    private TravelCompanion travelCompanion;
    
    /**
     * 동행 인원수
     */
    @Column(name = "companion_count")
    private Integer companionCount;
    
    /**
     * 아이 동반 여부
     */
    @Column(name = "with_children")
    private Boolean withChildren;
    
    /**
     * 아이 연령대 (유아, 초등학생, 중고등학생)
     */
    @Column(name = "children_age_group", length = 50)
    private String childrenAgeGroup;
    
    /**
     * 신체 조건 (휠체어, 유모차 필요 등)
     */
    @Column(name = "physical_condition", length = 100)
    private String physicalCondition;
    
    /**
     * 특별 요구사항
     * 예: 알레르기, 종교적 제약, 의료 조건 등
     */
    @Column(name = "special_requirements", columnDefinition = "TEXT")
    private String specialRequirements;
    
    /**
     * 언어 선호도 (한국어, 영어, 일본어, 중국어 등)
     */
    @Column(name = "language_preference", length = 50)
    private String languagePreference;
    
    /**
     * 과거 피드백 및 선호도 학습 데이터
     * 예: {"like_crowded": false, "prefer_morning": true}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "past_feedback", columnDefinition = "jsonb")
    private Map<String, Object> pastFeedback;
    
    /**
     * 현재 여행 목적 (관광, 비즈니스, 의료, 교육 등)
     */
    @Column(name = "current_trip_purpose", length = 50)
    private String currentTripPurpose;
    
    /**
     * 계절 선호도 (봄, 여름, 가을, 겨울)
     */
    @Column(name = "season_preference", length = 20)
    private String seasonPreference;
    
    /**
     * 추가 컨텍스트 정보 (JSONB)
     * 유연한 확장을 위한 필드
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "additional_context", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> additionalContext = new HashMap<>();
    
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
     * 동행 유형 열거형
     */
    public enum TravelCompanion {
        SOLO("혼자"),
        COUPLE("커플/연인"),
        FAMILY("가족"),
        FRIENDS("친구"),
        GROUP("단체"),
        BUSINESS("비즈니스");
        
        private final String description;
        
        TravelCompanion(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}