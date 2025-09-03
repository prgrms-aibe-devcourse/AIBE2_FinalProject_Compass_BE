package com.compass.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Response DTO for prompt template operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptResponse {
    
    private String prompt;
    
    private String templateName;
    
    private Map<String, Object> parameters;
    
    private LocalDateTime timestamp;
    
    private boolean success;
    
    private String errorMessage;
    
    private Map<String, Object> metadata;
}