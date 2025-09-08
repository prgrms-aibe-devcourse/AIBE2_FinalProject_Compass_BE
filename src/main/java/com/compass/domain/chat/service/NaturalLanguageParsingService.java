package com.compass.domain.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Natural Language Parsing Service using LLM
 * 자연어 입력을 구조화된 JSON으로 변환
 */
@Service
public class NaturalLanguageParsingService {
    
    private static final Logger logger = LoggerFactory.getLogger(NaturalLanguageParsingService.class);
    
    private final VertexAiGeminiChatModel geminiChatModel;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public NaturalLanguageParsingService(
            @Autowired(required = false) VertexAiGeminiChatModel geminiChatModel,
            ObjectMapper objectMapper) {
        this.geminiChatModel = geminiChatModel;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Parse natural language travel request into structured format
     */
    public Map<String, Object> parseNaturalLanguageRequest(String userInput) {
        try {
            String parsingPrompt = buildParsingPrompt(userInput);
            
            // Configure Gemini for parsing
            VertexAiGeminiChatOptions options = VertexAiGeminiChatOptions.builder()
                    .temperature(0.3)  // Lower temperature for consistent parsing
                    .maxOutputTokens(1000)
                    .build();
            
            Prompt prompt = new Prompt(parsingPrompt, options);
            ChatResponse response = geminiChatModel.call(prompt);
            String aiResponse = response.getResult().getOutput().getContent();
            
            logger.info("Parsed user input: {} -> {}", userInput, aiResponse);
            
            // Parse JSON response
            return parseJsonResponse(aiResponse);
            
        } catch (Exception e) {
            logger.error("Failed to parse natural language request: ", e);
            return createFallbackParsing(userInput);
        }
    }
    
    private String buildParsingPrompt(String userInput) {
        return """
            You are a travel request parser. Extract travel information from the user's natural language input and return it as JSON.
            
            User Input: "%s"
            
            Extract the following information:
            1. destination: The travel destination city or country
            2. nights: Number of nights (0 for day trip, derive from "2박3일" = 2 nights)
            3. travelStyle: Infer style (relaxed, active, adventurous, luxury)
            4. budget: Infer budget level (budget, moderate, luxury)
            5. interests: Array of interests (e.g., ["culture", "food", "shopping", "beach", "nature"])
            6. tripPurpose: Purpose of travel (leisure, business, honeymoon, family vacation)
            7. numberOfTravelers: Number of people (default 1 if not mentioned)
            8. groupType: Type of group (solo, couple, family, friends)
            
            Korean travel duration patterns:
            - "당일치기" = 0 nights
            - "1박2일" = 1 night
            - "2박3일" = 2 nights
            - "3박4일" = 3 nights
            
            Common Korean destinations:
            - 서울/Seoul, 부산/Busan, 제주/Jeju, 경주/Gyeongju, 전주/Jeonju
            
            Return ONLY a valid JSON object without any explanation or markdown:
            {
              "destination": "extracted destination",
              "nights": number,
              "travelStyle": "style",
              "budget": "level",
              "interests": ["interest1", "interest2"],
              "tripPurpose": "purpose",
              "numberOfTravelers": number,
              "groupType": "type"
            }
            
            If information is not provided, use reasonable defaults but always include all fields.
            """.formatted(userInput);
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonResponse(String response) {
        try {
            // Remove markdown code blocks if present
            String cleanJson = response.replaceAll("```json\\s*", "")
                                      .replaceAll("```\\s*", "")
                                      .trim();
            
            // Parse JSON
            return objectMapper.readValue(cleanJson, Map.class);
            
        } catch (Exception e) {
            logger.error("Failed to parse JSON response: {}", response, e);
            throw new RuntimeException("Failed to parse AI response as JSON", e);
        }
    }
    
    private Map<String, Object> createFallbackParsing(String userInput) {
        Map<String, Object> fallback = new HashMap<>();
        
        // Basic keyword detection fallback
        String lowerInput = userInput.toLowerCase();
        
        // Detect destination
        if (lowerInput.contains("서울") || lowerInput.contains("seoul")) {
            fallback.put("destination", "Seoul");
        } else if (lowerInput.contains("부산") || lowerInput.contains("busan")) {
            fallback.put("destination", "Busan");
        } else if (lowerInput.contains("제주") || lowerInput.contains("jeju")) {
            fallback.put("destination", "Jeju");
        } else {
            fallback.put("destination", "Seoul"); // Default
        }
        
        // Detect nights
        if (lowerInput.contains("당일")) {
            fallback.put("nights", 0);
        } else if (lowerInput.contains("1박")) {
            fallback.put("nights", 1);
        } else if (lowerInput.contains("2박")) {
            fallback.put("nights", 2);
        } else if (lowerInput.contains("3박")) {
            fallback.put("nights", 3);
        } else {
            fallback.put("nights", 2); // Default
        }
        
        // Set defaults for other fields
        fallback.put("travelStyle", "relaxed");
        fallback.put("budget", "moderate");
        fallback.put("interests", new String[]{"culture", "food"});
        fallback.put("tripPurpose", "leisure");
        fallback.put("numberOfTravelers", 1);
        fallback.put("groupType", "solo");
        
        logger.warn("Using fallback parsing for input: {}", userInput);
        
        return fallback;
    }
}