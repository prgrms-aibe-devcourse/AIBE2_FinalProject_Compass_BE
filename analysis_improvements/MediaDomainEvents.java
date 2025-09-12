package com.compass.domain.media.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Domain events for media processing operations
 */
public class MediaDomainEvents {
    
    @Getter
    @RequiredArgsConstructor
    public static class FileUploadedEvent extends ApplicationEvent {
        private final Long mediaId;
        private final Long userId;
        private final String filename;
        private final String mimeType;
        private final Long fileSize;
        private final String s3Url;
        private final LocalDateTime uploadedAt;
        
        public FileUploadedEvent(Object source, Long mediaId, Long userId, String filename, 
                               String mimeType, Long fileSize, String s3Url) {
            super(source);
            this.mediaId = mediaId;
            this.userId = userId;
            this.filename = filename;
            this.mimeType = mimeType;
            this.fileSize = fileSize;
            this.s3Url = s3Url;
            this.uploadedAt = LocalDateTime.now();
        }
    }
    
    @Getter
    @RequiredArgsConstructor
    public static class OCRProcessingStartedEvent extends ApplicationEvent {
        private final Long mediaId;
        private final Long userId;
        private final String filename;
        private final LocalDateTime startedAt;
        
        public OCRProcessingStartedEvent(Object source, Long mediaId, Long userId, String filename) {
            super(source);
            this.mediaId = mediaId;
            this.userId = userId;
            this.filename = filename;
            this.startedAt = LocalDateTime.now();
        }
    }
    
    @Getter
    @RequiredArgsConstructor
    public static class OCRProcessingCompletedEvent extends ApplicationEvent {
        private final Long mediaId;
        private final Long userId;
        private final String filename;
        private final boolean success;
        private final String extractedText;
        private final String errorMessage;
        private final LocalDateTime completedAt;
        
        public OCRProcessingCompletedEvent(Object source, Long mediaId, Long userId, String filename,
                                         boolean success, String extractedText, String errorMessage) {
            super(source);
            this.mediaId = mediaId;
            this.userId = userId;
            this.filename = filename;
            this.success = success;
            this.extractedText = extractedText;
            this.errorMessage = errorMessage;
            this.completedAt = LocalDateTime.now();
        }
    }
    
    @Getter
    @RequiredArgsConstructor
    public static class ThumbnailProcessingStartedEvent extends ApplicationEvent {
        private final Long mediaId;
        private final Long userId;
        private final String filename;
        private final LocalDateTime startedAt;
        
        public ThumbnailProcessingStartedEvent(Object source, Long mediaId, Long userId, String filename) {
            super(source);
            this.mediaId = mediaId;
            this.userId = userId;
            this.filename = filename;
            this.startedAt = LocalDateTime.now();
        }
    }
    
    @Getter
    @RequiredArgsConstructor
    public static class ThumbnailProcessingCompletedEvent extends ApplicationEvent {
        private final Long mediaId;
        private final Long userId;
        private final String filename;
        private final boolean success;
        private final String thumbnailUrl;
        private final String errorMessage;
        private final LocalDateTime completedAt;
        
        public ThumbnailProcessingCompletedEvent(Object source, Long mediaId, Long userId, String filename,
                                               boolean success, String thumbnailUrl, String errorMessage) {
            super(source);
            this.mediaId = mediaId;
            this.userId = userId;
            this.filename = filename;
            this.success = success;
            this.thumbnailUrl = thumbnailUrl;
            this.errorMessage = errorMessage;
            this.completedAt = LocalDateTime.now();
        }
    }
    
    @Getter
    @RequiredArgsConstructor
    public static class FileDeletedEvent extends ApplicationEvent {
        private final Long mediaId;
        private final Long userId;
        private final String filename;
        private final String s3Url;
        private final LocalDateTime deletedAt;
        
        public FileDeletedEvent(Object source, Long mediaId, Long userId, String filename, String s3Url) {
            super(source);
            this.mediaId = mediaId;
            this.userId = userId;
            this.filename = filename;
            this.s3Url = s3Url;
            this.deletedAt = LocalDateTime.now();
        }
    }
    
    @Getter
    @RequiredArgsConstructor
    public static class SecurityThreatDetectedEvent extends ApplicationEvent {
        private final Long userId;
        private final String filename;
        private final String threatType;
        private final String description;
        private final String clientIp;
        private final LocalDateTime detectedAt;
        
        public SecurityThreatDetectedEvent(Object source, Long userId, String filename, 
                                         String threatType, String description, String clientIp) {
            super(source);
            this.userId = userId;
            this.filename = filename;
            this.threatType = threatType;
            this.description = description;
            this.clientIp = clientIp;
            this.detectedAt = LocalDateTime.now();
        }
    }
    
    @Getter
    @RequiredArgsConstructor
    public static class MediaProcessingFailedEvent extends ApplicationEvent {
        private final Long mediaId;
        private final Long userId;
        private final String filename;
        private final String stage; // "upload", "ocr", "thumbnail"
        private final String errorMessage;
        private final Exception exception;
        private final LocalDateTime failedAt;
        
        public MediaProcessingFailedEvent(Object source, Long mediaId, Long userId, String filename,
                                        String stage, String errorMessage, Exception exception) {
            super(source);
            this.mediaId = mediaId;
            this.userId = userId;
            this.filename = filename;
            this.stage = stage;
            this.errorMessage = errorMessage;
            this.exception = exception;
            this.failedAt = LocalDateTime.now();
        }
    }
    
    @Getter
    @RequiredArgsConstructor
    public static class UserQuotaExceededEvent extends ApplicationEvent {
        private final Long userId;
        private final String quotaType; // "file_count", "storage_size", "monthly_uploads"
        private final Long currentUsage;
        private final Long quotaLimit;
        private final LocalDateTime exceededAt;
        
        public UserQuotaExceededEvent(Object source, Long userId, String quotaType, 
                                    Long currentUsage, Long quotaLimit) {
            super(source);
            this.userId = userId;
            this.quotaType = quotaType;
            this.currentUsage = currentUsage;
            this.quotaLimit = quotaLimit;
            this.exceededAt = LocalDateTime.now();
        }
    }
    
    @Getter
    @RequiredArgsConstructor
    public static class MetadataUpdatedEvent extends ApplicationEvent {
        private final Long mediaId;
        private final Long userId;
        private final String updateType; // "ocr", "thumbnail", "manual"
        private final Map<String, Object> oldMetadata;
        private final Map<String, Object> newMetadata;
        private final LocalDateTime updatedAt;
        
        public MetadataUpdatedEvent(Object source, Long mediaId, Long userId, String updateType,
                                  Map<String, Object> oldMetadata, Map<String, Object> newMetadata) {
            super(source);
            this.mediaId = mediaId;
            this.userId = userId;
            this.updateType = updateType;
            this.oldMetadata = oldMetadata;
            this.newMetadata = newMetadata;
            this.updatedAt = LocalDateTime.now();
        }
    }
}