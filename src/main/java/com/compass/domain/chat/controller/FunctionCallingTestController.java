package com.compass.domain.chat.controller;

import com.compass.domain.chat.service.FunctionCallingChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test controller for Function Calling with Spring AI
 * This controller provides endpoints to test Gemini and GPT-4 with function calling
 */
@Slf4j
@RestController
@RequestMapping("/api/test/function")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "spring.ai", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FunctionCallingTestController {
    
    private final FunctionCallingChatService functionCallingService;
    
    /**
     * Test natural language travel request processing with automatic parsing
     * Example: "성수로 당일치기 여행 가고 싶어"
     */
    @PostMapping("/travel/natural")
    public Map<String, Object> testNaturalLanguageTravel(@RequestBody Map<String, String> request) {
        log.info("=== Testing Natural Language Travel Request ===");
        String userMessage = request.get("message");
        log.info("User message: {}", userMessage);
        
        Map<String, Object> response = new HashMap<>();
        try {
            String result = functionCallingService.processTravelRequest(userMessage);
            response.put("success", true);
            response.put("response", result);
            response.put("model", "gemini-2.0-flash");
            log.info("Successfully processed travel request");
        } catch (Exception e) {
            log.error("Error processing travel request", e);
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        
        return response;
    }
    
    /**
     * Test Gemini with function calling
     * Example: "What's the weather like in Seoul today?"
     */
    @PostMapping("/gemini")
    public Map<String, Object> testGeminiFunctionCalling(@RequestBody Map<String, String> request) {
        log.info("=== Testing Gemini Function Calling ===");
        String userMessage = request.get("message");
        log.info("User message: {}", userMessage);
        
        Map<String, Object> response = new HashMap<>();
        try {
            String result = functionCallingService.chatWithFunctionsGemini(userMessage, null);
            response.put("success", true);
            response.put("response", result);
            response.put("model", "gemini-2.0-flash");
            log.info("Gemini function calling successful");
        } catch (Exception e) {
            log.error("Error with Gemini function calling", e);
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        
        return response;
    }
    
    /**
     * Test OpenAI with function calling
     * Example: "Find flights from Seoul to Tokyo"
     */
    @PostMapping("/openai")
    public Map<String, Object> testOpenAIFunctionCalling(@RequestBody Map<String, String> request) {
        log.info("=== Testing OpenAI Function Calling ===");
        String userMessage = request.get("message");
        log.info("User message: {}", userMessage);
        
        Map<String, Object> response = new HashMap<>();
        try {
            String result = functionCallingService.chatWithFunctionsOpenAI(userMessage, null);
            response.put("success", true);
            response.put("response", result);
            response.put("model", "gpt-4o-mini");
            log.info("OpenAI function calling successful");
        } catch (Exception e) {
            log.error("Error with OpenAI function calling", e);
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        
        return response;
    }
    
    /**
     * Test planned trip with specific dates and preferences
     */
    @PostMapping("/trip/plan")
    public Map<String, Object> testPlannedTrip(@RequestBody Map<String, Object> request) {
        log.info("=== Testing Planned Trip with Functions ===");
        
        String destination = (String) request.get("destination");
        String startDate = (String) request.get("startDate");
        String endDate = (String) request.get("endDate");
        Map<String, Object> preferences = (Map<String, Object>) request.getOrDefault("preferences", new HashMap<>());
        
        log.info("Planning trip to {} from {} to {}", destination, startDate, endDate);
        
        Map<String, Object> response = new HashMap<>();
        try {
            String result = functionCallingService.planTripWithFunctions(
                destination, startDate, endDate, preferences
            );
            response.put("success", true);
            response.put("response", result);
            response.put("destination", destination);
            response.put("dates", startDate + " ~ " + endDate);
            log.info("Trip planning successful");
        } catch (Exception e) {
            log.error("Error planning trip", e);
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        
        return response;
    }
    
    /**
     * Test available functions - shows which functions are registered
     */
    @GetMapping("/available")
    public Map<String, Object> getAvailableFunctions() {
        log.info("=== Listing Available Functions ===");
        
        Map<String, Object> response = new HashMap<>();
        response.put("functions", List.of(
            "searchFlights - Search for flights between cities",
            "searchHotels - Find hotels in a destination",
            "getWeather - Get weather information",
            "searchAttractions - Find tourist attractions",
            "searchRestaurants - Find restaurants",
            "searchCafes - Find cafes",
            "searchExhibitions - Find exhibitions and museums",
            "searchCulturalExperiences - Find cultural experiences",
            "searchLeisureActivities - Find leisure activities"
        ));
        response.put("models", Map.of(
            "gemini", "gemini-2.0-flash",
            "openai", "gpt-4o-mini"
        ));
        
        return response;
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "FunctionCallingChatService");
        response.put("enabled", true);
        return response;
    }
}