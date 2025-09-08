---
name: 테스트 명세서
about: 기능 명세서에 대한 테스트 케이스를 정의하는 문서
title: '[TEST] REQ-PREF-002 | 예산 수준 설정 테스트'
labels: '백엔드'
assignees: 'TRIP1'
---

# 🧪 통합 테스트 작성

## 📋 테스트 정보

### 테스트 대상
- **클래스**: `UserPreferenceControllerTest`, `UserPreferenceServiceTest`
- **주요 API**: `POST, GET, PUT /api/users/{userId}/preferences/budget-level`

### 테스트 목적
> 예산 수준 설정 기능의 API 흐름과 비즈니스 로직의 정확성을 검증합니다.

- **정상 흐름**: 유효한 요청에 대해 데이터가 DB에 정상적으로 저장/수정/조회되는지 확인합니다.
- **예외 처리**: 유효하지 않은 값, 필수값 누락 등 잘못된 요청에 대해 `400 Bad Request`를 올바르게 반환하는지 확인합니다.

---

## 🎯 테스트 케이스

### 정상 케이스 (Happy Path)
- [x] **`Service-HP-01`**: 새로운 예산 수준 설정 성공 (`Service`)
- [x] **`Service-HP-02`**: 기존 예산 수준 수정 성공 (`Service`)
- [x] **`Service-HP-03`**: 설정된 예산 수준 조회 성공 (`Service`)
- [x] **`Controller-HP-01`**: `POST` API - 예산 수준 설정 성공 (`Controller`)
- [x] **`Controller-HP-02`**: `GET` API - 예산 수준 조회 성공 (`Controller`)
- [x] **`Controller-HP-03`**: `PUT` API - 예산 수준 수정 성공 (`Controller`)

### 예외 케이스 (Exception Cases)
- [x] **`Service-EC-01`**: 유효하지 않은 `BudgetLevel` 문자열로 요청 (`Service`)
- [x] **`Controller-EC-01`**: 유효하지 않은 `budgetLevel` 값으로 API 요청 (`Controller`)
- [x] **`Controller-EC-02`**: 필수값이 누락된 데이터로 API 요청 (`Controller`)

---

## 🔧 테스트 환경 설정

- **의존성**: `build.gradle`에 `spring-boot-starter-test`, `h2database` 포함
- **데이터베이스**: H2 인메모리 DB (PostgreSQL 호환 모드)
- **보안**: `@WithMockUser`를 사용한 Mock 인증
- **테스트 격리**: `@Transactional`을 사용하여 각 테스트 후 롤백

---

## 📝 테스트 코드 구조

### `UserPreferenceServiceTest.java`
```java
@ExtendWith(MockitoExtension.class)
class UserPreferenceServiceTest {
    @InjectMocks
    private UserPreferenceService userPreferenceService;
    @Mock
    private UserPreferenceRepository userPreferenceRepository;

    @Test
    @DisplayName("새로운 예산 수준 설정 - 성공")
    void setBudgetLevel_Success() {
        // Given
        Long userId = 1L;
        BudgetRequest request = new BudgetRequest("STANDARD");
        when(userPreferenceRepository.findByUserIdAndPreferenceType(any(), any())).thenReturn(new ArrayList<>());
        when(userPreferenceRepository.save(any(UserPreference.class))).thenAnswer(i -> i.getArgument(0));

        // When
        BudgetResponse response = userPreferenceService.setOrUpdateBudgetLevel(userId, request);

        // Then
        assertThat(response.getBudgetLevel()).isEqualTo("STANDARD");
        verify(userPreferenceRepository, times(1)).save(any(UserPreference.class));
    }
    
    @Test
    @DisplayName("기존 예산 수준 수정 - 성공")
    void updateBudgetLevel_Success() {
        // Given
        Long userId = 1L;
        BudgetRequest request = BudgetRequest.builder().budgetLevel("LUXURY").build();
        UserPreference existingPreference = UserPreference.builder().userId(userId).preferenceKey("BUDGET").build();
        when(userPreferenceRepository.findByUserIdAndPreferenceType(userId, "BUDGET_LEVEL")).thenReturn(List.of(existingPreference));
        when(userPreferenceRepository.save(any(UserPreference.class))).thenAnswer(i -> i.getArgument(0));

        // When
        BudgetResponse response = userPreferenceService.setOrUpdateBudgetLevel(userId, request);

        // Then
        assertThat(response.getBudgetLevel()).isEqualTo("LUXURY");
        assertThat(existingPreference.getPreferenceKey()).isEqualTo("LUXURY");
    }

    @Test
    @DisplayName("유효하지 않은 BudgetLevel 문자열로 설정 요청")
    void setBudgetLevel_InvalidLevelString() {
        // Given
        Long userId = 1L;
        BudgetRequest request = BudgetRequest.builder().budgetLevel("INVALID").build();

        // When & Then
        assertThatThrownBy(() -> userPreferenceService.setOrUpdateBudgetLevel(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효하지 않은 예산 수준입니다.");
    }
}
```

