package com.compass.domain.trip.entity;

import com.compass.common.entity.BaseEntity;
import com.compass.domain.trip.Trip;
import com.compass.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 예약 정보 엔티티
 * 항공권, 숙소, 교통 등 모든 예약 정보 관리
 * OCR을 통한 자동 추출 정보 포함
 */
@Getter
@Entity
@Table(name = "reservations", indexes = {
    @Index(name = "idx_reservation_user", columnList = "user_id"),
    @Index(name = "idx_reservation_trip", columnList = "trip_id"),
    @Index(name = "idx_reservation_type", columnList = "type"),
    @Index(name = "idx_reservation_dates", columnList = "departure_time,arrival_time")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Reservation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id")
    private Trip trip;  // Nullable - 여행 계획과 연결되지 않은 예약도 가능

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ReservationType type;

    @Column(name = "confirmation_number", length = 100)
    private String confirmationNumber;  // 예약 확인 번호

    @Column(length = 100)
    private String provider;  // 대한항공, 아시아나, 신라호텔 등

    // 항공/교통 관련
    @Column(name = "departure_location", length = 200)
    private String departureLocation;

    @Column(name = "arrival_location", length = 200)
    private String arrivalLocation;

    @Column(name = "departure_time")
    private LocalDateTime departureTime;

    @Column(name = "arrival_time")
    private LocalDateTime arrivalTime;

    @Column(name = "flight_number", length = 20)
    private String flightNumber;  // 항공편명

    @Column(name = "seat_number", length = 20)
    private String seatNumber;    // 좌석번호

    // 숙소 관련
    @Column(name = "accommodation_name", length = 200)
    private String accommodationName;

    @Column(name = "room_type", length = 100)
    private String roomType;

    @Column(name = "check_in_date")
    private LocalDate checkInDate;

    @Column(name = "check_out_date")
    private LocalDate checkOutDate;

    @Column(name = "number_of_nights")
    private Integer numberOfNights;

    // 공통 정보
    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(length = 10)
    private String currency;  // KRW, USD, EUR 등

    @Column(name = "number_of_passengers")
    private Integer numberOfPassengers;  // 탑승객/투숙객 수

    @Column(name = "reservation_status", length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.CONFIRMED;

    // OCR 관련
    @Column(name = "ocr_extracted")
    @Builder.Default
    private Boolean ocrExtracted = false;  // OCR로 추출된 정보인지

    @Column(name = "original_image_url", length = 500)
    private String originalImageUrl;  // OCR 원본 이미지 URL

    @Column(name = "ocr_confidence")
    private Double ocrConfidence;  // OCR 인식 신뢰도 (0.0 ~ 1.0)

    // JSON 형태의 전체 예약 상세 정보
    @Column(name = "reservation_details", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String reservationDetails;

    @Column(columnDefinition = "TEXT")
    private String notes;  // 메모

    // 편의 메서드
    public void linkToTrip(Trip trip) {
        this.trip = trip;
    }

    public void updateStatus(ReservationStatus status) {
        this.status = status;
    }

    public void markAsOcrExtracted(String imageUrl, Double confidence) {
        this.ocrExtracted = true;
        this.originalImageUrl = imageUrl;
        this.ocrConfidence = confidence;
    }

    // Enum 정의
    public enum ReservationType {
        FLIGHT,         // 항공
        HOTEL,          // 호텔
        TRAIN,          // 기차
        BUS,            // 버스
        RENTAL_CAR,     // 렌터카
        RESTAURANT,     // 레스토랑
        ACTIVITY,       // 액티비티
        OTHER           // 기타
    }

    public enum ReservationStatus {
        PENDING,        // 대기중
        CONFIRMED,      // 확정
        CANCELLED,      // 취소됨
        COMPLETED,      // 완료됨
        EXPIRED         // 만료됨
    }
}