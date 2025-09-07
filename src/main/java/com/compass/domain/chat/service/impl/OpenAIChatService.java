package com.compass.domain.chat.service.impl;

import com.compass.domain.chat.service.ChatModelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * OpenAI Chat Service Implementation
 * Fallback chat service using GPT-4o-mini model
 * REQ-LLM-006: Includes conversation context management (not yet implemented for OpenAI)
 */
@Slf4j
@Service("openAIChatService")
@RequiredArgsConstructor
public class OpenAIChatService implements ChatModelService {
    
    private final OpenAiChatModel openAiChatModel;
    
    @Override
    public String generateResponse(String userMessage) {
        try {
            log.debug("Generating response with OpenAI for message: {}", userMessage);
            
            ChatClient chatClient = ChatClient.builder(openAiChatModel).build();
            
            String response = chatClient.prompt()
                    .user(userMessage)
                    .call()
                    .content();
            
            log.debug("OpenAI response generated successfully");
            return response;
            
        } catch (Exception e) {
            log.error("Error generating response with OpenAI: ", e);
            throw new RuntimeException("Failed to generate response with OpenAI", e);
        }
    }
    
    @Override
    public String generateResponse(String systemPrompt, String userMessage) {
        try {
            log.debug("Generating response with system prompt");
            
            var systemMsg = new SystemMessage(systemPrompt);
            var userMsg = new UserMessage(userMessage);
            var prompt = new Prompt(List.of(systemMsg, userMsg));
            
            String response = openAiChatModel.call(prompt).getResult().getOutput().getContent();
            
            log.debug("OpenAI response with system prompt generated successfully");
            return response;
            
        } catch (Exception e) {
            log.error("Error generating response with OpenAI: ", e);
            throw new RuntimeException("Failed to generate response with OpenAI", e);
        }
    }
    
    @Override
    public String getModelName() {
        return "GPT-4o-mini";
    }
    
    @Override
    public String generateResponseWithContext(String threadId, String userMessage) {
        // For now, OpenAI service doesn't implement context management
        // Just delegate to the regular generateResponse method
        log.warn("Context management not implemented for OpenAI service, using regular response generation");
        return generateResponse(userMessage);
    }
    
    @Override
    public String generateResponseWithContext(String threadId, String systemPrompt, String userMessage) {
        // For now, OpenAI service doesn't implement context management
        // Just delegate to the regular generateResponse method
        log.warn("Context management not implemented for OpenAI service, using regular response generation");
        return generateResponse(systemPrompt, userMessage);
    }
}