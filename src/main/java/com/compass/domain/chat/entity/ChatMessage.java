package com.compass.domain.chat.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Chat Message Entity
 * Represents a single message in a chat conversation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    
    private Long id;
    private String threadId;
    private String role; // "user", "assistant", "system"
    private String content;
    private LocalDateTime timestamp;
    
    // Optional fields for context management
    private Integer tokenCount;
    private String metadata; // JSON string for additional data
}