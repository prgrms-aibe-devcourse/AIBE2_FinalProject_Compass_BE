# Week 2 Requirements Definition

## Overview
Week 2 focuses on implementing three core features that form the backbone of our simplified travel planning system.

## Core Features

### 1. Intent Router (CHAT1 담당)

#### 목적
사용자 입력을 분석하여 적절한 처리 경로로 라우팅하는 간단한 키워드 기반 시스템

#### 기능 요구사항

##### REQ-INTENT-001: 키워드 기반 의도 분류
```java
public enum Intent {
    TRAVEL_PLAN,    // 여행 계획 수립
    RECOMMEND,      // 장소/활동 추천
    GENERAL,        // 일반 대화
    WEATHER,        // 날씨 정보
    UNKNOWN         // 분류 불가
}
```

##### REQ-INTENT-002: 키워드 사전 관리
```java
// 키워드 매핑 예시
Map<String, Intent> keywords = Map.of(
    "여행", Intent.TRAVEL_PLAN,
    "계획", Intent.TRAVEL_PLAN,
    "일정", Intent.TRAVEL_PLAN,
    "추천", Intent.RECOMMEND,
    "맛집", Intent.RECOMMEND,
    "관광지", Intent.RECOMMEND,
    "날씨", Intent.WEATHER,
    "기온", Intent.WEATHER
);
```

##### REQ-INTENT-003: 우선순위 기반 매칭
- 여러 키워드가 매칭될 경우 우선순위 적용
- 우선순위: TRAVEL_PLAN > RECOMMEND > WEATHER > GENERAL

##### REQ-INTENT-004: 기본 동작
- 키워드가 없을 경우 GENERAL로 분류
- 에러 발생 시 UNKNOWN 반환

#### API 스펙
```java
@RestController
@RequestMapping("/api/chat/intent")
public class IntentController {
    
    @PostMapping("/classify")
    public IntentResponse classifyIntent(@RequestBody IntentRequest request) {
        // 키워드 매칭 로직
        // 우선순위 적용
        // 결과 반환
    }
}
```

#### 테스트 요구사항
- 각 Intent 타입별 키워드 매칭 테스트
- 우선순위 적용 테스트
- 엣지 케이스 (빈 입력, 특수문자 등) 테스트

---

### 2. Follow-up Question System (CHAT2 담당)

#### 목적
여행 계획 수립에 필요한 정보를 5개의 순차적 질문으로 수집

#### 기능 요구사항

##### REQ-FOLLOWUP-001: 질문 흐름 정의
```java
public enum QuestionType {
    DESTINATION(1, "어디로 여행을 가시나요?"),
    DURATION(2, "며칠 동안 여행하시나요?"),
    COMPANIONS(3, "누구와 함께 가시나요?"),
    STYLE(4, "어떤 스타일의 여행을 원하시나요?"),
    BUDGET(5, "예산은 어느 정도로 생각하시나요?");
    
    private final int order;
    private final String defaultQuestion;
}
```

##### REQ-FOLLOWUP-002: 컨텍스트 기반 질문 생성
```java
public class FollowUpQuestionService {
    
    public String generateQuestion(QuestionType type, UserContext context) {
        // 이전 답변을 고려한 맞춤형 질문 생성
        // 예: "부산"이라고 답하면 "부산에서 2박 3일 정도면 충분할까요?"
    }
}
```

##### REQ-FOLLOWUP-003: 답변 파싱 및 검증
```java
public class AnswerParser {
    
    public ParsedAnswer parse(QuestionType type, String userAnswer) {
        switch(type) {
            case DESTINATION:
                return parseDestination(userAnswer);
            case DURATION:
                return parseDuration(userAnswer);
            // ...
        }
    }
    
    private ParsedAnswer parseDestination(String answer) {
        // "부산", "부산 해운대", "부산이요" → "부산"
        // 검증: 실제 존재하는 지역인지 확인
    }
    
    private ParsedAnswer parseDuration(String answer) {
        // "3일", "2박3일", "주말" → Duration 객체
        // 검증: 1일 ~ 30일 범위 체크
    }
}
```

