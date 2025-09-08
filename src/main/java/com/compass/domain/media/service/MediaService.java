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
    
    private final MediaRepository mediaRepository;
    private final UserRepository userRepository;
    private final FileValidationService fileValidationService;
    private final S3Service s3Service;
    
    
    @Transactional
    public MediaUploadResponse uploadFile(MediaDto.UploadRequest request, Long userId) {
        MultipartFile file = request.getFile();
        log.info("파일 업로드 시작 - 사용자: {}, 파일명: {}", userId, file.getOriginalFilename());
        
        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new FileValidationException("사용자를 찾을 수 없습니다."));
        
        // 파일 검증
        fileValidationService.validateFile(file);
        
        try {
            // 저장될 파일명 생성
            String storedFilename = generateStoredFilename(file.getOriginalFilename());
            
            // S3에 파일 업로드
            String s3Url = s3Service.uploadFile(file, userId.toString(), storedFilename);
            
            // 메타데이터 생성 (request에서 받은 것과 자동 생성된 것 합치기)
            Map<String, Object> metadata = createMetadata(file, request.getMetadata());
            
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
}