package com.compass.domain.media.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Asynchronous media processing service for performance optimization
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncMediaService {
    
    private final OCRService ocrService;
    private final MediaRepository mediaRepository;
    private final ThumbnailService thumbnailService;
    private final S3Service s3Service;
    
    /**
     * Processes OCR asynchronously without blocking the main request thread.
     * Updates media metadata when processing completes.
     * 
     * @param mediaId the media ID to process
     * @param imageBytes the image byte array
     * @param filename the original filename
     * @return CompletableFuture containing OCR results
     */
    @Async("mediaTaskExecutor")
    public CompletableFuture<Map<String, Object>> processOCRAsync(Long mediaId, byte[] imageBytes, String filename) {
        return CompletableFuture
                .supplyAsync(() -> {
                    try {
                        log.info("시작: 비동기 OCR 처리 - 미디어 ID: {}", mediaId);
                        return ocrService.extractTextFromBytes(imageBytes, filename);
                    } catch (Exception e) {
                        log.error("OCR 처리 실패 - 미디어 ID: {}", mediaId, e);
                        return Map.of(
                            "success", false,
                            "error", "Async OCR processing failed: " + e.getMessage(),
                            "processedAt", java.time.LocalDateTime.now().toString()
                        );
                    }
                })
                .thenApply(ocrResult -> {
                    // Update media metadata asynchronously
                    updateMediaMetadataAsync(mediaId, ocrResult);
                    return ocrResult;
                })
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("비동기 OCR 처리 중 오류 - 미디어 ID: {}", mediaId, throwable);
                    } else {
                        log.info("완료: 비동기 OCR 처리 - 미디어 ID: {}, 성공: {}", 
                                mediaId, result.get("success"));
                    }
                });
    }
    
    /**
     * Processes thumbnail generation asynchronously.
     * 
     * @param mediaId the media ID
     * @param imageBytes the image byte array
     * @param originalFilename the original filename
     * @param userId the user ID
     * @return CompletableFuture containing thumbnail results
     */
    @Async("mediaTaskExecutor")
    public CompletableFuture<Map<String, Object>> processThumbnailAsync(Long mediaId, byte[] imageBytes, 
                                                                       String originalFilename, String userId) {
        return CompletableFuture
                .supplyAsync(() -> {
                    try {
                        log.info("시작: 비동기 썸네일 처리 - 미디어 ID: {}", mediaId);
                        
                        // Generate thumbnail
                        byte[] thumbnailData = thumbnailService.generateThumbnailFromBytes(imageBytes, originalFilename);
                        String thumbnailFilename = thumbnailService.generateThumbnailFilename(originalFilename);
                        
                        // Upload to S3
                        String thumbnailS3Url = s3Service.uploadThumbnail(thumbnailData, userId, thumbnailFilename);
                        
                        return thumbnailService.createThumbnailMetadata(thumbnailS3Url, thumbnailFilename);
                        
                    } catch (Exception e) {
                        log.error("썸네일 처리 실패 - 미디어 ID: {}", mediaId, e);
                        return Map.of(
                            "success", false,
                            "error", "Async thumbnail processing failed: " + e.getMessage(),
                            "createdAt", java.time.LocalDateTime.now().toString()
                        );
                    }
                })
                .thenApply(thumbnailResult -> {
                    // Update media metadata with thumbnail info
                    updateMediaThumbnailAsync(mediaId, thumbnailResult);
                    return thumbnailResult;
                })
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("비동기 썸네일 처리 중 오류 - 미디어 ID: {}", mediaId, throwable);
                    } else {
                        log.info("완료: 비동기 썸네일 처리 - 미디어 ID: {}", mediaId);
                    }
                });
    }
    
    @Async("mediaTaskExecutor")
    @Transactional
    public void updateMediaMetadataAsync(Long mediaId, Map<String, Object> ocrResult) {
        try {
            Media media = mediaRepository.findById(mediaId)
                    .orElseThrow(() -> new RuntimeException("Media not found: " + mediaId));
            
            Map<String, Object> metadata = media.getMetadata() != null ? 
                    new java.util.HashMap<>(media.getMetadata()) : new java.util.HashMap<>();
            metadata.put("ocr", ocrResult);
            
            media.updateMetadata(metadata);
            mediaRepository.save(media);
            
            log.debug("OCR 메타데이터 업데이트 완료 - 미디어 ID: {}", mediaId);
            
        } catch (Exception e) {
            log.error("OCR 메타데이터 업데이트 실패 - 미디어 ID: {}", mediaId, e);
        }
    }
    
    @Async("mediaTaskExecutor")
    @Transactional
    public void updateMediaThumbnailAsync(Long mediaId, Map<String, Object> thumbnailResult) {
        try {
            Media media = mediaRepository.findById(mediaId)
                    .orElseThrow(() -> new RuntimeException("Media not found: " + mediaId));
            
            Map<String, Object> metadata = media.getMetadata() != null ? 
                    new java.util.HashMap<>(media.getMetadata()) : new java.util.HashMap<>();
            metadata.put("thumbnail", thumbnailResult);
            
            media.updateMetadata(metadata);
            mediaRepository.save(media);
            
            log.debug("썸네일 메타데이터 업데이트 완료 - 미디어 ID: {}", mediaId);
            
        } catch (Exception e) {
            log.error("썸네일 메타데이터 업데이트 실패 - 미디어 ID: {}", mediaId, e);
        }
    }
}