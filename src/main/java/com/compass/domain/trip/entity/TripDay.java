package com.compass.domain.trip.entity;

import com.compass.common.entity.BaseEntity;
import com.compass.domain.trip.Trip;
import com.compass.domain.trip.TripDetail;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 일자별 여행 일정 엔티티
 * 하루 단위로 여행 일정을 관리
 */
@Getter
@Entity
@Table(name = "trip_days", indexes = {
    @Index(name = "idx_trip_day_trip", columnList = "trip_id"),
    @Index(name = "idx_trip_day_date", columnList = "date")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TripDay extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(name = "day_number", nullable = false)
    private Integer dayNumber;  // 1일차, 2일차 등

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "start_time")
    private LocalTime startTime;  // 일정 시작 시간

    @Column(name = "end_time")
    private LocalTime endTime;    // 일정 종료 시간

    @Column(length = 100)
    private String theme;  // 오늘의 테마 (예: "제주 서쪽 탐방")

    @Column(columnDefinition = "TEXT")
    private String summary;  // 일정 요약

    // TripDetail과의 관계는 추후 TripDetail 엔티티 수정 시 연결
    @Transient
    @Builder.Default
    private List<TripDetail> details = new ArrayList<>();

    @Column(name = "total_cost_estimate")
    private Integer totalCostEstimate;  // 예상 총 비용

    @Column(name = "total_distance")
    private Double totalDistance;  // 총 이동 거리 (km)

    @Column(name = "weather_info", columnDefinition = "TEXT")
    private String weatherInfo;  // 날씨 정보

    @Column(columnDefinition = "TEXT")
    private String notes;  // 메모

    // 연관관계 편의 메서드 - TripDetail 수정 후 활성화 필요
    public void addDetail(TripDetail detail) {
        this.details.add(detail);
        // TODO: TripDetail에 TripDay 관계 추가 후 활성화
        // detail.setTripDay(this);
        detail.setTrip(this.trip);
    }

    public void removeDetail(TripDetail detail) {
        this.details.remove(detail);
        // TODO: TripDetail에 TripDay 관계 추가 후 활성화
        // detail.setTripDay(null);
    }

    // 비즈니스 메서드
    public void updateTotalCost() {
        this.totalCostEstimate = details.stream()
            .mapToInt(d -> d.getEstimatedCost() != null ? d.getEstimatedCost() : 0)
            .sum();
    }

    public void updateWeather(String weather) {
        this.weatherInfo = weather;
    }

    public void updateTimeRange(LocalTime start, LocalTime end) {
        this.startTime = start;
        this.endTime = end;
    }
}