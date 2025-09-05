package com.compass.domain.media.entity;

import com.compass.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;

import java.util.Map;

@Entity
@Table(name = "media")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Media extends BaseEntity {
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Column(name = "original_filename", nullable = false, length = 500)
    private String originalFilename;
    
    @Column(name = "stored_filename", nullable = false, length = 500)
    private String storedFilename;
    
    @Column(name = "s3_url", length = 1000)
    private String s3Url;
    
    @Column(name = "file_size", nullable = false)
    private Long fileSize;
    
    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private FileStatus status;
    
    @Type(JsonBinaryType.class)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;
    
    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;
    
    @Builder
    public Media(String userId, String originalFilename, String storedFilename, 
                String s3Url, Long fileSize, String mimeType, FileStatus status, 
                Map<String, Object> metadata) {
        this.userId = userId;
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.s3Url = s3Url;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        this.status = status != null ? status : FileStatus.UPLOADED;
        this.metadata = metadata;
        this.deleted = false;
    }
    
    public void updateS3Url(String s3Url) {
        this.s3Url = s3Url;
    }
    
    public void updateStatus(FileStatus status) {
        this.status = status;
    }
    
    public void updateMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    public void markAsDeleted() {
        this.deleted = true;
        this.status = FileStatus.DELETED;
    }
}