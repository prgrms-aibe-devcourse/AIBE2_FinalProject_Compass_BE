package com.compass.domain.chat.model.dto;

public record OCRText(String rawText) {
    public OCRText {
        if (rawText == null || rawText.isBlank()) {
            throw new IllegalArgumentException("텍스트가 비었습니다.");
        }
    }
}
