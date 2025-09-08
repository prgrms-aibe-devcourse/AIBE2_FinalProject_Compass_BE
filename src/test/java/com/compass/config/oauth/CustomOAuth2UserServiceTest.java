package com.compass.config.oauth;

import com.compass.domain.user.entity.User;
import com.compass.domain.user.enums.Role;
import com.compass.domain.user.enums.SocialType;
import com.compass.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @InjectMocks
    private CustomOAuth2UserService customOAuth2UserService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OAuth2UserRequest userRequest;

    private ClientRegistration clientRegistration;
    private Map<String, Object> attributes;

    @BeforeEach
    void setUp() {
        // Mock ClientRegistration 설정
        clientRegistration = ClientRegistration.withRegistrationId("google")
                .clientId("test-client-id")
                .clientSecret("test-client-secret")
                .clientAuthenticationMethod(org.springframework.security.oauth2.core.ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("profile", "email")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://www.googleapis.com/oauth2/v4/token")
                .userInfoUri("https://www.googleapis.com/oauth2/v1/userinfo")
                .userNameAttributeName("sub")
                .clientName("Google")
                .build();

        // Mock 사용자 속성 설정
        attributes = new HashMap<>();
        attributes.put("sub", "123456789");
        attributes.put("name", "테스트유저");
        attributes.put("email", "test@example.com");
    }

    @Test
    @DisplayName("신규 소셜 로그인 사용자 - GUEST로 저장")
    void loadUser_whenNewUser_thenSaveAsGuest() {
        // given
        // Mock OAuth2UserRequest
        when(userRequest.getClientRegistration()).thenReturn(clientRegistration);

        // Mock DefaultOAuth2UserService의 반환값 (실제 네트워크 호출 대신)
        OAuth2User oAuth2User = new DefaultOAuth2User(Collections.emptyList(), attributes, "sub");

        // DB에 해당 유저가 없음
        when(userRepository.findBySocialTypeAndSocialId(SocialType.GOOGLE, "123456789")).thenReturn(Optional.empty());

        // save 메서드가 호출되면 User 객체를 반환하도록 설정
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        DefaultOAuth2User result = (DefaultOAuth2User) customOAuth2UserService.processOAuth2User(userRequest, oAuth2User);

        // then
        // DB 조회 및 저장 메서드가 호출되었는지 검증
        verify(userRepository, times(1)).findBySocialTypeAndSocialId(SocialType.GOOGLE, "123456789");
        verify(userRepository, times(1)).save(any(User.class));

        // 반환된 사용자의 권한이 GUEST인지 확인
        assertThat(result.getAuthorities().iterator().next().getAuthority()).isEqualTo(Role.GUEST.getKey());
    }

    @Test
    @DisplayName("기존 소셜 로그인 사용자 - 정보 조회")
    void loadUser_whenExistingUser_thenLoadUser() {
        // given
        when(userRequest.getClientRegistration()).thenReturn(clientRegistration);
        OAuth2User oAuth2User = new DefaultOAuth2User(Collections.emptyList(), attributes, "sub");

        // DB에 이미 유저가 존재함 (Role=USER)
        User existingUser = User.builder()
                .email("test@example.com")
                .nickname("테스트유저")
                .role(Role.USER)
                .socialType(SocialType.GOOGLE)
                .socialId("123456789")
                .build();
        when(userRepository.findBySocialTypeAndSocialId(SocialType.GOOGLE, "123456789")).thenReturn(Optional.of(existingUser));

        // when
        DefaultOAuth2User result = (DefaultOAuth2User) customOAuth2UserService.processOAuth2User(userRequest, oAuth2User);

        // then
        // DB 조회 메서드는 호출되었지만, save는 호출되지 않았는지 검증
        verify(userRepository, times(1)).findBySocialTypeAndSocialId(SocialType.GOOGLE, "123456789");
        verify(userRepository, never()).save(any(User.class));

        // 반환된 사용자의 권한이 기존 권한(USER)인지 확인
        assertThat(result.getAuthorities().iterator().next().getAuthority()).isEqualTo(Role.USER.getKey());
    }
}