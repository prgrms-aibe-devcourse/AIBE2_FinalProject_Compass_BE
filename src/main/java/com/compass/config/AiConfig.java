package com.compass.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Spring AI Configuration for LLM Integration
 * 
 * This configuration sets up multiple AI models:
 * - Google Gemini 2.0 Flash for fast responses (primary)
 * - OpenAI GPT-4o-mini as fallback
 * 
 * As per CLAUDE.md specifications:
 * - Primary Agent: Gemini 2.0 Flash (general chat)
 * - Fallback: GPT-4o-mini
 */
@Configuration
public class AiConfig {

    @Value("${spring.ai.vertex.ai.gemini.chat.options.model:gemini-2.0-flash}")
    private String defaultGeminiModel;

    /**
     * Primary ChatClient using Gemini Flash for general chat operations
     * This is the default client for most chat interactions
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.ai.vertex.ai.gemini.project-id", matchIfMissing = false)
    public ChatClient geminiChatClient(VertexAiGeminiChatModel vertexAiGeminiChatModel) {
        return ChatClient.builder(vertexAiGeminiChatModel)
                .build();
    }


    /**
     * OpenAI ChatClient as a fallback or alternative option
     * Uses GPT-4o-mini model
     */
    @Bean(name = "openAiChatClient")
    @ConditionalOnProperty(name = "spring.ai.openai.api-key", matchIfMissing = false)
    public ChatClient openAiChatClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel)
                .build();
    }

    // Note: ChatModel beans are auto-configured by Spring AI
    // We only need to reference them, not redefine them

    /**
     * Model selector service for choosing between Flash and Pro models
     * based on task complexity
     */
    @Bean
    public ModelSelector modelSelector(
            @Qualifier("geminiChatClient") @Autowired(required = false) ChatClient geminiFlashClient,
            @Qualifier("openAiChatClient") @Autowired(required = false) ChatClient openAiClient) {
        return new ModelSelector(geminiFlashClient, openAiClient);
    }

    /**
     * Inner class for model selection logic
     */
    public static class ModelSelector {
        private final ChatClient geminiFlashClient;
        private final ChatClient openAiClient;

        public ModelSelector(ChatClient geminiFlashClient, 
                           ChatClient openAiClient) {
            this.geminiFlashClient = geminiFlashClient;
            this.openAiClient = openAiClient;
        }

        /**
         * Select appropriate model based on availability
         * 
         * @return Available ChatClient (Gemini preferred, OpenAI as fallback)
         */
        public ChatClient selectModel() {
            return geminiFlashClient != null ? geminiFlashClient : openAiClient;
        }
    }

}