package com.compass.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.compass.domain.chat.model.dto.HotelReservation;
import java.time.LocalDate;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@Import(HotelReservationService.class)
@ActiveProfiles("test")
@Disabled("Spring Context 로드 문제 해결 필요")
class HotelReservationServiceTest {

    @Autowired
    private HotelReservationService hotelReservationService;

    @Test
    @DisplayName("호텔 예약 정보가 정상적으로 저장된다")
    void saveReservation() {
        var reservation = new HotelReservation(
                "Grand Hotel",
                "123 Main St, City",
                LocalDate.of(2024, 3, 15),
                LocalDate.of(2024, 3, 17),
                "Deluxe Room",
                2,
                "CONF12345",
                500.0,
                2,
                37.5665,
                126.9780
        );

        var saved = hotelReservationService.save("thread-1", "user-1", reservation);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getHotelName()).isEqualTo("Grand Hotel");
        assertThat(saved.getThreadId()).isEqualTo("thread-1");
        assertThat(saved.getNumberOfGuests()).isEqualTo(2);
    }
}
