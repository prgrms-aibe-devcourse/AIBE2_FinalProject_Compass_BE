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
            "coffee",  // cafeType: 'coffee', 'dessert', 'brunch', 'study'
            "moderate" // priceRange: 'budget', 'moderate', 'premium'
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
            "korean",  // cuisineType
            "fine_dining",  // priceRange
            "dinner"  // mealType
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
            "sports",  // activityType
            "moderate"  // priceRange
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
            "traditional",  // experienceType
            "half-day",  // duration
            "moderate"  // priceRange
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
            "art",  // exhibitionType
            "moderate"  // entryFeeRange
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
            "Seoul", null, null
        );
        
        RestaurantSearchRequest restaurantRequest = new RestaurantSearchRequest(
            "Seoul", null, null, null
        );
        
        LeisureActivityRequest activityRequest = new LeisureActivityRequest(
            "Seoul", null, null
        );
        
        CulturalExperienceRequest experienceRequest = new CulturalExperienceRequest(
            "Seoul", null, null, null
        );
        
        ExhibitionSearchRequest exhibitionRequest = new ExhibitionSearchRequest(
            "Seoul", null, null
        );
        
        // Then
        assertThat(cafeRequest.cafeType()).isEqualTo("coffee");
        assertThat(cafeRequest.priceRange()).isEqualTo("moderate");
        
        assertThat(restaurantRequest.cuisineType()).isEqualTo("all");
        assertThat(restaurantRequest.priceRange()).isEqualTo("moderate");
        assertThat(restaurantRequest.mealType()).isEqualTo("all");
        
        assertThat(activityRequest.activityType()).isEqualTo("all");
        assertThat(activityRequest.priceRange()).isEqualTo("moderate");
        
        assertThat(experienceRequest.experienceType()).isEqualTo("traditional");
        assertThat(experienceRequest.duration()).isEqualTo("half-day");
        assertThat(experienceRequest.priceRange()).isEqualTo("moderate");
        
        assertThat(exhibitionRequest.exhibitionType()).isEqualTo("all");
        assertThat(exhibitionRequest.entryFeeRange()).isEqualTo("moderate");
    }
}