package com.compass.domain.user.service;

import com.compass.config.jwt.JwtTokenProvider;
import com.compass.domain.user.dto.UserDto;
import com.compass.domain.user.entity.User;
import com.compass.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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

    @Test
    @DisplayName("회원가입 성공")
    void signUp_success() {
        // given
        UserDto.SignUpRequest signUpRequest = new UserDto.SignUpRequest("test@example.com", "password123", "testuser");

        User user = User.builder()
                .id(1L)
                .email(signUpRequest.getEmail())
                .password("encodedPassword")
                .nickname(signUpRequest.getNickname())
                .build();

        when(userRepository.existsByEmail(signUpRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(signUpRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        // when
        UserDto.SignUpResponse response = userService.signUp(signUpRequest);

        // then
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo(signUpRequest.getEmail());
        assertThat(response.getNickname()).isEqualTo(signUpRequest.getNickname());
    }

    @Test
    @DisplayName("회원가입 실패 - 이미 존재하는 이메일")
    void signUp_fail_duplicateEmail() {
        // given
        UserDto.SignUpRequest signUpRequest = new UserDto.SignUpRequest("test@example.com", "password123", "testuser");
        when(userRepository.existsByEmail(signUpRequest.getEmail())).thenReturn(true);

        // when & then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> userService.signUp(signUpRequest));
        assertThat(exception.getMessage()).isEqualTo("Email already exists.");
    }

    @Test
    @DisplayName("로그인 성공")
    void login_success() {
        // given
        UserDto.LoginRequest loginRequest = new UserDto.LoginRequest("test@example.com", "password123");
        User user = User.builder()
                .email(loginRequest.getEmail())
                .password("encodedPassword")
                .build();

        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())).thenReturn(true);
        when(jwtTokenProvider.createToken(user.getEmail())).thenReturn("test.access.token");

        // when
        UserDto.LoginResponse response = userService.login(loginRequest);

        // then
        assertThat(response.getAccessToken()).isEqualTo("test.access.token");
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 사용자")
    void login_fail_userNotFound() {
        // given
        UserDto.LoginRequest loginRequest = new UserDto.LoginRequest("test@example.com", "password123");
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.empty());

        // when & then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> userService.login(loginRequest));
        assertThat(exception.getMessage()).isEqualTo("User not found.");
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
        assertThat(exception.getMessage()).isEqualTo("Invalid password.");
    }
}