package com.compass.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

/**
 * Google Cloud Configuration
 * Handles both local JSON file and BASE64 credentials from environment variable
 */
@Configuration
public class GoogleCloudConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(GoogleCloudConfig.class);
    
    @Value("${GOOGLE_CREDENTIALS_BASE64:#{null}}")
    private String credentialsBase64;
    
    @Value("${GOOGLE_APPLICATION_CREDENTIALS:#{null}}")
    private String credentialsFilePath;
    
    @Bean
    public CommandLineRunner setupGoogleCredentials() {
        return args -> {
            // Priority 1: Check if GOOGLE_APPLICATION_CREDENTIALS points to an existing file
            if (credentialsFilePath != null && !credentialsFilePath.isEmpty()) {
                File credentialFile = new File(credentialsFilePath);
                if (credentialFile.exists()) {
                    System.setProperty("GOOGLE_APPLICATION_CREDENTIALS", credentialsFilePath);
                    logger.info("✅ Google Cloud credentials configured from file: {}", credentialsFilePath);
                    return;
                }
                
                // Try relative path from project root
                String projectRoot = System.getProperty("user.dir");
                Path[] possiblePaths = {
                    Paths.get(projectRoot, "travelagent-468611-1ae0c9d4e187.json"),
                    Paths.get(projectRoot, "google-credentials.json"),
                    Paths.get("/app/google-credentials.json") // Docker path
                };
                
                for (Path path : possiblePaths) {
                    if (Files.exists(path)) {
                        System.setProperty("GOOGLE_APPLICATION_CREDENTIALS", path.toString());
                        logger.info("✅ Google Cloud credentials configured from file: {}", path);
                        return;
                    }
                }
            }
            
            // Priority 2: Use BASE64 credentials from environment variable (for AWS EC2)
            if (credentialsBase64 != null && !credentialsBase64.isEmpty()) {
                try {
                    // Decode BASE64 to JSON
                    byte[] decodedBytes = Base64.getDecoder().decode(credentialsBase64);
                    String jsonContent = new String(decodedBytes);
                    
                    // Write to temporary file
                    File tempFile = File.createTempFile("google-credentials", ".json");
                    tempFile.deleteOnExit();
                    
                    try (FileWriter writer = new FileWriter(tempFile)) {
                        writer.write(jsonContent);
                    }
                    
                    // Set Google Application Credentials environment variable
                    System.setProperty("GOOGLE_APPLICATION_CREDENTIALS", tempFile.getAbsolutePath());
                    logger.info("✅ Google Cloud credentials configured from BASE64");
                    logger.info("   Credentials file: {}", tempFile.getAbsolutePath());
                    
                } catch (Exception e) {
                    logger.error("⚠️ Failed to setup Google Cloud credentials from BASE64: {}", e.getMessage());
                }
            } else {
                logger.warn("⚠️ Google Cloud credentials not found. Please set either:");
                logger.warn("   1. GOOGLE_APPLICATION_CREDENTIALS environment variable pointing to JSON file");
                logger.warn("   2. GOOGLE_CREDENTIALS_BASE64 environment variable with BASE64 encoded JSON");
                logger.warn("   3. Place 'travelagent-468611-1ae0c9d4e187.json' in project root");
            }
        };
    }
}