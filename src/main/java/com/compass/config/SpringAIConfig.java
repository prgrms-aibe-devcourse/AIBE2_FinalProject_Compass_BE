package com.compass.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
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

    @PostConstruct
    public void init() {
        log.info("========================================");
        log.info("Spring AI 설정 초기화");
        log.info("Project ID: {}", projectId);
        log.info("Location: {}", location);
        log.info("Model: {}", modelName);
        log.info("Credentials: {}", credentialsPath.isEmpty() ? "NOT SET" : "SET");
        log.info("========================================");
    }

    // Spring Boot Starter가 자동으로 VertexAI와 ChatModel을 설정합니다.
    // application.yml의 spring.ai.vertex.ai.gemini 설정과
    // GOOGLE_APPLICATION_CREDENTIALS 환경변수를 통해 자동 구성됩니다.
}