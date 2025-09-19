package com.compass.domain.chat.model.response;

import com.compass.domain.chat.model.enums.DocumentType;

public record OCRResult(
        String imageUrl,
        String extractedText,
        DocumentType documentType
) {
    public OCRResult {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new IllegalArgumentException("이미지 URL이 비어 있습니다.");
        }
        extractedText = extractedText == null ? "" : extractedText;
        documentType = documentType == null ? DocumentType.UNKNOWN : documentType;
    }
}
