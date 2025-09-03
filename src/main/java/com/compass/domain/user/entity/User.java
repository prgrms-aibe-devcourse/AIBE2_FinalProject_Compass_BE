package com.compass.domain.user.entity;

import com.compass.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class User extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    private String name;
    
    private String nickname;
    
    private String password;
    
    @Enumerated(EnumType.STRING)
    private Role role;
    
    private String refreshToken;

    // Builder 생성자
    @Builder
    public User(String email, String name, String nickname, String password, Role role) {
        this.email = email;
        this.name = name;
        this.nickname = nickname;
        this.password = password;
        this.role = role != null ? role : Role.USER;
    }
    
    // 기본 생성자 (테스트용)
    public User(String email, String name) {
        this.email = email;
        this.name = name;
        this.role = Role.USER;
    }
    
    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}