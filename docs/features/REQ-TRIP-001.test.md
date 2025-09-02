---
name: 테스트 명세서
about: 기능 명세서에 대한 테스트 케이스를 정의하는 문서
title: '[TEST] REQ-TRIP-001 | 여행 계획 생성 API 테스트'
labels: '백엔드'
assignees: 'TRIP1'
---

# 🧪 통합 테스트 작성

## 📋 테스트 정보

### 테스트 대상
- **클래스명**: `TripControllerTest`
- **메서드명**: `createTrip()`, `createTripWithInvalidRequest()`
- **파일 경로**: `src/test/java/com/compass/domain/trip/controller/TripControllerTest.java`

### 테스트 목적
> `POST /api/trips` API의 전체 흐름이 올바르게 동작하는지 검증합니다.
> 
- 유효한 요청에 대해 HTTP `201 Created` 응답과 함께 데이터가 DB에 정상적으로 저장되는지 확인합니다.
- 유효하지 않은 요청(필수값 누락)에 대해 HTTP `400 Bad Request` 응답을 반환하며 요청을 거부하는지 확인합니다.

---

## 🎯 테스트 케이스

### 정상 케이스 (Happy Path)
- [x] **케이스 1**: 유효한 데이터로 여행 계획 생성
    - **입력**: `userId`, `title`, `destination` 등 모든 필수 정보를 포함한 `TripCreate.Request` DTO.
    - **예상 결과**: HTTP Status `201 Created`가 반환되고, 응답 본문에 `id`와 `tripUuid`가 포함됩니다. `TripRepository`로 조회 시 요청된 데이터가 DB에 저장되어 있습니다.
    - **설명**: API의 핵심 기능이 정상적으로 동작하는지 검증하는 가장 기본적인 시나리오입니다.

### 예외 케이스 (Exception Cases)
- [x]  **잘못된 입력**: 필수값이 누락된 데이터로 여행 계획 생성 시도
    - **입력**: 모든 필드가 `null`인 `TripCreate.Request` DTO.
    - **예상 결과**: HTTP Status `400 Bad Request`가 반환됩니다.
    - **설명**: `@Valid` 어노테이션을 통한 입력값 검증 기능이 올바르게 동작하는지 확인합니다.

---

## 🔧 테스트 환경 설정

### 새로 추가된 설정 파일
- **`src/main/resources/application-h2.yml`**: H2 데이터베이스 프로필 설정
- **`src/main/java/com/compass/config/TestSecurityConfig.java`**: H2 프로필용 Security 비활성화 설정

### 의존성
```gradle
// build.gradle
dependencies {
    // ...
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'com.h2database:h2'
    
    // H2 for development/testing (also available at runtime)
    runtimeOnly 'com.h2database:h2'
}

// 테스트 실행 시 성공한 테스트도 표시
tasks.named('test') {
    useJUnitPlatform()
    
    testLogging {
        events "passed", "skipped", "failed"
        showStandardStreams = false
        exceptionFormat = "full"
    }
}
```

### Mock 설정
- **데이터베이스**: 실제 DB 대신 H2 인메모리(In-memory) 데이터베이스를 사용하여 테스트를 격리합니다. (`src/test/resources/application.yml`)

### 테스트 데이터
```java
// TripControllerTest.java
TripCreate.Request.Activity activity = new TripCreate.Request.Activity(
        LocalTime.of(9, 0), "경복궁", "관광지", "조선 왕조의 법궁",
        3000, "서울특별시 종로구 사직로 161", 37.579617, 126.977041,
        "한복을 입으면 무료 입장", 1
);

TripCreate.Request.DailyPlan dailyPlan = new TripCreate.Request.DailyPlan(
        1, LocalDate.of(2025, 9, 10), List.of(activity)
);

TripCreate.Request request = new TripCreate.Request(
        1L, 101L, "서울 3박 4일 여행", "서울",
        LocalDate.of(2025, 9, 10), LocalDate.of(2025, 9, 13),
        2, 1000000, List.of(dailyPlan)
);
```

---

## 📝 테스트 코드 구조

### 테스트 파일명
- `TripControllerTest.java`

### 기본 구조
```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class TripControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private TripRepository tripRepository;

    @DisplayName("새로운 여행 계획을 생성한다.")
    @Test
    @WithMockUser
    void createTrip() throws Exception {
      // Given (준비)
      TripCreate.Request request = // ... 테스트 데이터 생성

      // When (실행) & Then (검증)
      mockMvc.perform(post("/api/trips")
                      .content(objectMapper.writeValueAsString(request))
                      .contentType(MediaType.APPLICATION_JSON)
                      .with(csrf()) // CSRF 토큰 추가
              )
              .andExpect(status().isCreated());
      
      // DB 검증
      List<Trip> trips = tripRepository.findAll();
      assertThat(trips).hasSize(1);
    }
    
    @DisplayName("필수값이 누락되면 여행 계획 생성에 실패한다.")
    @Test
    @WithMockUser
    void createTripWithInvalidRequest() throws Exception {
      // Given (준비)
      TripCreate.Request request = // ... 유효하지 않은 테스트 데이터 생성

      // When (실행) & Then (검증)
      mockMvc.perform(post("/api/trips")
                      .content(objectMapper.writeValueAsString(request))
                      .contentType(MediaType.APPLICATION_JSON)
                      .with(csrf()) // CSRF 토큰 추가
              )
              .andExpect(status().isBadRequest());
    }
}
```

