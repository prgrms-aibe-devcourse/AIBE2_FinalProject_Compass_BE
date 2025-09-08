package com.compass.domain.chat.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/debug/gemini")
@RequiredArgsConstructor
public class GeminiDebugController {
    
    private final VertexAiGeminiChatModel geminiChatModel;
    
    /**
     * Test simplest possible Gemini call without any function calling
     */
    @PostMapping("/simple")
    public Map<String, Object> testSimpleGemini(@RequestBody Map<String, String> request) {
        log.info("=== Testing Simple Gemini Call (No Functions) ===");
        String userMessage = request.getOrDefault("message", "Hello, can you respond?");
        log.info("User message: {}", userMessage);
        
        Map<String, Object> response = new HashMap<>();
        try {
            // Create simple options without function calling
            VertexAiGeminiChatOptions options = VertexAiGeminiChatOptions.builder()
                .model("gemini-2.0-flash")
                .temperature(0.7)
                .build();
            
            log.info("Calling Gemini with options: model={}, temperature={}", 
                options.getModel(), options.getTemperature());
            
            // Create prompt and call model
            Prompt prompt = new Prompt(userMessage, options);
            ChatResponse chatResponse = geminiChatModel.call(prompt);
            
            String result = chatResponse.getResult().getOutput().getContent();
            log.info("Gemini response received: {}", result.substring(0, Math.min(100, result.length())));
            
            response.put("success", true);
            response.put("response", result);
            response.put("model", "gemini-2.0-flash");
            
        } catch (Exception e) {
            log.error("Error calling Gemini", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("errorType", e.getClass().getSimpleName());
            
            // Log more details about the error
            if (e.getCause() != null) {
                log.error("Root cause: {}", e.getCause().getMessage());
                response.put("rootCause", e.getCause().getMessage());
            }
        }
        
        return response;
    }
    
    /**
     * Test with different model variations
     */
    @PostMapping("/model-test")
    public Map<String, Object> testModelVariations(@RequestBody Map<String, String> request) {
        String userMessage = request.getOrDefault("message", "Hello");
        String modelName = request.getOrDefault("model", "gemini-2.0-flash");
        
        log.info("Testing model: {}", modelName);
        
        Map<String, Object> response = new HashMap<>();
        try {
            VertexAiGeminiChatOptions options = VertexAiGeminiChatOptions.builder()
                .model(modelName)
                .temperature(0.5)
                .build();
            
            Prompt prompt = new Prompt(userMessage, options);
            ChatResponse chatResponse = geminiChatModel.call(prompt);
            
            response.put("success", true);
            response.put("response", chatResponse.getResult().getOutput().getContent());
            response.put("model", modelName);
            
        } catch (Exception e) {
            log.error("Error with model {}: {}", modelName, e.getMessage());
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("model", modelName);
        }
        
        return response;
    }
}