package com.compass.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Test controller to verify Spring AI configuration
 * Only loaded in non-test profiles
 */
@RestController
@RequestMapping("/api/test")
@Profile("!test")
public class TestController {

    @Value("${spring.ai.openai.api-key:not-set}")
    private String openAiKey;
    
    @Value("${spring.ai.vertex.ai.gemini.project-id:not-set}")
    private String gcpProjectId;
    
    @Value("${DB_HOST:not-set}")
    private String dbHost;
    
    @Autowired(required = false)
    private OpenAiChatModel openAiChatModel;
    
    @Autowired(required = false)
    private VertexAiGeminiChatModel geminiChatModel;

    /**
     * Test endpoint to check if environment variables are loaded
     */
    @GetMapping("/config")
    public Map<String, Object> checkConfig() {
        Map<String, Object> config = new HashMap<>();
        
        // Check if keys are loaded (mask actual values for security)
        config.put("openai_configured", !openAiKey.equals("not-set") && openAiKey.length() > 10);
        config.put("vertex_ai_configured", !gcpProjectId.equals("not-set") && gcpProjectId.length() > 5);
        config.put("db_host", dbHost);
        
        // Show key prefixes for debugging (safe to show)
        if (!openAiKey.equals("not-set") && openAiKey.length() > 10) {
            config.put("openai_key_prefix", openAiKey.substring(0, 7) + "...");
        }
        
        if (!gcpProjectId.equals("not-set")) {
            config.put("gcp_project_id", gcpProjectId);
        }
        
        config.put("status", "Spring AI configuration check");
        config.put("timestamp", System.currentTimeMillis());
        
        return config;
    }
    
    /**
     * Simple health check
     */
    @GetMapping("/hello")
    public Map<String, String> hello() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Hello from Compass Backend!");
        response.put("status", "running");
        return response;
    }
    
    /**
     * Test OpenAI Chat
     */
    @PostMapping("/openai")
    public Map<String, Object> testOpenAI(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (openAiChatModel == null) {
                response.put("error", "OpenAI Chat Model not configured");
                response.put("status", "failed");
                return response;
            }
            
            String prompt = request.getOrDefault("prompt", "Say hello in Korean");
            
            ChatClient chatClient = ChatClient.builder(openAiChatModel).build();
            String answer = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            
            response.put("model", "OpenAI GPT-4o-mini");
            response.put("prompt", prompt);
            response.put("response", answer);
            response.put("status", "success");
            
        } catch (Exception e) {
            response.put("error", e.getMessage());
            response.put("status", "failed");
            response.put("exception", e.getClass().getSimpleName());
        }
        
        return response;
    }
    
    /**
     * Test Vertex AI Gemini
     */
    @PostMapping("/gemini")
    public Map<String, Object> testGemini(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (geminiChatModel == null) {
                response.put("error", "Vertex AI Gemini Chat Model not configured");
                response.put("status", "failed");
                return response;
            }
            
            String prompt = request.getOrDefault("prompt", "Say hello in Korean");
            
            ChatClient chatClient = ChatClient.builder(geminiChatModel).build();
            String answer = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            
            response.put("model", "Vertex AI Gemini 2.0 Flash");
            response.put("prompt", prompt);
            response.put("response", answer);
            response.put("status", "success");
            
        } catch (Exception e) {
            response.put("error", e.getMessage());
            response.put("status", "failed");
            response.put("exception", e.getClass().getSimpleName());
        }
        
        return response;
    }
}