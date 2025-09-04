package com.compass.domain.chat.config;

import com.compass.domain.chat.parser.core.TripPlanningParser;
import com.compass.domain.chat.parser.impl.PatternBasedParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Configuration for trip planning parsers.
 * 
 * This configuration manages the creation and wiring of parser beans
 * based on active profiles and available dependencies.
 * 
 * Profiles:
 * - test: Only pattern-based parser (no AI dependencies)
 * - dev/prod: Full hybrid parser with AI enhancement
 */
@Slf4j
@Configuration
public class ParserConfiguration {
    
    /**
     * Fallback parser for test environments.
     * This bean is created only in test profile when hybrid parser is not available.
     * It provides a simple, dependency-free parser for testing.
     */
    @Bean
    @Primary
    @Profile("test")
    TripPlanningParser testParser() {
        log.info("Creating test parser (pattern-based only)");
        return new PatternBasedParser();
    }
    
    /**
     * Default parser bean for cases where no specific parser is configured.
     * This provides a fallback to ensure the application can always start.
     */
    @Bean
    @ConditionalOnMissingBean(TripPlanningParser.class)
    TripPlanningParser defaultParser() {
        log.warn("No specific parser configured, using pattern-based parser as default");
        return new PatternBasedParser();
    }
    
    /**
     * Log parser configuration on startup for debugging.
     */
    @Bean
    ParserConfigurationLogger parserConfigLogger() {
        return new ParserConfigurationLogger();
    }
    
    /**
     * Helper class to log parser configuration.
     */
    public static class ParserConfigurationLogger {
        public ParserConfigurationLogger() {
            log.info("=== Parser Configuration ===");
            log.info("Available parsers will be configured based on profile and dependencies");
            log.info("Test profile: pattern-based parser only");
            log.info("Other profiles: hybrid parser with AI enhancement (if available)");
            log.info("===========================");
        }
    }
}