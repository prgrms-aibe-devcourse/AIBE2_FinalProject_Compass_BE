package com.compass.domain.chat.function.processing.event;

public record ImageOcrQueuedEvent(
        String objectKey,
        String imageUrl,
        String threadId,
        String userId,
        String contentType
) {
}
