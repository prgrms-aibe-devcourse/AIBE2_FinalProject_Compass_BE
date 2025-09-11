package com.compass.domain.media.service;

import com.compass.domain.media.dto.MediaDto;
import com.compass.domain.media.dto.MediaUploadResponse;
import com.compass.domain.media.entity.FileStatus;
import com.compass.domain.media.entity.Media;
import com.compass.domain.media.exception.FileValidationException;
import com.compass.domain.media.repository.MediaRepository;
import com.compass.domain.user.entity.User;
import com.compass.domain.user.enums.Role;
import com.compass.domain.user.repository.UserRepository;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import java.io.IOException;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("MediaService OCR 통합 단위 테스트")
class MediaServiceOCRIntegrationTest {

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileValidationService fileValidationService;

    @Mock
    private S3Service s3Service;

    @Mock
    private OCRService ocrService;

    @Mock
    private ThumbnailService thumbnailService;

    @InjectMocks
    private MediaService mediaService;

    private User testUser;
    private Media testMedia;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .nickname("Test User")
                .role(Role.USER)
                .build();
        ReflectionTestUtils.setField(testUser, "id", 1L);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("isImage", true);
        metadata.put("imageProcessed", false);

        testMedia = Media.builder()
                .user(testUser)
                .originalFilename("test-image.jpg")
                .storedFilename("20240101_12345678.jpg")
                .s3Url("https://s3.example.com/test-image.jpg")
                .fileSize(1024L)
                .mimeType("image/jpeg")
                .status(FileStatus.UPLOADED)
                .metadata(metadata)
                .build();
    }

    @Test
    @DisplayName("이미지 업로드 시 자동 OCR 처리 성공")
    void uploadFile_ImageFile_AutoOCRProcessing() throws Exception {
        // Given
        MultipartFile imageFile = new MockMultipartFile(
                "file",
                "test-image.jpg",
                "image/jpeg",
                "fake image content".getBytes()
        );

        MediaDto.UploadRequest request = MediaDto.UploadRequest.builder()
                .file(imageFile)
                .metadata(new HashMap<>())
                .build();

        Map<String, Object> ocrResult = new HashMap<>();
        ocrResult.put("success", true);
        ocrResult.put("extractedText", "Sample text from image");
        ocrResult.put("textLength", 22);
        ocrResult.put("confidence", 0.95);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(fileValidationService.isSupportedImageFile("image/jpeg")).thenReturn(true);
        when(s3Service.uploadFile(any(), anyString(), anyString())).thenReturn("https://s3.example.com/test.jpg");
        when(thumbnailService.isImageFile("image/jpeg")).thenReturn(true);
        when(thumbnailService.generateThumbnail(any())).thenReturn("thumbnail_data".getBytes());
        when(thumbnailService.generateThumbnailFilename("test-image.jpg")).thenReturn("thumb_test-image.webp");
        when(s3Service.uploadThumbnail(any(), anyString(), anyString())).thenReturn("https://s3.example.com/thumb.webp");
        when(mediaRepository.save(any(Media.class))).thenReturn(testMedia);

        // When
        MediaUploadResponse response = mediaService.uploadFile(request, 1L);

        // Then
        assertThat(response).isNotNull();
        verify(mediaRepository, times(1)).save(any(Media.class));
        verify(thumbnailService, times(1)).isImageFile("image/jpeg");
    }

    @Test
    @DisplayName("이미지 파일이 아닌 경우 OCR 처리하지 않음")
    void uploadFile_NonImageFile_NoOCRProcessing() throws Exception {
        // Given
        MultipartFile textFile = new MockMultipartFile(
                "file",
                "test-document.txt",
                "text/plain",
                "Sample text content".getBytes()
        );

        MediaDto.UploadRequest request = MediaDto.UploadRequest.builder()
                .file(textFile)
                .metadata(new HashMap<>())
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(fileValidationService.isSupportedImageFile("text/plain")).thenReturn(false);
        when(thumbnailService.isImageFile("text/plain")).thenReturn(false);
        when(s3Service.uploadFile(any(), anyString(), anyString())).thenReturn("https://s3.example.com/test.txt");
        when(mediaRepository.save(any(Media.class))).thenReturn(testMedia);

        // When
        MediaUploadResponse response = mediaService.uploadFile(request, 1L);

        // Then
        assertThat(response).isNotNull();
        verify(mediaRepository, times(1)).save(any(Media.class));
        verify(thumbnailService, times(1)).isImageFile("text/plain");
    }

    @Test
    @DisplayName("기존 미디어에 대한 OCR 처리 성공")
    void processOCRForMedia_ValidImageMedia_Success() throws Exception {
        // Given
        Long mediaId = 1L;
        Long userId = 1L;

        Map<String, Object> ocrResult = new HashMap<>();
        ocrResult.put("success", true);
        ocrResult.put("extractedText", "OCR processed text");
        ocrResult.put("textLength", 18);

        byte[] imageBytes = "fake image bytes".getBytes();

        when(mediaRepository.findById(mediaId)).thenReturn(Optional.of(testMedia));
        when(fileValidationService.isSupportedImageFile(testMedia.getMimeType())).thenReturn(true);
        when(s3Service.downloadFile(testMedia.getS3Url())).thenReturn(imageBytes);
        when(ocrService.extractTextFromBytes(imageBytes, testMedia.getOriginalFilename())).thenReturn(ocrResult);
        when(mediaRepository.save(any(Media.class))).thenReturn(testMedia);

        // When
        mediaService.processOCRForMedia(mediaId, userId);

        // Then
        try {
            verify(ocrService, times(1)).extractTextFromBytes(imageBytes, testMedia.getOriginalFilename());
        } catch (IOException e) {
            // This should never happen in test
        }
        verify(mediaRepository, times(1)).save(any(Media.class));
    }

    @Test
    @DisplayName("존재하지 않는 미디어에 대한 OCR 처리 시 예외 발생")
    void processOCRForMedia_NonExistentMedia_ThrowsException() {
        // Given
        Long mediaId = 999L;
        Long userId = 1L;

        when(mediaRepository.findById(mediaId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> mediaService.processOCRForMedia(mediaId, userId))
                .isInstanceOf(FileValidationException.class)
                .hasMessage("파일을 찾을 수 없습니다.");

        try {
            verify(ocrService, never()).extractTextFromBytes(any(), anyString());
        } catch (IOException e) {
            // This should never happen in test
        }
    }

    @Test
    @DisplayName("권한이 없는 사용자의 OCR 처리 요청 시 예외 발생")
    void processOCRForMedia_UnauthorizedUser_ThrowsException() {
        // Given
        Long mediaId = 1L;
        Long userId = 999L; // Different user ID

        when(mediaRepository.findById(mediaId)).thenReturn(Optional.of(testMedia));

        // When & Then
        assertThatThrownBy(() -> mediaService.processOCRForMedia(mediaId, userId))
                .isInstanceOf(FileValidationException.class)
                .hasMessage("파일 접근 권한이 없습니다.");

        try {
            verify(ocrService, never()).extractTextFromBytes(any(), anyString());
        } catch (IOException e) {
            // This should never happen in test
        }
    }

    @Test
    @DisplayName("이미지가 아닌 파일에 대한 OCR 처리 시 예외 발생")
    void processOCRForMedia_NonImageFile_ThrowsException() {
        // Given
        Long mediaId = 1L;
        Long userId = 1L;

        Media textMedia = Media.builder()
                .user(testUser)
                .originalFilename("document.txt")
                .storedFilename("20240101_12345678.txt")
                .s3Url("https://s3.example.com/document.txt")
                .fileSize(1024L)
                .mimeType("text/plain")
                .status(FileStatus.UPLOADED)
                .metadata(new HashMap<>())
                .build();

        when(mediaRepository.findById(mediaId)).thenReturn(Optional.of(textMedia));
        when(fileValidationService.isSupportedImageFile("text/plain")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> mediaService.processOCRForMedia(mediaId, userId))
                .isInstanceOf(FileValidationException.class)
                .hasMessage("OCR은 이미지 파일만 지원합니다.");

        try {
            verify(ocrService, never()).extractTextFromBytes(any(), anyString());
        } catch (IOException e) {
            // This should never happen in test
        }
    }

    @Test
    @DisplayName("OCR 결과 조회 성공")
    void getOCRResult_ValidMedia_ReturnsOCRResult() {
        // Given
        Long mediaId = 1L;
        Long userId = 1L;

        Map<String, Object> ocrResult = new HashMap<>();
        ocrResult.put("success", true);
        ocrResult.put("extractedText", "Sample OCR text");
        ocrResult.put("textLength", 15);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("ocr", ocrResult);

        Media mediaWithOCR = Media.builder()
                .user(testUser)
                .originalFilename("test-image.jpg")
                .storedFilename("20240101_12345678.jpg")
                .s3Url("https://s3.example.com/test-image.jpg")
                .fileSize(1024L)
                .mimeType("image/jpeg")
                .status(FileStatus.UPLOADED)
                .metadata(metadata)
                .build();

        when(mediaRepository.findById(mediaId)).thenReturn(Optional.of(mediaWithOCR));

        // When
        Map<String, Object> result = mediaService.getOCRResult(mediaId, userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.get("success")).isEqualTo(true);
        assertThat(result.get("extractedText")).isEqualTo("Sample OCR text");
        assertThat(result.get("textLength")).isEqualTo(15);
    }

    @Test
    @DisplayName("OCR 결과가 없는 미디어 조회 시 적절한 응답 반환")
    void getOCRResult_NoOCRResult_ReturnsErrorResponse() {
        // Given
        Long mediaId = 1L;
        Long userId = 1L;

        when(mediaRepository.findById(mediaId)).thenReturn(Optional.of(testMedia));

        // When
        Map<String, Object> result = mediaService.getOCRResult(mediaId, userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.get("success")).isEqualTo(false);
        assertThat(result.get("error")).isEqualTo("OCR 결과가 없습니다. 이미지 파일이 아니거나 OCR이 처리되지 않았습니다.");
    }
}