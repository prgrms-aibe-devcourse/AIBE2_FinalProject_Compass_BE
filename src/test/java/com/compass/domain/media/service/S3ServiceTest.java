package com.compass.domain.media.service;

import com.compass.domain.media.exception.S3UploadException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    @Mock
    private S3Client s3Client;
    
    private S3Service s3Service;
    
    @BeforeEach
    void setUp() {
        s3Service = new S3Service(s3Client);
        ReflectionTestUtils.setField(s3Service, "bucketName", "test-bucket");
        ReflectionTestUtils.setField(s3Service, "s3BaseUrl", "https://test-bucket.s3.ap-northeast-2.amazonaws.com");
        ReflectionTestUtils.setField(s3Service, "region", "ap-northeast-2");
    }
    
    @Test
    @DisplayName("S3 파일 업로드 성공")
    void uploadFile_Success() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.jpg", 
            "image/jpeg",
            "test content".getBytes()
        );
        String userId = "testuser";
        String storedFilename = "20241204_12345678.jpg";
        
        when(s3Client.putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class)))
            .thenReturn(null); // S3 putObject는 성공 시 PutObjectResponse 반환
        
        // When
        String s3Url = s3Service.uploadFile(file, userId, storedFilename);
        
        // Then
        assertThat(s3Url).isNotNull();
        assertThat(s3Url).contains("test-bucket.s3.ap-northeast-2.amazonaws.com");
        assertThat(s3Url).contains(userId);
        assertThat(s3Url).contains(storedFilename);
        
        verify(s3Client).putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class));
    }
    
    @Test
    @DisplayName("S3 파일 업로드 실패 - S3Exception")
    void uploadFile_S3Exception() {
        // Given
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.jpg",
            "image/jpeg", 
            "test content".getBytes()
        );
        String userId = "testuser";
        String storedFilename = "20241204_12345678.jpg";
        
        when(s3Client.putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class)))
            .thenThrow(S3Exception.builder().message("S3 업로드 실패").build());
        
        // When & Then
        assertThatThrownBy(() -> s3Service.uploadFile(file, userId, storedFilename))
            .isInstanceOf(S3UploadException.class)
            .hasMessageContaining("S3 업로드 중 오류가 발생했습니다");
        
        verify(s3Client).putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class));
    }
    
    @Test
    @DisplayName("S3 파일 삭제 성공")
    void deleteFile_Success() {
        // Given
        String s3Url = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/media/testuser/2024/12/04/test.jpg";
        
        when(s3Client.deleteObject(any(software.amazon.awssdk.services.s3.model.DeleteObjectRequest.class)))
            .thenReturn(null);
        
        // When & Then
        assertThatCode(() -> s3Service.deleteFile(s3Url))
            .doesNotThrowAnyException();
        
        verify(s3Client).deleteObject(any(software.amazon.awssdk.services.s3.model.DeleteObjectRequest.class));
    }
    
    @Test
    @DisplayName("S3 파일 삭제 실패 - 잘못된 URL")
    void deleteFile_InvalidUrl() {
        // Given
        String invalidS3Url = "https://invalid-bucket.s3.amazonaws.com/test.jpg";
        
        // When & Then
        assertThatThrownBy(() -> s3Service.deleteFile(invalidS3Url))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("유효하지 않은 S3 URL입니다");
        
        verify(s3Client, never()).deleteObject(any(software.amazon.awssdk.services.s3.model.DeleteObjectRequest.class));
    }
    
    @Test
    @DisplayName("S3 버킷 존재 확인 - 존재함")
    void bucketExists_True() {
        // Given
        when(s3Client.headBucket(any(software.amazon.awssdk.services.s3.model.HeadBucketRequest.class)))
            .thenReturn(null);
        
        // When
        boolean exists = s3Service.bucketExists();
        
        // Then
        assertThat(exists).isTrue();
        verify(s3Client).headBucket(any(software.amazon.awssdk.services.s3.model.HeadBucketRequest.class));
    }
    
    @Test
    @DisplayName("S3 버킷 존재 확인 - 존재하지 않음")
    void bucketExists_False() {
        // Given
        when(s3Client.headBucket(any(software.amazon.awssdk.services.s3.model.HeadBucketRequest.class)))
            .thenThrow(software.amazon.awssdk.services.s3.model.NoSuchBucketException.builder()
                    .message("버킷이 존재하지 않음").build());
        
        // When
        boolean exists = s3Service.bucketExists();
        
        // Then
        assertThat(exists).isFalse();
        verify(s3Client).headBucket(any(software.amazon.awssdk.services.s3.model.HeadBucketRequest.class));
    }
}