---
name: 테스트 명세서
about: 기능 명세서에 대한 테스트 케이스를 정의하는 문서
title: '[TEST] REQ-TRIP-002 | 여행 계획 조회 API 테스트'
labels: '백엔드'
assignees: 'TRIP1'
---

# 🧪 통합 테스트 작성

## 📋 테스트 정보

### 테스트 대상
- **클래스명**: `TripControllerTest`
- **메서드명**: `getTripById()`, `getTripByIdNotFound()`, `getTripsByUserId()`, `getTripsByUserIdEmpty()`
- **파일 경로**: `src/test/java/com/compass/domain/trip/controller/TripControllerTest.java`

### 테스트 목적
> `GET /api/trips/{tripId}`와 `GET /api/trips` API의 전체 흐름이 올바르게 동작하는지 검증합니다.
> 
- 특정 여행 계획 조회 시 HTTP `200 OK` 응답과 함께 상세 정보를 반환하는지 확인합니다.
- 존재하지 않는 여행 계획 조회 시 HTTP `404 Not Found` 응답을 반환하는지 확인합니다.
- 사용자별 여행 계획 목록 조회 시 페이징된 결과를 정상 반환하는지 확인합니다.
- 존재하지 않는 사용자 ID로 조회 시 빈 목록을 반환하는지 확인합니다.

---

## 🎯 테스트 케이스

### 정상 케이스 (Happy Path)
- [x] **케이스 1**: 존재하는 여행 계획 상세 조회
    - **전제조건**: 먼저 여행 계획을 생성한 후 해당 ID로 조회
    - **입력**: 생성된 여행 계획의 `tripId`
    - **예상 결과**: HTTP Status `200 OK`가 반환되고, 응답 본문에 여행 계획의 상세 정보(일자별 계획 포함)가 포함됩니다.
    - **설명**: 여행 계획 상세 조회 API의 핵심 기능이 정상적으로 동작하는지 검증합니다.

- [x] **케이스 2**: 사용자의 여행 계획 목록 조회
    - **전제조건**: 먼저 여행 계획을 생성한 후 사용자 ID로 목록 조회
    - **입력**: 생성한 사용자의 `userId`와 페이징 파라미터
    - **예상 결과**: HTTP Status `200 OK`가 반환되고, 페이징된 여행 계획 목록이 포함됩니다.
    - **설명**: 사용자별 여행 계획 목록 조회 및 페이징 기능이 정상적으로 동작하는지 검증합니다.

### 예외 케이스 (Exception Cases)
- [x] **존재하지 않는 여행 계획 조회**: 존재하지 않는 ID로 여행 계획 조회 시도
    - **입력**: 존재하지 않는 `tripId` (예: 999L)
    - **예상 결과**: HTTP Status `404 Not Found`가 반환되고, 적절한 에러 메시지가 포함됩니다.
    - **설명**: `TripNotFoundException` 예외 처리가 올바르게 동작하는지 확인합니다.

- [x] **존재하지 않는 사용자 목록 조회**: 존재하지 않는 사용자 ID로 목록 조회 시도
    - **입력**: 존재하지 않는 `userId` (예: 999L)
    - **예상 결과**: HTTP Status `200 OK`가 반환되지만, 빈 목록(`content: []`)이 포함됩니다.
    - **설명**: 존재하지 않는 사용자의 경우 빈 목록을 반환하는 것이 정상 동작임을 확인합니다.

---

## 🔧 테스트 환경 설정

### 기존 설정 파일 활용
- **`src/main/resources/application-h2.yml`**: H2 데이터베이스 프로필 설정 (기존)
- **`src/main/java/com/compass/config/TestSecurityConfig.java`**: H2 프로필용 Security 비활성화 설정 (기존)
- **`src/test/resources/application.yml`**: 테스트용 H2 데이터베이스 설정 (기존)

### 추가된 DTO 클래스
```java
// TripDetail.java - 상세 조회용 Response DTO
public record Response(
    Long id, UUID tripUuid, Long userId, Long threadId,
    String title, String destination, LocalDate startDate, LocalDate endDate,
    Integer numberOfPeople, Integer totalBudget, TripStatus status,
    Integer version, LocalDateTime createdAt, LocalDateTime updatedAt,
    List<DailyPlan> dailyPlans
) { ... }

// TripList.java - 목록 조회용 Response DTO  
public record Response(
    Long id, UUID tripUuid, String title, String destination,
    LocalDate startDate, LocalDate endDate, Integer numberOfPeople,
    Integer totalBudget, TripStatus status, LocalDateTime createdAt
) { ... }
```

