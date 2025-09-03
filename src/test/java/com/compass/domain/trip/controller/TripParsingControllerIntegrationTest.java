package com.compass.domain.trip.controller;

import com.compass.domain.chat.dto.TripPlanningRequest;
import com.compass.domain.chat.dto.TripPlanningResponse;
import com.compass.domain.trip.parser.TripRequestParser;
import com.compass.domain.trip.service.TripPlanningService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.ai.openai.api-key=test-key",
    "spring.ai.vertex-ai.gemini.project-id=test-project"
})
@DisplayName("TripParsingController 통합 테스트")
class TripParsingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TripPlanningService tripPlanningService;

    private TripPlanningResponse mockResponse;

    @BeforeEach
    void setUp() {
        // Setup mock response
        mockResponse = new TripPlanningResponse();
        mockResponse.setPlanId("PLAN_123456");
        mockResponse.setDestination("제주도");
        mockResponse.setOrigin("서울");
        mockResponse.setStartDate(LocalDate.now().plusDays(7));
        mockResponse.setEndDate(LocalDate.now().plusDays(10));
        mockResponse.setNumberOfTravelers(2);
        mockResponse.setTravelStyle("moderate");
        mockResponse.setSummary("2명이서 제주도 여행");
        
        Map<String, Object> budget = new HashMap<>();
        budget.put("perPerson", 1000000);
        budget.put("total", 2000000);
        budget.put("currency", "KRW");
        mockResponse.setBudget(budget);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("parsedAt", LocalDate.now().toString());
        metadata.put("parsingMethod", "NER_PATTERN_MATCHING");
        metadata.put("confidence", 0.85);
        mockResponse.setMetadata(metadata);
    }

    @Test
    @DisplayName("자연어 파싱 API 테스트 - 정상 요청")
    void testParseTripRequest_Success() throws Exception {
        // Given
        String userInput = "다음주에 제주도 여행 가고 싶어요. 예산은 100만원입니다.";
        TripParsingController.ParseRequest request = new TripParsingController.ParseRequest(userInput);
        
        when(tripPlanningService.processTripRequest(anyString())).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(post("/api/trips/parse")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planId").value("PLAN_123456"))
                .andExpect(jsonPath("$.destination").value("제주도"))
                .andExpect(jsonPath("$.origin").value("서울"))
                .andExpect(jsonPath("$.numberOfTravelers").value(2))
                .andExpect(jsonPath("$.travelStyle").value("moderate"))
                .andExpect(jsonPath("$.budget.perPerson").value(1000000))
                .andExpect(jsonPath("$.budget.currency").value("KRW"))
                .andExpect(jsonPath("$.metadata.confidence").value(0.85));
    }

    @Test
    @DisplayName("Raw 파싱 API 테스트")
    void testParseRawRequest_Success() throws Exception {
        // Given
        String userInput = "3박4일로 부산 여행 계획중이에요.";
        TripParsingController.ParseRequest request = new TripParsingController.ParseRequest(userInput);
        
        TripPlanningRequest mockRawResponse = new TripPlanningRequest();
        mockRawResponse.setDestination("부산");
        mockRawResponse.setOrigin("서울");
        mockRawResponse.setStartDate(LocalDate.now().plusDays(7));
        mockRawResponse.setEndDate(LocalDate.now().plusDays(10));
        mockRawResponse.setNumberOfTravelers(1);
        
        when(tripPlanningService.parseInput(anyString())).thenReturn(mockRawResponse);

        // When & Then
        mockMvc.perform(post("/api/trips/parse/raw")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.destination").value("부산"))
                .andExpect(jsonPath("$.origin").value("서울"))
                .andExpect(jsonPath("$.numberOfTravelers").value(1));
    }

    @Test
    @DisplayName("빈 텍스트 요청 시 검증 실패")
    void testParseTripRequest_ValidationError_EmptyText() throws Exception {
        // Given
        TripParsingController.ParseRequest request = new TripParsingController.ParseRequest("");

        // When & Then
        mockMvc.perform(post("/api/trips/parse")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("너무 짧은 텍스트 요청 시 검증 실패")
    void testParseTripRequest_ValidationError_TooShort() throws Exception {
        // Given
        TripParsingController.ParseRequest request = new TripParsingController.ParseRequest("여행");

        // When & Then
        mockMvc.perform(post("/api/trips/parse")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("너무 긴 텍스트 요청 시 검증 실패")
    void testParseTripRequest_ValidationError_TooLong() throws Exception {
        // Given
        String longText = "a".repeat(1001);
        TripParsingController.ParseRequest request = new TripParsingController.ParseRequest(longText);

        // When & Then
        mockMvc.perform(post("/api/trips/parse")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("파싱 예제 조회 API 테스트")
    void testGetParsingExamples() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/trips/parse/examples"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isMap())
                .andExpect(jsonPath("$").isNotEmpty());
    }

    @Test
    @DisplayName("서비스 예외 발생 시 500 에러")
    void testParseTripRequest_ServiceException() throws Exception {
        // Given
        String userInput = "제주도 여행 가고 싶어요";
        TripParsingController.ParseRequest request = new TripParsingController.ParseRequest(userInput);
        
        when(tripPlanningService.processTripRequest(anyString()))
            .thenThrow(new RuntimeException("Service error"));

        // When & Then
        mockMvc.perform(post("/api/trips/parse")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }
}