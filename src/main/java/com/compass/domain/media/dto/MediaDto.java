package com.compass.domain.media.dto;

import com.compass.domain.media.entity.FileStatus;
import com.compass.domain.media.entity.Media;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Map;

public class MediaDto {

    @Getter
    @Builder
    public static class UploadRequest {
        @NotNull(message = "File is required.")
        private MultipartFile file;
        private Map<String, Object> metadata;
    }

    @Getter
    @Builder
    public static class UploadResponse {
        private Long id;
        private String originalFilename;
        private String storedFilename;
        private String s3Url;
        private Long fileSize;
        private String mimeType;
        private FileStatus status;
        private Map<String, Object> metadata;
        private LocalDateTime createdAt;

        public static UploadResponse from(Media media) {
            return UploadResponse.builder()
                    .id(media.getId())
                    .originalFilename(media.getOriginalFilename())
                    .storedFilename(media.getStoredFilename())
                    .s3Url(media.getS3Url())
                    .fileSize(media.getFileSize())
                    .mimeType(media.getMimeType())
                    .status(media.getStatus())
                    .metadata(media.getMetadata())
                    .createdAt(media.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class GetResponse {
        private Long id;
        private String originalFilename;
        private String mimeType;
        private Long fileSize;
        private String presignedUrl;
        private FileStatus status;
        private Map<String, Object> metadata;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static GetResponse from(Media media, String presignedUrl) {
            return GetResponse.builder()
                    .id(media.getId())
                    .originalFilename(media.getOriginalFilename())
                    .mimeType(media.getMimeType())
                    .fileSize(media.getFileSize())
                    .presignedUrl(presignedUrl)
                    .status(media.getStatus())
                    .metadata(media.getMetadata())
                    .createdAt(media.getCreatedAt())
                    .updatedAt(media.getUpdatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class ListResponse {
        private Long id;
        private String originalFilename;
        private String mimeType;
        private Long fileSize;
        private FileStatus status;
        private LocalDateTime createdAt;

        public static ListResponse from(Media media) {
            return ListResponse.builder()
                    .id(media.getId())
                    .originalFilename(media.getOriginalFilename())
                    .mimeType(media.getMimeType())
                    .fileSize(media.getFileSize())
                    .status(media.getStatus())
                    .createdAt(media.getCreatedAt())
                    .build();
        }
    }
}