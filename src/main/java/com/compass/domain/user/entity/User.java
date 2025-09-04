package com.compass.domain.user.entity;

import com.compass.domain.user.enums.Role;
import com.compass.domain.user.enums.SocialType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    private String password; // 소셜 로그인은 비밀번호가 없을 수 있음

    @Column(nullable = false)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    private SocialType socialType; // KAKAO, GOOGLE

    private String socialId; // 소셜 로그인 ID

    @Builder
    public User(String email, String password, String nickname, Role role, SocialType socialType, String socialId) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.role = role != null ? role : Role.USER;
        this.socialType = socialType;
        this.socialId = socialId;
    }


}