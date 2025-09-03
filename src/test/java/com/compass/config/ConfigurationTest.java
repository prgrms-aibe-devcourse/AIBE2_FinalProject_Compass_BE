package com.compass.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Basic configuration test to ensure the test environment is properly set up
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "jwt.access-secret=test-access-secret-key-for-config-tests-12345678901234567890",
        "jwt.refresh-secret=test-refresh-secret-key-for-config-tests-12345678901234567890",
        "jwt.access-expiration=3600000",
        "jwt.refresh-expiration=604800000"
})
@DisplayName("Configuration Tests")
public class ConfigurationTest {

    @Test
    @DisplayName("Test environment configuration")
    void testEnvironmentConfiguration() {
        // This test verifies that the test environment can be loaded
        assertTrue(true, "Test environment is properly configured");
    }
}