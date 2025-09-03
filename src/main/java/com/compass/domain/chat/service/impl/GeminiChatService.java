package com.compass.domain.chat.service.impl;

import com.compass.domain.chat.service.ChatModelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Gemini Chat Service Implementation
 * Primary chat service using Gemini 2.0 Flash model
 */
@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class GeminiChatService implements ChatModelService {
    
    private final VertexAiGeminiChatModel geminiChatModel;
    
    @Override
    public String generateResponse(String userMessage) {
        try {
            log.debug("Generating response with Gemini for message: {}", userMessage);
            
            ChatClient chatClient = ChatClient.builder(geminiChatModel).build();
            String response = chatClient.prompt()
                    .user(userMessage)
                    .call()
                    .content();
            
            log.debug("Gemini response generated successfully");
            return response;
            
        } catch (Exception e) {
            log.error("Error generating response with Gemini: ", e);
            throw new RuntimeException("Failed to generate response with Gemini", e);
        }
    }
    
    @Override
    public String generateResponse(String systemPrompt, String userMessage) {
        try {
            log.debug("Generating response with system prompt");
            
            var systemMsg = new SystemMessage(systemPrompt);
            var userMsg = new UserMessage(userMessage);
            var prompt = new Prompt(List.of(systemMsg, userMsg));
            
            String response = geminiChatModel.call(prompt).getResult().getOutput().getContent();
            
            log.debug("Gemini response with system prompt generated successfully");
            return response;
            
        } catch (Exception e) {
            log.error("Error generating response with Gemini: ", e);
            throw new RuntimeException("Failed to generate response with Gemini", e);
        }
    }
    
    @Override
    public String getModelName() {
        return "Gemini 2.0 Flash";
    }
}