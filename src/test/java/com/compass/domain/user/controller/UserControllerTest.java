package com.compass.domain.user.controller;

import com.compass.config.BaseIntegrationTest;
import com.compass.domain.user.dto.UserDto;
import com.compass.domain.user.enums.Role;
import com.compass.domain.user.entity.User;
import com.compass.domain.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class UserControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("회원가입 API 성공")
    void signUp_api_success() throws Exception {
        // given
        UserDto.SignUpRequest request = new UserDto.SignUpRequest("test@example.com", "password123", "testuser");
        String requestBody = objectMapper.writeValueAsString(request);

        // when
        ResultActions resultActions = mockMvc.perform(post("/api/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.nickname").value("testuser"))
                .andDo(print());
    }

    @Test
    @DisplayName("회원가입 API 실패 - 중복된 이메일")
    void signUp_api_fail_duplicateEmail() throws Exception {
        // given
        userRepository.save(User.builder()
                .email("test@example.com")
                .password("any")
                .nickname("any")
                .role(Role.USER)
                .build());
        UserDto.SignUpRequest request = new UserDto.SignUpRequest("test@example.com", "password123", "testuser");
        String requestBody = objectMapper.writeValueAsString(request);

        // when
        ResultActions resultActions = mockMvc.perform(post("/api/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // then
        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email already exists."))
                .andDo(print());
    }

    @Test
    @DisplayName("회원가입 API 실패 - 유효성 검사 실패 (잘못된 이메일 형식)")
    void signUp_api_fail_validation() throws Exception {
        // given
        UserDto.SignUpRequest request = new UserDto.SignUpRequest("not-an-email", "password123", "testuser");
        String requestBody = objectMapper.writeValueAsString(request);

        // when
        ResultActions resultActions = mockMvc.perform(post("/api/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // then
        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid email format."))
                .andDo(print());
    }

    @Test
    @DisplayName("로그인 API 성공")
    void login_api_success() throws Exception {
        // given
        userRepository.save(User.builder()
                .email("test@example.com")
                .password(passwordEncoder.encode("password123"))
                .nickname("testuser")
                .role(Role.USER)
                .build());
        UserDto.LoginRequest request = new UserDto.LoginRequest("test@example.com", "password123");
        String requestBody = objectMapper.writeValueAsString(request);

        // when
        ResultActions resultActions = mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andDo(print());
    }

    @Test
    @DisplayName("로그인 API 실패 - 존재하지 않는 사용자")
    void login_api_fail_userNotFound() throws Exception {
        // given
        UserDto.LoginRequest request = new UserDto.LoginRequest("nonexistent@example.com", "password123");
        String requestBody = objectMapper.writeValueAsString(request);

        // when & then
        mockMvc.perform(post("/api/users/login").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User not found."));
    }

    @Test
    @DisplayName("로그인 API 실패 - 비밀번호 불일치")
    void login_api_fail_invalidPassword() throws Exception {
        // given
        userRepository.save(User.builder()
                .email("test@example.com")
                .password(passwordEncoder.encode("password123"))
                .nickname("testuser")
                .role(Role.USER)
                .build());
        UserDto.LoginRequest request = new UserDto.LoginRequest("test@example.com", "wrongpassword");
        String requestBody = objectMapper.writeValueAsString(request);

        // when & then
        mockMvc.perform(post("/api/users/login").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid password."));
    }
}