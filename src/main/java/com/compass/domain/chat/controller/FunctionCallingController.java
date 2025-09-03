package com.compass.domain.chat.controller;

import com.compass.domain.chat.dto.FunctionChatRequest;
import com.compass.domain.chat.dto.FunctionChatResponse;
import com.compass.domain.chat.dto.TripPlanningRequest;
import com.compass.domain.chat.dto.TripPlanningResponse;
import com.compass.domain.chat.service.FunctionCallingChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * REST Controller for chat operations with Function Calling capabilities
 * Provides endpoints for AI-powered travel planning with real-time data
 */
@Slf4j
@RestController
@RequestMapping("/api/chat/functions")
@RequiredArgsConstructor
@Tag(name = "Function Calling Chat", description = "Chat endpoints with function calling capabilities")
@ConditionalOnProperty(prefix = "spring.ai", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FunctionCallingController {

    private final FunctionCallingChatService functionCallingChatService;

    /**
     * Chat endpoint with OpenAI and function calling
     * 
     * @param request Chat request with user message
     * @return AI response with function call results if applicable
     */
    @PostMapping("/openai")
    @Operation(summary = "Chat with OpenAI using function calling", 
               description = "Send a message to OpenAI GPT-4 with automatic function calling for travel-related queries")
    public ResponseEntity<FunctionChatResponse> chatWithOpenAI(@Valid @RequestBody FunctionChatRequest request) {
        log.info("Received OpenAI chat request: {}", request.getMessage());
        
        try {
            String response = functionCallingChatService.chatWithFunctionsOpenAI(
                request.getMessage(), 
                request.getConversationHistory()
            );
            
            return ResponseEntity.ok(FunctionChatResponse.builder()
                .messageId(UUID.randomUUID().toString())
                .message(response)
                .model("gpt-4o-mini")
                .functionsUsed(true)
                .timestamp(LocalDateTime.now())
                .build());
        } catch (Exception e) {
            log.error("Error in OpenAI chat with functions", e);
            return ResponseEntity.internalServerError()
                .body(FunctionChatResponse.builder()
                    .messageId(UUID.randomUUID().toString())
                    .message("Error processing your request: " + e.getMessage())
                    .model("gpt-4o-mini")
                    .functionsUsed(false)
                    .timestamp(LocalDateTime.now())
                    .build());
        }
    }

    /**
     * Chat endpoint with Gemini and function calling
     * 
     * @param request Chat request with user message
     * @return AI response with function call results if applicable
     */
    @PostMapping("/gemini")
    @Operation(summary = "Chat with Gemini using function calling", 
               description = "Send a message to Gemini with automatic function calling for travel-related queries")
    public ResponseEntity<FunctionChatResponse> chatWithGemini(@Valid @RequestBody FunctionChatRequest request) {
        log.info("Received Gemini chat request: {}", request.getMessage());
        
        try {
            String response = functionCallingChatService.chatWithFunctionsGemini(
                request.getMessage(), 
                request.getConversationHistory()
            );
            
            return ResponseEntity.ok(FunctionChatResponse.builder()
                .messageId(UUID.randomUUID().toString())
                .message(response)
                .model("gemini-2.0-flash")
                .functionsUsed(true)
                .timestamp(LocalDateTime.now())
                .build());
        } catch (Exception e) {
            log.error("Error in Gemini chat with functions", e);
            return ResponseEntity.internalServerError()
                .body(FunctionChatResponse.builder()
                    .messageId(UUID.randomUUID().toString())
                    .message("Error processing your request: " + e.getMessage())
                    .model("gemini-2.0-flash")
                    .functionsUsed(false)
                    .timestamp(LocalDateTime.now())
                    .build());
        }
    }

    /**
     * Comprehensive trip planning endpoint with multiple function calls
     * 
     * @param request Trip planning request with destination and preferences
     * @return Complete trip itinerary with flights, hotels, weather, and attractions
     */
    @PostMapping("/plan-trip")
    @Operation(summary = "Plan a complete trip with AI and real-time data", 
               description = "Generate a comprehensive trip plan using multiple function calls for flights, hotels, weather, and attractions")
    public ResponseEntity<TripPlanningResponse> planTrip(@Valid @RequestBody TripPlanningRequest request) {
        log.info("Received trip planning request for {} from {} to {}", 
            request.getDestination(), request.getStartDate(), request.getEndDate());
        
        try {
            String tripPlan = functionCallingChatService.planTripWithFunctions(
                request.getDestination(),
                request.getStartDate().toString(),
                request.getEndDate().toString(),
                request.getPreferences()
            );
            
            return ResponseEntity.ok(TripPlanningResponse.builder()
                .tripId(UUID.randomUUID().toString())
                .destination(request.getDestination())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .itinerary(tripPlan)
                .generatedAt(LocalDateTime.now())
                .model("gemini-2.0-flash")
                .functionsUsed(new String[]{"searchFlights", "searchHotels", "getWeather", "searchAttractions"})
                .build());
        } catch (Exception e) {
            log.error("Error in trip planning", e);
            return ResponseEntity.internalServerError()
                .body(TripPlanningResponse.builder()
                    .tripId(UUID.randomUUID().toString())
                    .destination(request.getDestination())
                    .startDate(request.getStartDate())
                    .endDate(request.getEndDate())
                    .itinerary("Error generating trip plan: " + e.getMessage())
                    .generatedAt(LocalDateTime.now())
                    .model("gemini-2.0-flash")
                    .functionsUsed(new String[]{})
                    .build());
        }
    }

    /**
     * Demo endpoint to test function calling capabilities
     * 
     * @return Status message indicating successful demonstration
     */
    @GetMapping("/demo")
    @Operation(summary = "Demonstrate function calling capabilities", 
               description = "Run a demo of various function calling scenarios")
    public ResponseEntity<String> demonstrateFunctionCalling() {
        log.info("Running function calling demonstration");
        
        try {
            functionCallingChatService.demonstrateFunctionCalling();
            return ResponseEntity.ok("Function calling demonstration completed. Check logs for details.");
        } catch (Exception e) {
            log.error("Error in function calling demonstration", e);
            return ResponseEntity.internalServerError()
                .body("Error during demonstration: " + e.getMessage());
        }
    }
}