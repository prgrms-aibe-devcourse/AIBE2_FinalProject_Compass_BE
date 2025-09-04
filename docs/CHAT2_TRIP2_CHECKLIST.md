# ✅ CHAT2+TRIP2 통합 개발 체크리스트

## 👤 개발자: CHAT2+TRIP2 통합 담당
**핵심 역할**: LLM 통합, Function Calling, Lambda MCP, AI 여행 계획 생성, 개인화 시스템

---

## 📅 Week 1 (MVP) - AI 기반 구축

### ✅ 완료된 작업
- [x] REQ-LLM-001: Spring AI 설정 완료
- [x] REQ-LLM-002: Gemini 2.0 Flash 연동 완료

### 🔵 독립 개발 가능 (즉시 시작)
- [ ] REQ-AI-003: 기본 일정 템플릿 구축
  ```json
  {
    "type": "3D2N",
    "day1": ["체크인", "관광지1", "저녁식사", "휴식"],
    "day2": ["조식", "관광지2", "중식", "관광지3", "저녁"],
    "day3": ["조식", "쇼핑", "체크아웃"]
  }
  ```

- [ ] REQ-LLM-005: Function Calling 프레임워크 설정
  ```java
  @Configuration
  public class FunctionConfig {
      @Bean
      public FunctionCallback createTravelPlan() {
          // 구현
      }
  }
  ```

- [ ] REQ-AI-001: AI 여행 계획 Function 구현
  - createTravelPlan() 함수
  - validateTravelRequest() 검증
  - enrichWithTemplate() 템플릿 적용

- [ ] REQ-AI-002: 사용자 입력 파싱 로직
  - NER 엔티티 추출
  - 자연어 날짜 파싱
  - 예산 정규화

- [ ] REQ-PERS-007: 콜드 스타트 해결
  - 온보딩 메시지 템플릿
  - 초기 선호도 수집 플로우

- [ ] REQ-MON-001/002: 모니터링 설정
  - Logback 설정
  - LLM 호출 로깅
  - 에러 추적

### 🟡 Mock 구현 필요 (의존성 있음)
- [ ] REQ-LLM-004: 프롬프트 템플릿 (TRIP1 의존)
  ```java
  // Mock 구현
  @Profile("dev")
  public class MockTripService {
      public TravelPlan save(TravelPlan plan) {
          return plan; // 메모리 저장
      }
  }
  ```

- [ ] REQ-LLM-006: 대화 컨텍스트 관리 (CHAT1 의존)
  ```java
  // Mock 구현
  @Profile("dev")
  public class MockChatService {
      public List<Message> getRecentMessages(Long threadId) {
          return Collections.emptyList();
      }
  }
  ```

### 📋 Week 1 완료 기준
- [ ] Gemini로 기본 대화 가능
- [ ] Function Calling으로 여행 계획 생성
- [ ] 입력 파싱 및 템플릿 적용
- [ ] Mock으로 독립 실행 가능

---

## 📅 Week 2 (1차 고도화) - Lambda MCP + Multi-LLM

### 🔵 독립 개발 가능
- [ ] REQ-MCP-001: Lambda 프로젝트 설정
  ```yaml
  service: compass-mcp
  provider:
    name: aws
    runtime: nodejs18.x
    region: ap-northeast-2
  ```

- [ ] REQ-MCP-002: Tour API MCP (5개 함수)
  - [ ] getTourSpots()
  - [ ] getRestaurants()
  - [ ] getActivities()
  - [ ] getLocalEvents()
  - [ ] getCulturalSites()

- [ ] REQ-MCP-003: Weather API MCP (3개 함수)
  - [ ] getCurrentWeather()
  - [ ] getWeatherForecast()
  - [ ] getWeatherAlerts()

- [ ] REQ-MCP-004: Hotel API MCP (4개 함수)
  - [ ] searchHotels()
  - [ ] getHotelPricing()
  - [ ] getHotelReviews()
  - [ ] checkAvailability()

