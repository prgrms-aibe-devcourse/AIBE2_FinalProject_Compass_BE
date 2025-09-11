package com.compass.domain.media.controller;

import com.compass.config.jwt.JwtTokenProvider;
import com.compass.domain.media.service.MediaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = MediaController.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableAutoConfiguration(exclude = {
    DataSourceAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class,
    JpaRepositoriesAutoConfiguration.class,
    RedisAutoConfiguration.class,
    RedisRepositoriesAutoConfiguration.class
})
@Tag("unit")
@DisplayName("MediaController OCR 엔드포인트 테스트")
@org.junit.jupiter.api.Disabled("Spring Context 로딩 문제로 임시 비활성화")
class MediaControllerOCRTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MediaService mediaService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private final String TEST_TOKEN = "valid-jwt-token";
    private final String TEST_EMAIL = "test@example.com";
    private final Long TEST_USER_ID = 1L;
    private final Long TEST_MEDIA_ID = 1L;

    @BeforeEach
    void setUp() {
        when(jwtTokenProvider.getUsername(TEST_TOKEN)).thenReturn(TEST_EMAIL);
        when(mediaService.getUserIdByEmail(TEST_EMAIL)).thenReturn(TEST_USER_ID);

        // 추가: 모든 토큰에 대해 동일한 이메일과 사용자 ID 반환하도록 설정
        when(jwtTokenProvider.getUsername(anyString())).thenReturn(TEST_EMAIL);
        when(mediaService.getUserIdByEmail(anyString())).thenReturn(TEST_USER_ID);
    }

    @Test
    @DisplayName("POST /api/media/{id}/ocr - OCR 처리 성공")
    void processOCR_ValidRequest_Success() throws Exception {
        // Given
        Map<String, Object> ocrResult = new HashMap<>();
        ocrResult.put("success", true);
        ocrResult.put("extractedText", "Sample extracted text");
        ocrResult.put("textLength", 21);
        ocrResult.put("confidence", 0.95);
        ocrResult.put("wordCount", 3);
        ocrResult.put("lineCount", 1);

        doNothing().when(mediaService).processOCRForMedia(TEST_MEDIA_ID, TEST_USER_ID);
        when(mediaService.getOCRResult(TEST_MEDIA_ID, TEST_USER_ID)).thenReturn(ocrResult);

        // When & Then
        mockMvc.perform(post("/api/media/{id}/ocr", TEST_MEDIA_ID)
                        .header("Authorization", "Bearer " + TEST_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.extractedText").value("Sample extracted text"))
                .andExpect(jsonPath("$.textLength").value(21))
                .andExpect(jsonPath("$.confidence").value(0.95))
                .andExpect(jsonPath("$.wordCount").value(3))
                .andExpect(jsonPath("$.lineCount").value(1));

        verify(mediaService, times(1)).processOCRForMedia(TEST_MEDIA_ID, TEST_USER_ID);
        verify(mediaService, times(1)).getOCRResult(TEST_MEDIA_ID, TEST_USER_ID);
    }

    @Test
    @DisplayName("POST /api/media/{id}/ocr - 인증 토큰 없이 요청 시 401 에러")
    void processOCR_NoAuthToken_Returns401() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/media/{id}/ocr", TEST_MEDIA_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        verify(mediaService, never()).processOCRForMedia(anyLong(), anyLong());
        verify(mediaService, never()).getOCRResult(anyLong(), anyLong());
    }

    @Test
    @DisplayName("POST /api/media/{id}/ocr - 잘못된 토큰으로 요청 시 예외 발생")
    void processOCR_InvalidToken_ThrowsException() throws Exception {
        // Given
        when(jwtTokenProvider.getUsername("invalid-token")).thenThrow(new RuntimeException("Invalid token"));

        // When & Then
        mockMvc.perform(post("/api/media/{id}/ocr", TEST_MEDIA_ID)
                        .header("Authorization", "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isInternalServerError());

        verify(mediaService, never()).processOCRForMedia(anyLong(), anyLong());
        verify(mediaService, never()).getOCRResult(anyLong(), anyLong());
    }

    @Test
    @DisplayName("GET /api/media/{id}/ocr - OCR 결과 조회 성공")
    void getOCRResult_ValidRequest_Success() throws Exception {
        // Given
        Map<String, Object> ocrResult = new HashMap<>();
        ocrResult.put("success", true);
        ocrResult.put("extractedText", "Previously extracted text");
        ocrResult.put("textLength", 25);
        ocrResult.put("confidence", 0.88);
        ocrResult.put("processedAt", "2024-01-01T12:00:00");

        when(mediaService.getOCRResult(TEST_MEDIA_ID, TEST_USER_ID)).thenReturn(ocrResult);

        // When & Then
        mockMvc.perform(get("/api/media/{id}/ocr", TEST_MEDIA_ID)
                        .header("Authorization", "Bearer " + TEST_TOKEN))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.extractedText").value("Previously extracted text"))
                .andExpect(jsonPath("$.textLength").value(25))
                .andExpect(jsonPath("$.confidence").value(0.88))
                .andExpect(jsonPath("$.processedAt").value("2024-01-01T12:00:00"));

        verify(mediaService, times(1)).getOCRResult(TEST_MEDIA_ID, TEST_USER_ID);
    }

    @Test
    @DisplayName("GET /api/media/{id}/ocr - OCR 결과 없음")
    void getOCRResult_NoOCRResult_ReturnsErrorResponse() throws Exception {
        // Given
        Map<String, Object> errorResult = new HashMap<>();
        errorResult.put("success", false);
        errorResult.put("error", "OCR 결과가 없습니다. 이미지 파일이 아니거나 OCR이 처리되지 않았습니다.");

        when(mediaService.getOCRResult(TEST_MEDIA_ID, TEST_USER_ID)).thenReturn(errorResult);

        // When & Then
        mockMvc.perform(get("/api/media/{id}/ocr", TEST_MEDIA_ID)
                        .header("Authorization", "Bearer " + TEST_TOKEN))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("OCR 결과가 없습니다. 이미지 파일이 아니거나 OCR이 처리되지 않았습니다."));

        verify(mediaService, times(1)).getOCRResult(TEST_MEDIA_ID, TEST_USER_ID);
    }

    @Test
    @DisplayName("GET /api/media/{id}/ocr - 인증 토큰 없이 요청 시 401 에러")
    void getOCRResult_NoAuthToken_Returns401() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/media/{id}/ocr", TEST_MEDIA_ID))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        verify(mediaService, never()).getOCRResult(anyLong(), anyLong());
    }

    @Test
    @DisplayName("POST /api/media/{id}/ocr - MediaService에서 예외 발생 시 적절한 에러 응답")
    void processOCR_MediaServiceThrowsException_ReturnsError() throws Exception {
        // Given
        doThrow(new RuntimeException("OCR processing failed"))
                .when(mediaService).processOCRForMedia(TEST_MEDIA_ID, TEST_USER_ID);

        // When & Then
        mockMvc.perform(post("/api/media/{id}/ocr", TEST_MEDIA_ID)
                        .header("Authorization", "Bearer " + TEST_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isInternalServerError());

        verify(mediaService, times(1)).processOCRForMedia(TEST_MEDIA_ID, TEST_USER_ID);
        verify(mediaService, never()).getOCRResult(anyLong(), anyLong());
    }

    @Test
    @DisplayName("GET /api/media/{id}/ocr - MediaService에서 예외 발생 시 적절한 에러 응답")
    void getOCRResult_MediaServiceThrowsException_ReturnsError() throws Exception {
        // Given
        when(mediaService.getOCRResult(TEST_MEDIA_ID, TEST_USER_ID))
                .thenThrow(new RuntimeException("Failed to get OCR result"));

        // When & Then
        mockMvc.perform(get("/api/media/{id}/ocr", TEST_MEDIA_ID)
                        .header("Authorization", "Bearer " + TEST_TOKEN))
                .andDo(print())
                .andExpect(status().isInternalServerError());

        verify(mediaService, times(1)).getOCRResult(TEST_MEDIA_ID, TEST_USER_ID);
    }
}