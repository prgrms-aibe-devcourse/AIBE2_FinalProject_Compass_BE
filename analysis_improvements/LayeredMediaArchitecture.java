package com.compass.domain.media.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Orchestration layer that coordinates media operations without handling business logic directly
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MediaOrchestrationService {
    
    private final FileUploadService fileUploadService;
    private final MediaProcessingService mediaProcessingService;
    private final MediaQueryService mediaQueryService;
    private final ApplicationEventPublisher eventPublisher;
    
    /**
     * Orchestrates complete file upload with processing
     */
    @Transactional
    public MediaUploadResponse uploadFileWithProcessing(MultipartFile file, Long userId, Map<String, Object> metadata) {
        log.info("파일 업로드 및 처리 시작 - 사용자: {}, 파일: {}", userId, file.getOriginalFilename());
        
        // Step 1: Upload file to storage
        MediaUploadResponse uploadResponse = fileUploadService.uploadFile(file, userId, metadata);
        
        // Step 2: Trigger async processing
        mediaProcessingService.processMediaAsync(uploadResponse.getId(), file, userId);
        
        log.info("파일 업로드 완료, 처리 시작됨 - ID: {}", uploadResponse.getId());
        return uploadResponse;
    }
    
    /**
     * Retrieves media with all processing results
     */
    public MediaGetResponse getMediaWithResults(Long mediaId, Long userId) {
        return mediaQueryService.getMediaById(mediaId, userId);
    }
    
    /**
     * Deletes media and all associated resources
     */
    @Transactional
    public void deleteMediaCompletely(Long mediaId, Long userId) {
        mediaQueryService.validateUserAccess(mediaId, userId);
        fileUploadService.deleteFile(mediaId, userId);
        mediaProcessingService.cleanupProcessingResults(mediaId);
    }
}

/**
 * Core file upload and storage operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadService {
    
    private final FileValidationService fileValidationService;
    private final S3StorageService s3StorageService;
    private final MediaRepository mediaRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    
    @Transactional
    public MediaUploadResponse uploadFile(MultipartFile file, Long userId, Map<String, Object> metadata) {
        // Validation
        fileValidationService.validateFile(file);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new FileValidationException("사용자를 찾을 수 없습니다."));
        
        // Storage
        String storedFilename = generateStoredFilename(file.getOriginalFilename());
        String s3Url = s3StorageService.uploadFile(file, userId.toString(), storedFilename);
        
        // Persistence
        Media media = Media.builder()
                .user(user)
                .originalFilename(file.getOriginalFilename())
                .storedFilename(storedFilename)
                .s3Url(s3Url)
                .fileSize(file.getSize())
                .mimeType(file.getContentType())
                .status(FileStatus.UPLOADED)
                .metadata(createInitialMetadata(file, metadata))
                .build();
        
        Media savedMedia = mediaRepository.save(media);
        
        // Event publishing
        eventPublisher.publishEvent(new MediaDomainEvents.FileUploadedEvent(
                this, savedMedia.getId(), userId, file.getOriginalFilename(),
                file.getContentType(), file.getSize(), s3Url));
        
        return MediaUploadResponse.from(savedMedia);
    }
    
    @Transactional
    public void deleteFile(Long mediaId, Long userId) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new FileValidationException("파일을 찾을 수 없습니다."));
        
        validateOwnership(media, userId);
        
        // Delete from S3
        if (media.getS3Url() != null) {
            s3StorageService.deleteFile(media.getS3Url());
        }
        
        // Mark as deleted
        media.markAsDeleted();
        mediaRepository.save(media);
        
        // Publish event
        eventPublisher.publishEvent(new MediaDomainEvents.FileDeletedEvent(
                this, mediaId, userId, media.getOriginalFilename(), media.getS3Url()));
    }
    
    private Map<String, Object> createInitialMetadata(MultipartFile file, Map<String, Object> requestMetadata) {
        Map<String, Object> metadata = new java.util.HashMap<>();
        metadata.put("uploadedAt", java.time.LocalDateTime.now().toString());
        metadata.put("originalSize", file.getSize());
        metadata.put("contentType", file.getContentType());
        
        if (fileValidationService.isSupportedImageFile(file.getContentType())) {
            metadata.put("isImage", true);
            metadata.put("processingStatus", "pending");
        }
        
        if (requestMetadata != null) {
            metadata.putAll(requestMetadata);
        }
        
        return metadata;
    }
    
    private void validateOwnership(Media media, Long userId) {
        if (!media.getUser().getId().equals(userId)) {
            throw new FileValidationException("파일 접근 권한이 없습니다.");
        }
    }
    
    private String generateStoredFilename(String originalFilename) {
        // Implementation for generating unique stored filename
        String extension = FileValidationService.extractFileExtension(originalFilename);
        String timestamp = java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = java.util.UUID.randomUUID().toString().substring(0, 8);
        return String.format("%s_%s%s", timestamp, uuid, extension);
    }
}

/**
 * Handles all media processing operations (OCR, thumbnails, etc.)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MediaProcessingService {
    
    private final OCRProcessingService ocrProcessingService;
    private final ThumbnailProcessingService thumbnailProcessingService;
    private final MediaMetadataService metadataService;
    private final ApplicationEventPublisher eventPublisher;
    
    /**
     * Coordinates async processing of uploaded media
     */
    public void processMediaAsync(Long mediaId, MultipartFile file, Long userId) {
        if (!isImageFile(file.getContentType())) {
            log.debug("비이미지 파일은 처리하지 않음 - ID: {}", mediaId);
            return;
        }
        
        // Start OCR processing
        CompletableFuture<Void> ocrFuture = ocrProcessingService.processOCRAsync(mediaId, file, userId);
        
        // Start thumbnail processing
        CompletableFuture<Void> thumbnailFuture = thumbnailProcessingService.processThumbnailAsync(mediaId, file, userId);
        
        // Combine futures and handle completion
        CompletableFuture.allOf(ocrFuture, thumbnailFuture)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("미디어 처리 중 오류 - ID: {}", mediaId, throwable);
                        eventPublisher.publishEvent(new MediaDomainEvents.MediaProcessingFailedEvent(
                                this, mediaId, userId, file.getOriginalFilename(),
                                "processing", throwable.getMessage(), (Exception) throwable));
                    } else {
                        log.info("미디어 처리 완료 - ID: {}", mediaId);
                        metadataService.updateProcessingStatus(mediaId, "completed");
                    }
                });
    }
    
    public void cleanupProcessingResults(Long mediaId) {
        // Clean up any processing artifacts
        ocrProcessingService.cleanupOCRResults(mediaId);
        thumbnailProcessingService.cleanupThumbnails(mediaId);
    }
    
    private boolean isImageFile(String contentType) {
        return contentType != null && contentType.startsWith("image/");
    }
}

