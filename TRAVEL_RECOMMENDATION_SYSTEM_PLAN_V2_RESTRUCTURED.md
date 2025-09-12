# 🎯 Compass - AI 기반 여행 계획 추천 시스템

---

## 📚 섹션 1: Spring AI와 프로젝트 소개

### 1.1 Spring AI란?
Spring AI는 스프링 생태계에서 **AI/LLM을 쉽게 통합**할 수 있도록 제공하는 공식 프레임워크입니다.

#### 핵심 특징
- OpenAI, Gemini, Claude 등 다양한 LLM을 **통일된 인터페이스**로 사용
- Spring의 의존성 주입(DI)과 자동 구성(Auto-configuration) 활용
- 프로덕션 레벨의 AI 애플리케이션 개발 지원

#### 일반 API vs Spring AI
```java
// ❌ 일반 API 호출 - 복잡하고 반복적
public String callGemini(String prompt) {
    // HTTP 클라이언트 설정
    // 요청 헤더 구성
    // JSON 직렬화/역직렬화
    // 에러 처리
    // 재시도 로직
    // ... 수십 줄의 보일러플레이트 코드
}

// ✅ Spring AI - 간단하고 강력
@Autowired
private ChatModel chatModel;

public String askQuestion(String question) {
    return chatModel.call(question);  // 한 줄로 끝!
}
```

### 1.2 Function Calling이란?

Function Calling은 LLM이 **필요한 도구를 스스로 선택하고 호출**하는 기능입니다.

#### "도구"란 무엇인가?
Function Calling에서 "도구(Tool)"는 다양한 형태가 될 수 있습니다:
- **외부 API**: 날씨 API, 지도 API, 결제 API 등
- **내부 함수**: 데이터 처리, 계산, 변환 함수
- **데이터베이스 쿼리**: 사용자 정보 조회, 여행 기록 검색
- **파일 시스템**: 이미지 처리, 문서 읽기/쓰기
- **다른 AI 모델**: OCR, 번역, 이미지 생성
- **시스템 명령**: 이메일 발송, 알림 전송

#### 작동 원리
```
사용자: "부산 날씨 어때?"
    ↓
LLM: "날씨 정보가 필요하구나"
    ↓
LLM: getWeatherInfo("부산") 호출 결정
    ↓
시스템: 실제 함수 실행 (날씨 API 호출)
    ↓
LLM: "부산은 현재 맑고 기온은 18도입니다"
```

#### Spring AI에서 Function Calling 구현
```java
@Configuration
public class FunctionConfig {
    
    @Bean
    @Description("도시의 날씨 정보를 가져옵니다")  // LLM이 이해할 설명
    public Function<WeatherRequest, WeatherResponse> getWeatherInfo() {
        return request -> {
            // 실제 날씨 API 호출 또는 DB 조회
            String city = request.city();
            WeatherData data = weatherService.getWeather(city);
            
            return new WeatherResponse(
                data.getTemperature(),
                data.getCondition()
            );
        };
    }
}

// 사용 - LLM이 알아서 필요하면 호출
public String chat(String userInput) {
    return chatModel.call(userInput);  // "부산 날씨 알려줘" → 자동으로 getWeatherInfo 호출
}
```

### 1.3 Compass 프로젝트 개요

#### 프로젝트 목적
- **AI 기반 맞춤형 여행 계획 생성**
- 사용자의 선호도와 제약사항을 고려한 최적 일정 제공
- 실시간 날씨, 예약 정보 등을 반영한 현실적인 계획

#### 기술 스택
- **Framework**: Spring Boot 3.x, Java 17
- **AI/ML**: Spring AI 1.0.0-M5 (Gemini 2.0 Flash, GPT-4o-mini)
- **Database**: AWS RDS PostgreSQL, Redis 7
- **Security**: JWT 인증
- **Deployment**: Docker, AWS

#### 팀 구성 (5개 도메인)
1. **USER Domain** - 인증/인가, 프로필 관리
2. **CHAT1 Domain** - 기본 채팅 기능
3. **CHAT2 Domain** - LLM 통합, Function Calling, Follow-up 질문 ⭐
4. **MEDIA Domain** - 이미지 처리, OCR
5. **TRIP Domain** - 여행 계획, RAG 추천

### 1.4 시스템 아키텍처

