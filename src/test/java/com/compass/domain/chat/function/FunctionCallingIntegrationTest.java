package com.compass.domain.chat.function;

import com.compass.config.IntegrationTest;
import com.compass.domain.chat.function.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Spring AI Function Calling
 * Note: This test validates that functions are properly registered as beans.
 * Actual LLM integration requires API keys and network access.
 */
@IntegrationTest
public class FunctionCallingIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("searchFlights function should be registered as a Spring bean")
    void testSearchFlightsFunctionRegistration() {
        // Given & When
        Function<FlightSearchRequest, FlightSearchResponse> searchFlights = 
            applicationContext.getBean("searchFlights", Function.class);
        
        // Then
        assertThat(searchFlights).isNotNull();
        assertThat(searchFlights).isInstanceOf(TravelFunctions.SearchFlights.class);
    }

    @Test
    @DisplayName("searchFlights function should return mock flight data")
    void testSearchFlightsFunction() {
        // Given
        Function<FlightSearchRequest, FlightSearchResponse> searchFlights = 
            applicationContext.getBean("searchFlights", Function.class);
        
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
    @DisplayName("searchHotels function should be registered and functional")
    void testSearchHotelsFunction() {
        // Given
        Function<HotelSearchRequest, HotelSearchResponse> searchHotels = 
            applicationContext.getBean("searchHotels", Function.class);
        
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
        Function<WeatherRequest, WeatherResponse> getWeather = 
            applicationContext.getBean("getWeather", Function.class);
        
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
        Function<AttractionSearchRequest, AttractionSearchResponse> searchAttractions = 
            applicationContext.getBean("searchAttractions", Function.class);
        
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
    @DisplayName("All travel functions should be available in the application context")
    void testAllFunctionsRegistered() {
        // Given & When
        boolean hasSearchFlights = applicationContext.containsBean("searchFlights");
        boolean hasSearchHotels = applicationContext.containsBean("searchHotels");
        boolean hasGetWeather = applicationContext.containsBean("getWeather");
        boolean hasSearchAttractions = applicationContext.containsBean("searchAttractions");
        
        // Then
        assertThat(hasSearchFlights).isTrue();
        assertThat(hasSearchHotels).isTrue();
        assertThat(hasGetWeather).isTrue();
        assertThat(hasSearchAttractions).isTrue();
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
}