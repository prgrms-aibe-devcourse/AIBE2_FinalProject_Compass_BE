package com.compass.config;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/**
 * 통합 테스트를 위한 기본 설정 클래스
 * 모든 @SpringBootTest 클래스는 이 클래스를 상속받아 일관된 테스트 환경을 보장합니다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(EmbeddedRedisConfig.class)
@TestPropertySource(properties = {
        // Redis 설정 (CI 환경 호환)
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379",
        "spring.data.redis.timeout=10000",
        "spring.data.redis.lettuce.pool.max-active=8",
        "spring.data.redis.lettuce.pool.max-idle=8",
        "spring.data.redis.lettuce.pool.min-idle=0",
        
        // JWT 설정 (CI 환경 필수)
        "spring.security.jwt.secret=test-access-secret-key-for-ci-integration-test-12345678901234567890",
        "spring.security.jwt.refresh-secret=test-refresh-secret-key-for-ci-integration-test-12345678901234567890",
        "spring.security.jwt.access-expiration=3600000",
        "spring.security.jwt.refresh-expiration=604800000",
        
        // AI 설정 (CI 환경 호환)
        "spring.ai.vertex.ai.gemini.project-id=test-project",
        "spring.ai.vertex.ai.gemini.location=us-central1",
        "spring.ai.openai.api-key=test-key",
        
        // 데이터베이스 설정 (H2 PostgreSQL 모드)
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.defer-datasource-initialization=true",
        
        // 로깅 설정 (CI 환경 디버깅)
        "logging.level.redis.embedded=WARN",
        "logging.level.io.lettuce=WARN",
        "logging.level.org.springframework.data.redis=WARN"
})
public abstract class BaseIntegrationTest {
    // 공통 테스트 설정이 필요한 경우 여기에 추가
}
