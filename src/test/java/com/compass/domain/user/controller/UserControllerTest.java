package com.compass.domain.user.controller;

import com.compass.domain.user.dto.SignUpRequest;
import com.compass.domain.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class) // UserController와 관련된 웹 레이어 빈만 로드합니다. (DB 연결 X)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc; // 가짜 HTTP 요청을 보내는 역할을 합니다.

    @Autowired
    private ObjectMapper objectMapper; // Java 객체를 JSON 문자열로 변환합니다.

    @MockBean // UserService의 가짜(Mock) 객체를 생성하여 주입합니다. 실제 서비스 로직이 실행되지 않습니다.
    private UserService userService;

    private SignUpRequest signUpRequest;

    @BeforeEach
    void setUp() {
        // given: 모든 테스트 전에 실행될 준비 과정
        signUpRequest = new SignUpRequest();
        signUpRequest.setEmail("test@example.com");
        signUpRequest.setPassword("password123!");
    }

    @Test
    @DisplayName("회원가입 성공 테스트")
    void signUpSuccess() throws Exception {
        // given: 테스트를 위한 추가 준비
        // userService.signUp() 메소드가 어떤 SignUpRequest 객체로 호출되든 아무 작업도 하지 않도록 설정
        doNothing().when(userService).signUp(any(SignUpRequest.class));

        // when: 실제 테스트하려는 동작 수행
        // then: 결과 검증
        mockMvc.perform(post("/api/users/signup") // POST /api/users/signup 요청을 보냅니다.
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpRequest))) // 요청 본문에 JSON 데이터를 담습니다.
                .andExpect(status().isCreated()) // 응답 상태가 201 Created인지 확인합니다.
                .andExpect(jsonPath("$.message").value("회원가입 신청이 완료되었습니다. 이메일을 확인하여 계정을 활성화해주세요.")) // 응답 JSON의 message 필드 값을 확인합니다.
                .andDo(print()); // 테스트 요청/응답 전체 내용을 콘솔에 출력합니다.

        // verify: userService.signUp() 메소드가 정확히 1번 호출되었는지 검증
        verify(userService).signUp(any(SignUpRequest.class));
    }
}