##### REQ-FOLLOWUP-004: 세션 상태 관리
```java
@Entity
public class QuestionSession {
    @Id
    private String sessionId;
    
    private QuestionType currentQuestion;
    private Map<QuestionType, String> answers;
    private LocalDateTime startedAt;
    private boolean completed;
    
    // 30분 후 자동 만료
    public boolean isExpired() {
        return startedAt.plusMinutes(30).isBefore(LocalDateTime.now());
    }
}
```

##### REQ-FOLLOWUP-005: 스킵 및 수정 기능
- 사용자가 "모르겠어요", "패스" 등 입력 시 다음 질문으로
- "이전", "다시" 입력 시 이전 질문으로 돌아가기
- 5개 질문 완료 후 요약 표시 및 수정 기회 제공

#### API 스펙
```java
@RestController
@RequestMapping("/api/chat/followup")
public class FollowUpController {
    
    @PostMapping("/start")
    public QuestionResponse startSession() {
        // 세션 생성 및 첫 질문 반환
    }
    
    @PostMapping("/answer")
    public QuestionResponse submitAnswer(@RequestBody AnswerRequest request) {
        // 답변 저장 및 다음 질문 반환
        // 마지막 질문이면 완료 상태 반환
    }
    
    @GetMapping("/session/{sessionId}")
    public SessionStatus getSession(@PathVariable String sessionId) {
        // 현재 세션 상태 조회
    }
}
```

#### 테스트 요구사항
- 각 질문 타입별 답변 파싱 테스트
- 세션 만료 테스트
- 질문 흐름 전체 시나리오 테스트
- 스킵/수정 기능 테스트

---

### 3. Perplexity API Integration (TRIP1 담당)

#### 목적
수집된 정보를 바탕으로 Perplexity API를 통해 효율적으로 장소 검색 (2-3회 검색으로 제한)

#### 기능 요구사항

##### REQ-PERPLEXITY-001: 스마트 검색 전략
```java
public class PerplexitySearchStrategy {
    
    public List<SearchQuery> buildQueries(TravelContext context) {
        // 최대 2-3개의 검색 쿼리 생성
        // 사용자의 전체 컨텍스트를 포함하여 더 정확한 결과 획득
        List<SearchQuery> queries = new ArrayList<>();
        
        // 전체 사용자 컨텍스트 구성
        String fullContext = buildFullContext(context);
        
        // Query 1: 전체 컨텍스트 기반 종합 장소 검색
        queries.add(SearchQuery.builder()
            .query(String.format(
                "다음 조건에 맞는 %s 여행 장소 추천 10곳: " +
                "목적지: %s, 기간: %s, 동행: %s, 스타일: %s, 예산: %s. " +
                "각 장소별로 추천 이유와 특징을 간단히 포함해주세요.",
                context.getDestination(),
                context.getDestination(),
                context.getDuration(),
                context.getCompanions(),
                context.getTravelStyle(),
                context.getBudget()))
            .type(SearchType.COMPREHENSIVE)
            .build());
        
        // Query 2: 맛집 + 카페 통합 검색 (선택적)
        if (context.includesFood() || context.includesCafe()) {
            queries.add(SearchQuery.builder()
                .query(String.format(
                    "%s에서 %s 여행자가 좋아할 만한 맛집과 카페 각 3곳씩 추천. " +
                    "동행: %s, 예산: %s 수준",
                    context.getDestination(),
                    context.getTravelStyle(),
                    context.getCompanions(),
                    context.getBudget()))
                .type(SearchType.FOOD_CAFE)
                .build());
        }
        
        // Query 3: 숙소 + 교통 정보 (1박 이상인 경우만)
        if (context.getDays() > 1) {
            queries.add(SearchQuery.builder()
                .query(String.format(
                    "%s %s 여행 %s 동행 시 추천 숙소 지역과 교통 팁. " +
                    "예산: %s, 주요 관광지 접근성 중심",
                    context.getDestination(),
                    context.getDuration(),
                    context.getCompanions(),
                    context.getBudget()))
                .type(SearchType.ACCOMMODATION_TRANSPORT)
                .build());
        }
        
        return queries;
    }
    
    private String buildFullContext(TravelContext context) {
        // 사용자의 원본 입력과 꼬리질문 답변을 모두 포함
        StringBuilder sb = new StringBuilder();
        sb.append("사용자 요청: ").append(context.getOriginalMessage()).append("\n");
        sb.append("목적지: ").append(context.getDestination()).append("\n");
        sb.append("기간: ").append(context.getDuration()).append("\n");
        sb.append("동행: ").append(context.getCompanions()).append("\n");
        sb.append("여행 스타일: ").append(context.getTravelStyle()).append("\n");
        sb.append("예산: ").append(context.getBudget());
        return sb.toString();
    }
}
```