### 테스트 데이터
```java
// 테스트용 사용자 생성
@BeforeEach
void setUp() {
    testUser = User.builder()
            .email("test@example.com")
            .password("password")
            .nickname("테스트유저")
            .role(Role.USER)
            .build();
    testUser = userRepository.save(testUser);
}

// 여행 계획 생성 데이터 (미래 날짜로 설정)
TripCreate.Request request = new TripCreate.Request(
        testUser.getId(), 101L, "서울 3박 4일 여행", "서울",
        LocalDate.of(2025, 12, 1), LocalDate.of(2025, 12, 4),
        2, 1000000, List.of(dailyPlan)
);
```

---

## 📝 테스트 코드 구조

### 테스트 파일명
- `TripControllerTest.java` (기존 파일에 조회 테스트 추가)

### 추가된 테스트 메서드
```java
@DisplayName("존재하는 여행 계획을 조회한다.")
@Test
@WithMockUser
void getTripById() throws Exception {
    // Given - 여행 계획 생성
    var createResult = mockMvc.perform(post("/api/trips")...)
            .andExpect(status().isCreated())
            .andReturn();
    
    // 생성된 여행 계획의 ID를 응답에서 추출
    String responseContent = createResult.getResponse().getContentAsString();
    var responseJson = objectMapper.readTree(responseContent);
    Long tripId = responseJson.get("id").asLong();

    // When & Then - 여행 계획 조회
    mockMvc.perform(get("/api/trips/{tripId}", tripId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(tripId))
            .andExpect(jsonPath("$.title").value("서울 3박 4일 여행"))
            .andExpect(jsonPath("$.dailyPlans").isArray());
}

@DisplayName("존재하지 않는 여행 계획을 조회하면 404 에러가 발생한다.")
@Test
@WithMockUser
void getTripByIdNotFound() throws Exception {
    // Given
    Long nonExistentTripId = 999L;

    // When & Then
    mockMvc.perform(get("/api/trips/{tripId}", nonExistentTripId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("TRIP_NOT_FOUND"))
            .andExpect(jsonPath("$.message").exists());
}

@DisplayName("사용자의 여행 계획 목록을 조회한다.")
@Test
@WithMockUser
void getTripsByUserId() throws Exception {
    // Given - 여행 계획 생성
    mockMvc.perform(post("/api/trips")...)
            .andExpect(status().isCreated());

    // When & Then - 목록 조회
    mockMvc.perform(get("/api/trips")
                    .param("userId", testUser.getId().toString())
                    .param("page", "0")
                    .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].title").value("서울 3박 4일 여행"))
            .andExpected(jsonPath("$.totalElements").value(1));
}
```

---

## 🚀 실행 방법

### 테스트 실행
```bash
# 전체 TripController 테스트 실행
./gradlew test --tests "com.compass.domain.trip.controller.TripControllerTest" --rerun-tasks

# 특정 조회 테스트만 실행
./gradlew test --tests "com.compass.domain.trip.controller.TripControllerTest.getTripById"
./gradlew test --tests "com.compass.domain.trip.controller.TripControllerTest.getTripsByUserId"
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

#### JUnit 통합 테스트 (2025-09-03 실행)
- **`getTripById()`**: 존재하는 여행 계획 상세 조회 테스트 **성공**
  - HTTP 응답: `200 OK` 
  - 여행 계획 상세 정보와 일자별 계획 정상 반환 확인
- **`getTripByIdNotFound()`**: 존재하지 않는 여행 계획 조회 테스트 **성공**
  - HTTP 응답: `404 Not Found`
  - 적절한 에러 메시지 (`TRIP_NOT_FOUND`) 반환 확인
- **`getTripsByUserId()`**: 사용자 여행 계획 목록 조회 테스트 **성공**
  - HTTP 응답: `200 OK`
  - 페이징된 여행 계획 목록 정상 반환 확인
- **`getTripsByUserIdEmpty()`**: 빈 목록 조회 테스트 **성공**
  - HTTP 응답: `200 OK`
  - 존재하지 않는 사용자 ID에 대해 빈 배열 반환 확인

#### 기존 생성 API 테스트도 모두 통과
- **`createTrip()`**: 여행 계획 생성 테스트 **성공**
- **`createTripWithInvalidRequest()`**: 유효성 검증 테스트 **성공**

### ⚠️ 해결된 문제점들

#### 1. trip_metadata JSON 변환 에러
**문제**: H2 데이터베이스에서 JSONB 타입 처리 시 `Data conversion error` 발생
**원인**: H2는 PostgreSQL의 JSONB를 완전히 지원하지 않아 빈 문자열 변환 시 에러
**해결**: 
- Trip과 TripDetail 엔티티의 JSON 필드를 TEXT 타입으로 변경
- null 값 처리 로직 추가

```java
// 기존: @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb")
// 수정: @Column(columnDefinition = "TEXT")
private String tripMetadata;

