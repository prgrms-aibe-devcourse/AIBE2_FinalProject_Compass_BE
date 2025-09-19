package com.compass.domain.chat.model.dto;

public record OCRText(String rawText, String threadId, String userId, String imageUrl) {
    public OCRText {
        if (rawText == null || rawText.isBlank()) {
            throw new IllegalArgumentException("텍스트가 비었습니다.");
        }
        threadId = threadId == null ? "" : threadId;
        userId = userId == null ? "" : userId;
        imageUrl = imageUrl == null ? "" : imageUrl;
    }
}