##### REQ-PERPLEXITY-002: API 호출 및 응답 처리
```java
@Service
public class PerplexityService {
    
    @Value("${perplexity.api.key}")
    private String apiKey;
    
    public SearchResult search(SearchQuery query) {
        // API 호출
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // 시스템 프롬프트로 구조화된 응답 유도
        PerplexityRequest request = PerplexityRequest.builder()
            .model("llama-3.1-sonar-small-128k-online")  // 저렴한 모델 사용
            .messages(List.of(
                Message.builder()
                    .role("system")
                    .content("당신은 여행 전문가입니다. 사용자의 전체 맥락을 고려하여 " +
                            "개인화된 여행 추천을 제공하세요. 각 장소마다 추천 이유를 포함하세요.")
                    .build(),
                Message.builder()
                    .role("user")
                    .content(query.getQuery())  // 전체 컨텍스트가 포함된 쿼리
                    .build()
            ))
            .temperature(0.2)  // 일관된 결과를 위해 낮은 temperature
            .max_tokens(1500)  // 상세한 답변을 위해 토큰 약간 증가
            .build();
        
        // 응답 파싱
        PerplexityResponse response = restTemplate.postForObject(
            "https://api.perplexity.ai/chat/completions",
            new HttpEntity<>(request, headers),
            PerplexityResponse.class
        );
        
        return parseResponse(response, query.getType());
    }
}
```

##### REQ-PERPLEXITY-003: 결과 파싱 및 구조화
```java
public class PerplexityResponseParser {
    
    public List<Place> parsePlaces(String response) {
        // 정규식 또는 간단한 파싱으로 장소 추출
        List<Place> places = new ArrayList<>();
        
        // 예시: "1. 해운대 해수욕장" 형태 파싱
        Pattern pattern = Pattern.compile("\\d+\\.\\s*([^\\n]+)");
        Matcher matcher = pattern.matcher(response);
        
        while (matcher.find()) {
            String placeName = matcher.group(1).trim();
            places.add(Place.builder()
                .name(extractName(placeName))
                .description(extractDescription(placeName))
                .category(detectCategory(placeName))
                .build());
        }
        
        return places;
    }
}
```

##### REQ-PERPLEXITY-004: 캐싱 전략
```java
@Service
public class PerplexityCacheService {
    
    @Autowired
    private RedisTemplate<String, SearchResult> redisTemplate;
    
    public Optional<SearchResult> getCached(String cacheKey) {
        return Optional.ofNullable(
            redisTemplate.opsForValue().get(cacheKey)
        );
    }
    
    public void cache(String cacheKey, SearchResult result) {
        // 30분 TTL로 캐싱
        redisTemplate.opsForValue().set(
            cacheKey, 
            result, 
            30, 
            TimeUnit.MINUTES
        );
    }
    
    public String generateCacheKey(SearchQuery query) {
        // 목적지 + 스타일 + 기간 조합으로 키 생성
        return String.format("perplexity:%s:%s:%d",
            query.getDestination(),
            query.getTravelStyle(),
            query.getDays()
        );
    }
}
```

##### REQ-PERPLEXITY-005: 에러 처리 및 폴백
```java
public class PerplexityFallbackService {
    
    public SearchResult getFallback(SearchQuery query) {
        // Perplexity API 실패 시 기본 데이터 반환
        // DB에 저장된 인기 장소 정보 활용
        
        return searchResultRepository
            .findByDestinationAndStyle(
                query.getDestination(),
                query.getTravelStyle()
            )
            .orElse(getDefaultPlaces(query.getDestination()));
    }
}
```

#### API 스펙
```java
@RestController
@RequestMapping("/api/trip/search")
public class PlaceSearchController {
    
    @PostMapping("/places")
    public PlaceSearchResponse searchPlaces(@RequestBody PlaceSearchRequest request) {
        // 캐시 확인
        // Perplexity API 호출 (최대 3회)
        // 결과 구조화 및 반환
    }
    
    @GetMapping("/cache-status")
    public CacheStatusResponse getCacheStatus() {
        // 캐시 상태 조회 (디버깅용)
    }
}
```

