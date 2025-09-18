package com.compass.domain.chat.function.processing;

import com.compass.domain.chat.model.dto.OCRText;
import com.compass.domain.chat.model.enums.DocumentType;
import com.compass.domain.chat.model.request.ImageUrlRequest;
import com.compass.domain.chat.model.response.OCRResult;
import com.compass.domain.chat.service.external.OCRClient;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessOCRFunction implements Function<ImageUrlRequest, OCRResult> {

    private final OCRClient ocrClient;
    private final Map<DocumentType, Function<OCRText, ?>> documentParsers = new EnumMap<>(DocumentType.class);

    @Override
    public OCRResult apply(ImageUrlRequest request) {
        try {
            var text = ocrClient.extractTextFromUrl(request.imageUrl());
            var documentType = detectDocumentType(text);
            triggerParserIfExists(documentType, text);
            return new OCRResult(request.imageUrl(), text, documentType);
        } catch (Exception e) {
            log.error("OCR 처리 실패 - url: {}", request.imageUrl(), e);
            throw new IllegalStateException("OCR 처리 중 문제가 발생했습니다.");
        }
    }

    private DocumentType detectDocumentType(String text) {
        var type = ocrClient.detectDocument(text);
        if (type == DocumentType.UNKNOWN) {
            log.debug("문서 유형을 판별하지 못했습니다.");
        }
        return type;
    }

    private void triggerParserIfExists(DocumentType type, String text) {
        var parser = documentParsers.get(type);
        if (parser == null) {
            log.debug("등록된 파서가 없어 스킵합니다 - type: {}", type);
            return;
        }
        try {
            parser.apply(new OCRText(text));
        } catch (Exception ex) {
            log.warn("추가 파서 실행 중 예외 발생 - type: {}", type, ex);
        }
    }

    public void registerParser(DocumentType type, Function<OCRText, ?> parser) {
        documentParsers.put(type, parser);
    }
}