```
사용자 입력
    ↓
MainLLMOrchestrator (Gemini 2.0 Flash)
    ↓
Function Calling 의사결정
    ↓
도구 선택 및 실행
    ├─→ 정보 수집 도구
    │    ├─→ showQuickInputForm()
    │    ├─→ submitQuickTravelForm()
    │    ├─→ analyzeDetailedNeeds()
    │    └─→ processReservationOCR()
    │
    ├─→ Follow-up 도구
    │    ├─→ startAdaptiveFollowUp()
    │    └─→ continueAdaptiveFollowUp()
    │
    └─→ 여행 계획 도구
         ├─→ generateTravelPlan()
         ├─→ modifyTravelPlan()
         └─→ getWeatherInfo()
```

### 1.5 시스템 프롬프트 vs 프롬프트 템플릿

#### 시스템 프롬프트 (고정, ~50 토큰)
```java
String systemPrompt = """
    당신은 한국인 여행객을 위한 AI 여행 플래너 '컴패스'입니다.
    친근하고 공손한 한국어를 사용하며, Function Calling으로 작업을 수행합니다.
    사용자의 예산과 취향을 최우선으로 고려합니다.
    """;
```

#### 프롬프트 템플릿 (동적, 상황별)
```java
String followUpPrompt = """
    현재 수집된 정보: {collectedInfo}
    사용자 응답 패턴: {responsePattern}
    
    다음 질문을 결정하세요:
    1. 필수 정보 중 누락된 것
    2. 사용자 피로도 고려
    3. 적절한 선택지 제공
    """;
```

---

## 📝 섹션 2: 정보 수집 단계

### 2.1 정보 수집 전략

#### 두 가지 접근 방식
1. **빠른 입력 폼** - 기본 정보를 한 번에 수집
2. **적응형 대화** - 상황에 맞춰 점진적으로 수집

### 2.2 빠른 입력 폼 (Quick Input Form)

#### UI 구조
```javascript
interface QuickTravelForm {
  // 기본 정보
  dates: {
    departure: DatePicker,     // 출발 날짜
    return: DatePicker         // 도착 날짜
  },
  times: {
    departureTime: TimePicker, // 출발 시간 (집 기준)
    returnTime: TimePicker     // 도착 시간 (집 기준)
  },
  travelers: NumberInput,      // 인원수
  budget: RangeSlider,         // 예산
  
  // 예약 정보 (선택)
  reservations: {
    flights: {
      outbound: FlightInfo,
      uploadButton: OCRButton   // "항공권 사진 업로드"
    },
    accommodation: {
      hotel: HotelInfo,
      uploadButton: OCRButton   // "예약 확인서 업로드"
    }
  },
  
  // 목적지/스타일
  destination: {
    selected: string,
    suggestions: DestinationGrid
  },
  travelStyle: MultiSelect     // 복수 선택 가능
}
```

#### OCR 예약 정보 자동 추출
```java
@Bean
@Description("항공권, 호텔 예약서 이미지에서 정보 자동 추출")
public Function<OCRRequest, ReservationInfo> processReservationOCR() {
    return request -> {
        // 이미지 OCR 처리
        String text = ocrService.extractText(request.imageData());
        
        // LLM으로 구조화된 정보 추출
        return switch(request.imageType()) {
            case "FLIGHT" -> extractFlightInfo(text);
            case "HOTEL" -> extractHotelInfo(text);
            default -> extractGeneralInfo(text);
        };
    };
}
```

### 2.3 적응형 Follow-up 시스템

#### 지능형 질문 생성 엔진
```java
public class DetailedNeedsAnalyzer {
    // 동적 우선순위 계산
    private Map<String, Integer> calculatePriorities(TravelInfoCollectionState state) {
        Map<String, Integer> priorities = new HashMap<>();
        
        // 여행 스타일에 따른 우선순위
        if (state.getTravelStyle().contains("맛집")) {
            priorities.put("FOOD", 100);
        }
        if (state.getTravelStyle().contains("휴양")) {
            priorities.put("ACCOMMODATION", 95);
        }
        
        // 동행자에 따른 우선순위
        if (state.getCompanions().contains("가족")) {
            priorities.put("FAMILY_NEEDS", 95);
        }
        
        // 예산에 따른 우선순위
        if (state.getBudget() < 500000) {
            priorities.put("COST_SAVING", 100);
        }
        
        return priorities;
    }
}
```

