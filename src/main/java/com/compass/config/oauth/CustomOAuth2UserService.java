package com.compass.config.oauth;

import com.compass.domain.user.entity.User;
import com.compass.domain.user.enums.SocialType;
import com.compass.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;

    private static final String KAKAO = "kakao";
    private static final String GOOGLE = "google";

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 1. 기본 OAuth2UserService를 통해 OAuth2User 정보 받아오기
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        // 2. 실제 비즈니스 로직을 처리하는 메서드 호출
        return processOAuth2User(userRequest, oAuth2User);
    }

    /**
     * 실제 OAuth2 사용자 정보를 처리하는 비즈니스 로직.
     * 테스트 용이성을 위해 public 또는 protected로 분리합니다.
     */
    protected OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        log.info("OAuth2 사용자 정보 처리 시작...");

        // 2. 소셜 서비스 구분 (google, kakao)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        SocialType socialType = getSocialType(registrationId);

        // 3. 소셜 로그인 시 키가 되는 필드 값 (PK)
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // 4. 소셜 서비스에 따른 분기 처리
        OAuthAttributes extractAttributes = OAuthAttributes.of(socialType, userNameAttributeName, attributes);

        // 5. DB에 사용자 정보 저장 또는 업데이트
        User createdUser = getUser(extractAttributes, socialType);

        // 6. DefaultOAuth2User 객체 생성 후 반환
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(createdUser.getRole().getKey())),
                attributes,
                extractAttributes.getNameAttributeKey());
    }

    private SocialType getSocialType(String registrationId) {
        switch (registrationId.toLowerCase()) {
            case GOOGLE:
                return SocialType.GOOGLE;
            case KAKAO:
                return SocialType.KAKAO;
            default:
                throw new IllegalArgumentException("지원하지 않는 소셜 로그인입니다: " + registrationId);
        }
    }

    private User getUser(OAuthAttributes attributes, SocialType socialType) {
        return userRepository.findBySocialTypeAndSocialId(socialType, attributes.getOauth2UserInfo().getId())
                .map(user -> {
                    log.info("기존 사용자입니다. UserID: {}", user.getId());
                    return user;
                })
                .orElseGet(() -> {
                    log.info("신규 사용자입니다. 저장을 시작합니다.");
                    return saveUser(attributes, socialType);
                });
    }

    private User saveUser(OAuthAttributes attributes, SocialType socialType) {
        User createdUser = attributes.toEntity(socialType, attributes.getOauth2UserInfo());
        return userRepository.save(createdUser);
    }
}