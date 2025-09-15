package com.compass.domain.chat.exception;

/**
 * 꼬리질문 플로우 관련 예외
 */
public class FollowUpException extends RuntimeException {
    
    public FollowUpException(String message) {
        super(message);
    }
    
    public FollowUpException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * 세션 관련 예외
     */
    public static class SessionNotFoundException extends FollowUpException {
        public SessionNotFoundException(String sessionId) {
            super("세션을 찾을 수 없습니다: " + sessionId);
        }
    }
    
    /**
     * 세션 만료 예외
     */
    public static class SessionExpiredException extends FollowUpException {
        public SessionExpiredException(String sessionId) {
            super("세션이 만료되었습니다: " + sessionId);
        }
    }
    
    /**
     * 사용자 관련 예외
     */
    public static class UserNotFoundException extends FollowUpException {
        public UserNotFoundException(String userId) {
            super("사용자를 찾을 수 없습니다: " + userId);
        }
    }
    
    /**
     * 유효하지 않은 사용자 ID 예외
     */
    public static class InvalidUserIdException extends FollowUpException {
        public InvalidUserIdException(String userId) {
            super("유효하지 않은 사용자 ID입니다: " + userId);
        }
    }
    
    /**
     * 저장 실패 예외
     */
    public static class StateSaveException extends FollowUpException {
        public StateSaveException(String message, Throwable cause) {
            super("상태 저장 실패: " + message, cause);
        }
    }
}