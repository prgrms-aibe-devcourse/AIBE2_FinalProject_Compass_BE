# 🔄 TRIP-CHAT 도메인 워크플로우 분석

## 📋 개요
TRIP과 CHAT 도메인 간의 의존성 관계와 데이터 흐름을 정의합니다. 
특히 TRIP2와 CHAT2를 같은 개발자가 담당하므로 통합 개발 전략을 수립합니다.

---

## 🎯 핵심 통합 포인트

### 1. TRIP2-CHAT2 동일 개발자 시너지
- **LLM + Function Calling 통합 관리**: 한 명이 전체 AI 파이프라인 관리
- **컨텍스트 일관성**: 대화 컨텍스트와 여행 계획 컨텍스트 통합
- **Lambda MCP 직접 연동**: CHAT에서 Lambda 함수 직접 호출 가능

### 2. 도메인 간 주요 의존성
```
USER → CHAT: JWT 인증, 사용자 정보
USER → TRIP: 사용자 ID, 인증 정보
CHAT ⇄ TRIP: 양방향 데이터 교환
MEDIA → CHAT: OCR 텍스트 전달
MEDIA → TRIP: 여행 관련 이미지 정보
```

---

## 🌊 주요 워크플로우

### 📱 워크플로우 1: 대화형 여행 계획 생성

```mermaid
sequenceDiagram
    participant U as 사용자
    participant C1 as CHAT1<br/>(메시지 수신)
    participant C2 as CHAT2<br/>(LLM 처리)
    participant T2 as TRIP2<br/>(AI 계획)
    participant T1 as TRIP1<br/>(저장/조회)
    participant DB as Database

    U->>C1: "부산 2박3일 여행 계획 짜줘"
    C1->>C2: 메시지 전달
    C2->>C2: 의도 분석 (여행 계획 요청)
    C2->>T2: createTravelPlan() 함수 호출
    T2->>T2: 입력 파싱 (목적지, 기간)
    T2->>T2: AI 여행 계획 생성
    T2->>T1: 계획 저장 요청
    T1->>DB: trips 테이블 저장
    T1-->>T2: 저장 완료 (planId)
    T2-->>C2: 여행 계획 응답
    C2->>C1: 포맷팅된 응답
    C1-->>U: "부산 2박3일 여행 계획입니다..."
```

### 🔄 워크플로우 2: 꼬리질문을 통한 정보 수집

```mermaid
sequenceDiagram
    participant U as 사용자
    participant C1 as CHAT1
    participant C2 as CHAT2<br/>(컨텍스트 관리)
    participant T2 as TRIP2<br/>(정보 수집)
    participant R as Redis<br/>(캐싱)

    U->>C1: "여행 가고 싶어"
    C1->>C2: 메시지 전달
    C2->>C2: 정보 부족 감지
    C2->>T2: 필요 정보 체크
    T2-->>C2: [목적지, 날짜, 예산] 부족
    C2->>R: 컨텍스트 저장
    C2->>C1: 꼬리질문 생성
    C1-->>U: "어디로 여행가고 싶으세요?"
    
    U->>C1: "제주도"
    C1->>C2: 메시지 전달
    C2->>R: 컨텍스트 업데이트
    C2->>T2: 정보 체크 (목적지: 제주도)
    T2-->>C2: [날짜, 예산] 부족
    C2->>C1: 꼬리질문 생성
    C1-->>U: "언제 출발하실 예정인가요?"
    
    U->>C1: "다음주 금요일"
    C1->>C2: 메시지 전달
    C2->>R: 컨텍스트 업데이트
    C2->>T2: 정보 체크 (날짜 추가)
    T2-->>C2: [예산] 부족
    C2->>C1: 꼬리질문 생성
    C1-->>U: "예산은 어느정도 생각하시나요?"
    
    U->>C1: "1인당 50만원"
    C1->>C2: 메시지 전달
    C2->>R: 전체 컨텍스트 로드
    C2->>T2: createTravelPlan() with 수집된 정보
    T2-->>C2: 여행 계획 생성 완료
```

### 🎨 워크플로우 3: 개인화된 추천

```mermaid
flowchart TB
    subgraph CHAT Domain
        C1[대화 히스토리]
        C2[선호도 추출]
        C3[암묵적 선호도]
    end
    
    subgraph TRIP Domain
        T1[명시적 선호도]
        T2[개인화 알고리즘]
        T3[RAG 추천]
    end
    
    subgraph Integration
        I1[선호도 병합]
        I2[가중치 계산]
        I3[최종 추천]
    end
    
    C1 --> C2
    C2 --> C3
    C3 --> I1
    T1 --> I1
    I1 --> I2
    I2 --> T2
    T2 --> T3
    T3 --> I3
```

