package com.compass.domain.trip.entity;

import com.compass.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.compass.domain.trip.enums.BudgetLevel;

import java.math.BigDecimal;

/**
 * 사용자 선호도 엔티티
 * 여행 스타일, 예산 수준 등 사용자의 다양한 선호도를 저장
 */
@Getter
@Entity
@Table(name = "user_preferences_1")// User 폴더에 있는 UserPreference와 겹쳐서 변경
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserPreference extends BaseEntity {

    /**
     * 기본 키
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 사용자 ID (추후 User 엔티티와 연관관계 설정 예정)
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 선호도 타입 (TRAVEL_STYLE, BUDGET_LEVEL 등)
     */
    @Column(name = "preference_type", nullable = false, length = 50)
    private String preferenceType;

    /**
     * 선호도 키 (RELAXATION, SIGHTSEEING, ACTIVITY 등)
     */
    @Column(name = "preference_key", nullable = false, length = 50)
    private String preferenceKey;

    /**
     * 선호도 값 (가중치: 0.00 ~ 1.00)
     */
    @Column(name = "preference_value", nullable = false, precision = 3, scale = 2)
    private BigDecimal preferenceValue;

    /**
     * 선호도 설명
     */
    @Column(name = "description")
    private String description;

    /**
     * 복합 유니크 제약조건: 한 사용자가 같은 타입의 같은 키를 중복으로 가질 수 없음
     */
    @Table(name = "user_preferences", 
           uniqueConstraints = {
               @UniqueConstraint(
                   name = "uk_user_preference_type_key",
                   columnNames = {"user_id", "preference_type", "preference_key"}
               )
           })
    public static class UserPreferenceConstraints {}

    /**
     * 선호도 값 업데이트
     * 
     * @param preferenceValue 새로운 선호도 값
     */
    public void updatePreferenceValue(BigDecimal preferenceValue) {
        validatePreferenceValue(preferenceValue);
        this.preferenceValue = preferenceValue;
    }

    public void updateBudgetData(BudgetLevel budgetLevel) {
        this.preferenceKey = budgetLevel.name();
        this.description = budgetLevel.getDescription();
        this.preferenceValue = BigDecimal.valueOf(1.0); // 고정값
    }

    /**
     * 설명 업데이트
     * 
     * @param description 새로운 설명
     */
    public void updateDescription(String description) {
        this.description = description;
    }

    /**
     * 선호도 값 검증
     * 
     * @param value 검증할 값
     */
    private void validatePreferenceValue(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("선호도 값은 null일 수 없습니다.");
        }
        
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("선호도 값은 0.0 ~ 1.0 범위 내여야 합니다. 입력값: " + value);
        }
    }

    /**
     * 여행 스타일 선호도 생성을 위한 팩토리 메서드
     * 
     * @param userId 사용자 ID
     * @param travelStyle 여행 스타일
     * @param weight 가중치
     * @param description 설명
     * @return UserPreference 인스턴스
     */
    public static UserPreference createTravelStylePreference(Long userId, String travelStyle, 
                                                           BigDecimal weight, String description) {
        return UserPreference.builder()
                .userId(userId)
                .preferenceType("TRAVEL_STYLE")
                .preferenceKey(travelStyle)
                .preferenceValue(weight)
                .description(description)
                .build();
    }

    /**
     * 선호도 타입이 여행 스타일인지 확인
     * 
     * @return 여행 스타일 여부
     */
    public boolean isTravelStylePreference() {
        return "TRAVEL_STYLE".equals(this.preferenceType);
    }
}
