package com.compass.domain.media.service;

import com.compass.domain.media.exception.FileValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class FileValidationService {
    
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
        "image/jpeg",
        "image/jpg", 
        "image/png",
        "image/webp",
        "image/gif"
    );
    
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
        ".jpg", ".jpeg", ".png", ".webp", ".gif"
    );
    
    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileValidationException("파일이 비어있습니다.");
        }
        
        validateFileSize(file);
        validateMimeType(file);
        validateFileExtension(file);
        validateFileHeader(file);
    }
    
    private void validateFileSize(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileValidationException(
                String.format("파일 크기가 허용된 최대 크기 %dMB를 초과합니다.", MAX_FILE_SIZE / (1024 * 1024))
            );
        }
    }
    
    private void validateMimeType(MultipartFile file) {
        String mimeType = file.getContentType();
        if (mimeType == null || !ALLOWED_MIME_TYPES.contains(mimeType.toLowerCase())) {
            throw new FileValidationException(
                "허용되지 않는 파일 형식입니다. 허용되는 형식: " + String.join(", ", ALLOWED_MIME_TYPES)
            );
        }
    }
    
    private void validateFileExtension(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new FileValidationException("파일명이 없습니다.");
        }
        
        String extension = getFileExtension(filename);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new FileValidationException(
                "허용되지 않는 파일 확장자입니다. 허용되는 확장자: " + String.join(", ", ALLOWED_EXTENSIONS)
            );
        }
    }
    
    private void validateFileHeader(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length < 4) {
                throw new FileValidationException("유효하지 않은 파일입니다.");
            }
            
            // JPEG 파일 헤더 체크
            if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8) {
                log.debug("JPEG 파일로 확인됨");
                return;
            }
            
            // PNG 파일 헤더 체크
            if (bytes[0] == (byte) 0x89 && bytes[1] == 0x50 && 
                bytes[2] == 0x4E && bytes[3] == 0x47) {
                log.debug("PNG 파일로 확인됨");
                return;
            }
            
            // WEBP 파일 헤더 체크
            if (bytes.length >= 12) {
                String header = new String(bytes, 0, 4);
                String webpHeader = new String(bytes, 8, 4);
                if ("RIFF".equals(header) && "WEBP".equals(webpHeader)) {
                    log.debug("WEBP 파일로 확인됨");
                    return;
                }
            }
            
            // GIF 파일 헤더 체크
            if (bytes.length >= 6) {
                String header = new String(bytes, 0, 6);
                if ("GIF87a".equals(header) || "GIF89a".equals(header)) {
                    log.debug("GIF 파일로 확인됨");
                    return;
                }
            }
            
            throw new FileValidationException("파일 헤더가 유효하지 않습니다. 이미지 파일이 아니거나 손상되었을 수 있습니다.");
            
        } catch (IOException e) {
            log.error("파일 헤더 검증 중 오류 발생", e);
            throw new FileValidationException("파일 검증 중 오류가 발생했습니다.");
        }
    }
    
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex);
    }
    
    public boolean isSupportedImageFile(String mimeType) {
        return mimeType != null && ALLOWED_MIME_TYPES.contains(mimeType.toLowerCase());
    }
}