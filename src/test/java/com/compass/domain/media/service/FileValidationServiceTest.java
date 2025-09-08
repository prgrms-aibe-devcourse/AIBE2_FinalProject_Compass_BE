package com.compass.domain.media.service;

import com.compass.domain.media.config.MediaValidationProperties;
import com.compass.domain.media.exception.FileValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class FileValidationServiceTest {
    
    private FileValidationService fileValidationService;
    private MediaValidationProperties validationProperties;
    
    @BeforeEach
    void setUp() {
        validationProperties = new MediaValidationProperties();
        fileValidationService = new FileValidationService(validationProperties);
    }
    
    @Test
    @DisplayName("유효한 JPEG 파일 검증 성공")
    void validateFile_ValidJpegFile_Success() {
        // Given
        byte[] jpegHeader = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        MockMultipartFile file = new MockMultipartFile(
            "file", 
            "test.jpg", 
            "image/jpeg", 
            jpegHeader
        );
        
        // When & Then
        assertThatNoException().isThrownBy(() -> fileValidationService.validateFile(file));
    }
    
    @Test
    @DisplayName("유효한 PNG 파일 검증 성공")
    void validateFile_ValidPngFile_Success() {
        // Given
        byte[] pngHeader = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        MockMultipartFile file = new MockMultipartFile(
            "file", 
            "test.png", 
            "image/png", 
            pngHeader
        );
        
        // When & Then
        assertThatNoException().isThrownBy(() -> fileValidationService.validateFile(file));
    }
    
    @Test
    @DisplayName("빈 파일 검증 실패")
    void validateFile_EmptyFile_ThrowsException() {
        // Given
        MockMultipartFile emptyFile = new MockMultipartFile(
            "file", 
            "empty.jpg", 
            "image/jpeg", 
            new byte[0]
        );
        
        // When & Then
        assertThatThrownBy(() -> fileValidationService.validateFile(emptyFile))
            .isInstanceOf(FileValidationException.class)
            .hasMessage("파일이 비어있습니다.");
    }
    
    @Test
    @DisplayName("파일 크기 초과 검증 실패")
    void validateFile_FileSizeExceeded_ThrowsException() {
        // Given
        byte[] largeFileContent = new byte[11 * 1024 * 1024]; // 11MB
        largeFileContent[0] = (byte) 0xFF;
        largeFileContent[1] = (byte) 0xD8;
        MockMultipartFile largeFile = new MockMultipartFile(
            "file", 
            "large.jpg", 
            "image/jpeg", 
            largeFileContent
        );
        
        // When & Then
        assertThatThrownBy(() -> fileValidationService.validateFile(largeFile))
            .isInstanceOf(FileValidationException.class)
            .hasMessageContaining("파일 크기가 허용된 최대 크기");
    }
    
    @Test
    @DisplayName("허용되지 않는 MIME 타입 검증 실패")
    void validateFile_InvalidMimeType_ThrowsException() {
        // Given
        byte[] content = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        MockMultipartFile file = new MockMultipartFile(
            "file", 
            "test.jpg", 
            "application/pdf", // 잘못된 MIME 타입
            content
        );
        
        // When & Then
        assertThatThrownBy(() -> fileValidationService.validateFile(file))
            .isInstanceOf(FileValidationException.class)
            .hasMessageContaining("허용되지 않는 파일 형식입니다");
    }
    
    @Test
    @DisplayName("허용되지 않는 파일 확장자 검증 실패")
    void validateFile_InvalidFileExtension_ThrowsException() {
        // Given
        byte[] content = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        MockMultipartFile file = new MockMultipartFile(
            "file", 
            "test.pdf", // 잘못된 확장자
            "image/jpeg", 
            content
        );
        
        // When & Then
        assertThatThrownBy(() -> fileValidationService.validateFile(file))
            .isInstanceOf(FileValidationException.class)
            .hasMessageContaining("허용되지 않는 파일 확장자입니다");
    }
    
    @Test
    @DisplayName("잘못된 파일 헤더 검증 실패")
    void validateFile_InvalidFileHeader_ThrowsException() {
        // Given
        byte[] invalidHeader = {0x00, 0x00, 0x00, 0x00}; // 유효하지 않은 헤더
        MockMultipartFile file = new MockMultipartFile(
            "file", 
            "test.jpg", 
            "image/jpeg", 
            invalidHeader
        );
        
        // When & Then
        assertThatThrownBy(() -> fileValidationService.validateFile(file))
            .isInstanceOf(FileValidationException.class)
            .hasMessageContaining("파일 헤더가 유효하지 않습니다");
    }
    
    @Test
    @DisplayName("지원되는 이미지 파일 형식 확인")
    void isSupportedImageFile_SupportedTypes_ReturnsTrue() {
        // When & Then
        assertThat(fileValidationService.isSupportedImageFile("image/jpeg")).isTrue();
        assertThat(fileValidationService.isSupportedImageFile("image/png")).isTrue();
        assertThat(fileValidationService.isSupportedImageFile("image/webp")).isTrue();
        assertThat(fileValidationService.isSupportedImageFile("image/gif")).isTrue();
    }
    
    @Test
    @DisplayName("지원되지 않는 파일 형식 확인")
    void isSupportedImageFile_UnsupportedTypes_ReturnsFalse() {
        // When & Then
        assertThat(fileValidationService.isSupportedImageFile("application/pdf")).isFalse();
        assertThat(fileValidationService.isSupportedImageFile("text/plain")).isFalse();
        assertThat(fileValidationService.isSupportedImageFile(null)).isFalse();
    }
    
    // =============== REQ-MEDIA-005 보안 강화 테스트 ===============
    
    @Test
    @DisplayName("파일명 보안 검증 - 경로 조작 차단")
    void validateFile_PathTraversalAttempt_ThrowsException() {
        // Given - 경로 조작 시도 파일명들
        String[] maliciousFilenames = {
            "../../../etc/passwd.jpg",
            "..\\..\\windows\\system32\\config.png", 
            ".././../sensitive.jpeg",
            "/etc/shadow.gif"
        };
        
        byte[] jpegHeader = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        
        for (String filename : maliciousFilenames) {
            MockMultipartFile file = new MockMultipartFile(
                "file", 
                filename, 
                "image/jpeg", 
                jpegHeader
            );
            
            // When & Then
            assertThatThrownBy(() -> fileValidationService.validateFile(file))
                .isInstanceOf(FileValidationException.class)
                .hasMessageContaining("경로 조작 패턴");
        }
    }
    
    @Test
    @DisplayName("파일명 보안 검증 - 위험한 특수문자 차단")
    void validateFile_DangerousCharacters_ThrowsException() {
        // Given - 위험한 특수문자 포함 파일명들
        String[] maliciousFilenames = {
            "test<script>.jpg",
            "file|cmd.png",
            "image*.jpeg",
            "pic?.gif",
            "photo\".webp"
        };
        
        byte[] jpegHeader = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        
        for (String filename : maliciousFilenames) {
            MockMultipartFile file = new MockMultipartFile(
                "file", 
                filename, 
                "image/jpeg", 
                jpegHeader
            );
            
            // When & Then
            assertThatThrownBy(() -> fileValidationService.validateFile(file))
                .isInstanceOf(FileValidationException.class)
                .hasMessageContaining("허용되지 않는 특수문자");
        }
    }
    
    @Test
    @DisplayName("파일명 보안 검증 - Windows 예약어 차단")
    void validateFile_WindowsReservedNames_ThrowsException() {
        // Given - Windows 예약어 파일명들
        String[] reservedNames = {
            "CON.jpg", "PRN.png", "AUX.jpeg", "NUL.gif",
            "COM1.webp", "LPT1.jpg"
        };
        
        byte[] jpegHeader = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        
        for (String filename : reservedNames) {
            MockMultipartFile file = new MockMultipartFile(
                "file", 
                filename, 
                "image/jpeg", 
                jpegHeader
            );
            
            // When & Then
            assertThatThrownBy(() -> fileValidationService.validateFile(file))
                .isInstanceOf(FileValidationException.class)
                .hasMessageContaining("예약어");
        }
    }
    
    @Test
    @DisplayName("파일명 보안 검증 - 과도한 길이 차단")
    void validateFile_ExcessivelyLongFilename_ThrowsException() {
        // Given - 255자 초과 파일명
        String longFilename = "a".repeat(300) + ".jpg";
        byte[] jpegHeader = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        
        MockMultipartFile file = new MockMultipartFile(
            "file", 
            longFilename, 
            "image/jpeg", 
            jpegHeader
        );
        
        // When & Then
        assertThatThrownBy(() -> fileValidationService.validateFile(file))
            .isInstanceOf(FileValidationException.class)
            .hasMessageContaining("파일명이 너무 깁니다");
    }
    
    @Test
    @DisplayName("악성 코드 검사 - 스크립트 태그 탐지")
    void validateFile_ScriptTagsInFile_ThrowsException() {
        // Given - 악성 스크립트가 포함된 파일
        String maliciousContent = new String(new byte[]{(byte) 0xFF, (byte) 0xD8}) + 
            "<script>alert('xss')</script><img src=x onerror=alert(1)>";
        
        MockMultipartFile file = new MockMultipartFile(
            "file", 
            "malicious.jpg", 
            "image/jpeg", 
            maliciousContent.getBytes()
        );
        
        // When & Then
        assertThatThrownBy(() -> fileValidationService.validateFile(file))
            .isInstanceOf(FileValidationException.class)
            .hasMessageContaining("위험한 스크립트 코드");
    }
    
    @Test
    @DisplayName("악성 코드 검사 - 실행 파일 시그니처 탐지 (PE)")
    void validateFile_ExecutableSignaturePE_ThrowsException() {
        // Given - PE 실행 파일 헤더 (MZ)
        byte[] peHeader = {0x4D, 0x5A, (byte) 0x90, 0x00}; // MZ header
        
        MockMultipartFile file = new MockMultipartFile(
            "file", 
            "fake.jpg", 
            "image/jpeg", 
            peHeader
        );
        
        // When & Then
        assertThatThrownBy(() -> fileValidationService.validateFile(file))
            .isInstanceOf(FileValidationException.class)
            .hasMessageContaining("악성 파일 시그니처가 탐지되었습니다");
    }
    
    @Test
    @DisplayName("악성 코드 검사 - 실행 파일 시그니처 탐지 (ELF)")
    void validateFile_ExecutableSignatureELF_ThrowsException() {
        // Given - ELF 실행 파일 헤더
        byte[] elfHeader = {0x7F, 'E', 'L', 'F'};
        
        MockMultipartFile file = new MockMultipartFile(
            "file", 
            "fake.png", 
            "image/png", 
            elfHeader
        );
        
        // When & Then
        assertThatThrownBy(() -> fileValidationService.validateFile(file))
            .isInstanceOf(FileValidationException.class)
            .hasMessageContaining("악성 파일 시그니처가 탐지되었습니다");
    }
    
    @Test
    @DisplayName("악성 코드 검사 - 폴리글롯 파일 탐지")
    void validateFile_PolygotFile_ThrowsException() {
        // Given - JPEG 헤더 + HTML 콘텐츠 (폴리글롯)
        byte[] jpegStart = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        String htmlContent = "<!DOCTYPE html><html><body>Hidden content</body></html>";
        byte[] polygotBytes = new byte[jpegStart.length + htmlContent.getBytes().length];
        System.arraycopy(jpegStart, 0, polygotBytes, 0, jpegStart.length);
        System.arraycopy(htmlContent.getBytes(), 0, polygotBytes, jpegStart.length, htmlContent.getBytes().length);
        
        MockMultipartFile file = new MockMultipartFile(
            "file", 
            "polygot.jpg", 
            "image/jpeg", 
            polygotBytes
        );
        
        // When & Then
        assertThatThrownBy(() -> fileValidationService.validateFile(file))
            .isInstanceOf(FileValidationException.class)
            .hasMessageContaining("폴리글롯");
    }
    
    @Test
    @DisplayName("이미지 메타데이터 검증 - 정상적인 이미지는 통과")
    void validateFile_NormalImageMetadata_Success() {
        // Given - 정상적인 JPEG 파일
        byte[] normalJpeg = {
            (byte) 0xFF, (byte) 0xD8, // JPEG SOI
            (byte) 0xFF, (byte) 0xE0, // APP0 marker
            0x00, 0x10, // Length
            'J', 'F', 'I', 'F', 0x00, // JFIF identifier
            0x01, 0x01, // Version
            0x01, // Units
            0x00, 0x48, 0x00, 0x48, // X/Y density
            0x00, 0x00 // Thumbnail dimensions
        };
        
        MockMultipartFile file = new MockMultipartFile(
            "file", 
            "normal.jpg", 
            "image/jpeg", 
            normalJpeg
        );
        
        // When & Then - 정상적인 이미지는 검증 통과
        assertThatNoException().isThrownBy(() -> fileValidationService.validateFile(file));
    }
    
    @Test
    @DisplayName("보안 검증 통과 - 정상적인 이미지 파일")
    void validateFile_LegitimateImageFile_Success() {
        // Given - 정상적인 JPEG 파일
        byte[] normalJpegHeader = {
            (byte) 0xFF, (byte) 0xD8, // JPEG SOI
            (byte) 0xFF, (byte) 0xE0, // APP0 marker
            0x00, 0x10, // Length
            'J', 'F', 'I', 'F', 0x00, // JFIF identifier
            0x01, 0x01, // Version
            0x01, // Units
            0x00, 0x48, 0x00, 0x48, // X/Y density
            0x00, 0x00 // Thumbnail dimensions
        };
        
        MockMultipartFile file = new MockMultipartFile(
            "file", 
            "normal_photo.jpg", 
            "image/jpeg", 
            normalJpegHeader
        );
        
        // When & Then
        assertThatNoException().isThrownBy(() -> fileValidationService.validateFile(file));
    }
}