#### 적응형 전략 (8가지)
1. **QUICK_ESSENTIAL** - 급한 사용자, 핵심만
2. **DETAILED_FRIENDLY** - 여유있는 사용자, 친근하게
3. **EXPERT_EFFICIENT** - 경험 많은 사용자
4. **CASUAL_EXPLORATORY** - 탐색중인 사용자
5. **FAMILY_FOCUSED** - 가족 중심
6. **BUDGET_CONSCIOUS** - 예산 중심
7. **EXPERIENCE_SEEKER** - 특별한 경험 추구
8. **ADAPTIVE_INTELLIGENT** - 상황에 따라 동적 변경

### 2.4 사용자 피로도 관리

#### 피로도 계산 시스템
```java
public class UserFatigueManager {
    private void calculateFatigueScore() {
        double baseScore = 0.0;
        
        // 질문 횟수 (5개 이상시 증가)
        if (questionCount > 5) {
            baseScore += (questionCount - 5) * 10;
        }
        
        // 응답 시간 패턴 (점점 빨라지면 피로)
        if (isGettingFaster()) {
            baseScore += 20;
        }
        
        // 짧은 답변 연속
        if (shortAnswers >= 3) {
            baseScore += shortAnswers * 5;
        }
        
        // 모호한 답변 ("모르겠어", "아무거나")
        baseScore += vagueAnswers * 15;
        
        fatigueScore = Math.min(100, baseScore);
    }
    
    public FatigueAction recommendAction() {
        if (fatigueScore < 30) return CONTINUE_NORMAL;
        if (fatigueScore < 60) return SIMPLIFY_QUESTIONS;
        if (fatigueScore < 80) return OFFER_QUICK_OPTIONS;
        return WRAP_UP_QUICKLY;
    }
}
```

### 2.5 정보 수집 예시

#### 예시: OCR로 예약 정보 자동 입력
```
사용자: "제주도 여행 계획 짜줘"
→ 빠른 입력 폼 표시

사용자: [항공권 사진 업로드]
→ OCR 처리
→ 추출: 김포 09:00 출발, 제주 10:20 도착

사용자: [호텔 예약서 업로드]
→ OCR 처리
→ 추출: 신라호텔, 체크인 15:00, 조식 포함

시스템: "항공권과 호텔 정보를 확인했어요!
        나머지 정보만 알려주시면 완벽한 일정을 만들어드릴게요."
```

---

## 🗺️ 섹션 3: 여행 계획 생성 단계

### 3.1 장소 데이터 소스 및 검색 전략

#### 하이브리드 장소 검색 시스템
여행 계획 생성 시 두 가지 데이터 소스를 활용합니다:

1. **RDS 데이터베이스 (기본 장소)**
   - Tour API로 크롤링한 검증된 장소 정보
   - LLM이 구조화한 JSON 형태로 저장
   - 카테고리별 분류 (관광지, 맛집, 카페, 숙소 등)
   - 빠른 조회와 안정적인 데이터 제공

2. **Perplexity API (트렌디한 장소)**
   - Function Calling으로 실시간 검색
   - 사용자 니즈를 반영한 맞춤형 검색
   - 최신 트렌드와 핫플레이스 발견
   - 테마별 특화 장소 탐색

#### Perplexity Function Calling 구현
```java
@Bean
@Description("사용자 니즈를 반영한 트렌디한 장소를 실시간으로 검색합니다")
public Function<TrendyPlaceSearchRequest, TrendyPlaceSearchResponse> searchTrendyPlaces() {
    return request -> {
        // Perplexity API 호출
        String query = buildSearchQuery(
            request.destination(),
            request.theme(),
            request.userPreferences(),
            request.travelStyle()
        );
        
        PerplexityResponse response = perplexityClient.search(query);
        
        // LLM이 검색 결과 평가 및 필터링
        List<Place> trendyPlaces = evaluateAndFilter(response);
        
        return new TrendyPlaceSearchResponse(trendyPlaces);
    };
}
```

#### 장소 선택 로직
```java
@Bean
@Description("DB와 Perplexity 검색 결과를 종합하여 최적의 장소를 선택합니다")
public Function<PlaceSelectionRequest, PlaceSelectionResponse> selectOptimalPlaces() {
    return request -> {
        // 1. RDS에서 기본 장소 조회
        List<Place> dbPlaces = placeRepository.findByDestinationAndCategory(
            request.destination(),
            request.categories()
        );
        
        // 2. Perplexity로 트렌디한 장소 검색
        List<Place> trendyPlaces = searchTrendyPlaces(request);
        
        // 3. LLM이 종합 평가하여 최적 조합 선택
        return llmPlaceEvaluator.selectBestCombination(
            dbPlaces,           // 검증된 기본 장소
            trendyPlaces,       // 트렌디한 신규 장소
            request.userNeeds(), // 사용자 요구사항
            request.constraints() // 시간, 예산 제약
        );
    };
}
```

