package com.compass.domain.user.controller;

import com.compass.config.IntegrationTest;
import com.compass.config.jwt.JwtTokenProvider;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureMockMvc
@IntegrationTest
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().flushAll(); // Clear Redis before each test
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
                .andExpect(jsonPath("$.message").value("이미 존재하는 이메일입니다.")) // UserService의 예외 메시지와 일치시킵니다.
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
                .andExpect(jsonPath("$.message").value("잘못된 이메일 형식입니다.")) // @Valid의 기본 메시지 또는 커스텀 메시지와 일치시킵니다.
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
                .andExpect(jsonPath("$.refreshToken").exists()) // refreshToken 존재 여부도 검증합니다.
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
                .andExpect(jsonPath("$.message").value("이메일 또는 비밀번호가 일치하지 않습니다.")); // 보안을 위해 통합된 메시지를 검증합니다.
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
                .andExpect(jsonPath("$.message").value("이메일 또는 비밀번호가 일치하지 않습니다.")); // 보안을 위해 통합된 메시지를 검증합니다.
    }

    @Test
    @DisplayName("로그아웃 API 성공")
    void logout_api_success() throws Exception {
        // given
        // 테스트를 위한 사용자 및 토큰 생성
        String userEmail = "logout@example.com";
        userRepository.save(User.builder()
                .email(userEmail)
                .password(passwordEncoder.encode("password123"))
                .nickname("logoutuser")
                .role(Role.USER)
                .build());

        String accessToken = jwtTokenProvider.createAccessToken(userEmail);

        // when
        ResultActions resultActions = mockMvc.perform(post("/api/users/logout")
                .header("Authorization", "Bearer " + accessToken));

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("로그아웃 되었습니다."));

        // Redis에 토큰이 블랙리스트로 등록되었는지 확인
        String blacklisted = redisTemplate.opsForValue().get("blacklist:" + accessToken);
        assertThat(blacklisted).isEqualTo("logout");
    }

    @Test
    @DisplayName("로그아웃 API 실패 - 잘못된 헤더 형식")
    void logout_api_fail_badHeader() throws Exception {
        // given & when
        ResultActions resultActions = mockMvc.perform(post("/api/users/logout")
                .header("Authorization", "invalid-token-format"));

        // then
        resultActions.andExpect(status().isBadRequest());
    }
}