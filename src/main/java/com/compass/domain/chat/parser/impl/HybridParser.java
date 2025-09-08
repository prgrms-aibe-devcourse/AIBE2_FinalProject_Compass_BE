package com.compass.domain.chat.parser.impl;

import com.compass.domain.chat.dto.TripPlanningRequest;
import com.compass.domain.chat.parser.core.TripPlanningParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Hybrid parser that intelligently selects between different parsing strategies.
 * 
 * Strategy selection is based on:
 * 1. Configuration settings
 * 2. Input characteristics
 * 3. Runtime availability of services
 * 
 * This is the primary parser used in production environments.
 */
@Slf4j
@Component("hybridParser")
@Primary // This is the default parser
@Profile("!test") // Disabled in test profile
public class HybridParser implements TripPlanningParser {
    
    private final TripPlanningParser patternParser;
    private final TripPlanningParser aiEnhancedParser;
    
    @Value("${compass.parser.strategy:auto}")
    private String parserStrategy; // "pattern", "ai", "auto"
    
    @Value("${compass.parser.ai.enabled:true}")
    private boolean aiEnabled;
    
    public HybridParser(
            @Qualifier("patternBasedParser") TripPlanningParser patternParser,
            @Autowired(required = false) @Qualifier("aiEnhancedParser") TripPlanningParser aiEnhancedParser) {
        this.patternParser = patternParser;
        this.aiEnhancedParser = aiEnhancedParser;
    }
    
    @Override
    public TripPlanningRequest parse(String userInput) {
        log.info("Hybrid parsing with strategy: {}", parserStrategy);
        
        TripPlanningParser selectedParser = selectParser(userInput);
        
        log.info("Selected parser: {}", selectedParser.getStrategyName());
        return selectedParser.parse(userInput);
    }
    
    @Override
    public String getStrategyName() {
        return "hybrid";
    }
    
    /**
     * Select the appropriate parser based on strategy and input characteristics.
     */
    private TripPlanningParser selectParser(String userInput) {
        // Check if AI parser is available
        boolean aiAvailable = aiEnhancedParser != null && aiEnabled;
        
        // Apply strategy
        switch (parserStrategy.toLowerCase()) {
            case "pattern":
                return patternParser;
                
            case "ai":
                if (!aiAvailable) {
                    log.warn("AI parser requested but not available, falling back to pattern parser");
                    return patternParser;
                }
                return aiEnhancedParser;
                
            case "auto":
            default:
                return selectAutoParser(userInput, aiAvailable);
        }
    }
    
    /**
     * Automatically select parser based on input characteristics.
     */
    private TripPlanningParser selectAutoParser(String userInput, boolean aiAvailable) {
        // Simple heuristics for parser selection
        
        // If input is very short, pattern matching might not work well
        if (userInput.length() < 20) {
            log.debug("Short input detected, preferring AI if available");
            return aiAvailable ? aiEnhancedParser : patternParser;
        }
        
        // If input contains complex or ambiguous language, prefer AI
        if (containsComplexLanguage(userInput)) {
            log.debug("Complex language detected, preferring AI if available");
            return aiAvailable ? aiEnhancedParser : patternParser;
        }
        
        // If input looks structured with clear patterns, use pattern parser
        if (containsClearPatterns(userInput)) {
            log.debug("Clear patterns detected, using pattern parser");
            return patternParser;
        }
        
        // Default: Use AI if available for best results
        return aiAvailable ? aiEnhancedParser : patternParser;
    }
    
    /**
     * Check if input contains complex or ambiguous language.
     */
    private boolean containsComplexLanguage(String input) {
        // Check for indirect references or complex sentences
        String[] complexIndicators = {
            "같은", "쯤", "정도", "아마", "대충", "얼추",
            "그때", "거기", "어디", "언젠가"
        };
        
        for (String indicator : complexIndicators) {
            if (input.contains(indicator)) {
                return true;
            }
        }
        
        // Check for questions or uncertainties
        return input.contains("?") || input.contains("할까") || input.contains("을까");
    }
    
    /**
     * Check if input contains clear, parseable patterns.
     */
    private boolean containsClearPatterns(String input) {
        int patternCount = 0;
        
        // Check for date patterns
        if (input.matches(".*\\d+박.*\\d+일.*") || 
            input.matches(".*\\d{4}[-/년].*\\d{1,2}[-/월].*")) {
            patternCount++;
        }
        
        // Check for budget patterns
        if (input.matches(".*\\d+.*만원.*") || 
            input.matches(".*예산.*\\d+.*")) {
            patternCount++;
        }
        
        // Check for clear destination
        String[] destinations = {"서울", "부산", "제주", "경주", "강릉"};
        for (String dest : destinations) {
            if (input.contains(dest)) {
                patternCount++;
                break;
            }
        }
        
        // If we have multiple clear patterns, pattern parser should work well
        return patternCount >= 2;
    }
}