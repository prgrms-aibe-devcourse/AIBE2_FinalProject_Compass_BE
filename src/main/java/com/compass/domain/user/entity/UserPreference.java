package com.compass.domain.user.entity;

import com.compass.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Entity(name = "user_preferences")
@Table(name = "user_preferences",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "preferenceType", "preferenceKey"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPreference extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String preferenceType; // 예: "TRAVEL_STYLE"

    @Column(nullable = false, length = 50)
    private String preferenceKey;  // 예: "RELAXATION", "SIGHTSEEING"

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal preferenceValue; // 예: 0.30 (30%)

    @Builder
    public UserPreference(User user, String preferenceType, String preferenceKey, BigDecimal preferenceValue) {
        this.user = user;
        this.preferenceType = preferenceType;
        this.preferenceKey = preferenceKey;
        this.preferenceValue = preferenceValue;
    }
}