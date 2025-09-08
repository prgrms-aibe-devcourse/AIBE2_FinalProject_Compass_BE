# CI/CD 테스트 가이드

## 현재 테스트 실패 원인 분석 (2024.01.07)

### 전체 테스트 현황
- **총 테스트**: 209개
- **성공**: 143개 (80%)
- **실패**: 34개
- **스킵**: 32개

### 실패 원인 분석

#### 1. Redis 연결 문제 (주요 원인)
대부분의 테스트 실패가 Redis 연결 거부로 인해 발생
- `java.net.ConnectException: Connection refused`
- EmbeddedRedis가 제대로 시작되지 않음
- ApplicationContext 로드 실패로 인한 연쇄 실패

#### 2. 도메인별 실패 현황

| 도메인 | 실패 개수 | 담당팀 | 원인 |
|--------|-----------|--------|------|
| USER | 8개 | USER팀 | Redis 연결 실패 |
| TRIP | 16개 | TRIP1팀 | Redis 연결 실패 |
| CHAT | 8개 | CHAT1팀 | Redis/템플릿 서비스 |
| CHAT2 코드 | 0개 | CHAT2팀 | **모두 성공 ✅** |

### CHAT2팀 작성 코드 테스트 결과
- `SimpleKeywordDetectorTest`: 24/24 성공 (100%)
- `TravelHistoryTest`: 14/14 성공 (100%)

## 해결 방안

### 1. 즉시 적용 완료 ✅

#### 테스트 분리 전략
```bash
# 단위 테스트만 실행 (Redis 불필요)
./gradlew unitTest

# 통합 테스트만 실행 (Redis 필요)
./gradlew integrationTest

# 전체 테스트 실행
./gradlew test
```

#### 구현 내용
1. **테스트 태그 추가**: `@Tag("unit")` / `@Tag("integration")`
2. **테스트 프로파일 분리**: `test-no-redis` 프로파일 생성
3. **Gradle 태스크 분리**: unitTest, integrationTest 태스크 추가

### 2. 테스트 작성 가이드라인

#### 단위 테스트 (Redis 불필요)
```java
@Tag("unit")
class MyServiceTest {
    // 순수 비즈니스 로직 테스트
    // Mock 객체 사용
    // 외부 의존성 없음
}
```

#### 통합 테스트 (Redis 필요)
```java
@Tag("integration")
@SpringBootTest
@ActiveProfiles("test")
class MyControllerTest {
    // 실제 Spring Context 로드
    // Redis, DB 등 외부 시스템 연동
}
```

### 3. CI 파이프라인 개선 제안

#### GitHub Actions 워크플로우
```yaml
jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - name: Run Unit Tests
        run: ./gradlew unitTest
        # Redis 없이 빠르게 실행
  
  integration-tests:
    runs-on: ubuntu-latest
    services:
      redis:
        image: redis:7-alpine
        options: --health-cmd "redis-cli ping"
    steps:
      - name: Run Integration Tests
        run: ./gradlew integrationTest
        # Redis 컨테이너와 함께 실행
```

### 4. 로컬 개발 환경 설정

#### Redis 없이 개발하기
```bash
# application-local-no-redis.yml 사용
./gradlew bootRun --args='--spring.profiles.active=local-no-redis'
```

#### Docker Compose 활용
```bash
# Redis만 실행
docker-compose up -d redis

# 전체 스택 실행
docker-compose up -d
```

## 팀 협업 가이드

### 각 팀별 대응 방안

#### CHAT2팀 (우리팀)
- [x] 단위 테스트 태그 추가 완료
- [x] Redis 독립적인 테스트 작성
- [x] 테스트 100% 통과 확인

#### 다른 팀 권장사항
1. **USER팀**: Controller 테스트를 단위 테스트로 분리
2. **TRIP1팀**: Repository 테스트에 @DataJpaTest 활용
3. **CHAT1팀**: 템플릿 서비스 Mock 처리

### 테스트 실행 명령어 모음

```bash
# CHAT2팀 테스트만 실행
./gradlew test --tests "com.compass.domain.chat.detector.*"
./gradlew test --tests "com.compass.domain.trip.entity.*"

# Redis 없이 단위 테스트만
./gradlew unitTest

# 특정 테스트 클래스만
./gradlew test --tests SimpleKeywordDetectorTest
./gradlew test --tests TravelHistoryTest

# 빌드 (테스트 제외)
./gradlew build -x test
```

## 문제 발생 시 연락처

- CHAT2팀: @kmj (현재 문서 작성자)
- Redis 설정: DevOps팀
- CI/CD 파이프라인: 인프라팀

## 참고 자료

- [Spring Boot Testing Best Practices](https://spring.io/guides/gs/testing-web/)
- [Testcontainers for Integration Testing](https://www.testcontainers.org/)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)