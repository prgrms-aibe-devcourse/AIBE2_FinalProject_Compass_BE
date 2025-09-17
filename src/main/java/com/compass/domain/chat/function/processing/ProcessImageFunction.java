package com.compass.domain.chat.function.processing;

import com.compass.domain.chat.model.request.ImageUploadRequest;
import com.compass.domain.chat.model.response.ImageProcessResult;
import com.compass.domain.chat.service.external.OCRClient;
import com.compass.domain.chat.service.external.S3Client;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessImageFunction implements Function<ImageUploadRequest, ImageProcessResult> {

    private final S3Client s3Client;
    private final OCRClient ocrClient;

    @Override
    public ImageProcessResult apply(ImageUploadRequest request) {
        try {
            var directory = resolveDirectory(request.threadId());
            // 이미지 저장 후 접근 가능한 URL 생성
            var objectKey = s3Client.upload(request.data(), directory, request.fileName(), request.contentType());
            var imageUrl = s3Client.getUrl(objectKey);
            // OCR 수행 및 문서 유형 분류
            var extractedText = ocrClient.extractText(request.data());
            var documentType = ocrClient.detectDocument(extractedText);
            return new ImageProcessResult(imageUrl, extractedText, documentType);
        } catch (Exception e) {
            log.error("이미지 처리 실패 - fileName: {}", request.fileName(), e);
            throw new IllegalStateException("이미지를 처리하지 못했습니다.");
        }
    }

    private String resolveDirectory(String threadId) {
        return threadId == null || threadId.isBlank() ? "uploads" : "threads/" + threadId;
    }
}
