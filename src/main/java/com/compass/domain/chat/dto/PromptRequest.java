package com.compass.domain.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for building prompts from templates
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptRequest {
    
    @NotBlank(message = "Template name is required")
    private String templateName;
    
    @NotNull(message = "Parameters are required")
    private Map<String, Object> parameters;
    
    private String sessionId;
    
    private String userId;
    
    private Map<String, Object> context;
}