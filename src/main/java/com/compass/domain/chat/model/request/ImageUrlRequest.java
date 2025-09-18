package com.compass.domain.chat.model.request;

public record ImageUrlRequest(
        String imageUrl,
        String threadId,
        String userId,
        String contentType
) {
    public ImageUrlRequest {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new IllegalArgumentException("이미지 URL이 비어 있습니다.");
        }
    }
}
