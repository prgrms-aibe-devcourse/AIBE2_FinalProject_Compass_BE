package com.compass.domain.chat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * Request DTO for chat with function calling capabilities
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FunctionChatRequest {
    
    @NotBlank(message = "Message cannot be empty")
    private String message;
    
    private List<Message> conversationHistory;
    
    private String userId;
    
    private String sessionId;
    
    private boolean enableFunctions = true;
}