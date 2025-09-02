# 📋 엔티티 정의서 (Entity Definitions)

이 문서는 Compass 프로젝트에서 사용하는 JPA 엔티티의 전체 코드를 관리합니다.

---

## 🗺️ Trip Domain

### `Trip.java`

```java
// src/main/java/com/compass/domain/trip/Trip.java

package com.compass.domain.trip;

import com.compass.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
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
@Where(clause = "deleted_at IS NULL")
public class Trip extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Builder
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
    }

    public void addDetail(TripDetail detail) {
        details.add(detail);
        detail.setTrip(this);
    }
}
```

### `TripDetail.java`

```java
// src/main/java/com/compass/domain/trip/TripDetail.java

package com.compass.domain.trip;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Entity
@Table(name = "trip_details")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TripDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    private Integer dayNumber;

    private LocalDate activityDate;
    
    private LocalTime activityTime;

    private String placeName;

    private String category;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Integer estimatedCost;

    private String address;
    
    private Double latitude;
    
    private Double longitude;

    @Column(columnDefinition = "TEXT")
    private String tips;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String additionalInfo;
    
    private Integer displayOrder;

    @Builder
    public TripDetail(Integer dayNumber, LocalDate activityDate, LocalTime activityTime, String placeName, String category, String description, Integer estimatedCost, String address, Double latitude, Double longitude, String tips, String additionalInfo, Integer displayOrder) {
        this.dayNumber = dayNumber;
        this.activityDate = activityDate;
        this.activityTime = activityTime;
        this.placeName = placeName;
        this.category = category;
        this.description = description;
        this.estimatedCost = estimatedCost;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.tips = tips;
        this.additionalInfo = additionalInfo;
        this.displayOrder = displayOrder;
    }
}
```
