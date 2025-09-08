# Redis 전환 전략 및 테스트 가이드

## 개요

현재 프로젝트는 Redis 인프라가 준비되지 않은 상태에서도 개발이 가능하도록 설계되었습니다.
Redis가 준비되면 코드 변경 없이 프로파일 전환만으로 즉시 사용 가능합니다.

## 테스트 분류 전략

### @Tag 어노테이션 시스템

JUnit 5의 @Tag 어노테이션을 사용하여 테스트를 분류합니다:

```java
@Tag("unit")  // 단위 테스트 - Redis 불필요
class SimpleKeywordDetectorTest {
    // 비즈니스 로직만 테스트
    // Mock 객체 사용
    // 외부 의존성 없음
}

@Tag("integration")  // 통합 테스트 - Redis 필요
class ChatIntegrationTest {
    // 실제 Spring Context 로드
    // Redis, DB 등 외부 시스템 연동
    // End-to-End 테스트
}
```

### Gradle 태스크 분리

`build.gradle`에서 태그별 실행 태스크 정의:

```gradle
// 단위 테스트만 실행 (Redis 없이)
task unitTest(type: Test) {
    useJUnitPlatform {
        includeTags 'unit'  // unit 태그가 있는 테스트만 실행
    }
    systemProperty 'spring.profiles.active', 'test-no-redis'
}

// 통합 테스트만 실행 (Redis 필요)
task integrationTest(type: Test) {
    useJUnitPlatform {
        includeTags 'integration'  // integration 태그가 있는 테스트만 실행
    }
    systemProperty 'spring.profiles.active', 'test'
}
```

## Redis 전환 시나리오

### 현재 상태 (Redis 없음)

```bash
# 단위 테스트만 실행
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.16/libexec/openjdk.jdk/Contents/Home ./gradlew unitTest

# 애플리케이션 실행 (Redis 기능 비활성화)
./gradlew bootRun --args='--spring.profiles.active=local-no-redis'
```

**application-test-no-redis.yml** 설정:
```yaml
spring:
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
      - org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration
      - org.springframework.ai.autoconfigure.vectorstore.redis.RedisVectorStoreAutoConfiguration
```

### 전환 후 (Redis 사용)

```bash
# 모든 테스트 실행 (단위 + 통합)
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.16/libexec/openjdk.jdk/Contents/Home ./gradlew test

# 통합 테스트만 실행
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.16/libexec/openjdk.jdk/Contents/Home ./gradlew integrationTest

# 애플리케이션 실행 (Redis 활성화)
docker-compose up -d redis  # Redis 컨테이너 시작
./gradlew bootRun  # 기본 프로파일로 실행
```

## 프로파일 전략

### 개발 환경별 프로파일

| 프로파일 | Redis | 용도 | 사용 시기 |
|---------|-------|------|----------|
| `test-no-redis` | ❌ | 단위 테스트 | Redis 인프라 없을 때 |
| `test` | ✅ | 통합 테스트 | Redis 준비 완료 후 |
| `local-no-redis` | ❌ | 로컬 개발 | Redis 없이 개발 |
| `local` | ✅ | 로컬 개발 | Redis와 함께 개발 |
| `prod` | ✅ | 프로덕션 | 실제 운영 환경 |

### 프로파일 전환 방법

```bash
# 프로파일 지정 실행
./gradlew bootRun --args='--spring.profiles.active=프로파일명'

# 환경 변수로 지정
export SPRING_PROFILES_ACTIVE=프로파일명
./gradlew bootRun
```

## CI/CD 파이프라인 전략

### 단계별 실행

```yaml
# .github/workflows/ci.yml
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

## 코드 작성 가이드라인

### 테스트 작성 시

```java
// 단위 테스트 예시
@Tag("unit")  // 반드시 태그 추가
@DisplayName("키워드 감지 단위 테스트")
class SimpleKeywordDetectorTest {
    // Redis 의존성 없는 순수 로직 테스트
}

// 통합 테스트 예시
@Tag("integration")  // 반드시 태그 추가
@SpringBootTest
@ActiveProfiles("test")  // Redis 포함 프로파일
class ChatControllerIntegrationTest {
    // 실제 Redis 연동 테스트
}
```

### Redis 의존 코드 작성 시

```java
@Service
@Profile("!test-no-redis & !local-no-redis")  // Redis 없는 프로파일에서는 비활성화
public class RedisVectorStoreService {
    // Redis 의존 서비스
}

@Service
@Profile("test-no-redis | local-no-redis")  // Redis 없는 프로파일에서만 활성화
public class InMemoryVectorStoreService {
    // Redis 대체 구현 (Mock)
}
```

## 장점

1. **즉시 전환 가능**: Redis 준비되면 프로파일만 변경
2. **개발 독립성**: Redis 없어도 개발 진행 가능
3. **CI/CD 유연성**: 인프라 상태에 따라 선택적 테스트
4. **팀 협업**: 각 팀이 독립적으로 개발 가능
5. **점진적 통합**: 단위 테스트 → 통합 테스트 순차 진행

## 주의사항

- 새로운 테스트 작성 시 반드시 @Tag 추가
- Redis 의존 코드는 프로파일로 분리
- 통합 테스트는 실제 Redis 연동 확인 필수
- 프로덕션 배포 전 모든 테스트 통과 확인

## 팀별 권장사항

| 팀 | 현재 상태 | 권장 작업 |
|----|----------|----------|
| CHAT2 | ✅ 완료 | 단위 테스트 100% 통과 |
| USER | Redis 의존 | @Tag 추가, Mock 서비스 구현 |
| TRIP1 | Redis 의존 | @Tag 추가, 프로파일 분리 |
| CHAT1 | Redis 의존 | @Tag 추가, 테스트 분리 |