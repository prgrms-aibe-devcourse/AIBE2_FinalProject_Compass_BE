package com.compass.domain.user.service;

import com.compass.config.jwt.JwtTokenProvider;
import com.compass.domain.trip.entity.TravelHistory;
import com.compass.domain.trip.repository.TravelHistoryRepository;
import com.compass.domain.user.dto.UserDto;
import com.compass.domain.user.dto.UserPreferenceDto;
import com.compass.domain.user.entity.User;
import com.compass.domain.user.entity.UserPreference;
import com.compass.domain.user.enums.Role;
import com.compass.domain.user.repository.UserPreferenceRepository;
import com.compass.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private static final String REFRESH_TOKEN_PREFIX = "RT:";
    private static final String BLACKLIST_PREFIX = "blacklist:";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserPreferenceRepository userPreferenceRepository;
    private final TravelHistoryRepository travelHistoryRepository;
    private final PreferenceAnalyzer preferenceAnalyzer;

    @Transactional
    public UserDto.SignUpResponse signUp(UserDto.SignUpRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .role(Role.USER) // 신규 가입자에게 USER 권한부여
                .build();

        User savedUser = userRepository.save(user);

        return UserDto.SignUpResponse.builder()
                .id(savedUser.getId())
                .email(savedUser.getEmail())
                .nickname(savedUser.getNickname())
                .build();
    }

    @Transactional
    public UserDto.LoginResponse login(UserDto.LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 일치하지 않습니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 일치하지 않습니다.");
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(); // Refresh Token 생성

        // Redis에 Refresh Token 저장 (Key: "RT:{userId}", Value: refreshToken)
        // 토큰의 유효기간만큼 Redis에도 유효기간을 설정합니다.
        long refreshTokenExpiration = jwtTokenProvider.getRefreshTokenExpiration();
        redisTemplate.opsForValue().set(REFRESH_TOKEN_PREFIX + user.getId(), refreshToken, refreshTokenExpiration, TimeUnit.MILLISECONDS);

        return UserDto.LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
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


    @Transactional(readOnly = true)
    public UserDto getUserProfileByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
        log.info("Fetched profile for user: {}", user.getEmail());
        return UserDto.from(user);
    }

    @Transactional
    public UserDto updateUserProfileByEmail(String email, UserDto.ProfileUpdateRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
        user.updateProfile(request.getNickname(), request.getProfileImageUrl());
        log.info("Updated profile for user: {}", user.getEmail());
        return UserDto.from(user);
    }

    @Transactional
    public List<UserPreferenceDto.Response> updateUserTravelStyle(String email, UserPreferenceDto.UpdateRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));

        // 선호도 값의 총합이 1.0인지 검증
        BigDecimal totalValue = request.getPreferences().stream()
                .map(UserPreferenceDto.PreferenceItem::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalValue.compareTo(BigDecimal.ONE) != 0) {
            throw new IllegalArgumentException("선호도 값의 총합은 1.0이 되어야 합니다.");
        }

        // 기존 여행 스타일 선호도 삭제
        userPreferenceRepository.deleteByUserAndPreferenceType(user, "TRAVEL_STYLE");

        // 새로운 선호도 저장
        List<UserPreference> preferences = request.getPreferences().stream()
                .map(item -> UserPreference.builder()
                        .user(user)
                        .preferenceType("TRAVEL_STYLE")
                        .preferenceKey(item.getKey())
                        .preferenceValue(item.getValue())
                        .build())
                .collect(Collectors.toList());
        // 3. 새로운 선호도 일괄 저장
        userPreferenceRepository.saveAll(preferences);
        log.info("Updated travel style preferences for user: {}", email);
        // 4. 결과 반환
        return preferences.stream().map(UserPreferenceDto.Response::from).collect(Collectors.toList());
    }



    @Transactional
    public UserPreferenceDto.Response updateBudgetLevel(String email, UserPreferenceDto.BudgetUpdateRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));

        final String preferenceType = "BUDGET_LEVEL";

        // 기존 예산 레벨 선호도 삭제
        userPreferenceRepository.deleteByUserAndPreferenceType(user, preferenceType);

        // 새로운 선호도 저장
        UserPreference newPreference = UserPreference.builder()
                .user(user)
                .preferenceType(preferenceType)
                .preferenceKey(request.getLevel().name())
                .preferenceValue(BigDecimal.ONE) // 단일 선택이므로 100%를 의미하는 1.0으로 저장
                .build();

        userPreferenceRepository.save(newPreference);
        log.info("Updated budget level preference for user: {}, level: {}", email, request.getLevel());

        return UserPreferenceDto.Response.from(newPreference);
    }



    @Transactional
    public Optional<UserPreferenceDto.Response> analyzeAndSavePreferences(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));

        // 1. 최근 여행 기록 조회
        List<TravelHistory> histories = travelHistoryRepository.findTop10ByUserIdOrderByCreatedAtDesc(user.getId());

        // 2. 분석기 실행
        String analyzedType = preferenceAnalyzer.analyzeTravelStyleWithAi(histories);

        // 3. (중요) 분석 결과가 유의미할 때만 저장 로직을 실행합니다.
        if ("NEW_TRAVELER".equals(analyzedType)) {
            log.info("No travel history for user: {}. Skipping preference analysis update.", email);
            return Optional.empty(); // 아무것도 저장하지 않고, 빈 결과를 반환합니다.
        }

        // 4. 분석 결과 저장 (기존 값 덮어쓰기)
        final String preferenceType = "ANALYZED_TRAVEL_TYPE";
        userPreferenceRepository.deleteByUserAndPreferenceType(user, preferenceType);

        UserPreference analyzedPreference = UserPreference.builder()
                .user(user)
                .preferenceType(preferenceType)
                .preferenceKey(analyzedType)
                .preferenceValue(BigDecimal.ONE) // 분석된 타입은 하나이므로 100%
                .build();

        userPreferenceRepository.save(analyzedPreference);
        return Optional.of(UserPreferenceDto.Response.from(analyzedPreference));
    }



}