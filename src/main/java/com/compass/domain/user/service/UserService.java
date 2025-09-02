package com.compass.domain.user.service;

import com.compass.domain.common.exception.ResourceNotFoundException;
import com.compass.domain.common.mail.EmailService;
import com.compass.domain.common.jwt.JwtTokenProvider;
import com.compass.domain.common.exception.DuplicateEmailException;
import com.compass.domain.common.exception.InvalidTokenException;

import com.compass.domain.user.dto.LoginRequest;
import com.compass.domain.user.dto.SignUpRequest;
import com.compass.domain.user.dto.TokenResponse;
import com.compass.domain.user.entity.User;
import com.compass.domain.user.entity.UserStatus;
import com.compass.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final RedisTemplate<String, String> redisTemplate;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${spring.data.redis.prefix.verification}")
    private String verificationPrefix;

    @Transactional
    public void signUp(SignUpRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("이미 가입된 이메일입니다.");
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        User newUser = User.builder()
                .email(request.getEmail())
                .password(encodedPassword)
                .nickname(request.getEmail().split("@")[0]) // 이메일 앞부분을 초기 닉네임으로 설정
                .status(UserStatus.PENDING) // 이메일 인증 대기 상태
                .build();

        userRepository.save(newUser);

        // 1. 인증 토큰 생성
        String token = UUID.randomUUID().toString();
        String redisKey = verificationPrefix + token;

        // 2. Redis에 토큰 저장 (사용자 ID, 24시간 유효)
        redisTemplate.opsForValue().set(redisKey, newUser.getId().toString(), Duration.ofHours(24));

        // 3. 인증 이메일 발송
        String subject = "[Compass] 회원가입 인증을 완료해주세요.";
        // 실제 서비스에서는 프론트엔드 URL로 변경해야 합니다.
        String verificationUrl = "http://localhost:8080/api/users/verify?token=" + token;
        String text = "아래 링크를 클릭하여 회원가입을 완료하세요.\n" + verificationUrl;

        emailService.sendEmail(newUser.getEmail(), subject, text);
    }

    public TokenResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        String accessToken = jwtTokenProvider.createAccessToken(authentication);
        return new TokenResponse(accessToken);
    }

    @Transactional
    public void verifyEmail(String token) {
        String redisKey = verificationPrefix + token;
        String userId = redisTemplate.opsForValue().get(redisKey);

        if (userId == null) {
            throw new InvalidTokenException("유효하지 않거나 만료된 토큰입니다.");
        }

        User user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        user.updateStatus(UserStatus.ACTIVE);

        redisTemplate.delete(redisKey);
    }
}