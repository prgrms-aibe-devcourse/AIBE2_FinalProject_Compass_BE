package com.compass.domain.chat.function;

import com.compass.domain.chat.function.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Function Calling models and functions
 * These tests do not require Spring context
 */
public class FunctionCallingUnitTest {

    @Test
    @DisplayName("searchFlights function should return mock flight data")
    void testSearchFlightsFunction() {
        // Given
        TravelFunctions.SearchFlights searchFlights = new TravelFunctions.SearchFlights();
        FlightSearchRequest request = new FlightSearchRequest(
            "Seoul",
            "Tokyo",
            LocalDate.of(2024, 12, 25),
            null,
            2,
            "economy"
        );
        
        // When
        FlightSearchResponse response = searchFlights.apply(request);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.origin()).isEqualTo("Seoul");
        assertThat(response.destination()).isEqualTo("Tokyo");
        assertThat(response.flights()).isNotEmpty();
        assertThat(response.flights()).hasSize(2);
        assertThat(response.flights().get(0).airline()).isNotBlank();
        assertThat(response.flights().get(0).price()).isPositive();
    }

    @Test
    @DisplayName("searchHotels function should return mock hotel data")
    void testSearchHotelsFunction() {
        // Given
        TravelFunctions.SearchHotels searchHotels = new TravelFunctions.SearchHotels();
        HotelSearchRequest request = new HotelSearchRequest(
            "Seoul",
            LocalDate.of(2024, 12, 25),
            LocalDate.of(2024, 12, 30),
            2,
            1,
            100000,
            200000,
            4,
            List.of("WiFi", "Breakfast")
        );
        
        // When
        HotelSearchResponse response = searchHotels.apply(request);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.location()).isEqualTo("Seoul");
        assertThat(response.hotels()).isNotEmpty();
        assertThat(response.hotels().get(0).name()).isNotBlank();
        assertThat(response.hotels().get(0).pricePerNight()).isPositive();
        assertThat(response.hotels().get(0).amenities()).contains("WiFi");
    }

    @Test
    @DisplayName("getWeather function should return weather data")
    void testGetWeatherFunction() {
        // Given
        TravelFunctions.GetWeather getWeather = new TravelFunctions.GetWeather();
        WeatherRequest request = new WeatherRequest(
            "Seoul",
            LocalDate.now(),
            "celsius"
        );
        
        // When
        WeatherResponse response = getWeather.apply(request);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.location()).isEqualTo("Seoul");
        assertThat(response.temperature()).isNotNull();
        assertThat(response.description()).isNotBlank();
        assertThat(response.humidity()).isBetween(0, 100);
    }

    @Test
    @DisplayName("searchAttractions function should return attraction data")
    void testSearchAttractionsFunction() {
        // Given
        TravelFunctions.SearchAttractions searchAttractions = new TravelFunctions.SearchAttractions();
        AttractionSearchRequest request = new AttractionSearchRequest(
            "Seoul",
            List.of("Historical Site", "Museum", "Park"),
            10.0,
            4.0,
            5,
            "rating"
        );
        
        // When
        AttractionSearchResponse response = searchAttractions.apply(request);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.location()).isEqualTo("Seoul");
        assertThat(response.attractions()).isNotEmpty();
        assertThat(response.attractions().get(0).name()).isNotBlank();
        assertThat(response.attractions().get(0).rating()).isPositive();
        assertThat(response.attractions().get(0).category()).isNotBlank();
    }

    @Test
    @DisplayName("Function request models should have proper default values")
    void testRequestModelDefaults() {
        // Given & When
        FlightSearchRequest flightRequest = new FlightSearchRequest(
            "Seoul", "Tokyo", LocalDate.now(), null, null, null
        );
        
        HotelSearchRequest hotelRequest = new HotelSearchRequest(
            "Seoul", LocalDate.now(), LocalDate.now().plusDays(1), 
            null, null, null, null, null, null
        );
        
        WeatherRequest weatherRequest = new WeatherRequest(
            "Seoul", null, null
        );
        
        AttractionSearchRequest attractionRequest = new AttractionSearchRequest(
            "Seoul", null, null, null, null, null
        );
        
        // Then
        assertThat(flightRequest.passengers()).isEqualTo(1);
        assertThat(flightRequest.travelClass()).isEqualTo("economy");
        
        assertThat(hotelRequest.guests()).isEqualTo(2);
        assertThat(hotelRequest.rooms()).isEqualTo(1);
        
        assertThat(weatherRequest.unit()).isEqualTo("celsius");
        
        assertThat(attractionRequest.maxResults()).isEqualTo(10);
        assertThat(attractionRequest.sortBy()).isEqualTo("rating");
    }

    @Test
    @DisplayName("FlightSearchResponse should build correctly with all fields")
    void testFlightSearchResponseBuilder() {
        // Given & When
        FlightSearchResponse response = FlightSearchResponse.builder()
            .origin("Seoul")
            .destination("Tokyo")
            .departureDate(LocalDate.of(2024, 12, 25))
            .flights(List.of(
                FlightSearchResponse.Flight.builder()
                    .airline("Korean Air")
                    .flightNumber("KE001")
                    .departureTime("08:00")
                    .arrivalTime("10:30")
                    .price(450000)
                    .currency("KRW")
                    .build()
            ))
            .build();
        
        // Then
        assertThat(response.origin()).isEqualTo("Seoul");
        assertThat(response.destination()).isEqualTo("Tokyo");
        assertThat(response.departureDate()).isEqualTo(LocalDate.of(2024, 12, 25));
        assertThat(response.flights()).hasSize(1);
        assertThat(response.flights().get(0).airline()).isEqualTo("Korean Air");
    }

    @Test
    @DisplayName("HotelSearchResponse should build correctly with all fields")
    void testHotelSearchResponseBuilder() {
        // Given & When
        HotelSearchResponse response = HotelSearchResponse.builder()
            .location("Seoul")
            .checkIn(LocalDate.of(2024, 12, 25))
            .checkOut(LocalDate.of(2024, 12, 30))
            .hotels(List.of(
                HotelSearchResponse.Hotel.builder()
                    .name("Grand Hotel Seoul")
                    .rating(4.5)
                    .pricePerNight(150000)
                    .currency("KRW")
                    .amenities(List.of("WiFi", "Breakfast"))
                    .address("123 Gangnam-daero, Seoul")
                    .build()
            ))
            .build();
        
        // Then
        assertThat(response.location()).isEqualTo("Seoul");
        assertThat(response.checkIn()).isEqualTo(LocalDate.of(2024, 12, 25));
        assertThat(response.checkOut()).isEqualTo(LocalDate.of(2024, 12, 30));
        assertThat(response.hotels()).hasSize(1);
        assertThat(response.hotels().get(0).name()).isEqualTo("Grand Hotel Seoul");
        assertThat(response.hotels().get(0).rating()).isEqualTo(4.5);
    }
}