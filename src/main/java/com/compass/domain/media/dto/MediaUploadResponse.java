package com.compass.domain.media.dto;

import com.compass.domain.media.entity.FileStatus;
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
    
    public static MediaUploadResponse from(Long id, String originalFilename, String storedFilename, 
                                         String s3Url, Long fileSize, String mimeType, 
                                         FileStatus status, Map<String, Object> metadata, 
                                         LocalDateTime createdAt) {
        return MediaUploadResponse.builder()
                .id(id)
                .originalFilename(originalFilename)
                .storedFilename(storedFilename)
                .s3Url(s3Url)
                .fileSize(fileSize)
                .mimeType(mimeType)
                .status(status)
                .metadata(metadata)
                .createdAt(createdAt)
                .build();
    }
}