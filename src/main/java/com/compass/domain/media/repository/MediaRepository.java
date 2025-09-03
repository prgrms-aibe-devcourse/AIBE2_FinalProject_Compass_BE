package com.compass.domain.media.repository;

import com.compass.domain.media.entity.Media;
import com.compass.domain.media.entity.FileStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * MediaRepository - 미디어 파일 데이터 접근 계층
 * REQ-MEDIA-003, REQ-MEDIA-004 요구사항에 따른 데이터 조회 기능
 */
@Repository
public interface MediaRepository extends JpaRepository<Media, Long> {
    
    /**
     * UUID로 미디어 파일 조회 (삭제되지 않은 파일만)
     */
    @Query("SELECT m FROM Media m WHERE m.fileUuid = :fileUuid AND m.deletedAt IS NULL")
    Optional<Media> findByFileUuidAndNotDeleted(@Param("fileUuid") UUID fileUuid);
    
    /**
     * UUID와 사용자 ID로 미디어 파일 조회 (삭제되지 않은 파일만)
     */
    @Query("SELECT m FROM Media m WHERE m.fileUuid = :fileUuid AND m.userId = :userId AND m.deletedAt IS NULL")
    Optional<Media> findByFileUuidAndUserId(@Param("fileUuid") UUID fileUuid, @Param("userId") Long userId);
    
    /**
     * 사용자 ID로 미디어 파일 목록 조회 (삭제되지 않은 파일만, 페이징)
     */
    @Query("SELECT m FROM Media m WHERE m.userId = :userId AND m.deletedAt IS NULL ORDER BY m.createdAt DESC")
    Page<Media> findByUserIdAndNotDeleted(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * 사용자 ID와 파일 상태로 미디어 파일 목록 조회
     */
    @Query("SELECT m FROM Media m WHERE m.userId = :userId AND m.fileStatus = :status AND m.deletedAt IS NULL ORDER BY m.createdAt DESC")
    List<Media> findByUserIdAndFileStatusAndNotDeleted(@Param("userId") Long userId, @Param("status") FileStatus status);
    
    /**
     * MIME 타입으로 미디어 파일 목록 조회 (이미지 파일만 필터링 등)
     */
    @Query("SELECT m FROM Media m WHERE m.userId = :userId AND m.mimeType LIKE :mimeTypePattern AND m.deletedAt IS NULL ORDER BY m.createdAt DESC")
    List<Media> findByUserIdAndMimeTypePattern(@Param("userId") Long userId, @Param("mimeTypePattern") String mimeTypePattern);
    
    /**
     * MIME 타입으로 미디어 파일 목록 조회 (삭제되지 않은 파일만)
     */
    @Query("SELECT m FROM Media m WHERE m.mimeType = :mimeType AND m.deletedAt IS NULL ORDER BY m.createdAt DESC")
    List<Media> findByMimeTypeAndDeletedAtIsNull(@Param("mimeType") String mimeType);
    
    /**
     * OCR 처리가 필요한 이미지 파일 조회
     */
    @Query("SELECT m FROM Media m WHERE m.mimeType LIKE 'image/%' AND m.ocrProcessed = false AND m.fileStatus = 'UPLOADED' AND m.deletedAt IS NULL")
    List<Media> findImagesNeedingOcrProcessing();
    
    /**
     * OCR 처리를 위한 이미지 파일 조회 (별칭 메서드)
     */
    @Query("SELECT m FROM Media m WHERE m.mimeType LIKE 'image/%' AND m.ocrProcessed = false AND m.fileStatus = 'UPLOADED' AND m.deletedAt IS NULL")
    List<Media> findImagesForOcrProcessing();
    
    /**
     * 특정 기간 이전에 생성된 삭제된 파일들 조회 (물리적 삭제를 위한 배치 작업용)
     */
    @Query("SELECT m FROM Media m WHERE m.deletedAt IS NOT NULL AND m.deletedAt < :beforeDate")
    List<Media> findDeletedFilesBefore(@Param("beforeDate") LocalDateTime beforeDate);
    
    /**
     * 사용자의 총 파일 크기 계산 (삭제되지 않은 파일만)
     */
    @Query("SELECT COALESCE(SUM(m.fileSize), 0) FROM Media m WHERE m.userId = :userId AND m.deletedAt IS NULL")
    Long calculateTotalFileSizeByUserId(@Param("userId") Long userId);
    
    /**
     * 사용자의 파일 개수 조회 (삭제되지 않은 파일만)
     */
    @Query("SELECT COUNT(m) FROM Media m WHERE m.userId = :userId AND m.deletedAt IS NULL")
    Long countByUserIdAndNotDeleted(@Param("userId") Long userId);
    
    /**
     * S3 키로 미디어 파일 조회
     */
    Optional<Media> findByS3Key(String s3Key);
    
    /**
     * 특정 상태의 파일들을 생성일 기준으로 조회
     */
    @Query("SELECT m FROM Media m WHERE m.fileStatus = :status AND m.createdAt BETWEEN :startDate AND :endDate ORDER BY m.createdAt DESC")
    List<Media> findByFileStatusAndCreatedAtBetween(
        @Param("status") FileStatus status, 
        @Param("startDate") LocalDateTime startDate, 
        @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * 업로드 실패한 파일들 중 재시도 가능한 파일들 조회
     */
    @Query("SELECT m FROM Media m WHERE m.fileStatus IN ('UPLOAD_FAILED', 'PROCESSING_FAILED') AND m.createdAt > :afterDate")
    List<Media> findFailedFilesForRetry(@Param("afterDate") LocalDateTime afterDate);
}