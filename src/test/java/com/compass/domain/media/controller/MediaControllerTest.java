package com.compass.domain.media.controller;

import com.compass.domain.media.dto.MediaUploadResponse;
import com.compass.domain.media.entity.FileStatus;
import com.compass.domain.media.exception.FileValidationException;
import com.compass.domain.media.service.MediaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MediaController.class)
class MediaControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private MediaService mediaService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("파일 업로드 성공")
    void uploadFile_Success() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.jpg",
            "image/jpeg",
            "test image content".getBytes()
        );
        
        MediaUploadResponse mockResponse = MediaUploadResponse.builder()
            .id(1L)
            .originalFilename("test.jpg")
            .storedFilename("20231201_12345678.jpg")
            .s3Url(null)
            .fileSize(18L)
            .mimeType("image/jpeg")
            .status(FileStatus.UPLOADED)
            .metadata(new HashMap<>())
            .createdAt(LocalDateTime.now())
            .build();
        
        when(mediaService.uploadFile(any(), eq("testuser"))).thenReturn(mockResponse);
        
        // When & Then
        mockMvc.perform(multipart("/api/media/upload")
                .file(file)
                .with(csrf()))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpected(jsonPath("$.originalFilename").value("test.jpg"))
            .andExpect(jsonPath("$.mimeType").value("image/jpeg"))
            .andExpected(jsonPath("$.status").value("UPLOADED"));
    }
    
    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("파일 업로드 실패 - 파일 검증 오류")
    void uploadFile_ValidationError() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.pdf",
            "application/pdf",
            "invalid file content".getBytes()
        );
        
        when(mediaService.uploadFile(any(), eq("testuser")))
            .thenThrow(new FileValidationException("허용되지 않는 파일 형식입니다."));
        
        // When & Then
        mockMvc.perform(multipart("/api/media/upload")
                .file(file)
                .with(csrf()))
            .andDo(print())
            .andExpected(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("File Validation Error"))
            .andExpected(jsonPath("$.message").value("허용되지 않는 파일 형식입니다."));
    }
    
    @Test
    @DisplayName("인증되지 않은 사용자 - 401 Unauthorized")
    void uploadFile_Unauthorized() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.jpg",
            "image/jpeg",
            "test image content".getBytes()
        );
        
        // When & Then
        mockMvc.perform(multipart("/api/media/upload")
                .file(file)
                .with(csrf()))
            .andDo(print())
            .andExpected(status().isUnauthorized());
    }
    
    @Test
    @DisplayName("Health Check 성공")
    void healthCheck_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/media/health"))
            .andDo(print())
            .andExpected(status().isOk())
            .andExpected(content().string("Media service is running"));
    }
}