package com.compass.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

// Spring AI ChatModel 충돌 해결을 위한 설정
@Configuration
public class SpringAIConfig {

    @Bean
    @Primary
    public ChatModel primaryChatModel(@Qualifier("vertexAiGeminiChat") ChatModel geminiChatModel) {
        return geminiChatModel;
    }
}