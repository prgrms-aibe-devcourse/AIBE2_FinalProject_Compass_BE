package com.compass.domain.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public class ChatDtos {

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
            long timestamp
    ) {}

    public record ThreadDto(
            String id,
            String userId,
            LocalDateTime createdAt,
            String title,
            String preview
    ) {}
}
