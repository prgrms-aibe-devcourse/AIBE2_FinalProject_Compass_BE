package com.compass.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration to load .env file variables into Spring Boot
 */
public class DotEnvConfig implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        try {
            // Load .env file from project root
            Dotenv dotenv = Dotenv.configure()
                    .ignoreIfMissing()
                    .load();

            // Convert dotenv entries to a Map
            Map<String, Object> envMap = new HashMap<>();
            dotenv.entries().forEach(entry -> {
                String key = entry.getKey();
                String value = entry.getValue();
                envMap.put(key, value);
                
                // Also add to system properties for Spring AI
                System.setProperty(key, value);
            });

            // Add as a property source with high priority
            MapPropertySource propertySource = new MapPropertySource("dotenv", envMap);
            environment.getPropertySources().addFirst(propertySource);

            System.out.println("✅ Loaded " + envMap.size() + " environment variables from .env file");
        } catch (Exception e) {
            System.err.println("⚠️ Warning: Could not load .env file: " + e.getMessage());
        }
    }
}