---

## 🚀 실행 방법

### 테스트 실행
```bash
./gradlew test --tests "com.compass.domain.trip.controller.TripControllerTest" --rerun-tasks
```

### Swagger UI 테스트
```bash
# 애플리케이션 실행 (H2 프로필)
./gradlew bootRun --args='--spring.profiles.active=h2'

# Swagger UI 접속
# http://localhost:8080/swagger-ui.html
```

---

## 📊 테스트 결과

### ✅ 성공한 테스트

#### JUnit 통합 테스트
- **`createTrip()`**: 유효한 데이터로 여행 계획 생성 테스트 **성공**
  - HTTP 응답: `201 Created` 
  - 데이터베이스에 `Trip`과 `TripDetail` 정상 저장 확인
- **`createTripWithInvalidRequest()`**: 유효성 검증 테스트 **성공**
  - HTTP 응답: `400 Bad Request`
  - 필수값 누락 시 적절한 오류 응답 확인

#### Swagger UI 수동 테스트
- **✅ Swagger UI 접속**: http://localhost:8080/swagger-ui.html 정상 접근
- **✅ API 문서화**: `POST /api/trips` 엔드포인트 완벽 표시
- **✅ Request Schema**: 모든 필드 및 검증 규칙 정확히 표시
- **✅ 유효성 검증**: 과거 날짜 입력 시 `400 Bad Request` 정상 응답
  ```json
  {
    "message": "Validation failed for object='request'. Error count: 2",
    "errors": [
      {
        "field": "startDate",
        "defaultMessage": "현재 또는 미래의 날짜여야 합니다",
        "rejectedValue": "2024-09-01"
      },
      {
        "field": "endDate", 
        "defaultMessage": "현재 또는 미래의 날짜여야 합니다",
        "rejectedValue": "2024-09-04"
      }
    ]
  }
  ```
- **✅ API 기능**: 미래 날짜 입력 시 정상 동작 확인

### ⚠️ 발견된 문제점과 해결책

#### 1. Spring Security 인증 문제
**문제**: POST 요청이 `401 Unauthorized` 응답을 받음
**원인**: Spring Security가 기본 인증을 요구하여 유효성 검증까지 도달하지 못함
**해결**: 테스트 메서드에 `@WithMockUser` 추가
```java
@Test
@WithMockUser
void createTrip() throws Exception {
    // 테스트 코드
}
```

#### 2. Spring Security CSRF 보호
**문제**: POST 요청이 `403 Forbidden` 응답을 받음  
**원인**: Spring Security가 CSRF 토큰 없는 요청을 차단
**해결**: 테스트에 `.with(csrf())` 추가
```java
mockMvc.perform(post("/api/trips")
    .content(objectMapper.writeValueAsString(request))
    .contentType(MediaType.APPLICATION_JSON)
    .with(csrf()) // CSRF 토큰 추가
)
```

#### 3. H2 데이터베이스 JSONB 타입 미지원
**문제**: `Unknown data type: "JSONB"` 오류 발생
**원인**: H2는 PostgreSQL의 JSONB 타입을 지원하지 않음
**해결**: H2 PostgreSQL 호환 모드 활성화
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE
```

### 🎯 최종 결과
- **✅ JUnit 테스트**: 2개 테스트 모두 성공
- **✅ Swagger UI 테스트**: API 문서화 및 수동 테스트 성공
- **🔄 반복 실행**: 안정적으로 성공 (상태 공유 문제 해결됨)
- **⏱️ 실행 시간**: 약 8-11초 (첫 실행), 약 38초 (재컴파일 시)
- **📅 최종 검증**: 2025-09-02 17:45 - Swagger UI 테스트 완료

### 📺 테스트 실행 결과
```
> Task :test

TripControllerTest > 필수값이 누락되면 여행 계획 생성에 실패한다. PASSED

TripControllerTest > 새로운 여행 계획을 생성한다. PASSED

BUILD SUCCESSFUL in 38s
5 actionable tasks: 5 executed
```

### 📋 적용된 최종 설정
- **Spring Security**: `@WithMockUser` + `csrf()` 토큰
- **테스트 격리**: `@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)`
- **H2 데이터베이스**: PostgreSQL 호환 모드 (`jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE`)
- **테스트 프레임워크**: MockMvc를 사용한 통합 테스트
- **테스트 로깅**: 성공/실패/건너뜀 모든 테스트 결과 표시 (`events "passed", "skipped", "failed"`)
