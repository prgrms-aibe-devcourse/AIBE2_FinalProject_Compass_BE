package com.compass.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.annotation.PostConstruct;

// Spring AI 설정 - 실제 Gemini API 사용
@Slf4j
@Configuration
public class SpringAIConfig {

    @Value("${spring.ai.vertex.ai.gemini.project-id:travelagent-468611}")
    private String projectId;

    @Value("${spring.ai.vertex.ai.gemini.location:asia-northeast3}")
    private String location;

    @Value("${spring.ai.vertex.ai.gemini.model:gemini-2.0-flash}")
    private String modelName;

    @Value("${GOOGLE_APPLICATION_CREDENTIALS:}")
    private String credentialsPath;

    @Autowired(required = false)
    private VertexAiGeminiChatModel vertexAiGeminiChatModel;

    @PostConstruct
    public void init() {
        log.info("========================================");
        log.info("Spring AI 설정 초기화");
        log.info("Project ID: {}", projectId);
        log.info("Location: {}", location);
        log.info("Model: {}", modelName);
        log.info("Credentials: {}", credentialsPath.isEmpty() ? "NOT SET" : "SET");

        // 환경 변수로 모델 설정 강제
        System.setProperty("spring.ai.vertex.ai.gemini.chat.options.model", modelName);
        log.info("Model 환경변수 설정: {}", modelName);
        log.info("========================================");
    }

    // Primary ChatModel Bean 설정
    @Bean
    @Primary
    public ChatModel primaryChatModel() {
        if (vertexAiGeminiChatModel != null) {
            log.info("Primary ChatModel: VertexAI Gemini 사용");
            return vertexAiGeminiChatModel;
        }
        throw new IllegalStateException("No ChatModel available");
    }
}