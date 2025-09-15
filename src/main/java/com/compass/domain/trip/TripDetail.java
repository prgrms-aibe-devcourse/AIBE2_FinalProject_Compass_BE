package com.compass.domain.trip;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Entity
@Table(name = "trip_details")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
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

    @Column(columnDefinition = "TEXT")
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
    
    // Manual setter for Trip relationship
    public void setTrip(Trip trip) {
        this.trip = trip;
    }

    // 수동 setter 메서드들 (Lombok @Setter가 작동하지 않는 경우를 대비)
    public void setActivityDate(LocalDate activityDate) {
        this.activityDate = activityDate;
    }

    public void setPlaceName(String placeName) {
        this.placeName = placeName;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDayNumber(Integer dayNumber) {
        this.dayNumber = dayNumber;
    }

    public void setActivityTime(LocalTime activityTime) {
        this.activityTime = activityTime;
    }

    public void setEstimatedCost(Integer estimatedCost) {
        this.estimatedCost = estimatedCost;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public void setTips(String tips) {
        this.tips = tips;
    }

    public void setAdditionalInfo(String additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}