- [ ] REQ-MCP-005: DynamoDB 캐싱
  - [ ] 캐시 테이블 생성
  - [ ] TTL 설정
  - [ ] 캐시 키 전략

- [ ] REQ-LLM-003: OpenAI GPT-4 연동
  - [ ] GPT-4o-mini 설정
  - [ ] 스트리밍 응답 처리

- [ ] REQ-LLM-007: 토큰 사용량 추적
  - [ ] 요청/응답 토큰 카운팅
  - [ ] 일일/월별 리포트

- [ ] REQ-MCP-006: Spring AI-Lambda 통합
  ```java
  @Component
  public class LambdaMCPClient {
      public TourResponse callTourAPI(TourRequest request) {
          // Lambda 호출
      }
  }
  ```

- [ ] REQ-AI-004: Lambda MCP 호출 통합
  - [ ] 병렬 호출 관리
  - [ ] 응답 집계

- [ ] REQ-CTX-002: 대화 컨텍스트 저장
  - [ ] HttpSession 설정
  - [ ] Redis 연동

- [ ] REQ-CTX-003: Redis 캐싱
  - [ ] 30분 TTL 설정
  - [ ] 자동 갱신

### 🟡 실제 통합 필요 (의존성 있음)
- [ ] REQ-LLM-008: LLM 폴백 처리 (CHAT1 필요)
  - [ ] 의도별 라우팅
  - [ ] 재시도 로직

- [ ] REQ-CTX-001: 사용자 프로필 로드 (TRIP1 필요)
  - [ ] 선호도 API 연동

- [ ] REQ-CTX-004: 컨텍스트 병합 (TRIP1 필요)
  - [ ] 프로필 + 대화 통합

- [ ] REQ-PERS-008: 암묵적 선호도 수집 (TRIP1 필요)
  - [ ] 키워드 추출
  - [ ] 선호도 업데이트

### 📋 Week 2 완료 기준
- [ ] Lambda MCP 3개 API 모두 동작
- [ ] Multi-LLM 라우팅 구현
- [ ] Redis 캐싱 적용
- [ ] 응답 시간 5초 이내

---

## 📅 Week 3 (2차 고도화) - 개인화 + 최적화

### 🔵 독립 개발 가능 (개인화 시스템)
- [ ] REQ-PERS-001: 선호도 벡터 저장
  - [ ] OpenAI ada-002 임베딩
  - [ ] Redis Vector Store 설정

- [ ] REQ-PERS-002: 키워드 빈도 계산
  - [ ] TF-IDF 알고리즘
  - [ ] 시간 가중치

- [ ] REQ-PERS-009: Perplexity API 통합
  - [ ] 실시간 웹 검색
  - [ ] Spring AI 어댑터

- [ ] REQ-LLM-009: 응답 캐싱
  - [ ] FAQ 캐싱 (24시간 TTL)

- [ ] REQ-LLM-010: 컨텍스트 요약
  - [ ] 20개 이상 메시지 요약

- [ ] REQ-LLM-011: 이미지 텍스트 추출
  - [ ] Vision API OCR

- [ ] REQ-CTX-005: 키워드 추출
  - [ ] NER 기반 추출

- [ ] REQ-CTX-007: 컨텍스트 요약
  - [ ] 8K 토큰 제한

- [ ] REQ-PERS-004: 부정 선호 제외
  - [ ] 블랙리스트 필터

- [ ] REQ-PERS-005: 추천 카드 표시
  - [ ] UI 데이터 생성

- [ ] REQ-PERS-006: 추천 피드백
  - [ ] 좋아요/싫어요 수집

### 🔵 독립 개발 가능 (Lambda 최적화)
- [ ] REQ-MCP-007: Cold Start 최적화
  - [ ] Provisioned Concurrency
  - [ ] Lambda 워밍

- [ ] REQ-MCP-010: API Gateway 보안
  - [ ] API Key 관리
  - [ ] Rate Limiting

- [ ] REQ-MCP-011: 병렬 처리 최적화
  ```java
  CompletableFuture.allOf(
      callTourAPI(),
      callWeatherAPI(),
      callHotelAPI()
  ).join();
  ```

