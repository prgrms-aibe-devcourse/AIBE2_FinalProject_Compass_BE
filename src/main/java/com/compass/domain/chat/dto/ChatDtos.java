package com.compass.domain.chat.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public class ChatDtos {
    /**
     * REQ-CHAT-006: 메시지 입력 검증
     */
    public record MessageCreateDto(
            @NotBlank(message = "Content cannot be empty.")
            @Size(min = 1, max = 1000, message = "Content must be between 1 and 1000 characters.")
            String content
    ) {}

    public record MessageDto(
            String id,
            String threadId,
            String role, // "user" or "ai"
            String content,
            long timestamp,
            FollowUpResponseDto followUpQuestion // REQ-FOLLOW: Follow-up question data
    ) {
        // Backward compatibility constructor
        public MessageDto(String id, String threadId, String role, String content, long timestamp) {
            this(id, threadId, role, content, timestamp, null);
        }
    }

    public record ThreadDto(
            String id,
            String userId,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime createdAt,
            String latestMessagePreview,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime lastMessageAt,
            String title
    ) {
        // Backward compatibility constructor
        public ThreadDto(String id, String userId, LocalDateTime createdAt, String latestMessagePreview) {
            this(id, userId, createdAt, latestMessagePreview, null, latestMessagePreview);
        }
    }
}
