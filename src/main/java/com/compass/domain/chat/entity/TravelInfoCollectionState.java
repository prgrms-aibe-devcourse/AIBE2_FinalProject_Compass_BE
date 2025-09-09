package com.compass.domain.chat.entity;

import com.compass.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 여행 정보 수집 상태 엔티티
 * REQ-FOLLOW-002: 사용자로부터 필수 여행 정보를 단계적으로 수집하기 위한 상태 관리
 */
@Entity
@Table(name = "travel_info_collection_states")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravelInfoCollectionState {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_thread_id")
    private ChatThread chatThread;
    
    @Column(name = "session_id", unique = true, nullable = false)
    private String sessionId;
    
    // 수집된 정보 필드들
    @Column(name = "origin")
    private String origin;
    
    @Column(name = "destination")
    private String destination;
    
    @Column(name = "start_date")
    private LocalDate startDate;
    
    @Column(name = "end_date")
    private LocalDate endDate;
    
    @Column(name = "duration_nights")
    private Integer durationNights;
    
    @Column(name = "number_of_travelers")
    private Integer numberOfTravelers;
    
    @Column(name = "companion_type")
    private String companionType; // solo, couple, family, friends, business
    
    @Column(name = "budget_per_person")
    private Integer budgetPerPerson;
    
    @Column(name = "budget_currency")
    private String budgetCurrency;
    
    @Column(name = "budget_level")
    private String budgetLevel; // budget, moderate, luxury
    
    // 수집 상태 추적
    @Column(name = "origin_collected")
    private boolean originCollected;
    
    @Column(name = "destination_collected")
    private boolean destinationCollected;
    
    @Column(name = "dates_collected")
    private boolean datesCollected;
    
    @Column(name = "duration_collected")
    private boolean durationCollected;
    
    @Column(name = "companions_collected")
    private boolean companionsCollected;
    
    @Column(name = "budget_collected")
    private boolean budgetCollected;
    
    // 추가 선호사항 (JSON으로 저장)
    @Column(name = "additional_preferences", columnDefinition = "TEXT")
    private String additionalPreferences;
    
    @Column(name = "current_step")
    @Enumerated(EnumType.STRING)
    private CollectionStep currentStep;
    
    @Column(name = "is_completed")
    private boolean isCompleted;
    
    @Column(name = "last_question_asked", columnDefinition = "TEXT")
    private String lastQuestionAsked;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    /**
     * 정보 수집 단계
     */
    public enum CollectionStep {
        INITIAL,        // 초기 상태
        ORIGIN,         // 출발지 수집 중
        DESTINATION,    // 목적지 수집 중
        DATES,          // 날짜 수집 중
        DURATION,       // 기간 수집 중
        COMPANIONS,     // 동행자 수집 중
        BUDGET,         // 예산 수집 중
        CONFIRMATION,   // 확인 단계
        COMPLETED       // 완료
    }
    
    /**
     * 모든 필수 정보가 수집되었는지 확인
     */
    public boolean isAllRequiredInfoCollected() {
        return originCollected &&
               destinationCollected && 
               datesCollected && 
               durationCollected && 
               companionsCollected && 
               budgetCollected;
    }
    
    /**
     * 다음 수집 단계 결정
     */
    public CollectionStep getNextRequiredStep() {
        if (!originCollected) return CollectionStep.ORIGIN;
        if (!destinationCollected) return CollectionStep.DESTINATION;
        if (!datesCollected) return CollectionStep.DATES;
        if (!durationCollected) return CollectionStep.DURATION;
        if (!companionsCollected) return CollectionStep.COMPANIONS;
        if (!budgetCollected) return CollectionStep.BUDGET;
        return CollectionStep.CONFIRMATION;
    }
    
    /**
     * 수집 진행률 계산 (0-100%)
     */
    public int getCompletionPercentage() {
        int collected = 0;
        if (originCollected) collected++;
        if (destinationCollected) collected++;
        if (datesCollected) collected++;
        if (durationCollected) collected++;
        if (companionsCollected) collected++;
        if (budgetCollected) collected++;
        return (collected * 100) / 6;  // 이제 6개 필드
    }
    
    /**
     * 수집 완료 처리
     */
    public void markAsCompleted() {
        this.isCompleted = true;
        this.completedAt = LocalDateTime.now();
        this.currentStep = CollectionStep.COMPLETED;
    }
    
    /**
     * 수집된 정보를 Map으로 변환
     */
    public Map<String, Object> toInfoMap() {
        Map<String, Object> info = new HashMap<>();
        
        if (originCollected) {
            info.put("origin", origin);
        }
        if (destinationCollected) {
            info.put("destination", destination);
        }
        if (datesCollected) {
            info.put("startDate", startDate);
            info.put("endDate", endDate);
        }
        if (durationCollected) {
            info.put("durationNights", durationNights);
        }
        if (companionsCollected) {
            info.put("numberOfTravelers", numberOfTravelers);
            info.put("companionType", companionType);
        }
        if (budgetCollected) {
            info.put("budgetPerPerson", budgetPerPerson);
            info.put("budgetCurrency", budgetCurrency);
            info.put("budgetLevel", budgetLevel);
        }
        
        return info;
    }
}