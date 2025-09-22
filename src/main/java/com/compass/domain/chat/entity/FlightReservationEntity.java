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
                @Index(name = "idx_flight_reservation_booking", columnList = "booking_reference", unique = true),
                @Index(name = "idx_flight_reservation_thread", columnList = "thread_id"),
                @Index(name = "idx_flight_reservation_flight_departure", columnList = "flight_number, departure_time")
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

    @NotNull
    @Column(name = "departure_time", nullable = false)
    private LocalDateTime departureTime;

    @NotNull
    @Column(name = "arrival_time", nullable = false)
    private LocalDateTime arrivalTime;

    @NotNull
    @Column(name = "passenger_name", nullable = false)
    private String passengerName;

    @NotNull
    @Column(name = "seat_number", nullable = false)
    private String seatNumber;

    @NotNull
    @Column(name = "booking_reference", nullable = false)
    private String bookingReference;
}