/**
 * Handles media queries and retrieval operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MediaQueryService {
    
    private final MediaRepository mediaRepository;
    private final S3StorageService s3StorageService;
    
    @Transactional(readOnly = true)
    public MediaGetResponse getMediaById(Long mediaId, Long userId) {
        Media media = findAndValidateMedia(mediaId, userId);
        
        // Generate presigned URL
        String presignedUrl = s3StorageService.generatePresignedUrl(media.getS3Url(), 15);
        
        return MediaGetResponse.from(media, presignedUrl);
    }
    
    @Transactional(readOnly = true)
    public java.util.List<MediaDto.ListResponse> getMediaListByUser(Long userId) {
        java.util.List<Media> mediaList = mediaRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId);
        
        return mediaList.stream()
                .map(MediaDto.ListResponse::from)
                .collect(java.util.stream.Collectors.toList());
    }
    
    public void validateUserAccess(Long mediaId, Long userId) {
        findAndValidateMedia(mediaId, userId);
    }
    
    private Media findAndValidateMedia(Long mediaId, Long userId) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new FileValidationException("파일을 찾을 수 없습니다."));
        
        if (!media.getUser().getId().equals(userId)) {
            throw new FileValidationException("파일 조회 권한이 없습니다.");
        }
        
        if (media.getDeleted() || media.getStatus() == FileStatus.DELETED) {
            throw new FileValidationException("삭제된 파일입니다.");
        }
        
        return media;
    }
}

/**
 * Dedicated service for media metadata operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MediaMetadataService {
    
    private final MediaRepository mediaRepository;
    private final ApplicationEventPublisher eventPublisher;
    
    @Transactional
    public void updateMetadata(Long mediaId, String updateType, Map<String, Object> newData) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new RuntimeException("Media not found: " + mediaId));
        
        Map<String, Object> oldMetadata = media.getMetadata() != null ? 
                new java.util.HashMap<>(media.getMetadata()) : new java.util.HashMap<>();
        
        Map<String, Object> updatedMetadata = new java.util.HashMap<>(oldMetadata);
        updatedMetadata.putAll(newData);
        
        media.updateMetadata(updatedMetadata);
        mediaRepository.save(media);
        
        // Publish metadata update event
        eventPublisher.publishEvent(new MediaDomainEvents.MetadataUpdatedEvent(
                this, mediaId, media.getUser().getId(), updateType, oldMetadata, updatedMetadata));
    }
    
    @Transactional
    public void updateProcessingStatus(Long mediaId, String status) {
        updateMetadata(mediaId, "status_update", Map.of("processingStatus", status, "updatedAt", 
                java.time.LocalDateTime.now().toString()));
    }
    
    @Transactional(readOnly = true)
    public Map<String, Object> getMetadata(Long mediaId, String metadataType) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new RuntimeException("Media not found: " + mediaId));
        
        if (media.getMetadata() == null) {
            return new java.util.HashMap<>();
        }
        
        return (Map<String, Object>) media.getMetadata().getOrDefault(metadataType, new java.util.HashMap<>());
    }
}