### 🔌 워크플로우 4: Lambda MCP 통합 호출

```mermaid
sequenceDiagram
    participant U as 사용자
    participant C2 as CHAT2
    participant T2 as TRIP2<br/>(같은 개발자)
    participant L as Lambda MCP
    participant E as External APIs

    U->>C2: "부산 날씨 어때?"
    C2->>C2: 의도 분석 (날씨 정보)
    C2->>T2: getWeatherInfo()
    T2->>L: Weather MCP 호출
    L->>E: 날씨 API 요청
    E-->>L: 날씨 데이터
    L-->>T2: 가공된 날씨 정보
    T2-->>C2: 날씨 응답
    C2-->>U: "부산 날씨는..."

    U->>C2: "그럼 맛집 추천해줘"
    C2->>T2: getRestaurants()
    T2->>L: Tour API MCP 호출
    L->>E: 맛집 정보 요청
    E-->>L: 맛집 리스트
    L-->>T2: 추천 맛집 정보
    T2-->>C2: 맛집 응답
    C2-->>U: "부산 추천 맛집..."
```

---

## 🔗 API 인터페이스 정의

### 1. CHAT → TRIP 인터페이스

#### 여행 계획 생성 요청
```java
// CHAT2에서 TRIP2 Function 호출
public interface TravelPlanFunction {
    TravelPlan createTravelPlan(TravelRequest request);
    TravelPlan optimizeTravelPlan(Long planId, OptimizationOptions options);
    List<Recommendation> getRecommendations(UserContext context);
}
```

#### 정보 수집 체크
```java
// TRIP2가 제공하는 유틸리티
public class TravelInfoValidator {
    public ValidationResult checkRequiredInfo(Map<String, Object> collectedInfo) {
        List<String> missing = new ArrayList<>();
        if (!collectedInfo.containsKey("destination")) missing.add("목적지");
        if (!collectedInfo.containsKey("startDate")) missing.add("출발일");
        if (!collectedInfo.containsKey("budget")) missing.add("예산");
        return new ValidationResult(missing);
    }
}
```

### 2. TRIP → CHAT 인터페이스

#### 꼬리질문 생성 요청
```java
// TRIP1에서 CHAT2로 요청
public interface FollowUpQuestionService {
    String generateFollowUpQuestion(List<String> missingInfo);
    String generateClarificationQuestion(String ambiguousInfo);
}
```

### 3. 공통 컨텍스트 관리

```java
// TRIP2-CHAT2 공유 컨텍스트 (같은 개발자가 관리)
@Component
public class SharedContextManager {
    private final RedisTemplate<String, Object> redisTemplate;
    
    public void saveContext(String sessionId, TravelContext context) {
        String key = "context:" + sessionId;
        redisTemplate.opsForValue().set(key, context, 30, TimeUnit.MINUTES);
    }
    
    public TravelContext loadContext(String sessionId) {
        String key = "context:" + sessionId;
        return (TravelContext) redisTemplate.opsForValue().get(key);
    }
    
    public void updateContext(String sessionId, Map<String, Object> updates) {
        TravelContext context = loadContext(sessionId);
        if (context == null) context = new TravelContext();
        context.merge(updates);
        saveContext(sessionId, context);
    }
}
```

---

## 📊 데이터 흐름 매트릭스

| 출발 도메인 | 도착 도메인 | 데이터 타입 | 빈도 | 동기/비동기 |
|------------|------------|------------|------|------------|
| CHAT2 | TRIP2 | 여행 계획 요청 | 높음 | 동기 |
| TRIP2 | CHAT2 | 꼬리질문 | 중간 | 동기 |
| CHAT2 | TRIP1 | 계획 저장 요청 | 높음 | 비동기 |
| TRIP1 | CHAT2 | 선호도 데이터 | 낮음 | 동기 |
| CHAT1 | CHAT2 | 메시지 | 매우 높음 | 동기 |
| CHAT2 | Redis | 컨텍스트 | 높음 | 비동기 |
| TRIP2 | Lambda | API 호출 | 중간 | 동기 |
| MEDIA | CHAT2 | OCR 텍스트 | 낮음 | 비동기 |

---

## 🚀 TRIP2-CHAT2 통합 개발 전략

### 1단계: 기반 구축 (Week 1)
```
CHAT2 개발자가 동시에 구축:
├── Spring AI 설정 (CHAT2)
├── LLM 연동 (CHAT2)
├── Function Calling 프레임워크 (CHAT2)
└── 여행 계획 Functions (TRIP2)
```

