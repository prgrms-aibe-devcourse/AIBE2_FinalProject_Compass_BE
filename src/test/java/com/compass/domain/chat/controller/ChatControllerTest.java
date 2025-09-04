package com.compass.domain.chat.controller;

import com.compass.domain.chat.dto.ChatDtos;
import com.compass.domain.chat.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// @WebMvcTest(ChatController.class)는 ChatController만 테스트하기 위해 사용합니다.
// 웹 계층과 관련된 빈들만 로드하여 테스트를 가볍게 만듭니다.
@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc; // MockMvc를 주입받아 API를 테스트합니다.

    @Autowired
    private ObjectMapper objectMapper; // 객체를 JSON으로 변환하기 위해 사용합니다.

    @MockBean // ChatController가 의존하는 ChatService를 가짜(Mock) 객체로 만듭니다.
    private ChatService chatService;

    @Test
    @DisplayName("REQ-CHAT-001: 채팅방 생성 API 성공 테스트")
    void createChatThread_Success() throws Exception {
        // given (테스트 준비)
        String testUserId = "test-user-123";
        ChatDtos.ThreadDto expectedThreadDto = new ChatDtos.ThreadDto(
                "thread-uuid-123",
                testUserId,
                LocalDateTime.now(),
                "아직 메시지가 없습니다."
        );

        // chatService.createThread 메소드가 어떤 userId로 호출되든(anyString()),
        // 위에서 만든 expectedThreadDto 객체를 반환하도록 설정합니다.
        given(chatService.createThread(anyString())).willReturn(expectedThreadDto);

        // when (테스트 실행)
        // /api/chat/threads 로 POST 요청을 보냅니다.
        ResultActions resultActions = mockMvc.perform(post("/api/chat/threads")
                .header("X-User-ID", testUserId)
                .contentType(MediaType.APPLICATION_JSON));

        // then (결과 검증)
        resultActions
                .andExpect(status().isCreated()) // HTTP 상태 코드가 201(Created)인지 확인
                .andExpect(jsonPath("$.id").value(expectedThreadDto.id())) // 응답 JSON의 id 필드 검증
                .andExpect(jsonPath("$.userId").value(testUserId)) // 응답 JSON의 userId 필드 검증
                .andDo(print()); // 요청/응답 전체 내용 출력
    }
}
