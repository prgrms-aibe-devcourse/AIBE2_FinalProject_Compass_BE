package com.compass.config.oauth;

import com.compass.config.jwt.JwtTokenProvider;
import com.compass.domain.user.entity.User;
import com.compass.domain.user.enums.Role;
import com.compass.domain.user.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String REFRESH_TOKEN_PREFIX = "RT:";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        log.info("OAuth2 Login 성공!");

        DefaultOAuth2User oAuth2User = (DefaultOAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // Kakao의 경우 email이 kakao_account 내에 있음
        String email = attributes.containsKey("email") ? (String) attributes.get("email") : (String) ((Map<String, Object>) attributes.get("kakao_account")).get("email");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        String targetUrl;
        if (user.getRole() == Role.GUEST) {
            // 신규 회원이므로 추가 정보를 입력받는 페이지로 리다이렉트
            String accessToken = jwtTokenProvider.createAccessToken(user.getEmail(), user.getId(), Collections.singletonList(user.getRole().name()));
            targetUrl = UriComponentsBuilder.fromUriString("http://localhost:3000/oauth2/signup") // 프론트엔드 추가 정보 입력 URL
                    .queryParam("token", accessToken)
                    .build()
                    .encode(StandardCharsets.UTF_8)
                    .toUriString();
            log.info("신규 유저입니다. 추가 정보 입력 페이지로 리다이렉트합니다. URL: {}", targetUrl);
        } else {
            // 기존 회원이므로 로그인 성공 처리 후 메인 페이지 등으로 리다이렉트
            String accessToken = jwtTokenProvider.createAccessToken(user.getEmail(), user.getId(), Collections.singletonList(user.getRole().name()));
            String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmail(), user.getId());

            // Redis에 Refresh Token 저장 (DB 저장 로직 대체)
            long refreshTokenExpiration = jwtTokenProvider.getRefreshTokenExpiration();
            redisTemplate.opsForValue().set(REFRESH_TOKEN_PREFIX + user.getId(), refreshToken, refreshTokenExpiration, TimeUnit.MILLISECONDS);

            targetUrl = UriComponentsBuilder.fromUriString("http://localhost:3000/login/success") // 프론트엔드 로그인 성공 URL
                    .queryParam("accessToken", accessToken)
                    .queryParam("refreshToken", refreshToken)
                    .build()
                    .encode(StandardCharsets.UTF_8)
                    .toUriString();

            log.info("기존 유저입니다. 로그인 성공. URL: {}", targetUrl);
        }

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}