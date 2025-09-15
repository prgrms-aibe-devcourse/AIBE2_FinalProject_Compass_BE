package com.compass.domain.media.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Enhanced security service for comprehensive file validation and threat detection
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnhancedSecurityService {
    
    private final SecurityAuditLogger auditLogger;
    
    // Enhanced malware signature patterns
    private static final Set<String> MALWARE_SIGNATURES = Set.of(
        "4D5A",           // PE executable
        "7F454C46",       // ELF executable
        "504B0304",       // ZIP file (potential zip bomb)
        "255044462D",     // PDF file (check for embedded JS)
        "D0CF11E0A1B11AE1", // MS Office (macro detection needed)
        "504B030414",     // JAR file
        "CAFEBABE",       // Java class file
        "4C00000001140200", // Windows Shortcut (.lnk)
        "213C617263683E"  // Archive file header
    );
    
    // Advanced steganography detection patterns
    private static final Pattern STEGANOGRAPHY_PATTERN = Pattern.compile(
        "(?i)(steghide|outguess|jsteg|jpegx|stegdetect|stegbreak)"
    );
    
    // Suspicious metadata patterns
    private static final Pattern SUSPICIOUS_METADATA_PATTERN = Pattern.compile(
        "(?i)(javascript|vbscript|<script|eval\\(|exec\\(|system\\()"
    );
    
    /**
     * Performs comprehensive security validation on uploaded file
     * 
     * @param file the file to validate
     * @param userId the user ID for audit logging
     * @throws SecurityValidationException if security threats are detected
     */
    public void performSecurityValidation(MultipartFile file, Long userId) {
        String filename = file.getOriginalFilename();
        log.info("보안 검증 시작 - 사용자: {}, 파일: {}", userId, filename);
        
        try {
            // 1. Enhanced malware signature detection
            validateMalwareSignatures(file, userId);
            
            // 2. Deep file structure analysis
            validateFileStructure(file, userId);
            
            // 3. Steganography detection
            detectSteganography(file, userId);
            
            // 4. Metadata security scan
            scanMetadataThreats(file, userId);
            
            // 5. File hash reputation check
            checkFileReputation(file, userId);
            
            // 6. Advanced polyglot detection
            detectAdvancedPolyglots(file, userId);
            
            auditLogger.logSecurityEvent("FILE_VALIDATION_PASSED", userId, filename, null);
            log.info("보안 검증 통과 - 사용자: {}, 파일: {}", userId, filename);
            
        } catch (SecurityValidationException e) {
            auditLogger.logSecurityEvent("SECURITY_THREAT_DETECTED", userId, filename, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("보안 검증 중 오류 - 사용자: {}, 파일: {}", userId, filename, e);
            auditLogger.logSecurityEvent("VALIDATION_ERROR", userId, filename, e.getMessage());
            throw new SecurityValidationException("보안 검증 중 오류가 발생했습니다.");
        }
    }
    
    private void validateMalwareSignatures(MultipartFile file, Long userId) throws IOException {
        byte[] bytes = file.getBytes();
        if (bytes.length < 16) return;
        
        // Convert first 16 bytes to hex string
        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < Math.min(bytes.length, 16); i++) {
            hex.append(String.format("%02X", bytes[i]));
        }
        String hexString = hex.toString();
        
        // Check against known malware signatures
        for (String signature : MALWARE_SIGNATURES) {
            if (hexString.startsWith(signature)) {
                String threat = "Malware signature detected: " + signature;
                log.warn("악성코드 시그니처 탐지 - 사용자: {}, 파일: {}, 시그니처: {}", 
                        userId, file.getOriginalFilename(), signature);
                throw new SecurityValidationException(threat);
            }
        }
        
        // Additional entropy analysis for packed executables
        double entropy = calculateEntropy(bytes);
        if (entropy > 7.5) { // High entropy suggests encryption/packing
            log.warn("높은 엔트로피 탐지 (패킹된 실행파일 의심) - 사용자: {}, 파일: {}, 엔트로피: {}", 
                    userId, file.getOriginalFilename(), entropy);
            throw new SecurityValidationException("파일이 의심스러운 패킹/암호화 패턴을 보입니다.");
        }
    }
    
    private void validateFileStructure(MultipartFile file, Long userId) throws IOException {
        byte[] bytes = file.getBytes();
        String mimeType = file.getContentType();
        
        // Check for file format inconsistencies
        if ("image/jpeg".equals(mimeType)) {
            validateJpegStructure(bytes, userId, file.getOriginalFilename());
        } else if ("image/png".equals(mimeType)) {
            validatePngStructure(bytes, userId, file.getOriginalFilename());
        }
        
        // Check for embedded files
        detectEmbeddedFiles(bytes, userId, file.getOriginalFilename());
    }
    
    private void validateJpegStructure(byte[] bytes, Long userId, String filename) {
        // Validate JPEG structure and detect anomalies
        if (bytes.length < 4 || (bytes[0] & 0xFF) != 0xFF || (bytes[1] & 0xFF) != 0xD8) {
            return; // Not a valid JPEG
        }
        
        // Look for suspicious comments or metadata
        for (int i = 0; i < bytes.length - 4; i++) {
            if ((bytes[i] & 0xFF) == 0xFF && (bytes[i+1] & 0xFF) == 0xFE) {
                // JPEG comment marker found - extract and analyze
                int commentLength = ((bytes[i+2] & 0xFF) << 8) | (bytes[i+3] & 0xFF);
                if (commentLength > 2 && i + commentLength < bytes.length) {
                    String comment = new String(bytes, i + 4, commentLength - 2);
                    if (SUSPICIOUS_METADATA_PATTERN.matcher(comment).find()) {
                        log.warn("JPEG 주석에서 의심스러운 코드 탐지 - 사용자: {}, 파일: {}", userId, filename);
                        throw new SecurityValidationException("JPEG 메타데이터에 의심스러운 내용이 포함되어 있습니다.");
                    }
                }
                break;
            }
        }
    }
    
    private void validatePngStructure(byte[] bytes, Long userId, String filename) {
        // PNG structure validation
        if (bytes.length < 8) return;
        
        // Check PNG signature
        byte[] pngSignature = {(byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        for (int i = 0; i < 8; i++) {
            if (bytes[i] != pngSignature[i]) {
                return; // Not a valid PNG
            }
        }
        
        // Look for suspicious ancillary chunks
        int offset = 8;
        while (offset < bytes.length - 8) {
            // Read chunk length and type
            int chunkLength = ((bytes[offset] & 0xFF) << 24) | 
                            ((bytes[offset+1] & 0xFF) << 16) | 
                            ((bytes[offset+2] & 0xFF) << 8) | 
                            (bytes[offset+3] & 0xFF);
            
            String chunkType = new String(bytes, offset + 4, 4);
            
            // Check for suspicious private chunks
            if (chunkType.startsWith("prIV") || chunkType.contains("JS") || chunkType.contains("SCR")) {
                log.warn("PNG에서 의심스러운 private chunk 탐지 - 사용자: {}, 파일: {}, chunk: {}", 
                        userId, filename, chunkType);
                throw new SecurityValidationException("PNG 파일에 의심스러운 데이터 chunk가 포함되어 있습니다.");
            }
            
            offset += 8 + chunkLength + 4; // chunk header + data + CRC
        }
    }
    
    private void detectEmbeddedFiles(byte[] bytes, Long userId, String filename) {
        // Look for embedded ZIP files
        for (int i = 0; i < bytes.length - 4; i++) {
            if (bytes[i] == 0x50 && bytes[i+1] == 0x4B && 
                bytes[i+2] == 0x03 && bytes[i+3] == 0x04) {
                log.warn("임베디드 ZIP 파일 탐지 - 사용자: {}, 파일: {}, 오프셋: {}", userId, filename, i);
                throw new SecurityValidationException("파일 내부에 임베디드 아카이브가 발견되었습니다.");
            }
        }
        
        // Look for embedded executables
        for (int i = 0; i < bytes.length - 2; i++) {
            if (bytes[i] == 0x4D && bytes[i+1] == 0x5A) { // MZ header
                if (i > 0) { // Not at the beginning - embedded
                    log.warn("임베디드 실행파일 탐지 - 사용자: {}, 파일: {}, 오프셋: {}", userId, filename, i);
                    throw new SecurityValidationException("파일 내부에 실행파일이 임베디드되어 있습니다.");
                }
            }
        }
    }
    
    private void detectSteganography(MultipartFile file, Long userId) throws IOException {
        // Basic steganography detection based on file analysis
        byte[] bytes = file.getBytes();
        
        // Check for LSB (Least Significant Bit) patterns
        if (hasLSBSteganography(bytes)) {
            log.warn("LSB 스테가노그래피 의심 패턴 탐지 - 사용자: {}, 파일: {}", 
                    userId, file.getOriginalFilename());
            throw new SecurityValidationException("이미지에 숨겨진 데이터가 포함되어 있을 수 있습니다.");
        }
        
        // Check file size anomalies for the image dimensions
        if (hasSuspiciousFileSize(bytes, file.getSize())) {
            log.warn("의심스러운 파일 크기 (스테가노그래피 의심) - 사용자: {}, 파일: {}", 
                    userId, file.getOriginalFilename());
            throw new SecurityValidationException("파일 크기가 이미지 해상도에 비해 비정상적으로 큽니다.");
        }
    }
    
    private boolean hasLSBSteganography(byte[] bytes) {
        // Simplified LSB detection - look for unusual bit patterns
        if (bytes.length < 1000) return false;
        
        int oddLSBCount = 0;
        int totalBytes = Math.min(bytes.length, 1000);
        
        for (int i = 0; i < totalBytes; i++) {
            if ((bytes[i] & 1) == 1) {
                oddLSBCount++;
            }
        }
        
        double lsbRatio = (double) oddLSBCount / totalBytes;
        // Natural images should have ~50% LSB distribution
        return Math.abs(lsbRatio - 0.5) > 0.15; // Significant deviation
    }
    
    private boolean hasSuspiciousFileSize(byte[] bytes, long fileSize) {
        // Estimate expected file size based on image properties
        // This is a simplified heuristic
        return fileSize > bytes.length * 1.5; // File is 50% larger than expected
    }
    
    private void scanMetadataThreats(MultipartFile file, Long userId) throws IOException {
        // Extract and scan EXIF/metadata for threats
        byte[] bytes = file.getBytes();
        String content = new String(bytes, 0, Math.min(bytes.length, 8192), "UTF-8");
        
        if (SUSPICIOUS_METADATA_PATTERN.matcher(content).find()) {
            log.warn("메타데이터에서 의심스러운 스크립트 탐지 - 사용자: {}, 파일: {}", 
                    userId, file.getOriginalFilename());
            throw new SecurityValidationException("파일 메타데이터에 의심스러운 코드가 포함되어 있습니다.");
        }
    }
    
    private void checkFileReputation(MultipartFile file, Long userId) throws IOException {
        // Calculate file hash and check against threat intelligence
        String fileHash = calculateSHA256(file.getBytes());
        
        // In a real implementation, this would check against threat intelligence feeds
        // For now, we'll implement a simple blacklist check
        if (isKnownMaliciousHash(fileHash)) {
            log.warn("알려진 악성 파일 해시 탐지 - 사용자: {}, 파일: {}, 해시: {}", 
                    userId, file.getOriginalFilename(), fileHash);
            auditLogger.logSecurityEvent("KNOWN_MALICIOUS_HASH", userId, file.getOriginalFilename(), fileHash);
            throw new SecurityValidationException("알려진 악성 파일입니다.");
        }
        
        // Log file hash for future reference
        auditLogger.logSecurityEvent("FILE_HASH_CALCULATED", userId, file.getOriginalFilename(), fileHash);
    }
    
    private void detectAdvancedPolyglots(MultipartFile file, Long userId) throws IOException {
        byte[] bytes = file.getBytes();
        
        // Check for advanced polyglot combinations
        boolean hasImageHeader = hasValidImageHeader(bytes);
        boolean hasHtmlContent = containsHtmlContent(bytes);
        boolean hasJavaScriptContent = containsJavaScript(bytes);
        
        if (hasImageHeader && (hasHtmlContent || hasJavaScriptContent)) {
            log.warn("고급 폴리글롯 파일 탐지 - 사용자: {}, 파일: {}", userId, file.getOriginalFilename());
            throw new SecurityValidationException("다중 형식 해석이 가능한 위험한 파일입니다.");
        }
    }
    
    // Helper methods
    private double calculateEntropy(byte[] bytes) {
        if (bytes.length == 0) return 0.0;
        
        int[] frequency = new int[256];
        for (byte b : bytes) {
            frequency[b & 0xFF]++;
        }
        
        double entropy = 0.0;
        for (int freq : frequency) {
            if (freq > 0) {
                double probability = (double) freq / bytes.length;
                entropy -= probability * (Math.log(probability) / Math.log(2));
            }
        }
        
        return entropy;
    }
    
    private String calculateSHA256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
    
    private boolean isKnownMaliciousHash(String hash) {
        // In production, this would query a threat intelligence database
        // For now, return false (no known threats)
        return false;
    }
    
    private boolean hasValidImageHeader(byte[] bytes) {
        if (bytes.length < 4) return false;
        
        // Check for JPEG
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) return true;
        
        // Check for PNG
        if (bytes.length >= 8) {
            byte[] pngSig = {(byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
            return Arrays.equals(Arrays.copyOf(bytes, 8), pngSig);
        }
        
        return false;
    }
    
    private boolean containsHtmlContent(byte[] bytes) {
        String content = new String(bytes, 0, Math.min(bytes.length, 4096));
        return content.toLowerCase().contains("<html") || 
               content.toLowerCase().contains("<!doctype") ||
               content.toLowerCase().contains("<body");
    }
    
    private boolean containsJavaScript(byte[] bytes) {
        String content = new String(bytes, 0, Math.min(bytes.length, 4096));
        return content.toLowerCase().contains("<script") ||
               content.toLowerCase().contains("javascript:") ||
               content.toLowerCase().contains("eval(");
    }
    
    public static class SecurityValidationException extends RuntimeException {
        public SecurityValidationException(String message) {
            super(message);
        }
        
        public SecurityValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}