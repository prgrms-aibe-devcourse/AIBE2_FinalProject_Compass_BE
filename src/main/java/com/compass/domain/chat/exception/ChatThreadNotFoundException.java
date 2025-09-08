package com.compass.domain.chat.exception;

/**
 * Exception thrown when a chat thread is not found
 */
public class ChatThreadNotFoundException extends ChatException {
    
    public ChatThreadNotFoundException(String threadId) {
        super("Chat thread not found with ID: " + threadId);
    }
    
    public ChatThreadNotFoundException(String threadId, Long userId) {
        super(String.format("Chat thread not found with ID: %s for user: %d", threadId, userId));
    }
}