### 2단계: 통합 구현 (Week 2)
```
통합 기능 개발:
├── 컨텍스트 관리 시스템
├── Lambda MCP 연동
├── 꼬리질문 생성 로직
└── 정보 수집 파이프라인
```

### 3단계: 고도화 (Week 3)
```
개인화 및 최적화:
├── RAG 기반 추천
├── 에이전트 패턴 구현
├── 개인화 알고리즘
└── 성능 최적화
```

---

## ⚠️ 의존성 관리 전략

### 1. 순환 의존성 방지
```java
// ❌ 잘못된 예: 순환 의존성
@Service
public class ChatService {
    @Autowired private TripService tripService; // 순환 참조
}

// ✅ 올바른 예: 이벤트 기반
@Service  
public class ChatService {
    @Autowired private ApplicationEventPublisher eventPublisher;
    
    public void processTravelRequest(String message) {
        // 이벤트 발행
        eventPublisher.publishEvent(new TravelRequestEvent(message));
    }
}
```

### 2. 인터페이스 기반 통신
```java
// 공통 인터페이스 정의
public interface TravelPlanningService {
    TravelPlan createPlan(TravelRequest request);
}

// TRIP2 구현
@Service
public class TripPlanningServiceImpl implements TravelPlanningService {
    // 구현
}

// CHAT2에서 사용
@Service
public class ChatService {
    @Autowired private TravelPlanningService planningService;
}
```

### 3. 비동기 처리
```java
@Service
public class AsyncTravelService {
    @Async
    public CompletableFuture<TravelPlan> generatePlanAsync(TravelRequest request) {
        // 비동기 처리로 의존성 완화
        return CompletableFuture.completedFuture(generatePlan(request));
    }
}
```

---

## 📈 성능 최적화 포인트

### 1. 캐싱 전략
- **Redis 캐싱**: 자주 사용되는 여행 정보
- **컨텍스트 캐싱**: 30분 TTL로 대화 컨텍스트 유지
- **Lambda 응답 캐싱**: DynamoDB로 외부 API 응답 캐싱

### 2. 병렬 처리
- **Lambda MCP 병렬 호출**: CompletableFuture 활용
- **다중 LLM 병렬 처리**: Gemini + GPT 동시 호출
- **배치 처리**: 여러 추천 요청 일괄 처리

### 3. 연결 최적화
- **Connection Pool**: DB 연결 풀 최적화
- **HTTP Client Pool**: RestTemplate 풀 관리
- **Redis Connection**: Lettuce 연결 풀 설정

---

## 🔍 모니터링 포인트

### 핵심 메트릭
1. **응답 시간**: CHAT → TRIP 왕복 시간
2. **Function 호출 빈도**: 시간당 Function 호출 횟수
3. **캐시 적중률**: Redis 캐시 효율성
4. **에러율**: 도메인 간 통신 실패율
5. **Lambda 성능**: Cold Start 빈도 및 실행 시간

### 알람 설정
```yaml
alerts:
  - name: "High Response Time"
    condition: response_time > 3s
    action: notify_slack
    
  - name: "Function Call Failure"
    condition: error_rate > 5%
    action: page_oncall
    
  - name: "Cache Miss Rate High"
    condition: cache_hit_rate < 70%
    action: notify_team
```

---

## 📝 개발 체크리스트

### TRIP2-CHAT2 개발자 (동일인)
- [ ] Spring AI 기본 설정 완료
- [ ] Gemini/GPT 연동 테스트
- [ ] Function Calling 구현
- [ ] 여행 계획 Functions 작성
- [ ] Lambda MCP 연동
- [ ] 컨텍스트 관리 시스템
- [ ] 꼬리질문 생성 로직
- [ ] 통합 테스트 작성

### TRIP1 개발자
- [ ] Trip 도메인 엔티티 설계
- [ ] 여행 계획 CRUD API
- [ ] 선호도 관리 API
- [ ] 개인화 알고리즘 구현

### CHAT1 개발자
- [ ] 채팅 메시지 CRUD
- [ ] 의도 분류 시스템
- [ ] 라우팅 로직 구현
- [ ] 에이전트 패턴 구현

---

## 🎯 성공 지표

### 기술적 지표
- API 응답 시간 < 3초
- 시스템 가용성 > 99%
- 테스트 커버리지 > 70%
- 캐시 적중률 > 80%

### 비즈니스 지표
- 여행 계획 생성 성공률 > 95%
- 꼬리질문 통한 정보 수집률 > 90%
- 개인화 추천 만족도 > 85%
- Lambda MCP 호출 성공률 > 98%