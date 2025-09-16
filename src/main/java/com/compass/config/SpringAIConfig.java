package com.compass.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vertexai.VertexAI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

import java.io.IOException;

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

    // Vertex AI 인증 설정
    @Bean
    public VertexAI vertexAI() throws IOException {
        log.info("Vertex AI 초기화 중 - Project: {}, Location: {}", projectId, location);

        // 서비스 계정 키 파일로 인증
        GoogleCredentials credentials = GoogleCredentials
                .fromStream(new FileSystemResource(credentialsPath).getInputStream());

        return new VertexAI(projectId, location, credentials);
    }

    // Spring Boot AutoConfiguration이 자동으로 ChatModel을 생성하므로
    // 추가 설정이 필요한 경우에만 커스텀 빈 정의
}