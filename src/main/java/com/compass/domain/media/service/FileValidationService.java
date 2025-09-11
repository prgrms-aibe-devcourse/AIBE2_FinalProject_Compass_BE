package com.compass.domain.media.service;

import com.compass.domain.media.config.MediaValidationProperties;
import com.compass.domain.media.exception.FileValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileValidationService {
    
    private final MediaValidationProperties validationProperties;
    
    
    // 보안 검증을 위한 상수들
    private static final int MAX_FILENAME_LENGTH = 255;
    private static final List<String> WINDOWS_RESERVED_NAMES = Arrays.asList(
        "CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", 
        "COM7", "COM8", "COM9", "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", 
        "LPT7", "LPT8", "LPT9"
    );
    
    // 악성 패턴 탐지
    private static final Pattern SCRIPT_PATTERN = Pattern.compile(
        "(?i)<\\s*(script|iframe|object|embed|form|input|link|meta)\\s*[^>]*>", Pattern.MULTILINE
    );
    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(
        "(\\.{2}[\\\\/])|(\\.{2}$)|([\\\\/]\\.{2})|(\\.{2}\\.)|(^[\\\\/])"
    );
    private static final Pattern DANGEROUS_CHARS_PATTERN = Pattern.compile(
        "[<>:\"|?*\\x00-\\x1f\\x7f-\\x9f]"
    );
    
    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileValidationException("파일이 비어있습니다.");
        }
        
        validateFileSize(file);
        validateMimeType(file);
        validateFileExtension(file);
        validateSecureFilename(file);
        scanForMaliciousContent(file);
        validateFileHeader(file);
        validateImageMetadata(file);
    }
    
    private void validateFileSize(MultipartFile file) {
        if (file.getSize() > validationProperties.getMaxFileSize()) {
            throw new FileValidationException(
                String.format("파일 크기가 허용된 최대 크기 %dMB를 초과합니다.", 
                    validationProperties.getMaxFileSize() / (1024 * 1024))
            );
        }
    }
    
    private void validateMimeType(MultipartFile file) {
        String mimeType = file.getContentType();
        if (mimeType == null || !validationProperties.getSupportedMimeTypes().contains(mimeType.toLowerCase())) {
            throw new FileValidationException(
                "허용되지 않는 파일 형식입니다. 허용되는 형식: " + 
                String.join(", ", validationProperties.getSupportedMimeTypes())
            );
        }
    }
    
    private void validateFileExtension(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new FileValidationException("파일명이 없습니다.");
        }
        
        String extension = getFileExtension(filename);
        if (!validationProperties.getSupportedExtensions().contains(extension.toLowerCase())) {
            throw new FileValidationException(
                "허용되지 않는 파일 확장자입니다. 허용되는 확장자: " + 
                String.join(", ", validationProperties.getSupportedExtensions())
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
    
    /**
     * 파일명에서 확장자를 추출합니다.
     * @param filename 파일명
     * @return 확장자 (점 포함, 예: ".jpg") 또는 빈 문자열
     */
    public static String extractFileExtension(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex);
    }
    
    private String getFileExtension(String filename) {
        return extractFileExtension(filename);
    }
    
    public boolean isSupportedImageFile(String mimeType) {
        return mimeType != null && validationProperties.getImageMimeTypes().contains(mimeType.toLowerCase());
    }
    
    /**
     * 파일명 보안 검증 - 경로 조작, 특수문자, 예약어 등 차단
     */
    private void validateSecureFilename(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null || filename.trim().isEmpty()) {
            throw new FileValidationException("유효한 파일명이 필요합니다.");
        }
        
        // 파일명 길이 체크
        if (filename.length() > MAX_FILENAME_LENGTH) {
            throw new FileValidationException(
                String.format("파일명이 너무 깁니다. 최대 %d자까지 허용됩니다.", MAX_FILENAME_LENGTH)
            );
        }
        
        // 경로 조작 시도 탐지
        if (PATH_TRAVERSAL_PATTERN.matcher(filename).find()) {
            log.warn("경로 조작 시도 탐지: {}", filename);
            throw new FileValidationException("파일명에 경로 조작 패턴이 포함되어 있습니다.");
        }
        
        // 위험한 특수문자 체크
        if (containsForbiddenChars(filename)) {
            log.warn("위험한 특수문자 탐지: {}", filename);
            throw new FileValidationException("파일명에 허용되지 않는 특수문자가 포함되어 있습니다.");
        }
        
        // Windows 예약어 체크
        String nameWithoutExt = filename.substring(0, filename.lastIndexOf('.') != -1 ? 
            filename.lastIndexOf('.') : filename.length());
        if (WINDOWS_RESERVED_NAMES.contains(nameWithoutExt.toUpperCase())) {
            log.warn("Windows 예약어 사용 탐지: {}", nameWithoutExt);
            throw new FileValidationException("파일명으로 사용할 수 없는 예약어입니다: " + nameWithoutExt);
        }
        
        log.debug("파일명 보안 검증 통과: {}", filename);
    }
    
    /**
     * 악성 코드 패턴 검사
     */
    private void scanForMaliciousContent(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            String content = new String(bytes, "UTF-8");
            
            // 스크립트 태그 탐지
            if (SCRIPT_PATTERN.matcher(content).find()) {
                log.warn("악성 스크립트 패턴 탐지됨 - 파일: {}", file.getOriginalFilename());
                throw new FileValidationException("파일에 위험한 스크립트 코드가 포함되어 있습니다.");
            }
            
            // 악성 파일 시그니처 체크
            if (containsMaliciousSignature(bytes)) {
                log.warn("악성 파일 시그니처 탐지: {}", file.getOriginalFilename());
                throw new FileValidationException("악성 파일 시그니처가 탐지되었습니다.");
            }
            
            // 폴리글롯 파일 탐지 (JPEG + HTML 조합)
            if (isPolygotFile(bytes)) {
                log.warn("폴리글롯 파일 탐지: {}", file.getOriginalFilename());
                throw new FileValidationException("다중 형식 파일(폴리글롯)은 보안상 업로드할 수 없습니다.");
            }
            
            log.debug("악성 코드 검사 통과: {}", file.getOriginalFilename());
            
        } catch (IOException e) {
            log.error("악성 코드 검사 중 오류 발생", e);
            throw new FileValidationException("파일 보안 검사 중 오류가 발생했습니다.");
        }
    }
    
    /**
     * 폴리글롯 파일 탐지 (여러 형식으로 해석 가능한 파일)
     */
    private boolean isPolygotFile(byte[] bytes) {
        try {
            String content = new String(bytes, "UTF-8");
            
            // JPEG 시그니처와 HTML 태그가 동시에 존재하는지 확인
            boolean hasJpegSignature = bytes.length >= 2 && 
                (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8;
            boolean hasHtmlContent = content.toLowerCase().contains("<html") || 
                content.toLowerCase().contains("<!doctype") ||
                SCRIPT_PATTERN.matcher(content).find();
            
            return hasJpegSignature && hasHtmlContent;
            
        } catch (Exception e) {
            // 인코딩 오류 등은 무시하고 false 반환
            return false;
        }
    }
    
    /**
     * 이미지 메타데이터 검증
     */
    private void validateImageMetadata(MultipartFile file) {
        try {
            String mimeType = file.getContentType();
            if (!isSupportedImageFile(mimeType)) {
                return; // 이미지가 아니면 스킵
            }
            
            byte[] bytes = file.getBytes();
            
            // Image Bomb 방지 - 압축률 체크
            if (detectImageBomb(bytes, file.getSize())) {
                log.warn("Image Bomb 위험 탐지: {}", file.getOriginalFilename());
                throw new FileValidationException("이미지 압축 해제 시 과도한 메모리를 사용할 수 있는 파일입니다.");
            }
            
            // 과도한 EXIF 데이터 체크
            if (hasExcessiveMetadata(bytes)) {
                log.warn("과도한 메타데이터 탐지: {}", file.getOriginalFilename());
                throw new FileValidationException("이미지 메타데이터가 비정상적으로 큽니다.");
            }
            
            log.debug("이미지 메타데이터 검증 통과: {}", file.getOriginalFilename());
            
        } catch (IOException e) {
            log.error("이미지 메타데이터 검증 중 오류", e);
            throw new FileValidationException("이미지 메타데이터 검증 중 오류가 발생했습니다.");
        }
    }
    
    /**
     * Image Bomb 탐지 (압축 해제 시 메모리 폭증 위험)
     */
    private boolean detectImageBomb(byte[] bytes, long fileSize) {
        // 단순 휴리스틱: 파일 크기 대비 과도하게 큰 이미지 차원 추정
        if (fileSize < 1000 && bytes.length > 100) {
            // 매우 작은 파일 크기에 많은 데이터 - 의심스러운 압축률
            return true;
        }
        
        // JPEG의 경우 SOF 마커에서 이미지 차원 확인
        if (bytes.length >= 4 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) {
            return checkJpegDimensions(bytes, fileSize);
        }
        
        return false;
    }
    
    /**
     * JPEG 이미지 차원 체크
     */
    private boolean checkJpegDimensions(byte[] bytes, long fileSize) {
        // SOF 마커 (Start of Frame) 찾기
        for (int i = 2; i < bytes.length - 10; i++) {
            if ((bytes[i] & 0xFF) == 0xFF && 
                ((bytes[i+1] & 0xFF) == 0xC0 || (bytes[i+1] & 0xFF) == 0xC2)) {
                
                // 이미지 높이와 너비 추출 (빅 엔디안)
                int height = ((bytes[i+5] & 0xFF) << 8) | (bytes[i+6] & 0xFF);
                int width = ((bytes[i+7] & 0xFF) << 8) | (bytes[i+8] & 0xFF);
                
                // 4K 해상도 이상 제한
                if (width > 4000 || height > 4000) {
                    log.warn("과도한 이미지 해상도: {}x{}", width, height);
                    throw new FileValidationException(
                        String.format("이미지 해상도가 너무 큽니다. (%dx%d) 최대 4000x4000 픽셀까지 허용됩니다.", width, height)
                    );
                }
                
                // 압축률 기반 Image Bomb 탐지
                long estimatedUncompressedSize = (long) width * height * 3; // RGB
                double compressionRatio = (double) estimatedUncompressedSize / fileSize;
                
                if (compressionRatio > 1000) { // 1000:1 이상 압축률
                    log.warn("의심스러운 압축률: {}", compressionRatio);
                    return true;
                }
                
                break;
            }
        }
        return false;
    }
    
    /**
     * 과도한 메타데이터 체크
     */
    private boolean hasExcessiveMetadata(byte[] bytes) {
        // EXIF 데이터 크기가 원본 파일의 30% 이상이면 의심
        int exifSize = 0;
        
        // JPEG EXIF 데이터 크기 추정
        if (bytes.length >= 4 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) {
            for (int i = 2; i < bytes.length - 4; i++) {
                if ((bytes[i] & 0xFF) == 0xFF && (bytes[i+1] & 0xFF) == 0xE1) {
                    // EXIF 세그먼트 길이
                    exifSize = ((bytes[i+2] & 0xFF) << 8) | (bytes[i+3] & 0xFF);
                    break;
                }
            }
        }
        
        return exifSize > 0 && (double) exifSize / bytes.length > 0.3;
    }
    
    /**
     * 금지된 특수문자 포함 여부 검사
     */
    private boolean containsForbiddenChars(String filename) {
        return validationProperties.getForbiddenChars().stream()
                .anyMatch(filename::contains);
    }
    
    /**
     * 악성 파일 시그니처 검사
     */
    private boolean containsMaliciousSignature(byte[] bytes) {
        if (bytes.length < 4) return false;
        
        // 바이트 배열을 16진수 문자열로 변환
        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < Math.min(bytes.length, 16); i++) {
            hex.append(String.format("%02X", bytes[i]));
        }
        String hexString = hex.toString();
        
        // 설정된 악성 시그니처들과 비교
        return validationProperties.getMaliciousSignatures().stream()
                .anyMatch(signature -> hexString.startsWith(signature));
    }
    
}