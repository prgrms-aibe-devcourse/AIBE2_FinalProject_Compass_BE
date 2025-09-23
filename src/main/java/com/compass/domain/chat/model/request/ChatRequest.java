package com.compass.domain.chat.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 통합 채팅 요청 모델
 * 
 * 모든 채팅 요청의 기본 구조를 제공하는 통일된 요청 형식
 * - 메시지, threadId, userId를 포함한 핵심 정보
 * - 메타데이터를 통한 확장 가능한 구조
 * - 검증 어노테이션을 통한 데이터 무결성 보장
 * 
 * @author Trip 개발자
 * @since 2024-12-30
 */
public record ChatRequest(
    @NotBlank(message = "메시지는 필수입니다")
    @Size(max = 4000, message = "메시지는 4000자를 초과할 수 없습니다")
    String message,
    
    @NotNull(message = "Thread ID는 필수입니다")
    String threadId,
    
    @NotNull(message = "User ID는 필수입니다")
    String userId,
    
    Map<String, Object> metadata,
    
    LocalDateTime timestamp
) {
    /**
     * 기본 생성자 - timestamp 자동 설정
     */
    public ChatRequest {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
    
    /**
     * 메타데이터가 없는 간단한 생성자
     */
    public ChatRequest(String message, String threadId, String userId) {
        this(message, threadId, userId, null, LocalDateTime.now());
    }
    
    /**
     * 메타데이터 포함 생성자
     */
    public ChatRequest(String message, String threadId, String userId, Map<String, Object> metadata) {
        this(message, threadId, userId, metadata, LocalDateTime.now());
    }
    
    /**
     * 메시지가 비어있는지 확인
     */
    public boolean isEmpty() {
        return message == null || message.trim().isEmpty();
    }
    
    /**
     * 메시지가 여행 관련인지 간단 체크
     */
    public boolean isTravelRelated() {
        if (message == null) return false;
        String lowerMessage = message.toLowerCase();
        return lowerMessage.contains("여행") || 
               lowerMessage.contains("여행계획") ||
               lowerMessage.contains("여행지") ||
               lowerMessage.contains("여행일정");
    }
    
    /**
     * 메타데이터에서 특정 값 조회
     */
    @SuppressWarnings("unchecked")
    public <T> T getMetadataValue(String key, Class<T> type) {
        if (metadata == null) return null;
        Object value = metadata.get(key);
        if (value != null && type.isAssignableFrom(value.getClass())) {
            return (T) value;
        }
        return null;
    }
    
    /**
     * 메타데이터에 값 추가 (불변 객체이므로 새 인스턴스 반환)
     */
    public ChatRequest withMetadata(String key, Object value) {
        Map<String, Object> newMetadata = new java.util.HashMap<>();
        if (metadata != null) {
            newMetadata.putAll(metadata);
        }
        newMetadata.put(key, value);
        return new ChatRequest(message, threadId, userId, newMetadata, timestamp);
    }
    
    // 기존 코드와의 호환성을 위한 getter 메서드들
    public String getMessage() {
        return message;
    }
    
    public String getThreadId() {
        return threadId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    // 기존 코드와의 호환성을 위한 setter 대체 메서드들
    public ChatRequest setUserId(String userId) {
        return new ChatRequest(message, threadId, userId, metadata, timestamp);
    }
    
    public ChatRequest setThreadId(String threadId) {
        return new ChatRequest(message, threadId, userId, metadata, timestamp);
    }
}
