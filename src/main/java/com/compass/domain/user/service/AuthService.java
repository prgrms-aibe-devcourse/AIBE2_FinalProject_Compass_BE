package com.compass.domain.user.service;

import com.compass.config.jwt.JwtTokenProvider;
import com.compass.domain.user.dto.JwtDto;
import com.compass.domain.user.dto.LoginRequestDto;
import com.compass.domain.user.dto.SignupRequestDto;
import com.compass.domain.user.dto.UserDto;
import com.compass.domain.user.entity.User;
import com.compass.domain.user.enums.Role;
import com.compass.domain.user.exception.DuplicateEmailException;
import com.compass.domain.user.exception.InvalidCredentialsException;
import com.compass.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String REFRESH_TOKEN_PREFIX = "RT:";
    private static final String BLACKLIST_PREFIX = "blacklist:";

    public UserDto signup(SignupRequestDto signupRequest) {
        // Check if email already exists
        if (userRepository.findByEmail(signupRequest.getEmail()).isPresent()) {
            throw new DuplicateEmailException("Email already exists: " + signupRequest.getEmail());
        }

        // Create new user
        User user = User.builder()
                .email(signupRequest.getEmail())
                .password(passwordEncoder.encode(signupRequest.getPassword()))
                .nickname(signupRequest.getNickname())
                .role(Role.USER)
                .socialType(null) // local registration doesn't have social type
                .socialId(null)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with email: {}", savedUser.getEmail());

        return UserDto.builder()
                .id(savedUser.getId())
                .email(savedUser.getEmail())
                .nickname(savedUser.getNickname())
                .profileImageUrl(savedUser.getProfileImageUrl())
                .provider(savedUser.getProvider())
                .createdAt(savedUser.getCreatedAt())
                .updatedAt(savedUser.getUpdatedAt())
                .build();
    }

    public JwtDto login(LoginRequestDto loginRequest) {
        // Find user by email
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        // Verify password
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        // Generate tokens with userId
        String accessToken = jwtTokenProvider.createAccessToken(user.getEmail(), user.getId(), Collections.singletonList(user.getRole().name()));
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmail(), user.getId());

        log.info("User logged in successfully: {}", user.getEmail());

        return JwtDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpiration(accessToken))
                .build();
    }

    public void logout(String accessToken) {
        // 1. 토큰 유효성 검증
        if (!jwtTokenProvider.validateAccessToken(accessToken)) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }

        // 2. 토큰에서 만료 시간 정보 추출
        Long expiration = jwtTokenProvider.getExpiration(accessToken);

        // 3. Redis에 (Key: "blacklist:{accessToken}", Value: "logout") 저장 및 만료 시간 설정
        redisTemplate.opsForValue().set(BLACKLIST_PREFIX + accessToken, "logout", expiration, TimeUnit.MILLISECONDS);
    }



    public JwtDto refreshToken(String refreshToken) {
        // Validate refresh token
        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        // Extract email from refresh token
        String email = jwtTokenProvider.getUsername(refreshToken);

        // Find user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        // Generate new tokens
        String newAccessToken = jwtTokenProvider.createAccessToken(user.getEmail(), Collections.singletonList(user.getRole().name()));
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());

        log.info("Token refreshed for user: {}", user.getEmail());

        return JwtDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpiration(newAccessToken))
                .build();
    }
}