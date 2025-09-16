package com.compass.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

// Spring AI 설정
@Slf4j
@Configuration
public class SpringAIConfig {

    // Vertex AI Gemini ChatModel 빈 등록
    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.ai.vertex.ai.gemini.project-id")
    public ChatModel vertexAiGeminiChatModel() {
        log.info("Vertex AI Gemini ChatModel 설정 중...");

        // Spring Boot Starter가 자동으로 설정하므로
        // 여기서는 추가 설정만 필요한 경우에 사용
        // 실제 ChatModel은 AutoConfiguration으로 생성됨

        return null; // AutoConfiguration이 처리
    }

    // 개발 환경용 Mock ChatModel
    @Bean
    @ConditionalOnProperty(name = "spring.ai.vertex.ai.gemini.project-id", matchIfMissing = true, havingValue = "false")
    public ChatModel mockChatModel() {
        log.warn("Mock ChatModel 사용 중 - 실제 LLM 연동 안 됨");
        return null; // Mock 모드
    }
}