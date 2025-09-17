package com.compass.domain.chat.model.request;

public record ImageUrlRequest(String imageUrl) {
    public ImageUrlRequest {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new IllegalArgumentException("이미지 URL이 필요합니다.");
        }
    }
}