- [ ] REQ-MCP-008: 에러 핸들링
  - [ ] Exponential Backoff

- [ ] REQ-MCP-009: CloudWatch 모니터링
  - [ ] 메트릭 설정
  - [ ] 알람 구성

- [ ] REQ-MCP-012: 배포 자동화
  - [ ] GitHub Actions CI/CD

### 🟡 실제 통합 필요 (의존성 있음)
- [ ] REQ-PERS-003: RAG 기반 개인화 추천 (CHAT1 필요)
  - [ ] 3단계 파이프라인
  - [ ] 에이전트 통합

- [ ] REQ-CTX-006: 선호도 업데이트 (TRIP1 필요)
  - [ ] 비동기 처리

- [ ] REQ-CTX-008: 개인화 프롬프트 (TRIP1 필요)
  - [ ] 맞춤형 생성

- [ ] REQ-TRIP-006~010, 030: 여행 관리 기능 (TRIP1 필요)
  - [ ] JSONB 구조
  - [ ] UUID 공유
  - [ ] 템플릿 관리

### 📋 Week 3 완료 기준
- [ ] RAG 개인화 파이프라인 완성
- [ ] 응답 시간 3초 이내
- [ ] Lambda Cold Start 1초 이내
- [ ] 개인화 추천 정확도 70% 이상

---

## 🔧 개발 환경 설정

### 독립 실행 설정
```yaml
# application-standalone.yml
spring:
  profiles:
    active: dev,mock
  
mock:
  enabled: true
  trip-service: true
  user-service: true
  chat-service: true
```

### Mock 서비스 구현
```java
@Configuration
@Profile("mock")
public class MockConfiguration {
    
    @Bean
    public TripService mockTripService() {
        return new MockTripService();
    }
    
    @Bean
    public ChatService mockChatService() {
        return new MockChatService();
    }
    
    @Bean
    public UserService mockUserService() {
        return new MockUserService();
    }
}
```

### 통합 테스트
```java
@SpringBootTest
@ActiveProfiles("test")
public class IntegrationTest {
    
    @Test
    public void testEndToEndFlow() {
        // 1. LLM 호출
        // 2. Function Calling
        // 3. Lambda MCP 호출
        // 4. 결과 검증
    }
}
```

---

## 📊 성능 목표

### MVP (Week 1)
- 응답 시간: < 10초
- 동시 사용자: 5명
- 에러율: < 5%

### 1차 고도화 (Week 2)
- 응답 시간: < 5초
- 동시 사용자: 10명
- 캐시 히트율: > 50%
- Lambda 응답: < 2초

### 2차 고도화 (Week 3)
- 응답 시간: < 3초
- 동시 사용자: 20명
- 캐시 히트율: > 70%
- Lambda Cold Start: < 1초
- 개인화 정확도: > 70%

---

## 🚨 위험 관리

### 의존성 지연 대응
1. Mock 서비스 우선 사용
2. 인터페이스 정의 후 구현
3. 독립 가능한 작업 우선 진행

### 성능 이슈 대응
1. 캐싱 강화
2. Lambda 최적화
3. LLM 모델 다운그레이드

### 통합 이슈 대응
1. 일일 동기화 미팅
2. API 문서 공유
3. 통합 테스트 자동화

---

## 📝 일일 체크포인트

### 매일 확인 사항
- [ ] 독립 개발 가능한 작업 진행 중?
- [ ] Mock 서비스로 테스트 가능?
- [ ] 의존성 블로커 있는가?
- [ ] 통합 테스트 통과?
- [ ] 성능 목표 달성?

### 주간 마일스톤
- **Week 1 금요일**: Mock 환경에서 전체 플로우 동작
- **Week 2 금요일**: Lambda MCP 통합 완료
- **Week 3 금요일**: 개인화 시스템 완성

이 체크리스트를 따라 순차적으로 개발하면 의존성 충돌 없이 효율적으로 구현할 수 있습니다.