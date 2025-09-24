package com.compass.domain.chat.function.processing;

import com.compass.domain.chat.model.dto.OCRText;
import com.compass.domain.chat.model.enums.DocumentType;
import com.compass.domain.chat.model.request.ImageUrlRequest;
import com.compass.domain.chat.model.response.OCRResult;
import com.compass.domain.chat.service.external.OCRClient;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ProcessOCRFunction implements Function<ImageUrlRequest, OCRResult> {

    private static final int MIN_TEXT_LENGTH = 100;
    private static final int MAX_ATTEMPTS = 2;

    private static final Map<DocumentType, Pattern> QUALITY_PATTERNS = Map.of(
            DocumentType.FLIGHT_RESERVATION, Pattern.compile("BOARDING PASS|FLIGHT|DEPARTURE", Pattern.CASE_INSENSITIVE),
            DocumentType.HOTEL_RESERVATION, Pattern.compile("CHECK[- ]?IN|CHECK[- ]?OUT|RESERVATION", Pattern.CASE_INSENSITIVE)
    );

    private final OCRClient ocrClient;
    
    public ProcessOCRFunction(OCRClient ocrClient) {
        this.ocrClient = ocrClient;
    }
    private final Map<DocumentType, Function<OCRText, ?>> documentParsers = new EnumMap<>(DocumentType.class);

    @Override
    public OCRResult apply(ImageUrlRequest request) {
        try {
            var result = runOcrWithRetry(request);
            triggerParserIfExists(result.documentType(), result.extractedText(), request);
            return result;
        } catch (Exception e) {
            log.error("OCR 처리 실패 - url: {}", request.imageUrl(), e);
            throw new IllegalStateException("OCR 처리 중 문제가 발생했습니다.");
        }
    }

    private OCRResult runOcrWithRetry(ImageUrlRequest request) {
        String text = "";
        DocumentType type = DocumentType.UNKNOWN;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            text = ocrClient.extractTextFromUrl(request.imageUrl());
            type = detectDocumentType(text);
            if (isAcceptable(type, text) || attempt == MAX_ATTEMPTS) {
                if (attempt > 1) {
                    log.debug("OCR 재시도 종료 - attempts: {}", attempt);
                }
                break;
            }
            log.debug("OCR 결과 품질 부족 - 재시도, attempt: {}", attempt);
        }
        return new OCRResult(request.imageUrl(), text, type);
    }

    private DocumentType detectDocumentType(String text) {
        var type = ocrClient.detectDocument(text);
        if (type == DocumentType.UNKNOWN) {
            log.debug("문서 유형을 판별하지 못했습니다.");
        }
        return type;
    }

    private boolean isAcceptable(DocumentType type, String text) {
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
