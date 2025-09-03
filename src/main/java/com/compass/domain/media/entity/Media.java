package com.compass.domain.media.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Media Entity - 미디어 파일 정보를 저장하는 엔티티
 * REQ-MEDIA-001~005 요구사항에 따른 파일 메타데이터 관리
 */
@Entity
@Table(name = "media_files")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Media {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "file_uuid", unique = true, nullable = false)
    private UUID fileUuid;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "original_filename", nullable = false)
    private String originalFilename;
    
    @Column(name = "stored_filename", nullable = false)
    private String storedFilename;
    
    @Column(name = "file_path", nullable = false)
    private String filePath;
    
    @Column(name = "file_size", nullable = false)
    private Long fileSize;
    
    @Column(name = "mime_type", nullable = false)
    private String mimeType;
    
    @Column(name = "s3_bucket")
    private String s3Bucket;
    
    @Column(name = "s3_key")
    private String s3Key;
    
    @Column(name = "s3_url")
    private String s3Url;
    
    @Column(name = "file_status")
    @Enumerated(EnumType.STRING)
    private FileStatus fileStatus;
    
    @Column(name = "ocr_text", columnDefinition = "TEXT")
    private String ocrText;
    
    @Column(name = "ocr_processed")
    @Default
    private Boolean ocrProcessed = false;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    

    
    // 생성자
    public Media(Long userId, String originalFilename, String storedFilename, 
                String filePath, Long fileSize, String mimeType) {
        this();
        this.userId = userId;
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public UUID getFileUuid() {
        return fileUuid;
    }
    
    public void setFileUuid(UUID fileUuid) {
        this.fileUuid = fileUuid;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getOriginalFilename() {
        return originalFilename;
    }
    
    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }
    
    public String getStoredFilename() {
        return storedFilename;
    }
    
    public void setStoredFilename(String storedFilename) {
        this.storedFilename = storedFilename;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public Long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
    
    public String getMimeType() {
        return mimeType;
    }
    
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }
    
    public String getS3Bucket() {
        return s3Bucket;
    }
    
    public void setS3Bucket(String s3Bucket) {
        this.s3Bucket = s3Bucket;
    }
    
    public String getS3Key() {
        return s3Key;
    }
    
    public void setS3Key(String s3Key) {
        this.s3Key = s3Key;
    }
    
    public String getS3Url() {
        return s3Url;
    }
    
    public void setS3Url(String s3Url) {
        this.s3Url = s3Url;
    }
    
    public FileStatus getFileStatus() {
        return fileStatus;
    }
    
    public void setFileStatus(FileStatus fileStatus) {
        this.fileStatus = fileStatus;
    }
    
    public String getOcrText() {
        return ocrText;
    }
    
    public void setOcrText(String ocrText) {
        this.ocrText = ocrText;
    }
    
    public Boolean getOcrProcessed() {
        return ocrProcessed;
    }
    
    public void setOcrProcessed(Boolean ocrProcessed) {
        this.ocrProcessed = ocrProcessed;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }
    
    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
    
    // 비즈니스 메서드
    public void markAsUploaded(String s3Bucket, String s3Key, String s3Url) {
        this.s3Bucket = s3Bucket;
        this.s3Key = s3Key;
        this.s3Url = s3Url;
        this.fileStatus = FileStatus.UPLOADED;
    }
    
    public void markAsProcessed() {
        this.fileStatus = FileStatus.PROCESSED;
    }
    
    public void markAsDeleted() {
        this.fileStatus = FileStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
    }
    
    public void updateOcrText(String ocrText) {
        this.ocrText = ocrText;
        this.ocrProcessed = true;
    }
    
    public boolean isImage() {
        return mimeType != null && mimeType.startsWith("image/");
    }
    
    public boolean isDeleted() {
        return FileStatus.DELETED.equals(fileStatus) || deletedAt != null;
    }
}