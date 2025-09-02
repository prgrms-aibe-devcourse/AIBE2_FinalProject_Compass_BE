package com.compass.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 실제 Gemini API 통합 테스트
 * 
 * 이 테스트는 실제 Google Cloud 인증이 필요합니다.
 * 환경 변수가 설정된 경우에만 실행됩니다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class RealApiIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("Gemini API 실제 호출 테스트 - 간단한 질문")
    @EnabledIfEnvironmentVariable(named = "GOOGLE_CLOUD_PROJECT_ID", matches = ".+")
    public void testGeminiApiCall_SimpleQuestion() {
        // Given
        Map<String, String> request = new HashMap<>();
        request.put("prompt", "한국의 수도는 어디인가요? 한 단어로 답하세요.");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

        // When
        ResponseEntity<Map> response = restTemplate.exchange(
            "/api/test/gemini",
            HttpMethod.POST,
            entity,
            Map.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("success", response.getBody().get("status"));
        assertNotNull(response.getBody().get("response"));
        
        String geminiResponse = (String) response.getBody().get("response");
        assertTrue(geminiResponse.toLowerCase().contains("서울") || 
                  geminiResponse.toLowerCase().contains("seoul"),
                  "응답에 '서울' 또는 'Seoul'이 포함되어야 합니다: " + geminiResponse);
    }

    @Test
    @DisplayName("Gemini API 실제 호출 테스트 - 여행 관련 질문")
    @EnabledIfEnvironmentVariable(named = "GOOGLE_CLOUD_PROJECT_ID", matches = ".+")
    public void testGeminiApiCall_TravelQuestion() {
        // Given
        Map<String, String> request = new HashMap<>();
        request.put("prompt", "제주도 여행 3일 일정을 간단히 추천해주세요. 3줄 이내로 답변해주세요.");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

        // When
        ResponseEntity<Map> response = restTemplate.exchange(
            "/api/test/gemini",
            HttpMethod.POST,
            entity,
            Map.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("success", response.getBody().get("status"));
        assertNotNull(response.getBody().get("response"));
        
        String geminiResponse = (String) response.getBody().get("response");
        assertTrue(geminiResponse.length() > 10, "응답이 너무 짧습니다: " + geminiResponse);
        assertTrue(geminiResponse.contains("제주") || geminiResponse.contains("1일") || 
                  geminiResponse.contains("여행") || geminiResponse.contains("일정"),
                  "여행 관련 내용이 포함되어야 합니다: " + geminiResponse);
    }

    @Test
    @DisplayName("설정 확인 테스트")
    public void testConfigEndpoint() {
        // When
        ResponseEntity<Map> response = restTemplate.getForEntity(
            "/api/test/config",
            Map.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        // Google Cloud 설정이 있는 경우 확인
        if (System.getenv("GOOGLE_CLOUD_PROJECT_ID") != null) {
            assertTrue((Boolean) response.getBody().get("vertex_ai_configured"));
            assertNotNull(response.getBody().get("gcp_project_id"));
        }
    }

    @Test
    @DisplayName("Gemini API 에러 처리 테스트 - 환경 변수 없는 경우")
    @EnabledIfEnvironmentVariable(named = "SKIP_REAL_API_TESTS", matches = "false", disabledReason = "실제 API 테스트 스킵")
    public void testGeminiApiCall_NoCredentials() {
        // Given
        Map<String, String> request = new HashMap<>();
        request.put("prompt", "테스트 메시지");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

        // When - 환경 변수가 없는 경우 실행
        if (System.getenv("GOOGLE_CLOUD_PROJECT_ID") == null) {
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/test/gemini",
                HttpMethod.POST,
                entity,
                Map.class
            );

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("failed", response.getBody().get("status"));
            assertNotNull(response.getBody().get("error"));
        }
    }
}