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
- **클래스명**: `UserPreferenceControllerTest`, `UserPreferenceServiceTest`
- **파일 경로**: 
  - `src/test/java/com/compass/domain/trip/controller/UserPreferenceControllerTest.java`
  - `src/test/java/com/compass/domain/trip/service/UserPreferenceServiceTest.java`

### 테스트 목적
> `POST, GET, PUT /api/users/{userId}/preferences/budget-level` API의 전체 흐름이 올바르게 동작하는지 검증합니다.

- 유효한 요청에 대해 HTTP `200 OK` 응답과 함께 데이터가 DB에 정상적으로 저장/수정/조회되는지 확인합니다.
- 유효하지 않은 요청(유효하지 않은 값, 필수값 누락)에 대해 HTTP `400 Bad Request` 응답을 반환하며 요청을 거부하는지 확인합니다.

---

## 🎯 테스트 케이스

### 정상 케이스 (Happy Path)
- [ ] **케이스 1**: `POST` - 유효한 데이터로 새로운 예산 수준 설정
    - **입력**: `{"budgetLevel": "STANDARD"}`
    - **예상 결과**: HTTP Status `200 OK` 반환. DB에 해당 `userId`의 `BUDGET_LEVEL` 타입으로 데이터 저장.
- [ ] **케이스 2**: `PUT` - 유효한 데이터로 기존 예산 수준 수정
    - **입력**: `{"budgetLevel": "LUXURY"}`
    - **예상 결과**: HTTP Status `200 OK` 반환. DB의 기존 데이터가 "LUXURY"로 업데이트됨.
- [ ] **케이스 3**: `GET` - 설정된 예산 수준 조회
    - **입력**: 예산 수준이 설정된 `userId`
    - **예상 결과**: HTTP Status `200 OK` 반환 및 설정된 예산 수준(`"LUXURY"`) 정보 응답.

### 예외 케이스 (Exception Cases)
- [ ] **케이스 4**: 유효하지 않은 `budgetLevel` 값으로 설정/수정
    - **입력**: `{"budgetLevel": "INVALID_VALUE"}`
    - **예상 결과**: HTTP Status `400 Bad Request` 반환.
- [ ] **케이스 5**: 필수값이 누락된 데이터로 설정/수정
    - **입력**: `{"budgetLevel": ""}` 또는 `{}`
    - **예상 결과**: HTTP Status `400 Bad Request` 반환.

---

## 🔧 테스트 환경 설정
- **의존성**: `build.gradle`에 `spring-boot-starter-test`가 포함되어 있는지 확인합니다.
- **데이터베이스**: H2 인메모리 DB를 PostgreSQL 호환 모드로 사용합니다.
- **보안**: `@WithMockUser`를 사용하여 인증을 통과시킵니다.

---

## 📝 테스트 코드 구조
```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserPreferenceControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserPreferenceService userPreferenceService;
    
    @Autowired
    private ObjectMapper objectMapper;

    @DisplayName("새로운 예산 수준을 설정한다.")
    @Test
    @WithMockUser
    void setBudgetLevel() throws Exception {
      // Given (준비)
      BudgetRequest request = new BudgetRequest("STANDARD");
      
      // When (실행) & Then (검증)
      mockMvc.perform(post("/api/users/1/preferences/budget-level")
                      .content(objectMapper.writeValueAsString(request))
                      .contentType(MediaType.APPLICATION_JSON)
              )
              .andExpect(status().isOk());
    }
    
    // ... (기타 GET, PUT 및 예외 케이스 테스트)
}
```

---

## 🚀 실행 방법
```bash
./gradlew test --tests "com.compass.domain.trip.controller.UserPreferenceControllerTest"
./gradlew test --tests "com.compass.domain.trip.service.UserPreferenceServiceTest"
```

---

## 📊 테스트 결과 (예상)
- **JUnit 테스트**: 모든 테스트 케이스가 성공적으로 통과 (`PASSED`)
- **Swagger UI 테스트**: API 문서화 및 수동 테스트 정상 동작
- **최종 검증**: TBD
