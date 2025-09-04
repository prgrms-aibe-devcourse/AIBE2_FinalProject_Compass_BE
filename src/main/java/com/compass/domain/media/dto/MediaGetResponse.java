package com.compass.domain.media.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Builder
public class MediaGetResponse {
    
    private Long id;
    private String originalFilename;
    private String mimeType;
    private Long fileSize;
    private String presignedUrl;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static MediaGetResponse from(Long id, String originalFilename, String mimeType, 
                                       Long fileSize, String presignedUrl, 
                                       Map<String, Object> metadata, 
                                       LocalDateTime createdAt, LocalDateTime updatedAt) {
        return MediaGetResponse.builder()
                .id(id)
                .originalFilename(originalFilename)
                .mimeType(mimeType)
                .fileSize(fileSize)
                .presignedUrl(presignedUrl)
                .metadata(metadata)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }
}