### `UserPreferenceControllerTest.java`
```java
@AutoConfigureMockMvc
class UserPreferenceControllerTest extends BaseIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private UserPreferenceService userPreferenceService;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser
    @DisplayName("POST /budget-level - 예산 수준 설정 성공")
    void setBudgetLevel_Success() throws Exception {
        // Given
        Long userId = 1L;
        BudgetRequest request = new BudgetRequest("STANDARD");
        BudgetResponse mockResponse = BudgetResponse.from(userId, BudgetLevel.STANDARD, "...");
        when(userPreferenceService.setOrUpdateBudgetLevel(eq(userId), any(BudgetRequest.class))).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(post("/api/users/{userId}/preferences/budget-level", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.budgetLevel").value("STANDARD"));
    }
    
    @Test
    @WithMockUser
    @DisplayName("GET /budget-level - 예산 수준 조회 성공")
    void getBudgetLevel_Success() throws Exception {
        // Given
        Long userId = 1L;
        BudgetResponse mockResponse = BudgetResponse.of(userId, BudgetLevel.LUXURY);
        when(userPreferenceService.getBudgetLevel(userId)).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(get("/api/users/{userId}/preferences/budget-level", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.budgetLevel").value("LUXURY"));
    }

    @Test
    @WithMockUser
    @DisplayName("PUT /budget-level - 예산 수준 수정 성공")
    void updateBudgetLevel_Success() throws Exception {
        // Given
        Long userId = 1L;
        BudgetRequest request = new BudgetRequest("BUDGET");
        BudgetResponse mockResponse = BudgetResponse.from(userId, BudgetLevel.BUDGET, "...");
        when(userPreferenceService.setOrUpdateBudgetLevel(eq(userId), any(BudgetRequest.class))).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(put("/api/users/{userId}/preferences/budget-level", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.budgetLevel").value("BUDGET"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /budget-level - 필드 누락")
    void setBudgetLevel_MissingField() throws Exception {
        // Given
        Long userId = 1L;
        BudgetRequest request = new BudgetRequest(""); // 유효성 검증에 걸릴 빈 문자열

        // When & Then
        mockMvc.perform(post("/api/users/{userId}/preferences/budget-level", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
```

---

## 🚀 실행 방법
```bash
# 전체 테스트 실행
./gradlew test

# 특정 테스트만 실행
./gradlew test --tests "com.compass.domain.trip.service.UserPreferenceServiceTest"
./gradlew test --tests "com.compass.domain.trip.controller.UserPreferenceControllerTest"
```

---

## 📊 테스트 결과

### ✅ 테스트 성공
- `UserPreferenceServiceTest`: **11개 테스트 모두 성공**
- `UserPreferenceControllerTest`: **10개 테스트 모두 성공** (기존 5개 + 신규 5개)

### ✅ CI 환경 통합 테스트 성공
- **로컬 테스트**: 모든 `UserPreferenceServiceTest`, `UserPreferenceControllerTest` 통과
- **CI 환경 테스트**: GitHub Actions에서 모든 테스트 통과
- **Redis 연동**: EmbeddedRedis 설정 개선으로 CI 환경에서 안정적 동작 확인

### 📺 테스트 실행 결과 (일부)
```
UserPreferenceServiceTest > 기존 예산 수준 수정 - 성공 PASSED
UserPreferenceServiceTest > 설정된 예산 수준 조회 - 성공 PASSED
UserPreferenceServiceTest > 새로운 예산 수준 설정 - 성공 PASSED
UserPreferenceServiceTest > 미설정 예산 수준 조회 PASSED
UserPreferenceServiceTest > 유효하지 않은 BudgetLevel 문자열로 설정 요청 PASSED

...

UserPreferenceControllerTest > POST /budget-level - 예산 수준 설정 성공 PASSED
UserPreferenceControllerTest > GET /budget-level - 예산 수준 조회 성공 PASSED
UserPreferenceControllerTest > PUT /budget-level - 예산 수준 수정 성공 PASSED
UserPreferenceControllerTest > POST /budget-level - 유효하지 않은 값으로 요청 PASSED
UserPreferenceControllerTest > POST /budget-level - 필드 누락 PASSED
```

## 🎯 최종 검증
- **검증자**: TRIP 1
- **검증 일시**: 2025-09-04 16:58 (로컬), 2025-09-04 17:10 (CI)
- **PR 상태**: ✅ **Merged** 
- **결과**: ✅ **완전 통과** (로컬 + CI 환경)
