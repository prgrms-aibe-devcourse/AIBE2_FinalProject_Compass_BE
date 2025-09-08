package com.compass.domain.chat.entity;

import com.compass.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Chat Thread Entity
 * Represents a conversation thread between a user and the AI assistant
 */
@Entity
@Table(name = "chat_threads", indexes = {
    @Index(name = "idx_chat_thread_user_id", columnList = "user_id"),
    @Index(name = "idx_chat_thread_last_message", columnList = "last_message_at DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatThread {
    
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(length = 36)
    private String id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(length = 255)
    private String title;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;
    
    @OneToMany(mappedBy = "thread", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("timestamp DESC")
    @Builder.Default
    private List<ChatMessage> messages = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (title == null || title.isBlank()) {
            title = "New Chat";
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Add a message to this thread
     */
    public void addMessage(ChatMessage message) {
        messages.add(message);
        message.setThread(this);
        this.lastMessageAt = message.getTimestamp();
    }
    
    /**
     * Get the latest message preview for display
     */
    @Transient
    public String getLatestMessagePreview() {
        // Don't access the messages collection directly to avoid lazy loading issues
        // This should be populated by the service layer if needed
        return "New conversation";
    }
}