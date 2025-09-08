package com.compass.domain.chat.service.impl;

import com.compass.domain.chat.context.ConversationContextManager;
import com.compass.domain.chat.entity.ChatMessage;
import com.compass.domain.chat.service.ChatModelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Gemini Chat Service Implementation
 * Primary chat service using Gemini 2.0 Flash model
 * REQ-LLM-006: Includes conversation context management
 */
@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class GeminiChatService implements ChatModelService {
    
    private final VertexAiGeminiChatModel geminiChatModel;
    private final ConversationContextManager contextManager;
    
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
    
    @Override
    public String generateResponseWithContext(String threadId, String userMessage) {
        try {
            log.debug("Generating response with context for thread: {}", threadId);
            
            // Get conversation history from context manager
            List<Message> contextMessages = contextManager.getMessagesForAI(threadId);
            
            // Add current user message
            List<Message> allMessages = new ArrayList<>(contextMessages);
            allMessages.add(new UserMessage(userMessage));
            
            // Create prompt with full context
            Prompt prompt = new Prompt(allMessages);
            String response = geminiChatModel.call(prompt).getResult().getOutput().getContent();
            
            // Store messages in context for future use
            ChatMessage userMsg = ChatMessage.builder()
                    .threadId(threadId)
                    .role("user")
                    .content(userMessage)
                    .build();
            ChatMessage assistantMsg = ChatMessage.builder()
                    .threadId(threadId)
                    .role("assistant")
                    .content(response)
                    .build();
            
            contextManager.addMessage(threadId, userMsg);
            contextManager.addMessage(threadId, assistantMsg);
            
            log.debug("Context-aware response generated successfully");
            return response;
            
        } catch (Exception e) {
            log.error("Error generating context-aware response with Gemini: ", e);
            throw new RuntimeException("Failed to generate context-aware response", e);
        }
    }
    
    @Override
    public String generateResponseWithContext(String threadId, String systemPrompt, String userMessage) {
        try {
            log.debug("Generating response with context and system prompt for thread: {}", threadId);
            
            // Start with system message
            List<Message> allMessages = new ArrayList<>();
            allMessages.add(new SystemMessage(systemPrompt));
            
            // Add conversation history
            List<Message> contextMessages = contextManager.getMessagesForAI(threadId);
            allMessages.addAll(contextMessages);
            
            // Add current user message
            allMessages.add(new UserMessage(userMessage));
            
            // Create prompt with full context
            Prompt prompt = new Prompt(allMessages);
            String response = geminiChatModel.call(prompt).getResult().getOutput().getContent();
            
            // Store messages in context
            ChatMessage userMsg = ChatMessage.builder()
                    .threadId(threadId)
                    .role("user")
                    .content(userMessage)
                    .build();
            ChatMessage assistantMsg = ChatMessage.builder()
                    .threadId(threadId)
                    .role("assistant")
                    .content(response)
                    .build();
            
            contextManager.addMessage(threadId, userMsg);
            contextManager.addMessage(threadId, assistantMsg);
            
            log.debug("Context-aware response with system prompt generated successfully");
            return response;
            
        } catch (Exception e) {
            log.error("Error generating context-aware response with system prompt: ", e);
            throw new RuntimeException("Failed to generate context-aware response", e);
        }
    }
}