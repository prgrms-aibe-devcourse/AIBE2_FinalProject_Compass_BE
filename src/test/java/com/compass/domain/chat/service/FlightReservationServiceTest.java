package com.compass.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.compass.domain.chat.model.dto.FlightReservation;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(FlightReservationService.class)
class FlightReservationServiceTest {

    @Autowired
    private FlightReservationService flightReservationService;

    @Test
    @DisplayName("항공권 예약 정보가 정상적으로 저장된다")
    void saveReservation() {
        var reservation = new FlightReservation(
                "KE123",
                "ICN",
                "NRT",
                LocalDateTime.of(2024, 3, 15, 14, 30),
                LocalDateTime.of(2024, 3, 15, 17, 0),
                "홍길동",
                "12A",
                "ABC123"
        );

        var saved = flightReservationService.save("thread-1", "user-1", reservation);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getFlightNumber()).isEqualTo("KE123");
        assertThat(saved.getSeatNumber()).isEqualTo("12A");
        assertThat(saved.getThreadId()).isEqualTo("thread-1");
    }
}
