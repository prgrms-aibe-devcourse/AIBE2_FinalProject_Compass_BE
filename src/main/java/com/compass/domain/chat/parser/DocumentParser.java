package com.compass.domain.chat.parser;

import com.compass.domain.chat.model.dto.ConfirmedSchedule;
import com.compass.domain.chat.model.dto.OCRText;
import com.compass.domain.chat.model.enums.DocumentType;

// 문서 타입별 파싱 인터페이스
public interface DocumentParser {
    // 지원하는 문서 타입
    DocumentType getSupportedType();

    // OCR 텍스트를 확정 일정으로 파싱
    ConfirmedSchedule parse(OCRText ocrText, String imageUrl);

    // 파싱 가능 여부 확인
    default boolean canParse(DocumentType type) {
        return getSupportedType() == type;
    }
}