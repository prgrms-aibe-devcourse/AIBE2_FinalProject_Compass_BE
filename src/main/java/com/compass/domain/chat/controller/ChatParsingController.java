package com.compass.domain.chat.controller;

import com.compass.domain.chat.dto.TripPlanningRequest;
import com.compass.domain.chat.dto.TripPlanningResponse;
import com.compass.domain.chat.parser.ChatInputParser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for parsing natural language chat input into structured trip planning data
 * Part of CHAT domain - handles NER-based entity extraction from user messages
 */
@Slf4j
@RestController
@RequestMapping("/api/chat/parse")
@RequiredArgsConstructor
@Tag(name = "Chat Parsing", description = "Natural language parsing for chat messages")
public class ChatParsingController {

    private final ChatInputParser chatInputParser;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParseRequest {
        @NotBlank(message = "Text input is required")
        @Size(min = 3, max = 1000, message = "Text must be between 3 and 1000 characters")
        private String text;
    }

    /**
     * Parse natural language input to extract trip planning information
     * This endpoint is used by the chat service to understand user intent
     */
    @PostMapping
    @Operation(summary = "Parse chat message for trip information",
               description = "Extracts structured trip planning data from natural language chat input")
    public ResponseEntity<TripPlanningResponse> parseChatInput(
            @Valid @RequestBody ParseRequest request) {
        
        log.info("Parsing chat input: {}", request.getText());
        
        // Parse the user input
        TripPlanningRequest parsedRequest = chatInputParser.parseUserInput(request.getText());
        
        // Create response with parsed information
        TripPlanningResponse response = createResponseFromParsedRequest(parsedRequest);
        
        log.info("Successfully parsed chat input with confidence: {}", 
                response.getMetadata().get("confidence"));
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get raw parsing result without creating a full response
     * Used for testing and debugging parsing capabilities
     */
    @PostMapping("/raw")
    @Operation(summary = "Get raw parsing result",
               description = "Returns only the parsed request object without additional processing")
    public ResponseEntity<TripPlanningRequest> parseRaw(
            @Valid @RequestBody ParseRequest request) {
        
        log.debug("Raw parsing request: {}", request.getText());
        TripPlanningRequest parsed = chatInputParser.parseUserInput(request.getText());
        
        return ResponseEntity.ok(parsed);
    }

    /**
     * Get parsing examples to demonstrate capabilities
     */
    @GetMapping("/examples")
    @Operation(summary = "Get parsing examples",
               description = "Returns example inputs and their parsed results")
    public ResponseEntity<Map<String, TripPlanningRequest>> getExamples() {
        Map<String, TripPlanningRequest> examples = new HashMap<>();
        
        // Example 1: Complete input
        String example1 = "다음달 15일부터 3박4일로 제주도 여행을 가려고 해요. 예산은 100만원입니다.";
        examples.put(example1, chatInputParser.parseUserInput(example1));
        
        // Example 2: Minimal input
        String example2 = "부산 맛집 탐방하고 싶어요";
        examples.put(example2, chatInputParser.parseUserInput(example2));
        
        // Example 3: Complex input
        String example3 = "2명이서 이번 주말에 강릉 바다 보러 가려고 하는데 1박2일로 저렴하게";
        examples.put(example3, chatInputParser.parseUserInput(example3));
        
        return ResponseEntity.ok(examples);
    }

    /**
     * Helper method to create a full response from parsed request
     */
    private TripPlanningResponse createResponseFromParsedRequest(TripPlanningRequest request) {
        TripPlanningResponse response = new TripPlanningResponse();
        
        // Generate a unique plan ID for this parsing session
        response.setPlanId("CHAT_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        
        // Copy basic information
        response.setDestination(request.getDestination());
        response.setOrigin(request.getOrigin());
        response.setStartDate(request.getStartDate());
        response.setEndDate(request.getEndDate());
        response.setNumberOfTravelers(request.getNumberOfTravelers());
        response.setTravelStyle(request.getTravelStyle());
        response.setInterests(request.getInterests());
        
        // Set budget information
        if (request.getBudgetPerPerson() != null) {
            Map<String, Object> budget = new HashMap<>();
            budget.put("perPerson", request.getBudgetPerPerson());
            budget.put("total", request.getBudgetPerPerson() * 
                      (request.getNumberOfTravelers() != null ? request.getNumberOfTravelers() : 1));
            budget.put("currency", request.getCurrency());
            response.setBudget(budget);
        }
        
        // Set preferences
        response.setPreferences(request.getPreferences() != null ? 
                               request.getPreferences() : new HashMap<>());
        
        // Create summary
        String summary = String.format("%d명이서 %s부터 %s까지 %s 여행",
            request.getNumberOfTravelers() != null ? request.getNumberOfTravelers() : 1,
            request.getStartDate() != null ? request.getStartDate().toString() : "미정",
            request.getEndDate() != null ? request.getEndDate().toString() : "미정",
            request.getDestination() != null ? request.getDestination() : "미정"
        );
        
        if (request.getBudgetPerPerson() != null) {
            summary += String.format(" (예산: 1인당 %,d원)", request.getBudgetPerPerson());
        }
        response.setSummary(summary);
        
        // Set metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("parsedAt", LocalDate.now().toString());
        metadata.put("parsingMethod", "NER_PATTERN_MATCHING");
        metadata.put("confidence", calculateConfidence(request));
        metadata.put("chatDomain", true);  // Indicate this came from chat parsing
        response.setMetadata(metadata);
        
        return response;
    }

    /**
     * Calculate confidence score based on extracted information
     */
    private double calculateConfidence(TripPlanningRequest request) {
        double score = 0.0;
        double maxScore = 0.0;
        
        // Essential fields (higher weight)
        if (request.getDestination() != null) {
            score += 1.0;
        }
        maxScore += 1.0;
        
        if (request.getStartDate() != null) {
            score += 1.0;
        }
        maxScore += 1.0;
        
        if (request.getEndDate() != null) {
            score += 1.0;
        }
        maxScore += 1.0;
        
        // Optional fields (lower weight)
        if (request.getOrigin() != null && !request.getOrigin().equals("서울")) {
            score += 0.5;
        }
        maxScore += 0.5;
        
        if (request.getNumberOfTravelers() != null && request.getNumberOfTravelers() != 1) {
            score += 0.5;
        }
        maxScore += 0.5;
        
        if (request.getBudgetPerPerson() != null) {
            score += 1.0;
        }
        maxScore += 1.0;
        
        if (request.getTravelStyle() != null && !request.getTravelStyle().equals("moderate")) {
            score += 0.5;
        }
        maxScore += 0.5;
        
        if (request.getInterests() != null && request.getInterests().length > 0) {
            score += 1.0;
        }
        maxScore += 1.0;
        
        // Calculate percentage and round to 2 decimal places
        double confidence = maxScore > 0 ? (score / maxScore) : 0.0;
        return Math.round(confidence * 100.0) / 100.0;
    }
}