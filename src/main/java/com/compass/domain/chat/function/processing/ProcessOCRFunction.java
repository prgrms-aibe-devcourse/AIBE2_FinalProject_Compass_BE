package com.compass.domain.chat.function.processing;

import com.compass.domain.chat.model.request.ImageUrlRequest;
import com.compass.domain.chat.model.response.OCRResult;
import com.compass.domain.chat.service.external.OCRClient;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessOCRFunction implements Function<ImageUrlRequest, OCRResult> {

    private final OCRClient ocrClient;

    @Override
    public OCRResult apply(ImageUrlRequest request) {
        try {
            // URL을 그대로 Vision API에 전달해 텍스트를 뽑아낸다
            var text = ocrClient.extractTextFromUrl(request.imageUrl());
            // 추출된 내용으로 예약 문서 유형을 가늠한다
            var documentType = ocrClient.detectDocument(text);
            return new OCRResult(request.imageUrl(), text, documentType);
        } catch (Exception e) {
            log.error("OCR 처리 실패 - url: {}", request.imageUrl(), e);
            throw new IllegalStateException("OCR 처리 중 문제가 발생했습니다.");
        }
    }
}
