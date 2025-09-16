package com.compass.domain.auth.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Role {
    GUEST("ROLE_GUEST"), // 소셜 로그인 최초 가입자
    USER("ROLE_USER"),   // 일반 사용자
    ADMIN("ROLE_ADMIN"); // 관리자

    private final String key;
}