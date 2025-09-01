# 도메인 패키지 구조 가이드

## 📁 패키지 구조

```
domain/
├── user/          # USER 도메인 (인증/인가, 프로필)
│   ├── entity/
│   ├── dto/
│   ├── repository/
│   ├── service/
│   └── controller/
├── chat/          # CHAT 도메인 (채팅, 메시지)
│   ├── entity/
│   ├── dto/
│   ├── repository/
│   ├── service/
│   └── controller/
└── trip/          # TRIP 도메인 (여행 계획, 추천)
    ├── entity/
    ├── dto/
    ├── repository/
    ├── service/
    └── controller/
```

## 🎯 도메인별 담당자

| 도메인 | 담당 기능 | 브랜치 이름 예시 |
|--------|-----------|------------------|
| USER | 회원가입, 로그인, JWT, 프로필 | `feature/user-auth` |
| CHAT | 채팅방, 메시지 CRUD, LLM 통합 | `feature/chat-core` |
| TRIP | 여행 계획, 추천, 날씨 API | `feature/trip-planning` |

## 💡 개발 가이드

### 1. Entity 생성 예시
```java
package com.compass.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    // ... other fields
}
```

### 2. Service 생성 예시
```java
package com.compass.domain.user.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    
    // Service methods
}
```

### 3. Controller 생성 예시
```java
package com.compass.domain.user.controller;

import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    
    // API endpoints
}
```

## 📝 명명 규칙

- **Entity**: 단수형 (User, ChatThread, Trip)
- **Repository**: Entity명 + Repository (UserRepository)
- **Service**: Entity명 + Service (UserService)
- **Controller**: Entity명 + Controller (UserController)
- **DTO**: 용도 + Request/Response (LoginRequest, UserResponse)

## 🔄 브랜치 전략

1. `main`에서 최신 코드 pull
2. 도메인별 feature 브랜치 생성
3. 작업 완료 후 `develop`으로 PR
4. 코드 리뷰 후 merge

## ⚠️ 주의사항

- 다른 도메인의 코드를 직접 수정하지 마세요
- 도메인 간 통신은 Service 레이어를 통해서만
- 공통 기능은 `com.compass.common` 패키지에 추가
- DB 스키마 변경 시 팀원들과 공유 필수