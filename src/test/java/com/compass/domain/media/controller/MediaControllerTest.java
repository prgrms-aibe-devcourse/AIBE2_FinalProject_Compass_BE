package com.compass.domain.media.controller;

import com.compass.domain.media.dto.MediaUploadResponse;
import com.compass.domain.media.entity.FileStatus;
import com.compass.domain.media.exception.FileValidationException;
import com.compass.domain.media.service.MediaService;
import com.compass.domain.media.service.S3Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.ai.openai.api-key=test-key",
    "spring.ai.vertex.ai.gemini.project-id=test-project",
    "spring.ai.vertex.ai.gemini.location=test-location"
})
class MediaControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private MediaService mediaService;

    @MockBean
    private S3Service s3Service;

    // AI 서비스 빈들 모킹 (API 키가 필요하지 않도록)
    @MockBean
    private org.springframework.ai.openai.OpenAiChatModel openAiChatModel;

    @MockBean
    private org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel vertexAiGeminiChatModel;
    
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
            .s3Url("https://compass-media-bucket.s3.ap-northeast-2.amazonaws.com/media/testuser/2023/12/01/20231201_12345678.jpg")
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
            .andExpect(jsonPath("$.originalFilename").value("test.jpg"))
            .andExpect(jsonPath("$.mimeType").value("image/jpeg"))
            .andExpect(jsonPath("$.status").value("UPLOADED"))
            .andExpect(jsonPath("$.s3Url").value("https://compass-media-bucket.s3.ap-northeast-2.amazonaws.com/media/testuser/2023/12/01/20231201_12345678.jpg"));
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
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("File Validation Error"))
            .andExpect(jsonPath("$.message").value("허용되지 않는 파일 형식입니다."));
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
                .with(csrf())
                .with(anonymous()))
            .andDo(print())
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    @DisplayName("Health Check 성공")
    void healthCheck_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/media/health"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().string("Media service is running"));
    }
}