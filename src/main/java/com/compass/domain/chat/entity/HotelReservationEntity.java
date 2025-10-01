package com.compass.domain.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@Entity
@Table(name = "hotel_reservations",
        indexes = {
                @Index(name = "idx_hotel_reservation_confirmation", columnList = "confirmation_number", unique = true),
                @Index(name = "idx_hotel_reservation_thread", columnList = "thread_id"),
                @Index(name = "idx_hotel_reservation_hotel_checkin", columnList = "hotel_name, check_in_date")
        })
public class HotelReservationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "thread_id", nullable = false)
    private String threadId;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private String userId;

    @NotNull
    @Column(name = "hotel_name", nullable = false)
    private String hotelName;

    @Column(name = "address")
    private String address;

    @NotNull
    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;

    @Column(name = "check_in_time")
    private java.time.LocalTime checkInTime;

    @NotNull
    @Column(name = "check_out_date", nullable = false)
    private LocalDate checkOutDate;

    @Column(name = "check_out_time")
    private java.time.LocalTime checkOutTime;

    @Column(name = "room_type")
    private String roomType;

    @NotNull
    @Min(1)
    @Column(name = "guests", nullable = false)
    private Integer numberOfGuests;

    @NotNull
    @Column(name = "confirmation_number", nullable = false, unique = true)
    private String confirmationNumber;

    @NotNull
    @PositiveOrZero
    @Column(name = "total_price", nullable = false)
    private Double totalPrice;

    @Column(name = "nights")
    private Integer nights;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "guest_name")
    private String guestName;

    @Column(name = "phone")
    private String phone;

    // 숙박 기간의 유효성을 검증한다
    @PrePersist
    @PreUpdate
    private void validateStayPeriod() {
        if (checkInDate == null || checkOutDate == null) {
            throw new IllegalStateException("Check-in and check-out dates are required");
        }
        if (checkOutDate.isBefore(checkInDate)) {
            throw new IllegalStateException("Check-out date must be on or after check-in date");
        }
    }
}
