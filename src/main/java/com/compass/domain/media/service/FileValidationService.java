package com.compass.domain.media.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * FileValidationService - 파일 유효성 검증 및 보안 검사 서비스
 * REQ-MEDIA-005 요구사항 구현
 */
@Service
public class FileValidationService {
    
    private static final Logger logger = LoggerFactory.getLogger(FileValidationService.class);
    
    // 허용된 이미지 MIME 타입
    private static final Set<String> ALLOWED_IMAGE_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/jpg", 
            "image/png",
            "image/gif",
            "image/webp",
            "image/bmp",
            "image/tiff"
    );
    
    // 허용된 파일 확장자
    private static final Set<String> ALLOWED_FILE_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "tiff"
    );
    
    // 이미지 파일 시그니처 (매직 넘버)
    private static final List<byte[]> IMAGE_SIGNATURES = Arrays.asList(
            new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}, // JPEG
            new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}, // PNG
            new byte[]{0x47, 0x49, 0x46, 0x38, 0x37, 0x61}, // GIF87a
            new byte[]{0x47, 0x49, 0x46, 0x38, 0x39, 0x61}, // GIF89a
            new byte[]{0x52, 0x49, 0x46, 0x46}, // RIFF (WebP)
            new byte[]{0x42, 0x4D}, // BMP
            new byte[]{0x49, 0x49, 0x2A, 0x00}, // TIFF (little endian)
            new byte[]{0x4D, 0x4D, 0x00, 0x2A}  // TIFF (big endian)
    );
    
    // 악성 파일 시그니처 (차단할 파일 타입)
    private static final List<byte[]> MALICIOUS_SIGNATURES = Arrays.asList(
            new byte[]{0x4D, 0x5A}, // PE/EXE files
            new byte[]{0x50, 0x4B, 0x03, 0x04}, // ZIP files (potential script archives)
            new byte[]{0x50, 0x4B, 0x05, 0x06}, // ZIP files (empty archive)
            new byte[]{0x50, 0x4B, 0x07, 0x08}, // ZIP files (spanned archive)
            new byte[]{0x7F, 0x45, 0x4C, 0x46}, // ELF executables
            new byte[]{(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE}, // Java class files
            new byte[]{0x3C, 0x3F, 0x70, 0x68, 0x70} // PHP files
    );
    
    @Value("${app.file.max-size:10485760}") // 10MB 기본값
    private long maxFileSize;
    
    @Value("${app.file.max-width:4096}")
    private int maxImageWidth;
    
    @Value("${app.file.max-height:4096}")
    private int maxImageHeight;
    
    /**
     * 파일 전체 유효성 검증
     * 
     * @param file 검증할 파일
     * @throws FileValidationException 검증 실패 시
     */
    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileValidationException("파일이 비어있습니다.");
        }
        
        validateFileSize(file);
        validateFileName(file.getOriginalFilename());
        validateMimeType(file.getContentType());
        validateFileSignature(file);
        validateFileContent(file);
        
        logger.info("파일 유효성 검증 완료: {}", file.getOriginalFilename());
    }
    
    /**
     * 파일 크기 검증
     */
    private void validateFileSize(MultipartFile file) {
        if (file.getSize() > maxFileSize) {
            throw new FileValidationException(
                String.format("파일 크기가 제한을 초과했습니다. 최대 크기: %d bytes, 현재 크기: %d bytes", 
                    maxFileSize, file.getSize())
            );
        }
        
        if (file.getSize() == 0) {
            throw new FileValidationException("파일 크기가 0입니다.");
        }
    }
    
    /**
     * 파일명 검증
     */
    private void validateFileName(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new FileValidationException("파일명이 비어있습니다.");
        }
        
        // 파일명 길이 제한
        if (filename.length() > 255) {
            throw new FileValidationException("파일명이 너무 깁니다. (최대 255자)");
        }
        
        // 위험한 문자 검사
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new FileValidationException("파일명에 허용되지 않는 문자가 포함되어 있습니다.");
        }
        
        // 파일 확장자 검증
        String extension = getFileExtension(filename).toLowerCase();
        if (!ALLOWED_FILE_EXTENSIONS.contains(extension)) {
            throw new FileValidationException(
                String.format("허용되지 않는 파일 확장자입니다: %s. 허용된 확장자: %s", 
                    extension, ALLOWED_FILE_EXTENSIONS)
            );
        }
    }
    
    /**
     * MIME 타입 검증
     */
    private void validateMimeType(String mimeType) {
        if (mimeType == null || mimeType.trim().isEmpty()) {
            throw new FileValidationException("MIME 타입이 비어있습니다.");
        }
        
        if (!ALLOWED_IMAGE_MIME_TYPES.contains(mimeType.toLowerCase())) {
            throw new FileValidationException(
                String.format("허용되지 않는 MIME 타입입니다: %s. 허용된 타입: %s", 
                    mimeType, ALLOWED_IMAGE_MIME_TYPES)
            );
        }
    }
    
    /**
     * 파일 시그니처 검증 (매직 넘버 체크)
     */
    private void validateFileSignature(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            byte[] header = new byte[16]; // 처음 16바이트 읽기
            int bytesRead = inputStream.read(header);
            
            if (bytesRead < 4) {
                throw new FileValidationException("파일이 너무 작습니다.");
            }
            
            // 악성 파일 시그니처 검사
            for (byte[] maliciousSignature : MALICIOUS_SIGNATURES) {
                if (matchesSignature(header, maliciousSignature)) {
                    throw new FileValidationException("허용되지 않는 파일 형식입니다.");
                }
            }
            
            // 이미지 파일 시그니처 검사
            boolean isValidImage = false;
            for (byte[] imageSignature : IMAGE_SIGNATURES) {
                if (matchesSignature(header, imageSignature)) {
                    isValidImage = true;
                    break;
                }
            }
            
            if (!isValidImage) {
                throw new FileValidationException("유효한 이미지 파일이 아닙니다.");
            }
            
        } catch (IOException e) {
            logger.error("파일 시그니처 검증 중 오류 발생", e);
            throw new FileValidationException("파일 읽기 중 오류가 발생했습니다.");
        }
    }
    
    /**
     * 파일 내용 검증 (추가 보안 검사)
     */
    private void validateFileContent(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            
            // 파일 내용에서 스크립트 태그나 악성 코드 패턴 검사
            StringBuilder content = new StringBuilder();
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                content.append(new String(buffer, 0, bytesRead));
                
                // 메모리 사용량 제한 (처음 10KB만 검사)
                if (content.length() > 10240) {
                    break;
                }
            }
            
            String fileContent = content.toString().toLowerCase();
            
            // 위험한 스크립트 패턴 검사
            String[] dangerousPatterns = {
                "<script", "javascript:", "vbscript:", "onload=", "onerror=",
                "<?php", "<%", "<jsp:", "eval(", "exec("
            };
            
            for (String pattern : dangerousPatterns) {
                if (fileContent.contains(pattern)) {
                    throw new FileValidationException("파일에 허용되지 않는 스크립트가 포함되어 있습니다.");
                }
            }
            
        } catch (IOException e) {
            logger.error("파일 내용 검증 중 오류 발생", e);
            throw new FileValidationException("파일 내용 검증 중 오류가 발생했습니다.");
        }
    }
    
    /**
     * 시그니처 매칭 검사
     */
    private boolean matchesSignature(byte[] header, byte[] signature) {
        if (header.length < signature.length) {
            return false;
        }
        
        for (int i = 0; i < signature.length; i++) {
            if (header[i] != signature[i]) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
    
    /**
     * 이미지 파일 여부 확인
     */
    public boolean isImageFile(String mimeType) {
        return mimeType != null && ALLOWED_IMAGE_MIME_TYPES.contains(mimeType.toLowerCase());
    }
    
    /**
     * 허용된 파일 확장자 목록 반환
     */
    public Set<String> getAllowedExtensions() {
        return ALLOWED_FILE_EXTENSIONS;
    }
    
    /**
     * 허용된 MIME 타입 목록 반환
     */
    public Set<String> getAllowedMimeTypes() {
        return ALLOWED_IMAGE_MIME_TYPES;
    }
    
    /**
     * 최대 파일 크기 반환
     */
    public long getMaxFileSize() {
        return maxFileSize;
    }
    
    /**
     * 파일 유효성 검증 예외 클래스
     */
    public static class FileValidationException extends RuntimeException {
        public FileValidationException(String message) {
            super(message);
        }
        
        public FileValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}