package com.compass.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.compass.domain.chat.model.dto.HotelReservation;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(HotelReservationService.class)
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

    @Test
    @DisplayName("체크인 날짜가 없으면 예외가 발생한다")
    void saveReservationFailsWhenCheckInMissing() {
        var reservation = new HotelReservation(
                "Grand Hotel",
                "123 Main St, City",
                null,
                LocalDate.of(2024, 3, 17),
                "Deluxe Room",
                2,
                "CONF12345",
                500.0,
                2,
                37.5665,
                126.9780
        );

        assertThatThrownBy(() -> hotelReservationService.save("thread-1", "user-1", reservation))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("체크인");
    }

    @Test
    @DisplayName("체크아웃이 체크인보다 빠르면 예외가 발생한다")
    void saveReservationFailsWhenCheckoutBeforeCheckin() {
        var reservation = new HotelReservation(
                "Grand Hotel",
                "123 Main St, City",
                LocalDate.of(2024, 3, 17),
                LocalDate.of(2024, 3, 16),
                "Deluxe Room",
                2,
                "CONF12345",
                500.0,
                2,
                37.5665,
                126.9780
        );

        assertThatThrownBy(() -> hotelReservationService.save("thread-1", "user-1", reservation))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("체크아웃");
    }
}
