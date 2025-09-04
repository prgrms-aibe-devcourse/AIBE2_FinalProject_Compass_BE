package com.compass.domain.chat.service;

import com.compass.domain.chat.prompt.PromptTemplateRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Prompt Engineering Service for travel planning
 * 프롬프트 엔지니어링 서비스 - 템플릿 기반 프롬프트 생성 및 AI 응답 처리
 */
@Service
public class PromptEngineeringService {
    
    private static final Logger logger = LoggerFactory.getLogger(PromptEngineeringService.class);
    
    private final PromptTemplateRegistry promptRegistry;
    private final TravelTemplateService templateService;
    private final ChatModel geminiChatModel;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public PromptEngineeringService(
            PromptTemplateRegistry promptRegistry,
            TravelTemplateService templateService,
            @Autowired(required = false) VertexAiGeminiChatModel geminiChatModel,
            ObjectMapper objectMapper) {
        this.promptRegistry = promptRegistry;
        this.templateService = templateService;
        this.geminiChatModel = geminiChatModel;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Generate travel plan using prompt templates and Gemini
     * 프롬프트 템플릿과 Gemini를 사용한 여행 계획 생성
     */
    public Map<String, Object> generateTravelPlan(Map<String, Object> request) {
        try {
            // 1. Extract parameters from request
            String destination = (String) request.get("destination");
            Integer nights = (Integer) request.getOrDefault("nights", 0);
            String travelStyle = (String) request.getOrDefault("travelStyle", "relaxed");
            String budget = (String) request.getOrDefault("budget", "moderate");
            List<String> interests = (List<String>) request.getOrDefault("interests", Arrays.asList());
            
            logger.info("Generating travel plan for {} ({}박), style: {}, budget: {}", 
                    destination, nights, travelStyle, budget);
            
            // 2. Select appropriate travel template
            Optional<Map<String, Object>> templateOpt = templateService.recommendTemplate(nights);
            Map<String, Object> template = templateOpt.orElseThrow(() -> 
                new RuntimeException("No template found for " + nights + " nights"));
            String templateId = (String) template.get("templateId");
            
            // 3. Select and build prompt
            String promptType = determinePromptType(request);
            Map<String, Object> promptParams = buildPromptParameters(request, template);
            String prompt = promptRegistry.buildPrompt(promptType, promptParams);
            
            logger.debug("Using prompt type: {}, template: {}", promptType, templateId);
            logger.debug("Generated prompt: {}", prompt);
            
            // 4. Call Gemini API
            ChatResponse response = callGeminiWithRetry(prompt);
            String aiResponse = response.getResult().getOutput().getContent();
            
            logger.info("Received AI response: {} characters", aiResponse.length());
            
            // 5. Parse and structure response
            Map<String, Object> structuredPlan = parseAiResponse(aiResponse, template);
            
            // 6. Build final response
            Map<String, Object> result = new HashMap<>();
            result.put("templateId", templateId);
            result.put("promptType", promptType);
            result.put("plan", structuredPlan);
            result.put("metadata", buildMetadata(request, templateId, promptType));
            
            return result;
            
        } catch (Exception e) {
            logger.error("Failed to generate travel plan: ", e);
            throw new RuntimeException("Travel plan generation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Determine which prompt type to use based on request
     */
    private String determinePromptType(Map<String, Object> request) {
        // Priority-based selection
        if (request.containsKey("dailyDetail") && (Boolean) request.get("dailyDetail")) {
            return "daily_itinerary";
        }
        if (request.containsKey("budgetFocus") && (Boolean) request.get("budgetFocus")) {
            return "budget_optimization";
        }
        if (request.containsKey("localExperience") && (Boolean) request.get("localExperience")) {
            return "local_experience";
        }
        if (request.containsKey("discovery") && (Boolean) request.get("discovery")) {
            return "destination_discovery";
        }
        if (request.containsKey("recommendation") && (Boolean) request.get("recommendation")) {
            return "travel_recommendation";
        }
        
        // Default to comprehensive planning
        return "travel_planning";
    }
    
    /**
     * Build parameters for prompt template
     */
    private Map<String, Object> buildPromptParameters(Map<String, Object> request, Map<String, Object> template) {
        Map<String, Object> params = new HashMap<>();
        
        // Required parameters for TravelPlanningPrompt
        params.put("destination", request.getOrDefault("destination", "Seoul"));
        params.put("duration", template.get("duration")); // From template
        
        // Travel dates - generate default if not provided
        String travelDates = (String) request.get("travelDates");
        if (travelDates == null || travelDates.isEmpty()) {
            // Generate default dates (e.g., "2025-01-15 to 2025-01-17")
            Integer nights = (Integer) request.getOrDefault("nights", 0);
            travelDates = String.format("next %d days", nights + 1);
        }
        params.put("travelDates", travelDates);
        
        // Number of travelers
        Integer numberOfTravelers = (Integer) request.get("numberOfTravelers");
        if (numberOfTravelers == null) {
            String groupType = (String) request.getOrDefault("groupType", "solo");
            numberOfTravelers = groupType.equals("solo") ? 1 : 2;
        }
        params.put("numberOfTravelers", numberOfTravelers);
        
        // Trip purpose
        String tripPurpose = (String) request.get("tripPurpose");
        if (tripPurpose == null || tripPurpose.isEmpty()) {
            tripPurpose = "leisure travel";
        }
        params.put("tripPurpose", tripPurpose);
        
        // User preferences - combine interests and style
        String userPreferences = (String) request.get("userPreferences");
        if (userPreferences == null || userPreferences.isEmpty()) {
            List<String> interests = (List<String>) request.getOrDefault("interests", Arrays.asList("culture", "food"));
            String travelStyle = (String) request.getOrDefault("travelStyle", "relaxed");
            userPreferences = String.format("%s style, interests: %s", travelStyle, String.join(", ", interests));
        }
        params.put("userPreferences", userPreferences);
        
        // Travel style
        params.put("travelStyle", request.getOrDefault("travelStyle", "relaxed"));
        
        // Budget range
        String budgetRange = (String) request.get("budgetRange");
        if (budgetRange == null || budgetRange.isEmpty()) {
            budgetRange = (String) request.getOrDefault("budget", "moderate");
        }
        params.put("budgetRange", budgetRange);
        
        // Optional parameters
        params.put("specialRequirements", request.getOrDefault("specialRequirements", ""));
        params.put("durationGuidelines", "Plan for " + template.get("duration"));
        params.put("planInclusions", "Daily itinerary, accommodation suggestions, restaurant recommendations, activities");
        params.put("durationSpecificRequirements", "Optimize for " + template.get("duration") + " travel");
        
        // Additional context
        params.put("season", request.getOrDefault("season", "all-season"));
        params.put("groupType", request.getOrDefault("groupType", "solo"));
        
        return params;
    }
    
    /**
     * Call Gemini API with retry logic
     */
    private ChatResponse callGeminiWithRetry(String prompt) {
        int maxRetries = 3;
        int retryCount = 0;
        Exception lastException = null;
        
        while (retryCount < maxRetries) {
            try {
                // Configure Gemini options
                VertexAiGeminiChatOptions options = VertexAiGeminiChatOptions.builder()
                        .withTemperature(0.7)
                        .withMaxOutputTokens(4000)
                        .withTopP(0.9)
                        .build();
                
                // Create prompt and call
                Prompt aiPrompt = new Prompt(prompt, options);
                return geminiChatModel.call(aiPrompt);
                
            } catch (Exception e) {
                lastException = e;
                retryCount++;
                logger.warn("Gemini API call failed (attempt {}/{}): {}", 
                        retryCount, maxRetries, e.getMessage());
                
                if (retryCount < maxRetries) {
                    try {
                        Thread.sleep(1000 * retryCount); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        throw new RuntimeException("Failed to call Gemini API after " + maxRetries + " attempts", lastException);
    }
    
    /**
     * Parse AI response and structure it according to template
     */
    private Map<String, Object> parseAiResponse(String aiResponse, Map<String, Object> template) {
        Map<String, Object> structuredPlan = new HashMap<>();
        
        try {
            // Try to parse as JSON first
            if (aiResponse.trim().startsWith("{")) {
                structuredPlan = objectMapper.readValue(aiResponse, Map.class);
            } else {
                // Parse as text and structure it
                structuredPlan = parseTextResponse(aiResponse, template);
            }
            
            // Merge with template structure
            structuredPlan = mergeWithTemplate(structuredPlan, template);
            
        } catch (Exception e) {
            logger.error("Failed to parse AI response, using raw response: ", e);
            structuredPlan.put("rawResponse", aiResponse);
            structuredPlan.put("parseError", e.getMessage());
        }
        
        return structuredPlan;
    }
    
    /**
     * Parse text response into structured format
     */
    private Map<String, Object> parseTextResponse(String text, Map<String, Object> template) {
        Map<String, Object> parsed = new HashMap<>();
        
        // Basic parsing logic - split by days
        String[] lines = text.split("\n");
        List<Map<String, Object>> days = new ArrayList<>();
        Map<String, Object> currentDay = null;
        List<Map<String, Object>> currentActivities = null;
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            // Check for day markers
            if (line.matches("(?i).*day\\s*\\d+.*") || line.matches("(?i).*\\d+일차.*")) {
                if (currentDay != null) {
                    currentDay.put("activities", currentActivities);
                    days.add(currentDay);
                }
                currentDay = new HashMap<>();
                currentDay.put("dayNumber", days.size() + 1);
                currentDay.put("title", line);
                currentActivities = new ArrayList<>();
            } 
            // Parse activity lines
            else if (currentDay != null && line.contains(":")) {
                Map<String, Object> activity = new HashMap<>();
                String[] parts = line.split(":", 2);
                activity.put("time", parts[0].trim());
                activity.put("activity", parts.length > 1 ? parts[1].trim() : line);
                currentActivities.add(activity);
            }
        }
        
        // Add last day
        if (currentDay != null) {
            currentDay.put("activities", currentActivities);
            days.add(currentDay);
        }
        
        parsed.put("itinerary", days);
        parsed.put("originalText", text);
        
        return parsed;
    }
    
    /**
     * Merge parsed response with template structure
     */
    private Map<String, Object> mergeWithTemplate(Map<String, Object> parsed, Map<String, Object> template) {
        Map<String, Object> merged = new HashMap<>(template);
        
        // Override template values with AI-generated content
        if (parsed.containsKey("itinerary")) {
            merged.put("itinerary", parsed.get("itinerary"));
        }
        
        if (parsed.containsKey("recommendations")) {
            merged.put("recommendations", parsed.get("recommendations"));
        }
        
        if (parsed.containsKey("tips")) {
            merged.put("tips", parsed.get("tips"));
        }
        
        // Keep original response for reference
        merged.put("aiGenerated", parsed);
        
        return merged;
    }
    
    /**
     * Build metadata for response
     */
    private Map<String, Object> buildMetadata(Map<String, Object> request, String templateId, String promptType) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("generatedAt", new Date());
        metadata.put("templateUsed", templateId);
        metadata.put("promptType", promptType);
        metadata.put("requestParameters", request);
        metadata.put("version", "1.0.0");
        metadata.put("model", "gemini-2.0-flash");
        return metadata;
    }
    
    /**
     * Get available prompt templates
     */
    public Set<String> getAvailablePromptTemplates() {
        return promptRegistry.getTemplateNames();
    }
    
    /**
     * Get available travel templates
     */
    public List<Map<String, Object>> getAvailableTravelTemplates() {
        List<Map<String, String>> summaries = templateService.getTemplateSummaries();
        // Convert Map<String, String> to Map<String, Object>
        return summaries.stream()
                .map(summary -> {
                    Map<String, Object> result = new HashMap<>();
                    result.putAll(summary);
                    return result;
                })
                .collect(Collectors.toList());
    }
}