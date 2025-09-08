package com.compass.domain.chat.parser.impl;

import com.compass.domain.chat.dto.TripPlanningRequest;
import com.compass.domain.chat.parser.core.TripPlanningParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * AI-enhanced parser that decorates a base parser with AI capabilities.
 * Uses the Decorator pattern to add AI enhancement to any TripPlanningParser.
 * 
 * This implementation:
 * 1. First tries the base parser (usually pattern-based)
 * 2. If critical information is missing, enhances with AI
 * 3. Merges AI results with pattern-based results
 * 
 * This design keeps AI as an optional enhancement rather than a requirement.
 */
@Slf4j
@Component("aiEnhancedParser")
@Profile("!test") // Disabled in test profile
public class AiEnhancedParser implements TripPlanningParser {
    
    private final TripPlanningParser baseParser;
    private final ChatModel chatModel;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Constructor with explicit dependencies.
     * Uses @Qualifier to avoid ambiguity in Spring DI.
     */
    public AiEnhancedParser(
            @Qualifier("patternBasedParser") TripPlanningParser baseParser,
            @Qualifier("vertexAiGeminiChat") ChatModel chatModel) {
        this.baseParser = baseParser;
        this.chatModel = chatModel;
    }
    
    @Override
    public TripPlanningRequest parse(String userInput) {
        log.info("AI-enhanced parsing for input: {}", userInput);
        
        // First, try pattern-based parsing
        TripPlanningRequest request = baseParser.parse(userInput);
        
        // Check if AI enhancement is needed
        if (needsAiEnhancement(request)) {
            log.info("Enhancing with AI for missing information");
            enhanceWithAI(userInput, request);
        }
        
        log.info("AI-enhanced parsing complete: {}", request);
        return request;
    }
    
    @Override
    public String getStrategyName() {
        return "ai-enhanced";
    }
    
    /**
     * Check if AI enhancement is needed based on missing critical information.
     */
    private boolean needsAiEnhancement(TripPlanningRequest request) {
        // Consider AI enhancement if critical fields are missing
        boolean missingDestination = request.getDestination() == null;
        boolean missingDates = request.getStartDate() == null;
        boolean missingInterests = request.getInterests() == null || 
                                  request.getInterests().length == 0;
        
        return missingDestination || missingDates || missingInterests;
    }
    
    /**
     * Enhance the request with AI-extracted information.
     */
    private void enhanceWithAI(String userInput, TripPlanningRequest request) {
        String promptText = buildAIPrompt(userInput, request);
        
        try {
            Prompt prompt = new Prompt(promptText);
            ChatResponse chatResponse = chatModel.call(prompt);
            String response = chatResponse.getResult().getOutput().getContent();
            
            mergeAIResponse(response, request);
        } catch (Exception e) {
            log.error("Failed to enhance with AI: {}", e.getMessage());
            // Continue with pattern-based results
        }
    }
    
    /**
     * Build AI prompt based on what information is missing.
     */
    private String buildAIPrompt(String userInput, TripPlanningRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Extract travel information from the following text.\n");
        prompt.append("Text: \"").append(userInput).append("\"\n\n");
        
        prompt.append("Focus on extracting:\n");
        
        if (request.getDestination() == null) {
            prompt.append("- destination (여행지)\n");
        }
        if (request.getStartDate() == null) {
            prompt.append("- startDate (시작일, format: yyyy-MM-dd)\n");
            prompt.append("- endDate (종료일, format: yyyy-MM-dd)\n");
        }
        if (request.getNumberOfTravelers() == null) {
            prompt.append("- numberOfTravelers (인원수)\n");
        }
        if (request.getBudgetPerPerson() == null) {
            prompt.append("- budgetPerPerson (1인당 예산, KRW)\n");
        }
        if (request.getInterests() == null || request.getInterests().length == 0) {
            prompt.append("- interests (culture/food/adventure/shopping/nature)\n");
        }
        
        prompt.append("\nIf dates are relative (like \"next week\"), calculate from today's date: ");
        prompt.append(LocalDate.now().format(DateTimeFormatter.ISO_DATE));
        prompt.append("\n\nReturn ONLY valid JSON without any explanation or markdown formatting.");
        prompt.append("\nExample format: {\"destination\":\"제주도\",\"startDate\":\"2024-01-15\"}");
        
        return prompt.toString();
    }
    
    /**
     * Merge AI response with existing request data.
     * Pattern-based results take precedence; AI fills gaps.
     */
    private void mergeAIResponse(String aiResponse, TripPlanningRequest request) {
        try {
            // Clean up the response (remove markdown if present)
            String cleanedResponse = aiResponse.replaceAll("```json", "")
                                             .replaceAll("```", "")
                                             .trim();
            
            // Parse JSON response
            Map<String, Object> aiData = objectMapper.readValue(
                cleanedResponse, 
                objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class)
            );
            
            // Merge only missing fields
            if (request.getDestination() == null && aiData.containsKey("destination")) {
                request.setDestination((String) aiData.get("destination"));
            }
            
            if (request.getStartDate() == null && aiData.containsKey("startDate")) {
                try {
                    LocalDate startDate = LocalDate.parse((String) aiData.get("startDate"));
                    request.setStartDate(startDate);
                } catch (Exception e) {
                    log.warn("Failed to parse AI startDate: {}", aiData.get("startDate"));
                }
            }
            
            if (request.getEndDate() == null && aiData.containsKey("endDate")) {
                try {
                    LocalDate endDate = LocalDate.parse((String) aiData.get("endDate"));
                    request.setEndDate(endDate);
                } catch (Exception e) {
                    log.warn("Failed to parse AI endDate: {}", aiData.get("endDate"));
                }
            }
            
            if (request.getNumberOfTravelers() == null && aiData.containsKey("numberOfTravelers")) {
                try {
                    Integer travelers = ((Number) aiData.get("numberOfTravelers")).intValue();
                    request.setNumberOfTravelers(travelers);
                } catch (Exception e) {
                    log.warn("Failed to parse AI numberOfTravelers: {}", aiData.get("numberOfTravelers"));
                }
            }
            
            if (request.getBudgetPerPerson() == null && aiData.containsKey("budgetPerPerson")) {
                try {
                    Integer budget = ((Number) aiData.get("budgetPerPerson")).intValue();
                    request.setBudgetPerPerson(budget);
                } catch (Exception e) {
                    log.warn("Failed to parse AI budgetPerPerson: {}", aiData.get("budgetPerPerson"));
                }
            }
            
            if ((request.getInterests() == null || request.getInterests().length == 0) 
                    && aiData.containsKey("interests")) {
                try {
                    Object interestsObj = aiData.get("interests");
                    if (interestsObj instanceof String) {
                        request.setInterests(new String[]{(String) interestsObj});
                    } else if (interestsObj instanceof java.util.List) {
                        java.util.List<?> interestsList = (java.util.List<?>) interestsObj;
                        String[] interests = interestsList.stream()
                            .map(Object::toString)
                            .toArray(String[]::new);
                        request.setInterests(interests);
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse AI interests: {}", aiData.get("interests"));
                }
            }
            
            log.debug("Successfully merged AI response with request");
            
        } catch (Exception e) {
            log.error("Failed to parse AI response: {}", e.getMessage());
            // Continue with existing request data
        }
    }
}