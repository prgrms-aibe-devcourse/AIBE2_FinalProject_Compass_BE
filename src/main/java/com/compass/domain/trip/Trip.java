package com.compass.domain.trip;

import com.compass.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
@SQLDelete(sql = "UPDATE trips SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Trip extends BaseEntity {



    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID tripUuid = UUID.randomUUID();

    // TODO: 추후 User 엔티티와 연관관계 설정
    private Long userId;

    // TODO: 추후 ChatThread 엔티티와 연관관계 설정
    private Long threadId;
    
    private String title;

    private String destination;

    private LocalDate startDate;

    private LocalDate endDate;
    
    private Integer numberOfPeople;

    private Integer totalBudget;

    private String status = "PLANNING";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String tripMetadata;

    @Version
    private Integer version;

    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TripDetail> details = new ArrayList<>();

    // Lombok @Builder가 자동으로 생성하므로 수동 생성자 제거

    // Constructor for DTO usage
    public Trip(Long userId, Long threadId, String title, String destination, LocalDate startDate, LocalDate endDate, Integer numberOfPeople, Integer totalBudget, String status, String tripMetadata) {
        this.userId = userId;
        this.threadId = threadId;
        this.title = title;
        this.destination = destination;
        this.startDate = startDate;
        this.endDate = endDate;
        this.numberOfPeople = numberOfPeople;
        this.totalBudget = totalBudget;
        this.status = (status != null) ? status : "PLANNING";
        this.tripMetadata = tripMetadata;
        this.details = new ArrayList<>();
    }

    public void addDetail(TripDetail detail) {
        details.add(detail);
        detail.setTrip(this);
    }
}
