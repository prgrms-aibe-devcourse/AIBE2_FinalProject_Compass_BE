package com.compass.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for chat with function calling capabilities
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FunctionChatResponse {
    
    private String messageId;
    
    private String message;
    
    private String model;
    
    private boolean functionsUsed;
    
    private LocalDateTime timestamp;
    
    private Long processingTimeMs;
    
    private String[] functionsInvoked;
}