// 생성자에서 null 처리
this.tripMetadata = (tripMetadata != null && !tripMetadata.isEmpty()) ? tripMetadata : null;
```

#### 2. UUID 생성 문제
**문제**: Trip 생성 시 `trip_uuid` 필드가 NULL로 저장되어 제약 조건 위반
**원인**: 수동 생성자에서 UUID 초기화 누락
**해결**: 생성자에 UUID 초기화 코드 추가

```java
public Trip(...) {
    this.tripUuid = UUID.randomUUID(); // UUID 초기화 추가
    // ... 나머지 필드 초기화
}
```

#### 3. User 연관관계 처리
**문제**: Trip 생성 시 User 엔티티와의 연관관계 설정 누락
**해결**: TripService에서 User 조회 후 Trip에 설정

```java
@Transactional
public TripCreate.Response createTrip(TripCreate.Request request) {
    // User 조회
    User user = userRepository.findById(request.userId())
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    
    Trip trip = request.toTripEntity();
    trip.setUser(user); // User 설정
    
    Trip savedTrip = tripRepository.save(trip);
    return TripCreate.Response.from(savedTrip);
}
```

### 🎯 최종 결과
- **✅ JUnit 테스트**: 6개 테스트 모두 성공 (생성 2개 + 조회 4개)
- **✅ API 기능**: 여행 계획 생성, 상세 조회, 목록 조회, 예외 처리 모두 정상 동작
- **✅ 예외 처리**: 404 Not Found 에러 적절히 반환
- **✅ 페이징**: Spring Data JPA의 Pageable을 활용한 목록 조회 정상 동작
- **⏱️ 실행 시간**: 약 50초 (전체 테스트)
- **📅 최종 검증**: 2025-09-03 14:42 - 모든 테스트 통과 확인

### 📺 테스트 실행 결과
```
> Task :test

TripControllerTest > 존재하는 여행 계획을 조회한다. PASSED
TripControllerTest > 사용자의 여행 계획 목록을 조회한다. PASSED
TripControllerTest > 존재하지 않는 여행 계획을 조회하면 404 에러가 발생한다. PASSED
TripControllerTest > 잘못된 사용자 ID로 여행 계획 목록을 조회하면 빈 목록을 반환한다. PASSED
TripControllerTest > 필수값이 누락되면 여행 계획 생성에 실패한다. PASSED
TripControllerTest > 새로운 여행 계획을 생성한다. PASSED

BUILD SUCCESSFUL in 50s
5 actionable tasks: 5 executed
```

### 📋 적용된 설정
- **Spring Security**: `@WithMockUser` + `csrf()` 토큰 (기존 설정 활용)
- **테스트 격리**: `@DirtiesContext` + `@Transactional` (기존 설정 활용)
- **H2 데이터베이스**: PostgreSQL 호환 모드 (기존 설정 활용)
- **테스트 데이터**: `@BeforeEach`에서 User 생성, 테스트별로 Trip 생성
- **DTO 변환**: Entity → Response DTO 변환 로직 검증
- **예외 처리**: `TripNotFoundException` → `GlobalExceptionHandler` → 404 응답

### 🔗 관련 구현 파일
- **DTO**: `TripDetail.java`, `TripList.java`
- **Service**: `TripService.getTripById()`, `getTripsByUserId()`
- **Controller**: `TripController` GET 엔드포인트들
- **Exception**: `TripNotFoundException`, `GlobalExceptionHandler`
- **Repository**: `TripRepository.findByIdWithDetails()`, `findByUserIdOrderByCreatedAtDesc()`
- **Test**: `TripControllerTest` 조회 테스트 메서드들
