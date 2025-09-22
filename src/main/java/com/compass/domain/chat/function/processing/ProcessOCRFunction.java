package com.compass.domain.chat.function.processing;

import com.compass.domain.chat.model.dto.OCRText;
import com.compass.domain.chat.model.enums.DocumentType;
import com.compass.domain.chat.model.request.ImageUrlRequest;
import com.compass.domain.chat.model.response.OCRResult;
import com.compass.domain.chat.service.external.OCRClient;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessOCRFunction implements Function<ImageUrlRequest, OCRResult> {

    private static final int MIN_TEXT_LENGTH = 100;
    private static final int MAX_ATTEMPTS = 2;
    private static final float MIN_CONFIDENCE = 0.95F;

    private static final Map<DocumentType, Pattern> QUALITY_PATTERNS = Map.of(
            DocumentType.FLIGHT_RESERVATION, Pattern.compile("BOARDING PASS|FLIGHT|DEPARTURE", Pattern.CASE_INSENSITIVE),
            DocumentType.HOTEL_RESERVATION, Pattern.compile("CHECK[- ]?IN|CHECK[- ]?OUT|RESERVATION", Pattern.CASE_INSENSITIVE)
    );

    private final OCRClient ocrClient;
    private final Map<DocumentType, Function<OCRText, ?>> documentParsers = new EnumMap<>(DocumentType.class);

    @Override
    public OCRResult apply(ImageUrlRequest request) {
        try {
            var result = runOcrWithRetry(request);
            triggerParserIfExists(result.documentType(), result.extractedText(), request);
            return result;
        } catch (IllegalStateException e) {
            log.error("OCR 처리 실패 - url: {}", request.imageUrl(), e);
            throw e;
        } catch (Exception e) {
            log.error("OCR 처리 실패 - url: {}", request.imageUrl(), e);
            throw new IllegalStateException("OCR 처리 중 문제가 발생했습니다.", e);
        }
    }

    private OCRResult runOcrWithRetry(ImageUrlRequest request) {
        OCRClient.OcrExtraction extraction = new OCRClient.OcrExtraction("", 0F);
        DocumentType type = DocumentType.UNKNOWN;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            extraction = ocrClient.extractTextFromUrl(request.imageUrl());
            var text = extraction.text();
            type = detectDocumentType(text);
            if (isAcceptable(type, extraction)) {
                if (attempt > 1) {
                    log.debug("OCR 재시도 종료 - attempts: {}", attempt);
                }
                return new OCRResult(request.imageUrl(), text, type);
            }
            if (attempt < MAX_ATTEMPTS) {
                log.debug("OCR 결과 품질 부족 - 재시도, attempt: {}", attempt);
            }
        }
        log.warn("OCR 결과 정확도가 기준을 충족하지 못했습니다 - confidence: {}, url: {}", extraction.confidence(), request.imageUrl());
        throw new IllegalStateException("OCR 결과 정확도가 기준(95%)을 충족하지 못했습니다.");
    }

    private DocumentType detectDocumentType(String text) {
        var type = ocrClient.detectDocument(text);
        if (type == DocumentType.UNKNOWN) {
            log.debug("문서 유형을 판별하지 못했습니다.");
        }
        return type;
    }

    private boolean isAcceptable(DocumentType type, OCRClient.OcrExtraction extraction) {
        return meetsConfidence(extraction.confidence()) && meetsTextQuality(type, extraction.text());
    }

    private boolean meetsConfidence(float confidence) {
        return confidence >= MIN_CONFIDENCE;
    }

    private boolean meetsTextQuality(DocumentType type, String text) {
        if (text == null || text.length() < MIN_TEXT_LENGTH) {
            return false;
        }
        var pattern = QUALITY_PATTERNS.get(type);
        if (pattern == null) {
            return true;
        }
        return pattern.matcher(text).find();
    }

    private void triggerParserIfExists(DocumentType type, String text, ImageUrlRequest request) {
        var parser = documentParsers.get(type);
        if (parser == null) {
            log.debug("등록된 파서가 없어 스킵합니다 - type: {}", type);
            return;
        }
        try {
            parser.apply(new OCRText(text, request.threadId(), request.userId(), request.imageUrl()));
        } catch (Exception ex) {
            log.warn("추가 파서 실행 중 예외 발생 - type: {}", type, ex);
        }
    }

    public void registerParser(DocumentType type, Function<OCRText, ?> parser) {
        documentParsers.put(type, parser);
    }
}
