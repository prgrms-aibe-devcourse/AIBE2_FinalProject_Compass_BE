package com.compass.domain.media.service;

import com.compass.domain.media.entity.FileStatus;
import com.compass.domain.media.entity.Media;
import com.compass.domain.media.repository.MediaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * MEDIA 도메인의 비즈니스 로직을 담당하는 서비스
 * 파일 업로드, 조회, 삭제 등의 핵심 기능과 트랜잭션 관리를 수행
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MediaService {

    private final MediaRepository mediaRepository;
    private final S3Service s3Service;
    private final FileValidationService fileValidationService;

    /**
     * 파일 업로드 처리
     * 1. 파일 유효성 검증
     * 2. S3 업로드
     * 3. 메타데이터 저장
     * 
     * @param file 업로드할 파일
     * @param userId 사용자 ID
     * @return 저장된 Media 엔티티
     */
    @Transactional
    public Media uploadFile(MultipartFile file, Long userId) {
        log.info("파일 업로드 시작 - 사용자: {}, 파일명: {}", userId, file.getOriginalFilename());
        
        try {
            // 1. 파일 유효성 검증
            fileValidationService.validateFile(file);
            
            // 2. UUID 생성
            UUID fileUuid = UUID.randomUUID();
            
            // 3. S3 업로드 (uploadFile 메서드가 내부적으로 s3Key를 생성함)
            String s3Key = s3Service.uploadFile(file, userId);
            
            // 4. Media 엔티티 생성 및 저장
            Media media = Media.builder()
                    .fileUuid(fileUuid)
                    .userId(userId)
                    .originalFilename(file.getOriginalFilename())
                    .storedFilename(generateStoredFilename(file.getOriginalFilename()))
                    .filePath(s3Key)
                    .fileSize(file.getSize())
                    .mimeType(file.getContentType())
                    .s3Bucket(s3Service.getBucketName())
                    .s3Key(s3Key)
                    .s3Url(s3Service.generatePublicUrl(s3Key))
                    .fileStatus(FileStatus.UPLOADED)
                    .ocrProcessed(false)
                    .build();
            
            Media savedMedia = mediaRepository.save(media);
            log.info("파일 업로드 완료 - ID: {}, UUID: {}", savedMedia.getId(), savedMedia.getFileUuid());
            
            return savedMedia;
            
        } catch (Exception e) {
            log.error("파일 업로드 실패 - 사용자: {}, 파일명: {}", userId, file.getOriginalFilename(), e);
            throw new RuntimeException("파일 업로드 중 오류가 발생했습니다.", e);
        }
    }
    
    /**
     * UUID로 미디어 파일 조회
     * 
     * @param fileUuid 파일 UUID
     * @return Media 엔티티
     */
    public Optional<Media> findByUuid(UUID fileUuid) {
        return mediaRepository.findByFileUuidAndNotDeleted(fileUuid);
    }
    
    /**
     * 사용자별 미디어 파일 목록 조회 (페이징)
     * 
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 페이징된 Media 목록
     */
    public Page<Media> findByUserId(Long userId, Pageable pageable) {
        return mediaRepository.findByUserIdAndNotDeleted(userId, pageable);
    }
    
    /**
     * 사용자별 특정 파일 상태의 미디어 파일 조회
     * 
     * @param userId 사용자 ID
     * @param fileStatus 파일 상태
     * @return Media 목록
     */
    public List<Media> findByUserIdAndStatus(Long userId, FileStatus fileStatus) {
        return mediaRepository.findByUserIdAndFileStatusAndNotDeleted(userId, fileStatus);
    }
    
    /**
     * MIME 타입별 미디어 파일 조회
     * 
     * @param mimeType MIME 타입
     * @return Media 목록
     */
    public List<Media> findByMimeType(String mimeType) {
        return mediaRepository.findByMimeTypeAndDeletedAtIsNull(mimeType);
    }
    
    /**
     * OCR 처리가 필요한 이미지 파일 조회
     * 
     * @return OCR 처리 대상 Media 목록
     */
    public List<Media> findImagesForOcrProcessing() {
        return mediaRepository.findImagesForOcrProcessing();
    }
    
    /**
     * 파일 삭제 (소프트 삭제)
     * 
     * @param fileUuid 파일 UUID
     * @param userId 사용자 ID (권한 확인용)
     * @return 삭제 성공 여부
     */
    @Transactional
    public boolean deleteFile(UUID fileUuid, Long userId) {
        log.info("파일 삭제 요청 - UUID: {}, 사용자: {}", fileUuid, userId);
        
        Optional<Media> mediaOpt = mediaRepository.findByFileUuidAndUserId(fileUuid, userId);
        if (mediaOpt.isEmpty()) {
            log.warn("삭제할 파일을 찾을 수 없음 - UUID: {}, 사용자: {}", fileUuid, userId);
            return false;
        }
        
        Media media = mediaOpt.get();
        if (media.getDeletedAt() != null) {
            log.warn("이미 삭제된 파일 - UUID: {}", fileUuid);
            return false;
        }
        
        // 소프트 삭제 처리
        media.markAsDeleted();
        mediaRepository.save(media);
        
        log.info("파일 소프트 삭제 완료 - UUID: {}", fileUuid);
        return true;
    }
    
    /**
     * 파일 물리적 삭제 (S3에서도 삭제)
     * 
     * @param fileUuid 파일 UUID
     * @param userId 사용자 ID (권한 확인용)
     * @return 삭제 성공 여부
     */
    @Transactional
    public boolean permanentDeleteFile(UUID fileUuid, Long userId) {
        log.info("파일 영구 삭제 요청 - UUID: {}, 사용자: {}", fileUuid, userId);
        
        Optional<Media> mediaOpt = mediaRepository.findByFileUuidAndUserId(fileUuid, userId);
        if (mediaOpt.isEmpty()) {
            log.warn("삭제할 파일을 찾을 수 없음 - UUID: {}, 사용자: {}", fileUuid, userId);
            return false;
        }
        
        Media media = mediaOpt.get();
        
        try {
            // S3에서 파일 삭제
            try {
                s3Service.deleteFile(media.getS3Key());
            } catch (Exception e) {
                log.warn("S3 파일 삭제 실패 - S3 Key: {}", media.getS3Key(), e);
            }
            
            // 데이터베이스에서 삭제
            mediaRepository.delete(media);
            
            log.info("파일 영구 삭제 완료 - UUID: {}", fileUuid);
            return true;
            
        } catch (Exception e) {
            log.error("파일 영구 삭제 실패 - UUID: {}", fileUuid, e);
            return false;
        }
    }
    
    /**
     * OCR 텍스트 업데이트
     * 
     * @param fileUuid 파일 UUID
     * @param ocrText 추출된 OCR 텍스트
     * @return 업데이트 성공 여부
     */
    @Transactional
    public boolean updateOcrText(UUID fileUuid, String ocrText) {
        log.info("OCR 텍스트 업데이트 - UUID: {}", fileUuid);
        
        Optional<Media> mediaOpt = mediaRepository.findByFileUuidAndNotDeleted(fileUuid);
        if (mediaOpt.isEmpty()) {
            log.warn("OCR 업데이트할 파일을 찾을 수 없음 - UUID: {}", fileUuid);
            return false;
        }
        
        Media media = mediaOpt.get();
        media.updateOcrText(ocrText);
        mediaRepository.save(media);
        
        log.info("OCR 텍스트 업데이트 완료 - UUID: {}", fileUuid);
        return true;
    }
    
    /**
     * 파일 상태 업데이트
     * 
     * @param fileUuid 파일 UUID
     * @param newStatus 새로운 파일 상태
     * @return 업데이트 성공 여부
     */
    @Transactional
    public boolean updateFileStatus(UUID fileUuid, FileStatus newStatus) {
        log.info("파일 상태 업데이트 - UUID: {}, 새 상태: {}", fileUuid, newStatus);
        
        Optional<Media> mediaOpt = mediaRepository.findByFileUuidAndNotDeleted(fileUuid);
        if (mediaOpt.isEmpty()) {
            log.warn("상태 업데이트할 파일을 찾을 수 없음 - UUID: {}", fileUuid);
            return false;
        }
        
        Media media = mediaOpt.get();
        media.setFileStatus(newStatus);
        mediaRepository.save(media);
        
        log.info("파일 상태 업데이트 완료 - UUID: {}, 상태: {}", fileUuid, newStatus);
        return true;
    }
    
    /**
     * 사용자별 총 파일 크기 조회
     * 
     * @param userId 사용자 ID
     * @return 총 파일 크기 (바이트)
     */
    public Long getTotalFileSizeByUser(Long userId) {
        return mediaRepository.calculateTotalFileSizeByUserId(userId);
    }
    
    /**
     * 사용자별 파일 개수 조회
     * 
     * @param userId 사용자 ID
     * @return 파일 개수
     */
    public Long getFileCountByUser(Long userId) {
        return mediaRepository.countByUserIdAndNotDeleted(userId);
    }
    
    /**
     * Presigned URL 생성
     * 
     * @param fileUuid 파일 UUID
     * @param userId 사용자 ID (권한 확인용)
     * @return Presigned URL
     */
    public Optional<String> generatePresignedUrl(UUID fileUuid, Long userId) {
        Optional<Media> mediaOpt = mediaRepository.findByFileUuidAndUserId(fileUuid, userId);
        if (mediaOpt.isEmpty()) {
            return Optional.empty();
        }
        
        Media media = mediaOpt.get();
        if (media.getDeletedAt() != null) {
            return Optional.empty();
        }
        
        try {
            String presignedUrl = s3Service.generatePresignedUrl(media.getS3Key());
            return Optional.of(presignedUrl);
        } catch (Exception e) {
            log.error("Presigned URL 생성 실패 - UUID: {}", fileUuid, e);
            return Optional.empty();
        }
    }
    
    /**
     * 저장용 파일명 생성
     * 
     * @param originalFilename 원본 파일명
     * @return 저장용 파일명
     */
    private String generateStoredFilename(String originalFilename) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String extension = "";
        
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        return timestamp + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
    }
}