#### 테스트 요구사항
- 검색 쿼리 생성 로직 테스트
- API 응답 파싱 테스트
- 캐싱 동작 테스트
- 에러 시나리오 및 폴백 테스트
- API 호출 횟수 제한 테스트

---

## 통합 요구사항

### REQ-INTEGRATION-001: 전체 플로우 연동
```java
@Service
public class TravelPlanningOrchestrator {
    
    public TravelPlan createPlan(String userId, String message) {
        // 1. Intent 분류
        Intent intent = intentRouter.classify(message);
        
        if (intent != Intent.TRAVEL_PLAN) {
            return handleOtherIntent(intent, message);
        }
        
        // 2. 꼬리질문 세션 시작
        QuestionSession session = followUpService.startSession(userId);
        
        // 3. 5개 질문 완료 대기 (비동기 처리)
        
        // 4. 완료 후 Perplexity 검색
        TravelContext context = buildContext(session.getAnswers());
        List<Place> places = perplexityService.searchPlaces(context);
        
        // 5. 여행 계획 생성
        return travelPlanBuilder.build(context, places);
    }
}
```

### REQ-INTEGRATION-002: 에러 처리
- 각 단계별 실패 시 graceful degradation
- 사용자에게 명확한 에러 메시지 제공
- 부분 완료 상태 저장 및 복구 가능

### REQ-INTEGRATION-003: 성능 요구사항
- Intent 분류: < 100ms
- 꼬리질문 응답: < 200ms
- Perplexity 검색: < 3초 (3회 검색 총합)
- 전체 플로우: < 5초 (꼬리질문 제외)

---

## 개발 우선순위

### Phase 1 (Day 1-2)
1. 각 컴포넌트 기본 구조 구현
2. 단위 테스트 작성
3. Mock 데이터로 개별 동작 확인

### Phase 2 (Day 3-4)
1. API 연동 구현
2. 통합 테스트 작성
3. 에러 처리 추가

### Phase 3 (Day 5)
1. 전체 플로우 통합
2. 성능 최적화
3. 문서화

---

## 제약사항

### 기술적 제약
- Perplexity API 호출은 최대 3회로 제한
- Redis 캐시 TTL은 30분
- 세션 타임아웃 30분

### 비즈니스 제약
- 무료 사용자는 하루 10회 검색 제한
- 캐시된 결과 우선 제공
- 개인정보는 저장하지 않음

---

## 테스트 시나리오

### 시나리오 1: 정상 플로우
```
사용자: "부산 여행 계획 짜줘"
→ Intent: TRAVEL_PLAN
→ 질문1: "부산 어디로 가실 예정인가요?"
→ 답변1: "해운대"
→ 질문2: "며칠 동안 여행하시나요?"
→ 답변2: "2박 3일"
→ 질문3: "누구와 함께 가시나요?"
→ 답변3: "친구들이랑"
→ 질문4: "어떤 스타일의 여행을 원하시나요?"
→ 답변4: "액티비티 위주로"
→ 질문5: "예산은 어느 정도로 생각하시나요?"
→ 답변5: "1인당 30만원"
→ Perplexity 검색 (2회)
  - Query 1: 전체 컨텍스트 포함 종합 장소 검색
  - Query 2: 맛집/카페 추천
→ 여행 계획 생성
```

### 시나리오 2: 캐시 히트
```
동일한 조건으로 재검색 시
→ Redis 캐시에서 즉시 반환
→ Perplexity API 호출 없음
```

### 시나리오 3: 에러 복구
```
Perplexity API 실패 시
→ Fallback 데이터 사용
→ 사용자에게 제한된 결과 안내
```

---

## 참고사항

### Perplexity API 문서
- https://docs.perplexity.ai/reference/post_chat_completions
- 모델: llama-3.1-sonar-small-128k-online (비용 효율적)
- Rate Limit: 분당 50회

### Spring Boot 통합
- RestTemplate 또는 WebClient 사용
- Circuit Breaker 패턴 적용 권장
- Retry 로직 구현 필수

### Redis 설정
```yaml
spring:
  redis:
    host: localhost
    port: 6379
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
```