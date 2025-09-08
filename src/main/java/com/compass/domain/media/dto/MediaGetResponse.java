package com.compass.domain.media.dto;

import com.compass.domain.media.entity.FileStatus;
import com.compass.domain.media.entity.Media;
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
    private FileStatus status;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MediaGetResponse from(Media media, String presignedUrl) {
        return MediaGetResponse.builder()
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
