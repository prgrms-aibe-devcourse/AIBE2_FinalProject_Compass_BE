package com.compass.domain.trip;

import com.compass.domain.common.BaseEntity;
import com.compass.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Entity
@Table(name = "trips")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@SQLDelete(sql = "UPDATE trips SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Trip extends BaseEntity {



    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    @Builder.Default
    private UUID tripUuid = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // userId는 user.getId()로 대체됨 - 중복 매핑 제거

    // TODO: 추후 ChatThread 엔티티와 연관관계 설정
    private Long threadId;
    
    private String title;

    private String destination;

    private LocalDate startDate;

    private LocalDate endDate;
    
    private Integer numberOfPeople;

    private Integer totalBudget;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Setter
    private TripStatus status = TripStatus.PLANNING;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String tripMetadata;

    @Version
    private Integer version;

    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TripDetail> details = new ArrayList<>();

    // Lombok @Builder가 자동으로 생성하므로 수동 생성자 제거

    // Constructor for DTO usage - userId 제거됨
    public Trip(Long threadId, String title, String destination, LocalDate startDate, LocalDate endDate, Integer numberOfPeople, Integer totalBudget, String status, String tripMetadata) {
        this.threadId = threadId;
        this.title = title;
        this.destination = destination;
        this.startDate = startDate;
        this.endDate = endDate;
        this.numberOfPeople = numberOfPeople;
        this.totalBudget = totalBudget;
        // Convert String status to TripStatus enum
        if (status != null) {
            try {
                this.status = TripStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                this.status = TripStatus.PLANNING;
            }
        } else {
            this.status = TripStatus.PLANNING;
        }
        this.tripMetadata = tripMetadata;
        this.details = new ArrayList<>();
    }

    public void addDetail(TripDetail detail) {
        details.add(detail);
        detail.setTrip(this);
    }


}