### 3.2 여행 계획 생성 전략

#### 계획 전략 유형
```java
public enum PlanningStrategy {
    USER_PLACES_CENTERED,    // 사용자 지정 장소 중심
    PACKED_SCHEDULE,         // 타이트한 일정 (9-10개/일)
    RELAXED_SCHEDULE,        // 여유로운 일정 (5-6개/일)
    BALANCED_SCHEDULE        // 균형잡힌 일정 (7개/일)
}
```

### 3.3 시간 정보 활용

#### 실제 관광 가능 시간 계산
```java
private TravelTimeInfo calculateActualTravelTime(TravelInfoCollectionState state) {
    // 첫날: 집→목적지 이동 시간 계산
    Duration travelTime = calculateTravelTime(departure, destination);
    LocalTime arrivalTime = departureTime.plus(travelTime);
    
    // 마지막날: 목적지→집 이동 시간 역산
    Duration returnTime = calculateTravelTime(destination, departure);
    LocalTime mustLeaveTime = returnTime.minus(returnTime);
    
    // 시간별 적정 장소 수
    if (availableHours < 3) return 1;      // 3시간 미만: 1곳
    if (availableHours < 5) return 2;      // 5시간 미만: 2곳
    if (availableHours < 8) return 4;      // 8시간 미만: 3-4곳
    if (availableHours < 11) return 6;     // 11시간 미만: 5-6곳
    return 8;                               // 전일: 7-8곳
}
```

### 3.4 날씨 연동 (1주일 이내 자동)

#### 날씨 기반 제약사항 생성
```java
private TravelConstraints generateWeatherConstraints(WeatherData weather) {
    TravelConstraints constraints = new TravelConstraints();
    
    // 비/눈 예보
    if (weather.getPrecipitation() > 60) {
        constraints.add("INDOOR_PRIORITY", "실내 활동 위주");
        constraints.add("RAIN_GEAR", "우산/우비 필수");
    }
    
    // 극한 기온
    if (weather.getTemperature() > 35) {
        constraints.add("HEAT_WARNING", "폭염 주의");
        constraints.add("AVOID_MIDDAY", "정오~3시 야외 자제");
    } else if (weather.getTemperature() < -5) {
        constraints.add("COLD_WARNING", "한파 주의");
        constraints.add("INDOOR_WARMUP", "실내 휴식 확보");
    }
    
    return constraints;
}
```

### 3.5 여행 계획 수정 기능

#### 대화형 수정 (8가지 유형)
```java
@Bean
@Description("생성된 여행 계획을 사용자 요청에 따라 수정")
public Function<ModifyPlanRequest, TravelPlanResponse> modifyTravelPlan() {
    return request -> {
        ModificationIntent intent = analyzeIntent(request.modification());
        
        return switch(intent.getType()) {
            case ADD_PLACE -> addPlaceToPlan(plan, intent);
            case REMOVE_PLACE -> removeFromPlan(plan, intent);
            case CHANGE_TIME -> adjustTiming(plan, intent);
            case SWAP_PLACES -> swapPlaces(plan, intent);
            case CHANGE_DAY -> moveToDifferentDay(plan, intent);
            case ADJUST_PACE -> adjustPace(plan, intent);
            case ADD_MEAL -> addMealStop(plan, intent);
            case EXTEND_STAY -> extendStayTime(plan, intent);
        };
    };
}
```

### 3.6 여행 계획 생성 예시

#### 예시: 하이브리드 장소 선택
```
[사용자 요청]
"제주도 3박4일, 카페 투어 좋아하고 SNS 핫플 가고 싶어"

[장소 검색 프로세스]
1. RDS DB 조회:
   - 성산일출봉, 한라산, 우도 등 기본 관광지
   - 검증된 맛집 리스트
   
2. Perplexity API 검색:
   - "제주도 2024 인스타그램 핫플레이스 카페"
   - "제주도 SNS 인기 스팟 최신"
   - 결과: 애월 신상 카페 3곳, 성산 오션뷰 카페 2곳

3. LLM 최종 선택:
   - 기본 관광지 40% + 트렌디 카페 40% + 맛집 20%
   - 동선 최적화 고려한 배치
```

