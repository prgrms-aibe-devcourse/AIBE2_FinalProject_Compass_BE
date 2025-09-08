package com.compass;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions;
import com.google.cloud.vertexai.VertexAI;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.List;

/**
 * Standalone test for Function Calling with Gemini
 * Run this directly to test if API keys work
 */
public class FunctionCallingStandaloneTest {
    
    public static void main(String[] args) {
        System.out.println("=== Function Calling Standalone Test ===");
        
        // Load environment variables
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();
        
        String projectId = dotenv.get("GOOGLE_CLOUD_PROJECT_ID");
        String location = dotenv.get("GOOGLE_CLOUD_LOCATION", "us-central1");
        String model = dotenv.get("GEMINI_MODEL", "gemini-2.0-flash");
        
        System.out.println("Project ID: " + projectId);
        System.out.println("Location: " + location);
        System.out.println("Model: " + model);
        
        if (projectId == null || projectId.isEmpty()) {
            System.err.println("ERROR: GOOGLE_CLOUD_PROJECT_ID not found in .env file");
            System.err.println("Please add it to your .env file");
            return;
        }
        
        try {
            // Initialize Vertex AI
            System.out.println("\nInitializing Vertex AI...");
            VertexAI vertexAI = new VertexAI(projectId, location);
            
            // Create chat model without GenerativeModel (not needed in Spring AI)
            VertexAiGeminiChatModel chatModel = new VertexAiGeminiChatModel(
                vertexAI, 
                VertexAiGeminiChatOptions.builder()
                    .withTemperature(0.7)
                    .build()
            );
            
            // Test simple prompt
            System.out.println("\nTesting simple travel query...");
            String testQuery = "제주도 2박3일 여행 계획 짜줘";
            
            Message userMessage = new UserMessage(testQuery);
            Prompt prompt = new Prompt(List.of(userMessage));
            
            System.out.println("User: " + testQuery);
            System.out.println("\nWaiting for Gemini response...");
            
            var response = chatModel.call(prompt);
            
            System.out.println("\n=== GEMINI RESPONSE ===");
            System.out.println(response.getResult().getOutput().getContent());
            System.out.println("======================");
            
            System.out.println("\n✅ Function Calling test successful!");
            
        } catch (Exception e) {
            System.err.println("❌ Error during test: " + e.getMessage());
            e.printStackTrace();
        }
    }
}