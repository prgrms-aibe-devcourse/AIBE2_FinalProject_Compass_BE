package com.compass.config;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        // JWT
        "jwt.access-secret=test-access-secret-key-for-integration-test-12345678901234567890",
        "jwt.refresh-secret=test-refresh-secret-key-for-integration-test-12345678901234567890",
        "jwt.access-expiration=3600000",
        "jwt.refresh-expiration=604800000",
        // Redis
        "spring.data.redis.port=63790", // 모든 테스트가 이 포트를 공유
        // AI
        "spring.ai.vertex.ai.gemini.project-id=test-project",
        "spring.ai.vertex.ai.gemini.location=asia-northeast3",
        "spring.ai.openai.api-key=test-key"
})
public @interface IntegrationTest {
}