#### 예시: 시간 제약 반영 당일치기
```
[입력 정보]
- 날짜: 12월 25일
- 출발: 서울 07:00
- 도착: 서울 22:00
- 목적지: 부산

[시간 계산]
- 서울→부산: KTX 3시간
- 실제 관광: 9시간 (10:00-19:00)

[생성된 일정]
10:00 부산역 도착
10:30-12:00 감천문화마을
12:00-13:30 자갈치시장 점심
13:30-15:00 용두산공원
15:00-16:30 해운대
16:30-18:00 광안리
18:00-19:00 부산역 이동
19:00 KTX 탑승
```

#### 예시: 날씨 반영 일정 조정
```
[날씨 정보]
- 제주도, 3일 후
- 비 80%, 강풍 20m/s

[제약사항 생성]
- INDOOR_PRIORITY: 실내 활동 위주
- RAIN_GEAR: 우산/우비 필수
- WIND_WARNING: 해안가 자제

[조정된 일정]
- 한라산 → 제주 아쿠아리움
- 해안 드라이브 → 박물관 투어
- 야외 카페 → 실내 카페
```

#### 예시: 대화형 수정
```
사용자: "둘째날 성산일출봉 대신 한라산 가고 싶어"
→ 장소 변경 + 시간 재조정

사용자: "일정이 너무 빡빡해. 여유롭게 해줘"
→ 장소 7개→5개, 체류시간 1.5h→2h

사용자: "첫날 저녁에 흑돼지 먹고 싶어"
→ 특정 식사 추가
```

---

## 💾 데이터 모델

### 주요 레코드
```java
// 빠른 입력 폼 요청
public record QuickTravelFormRequest(
    List<String> destinations,
    DateRange dates,
    TimeInfo times,
    ReservationInfo reservations,  // 예약 정보
    Integer travelers,
    Integer budget,
    List<String> travelStyle,
    boolean needDestinationSuggestion
) {}

// 시간 정보
public record TimeInfo(
    LocalTime departureTime,
    LocalTime returnTime,
    String departureLocation,
    boolean isHomeBase
) {}

// 예약 정보
public record ReservationInfo(
    FlightInfo outboundFlight,
    FlightInfo returnFlight,
    HotelInfo accommodation,
    TrainInfo trainTicket,
    boolean hasReservations
) {}
```

---

## 🎯 핵심 특징 요약

### 정보 수집 단계
✅ **빠른 입력 폼** - 기본 정보 한번에 입력  
✅ **OCR 자동 입력** - 예약서 사진으로 자동 추출  
✅ **적응형 질문** - 8가지 전략으로 맞춤 질문  
✅ **피로도 관리** - 사용자 상태 감지 및 조정  

### 여행 계획 단계
✅ **시간 정보 활용** - 실제 관광 가능 시간 계산  
✅ **날씨 자동 확인** - 1주일 이내 자동 연동  
✅ **대화형 수정** - 8가지 수정 유형 지원  
✅ **제약사항 반영** - 날씨, 시간, 예약 정보 고려  

### 기술적 특징
✅ **Spring AI 활용** - 간단한 LLM 통합  
✅ **Function Calling** - LLM이 도구 자동 선택  
✅ **토큰 최적화** - 시스템 프롬프트 50토큰  
✅ **모듈화 설계** - 5개 도메인 독립 개발  

---

## 📊 토큰 최적화 전략

| 기능 | 기존 방식 | 최적화 방식 | 절감률 |
|-----|---------|----------|-------|
| 시스템 프롬프트 | 매번 포함 | 50토큰 고정 | 90% |
| 장소 분류 | 700 토큰/회 | 100 토큰/회 | 86% |
| Follow-up 결정 | 800 토큰/회 | 200 토큰/회 | 75% |
| **세션당 총합** | 8,000-10,000 | 1,500-2,000 | 80% |

---

## 🚀 구현 우선순위

### Phase 1: 핵심 기능 (1주차)
- [x] MainLLMOrchestrator 구현
- [x] 빠른 입력 폼 UI
- [x] 기본 Follow-up 시스템
- [x] 여행 계획 생성

### Phase 2: 고도화 (2주차)
- [x] OCR 예약 정보 추출
- [x] 적응형 질문 전략
- [x] 날씨 API 연동
- [x] 대화형 수정 기능

### Phase 3: 최적화 (3주차)
- [ ] 피로도 관리 정교화
- [ ] 토큰 사용량 모니터링
- [ ] 응답 속도 개선
- [ ] 사용자 피드백 반영