package com.compass.domain.chat.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Chat Message Entity
 * Represents a single message in a chat conversation
 */
@Entity
@Table(name = "chat_messages", indexes = {
    @Index(name = "idx_chat_message_thread_id", columnList = "thread_id"),
    @Index(name = "idx_chat_message_timestamp", columnList = "timestamp DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id", nullable = false)
    private ChatThread thread;
    
    @Column(nullable = false, length = 20)
    private String role; // "user", "assistant", "system"
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    // Optional fields for context management
    @Column(name = "token_count")
    private Integer tokenCount;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata; // JSONB for flexible additional data
    
    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
    
    /**
     * Get timestamp as milliseconds for backward compatibility
     */
    @Transient
    public long getTimestampMillis() {
        return timestamp != null ? 
            timestamp.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : 
            System.currentTimeMillis();
    }
}