package com.compass.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.compass.config.jwt.JwtAuthenticationFilter;
import com.compass.config.jwt.JwtTokenProvider;
import com.compass.config.oauth.CustomOAuth2UserService;
import com.compass.config.oauth.OAuth2AuthenticationFailureHandler;
import com.compass.config.oauth.OAuth2AuthenticationSuccessHandler;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired(required = false)
    private CustomOAuth2UserService customOAuth2UserService;

    @Autowired(required = false)
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    @Autowired(required = false)
    private OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtTokenProvider, redisTemplate);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        if ("docker".equals(activeProfile)) {
            configuration.addAllowedOriginPattern("*"); // Docker 환경에서 모든 origin 허용
            configuration.setAllowCredentials(true); // 쿠키 포함 요청 허용
        } else {
            configuration.addAllowedOrigin("http://localhost:3000"); // React 개발 서버
            configuration.addAllowedOrigin("http://localhost:5173"); // Vite 개발 서버
            configuration.addAllowedOrigin("http://localhost:5500"); // VS Code Live Server
            configuration.addAllowedOrigin("http://127.0.0.1:5500"); // VS Code Live Server (127.0.0.1)
            configuration.setAllowCredentials(true); // 쿠키 포함 요청 허용
        }

        configuration.addAllowedMethod("*"); // 모든 HTTP 메서드 허용
        configuration.addAllowedHeader("*"); // 모든 헤더 허용
        configuration.addExposedHeader("Authorization"); // Authorization 헤더 노출
        configuration.setMaxAge(3600L); // preflight 캐시 시간

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .httpBasic(httpBasic -> httpBasic.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/error").permitAll()
                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                .requestMatchers("/api/auth/**", "/api/v1/auth/**").permitAll()  // Authentication endpoints
                .requestMatchers("/api/users/**").permitAll()
                .requestMatchers("/api/test/**").permitAll()
                .requestMatchers("/api/debug/**").permitAll()  // Debug endpoints for testing
                .requestMatchers("/api/chat/**", "/api/v1/chat/**").permitAll()  // Chat endpoints
                .requestMatchers("/api/trips/**").permitAll()  // Trips endpoints for testing
                .requestMatchers("/api/tour/**").permitAll()  // Tour API endpoints for testing
                .requestMatchers("/api/search/**").permitAll()  // Search API endpoints
                .requestMatchers("/api/crawl/**").permitAll()  // Crawl API endpoints for testing
                .requestMatchers("/health").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                .anyRequest().authenticated()
            );

        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        // OAuth2는 docker 프로필이 아닐 때만 활성화
        if (!"docker".equals(activeProfile) && customOAuth2UserService != null) {
            http.oauth2Login(oauth2 -> oauth2
                    .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                    .successHandler(oAuth2AuthenticationSuccessHandler)
                    .failureHandler(oAuth2AuthenticationFailureHandler));
        }

        return http.build();
    }
}