package com.compass.domain.chat.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for handling chat conversations with Function Calling capabilities
 * Supports both OpenAI and Gemini models with automatic function detection and invocation
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "spring.ai", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FunctionCallingChatService {

    private final List<FunctionCallback> functionCallbacks;
    private final ChatModel openAiChatModel;
    private final ChatModel geminiChatModel;
    
    public FunctionCallingChatService(
            List<FunctionCallback> functionCallbacks,
            @Qualifier("openAiChatModel") ChatModel openAiChatModel,
            @Qualifier("vertexAiGeminiChat") ChatModel geminiChatModel) {
        this.functionCallbacks = functionCallbacks;
        this.openAiChatModel = openAiChatModel;
        this.geminiChatModel = geminiChatModel;
    }

    /**
     * Process a chat message with function calling capabilities using OpenAI
     * 
     * @param userMessage The user's message
     * @param conversationHistory Previous messages in the conversation
     * @return The assistant's response with any function call results
     */
    public String chatWithFunctionsOpenAI(String userMessage, List<Message> conversationHistory) {
        log.info("Processing chat with OpenAI function calling: {}", userMessage);
        
        // Prepare messages
        List<Message> messages = new ArrayList<>();
        if (conversationHistory != null) {
            messages.addAll(conversationHistory);
        }
        messages.add(new UserMessage(userMessage));
        
        // Configure OpenAI with function calling
        OpenAiChatOptions options = OpenAiChatOptions.builder()
            .model("gpt-4o-mini")
            .temperature(0.7)
            .functions(getFunctionNames())
            .build();
        
        // Create prompt with functions
        Prompt prompt = new Prompt(messages, options);
        
        // Call the model (Spring AI handles function execution automatically)
        ChatResponse response = openAiChatModel.call(prompt);
        
        String assistantResponse = response.getResult().getOutput().getContent();
        log.info("OpenAI response with functions: {}", assistantResponse);
        
        return assistantResponse;
    }

    /**
     * Process a chat message with function calling capabilities using Gemini
     * 
     * @param userMessage The user's message
     * @param conversationHistory Previous messages in the conversation
     * @return The assistant's response with any function call results
     */
    public String chatWithFunctionsGemini(String userMessage, List<Message> conversationHistory) {
        log.info("Processing chat with Gemini function calling: {}", userMessage);
        
        // Prepare messages
        List<Message> messages = new ArrayList<>();
        if (conversationHistory != null) {
            messages.addAll(conversationHistory);
        }
        messages.add(new UserMessage(userMessage));
        
        // Configure Gemini with function calling
        VertexAiGeminiChatOptions options = VertexAiGeminiChatOptions.builder()
            .model("gemini-2.0-flash")
            .temperature(0.7)
            .functions(getFunctionNames())
            .build();
        
        // Create prompt with functions
        Prompt prompt = new Prompt(messages, options);
        
        // Call the model (Spring AI handles function execution automatically)
        ChatResponse response = geminiChatModel.call(prompt);
        
        String assistantResponse = response.getResult().getOutput().getContent();
        log.info("Gemini response with functions: {}", assistantResponse);
        
        return assistantResponse;
    }

    /**
     * Process a travel planning request with automatic function calling
     * 
     * @param destination The travel destination
     * @param startDate The start date of the trip
     * @param endDate The end date of the trip
     * @param preferences User preferences for the trip
     * @return A comprehensive travel plan with real-time data
     */
    public String planTripWithFunctions(String destination, String startDate, String endDate, Map<String, Object> preferences) {
        log.info("Planning trip to {} from {} to {} with functions", destination, startDate, endDate);
        
        // Build the travel planning prompt
        String prompt = buildTravelPlanningPrompt(destination, startDate, endDate, preferences);
        
        // Use Gemini for complex travel planning
        List<Message> messages = List.of(new UserMessage(prompt));
        
        VertexAiGeminiChatOptions options = VertexAiGeminiChatOptions.builder()
            .model("gemini-2.0-flash")
            .temperature(0.8)
            .functions(getFunctionNames())
            .build();
        
        Prompt aiPrompt = new Prompt(messages, options);
        ChatResponse response = geminiChatModel.call(aiPrompt);
        
        return response.getResult().getOutput().getContent();
    }

    /**
     * Get all available function names for configuration
     */
    private Set<String> getFunctionNames() {
        return functionCallbacks.stream()
            .map(FunctionCallback::getName)
            .collect(Collectors.toSet());
    }

    /**
     * Build a comprehensive travel planning prompt
     */
    private String buildTravelPlanningPrompt(String destination, String startDate, String endDate, Map<String, Object> preferences) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Please help me plan a trip to ").append(destination)
              .append(" from ").append(startDate).append(" to ").append(endDate).append(".\n\n");
        
        prompt.append("Please use the available functions to:\n");
        prompt.append("1. Search for flights from my location to ").append(destination).append("\n");
        prompt.append("2. Find suitable hotels in ").append(destination).append("\n");
        prompt.append("3. Check the weather forecast for those dates\n");
        prompt.append("4. Recommend tourist attractions and activities\n\n");
        
        if (preferences != null && !preferences.isEmpty()) {
            prompt.append("My preferences:\n");
            preferences.forEach((key, value) -> 
                prompt.append("- ").append(key).append(": ").append(value).append("\n")
            );
            prompt.append("\n");
        }
        
        prompt.append("Please provide a detailed itinerary with specific recommendations based on the real-time data from the functions.");
        
        return prompt.toString();
    }

    /**
     * Example method to demonstrate conversation with function calling
     */
    public void demonstrateFunctionCalling() {
        log.info("=== Demonstrating Function Calling ===");
        
        // Example 1: Simple weather query using Gemini 2.0 Flash
        String weatherResponse = chatWithFunctionsGemini(
            "What's the weather like in Seoul today?", 
            null
        );
        log.info("Weather query response (Gemini 2.0 Flash): {}", weatherResponse);
        
        // Example 2: Flight search
        String flightResponse = chatWithFunctionsOpenAI(
            "Find me flights from Seoul to Tokyo on December 25th, 2024", 
            null
        );
        log.info("Flight search response: {}", flightResponse);
        
        // Example 3: Complex travel planning
        Map<String, Object> preferences = Map.of(
            "budget", "medium",
            "interests", "culture, food, shopping",
            "hotel_type", "boutique"
        );
        
        String travelPlan = planTripWithFunctions(
            "Tokyo", 
            "2024-12-25", 
            "2024-12-30", 
            preferences
        );
        log.info("Travel plan: {}", travelPlan);
    }
}