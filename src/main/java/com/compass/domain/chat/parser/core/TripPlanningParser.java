package com.compass.domain.chat.parser.core;

import com.compass.domain.chat.dto.TripPlanningRequest;

/**
 * Core interface for parsing natural language input into trip planning requests.
 * This interface represents pure domain logic without any infrastructure dependencies.
 * 
 * Different implementations can provide various parsing strategies:
 * - Pattern-based parsing using regex
 * - AI-enhanced parsing with language models
 * - Hybrid approaches combining multiple strategies
 * 
 * This design follows Dependency Inversion Principle, allowing the domain
 * to depend on abstractions rather than concrete implementations.
 */
public interface TripPlanningParser {
    
    /**
     * Parse natural language input to extract trip planning information.
     * 
     * @param userInput Natural language text from user containing trip details
     * @return TripPlanningRequest with extracted and normalized information
     */
    TripPlanningRequest parse(String userInput);
    
    /**
     * Get the parser strategy name for logging and monitoring.
     * 
     * @return Strategy identifier (e.g., "pattern", "ai", "hybrid")
     */
    default String getStrategyName() {
        return "default";
    }
    
    /**
     * Check if this parser supports the given input.
     * Some parsers might have specific requirements or limitations.
     * 
     * @param userInput The input to check
     * @return true if this parser can handle the input
     */
    default boolean canHandle(String userInput) {
        return userInput != null && !userInput.trim().isEmpty();
    }
}