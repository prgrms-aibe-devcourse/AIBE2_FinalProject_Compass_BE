package com.compass.domain.user.entity;

import com.compass.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    private String name;

    // 기본 생성자 (테스트용)
    public User(String email, String name) {
        this.email = email;
        this.name = name;
    }
}