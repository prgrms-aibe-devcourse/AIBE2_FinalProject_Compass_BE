package com.compass.domain.user.controller;

import com.compass.config.BaseIntegrationTest;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureMockMvc
class UserControllerTest extends BaseIntegrationTest {

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
                .andExpect(jsonPath("$.message").value("이미 존재하는 이메일입니다."))
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
                .andExpect(jsonPath("$.message").value("잘못된 이메일 형식입니다."))
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
                .andExpect(jsonPath("$.refreshToken").exists())
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
                .andExpect(jsonPath("$.message").value("이메일 또는 비밀번호가 일치하지 않습니다."));
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
                .andExpect(jsonPath("$.message").value("이메일 또는 비밀번호가 일치하지 않습니다."));
    }

    @Test
    @DisplayName("로그아웃 API 성공")
    void logout_api_success() throws Exception {
        // given
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


    @Test
    @DisplayName("내 프로필 조회 성공")
    void getMyProfile_success() throws Exception {
        // given
        User savedUser = userRepository.save(User.builder()
                .email("profile@example.com")
                .password(passwordEncoder.encode("password123"))
                .nickname("profileUser")
                .role(Role.USER)
                .build());

        String accessToken = jwtTokenProvider.createAccessToken(savedUser.getEmail());

        // when
        ResultActions resultActions = mockMvc.perform(get("/api/users/profile")
                .header("Authorization", "Bearer " + accessToken));

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("profile@example.com"))
                .andExpect(jsonPath("$.nickname").value("profileUser"));
    }

    @Test
    @DisplayName("내 프로필 조회 실패 - 인증되지 않은 사용자")
    void getMyProfile_fail_unauthorized() throws Exception {
        // given
        // No token is provided

        // when
        ResultActions resultActions = mockMvc.perform(get("/api/users/profile"));

        // then
        resultActions.andExpect(status().isUnauthorized());
    }


    @Test
    @DisplayName("내 프로필 수정 성공")
    void updateMyProfile_success() throws Exception {
        // given
        User savedUser = userRepository.save(User.builder()
                .email("update@example.com")
                .password(passwordEncoder.encode("password123"))
                .nickname("oldNickname")
                .role(Role.USER)
                .build());
        String accessToken = jwtTokenProvider.createAccessToken(savedUser.getEmail());

        String requestBody = objectMapper.writeValueAsString(
                Map.of("nickname", "newNickname", "profileImageUrl", "http://new.image/url")
        );

        // when
        ResultActions resultActions = mockMvc.perform(patch("/api/users/profile")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("newNickname"))
                .andExpect(jsonPath("$.profileImageUrl").value("http://new.image/url"));
    }

    @Test
    @DisplayName("내 프로필 수정 실패 - 인증되지 않은 사용자")
    void updateMyProfile_fail_unauthorized() throws Exception {
        // given
        String emptyRequestBody = objectMapper.writeValueAsString(Map.of());

        // when
        ResultActions resultActions = mockMvc.perform(patch("/api/users/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(emptyRequestBody));

        // then
        resultActions.andExpect(status().isUnauthorized());
    }


    @Test
    @DisplayName("여행 스타일 선호도 수정 성공")
    void updateTravelStylePreferences_success() throws Exception {
        // given
        User savedUser = userRepository.save(User.builder()
                .email("style@example.com")
                .password(passwordEncoder.encode("password123"))
                .nickname("styleUser")
                .role(Role.USER)
                .build());
        String accessToken = jwtTokenProvider.createAccessToken(savedUser.getEmail());

        String requestBody = objectMapper.writeValueAsString(
                Map.of("preferences", List.of(
                        Map.of("key", "RELAXATION", "value", 0.7),
                        Map.of("key", "ACTIVITY", "value", 0.3)
                ))
        );

        // when
        ResultActions resultActions = mockMvc.perform(put("/api/users/preferences")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].preferenceKey").value("RELAXATION"))
                .andExpect(jsonPath("$[0].preferenceValue").value(0.7));
    }

    @Test
    @DisplayName("여행 스타일 선호도 수정 실패 - 합계가 1이 아님")
    void updateTravelStylePreferences_fail_invalidSum() throws Exception {
        // given
        User savedUser = userRepository.save(User.builder()
                .email("style@example.com")
                .password(passwordEncoder.encode("password123"))
                .nickname("styleUser")
                .role(Role.USER)
                .build());
        String accessToken = jwtTokenProvider.createAccessToken(savedUser.getEmail());

        // 합계가 1.1로, 유효하지 않은 요청
        String requestBody = objectMapper.writeValueAsString(
                Map.of("preferences", List.of(
                        Map.of("key", "RELAXATION", "value", 0.8),
                        Map.of("key", "ACTIVITY", "value", 0.3)
                ))
        );

        // when
        ResultActions resultActions = mockMvc.perform(put("/api/users/preferences")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // then
        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("선호도 값의 총합은 1.0이 되어야 합니다."));
    }

    @Test
    @DisplayName("여행 스타일 선호도 수정 실패 - 인증되지 않은 사용자")
    void updateTravelStylePreferences_fail_unauthorized() throws Exception {
        // given
        String requestBody = objectMapper.writeValueAsString(Map.of("preferences", List.of()));

        // when
        ResultActions resultActions = mockMvc.perform(put("/api/users/preferences")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // then
        resultActions.andExpect(status().isUnauthorized());
    }


    @Test
    @DisplayName("예산 수준 설정 성공")
    void updateBudgetLevel_success() throws Exception {
        // given
        User savedUser = userRepository.save(User.builder()
                .email("budget@example.com")
                .password(passwordEncoder.encode("password123"))
                .nickname("budgetUser")
                .role(Role.USER)
                .build());
        String accessToken = jwtTokenProvider.createAccessToken(savedUser.getEmail());

        String requestBody = objectMapper.writeValueAsString(Map.of("level", "STANDARD"));

        // when
        ResultActions resultActions = mockMvc.perform(put("/api/users/preferences/budget-level")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.preferenceKey").value("STANDARD"))
                .andExpect(jsonPath("$.preferenceValue").value(1.0));
    }

    @Test
    @DisplayName("예산 수준 설정 실패 - 유효하지 않은 요청 값")
    void updateBudgetLevel_fail_badRequest() throws Exception {
        // given
        User savedUser = userRepository.save(User.builder()
                .email("budget@example.com")
                .password(passwordEncoder.encode("password123"))
                .nickname("budgetUser")
                .role(Role.USER)
                .build());
        String accessToken = jwtTokenProvider.createAccessToken(savedUser.getEmail());

        // 'level' 필드가 없는 잘못된 요청
        String requestBody = objectMapper.writeValueAsString(Map.of("invalidKey", "someValue"));

        // when
        ResultActions resultActions = mockMvc.perform(put("/api/users/preferences/budget-level")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // then
        resultActions.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("선호도 분석 실행 성공 - 분석할 기록이 없어 204 No Content 반환")
    void analyzeMyPreferences_success_noContent() throws Exception {
        // given
        // 여행 기록이 없는 새로운 사용자를 생성
        User newUser = userRepository.save(User.builder()
                .email("new@example.com")
                .password(passwordEncoder.encode("password123"))
                .nickname("newUser")
                .role(Role.USER)
                .build());
        String accessToken = jwtTokenProvider.createAccessToken(newUser.getEmail());

        // when
        // 실제 DB에는 이 사용자의 여행 기록이 없으므로, 분석 결과는 "NEW_TRAVELER"가 될 것임
        ResultActions resultActions = mockMvc.perform(post("/api/users/preferences/analyze")
                .header("Authorization", "Bearer " + accessToken));

        // then
        // 컨트롤러가 Optional.empty()를 받아 204 No Content를 반환하는지 검증
        resultActions.andExpect(status().isNoContent());
    }


    @Test
    @DisplayName("피드백 제출 성공")
    void submitFeedback_success() throws Exception {
        // given
        User savedUser = userRepository.save(User.builder()
                .email("feedback@example.com")
                .password(passwordEncoder.encode("password123"))
                .nickname("feedbackUser")
                .role(Role.USER)
                .build());
        String accessToken = jwtTokenProvider.createAccessToken(savedUser.getEmail());

        String requestBody = objectMapper.writeValueAsString(
                Map.of(
                        "satisfaction", 5,
                        "comment", "Very good!",
                        "revisitIntent", true
                )
        );

        // when
        ResultActions resultActions = mockMvc.perform(post("/api/users/feedback")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // then
        resultActions.andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.message").value("Feedback submitted successfully."));
    }

    @Test
    @DisplayName("피드백 제출 실패 - 유효하지 않은 만족도 점수")
    void submitFeedback_fail_invalidSatisfaction() throws Exception {
        // given
        User savedUser = userRepository.save(User.builder()
                .email("feedback@example.com")
                .password(passwordEncoder.encode("password123"))
                .nickname("feedbackUser")
                .role(Role.USER)
                .build());
        String accessToken = jwtTokenProvider.createAccessToken(savedUser.getEmail());

        // 만족도 점수가 6 (유효하지 않음)
        String requestBody = objectMapper.writeValueAsString(
                Map.of("satisfaction", 6)
        );

        // when
        ResultActions resultActions = mockMvc.perform(post("/api/users/feedback")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // then
        resultActions.andExpect(status().isBadRequest());
    }



}
