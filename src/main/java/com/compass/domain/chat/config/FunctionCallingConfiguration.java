package com.compass.domain.chat.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.model.function.FunctionCallbackWrapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Function;

/**
 * Configuration for Spring AI Function Calling
 * This class registers functions that can be called by the LLM during conversation
 *
 * NOTE: 이 설정의 모든 Function들은 MainLLMOrchestrator (CHAT2 도메인)에 의해
 * 자동으로 수집되어 통합 관리됩니다.
 *
 * @see com.compass.domain.chat2.orchestrator.MainLLMOrchestrator
 * @see com.compass.domain.chat2.config.OrchestratorConfiguration
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "spring.ai", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FunctionCallingConfiguration {

    /**
     * Function Callback for flight search
     * This bean wraps the searchFlights function and makes it available to the LLM
     */
    @Bean
    public FunctionCallback searchFlightsFunctionCallback(ApplicationContext context) {
        return FunctionCallbackWrapper.builder(context.getBean("searchFlights", Function.class))
            .withName("searchFlights")
            .withDescription("Search for available flights between two locations on specific dates")
            .build();
    }

    /**
     * Function Callback for hotel search
     * This bean wraps the searchHotels function and makes it available to the LLM
     */
    @Bean
    public FunctionCallback searchHotelsFunctionCallback(ApplicationContext context) {
        return FunctionCallbackWrapper.builder(context.getBean("searchHotels", Function.class))
            .withName("searchHotels")
            .withDescription("Search for available hotels in a specific location with various filters")
            .build();
    }

    /**
     * Function Callback for weather information
     * This bean wraps the getWeather function and makes it available to the LLM
     */
    @Bean
    public FunctionCallback getWeatherFunctionCallback(ApplicationContext context) {
        return FunctionCallbackWrapper.builder(context.getBean("getWeather", Function.class))
            .withName("getWeather")
            .withDescription("Get current or forecasted weather information for a specific location")
            .build();
    }

    /**
     * Function Callback for attraction search
     * This bean wraps the searchAttractions function and makes it available to the LLM
     */
    @Bean
    public FunctionCallback searchAttractionsFunctionCallback(ApplicationContext context) {
        return FunctionCallbackWrapper.builder(context.getBean("searchAttractions", Function.class))
            .withName("searchAttractions")
            .withDescription("Search for tourist attractions and points of interest in a specific location")
            .build();
    }

    /**
     * Function Callback for cafe search
     * This bean wraps the searchCafes function and makes it available to the LLM
     */
    @Bean
    public FunctionCallback searchCafesFunctionCallback(ApplicationContext context) {
        return FunctionCallbackWrapper.builder(context.getBean("searchCafes", Function.class))
            .withName("searchCafes")
            .withDescription("Search for cafes with specific atmosphere and amenities in a location")
            .build();
    }

    /**
     * Function Callback for restaurant search
     * This bean wraps the searchRestaurants function and makes it available to the LLM
     */
    @Bean
    public FunctionCallback searchRestaurantsFunctionCallback(ApplicationContext context) {
        return FunctionCallbackWrapper.builder(context.getBean("searchRestaurants", Function.class))
            .withName("searchRestaurants")
            .withDescription("Search for restaurants with cuisine preferences and dietary restrictions")
            .build();
    }

    /**
     * Function Callback for leisure activity search
     * This bean wraps the searchLeisureActivities function and makes it available to the LLM
     */
    @Bean
    public FunctionCallback searchLeisureActivitiesFunctionCallback(ApplicationContext context) {
        return FunctionCallbackWrapper.builder(context.getBean("searchLeisureActivities", Function.class))
            .withName("searchLeisureActivities")
            .withDescription("Search for leisure activities, sports, and recreational options")
            .build();
    }

    /**
     * Function Callback for cultural experience search
     * This bean wraps the searchCulturalExperiences function and makes it available to the LLM
     */
    @Bean
    public FunctionCallback searchCulturalExperiencesFunctionCallback(ApplicationContext context) {
        return FunctionCallbackWrapper.builder(context.getBean("searchCulturalExperiences", Function.class))
            .withName("searchCulturalExperiences")
            .withDescription("Search for cultural experiences, traditional activities, and local customs")
            .build();
    }

    /**
     * Function Callback for exhibition search
     * This bean wraps the searchExhibitions function and makes it available to the LLM
     */
    @Bean
    public FunctionCallback searchExhibitionsFunctionCallback(ApplicationContext context) {
        return FunctionCallbackWrapper.builder(context.getBean("searchExhibitions", Function.class))
            .withName("searchExhibitions")
            .withDescription("Search for exhibitions, art shows, museums, and cultural events")
            .build();
    }

    /**
     * Log all registered functions on startup
     */
    @Bean
    public FunctionRegistryLogger functionRegistryLogger() {
        return new FunctionRegistryLogger();
    }

    /**
     * Helper class to log registered functions
     */
    public static class FunctionRegistryLogger {
        public FunctionRegistryLogger() {
            log.info("=== Spring AI Function Calling Initialized ===");
            log.info("Registered Travel Functions:");
            log.info("  ✈️  searchFlights: Search for flights between locations");
            log.info("  🏨 searchHotels: Search for accommodations");
            log.info("  ☀️  getWeather: Get weather information");
            log.info("  🏛️  searchAttractions: Find tourist attractions");
            log.info("  ☕ searchCafes: Find cafes and coffee shops");
            log.info("  🍽️  searchRestaurants: Find restaurants and dining options");
            log.info("  🎾 searchLeisureActivities: Find sports and recreational activities");
            log.info("  🎭 searchCulturalExperiences: Find cultural and traditional experiences");
            log.info("  🎨 searchExhibitions: Find exhibitions and art shows");
            log.info("=============================================");
            log.info("Total Functions Registered: 9");
            log.info("=============================================");
        }
    }
}