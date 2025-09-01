package com.compass.domain.user.service;

import com.compass.common.exception.DuplicateEmailException;
import com.compass.domain.user.dto.SignUpRequest;
import com.compass.domain.user.entity.User;
import com.compass.domain.user.entity.UserStatus;
import com.compass.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void signUp(SignUpRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("이미 가입된 이메일입니다.");
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        User newUser = User.builder()
                .email(request.getEmail())
                .password(encodedPassword)
                .nickname(request.getEmail().split("@")[0]) // 이메일 앞부분을 초기 닉네임으로 설정
                .status(UserStatus.PENDING) // 이메일 인증 대기 상태
                .build();

        userRepository.save(newUser);

        // TODO: 이메일 인증 토큰 생성 및 발송 로직 구현
    }
}