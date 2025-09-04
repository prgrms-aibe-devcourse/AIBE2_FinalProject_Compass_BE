package com.compass.config;

import com.compass.config.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spring Boot 애플리케이션 컨텍스트가 테스트 환경에서
 * 필요한 모든 설정을 포함하여 정상적으로 로드되는지 검증하는 테스트.
 */
@IntegrationTest
@DisplayName("Configuration Tests")
public class ConfigurationTest {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("애플리케이션 컨텍스트가 성공적으로 로드되어야 한다")
    void contextLoads() {
        // 이 테스트는 @SpringBootTest가 붙은 테스트 클래스가 실행될 때,
        // Spring ApplicationContext가 오류 없이 성공적으로 로드되는지 확인합니다.
        // 추가적으로, 주요 설정에 의존하는 Bean이 정상적으로 주입되는지 검증합니다.
        assertThat(jwtTokenProvider).isNotNull();
    }
}