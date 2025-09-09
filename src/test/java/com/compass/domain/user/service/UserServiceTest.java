package com.compass.domain.user.service;

import com.compass.config.jwt.JwtTokenProvider;
import com.compass.domain.user.dto.UserDto;
import com.compass.domain.user.entity.User;
import com.compass.domain.user.enums.Role;
import com.compass.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    @DisplayName("회원가입 성공")
    void signUp_success() {
        // given
        UserDto.SignUpRequest signUpRequest = new UserDto.SignUpRequest("test@example.com", "password123", "testuser");

        User user = User.builder() // ID가 없는 User 객체 생성
                .email(signUpRequest.getEmail())
                .password("encodedPassword")
                .nickname(signUpRequest.getNickname())
                .role(Role.USER)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L); // 테스트를 위해 리플렉션으로 ID 설정

        when(userRepository.existsByEmail(signUpRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(signUpRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        // when
        UserDto.SignUpResponse response = userService.signUp(signUpRequest);

        // then
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo(signUpRequest.getEmail());
        assertThat(response.getNickname()).isEqualTo(signUpRequest.getNickname());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 이미 존재하는 이메일")
    void signUp_fail_duplicateEmail() {
        // given
        UserDto.SignUpRequest signUpRequest = new UserDto.SignUpRequest("test@example.com", "password123", "testuser");
        when(userRepository.existsByEmail(signUpRequest.getEmail())).thenReturn(true);

        // when & then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> userService.signUp(signUpRequest));
        assertThat(exception.getMessage()).isEqualTo("이미 존재하는 이메일입니다.");
    }

    @Test
    @DisplayName("로그인 성공")
    void login_success() {
        // given
        UserDto.LoginRequest loginRequest = new UserDto.LoginRequest("test@example.com", "password123");
        User user = User.builder()
                .email(loginRequest.getEmail())
                .password("encodedPassword")
                .role(Role.USER)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);

        // RedisTemplate의 ValueOperations를 Mocking합니다.
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);

        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())).thenReturn(true);
        when(jwtTokenProvider.createAccessToken(user.getEmail())).thenReturn("test.access.token");
        when(jwtTokenProvider.createRefreshToken()).thenReturn("test.refresh.token");
        when(jwtTokenProvider.getRefreshTokenExpiration()).thenReturn(604800000L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // when
        UserDto.LoginResponse response = userService.login(loginRequest);

        // then
        assertThat(response.getAccessToken()).isEqualTo("test.access.token");
        assertThat(response.getRefreshToken()).isEqualTo("test.refresh.token");

        // Redis에 Refresh Token이 올바르게 저장되는지 검증합니다.
        verify(redisTemplate.opsForValue(), times(1)).set("RT:1", "test.refresh.token", 604800000L, TimeUnit.MILLISECONDS);
        // 더 이상 DB에 Refresh Token을 저장하지 않으므로 save는 호출되지 않아야 합니다.
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 사용자")
    void login_fail_userNotFound() {
        // given
        UserDto.LoginRequest loginRequest = new UserDto.LoginRequest("test@example.com", "password123");
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.empty());

        // when & then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> userService.login(loginRequest));
        assertThat(exception.getMessage()).isEqualTo("이메일 또는 비밀번호가 일치하지 않습니다.");
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    void login_fail_invalidPassword() {
        // given
        UserDto.LoginRequest loginRequest = new UserDto.LoginRequest("test@example.com", "password123");
        User user = User.builder().email(loginRequest.getEmail()).password("encodedPassword").build();

        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())).thenReturn(false);

        // when & then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> userService.login(loginRequest));
        assertThat(exception.getMessage()).isEqualTo("이메일 또는 비밀번호가 일치하지 않습니다.");
    }

    @Test
    @DisplayName("로그아웃 성공")
    void logout_success() {
        // given
        String accessToken = "valid.access.token";
        long expiration = 3600000L; // 1 hour

        when(jwtTokenProvider.validateAccessToken(accessToken)).thenReturn(true);
        when(jwtTokenProvider.getExpiration(accessToken)).thenReturn(expiration);

        // RedisTemplate Mocking
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // when
        userService.logout(accessToken);

        // then
        // Redis에 "blacklist:{token}" 키로 "logout"이 저장되었는지, 그리고 만료 시간이 올바르게 설정되었는지 검증
        verify(redisTemplate.opsForValue(), times(1)).set("blacklist:" + accessToken, "logout", expiration, TimeUnit.MILLISECONDS);
    }

    @Test
    @DisplayName("로그아웃 실패 - 유효하지 않은 토큰")
    void logout_fail_invalidToken() {
        // given
        String invalidToken = "invalid.access.token";
        when(jwtTokenProvider.validateAccessToken(invalidToken)).thenReturn(false);

        // when & then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> userService.logout(invalidToken));
        assertThat(exception.getMessage()).isEqualTo("유효하지 않은 토큰입니다.");
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    @DisplayName("이메일로 프로필 조회 성공")
    void getUserProfileByEmail_success() {
        // given
        String email = "profile@example.com";
        User mockUser = User.builder() // ID는 데이터베이스에서 자동 생성되므로 빌더에서 설정하지 않습니다.
                .email(email)
                .nickname("profileUser")
                .build();
        ReflectionTestUtils.setField(mockUser, "id", 1L); // DB에 저장된 상태를 모방하기 위해 ID를 수동으로 설정합니다.
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));;

        // when
        UserDto result = userService.getUserProfileByEmail(email);

        // then
        assertThat(result.getEmail()).isEqualTo(email);
        assertThat(result.getNickname()).isEqualTo("profileUser");
    }

    @Test
    @DisplayName("프로필 조회 실패 - 존재하지 않는 사용자")
    void getUserProfileByEmail_fail_userNotFound() {
        // given
        String email = "nonexistent@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // when & then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.getUserProfileByEmail(email));
        assertThat(exception.getMessage()).isEqualTo("User not found with email: " + email);
    }

    @Test
    @DisplayName("이메일로 프로필 수정 성공")
    void updateUserProfileByEmail_success() {
        // given
        String email = "update@example.com";
        User mockUser = User.builder()
                .email(email)
                .nickname("oldNickname")
                .build();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));

        UserDto.ProfileUpdateRequest updateRequest = new UserDto.ProfileUpdateRequest();
        ReflectionTestUtils.setField(updateRequest, "nickname", "newNickname");
        ReflectionTestUtils.setField(updateRequest, "profileImageUrl", "http://new.image/url");

        // when
        UserDto result = userService.updateUserProfileByEmail(email, updateRequest);

        // then
        assertThat(result.getNickname()).isEqualTo("newNickname");
        assertThat(result.getProfileImageUrl()).isEqualTo("http://new.image/url");
        verify(userRepository).findByEmail(email);
    }

    @Test
    @DisplayName("이메일로 프로필 수정 실패 - 존재하지 않는 사용자")
    void updateUserProfileByEmail_fail_userNotFound() {
        // given
        String email = "nonexistent@example.com";
        UserDto.ProfileUpdateRequest updateRequest = new UserDto.ProfileUpdateRequest();
        ReflectionTestUtils.setField(updateRequest, "nickname", "newNickname");

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // when & then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.updateUserProfileByEmail(email, updateRequest));
        assertThat(exception.getMessage()).isEqualTo("User not found with email: " + email);
    }


}