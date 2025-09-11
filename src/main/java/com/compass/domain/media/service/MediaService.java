package com.compass.domain.media.service;

import com.compass.domain.media.dto.MediaDto;
import com.compass.domain.media.dto.MediaGetResponse;
import com.compass.domain.media.dto.MediaUploadResponse;
import com.compass.domain.media.entity.FileStatus;
import com.compass.domain.media.entity.Media;
import com.compass.domain.media.exception.FileValidationException;
import com.compass.domain.media.repository.MediaRepository;
import com.compass.domain.user.entity.User;
import com.compass.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.CacheControl;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaService {
    
    // OCR 처리 가능한 최대 파일 크기 (50MB)
    private static final long MAX_OCR_FILE_SIZE = 50 * 1024 * 1024;
    
    // 일반 파일 업로드 최대 크기 (100MB)
    private static final long MAX_UPLOAD_FILE_SIZE = 100 * 1024 * 1024;
    
    private final MediaRepository mediaRepository;
    private final UserRepository userRepository;
    private final FileValidationService fileValidationService;
    private final S3Service s3Service;
    private final OCRService ocrService;
    private final ThumbnailService thumbnailService;
    
    
    @Transactional
    public MediaUploadResponse uploadFile(MediaDto.UploadRequest request, Long userId) {
        MultipartFile file = request.getFile();
        log.info("파일 업로드 시작 - 사용자: {}, 파일명: {}", userId, file.getOriginalFilename());
        
        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new FileValidationException("사용자를 찾을 수 없습니다."));
        
        // 파일 검증
        fileValidationService.validateFile(file);
        
        // 파일 크기 제한 검증
        if (file.getSize() > MAX_UPLOAD_FILE_SIZE) {
            throw new FileValidationException("파일 크기가 너무 큽니다. 최대 크기: " + 
                    (MAX_UPLOAD_FILE_SIZE / 1024 / 1024) + "MB");
        }
        
        try {
            // 저장될 파일명 생성
            String storedFilename = generateStoredFilename(file.getOriginalFilename());
            
            // S3에 파일 업로드
            String s3Url = s3Service.uploadFile(file, userId.toString(), storedFilename);
            
            // 메타데이터 생성 (request에서 받은 것과 자동 생성된 것 합치기)
            Map<String, Object> metadata = createMetadata(file, request.getMetadata());
            
            // 이미지 파일인 경우 OCR 처리 및 썸네일 생성
            if (fileValidationService.isSupportedImageFile(file.getContentType())) {
                // OCR 처리 (파일 크기 제한 적용)
                if (file.getSize() <= MAX_OCR_FILE_SIZE) {
                    try {
                        log.info("이미지 파일 OCR 처리 시작 - 파일: {}, 크기: {}MB", 
                                file.getOriginalFilename(), file.getSize() / 1024 / 1024);
                        Map<String, Object> ocrResult = ocrService.extractTextFromImage(file);
                        metadata.put("ocr", ocrResult);
                        log.info("OCR 처리 완료 - 파일: {}, 성공: {}",
                                file.getOriginalFilename(), ocrResult.get("success"));
                } catch (Exception e) {
                    log.warn("OCR 처리 실패 - 파일: {}, 에러: {}",
                            file.getOriginalFilename(), e.getMessage(), e);
                    Map<String, Object> ocrError = new HashMap<>();
                    ocrError.put("success", false);
                    ocrError.put("error", "OCR processing failed: " + e.getMessage());
                    ocrError.put("processedAt", java.time.LocalDateTime.now().toString());
                    metadata.put("ocr", ocrError);
                }
                } else {
                    // 파일이 OCR 제한 크기를 초과하는 경우
                    log.info("OCR 생략 - 파일 크기가 제한을 초과함: {}, 크기: {}MB, 제한: {}MB", 
                            file.getOriginalFilename(), 
                            file.getSize() / 1024 / 1024, 
                            MAX_OCR_FILE_SIZE / 1024 / 1024);
                    
                    Map<String, Object> ocrSkipped = new HashMap<>();
                    ocrSkipped.put("success", false);
                    ocrSkipped.put("error", "File too large for OCR processing (max: " + 
                            (MAX_OCR_FILE_SIZE / 1024 / 1024) + "MB)");
                    ocrSkipped.put("processedAt", java.time.LocalDateTime.now().toString());
                    ocrSkipped.put("skipped", true);
                    metadata.put("ocr", ocrSkipped);
                }

                // 썸네일 생성
                try {
                    log.info("썸네일 생성 시작 - 파일: {}", file.getOriginalFilename());
                    byte[] thumbnailData = thumbnailService.generateThumbnail(file);
                    String thumbnailFilename = thumbnailService.generateThumbnailFilename(storedFilename);
                    String thumbnailS3Url = s3Service.uploadThumbnail(thumbnailData, userId.toString(), thumbnailFilename);

                    // 썸네일 메타데이터 추가
                    Map<String, Object> thumbnailMetadata = thumbnailService.createThumbnailMetadata(thumbnailS3Url, thumbnailFilename);
                    metadata.put("thumbnail", thumbnailMetadata);

                    log.info("썸네일 생성 완료 - 파일: {}, 썸네일 URL: {}", file.getOriginalFilename(), thumbnailS3Url);
                } catch (Exception e) {
                    log.warn("썸네일 생성 실패 - 파일: {}, 에러: {}",
                            file.getOriginalFilename(), e.getMessage(), e);
                    // 썸네일 생성 실패해도 업로드 계속 진행 (에러 맵 생성)
                    Map<String, Object> thumbnailError = new HashMap<>();
                    thumbnailError.put("success", false);
                    thumbnailError.put("error", "Thumbnail generation failed: " + e.getMessage());
                    thumbnailError.put("createdAt", java.time.LocalDateTime.now().toString());
                    metadata.put("thumbnail", thumbnailError);
                }
            }
            
            // Media 엔티티 생성 및 저장
            Media media = Media.builder()
                    .user(user)
                    .originalFilename(file.getOriginalFilename())
                    .storedFilename(storedFilename)
                    .s3Url(s3Url)
                    .fileSize(file.getSize())
                    .mimeType(file.getContentType())
                    .status(FileStatus.UPLOADED)
                    .metadata(metadata)
                    .build();
            
            Media savedMedia = mediaRepository.save(media);
            
            log.info("파일 업로드 완료 - ID: {}, S3 URL: {}", savedMedia.getId(), s3Url);
            
            return MediaUploadResponse.from(savedMedia);
            
        } catch (Exception e) {
            log.error("파일 업로드 중 오류 발생", e);
            throw new FileValidationException("파일 저장 중 오류가 발생했습니다.", e);
        }
    }
    
    private String generateStoredFilename(String originalFilename) {
        String extension = fileValidationService.extractFileExtension(originalFilename);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return String.format("%s_%s%s", timestamp, uuid, extension);
    }
    
    /**
     * 파일을 삭제합니다 (S3에서 삭제하고 DB에서 삭제 표시)
     */
    @Transactional
    public void deleteFile(Long mediaId, Long userId) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new FileValidationException("파일을 찾을 수 없습니다."));
        
        if (!media.getUser().getId().equals(userId)) {
            throw new FileValidationException("파일 삭제 권한이 없습니다.");
        }
        
        if (media.getS3Url() != null) {
            s3Service.deleteFile(media.getS3Url());
        }
        
        media.markAsDeleted();
        mediaRepository.save(media);
        
        log.info("파일 삭제 완료 - ID: {}, 사용자: {}", mediaId, userId);
    }
    
    private Map<String, Object> createMetadata(MultipartFile file, Map<String, Object> requestMetadata) {
        Map<String, Object> metadata = new HashMap<>();
        
        // 자동 생성 메타데이터
        metadata.put("uploadedAt", LocalDateTime.now().toString());
        metadata.put("originalSize", file.getSize());
        metadata.put("contentType", file.getContentType());
        
        // 이미지 파일인 경우 추가 메타데이터 (향후 확장)
        if (fileValidationService.isSupportedImageFile(file.getContentType())) {
            metadata.put("isImage", true);
            metadata.put("imageProcessed", false);
        }
        
        // 요청에서 받은 메타데이터 추가
        if (requestMetadata != null && !requestMetadata.isEmpty()) {
            metadata.putAll(requestMetadata);
        }
        
        return metadata;
    }
    
    /**
     * 파일 정보를 조회합니다 (서명된 URL 포함)
     */
    @Transactional(readOnly = true)
    public MediaGetResponse getMediaById(Long mediaId, Long userId) {
        log.info("파일 조회 시작 - ID: {}, 사용자: {}", mediaId, userId);
        
        // 파일 존재 여부 확인
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new FileValidationException("파일을 찾을 수 없습니다."));
        
        // 권한 체크 (본인 파일인지 확인)
        if (!media.getUser().getId().equals(userId)) {
            throw new FileValidationException("파일 조회 권한이 없습니다.");
        }
        
        // 삭제된 파일인지 확인
        if (media.getDeleted() || media.getStatus() == FileStatus.DELETED) {
            throw new FileValidationException("삭제된 파일입니다.");
        }
        
        // Presigned URL 생성 (15분 만료)
        String presignedUrl = s3Service.generatePresignedUrl(media.getS3Url(), 15);
        
        log.info("파일 조회 완료 - ID: {}, 사용자: {}", mediaId, userId);
        
        return MediaGetResponse.from(media, presignedUrl);
    }
    
    @Transactional(readOnly = true)
    public List<MediaDto.ListResponse> getMediaListByUser(Long userId) {
        log.info("사용자 파일 목록 조회 시작 - 사용자: {}", userId);
        
        List<Media> mediaList = mediaRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId);
        
        log.info("사용자 파일 목록 조회 완료 - 사용자: {}, 파일 수: {}", userId, mediaList.size());
        
        return mediaList.stream()
                .map(MediaDto.ListResponse::from)
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * 미디어 조회 응답용 HTTP 헤더를 생성합니다.
     */
    public HttpHeaders createMediaHeaders(MediaGetResponse response) {
        HttpHeaders headers = new HttpHeaders();
        
        // 캐싱 헤더 설정 (15분)
        CacheControl cacheControl = CacheControl.maxAge(Duration.ofMinutes(15))
                .cachePrivate()
                .mustRevalidate();
        headers.setCacheControl(cacheControl);
        
        // ETag 생성 (mediaId + updatedAt)
        String etag = String.format("\"%d-%s\"", 
                response.getId(), 
                response.getUpdatedAt().toString());
        headers.setETag(etag);
        
        // Last-Modified 설정 (LocalDateTime을 Instant로 변환)
        headers.setLastModified(response.getUpdatedAt().atZone(ZoneOffset.UTC).toInstant());
        
        return headers;
    }
    
    public Long getUserIdByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new FileValidationException("사용자를 찾을 수 없습니다."));
        return user.getId();
    }
    
    /**
     * 기존 미디어 파일에 대해 OCR을 수행하고 결과를 메타데이터에 저장합니다.
     * 트랜잭션 경계를 최적화하여 외부 API 호출을 분리합니다.
     */
    public void processOCRForMedia(Long mediaId, Long userId) {
        log.info("기존 미디어 OCR 처리 시작 - ID: {}, 사용자: {}", mediaId, userId);
        
        // 1. 권한 검증 및 파일 정보 조회 (빠른 트랜잭션)
        Media media = validateMediaAccess(mediaId, userId);
        
        if (!fileValidationService.isSupportedImageFile(media.getMimeType())) {
            throw new FileValidationException("OCR은 이미지 파일만 지원합니다.");
        }
        
        // 파일 크기 체크 (메모리 보호)
        if (media.getFileSize() > MAX_OCR_FILE_SIZE) {
            throw new FileValidationException("OCR 처리 가능한 최대 파일 크기를 초과했습니다. (최대: " + 
                    (MAX_OCR_FILE_SIZE / 1024 / 1024) + "MB)");
        }
        
        // 2. OCR 처리 (외부 API 호출, 트랜잭션 외부)
        Map<String, Object> ocrResult = performOCRProcessing(media);
        
        // 3. 결과 저장 (빠른 트랜잭션)
        updateMediaMetadata(mediaId, ocrResult);
    }
    
    /**
     * 미디어 접근 권한을 검증합니다.
     */
    @Transactional(readOnly = true)
    public Media validateMediaAccess(Long mediaId, Long userId) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new FileValidationException("파일을 찾을 수 없습니다."));
        
        if (!media.getUser().getId().equals(userId)) {
            throw new FileValidationException("파일 처리 권한이 없습니다.");
        }
        
        return media;
    }
    
    /**
     * OCR 처리를 수행합니다 (트랜잭션 외부).
     */
    private Map<String, Object> performOCRProcessing(Media media) {
        try {
            // S3에서 파일 다운로드하여 OCR 처리
            byte[] imageBytes = s3Service.downloadFile(media.getS3Url());
            return ocrService.extractTextFromBytes(imageBytes, media.getOriginalFilename());
            
        } catch (Exception e) {
            log.error("OCR 처리 중 오류 발생 - 미디어 ID: {}", media.getId(), e);
            
            // OCR 실패 정보를 포함한 결과 반환 (graceful degradation)
            Map<String, Object> failedResult = new HashMap<>();
            failedResult.put("success", false);
            failedResult.put("error", "OCR processing failed: " + e.getMessage());
            failedResult.put("processedAt", java.time.LocalDateTime.now().toString());
            return failedResult;
        }
    }
    
    /**
     * 미디어 메타데이터를 업데이트합니다.
     */
    @Transactional
    public void updateMediaMetadata(Long mediaId, Map<String, Object> ocrResult) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new FileValidationException("파일을 찾을 수 없습니다."));
        
        // 기존 메타데이터에 OCR 결과 추가
        Map<String, Object> metadata = media.getMetadata() != null ? 
                new HashMap<>(media.getMetadata()) : new HashMap<>();
        metadata.put("ocr", ocrResult);
        
        media.updateMetadata(metadata);
        mediaRepository.save(media);
        
        log.info("미디어 메타데이터 업데이트 완료 - ID: {}, OCR 성공: {}", 
                mediaId, ocrResult.get("success"));
    }
    
    /**
     * 미디어 파일의 OCR 결과를 조회합니다.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getOCRResult(Long mediaId, Long userId) {
        log.info("OCR 결과 조회 시작 - ID: {}, 사용자: {}", mediaId, userId);
        
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new FileValidationException("파일을 찾을 수 없습니다."));
        
        if (!media.getUser().getId().equals(userId)) {
            throw new FileValidationException("파일 조회 권한이 없습니다.");
        }
        
        if (media.getMetadata() == null || !media.getMetadata().containsKey("ocr")) {
            Map<String, Object> emptyResult = new HashMap<>();
            emptyResult.put("success", false);
            emptyResult.put("error", "OCR 결과가 없습니다. 이미지 파일이 아니거나 OCR이 처리되지 않았습니다.");
            return emptyResult;
        }
        
        return (Map<String, Object>) media.getMetadata().get("ocr");
    }

    /**
     * 미디어 파일의 썸네일 결과를 조회합니다.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getThumbnailResult(Long mediaId, Long userId) {
        log.info("썸네일 결과 조회 시작 - ID: {}, 사용자: {}", mediaId, userId);

        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new FileValidationException("파일을 찾을 수 없습니다."));

        if (!media.getUser().getId().equals(userId)) {
            throw new FileValidationException("파일 조회 권한이 없습니다.");
        }

        if (media.getMetadata() == null || !media.getMetadata().containsKey("thumbnail")) {
            Map<String, Object> emptyResult = new HashMap<>();
            emptyResult.put("success", false);
            emptyResult.put("error", "썸네일이 없습니다. 이미지 파일이 아니거나 썸네일 생성이 실패했습니다.");
            return emptyResult;
        }

        return (Map<String, Object>) media.getMetadata().get("thumbnail");
    }

    /**
     * 미디어 파일의 썸네일 URL 결과를 조회합니다.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getThumbnailUrlResult(Long mediaId, Long userId) {
        log.info("썸네일 URL 결과 조회 시작 - ID: {}, 사용자: {}", mediaId, userId);

        Map<String, Object> thumbnailResult = getThumbnailResult(mediaId, userId);

        if (!(Boolean) thumbnailResult.get("success")) {
            return thumbnailResult;
        }

        // 썸네일 URL에 대해 Presigned URL 생성
        String thumbnailUrl = (String) thumbnailResult.get("url");
        String presignedThumbnailUrl = s3Service.generatePresignedUrl(thumbnailUrl, 15); // 15분 만료

        Map<String, Object> result = new HashMap<>(thumbnailResult);
        result.put("presignedUrl", presignedThumbnailUrl);

        log.info("썸네일 URL 결과 조회 완료 - ID: {}, 사용자: {}", mediaId, userId);

        return result;
    }
}