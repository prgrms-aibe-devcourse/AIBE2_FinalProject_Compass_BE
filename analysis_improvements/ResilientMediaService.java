package com.compass.domain.media.service;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Resilient media service with retry logic and circuit breaker pattern
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResilientMediaService {
    
    private final OCRService ocrService;
    private final S3Service s3Service;
    private final ThumbnailService thumbnailService;
    
    private final Retry ocrRetry;
    private final Retry s3Retry;
    private final CircuitBreaker s3CircuitBreaker;
    
    public ResilientMediaService(OCRService ocrService, S3Service s3Service, ThumbnailService thumbnailService) {
        this.ocrService = ocrService;
        this.s3Service = s3Service;
        this.thumbnailService = thumbnailService;
        
        // Configure retry for OCR operations
        this.ocrRetry = Retry.of("ocr-retry", RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofSeconds(2))
                .retryOnException(throwable -> 
                    throwable instanceof java.io.IOException ||
                    throwable.getMessage().contains("quota") ||
                    throwable.getMessage().contains("timeout"))
                .build());
        
        // Configure retry for S3 operations
        this.s3Retry = Retry.of("s3-retry", RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofSeconds(1))
                .retryOnException(throwable -> 
                    throwable.getMessage().contains("timeout") ||
                    throwable.getMessage().contains("connection") ||
                    throwable.getMessage().contains("throttle"))
                .build());
        
        // Configure circuit breaker for S3
        this.s3CircuitBreaker = CircuitBreaker.of("s3-circuit-breaker", CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMinutes(1))
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .build());
    }
    
    /**
     * Performs OCR with retry logic and graceful degradation
     */
    public OCRResult performResilientOCR(byte[] imageBytes, String filename) {
        Supplier<Map<String, Object>> ocrOperation = () -> {
            try {
                return ocrService.extractTextFromBytes(imageBytes, filename);
            } catch (Exception e) {
                log.warn("OCR 시도 실패 - 파일: {}, 에러: {}", filename, e.getMessage());
                throw new RuntimeException("OCR processing failed", e);
            }
        };
        
        try {
            Map<String, Object> result = ocrRetry.executeSupplier(ocrOperation);
            return OCRResult.success(result);
            
        } catch (Exception e) {
            log.error("OCR 최종 실패 - 파일: {} (모든 재시도 실패)", filename, e);
            return OCRResult.failure("OCR processing failed after retries: " + e.getMessage());
        }
    }
    
    /**
     * Uploads file to S3 with circuit breaker and retry
     */
    public S3UploadResult uploadWithResilience(byte[] fileData, String userId, String filename) {
        Supplier<String> s3Operation = () -> {
            try {
                return s3Service.uploadFile(createMultipartFile(fileData, filename), userId, filename);
            } catch (Exception e) {
                log.warn("S3 업로드 시도 실패 - 파일: {}, 에러: {}", filename, e.getMessage());
                throw new RuntimeException("S3 upload failed", e);
            }
        };
        
        try {
            // Apply circuit breaker and retry
            Supplier<String> resilientOperation = s3CircuitBreaker.decorateSupplier(s3Retry.decorate(s3Operation));
            String s3Url = resilientOperation.get();
            return S3UploadResult.success(s3Url);
            
        } catch (Exception e) {
            log.error("S3 업로드 최종 실패 - 파일: {}", filename, e);
            return S3UploadResult.failure("S3 upload failed: " + e.getMessage());
        }
    }
    
    /**
     * Generates thumbnail with fallback strategies
     */
    public ThumbnailResult generateResilientThumbnail(byte[] imageData, String filename) {
        try {
            // Primary strategy: WebP thumbnail
            byte[] thumbnail = thumbnailService.generateThumbnailFromBytes(imageData, filename);
            return ThumbnailResult.success(thumbnail, "webp");
            
        } catch (Exception e) {
            log.warn("WebP 썸네일 생성 실패, JPEG 대안 시도 - 파일: {}", filename, e);
            
            try {
                // Fallback strategy: JPEG thumbnail
                byte[] jpegThumbnail = generateJpegThumbnail(imageData, filename);
                return ThumbnailResult.success(jpegThumbnail, "jpeg");
                
            } catch (Exception fallbackError) {
                log.error("모든 썸네일 생성 전략 실패 - 파일: {}", filename, fallbackError);
                return ThumbnailResult.failure("Thumbnail generation failed: " + fallbackError.getMessage());
            }
        }
    }
    
    private byte[] generateJpegThumbnail(byte[] imageData, String filename) {
        // Simplified JPEG thumbnail generation as fallback
        // Implementation would use Java's built-in image processing
        throw new UnsupportedOperationException("JPEG fallback not implemented yet");
    }
    
    private org.springframework.web.multipart.MultipartFile createMultipartFile(byte[] data, String filename) {
        // Helper method to create MultipartFile from byte array
        // Implementation would create a mock MultipartFile
        throw new UnsupportedOperationException("MultipartFile creation not implemented yet");
    }
    
    // Result classes for type-safe error handling
    public static class OCRResult {
        private final boolean success;
        private final Map<String, Object> data;
        private final String errorMessage;
        
        private OCRResult(boolean success, Map<String, Object> data, String errorMessage) {
            this.success = success;
            this.data = data;
            this.errorMessage = errorMessage;
        }
        
        public static OCRResult success(Map<String, Object> data) {
            return new OCRResult(true, data, null);
        }
        
        public static OCRResult failure(String errorMessage) {
            return new OCRResult(false, null, errorMessage);
        }
        
        public boolean isSuccess() { return success; }
        public Map<String, Object> getData() { return data; }
        public String getErrorMessage() { return errorMessage; }
    }
    
    public static class S3UploadResult {
        private final boolean success;
        private final String s3Url;
        private final String errorMessage;
        
        private S3UploadResult(boolean success, String s3Url, String errorMessage) {
            this.success = success;
            this.s3Url = s3Url;
            this.errorMessage = errorMessage;
        }
        
        public static S3UploadResult success(String s3Url) {
            return new S3UploadResult(true, s3Url, null);
        }
        
        public static S3UploadResult failure(String errorMessage) {
            return new S3UploadResult(false, null, errorMessage);
        }
        
        public boolean isSuccess() { return success; }
        public String getS3Url() { return s3Url; }
        public String getErrorMessage() { return errorMessage; }
    }
    
    public static class ThumbnailResult {
        private final boolean success;
        private final byte[] thumbnailData;
        private final String format;
        private final String errorMessage;
        
        private ThumbnailResult(boolean success, byte[] thumbnailData, String format, String errorMessage) {
            this.success = success;
            this.thumbnailData = thumbnailData;
            this.format = format;
            this.errorMessage = errorMessage;
        }
        
        public static ThumbnailResult success(byte[] thumbnailData, String format) {
            return new ThumbnailResult(true, thumbnailData, format, null);
        }
        
        public static ThumbnailResult failure(String errorMessage) {
            return new ThumbnailResult(false, null, null, errorMessage);
        }
        
        public boolean isSuccess() { return success; }
        public byte[] getThumbnailData() { return thumbnailData; }
        public String getFormat() { return format; }
        public String getErrorMessage() { return errorMessage; }
    }
}