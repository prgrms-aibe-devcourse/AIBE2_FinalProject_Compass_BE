package com.compass.domain.chat.function;

import com.compass.domain.chat.function.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for extended travel function calling features
 * Tests for cafes, restaurants, leisure, cultural experiences, and exhibitions
 */
public class ExtendedFunctionCallingUnitTest {

    @Test
    @DisplayName("searchCafes function should return mock cafe data")
    void testSearchCafesFunction() {
        // Given
        TravelFunctions.SearchCafes searchCafes = new TravelFunctions.SearchCafes();
        CafeSearchRequest request = new CafeSearchRequest(
            "Seoul",
            "coffee_shop",
            List.of("quiet", "modern"),
            List.of("wifi", "power_outlets"),
            "moderate",
            5.0,
            4.0,
            10,
            "rating"
        );
        
        // When
        CafeSearchResponse response = searchCafes.apply(request);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.location()).isEqualTo("Seoul");
        assertThat(response.cafes()).isNotEmpty();
        assertThat(response.cafes()).hasSize(2);
        
        CafeSearchResponse.Cafe firstCafe = response.cafes().get(0);
        assertThat(firstCafe.name()).isEqualTo("Blue Bottle Coffee Gangnam");
        assertThat(firstCafe.type()).isEqualTo("coffee_shop");
        assertThat(firstCafe.rating()).isEqualTo(4.6);
        assertThat(firstCafe.amenities()).contains("wifi");
        assertThat(firstCafe.specialties()).isNotEmpty();
    }

    @Test
    @DisplayName("searchRestaurants function should return mock restaurant data with Michelin stars")
    void testSearchRestaurantsFunction() {
        // Given
        TravelFunctions.SearchRestaurants searchRestaurants = new TravelFunctions.SearchRestaurants();
        RestaurantSearchRequest request = new RestaurantSearchRequest(
            "Seoul",
            List.of("korean", "modern"),
            "dinner",
            "fine_dining",
            null,
            List.of("view", "private_room"),
            List.of("elegant"),
            2,
            5.0,
            4.5,
            true,
            10,
            "rating"
        );
        
        // When
        RestaurantSearchResponse response = searchRestaurants.apply(request);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.location()).isEqualTo("Seoul");
        assertThat(response.restaurants()).isNotEmpty();
        
        RestaurantSearchResponse.Restaurant mingles = response.restaurants().get(0);
        assertThat(mingles.name()).isEqualTo("Mingles");
        assertThat(mingles.michelinStars()).isEqualTo(2);
        assertThat(mingles.chefName()).isEqualTo("Mingoo Kang");
        assertThat(mingles.cuisineTypes()).contains("korean", "modern");
        assertThat(mingles.priceRange()).isEqualTo("fine_dining");
        assertThat(mingles.reservationRequired()).isTrue();
    }

    @Test
    @DisplayName("searchLeisureActivities function should return outdoor and wellness activities")
    void testSearchLeisureActivitiesFunction() {
        // Given
        TravelFunctions.SearchLeisureActivities searchActivities = new TravelFunctions.SearchLeisureActivities();
        LeisureActivityRequest request = new LeisureActivityRequest(
            "Seoul",
            List.of("sports", "wellness"),
            List.of("cycling", "spa"),
            "all_levels",
            "3_6",
            "adults",
            "couple",
            "both",
            "moderate",
            true,
            true,
            10.0,
            4.0,
            10,
            "rating"
        );
        
        // When
        LeisureActivityResponse response = searchActivities.apply(request);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.location()).isEqualTo("Seoul");
        assertThat(response.activities()).hasSize(2);
        
        LeisureActivityResponse.Activity bikeActivity = response.activities().get(0);
        assertThat(bikeActivity.name()).contains("Han River");
        assertThat(bikeActivity.type()).isEqualTo("sports");
        assertThat(bikeActivity.equipmentProvided()).isTrue();
        assertThat(bikeActivity.equipmentIncluded()).contains("bike", "helmet");
        assertThat(bikeActivity.languages()).contains("english");
        
        LeisureActivityResponse.Activity spaActivity = response.activities().get(1);
        assertThat(spaActivity.name()).contains("Dragon Hill Spa");
        assertThat(spaActivity.type()).isEqualTo("wellness");
        assertThat(spaActivity.environment()).isEqualTo("indoor");
    }

    @Test
    @DisplayName("searchCulturalExperiences function should return traditional and culinary experiences")
    void testSearchCulturalExperiencesFunction() {
        // Given
        TravelFunctions.SearchCulturalExperiences searchExperiences = new TravelFunctions.SearchCulturalExperiences();
        CulturalExperienceRequest request = new CulturalExperienceRequest(
            "Seoul",
            List.of("traditional", "culinary"),
            List.of("tea_ceremony", "cooking_class"),
            List.of("traditions", "cuisine"),
            "1_3_hours",
            "hands_on",
            List.of("english", "korean"),
            "small_group",
            "authentic",
            true,
            true,
            "moderate",
            false,
            5.0,
            4.5,
            10,
            "rating"
        );
        
        // When
        CulturalExperienceResponse response = searchExperiences.apply(request);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.location()).isEqualTo("Seoul");
        assertThat(response.experiences()).hasSize(2);
        
        CulturalExperienceResponse.Experience teaCeremony = response.experiences().get(0);
        assertThat(teaCeremony.name()).contains("Tea Ceremony");
        assertThat(teaCeremony.type()).isEqualTo("traditional");
        assertThat(teaCeremony.authenticityLevel()).isEqualTo("highly_authentic");
        assertThat(teaCeremony.certificateProvided()).isTrue();
        assertThat(teaCeremony.takeaways()).contains("tea samples");
        
        CulturalExperienceResponse.Experience kimchiClass = response.experiences().get(1);
        assertThat(kimchiClass.name()).contains("Kimchi Making");
        assertThat(kimchiClass.type()).isEqualTo("culinary");
        assertThat(kimchiClass.participationLevel()).isEqualTo("hands_on");
        assertThat(kimchiClass.takeaways()).contains("homemade kimchi");
    }

    @Test
    @DisplayName("searchExhibitions function should return art and history exhibitions")
    void testSearchExhibitionsFunction() {
        // Given
        TravelFunctions.SearchExhibitions searchExhibitions = new TravelFunctions.SearchExhibitions();
        ExhibitionSearchRequest request = new ExhibitionSearchRequest(
            "Seoul",
            List.of("art", "history"),
            List.of("digital", "traditional"),
            List.of("temporary", "permanent"),
            LocalDate.now(),
            LocalDate.now().plusMonths(3),
            List.of("museum", "gallery"),
            "general",
            List.of("Monet"),
            List.of("english", "korean"),
            List.of("wheelchair_access"),
            true,
            true,
            "moderate",
            false,
            10.0,
            4.0,
            10,
            "popularity"
        );
        
        // When
        ExhibitionSearchResponse response = searchExhibitions.apply(request);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.location()).isEqualTo("Seoul");
        assertThat(response.exhibitions()).hasSize(2);
        
        ExhibitionSearchResponse.Exhibition monetExhibit = response.exhibitions().get(0);
        assertThat(monetExhibit.name()).contains("Monet");
        assertThat(monetExhibit.type()).isEqualTo("art");
        assertThat(monetExhibit.interactiveElements()).isTrue();
        assertThat(monetExhibit.photographyAllowed()).isTrue();
        assertThat(monetExhibit.languagesAvailable()).contains("english", "korean");
        
        ExhibitionSearchResponse.Exhibition joseonExhibit = response.exhibitions().get(1);
        assertThat(joseonExhibit.name()).contains("Joseon");
        assertThat(joseonExhibit.type()).isEqualTo("history");
        assertThat(joseonExhibit.entryFee()).isEqualTo(0);
        assertThat(joseonExhibit.venue()).contains("National Museum");
    }

    @Test
    @DisplayName("Request models should have proper default values")
    void testRequestModelDefaults() {
        // Given & When
        CafeSearchRequest cafeRequest = new CafeSearchRequest(
            "Seoul", null, null, null, null, null, null, null, null
        );
        
        RestaurantSearchRequest restaurantRequest = new RestaurantSearchRequest(
            "Seoul", null, null, null, null, null, null, null, null, null, null, null, null
        );
        
        LeisureActivityRequest activityRequest = new LeisureActivityRequest(
            "Seoul", null, null, null, null, null, null, null, null, null, null, null, null, null, null
        );
        
        CulturalExperienceRequest experienceRequest = new CulturalExperienceRequest(
            "Seoul", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null
        );
        
        ExhibitionSearchRequest exhibitionRequest = new ExhibitionSearchRequest(
            "Seoul", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null
        );
        
        // Then
        assertThat(cafeRequest.cafeType()).isEqualTo("coffee_shop");
        assertThat(cafeRequest.priceRange()).isEqualTo("moderate");
        assertThat(cafeRequest.maxResults()).isEqualTo(10);
        assertThat(cafeRequest.sortBy()).isEqualTo("rating");
        
        assertThat(restaurantRequest.priceRange()).isEqualTo("moderate");
        assertThat(restaurantRequest.maxResults()).isEqualTo(10);
        assertThat(restaurantRequest.sortBy()).isEqualTo("rating");
        
        assertThat(activityRequest.difficultyLevel()).isEqualTo("all_levels");
        assertThat(activityRequest.ageGroup()).isEqualTo("adults");
        assertThat(activityRequest.environment()).isEqualTo("both");
        
        assertThat(experienceRequest.duration()).isEqualTo("1_3_hours");
        assertThat(experienceRequest.participationLevel()).isEqualTo("hands_on");
        assertThat(experienceRequest.authenticityLevel()).isEqualTo("authentic");
        
        assertThat(exhibitionRequest.entryFeeRange()).isEqualTo("moderate");
        assertThat(exhibitionRequest.sortBy()).isEqualTo("popularity");
    }
}