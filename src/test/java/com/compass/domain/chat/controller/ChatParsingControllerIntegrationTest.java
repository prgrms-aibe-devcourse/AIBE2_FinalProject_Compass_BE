package com.compass.domain.chat.controller;

import com.compass.domain.chat.dto.TripPlanningRequest;
import com.compass.domain.chat.dto.TripPlanningResponse;
import com.compass.domain.chat.parser.core.TripPlanningParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
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

@WebMvcTest(controllers = ChatParsingController.class, 
    excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration.class
    })
@ActiveProfiles("test")
@DisplayName("ChatParsingController 통합 테스트")
@Disabled("Spring Context 로드 문제로 비활성화 - Unit Test로 대체")
class ChatParsingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TripPlanningParser tripPlanningParser;

    private TripPlanningRequest mockParsedRequest;

    @BeforeEach
    void setUp() {
        // Setup mock parsed request
        mockParsedRequest = new TripPlanningRequest();
        mockParsedRequest.setDestination("제주도");
        mockParsedRequest.setOrigin("서울");
        mockParsedRequest.setStartDate(LocalDate.now().plusDays(7));
        mockParsedRequest.setEndDate(LocalDate.now().plusDays(10));
        mockParsedRequest.setNumberOfTravelers(2);
        mockParsedRequest.setTravelStyle("moderate");
        mockParsedRequest.setBudgetPerPerson(1000000);
        mockParsedRequest.setCurrency("KRW");
    }

    @Test
    @DisplayName("자연어 파싱 API 테스트 - 정상 요청")
    void testParseChatInput_Success() throws Exception {
        // Given
        String userInput = "다음주에 제주도 여행 가고 싶어요. 예산은 100만원입니다.";
        ChatParsingController.ParseRequest request = new ChatParsingController.ParseRequest(userInput);
        
        when(tripPlanningParser.parse(anyString())).thenReturn(mockParsedRequest);

        // When & Then
        mockMvc.perform(post("/api/chat/parse")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planId").exists())
                .andExpect(jsonPath("$.planId").value(startsWith("CHAT_")))
                .andExpect(jsonPath("$.destination").value("제주도"))
                .andExpect(jsonPath("$.origin").value("서울"))
                .andExpect(jsonPath("$.numberOfTravelers").value(2))
                .andExpect(jsonPath("$.travelStyle").value("moderate"))
                .andExpect(jsonPath("$.budget.perPerson").value(1000000))
                .andExpect(jsonPath("$.budget.currency").value("KRW"))
                .andExpect(jsonPath("$.metadata.chatDomain").value(true));
    }

    @Test
    @DisplayName("Raw 파싱 API 테스트")
    void testParseRawRequest_Success() throws Exception {
        // Given
        String userInput = "3박4일로 부산 여행 계획중이에요.";
        ChatParsingController.ParseRequest request = new ChatParsingController.ParseRequest(userInput);
        
        TripPlanningRequest mockRawResponse = new TripPlanningRequest();
        mockRawResponse.setDestination("부산");
        mockRawResponse.setOrigin("서울");
        mockRawResponse.setStartDate(LocalDate.now().plusDays(7));
        mockRawResponse.setEndDate(LocalDate.now().plusDays(10));
        mockRawResponse.setNumberOfTravelers(1);
        
        when(tripPlanningParser.parse(anyString())).thenReturn(mockRawResponse);

        // When & Then
        mockMvc.perform(post("/api/chat/parse/raw")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.destination").value("부산"))
                .andExpect(jsonPath("$.origin").value("서울"))
                .andExpect(jsonPath("$.numberOfTravelers").value(1));
    }

    @Test
    @DisplayName("빈 텍스트 요청 시 검증 실패")
    void testParseChatInput_ValidationError_EmptyText() throws Exception {
        // Given
        ChatParsingController.ParseRequest request = new ChatParsingController.ParseRequest("");

        // When & Then
        mockMvc.perform(post("/api/chat/parse")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("너무 짧은 텍스트 요청 시 검증 실패")
    void testParseChatInput_ValidationError_TooShort() throws Exception {
        // Given
        ChatParsingController.ParseRequest request = new ChatParsingController.ParseRequest("여행");

        // When & Then
        mockMvc.perform(post("/api/chat/parse")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("너무 긴 텍스트 요청 시 검증 실패")
    void testParseChatInput_ValidationError_TooLong() throws Exception {
        // Given
        String longText = "a".repeat(1001);
        ChatParsingController.ParseRequest request = new ChatParsingController.ParseRequest(longText);

        // When & Then
        mockMvc.perform(post("/api/chat/parse")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("파싱 예제 조회 API 테스트")
    void testGetParsingExamples() throws Exception {
        // Given
        when(tripPlanningParser.parse(anyString())).thenReturn(mockParsedRequest);

        // When & Then
        mockMvc.perform(get("/api/chat/parse/examples"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isMap())
                .andExpect(jsonPath("$").isNotEmpty());
    }

    @Test
    @DisplayName("서비스 예외 발생 시 500 에러")
    void testParseChatInput_ServiceException() throws Exception {
        // Given
        String userInput = "제주도 여행 가고 싶어요";
        ChatParsingController.ParseRequest request = new ChatParsingController.ParseRequest(userInput);
        
        when(tripPlanningParser.parse(anyString()))
            .thenThrow(new RuntimeException("Service error"));

        // When & Then
        mockMvc.perform(post("/api/chat/parse")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }
}