package com.compass.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Epic 통합 테스트")
public class EpicIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String jwtToken;
    private final String threadId = "test-thread-001";
    private final String userId = "test-user-001";

    @BeforeEach
    void setUp() {
        // JWT 토큰 생성 (실제 환경에서는 로그인 API 호출)
        jwtToken = "Bearer test-token";
    }

    @Test
    @DisplayName("Epic 1: 여행 계획 폼 요청 테스트")
    void testShowQuickInputForm() throws Exception {
        Map<String, Object> request = Map.of(
            "message", "여행 계획 짜줘",
            "threadId", threadId,
            "userId", userId
        );

        mockMvc.perform(post("/api/chat/unified")
                .header("Authorization", jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("QUICK_FORM"))
                .andExpect(jsonPath("$.data.formId").value("QUICK_INPUT_V1"))
                .andExpect(jsonPath("$.data.fields").isArray());
    }

    @Test
    @DisplayName("Epic 2: Follow-up 질문 트리거 테스트")
    void testTriggerFollowUp() throws Exception {
        Map<String, Object> request = Map.of(
            "message", "form_submit",
            "threadId", threadId,
            "userId", userId,
            "data", Map.of(
                "destinations", List.of("목적지 미정"),
                "departureLocation", "서울",
                "travelDates", Map.of(
                    "start", "2024-03-01",
                    "end", "2024-03-03"
                )
            )
        );

        mockMvc.perform(post("/api/chat/unified")
                .header("Authorization", jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("ASSISTANT_MESSAGE"))
                .andExpect(jsonPath("$.nextAction").value("START_FOLLOW_UP"));
    }

    @Test
    @DisplayName("Epic 3: 완전한 폼 제출로 계획 생성 트리거")
    void testTriggerPlanGeneration() throws Exception {
        Map<String, Object> request = Map.of(
            "message", "form_submit",
            "threadId", threadId + "-complete",
            "userId", userId,
            "data", Map.of(
                "destinations", List.of("제주도", "부산"),
                "departureLocation", "서울",
                "travelDates", Map.of(
                    "start", "2024-03-01",
                    "end", "2024-03-05"
                ),
                "companions", "가족",
                "budget", 2000000,
                "travelStyle", List.of("휴양", "관광")
            )
        );

        mockMvc.perform(post("/api/chat/unified")
                .header("Authorization", jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("ASSISTANT_MESSAGE"))
                .andExpect(jsonPath("$.nextAction").value("TRIGGER_PLAN_GENERATION"));
    }

    @Test
    @DisplayName("Epic 4: 이미지 업로드 처리 테스트")
    void testImageUpload() throws Exception {
        Map<String, Object> request = Map.of(
            "message", "항공권 이미지 업로드했어요",
            "threadId", threadId + "-image",
            "userId", userId,
            "attachments", List.of(Map.of(
                "type", "image",
                "url", "https://example.com/flight-ticket.jpg"
            ))
        );

        mockMvc.perform(post("/api/chat/unified")
                .header("Authorization", jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk());
                // ProcessImageFunction이 구현되면 더 구체적인 검증 추가
    }

    @Test
    @DisplayName("Epic 5: 일반 대화 처리 테스트")
    void testGeneralConversation() throws Exception {
        Map<String, Object> request = Map.of(
            "message", "오늘 서울 날씨 어때?",
            "threadId", threadId + "-general",
            "userId", userId
        );

        mockMvc.perform(post("/api/chat/unified")
                .header("Authorization", jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("ASSISTANT_MESSAGE"));
    }
}