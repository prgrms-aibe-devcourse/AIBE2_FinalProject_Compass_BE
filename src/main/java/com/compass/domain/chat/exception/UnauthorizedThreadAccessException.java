package com.compass.domain.chat.exception;

/**
 * Exception thrown when a user tries to access a thread they don't own
 */
public class UnauthorizedThreadAccessException extends ChatException {
    
    public UnauthorizedThreadAccessException(String threadId, Long userId) {
        super(String.format("User %d is not authorized to access thread: %s", userId, threadId));
    }
    
    public UnauthorizedThreadAccessException(String message) {
        super(message);
    }
}