package com.compass.domain.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
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
                @Index(name = "idx_hotel_reservation_confirmation", columnList = "confirmation_number"),
                @Index(name = "idx_hotel_reservation_thread", columnList = "thread_id")
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

    @Column(name = "check_in_date")
    private LocalDate checkInDate;

    @Column(name = "check_out_date")
    private LocalDate checkOutDate;

    @Column(name = "room_type")
    private String roomType;

    @Column(name = "guests")
    private Integer numberOfGuests;

    @Column(name = "confirmation_number")
    private String confirmationNumber;

    @Column(name = "total_price")
    private Double totalPrice;

    @Column(name = "nights")
    private Integer nights;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;
}
