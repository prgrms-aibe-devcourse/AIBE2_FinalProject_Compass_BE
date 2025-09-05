package com.compass.config;

import com.compass.config.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Configuration Tests")
public class ConfigurationTest extends BaseIntegrationTest {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("애플리케이션 컨텍스트가 성공적으로 로드되어야 한다")
    void contextLoads() {
        assertThat(jwtTokenProvider).isNotNull();
    }
}