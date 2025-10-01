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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

    @Value("${cors.allowed-origins:}")
    private String allowedOriginsProperty;

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

        List<String> resolvedOrigins = resolveAllowedOrigins();

        // "*" 처리: allowCredentials(true)와 함께 사용하려면 addAllowedOriginPattern 사용
        if (resolvedOrigins.size() == 1 && "*".equals(resolvedOrigins.get(0))) {
            configuration.addAllowedOriginPattern("*");
            configuration.setAllowCredentials(true);
        } else if (!resolvedOrigins.isEmpty()) {
            resolvedOrigins.forEach(configuration::addAllowedOrigin);
            configuration.setAllowCredentials(true);
        } else if ("docker".equals(activeProfile)) {
            configuration.addAllowedOriginPattern("*");
            configuration.setAllowCredentials(true);
        } else {
            configuration.addAllowedOrigin("http://localhost:3000");
            configuration.addAllowedOrigin("http://localhost:5173");
            configuration.addAllowedOrigin("http://localhost:5500");
            configuration.addAllowedOrigin("http://127.0.0.1:5500");
            configuration.setAllowCredentials(true);
        }

        configuration.addAllowedMethod("*"); // 모든 HTTP 메서드 허용
        configuration.addAllowedHeader("*"); // 모든 헤더 허용
        configuration.addExposedHeader("Authorization"); // Authorization 헤더 노출
        configuration.setMaxAge(3600L); // preflight 캐시 시간

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private List<String> resolveAllowedOrigins() {
        List<String> origins = new ArrayList<>();

        if (allowedOriginsProperty != null && !allowedOriginsProperty.isBlank()) {
            origins.addAll(Arrays.stream(allowedOriginsProperty.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .collect(Collectors.toList()));
        }

        if (!"docker".equals(activeProfile)) {
            List<String> localDefaults = List.of(
                "http://localhost:3000",
                "http://localhost:5173",
                "http://localhost:5500",
                "http://127.0.0.1:5500"
            );

            localDefaults.stream()
                .filter(origin -> !origins.contains(origin))
                .forEach(origins::add);
        }

        return origins;
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .httpBasic(httpBasic -> httpBasic.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/").permitAll()  // 루트 경로 허용
                .requestMatchers("/error").permitAll()
                .requestMatchers("/static/**", "/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()  // Static resources
                .requestMatchers("/*.html", "/*.css", "/*.js").permitAll()  // HTML, CSS, JS files
                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                .requestMatchers("/api/auth/**", "/api/v1/auth/**").permitAll()  // Authentication endpoints
                .requestMatchers("/api/users/**").permitAll()
                .requestMatchers("/api/test/**").permitAll()
                .requestMatchers("/api/debug/**").permitAll()  // Debug endpoints for testing
                .requestMatchers("/api/chat/**", "/api/v1/chat/**").permitAll()  // Chat endpoints
                .requestMatchers("/api/stage1/**").permitAll()  // Stage1 endpoints
                .requestMatchers("/api/tourplace/**").permitAll()  // TourPlace endpoints
                .requestMatchers("/api/cluster/**").permitAll()  // Cluster endpoints
                .requestMatchers("/api/kakao-places/**").permitAll()  // Kakao Places API endpoints
                .requestMatchers("/api/trips/**").permitAll()  // Trips endpoints for testing
                .requestMatchers("/api/tour/**").permitAll()  // Tour API endpoints for testing
                .requestMatchers("/api/search/**").permitAll()  // Search API endpoints
                .requestMatchers("/api/crawl/**").permitAll()  // Crawl API endpoints for testing
                .requestMatchers("/api/mock/**").permitAll()  // Mock data endpoints for Stage 2 testing
                .requestMatchers("/api/admin/**", "/api/v1/admin/**").permitAll()  // Admin endpoints for data collection
                .requestMatchers("/api/ocr/**", "/api/v1/ocr/**").permitAll()  // OCR endpoints for image processing
                .requestMatchers("/api/phase3/**").permitAll()  // Phase3 endpoints for testing
                .requestMatchers("/health").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                .anyRequest().authenticated()
            );

        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        // OAuth2 설정 비활성화 (ClientRegistrationRepository 빈이 없어서 오류 발생)
        // OAuth2는 docker 프로필이 아닐 때만 활성화
        // if (!"docker".equals(activeProfile) && customOAuth2UserService != null) {
        //     http.oauth2Login(oauth2 -> oauth2
        //             .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
        //             .successHandler(oAuth2AuthenticationSuccessHandler)
        //             .failureHandler(oAuth2AuthenticationFailureHandler));
        // }

        return http.build();
    }
}
