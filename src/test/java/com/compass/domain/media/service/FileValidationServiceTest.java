package com.compass.domain.media.service;

import com.compass.domain.media.exception.FileValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.*;

class FileValidationServiceTest {
    
    private FileValidationService fileValidationService;
    
    @BeforeEach
    void setUp() {
        fileValidationService = new FileValidationService();
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
}