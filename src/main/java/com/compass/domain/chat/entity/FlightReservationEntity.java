package com.compass.domain.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@Entity
@Table(name = "flight_reservations",
        indexes = {
                @Index(name = "idx_flight_reservation_booking", columnList = "booking_reference"),
                @Index(name = "idx_flight_reservation_thread", columnList = "thread_id")
        })
public class FlightReservationEntity {

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
    @Column(name = "flight_number", nullable = false)
    private String flightNumber;

    @NotNull
    @Column(name = "departure_airport", nullable = false)
    private String departureAirport;

    @NotNull
    @Column(name = "arrival_airport", nullable = false)
    private String arrivalAirport;

    @Column(name = "departure_time")
    private LocalDateTime departureDateTime;

    @Column(name = "arrival_time")
    private LocalDateTime arrivalDateTime;

    @Column(name = "passenger_name")
    private String passengerName;

    @Column(name = "seat_number")
    private String seatNumber;

    @Column(name = "booking_reference")
    private String bookingReference;
}
