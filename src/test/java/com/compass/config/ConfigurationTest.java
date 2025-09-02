package com.compass.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Basic configuration test to ensure the test environment is properly set up
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Configuration Tests")
public class ConfigurationTest {

    @Test
    @DisplayName("Test environment configuration")
    void testEnvironmentConfiguration() {
        // This test verifies that the test environment can be loaded
        assertTrue(true, "Test environment is properly configured");
    }
}