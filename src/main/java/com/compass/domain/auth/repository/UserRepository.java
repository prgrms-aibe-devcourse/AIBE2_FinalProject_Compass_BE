package com.compass.domain.auth.repository;

import com.compass.domain.auth.entity.User;
import com.compass.domain.auth.enums.SocialType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findBySocialTypeAndSocialId(SocialType socialType, String socialId);
    boolean existsByEmail(String email);
}