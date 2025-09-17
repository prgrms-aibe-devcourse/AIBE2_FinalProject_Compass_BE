package com.compass.domain.chat.model.response;

import com.compass.domain.chat.model.enums.DocumentType;

public record ImageProcessResult(
        String imageUrl,
        String extractedText,
        DocumentType documentType
) {
    public ImageProcessResult {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new IllegalArgumentException("이미지 URL이 비어 있습니다.");
        }
        extractedText = extractedText == null ? "" : extractedText.trim();
        documentType = documentType == null ? DocumentType.UNKNOWN : documentType;
    }
}
