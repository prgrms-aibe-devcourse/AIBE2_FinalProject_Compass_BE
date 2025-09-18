package com.compass.domain.chat.function.processing;

import com.compass.domain.chat.model.request.ImageUploadRequest;
import com.compass.domain.chat.model.response.ImageProcessResult;
import com.compass.domain.chat.function.processing.event.ImageOcrQueuedEvent;
import com.compass.domain.chat.service.external.OCRClient;
import com.compass.domain.chat.service.external.S3Client;
import java.time.LocalDate;
import java.util.Set;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessImageFunction implements Function<ImageUploadRequest, ImageProcessResult> {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "application/pdf"
    );

    private final S3Client s3Client;
    private final OCRClient ocrClient;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public ImageProcessResult apply(ImageUploadRequest request) {
        try {
            validateFile(request);
            var directory = buildDirectory();
            var objectKey = s3Client.upload(request.data(), directory, request.fileName(), request.contentType());
            var imageUrl = s3Client.getUrl(objectKey);
            // OCR 수행 및 문서 유형 분류
            var extractedText = ocrClient.extractText(request.data());
            var documentType = ocrClient.detectDocument(extractedText);
            enqueueOcrTask(request, objectKey, imageUrl);
            return new ImageProcessResult(imageUrl, extractedText, documentType);
        } catch (Exception e) {
            log.error("이미지 처리 실패 - fileName: {}", request.fileName(), e);
            throw new IllegalStateException("이미지를 처리하지 못했습니다.");
        }
    }

    private void validateFile(ImageUploadRequest request) {
        if (request.data().length > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("지원 용량(10MB)을 초과했습니다.");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(request.contentType())) {
            throw new IllegalArgumentException("지원하지 않는 파일 형식입니다.");
        }
    }

    private String buildDirectory() {
        return "travel-images/" + LocalDate.now();
    }

    private void enqueueOcrTask(ImageUploadRequest request, String objectKey, String imageUrl) {
        var event = new ImageOcrQueuedEvent(
                objectKey,
                imageUrl,
                request.threadId(),
                request.userId(),
                request.contentType()
        );
        eventPublisher.publishEvent(event);
        log.debug("OCR 큐 대기 등록 - key: {}", objectKey);
    }
}
