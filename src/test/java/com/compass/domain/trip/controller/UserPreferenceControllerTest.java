package com.compass.domain.trip.controller;

import com.compass.domain.trip.dto.TravelStyleItem;
import com.compass.domain.trip.dto.TravelStylePreferenceRequest;
import com.compass.domain.trip.dto.TravelStylePreferenceResponse;
import com.compass.domain.trip.service.UserPreferenceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserPreferenceController.class)
class UserPreferenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserPreferenceService userPreferenceService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("여행 스타일 설정 API - 성공")
    void setTravelStylePreferences_Success() throws Exception {
        // Given
        Long userId = 1L;
        TravelStylePreferenceRequest request = TravelStylePreferenceRequest.builder()
                .preferences(Arrays.asList(
                        TravelStyleItem.builder().travelStyle("RELAXATION").weight(new BigDecimal("0.5")).build(),
                        TravelStyleItem.builder().travelStyle("SIGHTSEEING").weight(new BigDecimal("0.3")).build(),
                        TravelStyleItem.builder().travelStyle("ACTIVITY").weight(new BigDecimal("0.2")).build()
                ))
                .build();

        TravelStylePreferenceResponse mockResponse = TravelStylePreferenceResponse.builder()
                .userId(userId)
                .preferences(Arrays.asList(
                        TravelStyleItem.builder()
                                .travelStyle("RELAXATION")
                                .weight(new BigDecimal("0.5"))
                                .description("휴양 및 힐링을 중심으로 한 여행")
                                .build(),
                        TravelStyleItem.builder()
                                .travelStyle("SIGHTSEEING")
                                .weight(new BigDecimal("0.3"))
                                .description("관광지 방문 및 문화 체험 중심 여행")
                                .build(),
                        TravelStyleItem.builder()
                                .travelStyle("ACTIVITY")
                                .weight(new BigDecimal("0.2"))
                                .description("액티비티 및 체험 중심 여행")
                                .build()
                ))
                .totalWeight(BigDecimal.ONE)
                .message("여행 스타일 선호도가 성공적으로 설정되었습니다.")
                .build();

        when(userPreferenceService.setTravelStylePreferences(eq(userId), any(TravelStylePreferenceRequest.class)))
                .thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(post("/api/users/{userId}/preferences/travel-style", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.totalWeight").value(1.0))
                .andExpect(jsonPath("$.preferences").isArray())
                .andExpect(jsonPath("$.preferences.length()").value(3))
                .andExpect(jsonPath("$.message").value("여행 스타일 선호도가 성공적으로 설정되었습니다."));
    }

    @Test
    @DisplayName("여행 스타일 조회 API - 성공")
    void getTravelStylePreferences_Success() throws Exception {
        // Given
        Long userId = 1L;
        TravelStylePreferenceResponse mockResponse = TravelStylePreferenceResponse.builder()
                .userId(userId)
                .preferences(Arrays.asList(
                        TravelStyleItem.builder()
                                .travelStyle("RELAXATION")
                                .weight(new BigDecimal("0.5"))
                                .description("휴양 및 힐링을 중심으로 한 여행")
                                .build()
                ))
                .totalWeight(new BigDecimal("0.5"))
                .build();

        when(userPreferenceService.getTravelStylePreferences(userId))
                .thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(get("/api/users/{userId}/preferences/travel-style", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.totalWeight").value(0.5))
                .andExpect(jsonPath("$.preferences").isArray())
                .andExpect(jsonPath("$.preferences.length()").value(1));
    }

    @Test
    @DisplayName("여행 스타일 조회 API - 선호도 미설정")
    void getTravelStylePreferences_NotFound() throws Exception {
        // Given
        Long userId = 999L;
        TravelStylePreferenceResponse mockResponse = TravelStylePreferenceResponse.empty(
                userId, 
                "설정된 여행 스타일 선호도가 없습니다."
        );

        when(userPreferenceService.getTravelStylePreferences(userId))
                .thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(get("/api/users/{userId}/preferences/travel-style", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.totalWeight").value(0.0))
                .andExpect(jsonPath("$.preferences").isArray())
                .andExpect(jsonPath("$.preferences.length()").value(0))
                .andExpect(jsonPath("$.message").value("설정된 여행 스타일 선호도가 없습니다."));
    }

    @Test
    @DisplayName("여행 스타일 수정 API - 성공")
    void updateTravelStylePreferences_Success() throws Exception {
        // Given
        Long userId = 1L;
        TravelStylePreferenceRequest request = TravelStylePreferenceRequest.builder()
                .preferences(Arrays.asList(
                        TravelStyleItem.builder().travelStyle("RELAXATION").weight(new BigDecimal("0.4")).build(),
                        TravelStyleItem.builder().travelStyle("SIGHTSEEING").weight(new BigDecimal("0.4")).build(),
                        TravelStyleItem.builder().travelStyle("ACTIVITY").weight(new BigDecimal("0.2")).build()
                ))
                .build();

        TravelStylePreferenceResponse mockResponse = TravelStylePreferenceResponse.builder()
                .userId(userId)
                .preferences(Arrays.asList(
                        TravelStyleItem.builder()
                                .travelStyle("RELAXATION")
                                .weight(new BigDecimal("0.4"))
                                .description("휴양 및 힐링을 중심으로 한 여행")
                                .build(),
                        TravelStyleItem.builder()
                                .travelStyle("SIGHTSEEING")
                                .weight(new BigDecimal("0.4"))
                                .description("관광지 방문 및 문화 체험 중심 여행")
                                .build(),
                        TravelStyleItem.builder()
                                .travelStyle("ACTIVITY")
                                .weight(new BigDecimal("0.2"))
                                .description("액티비티 및 체험 중심 여행")
                                .build()
                ))
                .totalWeight(BigDecimal.ONE)
                .message("여행 스타일 선호도가 성공적으로 수정되었습니다.")
                .build();

        when(userPreferenceService.updateTravelStylePreferences(eq(userId), any(TravelStylePreferenceRequest.class)))
                .thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(put("/api/users/{userId}/preferences/travel-style", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.totalWeight").value(1.0))
                .andExpect(jsonPath("$.preferences").isArray())
                .andExpect(jsonPath("$.preferences.length()").value(3))
                .andExpect(jsonPath("$.message").value("여행 스타일 선호도가 성공적으로 수정되었습니다."));
    }

    @Test
    @DisplayName("여행 스타일 설정 API - 유효성 검증 실패")
    void setTravelStylePreferences_ValidationFailed() throws Exception {
        // Given
        Long userId = 1L;
        TravelStylePreferenceRequest request = TravelStylePreferenceRequest.builder()
                .preferences(Arrays.asList()) // 빈 리스트
                .build();

        // When & Then
        mockMvc.perform(post("/api/users/{userId}/preferences/travel-style", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}
