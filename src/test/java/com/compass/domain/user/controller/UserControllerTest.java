package com.compass.domain.user.controller;

import com.compass.domain.common.exception.DuplicateEmailException;
import com.compass.domain.common.exception.GlobalExceptionHandler;
import com.compass.domain.common.jwt.JwtTokenProvider;
import com.compass.domain.config.SecurityConfig;
import com.compass.domain.user.dto.LoginRequest;
import com.compass.domain.user.dto.SignUpRequest;
import com.compass.domain.user.dto.TokenResponse;
import com.compass.domain.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class}) // SecurityConfig와 GlobalExceptionHandler를 테스트 컨텍스트에 포함시킵니다.
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc; // 가짜 HTTP 요청을 보내는 역할을 합니다.

    @Autowired
    private ObjectMapper objectMapper; // Java 객체를 JSON 문자열로 변환합니다.

    @MockBean // UserService의 가짜(Mock) 객체를 생성하여 주입합니다. 실제 서비스 로직이 실행되지 않습니다.
    private UserService userService;

    @MockBean // SecurityConfig가 의존하는 JwtTokenProvider를 Mock 객체로 주입합니다.
    private JwtTokenProvider jwtTokenProvider;

    private SignUpRequest signUpRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        // given: 모든 테스트 전에 실행될 준비 과정
        signUpRequest = new SignUpRequest("test@example.com", "password123!");
        loginRequest = new LoginRequest("test@example.com", "password123!");
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
                        .content(objectMapper.writeValueAsString(signUpRequest)) // 요청 본문에 JSON 데이터를 담습니다.
                        .with(anonymous())) // 이 요청이 익명 사용자에 의해 수행됨을 명시합니다.
                .andExpect(status().isCreated()) // 응답 상태가 201 Created인지 확인합니다.
                .andExpect(jsonPath("$.message").value("회원가입 신청이 완료되었습니다. 이메일을 확인하여 계정을 활성화해주세요.")) // 응답 JSON의 message 필드 값을 확인합니다.
                .andDo(print()); // 테스트 요청/응답 전체 내용을 콘솔에 출력합니다.

        // verify: userService.signUp() 메소드가 정확히 1번 호출되었는지 검증
        ArgumentCaptor<SignUpRequest> captor = ArgumentCaptor.forClass(SignUpRequest.class);
        verify(userService).signUp(captor.capture());
        SignUpRequest capturedRequest = captor.getValue();

        // 캡처된 객체의 내용이 예상과 일치하는지 검증
        assertThat(capturedRequest.getEmail()).isEqualTo(signUpRequest.getEmail());
        assertThat(capturedRequest.getPassword()).isEqualTo(signUpRequest.getPassword());
    }

    @Test
    @DisplayName("회원가입 실패 테스트 - 이메일 중복")
    void signUpFail_whenEmailIsDuplicated() throws Exception {
        // given: 서비스 레이어에서 DuplicateEmailException을 던지도록 설정
        String duplicatedEmailMessage = "이미 가입된 이메일입니다.";
        doThrow(new DuplicateEmailException(duplicatedEmailMessage))
                .when(userService).signUp(any(SignUpRequest.class));

        // when & then: API 호출 및 409 Conflict 응답 검증
        mockMvc.perform(post("/api/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpRequest))
                        .with(anonymous()))
                .andExpect(status().isConflict()) // 409 Conflict 상태를 기대
                .andExpect(jsonPath("$.message").value(duplicatedEmailMessage))
                .andDo(print());
    }

    @Test
    @DisplayName("회원가입 실패 테스트 - 유효하지 않은 비밀번호")
    void signUpFail_whenPasswordIsInvalid() throws Exception {
        // given: 유효하지 않은 비밀번호를 가진 요청 객체 생성
        SignUpRequest invalidRequest = new SignUpRequest("test@example.com", "short");

        // when & then: API 호출 및 400 Bad Request 응답 검증
        mockMvc.perform(post("/api/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .with(anonymous()))
                .andExpect(status().isBadRequest()) // 400 Bad Request 상태를 기대
                .andExpect(jsonPath("$.message").value("비밀번호는 8~16자의 영문, 숫자, 특수문자를 포함해야 합니다."))
                .andDo(print());
    }

    @Test
    @DisplayName("로그인 성공 테스트")
    void loginSuccess() throws Exception {
        // given: 서비스가 더미 토큰을 반환하도록 설정
        String dummyToken = "dummy.jwt.token.string";
        TokenResponse tokenResponse = new TokenResponse(dummyToken);
        when(userService.login(any(LoginRequest.class))).thenReturn(tokenResponse);

        // when & then: 로그인 API 호출 및 200 OK, 토큰 응답 검증
        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest))
                        .with(anonymous()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(dummyToken))
                .andDo(print());

        // verify: 서비스의 login 메소드가 올바른 요청 객체와 함께 호출되었는지 검증
        ArgumentCaptor<LoginRequest> captor = ArgumentCaptor.forClass(LoginRequest.class);
        verify(userService).login(captor.capture());
        LoginRequest capturedRequest = captor.getValue();

        assertThat(capturedRequest.getEmail()).isEqualTo(loginRequest.getEmail());
        assertThat(capturedRequest.getPassword()).isEqualTo(loginRequest.getPassword());
    }

    @Test
    @DisplayName("로그인 실패 테스트 - 잘못된 자격 증명")
    void loginFail_withBadCredentials() throws Exception {
        // given: 서비스 레이어에서 인증 실패 시 BadCredentialsException을 던지도록 설정
        when(userService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // when & then: 로그인 API 호출 및 401 Unauthorized, 에러 메시지 검증
        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest))
                        .with(anonymous()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("이메일 또는 비밀번호가 일치하지 않습니다."))
                .andDo(print());
    }
}