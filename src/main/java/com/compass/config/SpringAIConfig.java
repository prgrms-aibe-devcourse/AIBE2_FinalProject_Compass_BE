package com.compass.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

// Spring AI 설정 - 실제 Gemini API 사용
@Slf4j
@Configuration
public class SpringAIConfig {

    @Value("${GOOGLE_CLOUD_PROJECT_ID:travelagent-468611}")
    private String projectId;

    @Value("${GOOGLE_CLOUD_LOCATION:asia-northeast3}")
    private String location;

    @Value("${GOOGLE_APPLICATION_CREDENTIALS}")
    private String credentialsPath;

    // Spring Boot Starter가 자동으로 VertexAI와 ChatModel을 설정합니다.
    // application.yml의 spring.ai.vertex.ai.gemini 설정과
    // GOOGLE_APPLICATION_CREDENTIALS 환경변수를 통해 자동 구성됩니다.

    // 필요한 경우 여기에 추가 설정을 할 수 있습니다.
}