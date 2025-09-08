package com.compass.domain.media.dto;

import com.compass.domain.media.entity.FileStatus;
import com.compass.domain.media.entity.Media;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Builder
public class MediaUploadResponse {
    private Long id;
    private String originalFilename;
    private String storedFilename;
    private String s3Url;
    private Long fileSize;
    private String mimeType;
    private FileStatus status;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;

    public static MediaUploadResponse from(Media media) {
        return MediaUploadResponse.builder()
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
