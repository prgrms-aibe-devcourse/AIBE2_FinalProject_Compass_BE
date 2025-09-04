package com.compass.domain.media.service;

import com.compass.domain.media.dto.MediaUploadResponse;
import com.compass.domain.media.entity.FileStatus;
import com.compass.domain.media.entity.Media;
import com.compass.domain.media.exception.FileValidationException;
import com.compass.domain.media.repository.MediaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaService {
    
    private final MediaRepository mediaRepository;
    private final FileValidationService fileValidationService;
    
    private static final String UPLOAD_DIR = "uploads/temp/";
    
    @Transactional
    public MediaUploadResponse uploadFile(MultipartFile file, String userId) {
        log.info("파일 업로드 시작 - 사용자: {}, 파일명: {}", userId, file.getOriginalFilename());
        
        // 파일 검증
        fileValidationService.validateFile(file);
        
        try {
            // 임시 저장소에 파일 저장
            String storedFilename = generateStoredFilename(file.getOriginalFilename());
            String tempFilePath = saveFileToTempStorage(file, storedFilename);
            
            // 메타데이터 생성
            Map<String, Object> metadata = createMetadata(file);
            
            // Media 엔티티 생성 및 저장
            Media media = Media.builder()
                    .userId(userId)
                    .originalFilename(file.getOriginalFilename())
                    .storedFilename(storedFilename)
                    .s3Url(null) // S3 업로드는 다음 단계에서 구현
                    .fileSize(file.getSize())
                    .mimeType(file.getContentType())
                    .status(FileStatus.UPLOADED)
                    .metadata(metadata)
                    .build();
            
            Media savedMedia = mediaRepository.save(media);
            
            log.info("파일 업로드 완료 - ID: {}, 파일명: {}", savedMedia.getId(), storedFilename);
            
            return MediaUploadResponse.from(
                    savedMedia.getId(),
                    savedMedia.getOriginalFilename(),
                    savedMedia.getStoredFilename(),
                    savedMedia.getS3Url(),
                    savedMedia.getFileSize(),
                    savedMedia.getMimeType(),
                    savedMedia.getStatus(),
                    savedMedia.getMetadata(),
                    savedMedia.getCreatedAt()
            );
            
        } catch (IOException e) {
            log.error("파일 업로드 중 오류 발생", e);
            throw new FileValidationException("파일 저장 중 오류가 발생했습니다.", e);
        }
    }
    
    private String generateStoredFilename(String originalFilename) {
        String extension = getFileExtension(originalFilename);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return String.format("%s_%s%s", timestamp, uuid, extension);
    }
    
    private String getFileExtension(String filename) {
        if (filename == null) return "";
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex == -1 ? "" : filename.substring(lastDotIndex);
    }
    
    private String saveFileToTempStorage(MultipartFile file, String storedFilename) throws IOException {
        // 임시 저장소 디렉터리 생성
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // 파일 저장
        Path filePath = uploadPath.resolve(storedFilename);
        Files.write(filePath, file.getBytes());
        
        log.debug("임시 파일 저장 완료: {}", filePath.toString());
        return filePath.toString();
    }
    
    private Map<String, Object> createMetadata(MultipartFile file) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("uploadedAt", LocalDateTime.now().toString());
        metadata.put("originalSize", file.getSize());
        metadata.put("contentType", file.getContentType());
        
        // 이미지 파일인 경우 추가 메타데이터 (향후 확장)
        if (fileValidationService.isSupportedImageFile(file.getContentType())) {
            metadata.put("isImage", true);
            metadata.put("imageProcessed", false);
        }
        
        return metadata;
    }
}