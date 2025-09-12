# 🎯 LLM 중앙 통제 기반 여행 계획 추천 시스템 V2

## 📋 목차

### Part 1: 시스템 이해
0. [Spring AI 소개 및 핵심 개념](#0-spring-ai-소개-및-핵심-개념)
   - 0.1 [Spring AI란?](#01-spring-ai란)
   - 0.2 [우리 프로젝트에서 Spring AI를 사용하는 이유](#02-우리-프로젝트에서-spring-ai를-사용하는-이유)
   - 0.3 [Spring AI의 핵심 장점](#03-spring-ai의-핵심-장점)
   - 0.4 [Function Calling이란?](#04-function-calling이란)
   - 0.5 [Spring AI에서 Function Calling 구현하기](#05-spring-ai에서-function-calling-구현하기)
   - 0.6 [우리 프로젝트의 Function Calling 전략](#06-우리-프로젝트의-function-calling-전략)

1. [시스템 개요](#1-시스템-개요)
   - 1.1 [목적 및 비전](#11-목적-및-비전)
   - 1.2 [핵심 특징](#12-핵심-특징)
   - 1.3 [기대 효과](#13-기대-효과)

2. [아키텍처](#2-아키텍처)
   - 2.1 [시스템 아키텍처](#21-시스템-아키텍처)
   - 2.2 [Function Calling 구조](#22-function-calling-구조)
   - 2.3 [데이터 흐름](#23-데이터-흐름)

### Part 2: Function 도구
3. [Function Calling 도구 상세](#3-function-calling-도구-상세)
   - 3.1 [Adaptive Follow-up 도구](#31-adaptive-follow-up-도구)
   - 3.2 [검색 및 추천 도구](#32-검색-및-추천-도구)
   - 3.3 [여행 계획 도구](#33-여행-계획-도구)
   - 3.4 [외부 API 연동 도구](#34-외부-api-연동-도구)
   - 3.5 [데이터베이스 도구](#35-데이터베이스-도구)
   - 3.6 [분석 도구](#36-분석-도구)

### Part 3: 구현
4. [핵심 구현](#4-핵심-구현)
   - 4.1 [MainLLMOrchestrator](#41-mainllmorchestrator)
   - 4.2 [Function Configuration](#42-function-configuration)
   - 4.3 [프롬프트 엔지니어링](#43-프롬프트-엔지니어링)

5. [대화 플로우](#5-대화-플로우)
   - 5.1 [기본 대화 플로우](#51-기본-대화-플로우)
   - 5.2 [Adaptive Follow-up 플로우](#52-adaptive-follow-up-플로우)
   - 5.3 [여행 계획 생성 플로우](#53-여행-계획-생성-플로우)

### Part 4: 프로젝트 관리
6. [구현 계획](#6-구현-계획)
   - 6.1 [팀 구성 및 역할](#61-팀-구성-및-역할)
   - 6.2 [개발 일정](#62-개발-일정)
   - 6.3 [성공 지표](#63-성공-지표)

7. [부록](#7-부록)
   - 7.1 [장점 및 특징](#71-장점-및-특징)
   - 7.2 [주의사항](#72-주의사항)
   - 7.3 [참고자료](#73-참고자료)

---

# Part 1: 시스템 이해

## 0. Spring AI 소개 및 핵심 개념

### 0.1 Spring AI란?
Spring AI는 스프링 생태계에서 **AI/LLM을 쉽게 통합**할 수 있도록 제공하는 공식 프레임워크입니다.
- OpenAI, Gemini, Claude 등 다양한 LLM을 **통일된 인터페이스**로 사용
- Spring의 의존성 주입(DI)과 자동 구성(Auto-configuration) 활용
- 프로덕션 레벨의 AI 애플리케이션 개발 지원

### 0.2 우리 프로젝트에서 Spring AI를 사용하는 이유

#### 일반 API 호출의 문제점
```java
// ❌ 일반 API 호출 방식 - 복잡하고 반복적
public String callGemini(String prompt) {
    // HTTP 클라이언트 설정
    // 요청 헤더 구성
    // JSON 직렬화/역직렬화
    // 에러 처리
    // 재시도 로직
    // 토큰 계산
    // ... 수십 줄의 보일러플레이트 코드
}
```

#### Spring AI 사용 시 장점
```java
// ✅ Spring AI 방식 - 간단하고 강력
@Autowired
private ChatModel chatModel;

public String askQuestion(String question) {
    return chatModel.call(question);  // 한 줄로 끝!
}
```

### 0.3 Spring AI의 핵심 장점

1. **통일된 인터페이스**
   - 모델을 바꿔도 코드 변경 최소화
   - Gemini → GPT-4 전환이 설정 변경만으로 가능

2. **Function Calling 자동화**
   - 복잡한 Function Calling을 자동으로 처리
   - LLM이 필요한 함수를 자동으로 선택하고 실행

3. **Spring 생태계 통합**
   - `@Bean`, `@Component` 등 익숙한 Spring 어노테이션 사용
   - Spring Boot의 자동 구성으로 설정 간소화

4. **엔터프라이즈 기능**
   - 자동 재시도, 에러 처리, 로깅
   - 메트릭 수집, 모니터링 지원
   - 스트리밍 응답 처리

### 0.4 Function Calling이란?

**Function Calling**은 LLM이 **언제, 어떤 함수를 호출해야 할지 스스로 결정**하는 기능입니다.

#### 작동 원리
```
사용자: "부산 날씨 어때?"
    ↓
LLM: "날씨 정보가 필요하구나"
    ↓
LLM: getWeatherInfo("부산") 호출 결정
    ↓
시스템: 실제 함수 실행
    ↓
LLM: "부산은 현재 맑고 기온은 18도입니다"
```

#### 핵심 포인트
- LLM은 **함수를 직접 실행하지 않음** (실행 지시만)
- 시스템이 LLM의 지시를 받아 **실제 함수 실행**
- DB 접근, API 호출 등 **모든 작업을 Function으로 래핑 가능**

### 0.5 Spring AI에서 Function Calling 구현하기

#### Step 1: Function 정의 (Java Bean으로)
```java
@Configuration
public class WeatherFunctions {
    
    @Bean
    @Description("도시의 날씨 정보를 가져옵니다")  // LLM이 이해할 설명
    public Function<WeatherRequest, WeatherResponse> getWeatherInfo() {
        return request -> {
            // 실제 날씨 API 호출 또는 DB 조회
            String city = request.city();
            WeatherData data = weatherService.getWeather(city);
            
            return new WeatherResponse(
                data.getTemperature(),
                data.getCondition(),
                data.getHumidity()
            );
        };
    }
}

// 요청/응답 레코드 (Java 14+ record 사용)
public record WeatherRequest(String city) {}
public record WeatherResponse(int temperature, String condition, int humidity) {}
```

#### Step 2: ChatModel 설정
```java
@Configuration
public class AIConfig {
    
    @Bean
    public ChatModel chatModel() {
        return ChatModel.builder()
            .withModel("gemini-2.0-flash")
            .withFunctions(weatherFunctions())  // Function 등록
            .build();
    }
}
```

#### Step 3: 사용
```java
@Service
public class ChatService {
    @Autowired
    private ChatModel chatModel;
    
    public String chat(String userInput) {
        // LLM이 알아서 필요하면 getWeatherInfo를 호출
        return chatModel.call(userInput);
    }
}
```

### 0.6 우리 프로젝트의 Function Calling 전략

```
사용자 입력 → Gemini 2.0 Flash (오케스트레이터)
                    ↓
            자동으로 필요한 Function 선택
                    ↓
    ┌──────────────┼──────────────┐
    ↓              ↓              ↓
여행정보수집    장소검색      계획생성
(Follow-up)   (Search)    (Generate)
```

**핵심**: 모든 비즈니스 로직을 Function으로 만들고, LLM이 상황에 맞게 선택하도록 구성

---

## 1. 시스템 개요

### 1.1 목적 및 비전
메인 LLM이 중앙 오케스트레이터 역할을 하며, 모든 기능을 Function Calling으로 호출하는 지능형 여행 계획 시스템

### 1.2 핵심 특징
✅ **단일 진입점**: 모든 사용자 요청이 메인 LLM을 통해 처리  
✅ **도구 기반 아키텍처**: 모든 기능이 독립적인 Function으로 구현  
✅ **동적 의사결정**: LLM이 상황에 따라 필요한 도구 선택  
✅ **확장 가능**: 새로운 기능을 Function으로 추가하기 쉬움  

### 1.3 기대 효과
- 사용자 맞춤형 여행 계획 자동 생성
- 실시간 정보 기반 추천
- 대화형 인터페이스를 통한 자연스러운 계획 수립
- 지속적인 학습을 통한 추천 품질 향상

---

## 2. 아키텍처

### 2.1 시스템 아키텍처

```
사용자 입력
    ↓
[메인 LLM Orchestrator]
    │
    ├─→ 직접 응답 (일반 대화, 간단한 질문)
    │
    └─→ Function Calling (필요시)
         ├─→ 꼬리질문 관련
         │    ├─→ startFollowUpSession()      // 꼬리질문 시작
         │    └─→ continueFollowUp()          // 꼬리질문 계속
         │
         ├─→ 검색 및 추천
         │    ├─→ recommendTemplates()        // 템플릿 추천
         │    ├─→ searchRegions()             // 지역 검색
         │    ├─→ searchAttractions()         // 관광지 검색
         │    ├─→ searchTourAPI()             // 한국관광공사 API 검색
         │    └─→ searchWithPerplexity()      // 실시간 정보 검색
         │
         ├─→ 여행 계획
         │    ├─→ generateTravelPlan()        // 여행 계획 생성
         │    └─→ getWeatherInfo()            // 날씨 정보 조회
         │
         ├─→ 컨텍스트 관리
         │    ├─→ getConversationContext()    // 대화 컨텍스트 조회
         │    └─→ updateConversationContext() // 컨텍스트 업데이트
         │
         └─→ 데이터베이스 조작
              ├─→ saveUserPreferences()       // 사용자 선호도 저장
              ├─→ saveTravelHistory()         // 여행 이력 저장
              ├─→ queryUserTravelData()       // 사용자 데이터 조회
              ├─→ updateTravelPlanStatus()    // 계획 상태 업데이트
              ├─→ searchSimilarTravels()      // 유사 여행 검색
              ├─→ createOrUpdateItinerary()   // 일정 생성/수정
              └─→ analyzeUserTravelPatterns() // 여행 패턴 분석
```

### 2.2 Function Calling 구조

메인 LLM이 Function Calling으로 사용할 수 있는 모든 도구들:

| 카테고리 | Function 이름 | 설명 |
|---------|--------------|------|
| **정보 수집** | showQuickInputForm | 빠른 입력 폼 UI 제시 |
| | submitQuickTravelForm | 빠른 입력 폼 제출 처리 |
| | analyzeDetailedNeeds | 세부 니즈 파악 |
| | startAdaptiveFollowUp | 적응형 꼬리질문 세션 시작 |
| | continueAdaptiveFollowUp | 꼬리질문 계속 진행 |
| **검색/추천** | recommendTemplates | 여행 템플릿 추천 |
| | searchRegions | 지역 검색 |
| | searchAttractions | 관광지 검색 |
| | searchTourAPI | 한국관광공사 API 검색 |
| | searchWithPerplexity | 실시간 정보 검색 |
| **여행 계획** | generateTravelPlan | 여행 일정 생성 |
| | getWeatherInfo | 날씨 정보 조회 |
| **컨텍스트** | getConversationContext | 대화 컨텍스트 조회 |
| | updateConversationContext | 컨텍스트 업데이트 |
| **데이터베이스** | saveUserPreference | 사용자 선호도 저장 |
| | saveTravelHistory | 여행 기록 저장 |
| | queryUserTravelData | 사용자 데이터 조회 |
| | updateTravelPlanStatus | 계획 상태 업데이트 |
| | saveGeneratedItinerary | 생성된 일정 저장 |
| **분석** | analyzeUserTravelPatterns | 여행 패턴 분석 |

### 2.3 데이터 흐름
1. 사용자 입력 → MainLLMOrchestrator
2. LLM이 의도 파악 및 필요 도구 선택
3. Function Calling 실행
4. 결과 통합 및 응답 생성
5. 사용자에게 전달

---

# Part 2: Function 도구

## 3. Function Calling 도구 상세

### 3.0 시스템 프롬프트와 프롬프트 템플릿 설정

#### 시스템 프롬프트 (고정된 기본 지침)
시스템 프롬프트는 LLM의 **정체성과 기본 행동 규칙**만 포함합니다. 매 API 호출마다 토큰을 소비하므로 최소한으로 유지합니다.

```java
@Configuration
public class ChatModelConfig {
    
    @Bean
    public ChatModel chatModel() {
        // 시스템 프롬프트: 최소한의 핵심 지침만 포함 (토큰 절약)
        String systemPrompt = """
            당신은 한국인 여행객을 위한 AI 여행 플래너 '컴패스'입니다.
            친근하고 공손한 한국어를 사용하며, Function Calling으로 작업을 수행합니다.
            사용자의 예산과 여행스타일을 최우선으로 고려합니다.
            """;
        
        return ChatModel.builder()
            .withSystemPrompt(systemPrompt)  // 간결한 시스템 프롬프트 (약 50토큰)
            .withModel("gemini-2.0-flash")  // Gemini 2.0 Flash 사용
            .withTemperature(0.7)
            .build();
    }
}
```

#### 프롬프트 템플릿 (상황별 동적 지침)
특정 작업에 필요한 세부 지침은 프롬프트 템플릿으로 관리합니다.

```java
@Component
public class PromptTemplates {
    
    // 장소 카테고리 분류용 템플릿
    public static final String CATEGORIZE_PLACES = """
        사용자가 언급한 장소들을 다음 12개 카테고리로 분류해주세요:
        1. 관광명소: 궁궐, 타워, 유적지, 공원
        2. 자연경관: 산, 바다, 호수, 해변
        3. 문화예술: 박물관, 미술관
        4. 테마파크: 놀이공원, 동물원
        5. 맛집음식: 식당, 카페, 시장
        6. 쇼핑: 백화점, 아울렛
        7. 액티비티: 스포츠, 체험
        8. 웰니스: 스파, 온천
        9. 나이트라이프: 바, 클럽, 야경
        10. 종교문화: 절, 성당
        11. 교통이동: 케이블카, 유람선
        12. 숙박시설: 호텔, 펜션, 에어비앤비, 게스트하우스
        
        사용자 입력: {userInput}
        JSON 형식으로 응답해주세요.
        """;
    
    // 여행 정보 추출용 템플릿
    public static final String EXTRACT_TRAVEL_INFO = """
        사용자의 메시지에서 다음 여행 정보를 추출하세요:
        - 목적지 (2일 이상이면 다중 목적지 가능)
        - 출발일/도착일
        - 여행 인원
        - 예산
        - 여행 스타일
        
        사용자 메시지: {message}
        현재 수집된 정보: {collectedInfo}
        """;
    
    // Follow-up 질문 생성용 템플릿  
    public static final String GENERATE_FOLLOWUP = """
        현재 여행 정보 수집 상태: {currentState}
        
        기준:
        1. 필수 정보 중 누락된 것 우선
        2. 2일 이상 여행인데 1개 도시만 있으면 추가 도시 제안
        3. 한 번에 하나씩만 질문
        
        다음 질문을 JSON으로 응답해주세요.
        """;
    
    // 여행 계획 생성용 템플릿
    public static final String GENERATE_TRAVEL_PLAN = """
        여행 정보:
        - 목적지: {destinations}
        - 날짜: {dates}
        - 인원: {travelers}
        - 예산: {budget}
        - 스타일: {travelStyle}
        
        위 정보를 바탕으로 상세한 여행 계획을 생성해주세요.
        추천 시 항상 3-5개 옵션을 제공합니다.
        """;
}
```

이제 각 Function은 **간단한 사용자 프롬프트**만 추가하면 됩니다:

```java
// Before: 모든 것을 매번 설명 (700 토큰)
String prompt = """
    당신은 여행 플래너입니다...
    12개 카테고리는...
    규칙은...
    장소를 분류해주세요: 경복궁
    """;

// After: 시스템 프롬프트 + 간단한 요청 (50 토큰)
String userPrompt = """
    장소 분류: 경복궁, 해운대
    JSON 응답
    """;
```

### 3.1 Adaptive Follow-up 도구 & 빠른 입력 인터페이스

여행 정보 수집을 위한 **두 가지 접근 방식**을 제공합니다.

#### 방식 1: 빠른 입력 인터페이스 (Quick Input Form) → 세부 니즈 파악
사용자가 여행 계획을 언급하면 **기본 정보를 빠르게 수집**한 후, Function Calling으로 **세부 니즈를 파악**합니다.

**프로세스**:
1. **빠른 입력 폼** → 기본 정보 수집 (목적지, 날짜, 인원, 예산, 스타일)
2. **Function Calling** → 세부 니즈 파악
   - 선호 활동 상세 (아침형/저녁형, 실내/실외)
   - 특별 요구사항 (알레르기, 어린이 동반, 휠체어 접근성)
   - 관심사 깊이 파악 (역사 깊이 있게 vs 사진만, 맛집 로컬 vs 유명)
   - 이동 수단 선호 (렌터카, 대중교통, 도보)
   - 숙박 선호 (위치, 등급, 조식 포함 여부)
3. **여행 계획 생성** → 맞춤형 일정 제공

```javascript
// 사용자: "부산 여행 가고 싶어"
// → 시스템이 빠른 입력 폼 제시

interface QuickTravelForm {
  // 직접 입력 필드 (대부분 정해져 있음)
  dates: {
    departure: DatePicker,     // 출발 날짜
    return: DatePicker         // 도착 날짜
  },
  times: {
    departureTime: TimePicker, // 출발 시간 (예: "07:00", "집에서 출발")
    returnTime: TimePicker     // 도착 시간 (예: "22:00", "집 도착 기준")
  },
  travelers: NumberInput,      // 인원수 (1-10명)
  budget: RangeSlider,         // 예산 범위 (10만-500만)
  
  // 예약 정보 (선택사항)
  reservations: {
    flights: {
      outbound: FlightInfo,    // 가는 항공편
      return: FlightInfo,       // 오는 항공편
      uploadButton: OCRButton   // "항공권 사진 업로드" 버튼
    },
    accommodation: {
      hotel: HotelInfo,         // 호텔/숙소 정보
      checkIn: DateTime,        // 체크인 시간
      checkOut: DateTime,       // 체크아웃 시간
      uploadButton: OCRButton   // "예약 확인서 업로드" 버튼
    }
  }
  
  // 선택/추천 필드 (미정일 가능성 높음)
  destination: {
    selected: string,          // 사용자가 언급한 목적지
    suggestions: [             // 인기 목적지 제안
      "제주도", "부산", "강릉", "경주", "여수",
      "일본", "태국", "베트남", "대만"
    ],
    multiCity: boolean         // 다중 도시 여행 옵션
  },
  
  travelStyle: {
    options: [                 // 여행 스타일 선택
      { id: "relax", label: "🏖️ 휴양", desc: "여유롭게 쉬는 여행" },
      { id: "sightseeing", label: "📸 관광", desc: "명소 중심 여행" },
      { id: "activity", label: "🎿 액티비티", desc: "활동적인 여행" },
      { id: "food", label: "🍜 맛집투어", desc: "음식 중심 여행" },
      { id: "culture", label: "🏛️ 문화탐방", desc: "박물관/전시 중심" }
    ],
    multiSelect: true          // 복수 선택 가능
  }
}
```

**UI 예시**:
```
┌─────────────────────────────────────────────┐
│ 🧳 여행 정보를 입력해주세요                    │
├─────────────────────────────────────────────┤
│ 📍 목적지: [부산 ▼] + 다른 도시 추가          │
│    추천: 제주 | 강릉 | 경주 | 여수            │
│                                             │
│ 📅 날짜: [2024.3.15] ~ [2024.3.17]          │
│    □ 아직 미정                               │
│                                             │
│ 👥 인원: [2명 ▼]                            │
│                                             │
│ 💰 예산: [────●────] 50만원                 │
│    □ 아직 미정                               │
│                                             │
│ ✨ 여행 스타일: (복수 선택 가능)              │
│    [✓] 관광  [ ] 휴양  [✓] 맛집             │
│    [ ] 액티비티  [ ] 문화                    │
│                                             │
│         [건너뛰기]  [여행 계획 만들기]         │
└─────────────────────────────────────────────┘
```

#### 방식 2: Adaptive Follow-up (대화형)
정보가 부족하거나 사용자가 대화를 선호하는 경우 **자연스러운 대화**로 정보 수집

**📋 필수 정보 체크리스트**:
1. **목적지** (destinations) - 어디로 가는지
   - 단일 목적지: 3일 이하 단기 여행
   - 다중 목적지: 4일 이상 장기 여행 가능
   - 자동 판단: 여행 기간에 따라 추가 목적지 제안
2. **날짜** (dates) - 언제 출발/도착
3. **인원** (travelers) - 몇 명이 가는지
4. **예산** (budget) - 대략적인 예산 범위
5. **여행 스타일** (style) - 휴양/관광/액티비티 등

**🎯 선택 정보 (자동 수집)**:
6. **방문 희망 장소** (wishPlaces) - 특정 장소/명소
   - 카테고리 자동 분류: 12개 주요 카테고리
   - 유사 장소 추천: 같은 카테고리 내 인기 장소
   - 동선 최적화: 위치 기반 일정 구성
   
   **📂 12개 확장 카테고리**:
   - **관광명소**: 유적지, 전망대, 랜드마크, 문화유산
   - **자연경관**: 산, 바다, 호수, 계곡, 섬, 동굴
   - **문화예술**: 박물관, 미술관, 공연장, 전시관
   - **테마파크**: 놀이공원, 워터파크, 동물원, 아쿠아리움
   - **맛집음식**: 현지맛집, 미슐랭, 카페, 디저트, 전통시장
   - **쇼핑**: 백화점, 아울렛, 재래시장, 기념품샵, 편집샵
   - **액티비티**: 수상스포츠, 하이킹, 스키, 골프, 낚시
   - **웰니스**: 스파, 온천, 마사지, 요가, 명상
   - **나이트라이프**: 바, 클럽, 야경명소, 야시장
   - **종교문화**: 사찰, 성당, 교회, 신사
   - **교통이동**: 케이블카, 유람선, 관광열차, 전망버스
   - **숙박시설**: 호텔, 리조트, 펜션, 게스트하우스, 캠핑

**🔄 동작 방식**:
- 사용자의 모든 대화에서 필수 정보를 자동 추출
- 이미 언급된 정보는 체크리스트에서 자동 차감
- 남은 필수 정보만 효율적으로 질문
- 모든 필수 정보가 수집되면 여행 계획 생성 가능

**⚠️ 중요**: 실제 대화 제어는 항상 **MainLLMOrchestrator**가 담당하며, Follow-up 도구는 남은 필수 정보를 제안하는 역할만 수행

#### submitQuickTravelForm (빠른 입력 처리)
```java
@Bean
public Function<QuickTravelFormRequest, TravelPlanResponse> submitQuickTravelForm() {
    return request -> {
        log.info("빠른 여행 정보 폼 제출 처리");
        
        // 입력된 정보 검증
        TravelInfoValidation validation = validateFormInput(request);
        if (!validation.isValid()) {
            // 누락된 필수 정보가 있으면 해당 필드 표시
            return new TravelPlanResponse(
                null,
                null,
                validation.getMissingFields(),
                "필수 정보를 모두 입력해주세요"
            );
        }
        
        // 세션 생성 및 정보 저장
        String sessionId = UUID.randomUUID().toString();
        TravelInfoCollectionState state = new TravelInfoCollectionState();
        state.setSessionId(sessionId);
        state.setDestinations(request.destinations());
        state.setDates(request.dates());
        state.setTravelers(request.travelers());
        state.setBudget(request.budget());
        state.setTravelStyle(request.travelStyle());
        
        // 목적지 제안 처리 (사용자가 목적지 미정인 경우)
        if (request.needDestinationSuggestion()) {
            List<String> suggestions = suggestDestinations(
                request.dates(),
                request.budget(),
                request.travelStyle()
            );
            return new TravelPlanResponse(
                sessionId,
                null,
                suggestions,
                "추천 목적지를 선택해주세요"
            );
        }
        
        // 기본 정보만 저장하고, 추가 니즈 파악 단계로 진행
        sessionService.saveSession(state);
        
        return new TravelPlanResponse(
            sessionId,
            null,
            null,
            "기본 정보 수집 완료! 이제 세부 니즈를 파악하겠습니다."
        );
    };
}

// 요청/응답 레코드 (시간 정보 + 예약 정보 추가)
public record QuickTravelFormRequest(
    List<String> destinations,    // 선택된 목적지
    DateRange dates,              // 출발/도착 날짜
    TimeInfo times,               // 출발/도착 시간
    ReservationInfo reservations, // 예약 정보 (NEW!)
    Integer travelers,            // 인원수
    Integer budget,              // 예산
    List<String> travelStyle,    // 여행 스타일 (복수 선택)
    boolean needDestinationSuggestion  // 목적지 추천 필요 여부
) {}

// 시간 정보 레코드
public record TimeInfo(
    LocalTime departureTime,     // 출발 시간 (집 출발 기준)
    LocalTime returnTime,         // 도착 시간 (집 도착 기준)
    String departureLocation,     // 출발 위치 (예: "서울 강남")
    boolean isHomeBase           // 집 기준 시간인지 여부
) {}

// 예약 정보 레코드
public record ReservationInfo(
    FlightInfo outboundFlight,   // 가는 항공편
    FlightInfo returnFlight,      // 오는 항공편
    HotelInfo accommodation,      // 숙소 정보
    TrainInfo trainTicket,        // 기차표 (KTX 등)
    boolean hasReservations       // 예약 정보 존재 여부
) {}

// 항공 정보
public record FlightInfo(
    String airline,               // 항공사
    String flightNumber,          // 편명
    String departureAirport,      // 출발 공항
    String arrivalAirport,        // 도착 공항
    LocalDateTime departureTime,  // 출발 시간
    LocalDateTime arrivalTime,    // 도착 시간
    String seatNumber,            // 좌석 번호
    String confirmationNumber     // 예약 번호
) {}

// 호텔 정보
public record HotelInfo(
    String hotelName,             // 호텔명
    String address,               // 주소
    LocalDateTime checkIn,        // 체크인
    LocalDateTime checkOut,       // 체크아웃
    String roomType,              // 객실 타입
    List<String> amenities,       // 편의시설
    String confirmationNumber     // 예약 번호
) {}
```

#### showQuickInputForm (폼 UI 제시)
```java
@Bean
public Function<ShowFormRequest, QuickFormResponse> showQuickInputForm() {
    return request -> {
        // 사용자가 이미 언급한 정보 파싱
        Map<String, Object> mentionedInfo = parseUserMessage(request.userMessage());
        
        // 인기 목적지 및 스타일 옵션 준비
        List<String> popularDestinations = getPopularDestinations();
        List<TravelStyleOption> styleOptions = getTravelStyleOptions();
        
        return new QuickFormResponse(
            mentionedInfo.get("destination"),  // 이미 언급된 목적지
            popularDestinations,
            styleOptions,
            getDefaultDateRange(),             // 다음 주말 기본값
            2,                                  // 기본 2명
            500000                             // 기본 50만원
        );
    };
}
```

#### analyzeDetailedNeeds (세부 니즈 파악 - 고도화)
```java
@Bean
public Function<DetailedNeedsRequest, DetailedNeedsResponse> analyzeDetailedNeeds() {
    return request -> {
        log.info("세부 니즈 파악 시작: {}", request.sessionId());
        
        // 기본 정보 조회
        TravelInfoCollectionState state = sessionService.getSession(request.sessionId());
        
        // 이전 응답이 있으면 먼저 처리 및 학습
        if (request.userResponse() != null) {
            processUserResponse(state, request.userResponse());
            updateUserProfile(state, request.userResponse());
        }
        
        // 지능형 질문 생성 엔진
        DetailedNeedsAnalyzer analyzer = new DetailedNeedsAnalyzer(state);
        
        // 동적 우선순위 기반 다음 질문 결정
        NextDetailedQuestion nextQuestion = analyzer.determineNextQuestion();
        
        // 질문이 없으면 완료
        if (nextQuestion == null) {
            return completeDetailedNeeds(state);
        }
        
        // 사용자 피로도 체크 - 너무 많은 질문 방지
        if (state.getQuestionCount() >= 5 && state.hasMinimumInfo()) {
            return new DetailedNeedsResponse(
                request.sessionId(),
                "SUFFICIENT",  // 충분한 정보 수집됨
                "충분한 정보를 수집했습니다. 추가로 알려주실 사항이 있으신가요?",
                0,
                90.0f,
                List.of(
                    new QuickOption("계획 생성하기", "GENERATE"),
                    new QuickOption("추가 정보 입력", "MORE_INFO")
                )
            );
        }
        
        // 적응형 질문 생성
        return new DetailedNeedsResponse(
            request.sessionId(),
            "IN_PROGRESS",
            nextQuestion.getQuestion(),
            analyzer.getRemainingPriorityQuestions(),
            calculateDetailedProgress(state),
            nextQuestion.getQuickOptions(),
            nextQuestion.getCategory(),
            nextQuestion.getPriority()
        );
    };
}

// 지능형 세부 니즈 분석기
private class DetailedNeedsAnalyzer {
    private final TravelInfoCollectionState state;
    private final Map<String, Integer> categoryPriorities;
    
    public DetailedNeedsAnalyzer(TravelInfoCollectionState state) {
        this.state = state;
        this.categoryPriorities = calculatePriorities(state);
    }
    
    private Map<String, Integer> calculatePriorities(TravelInfoCollectionState state) {
        Map<String, Integer> priorities = new HashMap<>();
        
        // 동적 우선순위 계산
        // 1. 여행 스타일에 따른 우선순위
        if (state.getTravelStyle() != null) {
            if (state.getTravelStyle().contains("맛집")) {
                priorities.put("FOOD", 100);
            }
            if (state.getTravelStyle().contains("관광")) {
                priorities.put("ACTIVITY_PATTERN", 90);
                priorities.put("TRANSPORT", 85);
            }
            if (state.getTravelStyle().contains("휴양")) {
                priorities.put("ACCOMMODATION", 95);
                priorities.put("ACTIVITY_PATTERN", 70);
            }
            if (state.getTravelStyle().contains("액티비티")) {
                priorities.put("PHYSICAL_CONDITION", 100);
                priorities.put("ACTIVITY_PATTERN", 95);
            }
        }
        
        // 2. 동행자에 따른 우선순위
        if (state.getCompanions() != null) {
            if (state.getCompanions().contains("가족")) {
                priorities.put("FAMILY_NEEDS", 95);
                priorities.put("ACCOMMODATION", 90);
            }
            if (state.getCompanions().contains("연인")) {
                priorities.put("ROMANTIC_PREFERENCES", 90);
            }
            if (state.getCompanions().contains("친구")) {
                priorities.put("GROUP_DYNAMICS", 85);
            }
        }
        
        // 3. 예산에 따른 우선순위
        if (state.getBudget() != null) {
            int budgetLevel = parseBudgetLevel(state.getBudget());
            if (budgetLevel < 500000) {  // 저예산
                priorities.put("COST_SAVING", 100);
                priorities.put("TRANSPORT", 90);  // 대중교통 선호 가능성
            } else if (budgetLevel > 2000000) {  // 고예산
                priorities.put("PREMIUM_EXPERIENCE", 90);
            }
        }
        
        // 4. 목적지에 따른 우선순위
        if (state.getDestination() != null) {
            if (isOverseas(state.getDestination())) {
                priorities.put("LANGUAGE", 85);
                priorities.put("CULTURAL_PREFERENCES", 80);
            }
            if (isIsland(state.getDestination())) {
                priorities.put("SEASICKNESS", 90);
                priorities.put("WATER_ACTIVITIES", 85);
            }
        }
        
        return priorities;
    }
    
    public NextDetailedQuestion determineNextQuestion() {
        // 우선순위가 가장 높은 미답변 질문 선택
        return categoryPriorities.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .map(entry -> generateQuestionForCategory(entry.getKey()))
            .filter(q -> q != null && !state.hasAnsweredCategory(q.getCategory()))
            .findFirst()
            .orElse(generateContextualQuestion());  // 문맥 기반 추가 질문
    }
    
    private NextDetailedQuestion generateQuestionForCategory(String category) {
        return switch(category) {
            case "FOOD" -> new NextDetailedQuestion(
                "FOOD",
                "맛집 선호도를 알려주세요! 😋",
                List.of(
                    new QuickOption("현지인 맛집", "LOCAL"),
                    new QuickOption("미슐랭/유명 맛집", "FAMOUS"),
                    new QuickOption("둘 다 좋아요", "BOTH")
                ),
                100
            );
            
            case "ACTIVITY_PATTERN" -> new NextDetailedQuestion(
                "ACTIVITY_PATTERN",
                "하루 일정을 어떻게 보내고 싶으신가요?",
                List.of(
                    new QuickOption("아침형 (7시 시작)", "EARLY_BIRD"),
                    new QuickOption("보통 (9-10시 시작)", "NORMAL"),
                    new QuickOption("늦잠형 (11시 이후)", "LATE_START")
                ),
                90
            );
            
            case "TRANSPORT" -> new NextDetailedQuestion(
                "TRANSPORT",
                state.getDestination() + "에서 이동 수단은 어떻게 하실 건가요?",
                List.of(
                    new QuickOption("렌터카", "CAR"),
                    new QuickOption("대중교통", "PUBLIC"),
                    new QuickOption("택시/우버", "TAXI"),
                    new QuickOption("도보 + 대중교통", "WALK")
                ),
                85
            );
            
            case "FAMILY_NEEDS" -> new NextDetailedQuestion(
                "FAMILY_NEEDS",
                "가족 구성원 중 특별히 배려가 필요한 분이 계신가요?",
                List.of(
                    new QuickOption("영유아 동반", "WITH_BABY"),
                    new QuickOption("초등학생 동반", "WITH_KIDS"),
                    new QuickOption("어르신 동반", "WITH_ELDERLY"),
                    new QuickOption("특별 배려 불필요", "NONE")
                ),
                95
            );
            
            case "PHYSICAL_CONDITION" -> new NextDetailedQuestion(
                "PHYSICAL_CONDITION",
                "체력 수준과 선호하는 활동 강도는 어떻게 되세요?",
                List.of(
                    new QuickOption("매우 활동적", "VERY_ACTIVE"),
                    new QuickOption("적당히 활동적", "MODERATE"),
                    new QuickOption("편안한 것 선호", "RELAXED")
                ),
                95
            );
            
            case "ACCOMMODATION" -> new NextDetailedQuestion(
                "ACCOMMODATION",
                "숙소는 어떤 스타일을 선호하세요?",
                List.of(
                    new QuickOption("호텔/리조트", "HOTEL"),
                    new QuickOption("펜션/빌라", "PENSION"),
                    new QuickOption("게스트하우스", "GUESTHOUSE"),
                    new QuickOption("에어비앤비", "AIRBNB")
                ),
                80
            );
            
            default -> null;
        };
    }
    
    private NextDetailedQuestion generateContextualQuestion() {
        // 수집된 정보를 바탕으로 추가 문맥 질문 생성
        if (state.hasFood() && !state.hasDietaryRestrictions()) {
            return new NextDetailedQuestion(
                "DIETARY",
                "혹시 못 드시는 음식이나 알레르기가 있으신가요?",
                List.of(
                    new QuickOption("없어요", "NONE"),
                    new QuickOption("해산물 알레르기", "SEAFOOD"),
                    new QuickOption("채식주의", "VEGETARIAN"),
                    new QuickOption("기타 (직접 입력)", "OTHER")
                ),
                70
            );
        }
        
        if (state.getDestination() != null && isBeachDestination(state.getDestination())) {
            return new NextDetailedQuestion(
                "BEACH_ACTIVITY",
                "바다에서 어떤 활동을 즐기고 싶으신가요?",
                List.of(
                    new QuickOption("수영/스노클링", "SWIMMING"),
                    new QuickOption("해변 산책", "WALKING"),
                    new QuickOption("수상 스포츠", "WATER_SPORTS"),
                    new QuickOption("선셋 감상", "SUNSET")
                ),
                75
            );
        }
        
        return null;
    }
}

// 요청/응답 레코드 (확장)
public record DetailedNeedsRequest(
    String sessionId,
    String userResponse,  // 이전 질문에 대한 사용자 응답
    Map<String, Object> additionalContext
) {}

public record DetailedNeedsResponse(
    String sessionId,
    String status,       // IN_PROGRESS, COMPLETE, SUFFICIENT
    String nextQuestion, // 다음 세부 질문
    int remainingQuestions,
    float progressPercentage,
    List<QuickOption> quickOptions,
    String questionCategory,
    int questionPriority
) {}

// 다음 세부 질문 정보
public class NextDetailedQuestion {
    private final String category;
    private final String question;
    private final List<QuickOption> quickOptions;
    private final int priority;
    
    // constructor, getters...
}
```

#### startAdaptiveFollowUp
```java
@Bean
public Function<StartAdaptiveFollowUpRequest, AdaptiveFollowUpResponse> startAdaptiveFollowUp() {
    return request -> {
        log.info("LLM starting adaptive follow-up session");
        
        // 세션 생성
        String sessionId = UUID.randomUUID().toString();
        TravelInfoCollectionState state = new TravelInfoCollectionState();
        state.setSessionId(sessionId);
        state.setUserId(request.userId());
        
        // LLM이 초기 메시지 분석하여 전략 결정
        FollowUpStrategy strategy = analyzeUserIntent(request);
        state.setStrategy(strategy);
        
        // 이미 언급된 정보 추출 및 저장
        Map<String, Object> extractedInfo = extractInitialInfo(request.initialMessage());
        state.updateCollectedInfo(extractedInfo);
        
        // LLM이 다음 질문 결정
        NextQuestionDecision decision = determineNextQuestion(state, request.context());
        
        return new AdaptiveFollowUpResponse(
            sessionId,
            decision.question(),
            decision.quickOptions(),
            state.getProgressPercentage(),
            decision.questionType(),
            strategy.name(),
            false
        );
    };
}

private FollowUpStrategy analyzeUserIntent(StartAdaptiveFollowUpRequest request) {
    // LLM이 사용자 의도와 상황 분석
    String analysisPrompt = String.format("""
        사용자 메시지: %s
        대화 컨텍스트: %s
        
        다음 요소를 분석하세요:
        1. 사용자의 급한 정도 (긴급/보통/여유)
        2. 여행 경험 수준 (초보/중급/전문가)
        3. 선호하는 대화 스타일 (빠른진행/상세설명/친근한대화)
        
        적절한 전략을 선택하세요:
        - QUICK_ESSENTIAL: 핵심 정보만 빠르게
        - DETAILED_FRIENDLY: 친근하게 상세히
        - EXPERT_EFFICIENT: 전문적이고 효율적으로
        - CASUAL_EXPLORATORY: 탐색적으로 여유있게
        """, 
        request.initialMessage(),
        request.context()
    );
    
    ChatResponse response = chatClient.call(analysisPrompt);
    return FollowUpStrategy.valueOf(response.getStrategy());
}
```

#### getNextAdaptiveQuestion
```java
@Bean
public Function<GetNextQuestionRequest, NextQuestionResponse> getNextAdaptiveQuestion() {
    return request -> {
        log.info("LLM determining next question for session: {}", request.sessionId());
        
        // 현재 상태 조회
        TravelInfoCollectionState state = sessionService.getSession(request.sessionId());
        
        // 프롬프트 템플릿 사용하여 다음 질문 결정
        String currentState = String.format("""
            목적지: %s, 날짜: %s, 인원: %s, 예산: %s, 스타일: %s
            """,
            state.getDestinations() != null ? state.getDestinations() : "미정",
            state.getDates() != null ? state.getDates() : "미정",
            state.getCompanions() != null ? state.getCompanions() : "미정",
            state.getBudget() != null ? state.getBudget() : "미정",
            state.getTravelStyle() != null ? state.getTravelStyle() : "미정"
        );
        
        String prompt = PromptTemplates.GENERATE_FOLLOWUP
            .replace("{currentState}", currentState);
        
        ChatResponse response = chatClient.call(prompt);
        
        return new NextQuestionResponse(
            response.getAction(),
            response.getQuestion(),
            response.getQuickOptions(),
            response.getReasoning()
        );
    };
}
```

#### continueAdaptiveFollowUp
```java
@Bean
public Function<ContinueAdaptiveFollowUpRequest, AdaptiveFollowUpResponse> continueAdaptiveFollowUp() {
    return request -> {
        log.info("LLM continuing adaptive follow-up for session: {}", request.sessionId());
        
        // 현재 상태 조회
        TravelInfoCollectionState state = sessionService.getSession(request.sessionId());
        
        // 사용자 응답 파싱 및 저장
        Map<String, Object> parsedInfo = parseUserResponse(
            request.userResponse(),
            state.getLastQuestionType()
        );
        state.updateCollectedInfo(parsedInfo);
        state.addToHistory(request.userResponse());
        
        // 응답 시간 및 패턴 기록 (사용자 피로도 판단용)
        state.recordResponseTime(request.responseTime());
        
        // LLM이 다음 행동 결정
        NextQuestionDecision decision = makeNextDecision(state);
        
        // 수집 완료 체크
        if (decision.isComplete() || hasEnoughInfo(state)) {
            return new AdaptiveFollowUpResponse(
                request.sessionId(),
                "완벽해요! 수집한 정보로 맞춤 여행 계획을 만들어드릴게요. 🎉",
                List.of(
                    new QuickOption("여행 계획 생성", "GENERATE_PLAN"),
                    new QuickOption("추가 정보 입력", "ADD_MORE")
                ),
                100,
                "COMPLETE",
                state.getStrategy().name(),
                true
            );
        }
        
        // 다음 질문 생성
        return new AdaptiveFollowUpResponse(
            request.sessionId(),
            decision.question(),
            decision.quickOptions(),
            calculateProgress(state),
            decision.questionType(),
            state.getStrategy().name(),
            false
        );
    };
}

private NextQuestionDecision makeNextDecision(TravelInfoCollectionState state) {
    // 전략에 따른 다른 프롬프트 사용
    String prompt = switch(state.getStrategy()) {
        case QUICK_ESSENTIAL -> buildQuickEssentialPrompt(state);
        case DETAILED_FRIENDLY -> buildDetailedFriendlyPrompt(state);
        case EXPERT_EFFICIENT -> buildExpertEfficientPrompt(state);
        case CASUAL_EXPLORATORY -> buildCasualExploratoryPrompt(state);
    };
    
    ChatResponse response = chatClient.call(prompt);
    return parseDecision(response);
}

private String buildQuickEssentialPrompt(TravelInfoCollectionState state) {
    return String.format("""
        전략: 빠른 핵심 정보 수집
        
        현재 상태: %s
        응답 시간 평균: %dms (빠를수록 급한 사용자)
        
        규칙:
        - 목적지, 날짜, 인원만 있으면 계획 생성 가능
        - 예산과 스타일은 선택사항
        - 사용자가 3번 이상 짧게 답하면 바로 완료
        
        다음 질문을 결정하세요.
        """,
        state.getCollectedInfo(),
        state.getAvgResponseTime()
    );
}

@Cacheable(value = "placeCategories", ttl = 3600)
private Map<String, List<String>> categorizeWishPlaces(List<String> places) {
    // 모든 장소를 LLM이 한 번에 분류 (배치 처리로 효율화)
    if (places.isEmpty()) {
        return new HashMap<>();
    }
    
    // 시스템 프롬프트에 이미 카테고리 정의되어 있음
    // 사용자 프롬프트만 추가 (50-100 토큰)
    String userPrompt = String.format("""
        장소 분류: %s
        
        JSON 응답: {"장소명":"카테고리명"}
        """,
        String.join(", ", places)
    );
    
    // chatModel은 이미 시스템 프롬프트 포함
    ChatResponse response = chatModel.call(userPrompt);
    Map<String, String> categorizedPlaces = response.getCategorizedMap();
    
    // 카테고리별로 그룹화
    Map<String, List<String>> result = new HashMap<>();
    categorizedPlaces.forEach((place, category) -> 
        result.computeIfAbsent(category, k -> new ArrayList<>()).add(place)
    );
    
    return result;
}

@Cacheable(value = "placeRecommendations", ttl = 7200)
private List<String> recommendSimilarPlaces(String place, String destination) {
    // 시스템 프롬프트가 기본 규칙 처리
    // 사용자 프롬프트만 추가 (50 토큰)
    String userPrompt = String.format("""
        %s에서 %s 방문 시 유사/인근 장소 3개 추천
        JSON: ["장소1", "장소2", "장소3"]
        """,
        destination, place
    );
    
    ChatResponse response = chatModel.call(userPrompt);
    return response.getRecommendations();
}
```

#### Request/Response DTOs
```java
// 적응형 꼬리질문 요청/응답 DTO
public record StartAdaptiveFollowUpRequest(
    String userId,
    String initialMessage,
    Map<String, Object> context,
    String userProfile  // 사용자 프로필 정보
) {}

public record AdaptiveFollowUpResponse(
    String sessionId,
    String question,
    List<QuickOption> quickOptions,
    int progressPercentage,
    String questionType,  // DESTINATION, DATES, BUDGET 등
    String strategy,      // QUICK_ESSENTIAL, DETAILED_FRIENDLY 등
    boolean isComplete
) {}

public record GetNextQuestionRequest(
    String sessionId,
    Map<String, Object> currentContext
) {}

public record NextQuestionResponse(
    String action,
    String question,
    List<QuickOption> quickOptions,
    String reasoning  // LLM이 이 질문을 선택한 이유
) {}

public record ContinueAdaptiveFollowUpRequest(
    String sessionId,
    String userResponse,
    long responseTime,  // 응답 시간 (밀리초)
    Map<String, Object> additionalContext
) {}

// 전략 enum (확장)
public enum FollowUpStrategy {
    QUICK_ESSENTIAL,      // 급한 사용자 - 핵심만
    DETAILED_FRIENDLY,    // 여유있는 사용자 - 친근하고 상세하게
    EXPERT_EFFICIENT,     // 경험 많은 사용자 - 전문적이고 효율적으로
    CASUAL_EXPLORATORY,   // 탐색중인 사용자 - 여러 옵션 제시
    ADAPTIVE_INTELLIGENT, // 지능형 적응 - 상황에 따라 동적 변경
    FAMILY_FOCUSED,       // 가족 중심 - 가족 구성원 배려
    BUDGET_CONSCIOUS,     // 예산 중심 - 비용 효율적
    EXPERIENCE_SEEKER    // 경험 중심 - 특별한 경험 추구
}

// 적응형 질문 전략 관리자
public class AdaptiveStrategyManager {
    private FollowUpStrategy currentStrategy;
    private final Map<String, Integer> strategyScores = new HashMap<>();
    private final List<UserInteraction> interactions = new ArrayList<>();
    
    public void analyzeAndAdapt(UserInteraction interaction) {
        // 사용자 상호작용 패턴 분석
        interactions.add(interaction);
        
        // 응답 시간 기반 분석
        if (interaction.responseTime < 1000) {  // 1초 미만 = 매우 빠름
            strategyScores.merge("QUICK_ESSENTIAL", 10, Integer::sum);
        } else if (interaction.responseTime > 30000) {  // 30초 이상 = 고민 중
            strategyScores.merge("DETAILED_FRIENDLY", 10, Integer::sum);
        }
        
        // 응답 길이 기반 분석
        if (interaction.responseLength < 10) {  // 짧은 답변
            strategyScores.merge("QUICK_ESSENTIAL", 5, Integer::sum);
        } else if (interaction.responseLength > 50) {  // 긴 답변
            strategyScores.merge("DETAILED_FRIENDLY", 5, Integer::sum);
            strategyScores.merge("EXPERT_EFFICIENT", 3, Integer::sum);
        }
        
        // 키워드 기반 분석
        String response = interaction.userResponse.toLowerCase();
        if (response.contains("모르겠") || response.contains("추천")) {
            strategyScores.merge("CASUAL_EXPLORATORY", 15, Integer::sum);
        }
        if (response.contains("빨리") || response.contains("급해")) {
            strategyScores.merge("QUICK_ESSENTIAL", 20, Integer::sum);
        }
        if (response.contains("자세히") || response.contains("설명")) {
            strategyScores.merge("DETAILED_FRIENDLY", 15, Integer::sum);
        }
        if (response.contains("아이") || response.contains("가족")) {
            strategyScores.merge("FAMILY_FOCUSED", 20, Integer::sum);
        }
        if (response.contains("저렴") || response.contains("싸게")) {
            strategyScores.merge("BUDGET_CONSCIOUS", 20, Integer::sum);
        }
        
        // 전략 재선택
        currentStrategy = selectOptimalStrategy();
    }
    
    private FollowUpStrategy selectOptimalStrategy() {
        return strategyScores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(entry -> FollowUpStrategy.valueOf(entry.getKey()))
            .orElse(FollowUpStrategy.ADAPTIVE_INTELLIGENT);
    }
    
    public String getStrategyPrompt() {
        return switch(currentStrategy) {
            case QUICK_ESSENTIAL -> """
                전략: 빠른 핵심 정보 수집
                - 필수 정보만 질문
                - 선택지 제공으로 빠른 응답 유도
                - 3개 이내 질문으로 완료
                """;
                
            case DETAILED_FRIENDLY -> """
                전략: 친근하고 상세한 정보 수집
                - 이모지와 친근한 어투 사용
                - 충분한 설명과 예시 제공
                - 사용자 페이스에 맞춤
                """;
                
            case EXPERT_EFFICIENT -> """
                전략: 전문적이고 효율적인 정보 수집
                - 전문 용어 사용 가능
                - 논리적 구조의 질문
                - 시간 효율성 중시
                """;
                
            case CASUAL_EXPLORATORY -> """
                전략: 탐색적 정보 수집
                - 다양한 옵션과 추천 제공
                - 영감을 주는 예시 포함
                - 결정 도움 제공
                """;
                
            case FAMILY_FOCUSED -> """
                전략: 가족 중심 정보 수집
                - 가족 구성원별 니즈 파악
                - 안전과 편의 중시
                - 연령별 활동 고려
                """;
                
            case BUDGET_CONSCIOUS -> """
                전략: 예산 효율적 정보 수집
                - 비용 절감 팁 제공
                - 무료/저렴한 옵션 우선
                - 가성비 중심 추천
                """;
                
            case EXPERIENCE_SEEKER -> """
                전략: 특별한 경험 중심
                - 유니크한 활동 추천
                - 현지 특별 경험 강조
                - 인스타그래머블한 장소
                """;
                
            default -> """
                전략: 지능형 적응
                - 사용자 반응에 따라 동적 조정
                - 최적 전략 자동 선택
                - 맞춤형 질문 생성
                """;
        };
    }
}

// 사용자 상호작용 기록
public record UserInteraction(
    String userResponse,
    long responseTime,
    int responseLength,
    String questionType,
    LocalDateTime timestamp
) {}

// 사용자 피로도 관리 시스템
public class UserFatigueManager {
    private final List<UserInteraction> interactions = new ArrayList<>();
    private int questionCount = 0;
    private double fatigueScore = 0.0;
    
    public void recordInteraction(UserInteraction interaction) {
        interactions.add(interaction);
        questionCount++;
        calculateFatigueScore();
    }
    
    private void calculateFatigueScore() {
        // 피로도 계산 요소들
        double baseScore = 0.0;
        
        // 1. 질문 횟수 (5개 이상시 피로도 증가)
        if (questionCount > 5) {
            baseScore += (questionCount - 5) * 10;
        }
        
        // 2. 응답 시간 패턴 (점점 빨라지면 피로도 증가)
        if (interactions.size() >= 3) {
            List<Long> recentTimes = interactions.subList(
                Math.max(0, interactions.size() - 3), 
                interactions.size()
            ).stream()
            .map(UserInteraction::responseTime)
            .toList();
            
            // 응답 시간이 점점 짧아지는 패턴
            if (recentTimes.get(2) < recentTimes.get(1) && 
                recentTimes.get(1) < recentTimes.get(0)) {
                baseScore += 20;  // 급하게 응답하는 패턴
            }
        }
        
        // 3. 짧은 답변 연속 (3번 이상)
        long shortAnswers = interactions.stream()
            .filter(i -> i.responseLength() < 10)
            .count();
        if (shortAnswers >= 3) {
            baseScore += shortAnswers * 5;
        }
        
        // 4. 모호한 답변 패턴
        long vagueAnswers = interactions.stream()
            .filter(i -> isVagueResponse(i.userResponse()))
            .count();
        baseScore += vagueAnswers * 15;
        
        // 5. 시간대 고려 (늦은 시간일수록 피로도 증가)
        LocalTime now = LocalTime.now();
        if (now.isAfter(LocalTime.of(22, 0))) {
            baseScore += 15;  // 밤 10시 이후
        } else if (now.isBefore(LocalTime.of(9, 0))) {
            baseScore += 10;  // 아침 9시 이전
        }
        
        fatigueScore = Math.min(100, baseScore);  // 최대 100
    }
    
    private boolean isVagueResponse(String response) {
        String lower = response.toLowerCase();
        return lower.contains("모르겠") || 
               lower.contains("아무거나") ||
               lower.contains("상관없") ||
               lower.contains("그냥") ||
               lower.equals("ㅇㅇ") ||
               lower.equals("ㅇㅋ") ||
               lower.equals("네") ||
               lower.equals("응");
    }
    
    public FatigueAction recommendAction() {
        if (fatigueScore < 30) {
            return FatigueAction.CONTINUE_NORMAL;
        } else if (fatigueScore < 60) {
            return FatigueAction.SIMPLIFY_QUESTIONS;
        } else if (fatigueScore < 80) {
            return FatigueAction.OFFER_QUICK_OPTIONS;
        } else {
            return FatigueAction.WRAP_UP_QUICKLY;
        }
    }
    
    public String getFatigueAdjustedPrompt(String originalPrompt) {
        return switch(recommendAction()) {
            case CONTINUE_NORMAL -> originalPrompt;
            
            case SIMPLIFY_QUESTIONS -> """
                사용자가 피로함을 느끼고 있습니다.
                - 질문을 간단명료하게
                - 선택지 제공으로 빠른 결정 유도
                - 추가 설명 최소화
                """ + originalPrompt;
                
            case OFFER_QUICK_OPTIONS -> """
                사용자가 상당히 피곤해합니다.
                - 즉시 선택 가능한 옵션 제시
                - 템플릿이나 추천 활용
                - "이걸로 진행할까요?" 형태로 마무리
                """ + originalPrompt;
                
            case WRAP_UP_QUICKLY -> """
                사용자가 매우 피곤합니다. 빠르게 마무리하세요.
                - 현재까지 정보로 계획 생성 제안
                - "충분한 정보를 모았습니다" 메시지
                - 나머지는 기본값으로 처리
                """ + originalPrompt;
        };
    }
    
    public boolean shouldStopAsking() {
        return fatigueScore >= 80 || questionCount >= 7;
    }
    
    public String getFatigueMessage() {
        if (fatigueScore >= 60) {
            return "거의 다 왔어요! 조금만 더 알려주시면 완벽한 계획을 만들 수 있어요 😊";
        } else if (fatigueScore >= 80) {
            return "충분한 정보를 모았어요! 이제 멋진 여행 계획을 만들어드릴게요 🎉";
        }
        return null;
    }
}

// 피로도 관리 액션
public enum FatigueAction {
    CONTINUE_NORMAL,      // 정상 진행
    SIMPLIFY_QUESTIONS,   // 질문 간소화
    OFFER_QUICK_OPTIONS,  // 빠른 옵션 제시
    WRAP_UP_QUICKLY      // 빠른 마무리
}

// 질문 결정 정보
public record NextQuestionDecision(
    String questionType,
    String question,
    List<QuickOption> quickOptions,
    boolean isComplete,
    String reasoning
) {}
```

### 2. 템플릿 추천 도구

#### recommendTemplates
```java
@Bean
public Function<TemplateRecommendRequest, TemplateRecommendResponse> recommendTemplates() {
    return request -> {
        // 사용자 메시지 분석하여 관련 템플릿 추천
        List<String> keywords = extractKeywords(request.userMessage());
        
        // 점수 계산 및 상위 9개 선정
        List<TemplateScore> scores = templateRepository.findAll().stream()
            .map(template -> calculateScore(template, keywords, request.context()))
            .sorted((a, b) -> Double.compare(b.score(), a.score()))
            .limit(9)
            .collect(Collectors.toList());
        
        return new TemplateRecommendResponse(
            scores.stream()
                .map(s -> new TemplateInfo(
                    s.template().getId(),
                    s.template().getName(),
                    s.template().getDescription(),
                    s.template().getIcon(),
                    s.score()
                ))
                .collect(Collectors.toList())
        );
    };
}

public record TemplateRecommendRequest(
    String userMessage,
    Map<String, Object> context
) {}

public record TemplateRecommendResponse(
    List<TemplateInfo> templates
) {}
```

### 3. 지역 검색 도구

#### searchRegions
```java
@Bean
public Function<RegionSearchRequest, RegionSearchResponse> searchRegions() {
    return request -> {
        // DB에서 조건에 맞는 지역 검색
        List<Region> regions = regionRepository.findByThemeAndRequirements(
            request.theme(),
            request.requirements()
        );
        
        // 추천 점수 계산
        List<RegionRecommendation> recommendations = regions.stream()
            .map(region -> calculateRecommendation(region, request))
            .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
            .limit(5)
            .collect(Collectors.toList());
        
        return new RegionSearchResponse(recommendations);
    };
}
```

#### searchAttractions
```java
@Bean
public Function<AttractionSearchRequest, AttractionSearchResponse> searchAttractions() {
    return request -> {
        // 특정 지역의 관광지 검색
        List<TourPlace> attractions = tourPlaceRepository.findByRegionAndCategory(
            request.regionName(),
            request.category()
        );
        
        return new AttractionSearchResponse(
            attractions.stream()
                .map(this::toAttractionInfo)
                .collect(Collectors.toList())
        );
    };
}
```

### 4. 여행 계획 생성 도구

#### generateTravelPlan (시간 정보 활용 강화)
```java
@Bean
public Function<GeneratePlanRequest, TravelPlanResponse> generateTravelPlan() {
    return request -> {
        log.info("여행 계획 생성 시작: {}", request.sessionId());
        
        // 수집된 정보 조회
        TravelInfoCollectionState state = sessionService.getSession(request.sessionId());
        
        // 시간 정보 기반 실제 관광 가능 시간 계산
        TravelTimeInfo timeInfo = calculateActualTravelTime(state);
        
        // 여행 계획 생성 전략 결정 (시간 정보 반영)
        PlanningStrategy strategy = determinePlanningStrategy(state, timeInfo);
        
        // 일정 생성 (시간 제약 반영)
        DailyItinerary itinerary = createTimeAwareItinerary(state, strategy, timeInfo);
        
        return new TravelPlanResponse(
            UUID.randomUUID().toString(),
            itinerary,
            itinerary.getRecommendations(),
            itinerary.getEstimatedBudget(),
            timeInfo
        );
    };
}

// 실제 관광 가능 시간 계산
private TravelTimeInfo calculateActualTravelTime(TravelInfoCollectionState state) {
    TimeInfo times = state.getTimeInfo();
    String destination = state.getDestination();
    String departure = state.getDepartureLocation();
    
    TravelTimeInfo result = new TravelTimeInfo();
    
    // 첫날 계산
    if (times != null && times.departureTime() != null) {
        // 집에서 목적지까지 이동 시간 계산
        Duration travelTime = calculateTravelTime(departure, destination);
        LocalTime arrivalTime = times.departureTime().plus(travelTime);
        
        result.setFirstDayStartTime(arrivalTime);
        result.setFirstDayAvailableHours(
            Duration.between(arrivalTime, LocalTime.of(22, 0)).toHours()
        );
        
        log.info("첫날: {}에 출발 → {} 도착, 관광 가능 시간: {}시간",
            times.departureTime(), arrivalTime, result.getFirstDayAvailableHours());
    }
    
    // 마지막날 계산
    if (times != null && times.returnTime() != null) {
        // 목적지에서 집까지 이동 시간 계산
        Duration returnTravelTime = calculateTravelTime(destination, departure);
        LocalTime mustLeaveTime = times.returnTime().minus(returnTravelTime);
        
        result.setLastDayEndTime(mustLeaveTime);
        result.setLastDayAvailableHours(
            Duration.between(LocalTime.of(9, 0), mustLeaveTime).toHours()
        );
        
        log.info("마지막날: {} 출발 필요 → {} 집 도착, 관광 가능 시간: {}시간",
            mustLeaveTime, times.returnTime(), result.getLastDayAvailableHours());
    }
    
    // 중간날들은 전일 관광 가능 (보통 9시-22시, 13시간)
    result.setMiddleDayAvailableHours(13);
    
    return result;
}

// 이동 시간 계산 (도시간 거리 기반)
private Duration calculateTravelTime(String from, String to) {
    Map<String, Duration> travelTimes = Map.of(
        "서울-부산", Duration.ofHours(3),      // KTX
        "서울-제주", Duration.ofHours(3),      // 비행기 + 공항 이동
        "서울-강릉", Duration.ofHours(2).plusMinutes(30),
        "서울-전주", Duration.ofHours(2),
        "서울-경주", Duration.ofHours(3),
        "부산-제주", Duration.ofHours(2).plusMinutes(30),
        "서울-대전", Duration.ofHours(1).plusMinutes(30)
    );
    
    String key = from + "-" + to;
    String reverseKey = to + "-" + from;
    
    return travelTimes.getOrDefault(key, 
           travelTimes.getOrDefault(reverseKey, Duration.ofHours(2)));
}

// 시간 제약을 반영한 일정 생성
private DailyItinerary createTimeAwareItinerary(
    TravelInfoCollectionState state, 
    PlanningStrategy strategy,
    TravelTimeInfo timeInfo) {
    
    DailyItinerary itinerary = new DailyItinerary();
    
    // 첫날 일정 (늦게 시작)
    if (timeInfo.getFirstDayStartTime() != null) {
        Day firstDay = new Day();
        firstDay.setStartTime(timeInfo.getFirstDayStartTime());
        
        // 도착 시간에 따른 장소 수 조정
        int placesCount = calculatePlacesForHours(timeInfo.getFirstDayAvailableHours());
        
        // 첫날은 가벼운 일정 (주로 저녁 활동)
        if (timeInfo.getFirstDayStartTime().isAfter(LocalTime.of(15, 0))) {
            // 오후 도착: 체크인 → 저녁 → 야경
            firstDay.addActivity("숙소 체크인", 30);
            firstDay.addActivity("저녁 식사", 90);
            firstDay.addActivity("야경 명소 또는 야시장", 120);
        } else if (timeInfo.getFirstDayStartTime().isAfter(LocalTime.of(12, 0))) {
            // 점심 시간 도착: 점심 → 오후 관광 → 저녁
            firstDay.addActivity("점심 식사", 60);
            firstDay.addActivity("주요 관광지 2-3곳", 180);
            firstDay.addActivity("저녁 식사", 90);
            firstDay.addActivity("야간 활동", 90);
        } else {
            // 오전 도착: 정상 일정 가능
            firstDay = createNormalDaySchedule(state, strategy);
        }
        
        itinerary.addDay(firstDay);
    }
    
    // 중간날 일정 (전일 관광)
    int middleDays = state.getDuration() - 2; // 첫날과 마지막날 제외
    for (int i = 0; i < middleDays; i++) {
        Day middleDay = createNormalDaySchedule(state, strategy);
        itinerary.addDay(middleDay);
    }
    
    // 마지막날 일정 (일찍 끝남)
    if (timeInfo.getLastDayEndTime() != null) {
        Day lastDay = new Day();
        lastDay.setEndTime(timeInfo.getLastDayEndTime());
        
        // 출발 시간에 따른 장소 수 조정
        int placesCount = calculatePlacesForHours(timeInfo.getLastDayAvailableHours());
        
        // 마지막날은 가벼운 일정 (주로 오전 활동)
        if (timeInfo.getLastDayEndTime().isBefore(LocalTime.of(14, 0))) {
            // 이른 출발: 아침 → 기념품 쇼핑 → 출발
            lastDay.addActivity("아침 식사", 60);
            lastDay.addActivity("기념품 쇼핑 또는 시장 방문", 90);
            lastDay.addActivity("공항/역으로 이동", 60);
        } else if (timeInfo.getLastDayEndTime().isBefore(LocalTime.of(17, 0))) {
            // 오후 출발: 오전 관광 → 점심 → 출발
            lastDay.addActivity("오전 관광지 2곳", 150);
            lastDay.addActivity("점심 식사", 60);
            lastDay.addActivity("가벼운 쇼핑", 60);
            lastDay.addActivity("출발", 30);
        } else {
            // 저녁 출발: 거의 전일 관광 가능
            lastDay = createNormalDaySchedule(state, strategy);
            lastDay.addActivity("공항/역으로 이동", 60);
        }
        
        itinerary.addDay(lastDay);
    }
    
    return itinerary;
}

// 시간에 따른 적정 장소 수 계산
private int calculatePlacesForHours(long availableHours) {
    // 1 장소당 평균 1.5-2시간 소요 가정
    if (availableHours < 3) return 1;      // 3시간 미만: 1곳
    if (availableHours < 5) return 2;      // 5시간 미만: 2곳
    if (availableHours < 8) return 3-4;    // 8시간 미만: 3-4곳
    if (availableHours < 11) return 5-6;   // 11시간 미만: 5-6곳
    return 7-8;                            // 전일: 7-8곳
}

// 계획 전략 결정
private PlanningStrategy determinePlanningStrategy(TravelInfoCollectionState state) {
    // 사용자가 장소를 지정한 경우
    if (state.hasSpecificPlaces()) {
        return PlanningStrategy.USER_PLACES_CENTERED;  // 사용자 지정 장소 중심
    }
    
    // 여행 스타일에 따른 전략
    String style = state.getTravelStyle();
    if ("타이트".equals(style)) {
        return PlanningStrategy.PACKED_SCHEDULE;  // 9-10개 장소/일
    } else if ("여유로운".equals(style)) {
        return PlanningStrategy.RELAXED_SCHEDULE;  // 5-6개 장소/일
    }
    
    return PlanningStrategy.BALANCED_SCHEDULE;  // 기본 7개 장소/일
}

// 일정 생성 로직
private DailyItinerary createItinerary(TravelInfoCollectionState state, PlanningStrategy strategy) {
    DailyItinerary itinerary = new DailyItinerary();
    
    switch (strategy) {
        case USER_PLACES_CENTERED:
            // 사용자 지정 장소를 포함하여 시간 여유 있게 구성
            return createUserCenteredItinerary(state);
            
        case PACKED_SCHEDULE:
            // 타이트한 일정 (아침 8시 ~ 밤 10시, 9-10개 장소)
            return createPackedItinerary(state);
            
        case RELAXED_SCHEDULE:
            // 여유로운 일정 (오전 10시 ~ 저녁 8시, 5-6개 장소)
            return createRelaxedItinerary(state);
            
        case BALANCED_SCHEDULE:
        default:
            // 균형잡힌 일정 (오전 9시 ~ 저녁 9시, 7개 장소)
            return createBalancedItinerary(state);
    }
}

// 사용자 지정 장소 중심 일정
private DailyItinerary createUserCenteredItinerary(TravelInfoCollectionState state) {
    /*
     * 사용자가 지정한 장소들을 우선 배치하고
     * 남은 시간에 추가 장소 추천
     * 충분한 이동 시간과 여유 시간 확보
     */
    List<Place> userPlaces = state.getSpecifiedPlaces();
    List<TimeSlot> schedule = new ArrayList<>();
    
    // 1. 사용자 지정 장소 우선 배치
    for (Place place : userPlaces) {
        schedule.add(allocateTimeForPlace(place, true));  // 여유있게 시간 할당
    }
    
    // 2. 남은 시간에 추가 장소 추천
    int remainingSlots = 7 - userPlaces.size();
    if (remainingSlots > 0) {
        List<Place> recommendations = recommendNearbyPlaces(userPlaces, remainingSlots);
        for (Place rec : recommendations) {
            schedule.add(allocateTimeForPlace(rec, false));
        }
    }
    
    return new DailyItinerary(schedule);
}

// 균형잡힌 일정 (기본)
private DailyItinerary createBalancedItinerary(TravelInfoCollectionState state) {
    /*
     * 오전 9시 시작, 저녁 9시 종료
     * 총 7개 장소 (관광 4곳, 식사 2곳, 카페 1곳)
     * 
     * 예시 일정:
     * 09:00-10:30 관광지1
     * 10:30-12:00 관광지2
     * 12:00-13:30 점심
     * 13:30-15:00 관광지3
     * 15:00-16:00 카페
     * 16:00-17:30 관광지4
     * 17:30-19:00 저녁
     * 19:00-21:00 야경/쇼핑
     */
    
    List<TimeSlot> schedule = new ArrayList<>();
    String destination = state.getDestinations().get(0);
    
    // 장소 카테고리별 추천
    List<Place> attractions = searchAttractions(destination, "관광명소", 4);
    List<Place> restaurants = searchRestaurants(destination, 2);
    Place cafe = searchCafe(destination);
    
    // 시간대별 배치
    schedule.add(new TimeSlot("09:00", "10:30", attractions.get(0)));
    schedule.add(new TimeSlot("10:30", "12:00", attractions.get(1)));
    schedule.add(new TimeSlot("12:00", "13:30", restaurants.get(0)));  // 점심
    schedule.add(new TimeSlot("13:30", "15:00", attractions.get(2)));
    schedule.add(new TimeSlot("15:00", "16:00", cafe));  // 휴식
    schedule.add(new TimeSlot("16:00", "17:30", attractions.get(3)));
    schedule.add(new TimeSlot("17:30", "19:00", restaurants.get(1)));  // 저녁
    
    return new DailyItinerary(schedule);
}

// 타이트한 일정
private DailyItinerary createPackedItinerary(TravelInfoCollectionState state) {
    /*
     * 오전 8시 시작, 밤 10시 종료
     * 총 9-10개 장소
     */
    // 구현...
}

// 여유로운 일정
private DailyItinerary createRelaxedItinerary(TravelInfoCollectionState state) {
    /*
     * 오전 10시 시작, 저녁 8시 종료
     * 총 5-6개 장소, 충분한 휴식 시간
     */
    // 구현...
}
```

### 5. 실시간 정보 검색 도구

#### searchWithPerplexity
```java
@Bean
public Function<PerplexitySearchRequest, PerplexitySearchResponse> searchWithPerplexity() {
    return request -> {
        // Perplexity API로 실시간 정보 검색
        String query = buildPerplexityQuery(request);
        
        PerplexityApiResponse apiResponse = perplexityClient.search(query);
        
        return new PerplexitySearchResponse(
            apiResponse.getAnswer(),
            apiResponse.getSources(),
            apiResponse.getRelatedQuestions()
        );
    };
}
```

### 6. 컨텍스트 관리 도구

#### getConversationContext
```java
@Bean
public Function<ContextRequest, ConversationContext> getConversationContext() {
    return request -> {
        // 현재 대화 컨텍스트 조회
        return contextManager.getContext(request.threadId());
    };
}
```

#### updateConversationContext
```java
@Bean
public Function<UpdateContextRequest, ConversationContext> updateConversationContext() {
    return request -> {
        // 대화 컨텍스트 업데이트
        ConversationContext context = contextManager.getContext(request.threadId());
        context.update(request.updates());
        contextManager.save(context);
        return context;
    };
}
```

### 7. 데이터베이스 조작 도구 (DB Management Tools)

#### saveUserPreferences
```java
@Bean
@Transactional
public Function<SaveUserPreferencesRequest, DatabaseOperationResponse> saveUserPreferences() {
    return request -> {
        log.info("LLM saving user preferences for userId: {}", request.userId());
        
        // 사용자 선호도 저장
        UserPreference preference = userPreferenceRepository.findByUserId(request.userId())
            .orElse(new UserPreference());
        
        preference.setUserId(request.userId());
        preference.setPreferredDestinations(request.destinations());
        preference.setTravelStyle(request.travelStyle());
        preference.setBudgetRange(request.budgetRange());
        preference.setUpdatedAt(LocalDateTime.now());
        
        UserPreference saved = userPreferenceRepository.save(preference);
        
        return new DatabaseOperationResponse(
            "SUCCESS",
            "User preferences saved",
            Map.of("id", saved.getId())
        );
    };
}
```

#### saveTravelHistory
```java
@Bean
@Transactional
public Function<SaveTravelHistoryRequest, DatabaseOperationResponse> saveTravelHistory() {
    return request -> {
        log.info("LLM saving travel history for userId: {}", request.userId());
        
        TravelHistory history = new TravelHistory();
        history.setUserId(request.userId());
        history.setDestination(request.destination());
        history.setStartDate(request.startDate());
        history.setEndDate(request.endDate());
        history.setCompanions(request.companions());
        history.setBudget(request.budget());
        history.setRating(request.rating());
        history.setNotes(request.notes());
        history.setCreatedAt(LocalDateTime.now());
        
        TravelHistory saved = travelHistoryRepository.save(history);
        
        // 캐시 업데이트
        cacheManager.evict("userHistory", request.userId());
        
        return new DatabaseOperationResponse(
            "SUCCESS",
            "Travel history saved",
            Map.of("historyId", saved.getId())
        );
    };
}
```

#### queryUserTravelData
```java
@Bean
@Transactional(readOnly = true)
public Function<QueryUserDataRequest, UserTravelDataResponse> queryUserTravelData() {
    return request -> {
        log.info("LLM querying travel data for userId: {}", request.userId());
        
        // 사용자의 모든 여행 관련 데이터 조회
        User user = userRepository.findById(request.userId())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        List<TravelHistory> histories = travelHistoryRepository
            .findByUserIdOrderByStartDateDesc(request.userId());
        
        Optional<UserPreference> preferences = userPreferenceRepository
            .findByUserId(request.userId());
        
        List<TravelInfoCollectionState> sessions = collectionRepository
            .findByUserOrderByCreatedAtDesc(user);
        
        return new UserTravelDataResponse(
            user.getUsername(),
            histories.stream()
                .map(this::toHistoryDto)
                .collect(Collectors.toList()),
            preferences.map(this::toPreferenceDto).orElse(null),
            sessions.stream()
                .filter(s -> s.isCompleted())
                .map(this::toSessionDto)
                .collect(Collectors.toList())
        );
    };
}
```

#### updateTravelPlanStatus
```java
@Bean
@Transactional
public Function<UpdatePlanStatusRequest, DatabaseOperationResponse> updateTravelPlanStatus() {
    return request -> {
        log.info("LLM updating travel plan status: {} -> {}", 
            request.planId(), request.newStatus());
        
        TravelPlan plan = travelPlanRepository.findById(request.planId())
            .orElseThrow(() -> new IllegalArgumentException("Plan not found"));
        
        // 상태 업데이트
        plan.setStatus(request.newStatus());
        plan.setUpdatedAt(LocalDateTime.now());
        
        if ("COMPLETED".equals(request.newStatus())) {
            plan.setCompletedAt(LocalDateTime.now());
        }
        
        TravelPlan updated = travelPlanRepository.save(plan);
        
        // 이벤트 발행
        eventPublisher.publishEvent(new TravelPlanStatusChangedEvent(
            updated.getId(),
            request.newStatus(),
            request.reason()
        ));
        
        return new DatabaseOperationResponse(
            "SUCCESS",
            "Travel plan status updated",
            Map.of(
                "planId", updated.getId(),
                "newStatus", updated.getStatus()
            )
        );
    };
}
```

#### searchSimilarTravels
```java
@Bean
@Transactional(readOnly = true)
public Function<SimilarTravelSearchRequest, SimilarTravelResponse> searchSimilarTravels() {
    return request -> {
        log.info("LLM searching similar travels for destination: {}", request.destination());
        
        // 유사한 여행 계획 검색 (PostgreSQL의 JSONB 쿼리 활용)
        List<TravelPlan> similarPlans = travelPlanRepository.findSimilarPlans(
            request.destination(),
            request.dateRange(),
            request.budgetRange(),
            request.companions()
        );
        
        // 인기도 계산
        Map<String, Integer> popularityScores = calculatePopularity(similarPlans);
        
        return new SimilarTravelResponse(
            similarPlans.stream()
                .map(plan -> toSimilarPlanDto(plan, popularityScores))
                .sorted((a, b) -> b.popularityScore().compareTo(a.popularityScore()))
                .limit(10)
                .collect(Collectors.toList())
        );
    };
}
```

#### createOrUpdateItinerary
```java
@Bean
@Transactional
public Function<ItineraryRequest, DatabaseOperationResponse> createOrUpdateItinerary() {
    return request -> {
        log.info("LLM creating/updating itinerary for planId: {}", request.planId());
        
        TravelPlan plan = travelPlanRepository.findById(request.planId())
            .orElseThrow(() -> new IllegalArgumentException("Plan not found"));
        
        // 일정 데이터 업데이트 (JSONB 형식)
        Map<String, Object> itinerary = new HashMap<>();
        itinerary.put("days", request.days());
        itinerary.put("activities", request.activities());
        itinerary.put("accommodations", request.accommodations());
        itinerary.put("transportation", request.transportation());
        itinerary.put("estimatedCost", request.estimatedCost());
        
        plan.setItinerary(itinerary);
        plan.setUpdatedAt(LocalDateTime.now());
        
        TravelPlan saved = travelPlanRepository.save(plan);
        
        // 관련 장소 정보도 저장
        for (var activity : request.activities()) {
            TourPlace place = new TourPlace();
            place.setName(activity.placeName());
            place.setRegion(request.destination());
            place.setCategory(activity.category());
            place.setDescription(activity.description());
            tourPlaceRepository.save(place);
        }
        
        return new DatabaseOperationResponse(
            "SUCCESS",
            "Itinerary created/updated",
            Map.of("planId", saved.getId())
        );
    };
}
```

### 8. 분석 및 통계 도구

#### analyzeUserTravelPatterns
```java
@Bean
@Transactional(readOnly = true)
public Function<UserAnalysisRequest, TravelPatternAnalysis> analyzeUserTravelPatterns() {
    return request -> {
        log.info("LLM analyzing travel patterns for userId: {}", request.userId());
        
        // 사용자의 여행 패턴 분석
        List<TravelHistory> histories = travelHistoryRepository
            .findByUserIdOrderByStartDateDesc(request.userId());
        
        // 선호 목적지 분석
        Map<String, Long> destinationFrequency = histories.stream()
            .collect(Collectors.groupingBy(
                TravelHistory::getDestination,
                Collectors.counting()
            ));
        
        // 계절별 여행 패턴
        Map<String, Long> seasonalPattern = histories.stream()
            .collect(Collectors.groupingBy(
                h -> getSeason(h.getStartDate()),
                Collectors.counting()
            ));
        
        // 평균 여행 기간
        double avgDuration = histories.stream()
            .mapToLong(h -> ChronoUnit.DAYS.between(h.getStartDate(), h.getEndDate()))
            .average()
            .orElse(0);
        
        // 예산 패턴
        IntSummaryStatistics budgetStats = histories.stream()
            .mapToInt(h -> h.getBudget())
            .summaryStatistics();
        
        return new TravelPatternAnalysis(
            destinationFrequency,
            seasonalPattern,
            avgDuration,
            budgetStats,
            generateRecommendations(histories)
        );
    };
}
```

### 9. OCR 및 예약 정보 처리 도구

#### processReservationOCR (OCR로 예약 정보 추출)
```java
@Bean
@Description("항공권, 호텔 예약 확인서 이미지에서 정보 자동 추출")
public Function<OCRRequest, ReservationInfo> processReservationOCR() {
    return request -> {
        log.info("OCR 처리 시작: {}", request.imageType());
        
        // 이미지 OCR 처리
        String extractedText = ocrService.extractText(request.imageData());
        
        // LLM을 사용한 정보 추출
        ReservationInfo info = switch(request.imageType()) {
            case "FLIGHT" -> extractFlightInfo(extractedText);
            case "HOTEL" -> extractHotelInfo(extractedText);
            case "TRAIN" -> extractTrainInfo(extractedText);
            default -> extractGeneralInfo(extractedText);
        };
        
        // 추출된 정보 검증
        validateReservationInfo(info);
        
        return info;
    };
}

// 항공권 정보 추출
private FlightInfo extractFlightInfo(String text) {
    String prompt = """
        다음 텍스트에서 항공권 정보를 추출하세요:
        %s
        
        JSON 형식으로 반환:
        {
            "airline": "항공사명",
            "flightNumber": "편명",
            "departure": {
                "airport": "출발공항",
                "time": "출발시간",
                "terminal": "터미널"
            },
            "arrival": {
                "airport": "도착공항",
                "time": "도착시간"
            },
            "seatNumber": "좌석번호",
            "confirmationNumber": "예약번호"
        }
        """.formatted(text);
    
    return llmService.extractStructuredData(prompt, FlightInfo.class);
}

// 호텔 정보 추출
private HotelInfo extractHotelInfo(String text) {
    String prompt = """
        다음 텍스트에서 호텔 예약 정보를 추출하세요:
        %s
        
        JSON 형식으로 반환:
        {
            "hotelName": "호텔명",
            "address": "주소",
            "checkIn": "체크인 날짜/시간",
            "checkOut": "체크아웃 날짜/시간",
            "roomType": "객실 타입",
            "confirmationNumber": "예약번호",
            "amenities": ["조식포함", "주차가능", ...]
        }
        """.formatted(text);
    
    return llmService.extractStructuredData(prompt, HotelInfo.class);
}
```

### 10. 여행 계획 수정 도구

#### modifyTravelPlan (대화형 여행 계획 수정)
```java
@Bean
@Description("생성된 여행 계획을 사용자 요청에 따라 수정합니다")
public Function<ModifyPlanRequest, TravelPlanResponse> modifyTravelPlan() {
    return request -> {
        log.info("여행 계획 수정: {}", request.modificationRequest());
        
        // 현재 여행 계획 조회
        TravelPlan currentPlan = planRepository.findById(request.planId());
        
        // 사용자 요청 분석
        ModificationIntent intent = analyzeModificationIntent(request.modificationRequest());
        
        // 수정 유형별 처리
        TravelPlan modifiedPlan = switch(intent.getType()) {
            case ADD_PLACE -> addPlaceToPlan(currentPlan, intent);
            case REMOVE_PLACE -> removeFromPlan(currentPlan, intent);
            case CHANGE_TIME -> adjustTiming(currentPlan, intent);
            case SWAP_PLACES -> swapPlaces(currentPlan, intent);
            case CHANGE_DAY -> moveToDifferentDay(currentPlan, intent);
            case ADJUST_PACE -> adjustPace(currentPlan, intent);
            case ADD_MEAL -> addMealStop(currentPlan, intent);
            case EXTEND_STAY -> extendStayTime(currentPlan, intent);
            default -> currentPlan;
        };
        
        // 수정된 계획 검증 (시간 충돌, 이동 거리 등)
        validateModifiedPlan(modifiedPlan);
        
        // 저장 및 반환
        planRepository.save(modifiedPlan);
        
        return new TravelPlanResponse(
            modifiedPlan.getId(),
            modifiedPlan.getItinerary(),
            modifiedPlan.getRecommendations(),
            modifiedPlan.getEstimatedBudget(),
            "수정 완료: " + intent.getSummary()
        );
    };
}

// 수정 의도 분석
private ModificationIntent analyzeModificationIntent(String request) {
    String prompt = """
        사용자의 여행 계획 수정 요청을 분석하세요:
        "%s"
        
        수정 유형 판단:
        - ADD_PLACE: 장소 추가
        - REMOVE_PLACE: 장소 제거
        - CHANGE_TIME: 시간 변경
        - SWAP_PLACES: 순서 변경
        - CHANGE_DAY: 다른 날로 이동
        - ADJUST_PACE: 일정 속도 조정
        - ADD_MEAL: 식사 추가
        - EXTEND_STAY: 체류 시간 연장
        
        구체적인 정보 추출:
        - 대상 장소/시간
        - 변경 내용
        - 이유 (있다면)
        """.formatted(request);
    
    return llmService.analyzeIntent(prompt, ModificationIntent.class);
}

// 장소 추가
private TravelPlan addPlaceToPlan(TravelPlan plan, ModificationIntent intent) {
    String placeName = intent.getTargetPlace();
    int targetDay = intent.getTargetDay();
    
    // 장소 정보 조회
    PlaceInfo place = placeService.getPlaceInfo(placeName);
    
    // 최적 시간대 찾기 (기존 일정 사이)
    TimeSlot bestSlot = findBestTimeSlot(plan.getDay(targetDay), place);
    
    // 장소 추가
    plan.getDay(targetDay).addActivity(
        bestSlot,
        new Activity(place, intent.getDuration())
    );
    
    // 이후 일정 시간 조정
    adjustSubsequentActivities(plan.getDay(targetDay), bestSlot);
    
    return plan;
}

// 일정 속도 조정
private TravelPlan adjustPace(TravelPlan plan, ModificationIntent intent) {
    String pacePreference = intent.getPacePreference(); // "느리게", "빠르게"
    
    for (Day day : plan.getDays()) {
        if (pacePreference.equals("느리게")) {
            // 장소 수 줄이기 (7개 → 5개)
            reduceActivities(day, 5);
            // 각 장소 체류 시간 늘리기
            extendActivityDurations(day, 1.3); // 30% 증가
            // 휴식 시간 추가
            addRestBreaks(day);
        } else if (pacePreference.equals("빠르게")) {
            // 장소 수 늘리기 (7개 → 9개)
            addMoreActivities(day, 9);
            // 체류 시간 단축
            shortenActivityDurations(day, 0.8); // 20% 감소
            // 이동 효율화
            optimizeRoute(day);
        }
    }
    
    return plan;
}
```

#### chatAboutPlan (여행 계획 관련 대화)
```java
@Bean
@Description("여행 계획에 대한 질문에 답하고 조언을 제공합니다")
public Function<PlanChatRequest, PlanChatResponse> chatAboutPlan() {
    return request -> {
        TravelPlan plan = planRepository.findById(request.planId());
        String question = request.question();
        
        // 질문 유형 분류
        QuestionType type = classifyQuestion(question);
        
        String response = switch(type) {
            case ABOUT_PLACE -> provideP laceDetails(plan, question);
            case TRANSPORTATION -> suggestTransportation(plan, question);
            case FOOD_RECOMMENDATION -> recommendFood(plan, question);
            case TIME_INQUIRY -> explainTiming(plan, question);
            case BUDGET_QUESTION -> analyzeBudget(plan, question);
            case ALTERNATIVE_SUGGESTION -> suggestAlternatives(plan, question);
            default -> generalResponse(plan, question);
        };
        
        return new PlanChatResponse(response, getSuggestedActions(type));
    };
}
```

### 11. 날씨 정보 도구

#### getWeatherInfo (강화된 날씨 연동 - 1주일 이내 자동 확인)
```java
@Bean
@Description("여행 날짜가 1주일 이내면 자동으로 날씨를 확인하여 여행 계획에 반영합니다")
public Function<WeatherInfoRequest, WeatherInfoResponse> getWeatherInfo() {
    return request -> {
        log.info("날씨 정보 조회: {} / {}", request.destination(), request.date());
        
        // 날짜 검증 - 1주일 이내인지 확인
        LocalDate today = LocalDate.now();
        LocalDate travelDate = request.date();
        long daysUntilTravel = ChronoUnit.DAYS.between(today, travelDate);
        
        // 1주일 이내인 경우에만 실제 날씨 API 호출
        if (daysUntilTravel >= 0 && daysUntilTravel <= 7) {
            return fetchActualWeather(request);
        } else if (daysUntilTravel > 7 && daysUntilTravel <= 14) {
            return fetchExtendedForecast(request);
        } else {
            return getHistoricalWeatherPattern(request);
        }
    };
}

// 실제 날씨 조회 (1주일 이내)
private WeatherInfoResponse fetchActualWeather(WeatherInfoRequest request) {
    try {
        // OpenWeatherMap API 또는 기상청 API 호출
        WeatherData weather = weatherApiClient.getCurrentForecast(
            request.destination(),
            request.date()
        );
        
        // 날씨 기반 여행 계획 제약사항 생성
        TravelConstraints constraints = generateWeatherConstraints(weather);
        
        return new WeatherInfoResponse(
            request.destination(),
            request.date(),
            weather.getTemperature(),
            weather.getConditionKorean(),
            weather.getPrecipitation(),
            weather.getHumidity(),
            buildDetailedWeatherSummary(weather),
            getSmartPackingRecommendation(weather),
            getWeatherBasedActivitySuggestion(weather),
            weather.getAlerts(),
            constraints,
            determineWeatherImpactLevel(weather)
        );
    } catch (Exception e) {
        log.error("실시간 날씨 조회 실패", e);
        return getFallbackWeatherResponse(request);
    }
}

// 날씨 기반 여행 제약사항 생성
private TravelConstraints generateWeatherConstraints(WeatherData weather) {
    TravelConstraints constraints = new TravelConstraints();
    
    // 비/눈 예보시
    if (weather.getPrecipitation() > 60) {
        constraints.addConstraint("INDOOR_PRIORITY", "실내 활동 위주로 계획");
        constraints.addConstraint("RAIN_GEAR", "우산/우비 필수");
        constraints.addConstraint("AVOID_OUTDOOR", "야외 활동 최소화");
    }
    
    // 극한 기온
    if (weather.getTemperature() > 35) {
        constraints.addConstraint("HEAT_WARNING", "폭염 주의");
        constraints.addConstraint("HYDRATION", "수분 섭취 중요");
        constraints.addConstraint("SHADE_PRIORITY", "그늘/실내 활동 추천");
        constraints.addConstraint("AVOID_MIDDAY", "정오~오후 3시 야외 활동 자제");
    } else if (weather.getTemperature() < -5) {
        constraints.addConstraint("COLD_WARNING", "한파 주의");
        constraints.addConstraint("WARM_CLOTHES", "방한복 필수");
        constraints.addConstraint("INDOOR_WARMUP", "실내 휴식 공간 확보");
    }
    
    // 태풍/강풍
    if (weather.getWindSpeed() > 15) {  // m/s
        constraints.addConstraint("WIND_WARNING", "강풍 주의");
        constraints.addConstraint("AVOID_COASTAL", "해안가 활동 자제");
        constraints.addConstraint("SECURE_ITEMS", "소지품 관리 주의");
    }
    
    // 미세먼지
    if (weather.getAirQuality() != null && weather.getAirQuality().getPm10() > 80) {
        constraints.addConstraint("AIR_QUALITY", "미세먼지 나쁨");
        constraints.addConstraint("MASK_REQUIRED", "마스크 착용 권장");
        constraints.addConstraint("INDOOR_PRIORITY", "실내 활동 우선");
    }
    
    return constraints;
}

// 스마트 짐 싸기 추천
private String getSmartPackingRecommendation(WeatherData weather) {
    StringBuilder packing = new StringBuilder();
    
    // 기온별 의류
    if (weather.getTemperature() < 10) {
        packing.append("🧥 필수: 패딩/코트, 목도리, 장갑\n");
        packing.append("👔 추천: 니트, 기모 옷, 히트텍\n");
    } else if (weather.getTemperature() < 20) {
        packing.append("🧥 필수: 가디건, 얇은 자켓\n");
        packing.append("👔 추천: 긴팔 티셔츠, 청바지\n");
    } else {
        packing.append("👕 필수: 반팔, 반바지, 선글라스\n");
        packing.append("🧴 추천: 선크림, 모자\n");
    }
    
    // 강수 대비
    if (weather.getPrecipitation() > 30) {
        packing.append("☔ 우천 대비: 우산, 방수 신발, 여벌 양말\n");
    }
    
    // 일교차 대비
    if (weather.getMaxTemp() - weather.getMinTemp() > 10) {
        packing.append("🌡️ 일교차 대비: 겉옷 필수, 여러 겹 레이어드\n");
    }
    
    return packing.toString();
}

// 날씨 기반 활동 추천
private String getWeatherBasedActivitySuggestion(WeatherData weather) {
    StringBuilder activities = new StringBuilder();
    
    if (weather.getCondition().equals("CLEAR") && 
        weather.getTemperature() >= 15 && weather.getTemperature() <= 25) {
        activities.append("✨ 완벽한 날씨! 야외 활동 적극 추천\n");
        activities.append("🚶 추천: 도보 관광, 피크닉, 야외 카페\n");
    } else if (weather.getPrecipitation() > 60) {
        activities.append("🌧️ 우천 대비 실내 활동 위주\n");
        activities.append("🏛️ 추천: 박물관, 쇼핑몰, 실내 테마파크\n");
        activities.append("☕ 대안: 카페 투어, 맛집 탐방\n");
    } else if (weather.getTemperature() > 30) {
        activities.append("🌡️ 더운 날씨 - 시원한 활동 추천\n");
        activities.append("💦 추천: 워터파크, 계곡, 실내 활동\n");
        activities.append("⏰ 팁: 이른 아침이나 저녁 시간 활용\n");
    }
    
    return activities.toString();
}

// 날씨 영향도 판단
private WeatherImpactLevel determineWeatherImpactLevel(WeatherData weather) {
    // 여행에 미치는 영향 정도 판단
    if (weather.hasExtremeConditions()) {
        return WeatherImpactLevel.SEVERE;  // 일정 대폭 수정 필요
    } else if (weather.hasMildDisruptions()) {
        return WeatherImpactLevel.MODERATE;  // 일부 조정 필요
    } else {
        return WeatherImpactLevel.MINIMAL;  // 영향 거의 없음
    }
}

private String buildWeatherSummary(WeatherData weather) {
    return String.format(
        "%s의 날씨는 %s이며, 기온은 %.1f°C입니다. %s",
        weather.getLocation(),
        weather.getConditionKorean(),
        weather.getTemperature(),
        weather.getPrecipitation() > 0 ? 
            String.format("강수 확률 %.0f%%", weather.getPrecipitation()) : 
            "맑은 날씨가 예상됩니다"
    );
}

private String getPackingRecommendation(WeatherData weather) {
    StringBuilder recommendation = new StringBuilder();
    
    // 온도별 의류 추천
    if (weather.getTemperature() < 5) {
        recommendation.append("두꺼운 겨울 코트, 목도리, 장갑 필수. ");
    } else if (weather.getTemperature() < 15) {
        recommendation.append("가벼운 자켓이나 가디건 추천. ");
    } else if (weather.getTemperature() < 25) {
        recommendation.append("긴팔 또는 반팔 셔츠 적당. ");
    } else {
        recommendation.append("시원한 여름 옷차림, 선크림 필수. ");
    }
    
    // 비 대비
    if (weather.getPrecipitation() > 30) {
        recommendation.append("우산이나 우비를 꼭 준비하세요. ");
    }
    
    return recommendation.toString();
}
```

### 10. 한국관광공사 Tour API 도구

#### searchTourAPI
```java
@Bean
public Function<TourAPIRequest, TourAPIResponse> searchTourAPI() {
    return request -> {
        log.info("LLM searching Tour API for: {} in {}", 
            request.searchType(), request.location());
        
        try {
            // 한국관광공사 Tour API 호출
            TourAPIClient.SearchResult result = tourAPIClient.search(
                TourAPIClient.SearchParams.builder()
                    .serviceKey(tourApiKey)
                    .contentTypeId(mapContentType(request.searchType()))
                    .areaCode(getAreaCode(request.location()))
                    .keyword(request.keyword())
                    .numOfRows(request.maxResults() != null ? request.maxResults() : 10)
                    .mobileOS("ETC")
                    .mobileApp("CompassTravel")
                    .build()
            );
            
            // 결과 변환 및 필터링
            List<TourItem> items = result.getItems().stream()
                .map(this::convertToTourItem)
                .filter(item -> isRelevantForTravel(item, request))
                .collect(Collectors.toList());
            
            // 추가 정보 enrichment
            items = enrichWithDetails(items, request.includeDetails());
            
            return new TourAPIResponse(
                request.searchType(),
                request.location(),
                items,
                generateRecommendationText(items, request),
                Map.of(
                    "totalCount", result.getTotalCount(),
                    "resultCode", result.getResultCode()
                )
            );
            
        } catch (Exception e) {
            log.error("Tour API error: ", e);
            return new TourAPIResponse(
                request.searchType(),
                request.location(),
                List.of(),
                "관광 정보를 가져올 수 없습니다. 다른 방법으로 검색해보겠습니다.",
                Map.of("error", e.getMessage())
            );
        }
    };
}

private Integer mapContentType(String searchType) {
    return switch(searchType.toUpperCase()) {
        case "TOURIST_SPOT", "관광지" -> 12;
        case "CULTURAL", "문화시설" -> 14;
        case "FESTIVAL", "축제" -> 15;
        case "COURSE", "여행코스" -> 25;
        case "ACCOMMODATION", "숙박" -> 32;
        case "SHOPPING", "쇼핑" -> 38;
        case "RESTAURANT", "음식점" -> 39;
        default -> 12; // 기본값: 관광지
    };
}

private String getAreaCode(String location) {
    // 지역명을 Tour API 지역코드로 변환
    Map<String, String> areaCodeMap = Map.of(
        "서울", "1",
        "인천", "2",
        "대전", "3",
        "대구", "4",
        "광주", "5",
        "부산", "6",
        "울산", "7",
        "세종", "8",
        "경기", "31",
        "강원", "32"
        // ... 더 많은 지역 코드
    );
    
    return areaCodeMap.entrySet().stream()
        .filter(entry -> location.contains(entry.getKey()))
        .map(Map.Entry::getValue)
        .findFirst()
        .orElse("1"); // 기본값: 서울
}

private TourItem convertToTourItem(TourAPIClient.Item apiItem) {
    return TourItem.builder()
        .contentId(apiItem.getContentid())
        .title(apiItem.getTitle())
        .address(apiItem.getAddr1() + " " + apiItem.getAddr2())
        .mapX(apiItem.getMapx())
        .mapY(apiItem.getMapy())
        .imageUrl(apiItem.getFirstimage())
        .thumbnailUrl(apiItem.getFirstimage2())
        .tel(apiItem.getTel())
        .overview(apiItem.getOverview())
        .build();
}

private List<TourItem> enrichWithDetails(List<TourItem> items, boolean includeDetails) {
    if (!includeDetails) return items;
    
    // 각 아이템에 대해 상세 정보 API 호출
    return items.stream()
        .map(item -> {
            try {
                TourAPIClient.DetailResult detail = tourAPIClient.getDetail(
                    item.getContentId()
                );
                item.setDetailInfo(detail);
                return item;
            } catch (Exception e) {
                log.warn("Failed to get details for item: {}", item.getContentId());
                return item;
            }
        })
        .collect(Collectors.toList());
}

private String generateRecommendationText(List<TourItem> items, TourAPIRequest request) {
    if (items.isEmpty()) {
        return String.format("%s 지역의 %s 정보를 찾을 수 없습니다.", 
            request.location(), request.searchType());
    }
    
    StringBuilder recommendation = new StringBuilder();
    recommendation.append(String.format("%s 지역의 추천 %s:\n", 
        request.location(), request.searchType()));
    
    items.stream().limit(3).forEach(item -> {
        recommendation.append(String.format("• %s: %s\n", 
            item.getTitle(), 
            item.getAddress()));
    });
    
    return recommendation.toString();
}
```

#### Request/Response DTOs for Weather and Tour API

```java
// 날씨 정보 요청/응답
public record WeatherInfoRequest(
    String destination,
    LocalDate date,
    boolean includeForecast,
    String units // metric, imperial
) {}

public record WeatherInfoResponse(
    String destination,
    LocalDate date,
    Double temperature,
    String condition,
    Double precipitation,
    Double humidity,
    String summary,
    String packingRecommendation,
    String activitySuggestion,
    List<String> weatherAlerts
) {}

// Tour API 요청/응답
public record TourAPIRequest(
    String searchType, // TOURIST_SPOT, CULTURAL, FESTIVAL, RESTAURANT, etc.
    String location,
    String keyword,
    Integer maxResults,
    boolean includeDetails,
    Map<String, String> filters
) {}

public record TourAPIResponse(
    String searchType,
    String location,
    List<TourItem> items,
    String recommendationText,
    Map<String, Object> metadata
) {}

public record TourItem(
    String contentId,
    String title,
    String address,
    Double mapX,
    Double mapY,
    String imageUrl,
    String thumbnailUrl,
    String tel,
    String overview,
    Map<String, Object> detailInfo
) {}
```

---

# Part 3: 구현

## 4. 핵심 구현

### 4.1 MainLLMOrchestrator

```java
@Service
@Slf4j
public class MainLLMOrchestrator {
    
    private final ChatClient chatClient;
    private final List<FunctionCallback> availableFunctions;
    
    public MainLLMOrchestrator(
        ChatClient chatClient,
        ApplicationContext context
    ) {
        this.chatClient = chatClient;
        
        // 모든 Function 자동 등록
        this.availableFunctions = Arrays.asList(
            createFunctionCallback(context, "startFollowUpSession"),
            createFunctionCallback(context, "continueFollowUp"),
            createFunctionCallback(context, "recommendTemplates"),
            createFunctionCallback(context, "searchRegions"),
            createFunctionCallback(context, "searchAttractions"),
            createFunctionCallback(context, "generateTravelPlan"),
            createFunctionCallback(context, "searchWithPerplexity"),
            createFunctionCallback(context, "getConversationContext"),
            createFunctionCallback(context, "updateConversationContext"),
            createFunctionCallback(context, "getWeatherInfo"),
            createFunctionCallback(context, "searchTourAPI")
        );
    }
    
    public ChatResponse processUserMessage(String message, String threadId) {
        // 시스템 프롬프트 구성
        String systemPrompt = buildSystemPrompt();
        
        // 사용자 메시지와 컨텍스트 결합
        String fullPrompt = buildFullPrompt(message, threadId);
        
        // ChatOptions에 Function Calling 설정
        ChatOptions options = ChatOptionsBuilder.builder()
            .withModel("gpt-4")
            .withTemperature(0.7)
            .withFunctions(availableFunctions)
            .build();
        
        // LLM 호출 (자동으로 필요한 Function 호출)
        return chatClient.call(new Prompt(fullPrompt, options));
    }
    
    private String buildSystemPrompt() {
        return """
            당신은 친근하고 도움이 되는 여행 AI 어시스턴트입니다.
            사용자와 자연스러운 대화를 나누면서 여행 계획을 도와줍니다.
            
            사용 가능한 도구:
            1. startFollowUpSession: 여행 정보 수집 시작
            2. continueFollowUp: 추가 정보 수집 계속
            3. recommendTemplates: 여행 템플릿 추천
            4. searchRegions: 지역 검색
            5. searchAttractions: 관광지 검색
            6. generateTravelPlan: 여행 계획 생성
            7. searchWithPerplexity: 실시간 정보 검색
            8. getConversationContext: 대화 컨텍스트 조회
            9. updateConversationContext: 컨텍스트 업데이트
            
            의사결정 가이드:
            - 일반적인 인사나 대화: 직접 응답
            - 여행 계획 요청: startFollowUpSession 호출
            - 템플릿/테마 질문: recommendTemplates 호출
            - 특정 지역 질문: searchRegions 또는 searchAttractions 호출
            - 실시간 정보 필요: searchWithPerplexity 호출
            - 진행 중인 세션 있음: continueFollowUp 호출
            
            응답 원칙:
            1. 친근하고 자연스러운 대화체 사용
            2. 이모지를 적절히 활용
            3. 필요한 경우에만 도구 사용
            4. 사용자 의도를 정확히 파악
            """;
    }
    
    private FunctionCallback createFunctionCallback(ApplicationContext context, String functionName) {
        return FunctionCallbackWrapper.builder(context.getBean(functionName, Function.class))
            .withName(functionName)
            .withDescription(getFunctionDescription(functionName))
            .build();
    }
    
    private String getFunctionDescription(String functionName) {
        return switch(functionName) {
            case "startFollowUpSession" -> "여행 정보 수집을 시작합니다";
            case "continueFollowUp" -> "여행 정보 수집을 계속합니다";
            case "recommendTemplates" -> "사용자에게 맞는 여행 템플릿을 추천합니다";
            case "searchRegions" -> "테마와 요구사항에 맞는 지역을 검색합니다";
            case "searchAttractions" -> "특정 지역의 관광지를 검색합니다";
            case "generateTravelPlan" -> "수집된 정보로 여행 계획을 생성합니다";
            case "searchWithPerplexity" -> "Perplexity로 실시간 정보를 검색합니다";
            case "getConversationContext" -> "현재 대화 컨텍스트를 조회합니다";
            case "updateConversationContext" -> "대화 컨텍스트를 업데이트합니다";
            default -> "기능 설명 없음";
        };
    }
}
```

### 2. Spring Configuration

```java
@Configuration
@EnableConfigurationProperties
public class FunctionCallingConfig {
    
    @Autowired
    private TravelInfoCollectionService followUpService;
    
    @Autowired
    private TemplateRecommendationService templateService;
    
    @Autowired
    private RegionSearchService regionService;
    
    @Autowired
    private TripService tripService;
    
    @Autowired
    private PerplexityClient perplexityClient;
    
    @Autowired
    private ConversationContextManager contextManager;
    
    // 모든 Function Bean 등록
    @Bean
    public Function<StartFollowUpRequest, FollowUpResponse> startFollowUpSession() {
        return request -> followUpService.startSession(request);
    }
    
    @Bean
    public Function<ContinueFollowUpRequest, FollowUpResponse> continueFollowUp() {
        return request -> followUpService.continueSession(request);
    }
    
    @Bean
    public Function<TemplateRecommendRequest, TemplateRecommendResponse> recommendTemplates() {
        return request -> templateService.recommend(request);
    }
    
    @Bean
    public Function<RegionSearchRequest, RegionSearchResponse> searchRegions() {
        return request -> regionService.search(request);
    }
    
    @Bean
    public Function<AttractionSearchRequest, AttractionSearchResponse> searchAttractions() {
        return request -> regionService.searchAttractions(request);
    }
    
    @Bean
    public Function<GeneratePlanRequest, TravelPlanResponse> generateTravelPlan() {
        return request -> tripService.generatePlan(request);
    }
    
    @Bean
    public Function<PerplexitySearchRequest, PerplexitySearchResponse> searchWithPerplexity() {
        return request -> perplexityClient.search(request);
    }
    
    @Bean
    public Function<ContextRequest, ConversationContext> getConversationContext() {
        return request -> contextManager.getContext(request.threadId());
    }
    
    @Bean
    public Function<UpdateContextRequest, ConversationContext> updateConversationContext() {
        return request -> contextManager.updateContext(request);
    }
}
```

### 3. 통합 ChatController

```java
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class UnifiedChatController {
    
    private final MainLLMOrchestrator orchestrator;
    
    @PostMapping("/message")
    public ResponseEntity<ChatResponse> handleMessage(
        @RequestBody ChatRequest request,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            // 모든 요청을 메인 LLM Orchestrator로 전달
            ChatResponse response = orchestrator.processUserMessage(
                request.getMessage(),
                request.getThreadId()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error processing message", e);
            return ResponseEntity.internalServerError()
                .body(ChatResponse.error("처리 중 오류가 발생했습니다."));
        }
    }
}
```

---

## 5. 대화 플로우

### 5.1 기본 대화 플로우

### 5.2 Adaptive Follow-up 플로우

**핵심 원칙**: MainLLMOrchestrator가 모든 대화를 제어하며, Follow-up 도구는 보조 역할만 수행

#### 예시 1: 필수 정보 자동 추출 및 차감
```
사용자: "다음주 금요일에 부산 2박3일로 가려고 하는데"

[MainLLMOrchestrator - 정보 추출]
✅ 목적지: 부산
✅ 날짜: 다음주 금요일, 2박3일
❌ 인원: ?
❌ 예산: ?
❌ 여행 스타일: ?

→ startAdaptiveFollowUp() 호출
→ 남은 필수 정보: 인원, 예산, 스타일

응답: "부산 2박3일 좋네요! 몇 명이서 가시나요?"

사용자: "여자친구랑 둘이서, 맛집 위주로 다니려고"

[MainLLMOrchestrator - 정보 업데이트]
✅ 인원: 2명
✅ 여행 스타일: 맛집 투어
❌ 예산: ?

→ continueAdaptiveFollowUp() 호출
→ 남은 필수 정보: 예산만

응답: "커플 맛집 여행이시군요! 예산은 대략 어느 정도 생각하세요?"

사용자: "50만원 정도?"

[MainLLMOrchestrator - 모든 필수 정보 수집 완료]
✅ 목적지: 부산
✅ 날짜: 다음주 금요일, 2박3일  
✅ 인원: 2명
✅ 예산: 50만원
✅ 여행 스타일: 맛집 투어

→ generateTravelPlan() 호출  // 계획 생성 시작

응답: "완벽해요! 부산 2박3일 맛집 투어 계획 만들어드릴게요!"
```

#### 예시 2: 장기 여행 - 다중 목적지 처리
```
사용자: "일주일 휴가 받았는데 일본 여행 가려고요"

[MainLLMOrchestrator - 정보 추출]
✅ 목적지: 일본 (국가 단위)
✅ 기간: 7일 (장기 여행 감지)
❌ 날짜: ?
❌ 인원: ?
❌ 예산: ?
❌ 여행 스타일: ?

→ startAdaptiveFollowUp() 호출
→ 7일 장기 여행 → 다중 도시 가능성

응답: "일주일 일본 여행 좋네요! 도쿄만 가실 건가요, 
      아니면 오사카나 교토도 함께 둘러보실 예정이신가요?"

사용자: "도쿄 들어가서 오사카로 나올까 생각중이에요"

[MainLLMOrchestrator - 정보 업데이트]
✅ 목적지: [도쿄, 오사카] (다중 목적지)
❌ 날짜: ?
❌ 인원: ?
❌ 예산: ?
❌ 여행 스타일: ?

응답: "도쿄-오사카 루트 인기 있죠! 중간에 교토도 들르시면 좋을 것 같은데요.
      언제 출발하실 예정이신가요?"

사용자: "4월 첫째주에 친구랑 둘이서 가요. 예산은 150만원 정도"

[MainLLMOrchestrator - 정보 업데이트]
✅ 목적지: [도쿄, 오사카] + 교토 제안
✅ 날짜: 4월 첫째주
✅ 인원: 2명
✅ 예산: 150만원
❌ 여행 스타일: ?

응답: "4월 벚꽃 시즌이네요! 관광 위주로 다니실 건가요, 
      아니면 쇼핑이나 맛집도 중요하신가요?"

사용자: "벚꽃 구경이랑 관광 위주로요"

[MainLLMOrchestrator - 모든 필수 정보 수집 완료]
✅ 목적지: [도쿄, 교토, 오사카] (3개 도시)
✅ 날짜: 4월 첫째주, 7일
✅ 인원: 2명
✅ 예산: 150만원
✅ 여행 스타일: 벚꽃, 관광

→ generateTravelPlan() 호출
→ 다중 도시 일정 생성 (도쿄 3일, 교토 2일, 오사카 2일)

응답: "도쿄-교토-오사카 7일 벚꽃 여행 일정 만들어드릴게요!"
```

#### 예시 3: 특정 장소 희망 - 카테고리 분류 및 추천
```
사용자: "부산 가서 감천문화마을이랑 해운대 가고 싶어요"

[MainLLMOrchestrator - 정보 추출]
✅ 목적지: 부산
✅ 방문 희망 장소: [감천문화마을(관광지), 해운대(관광지)]
❌ 날짜: ?
❌ 인원: ?
❌ 예산: ?
❌ 여행 스타일: ?

→ startAdaptiveFollowUp() 호출
→ 관광지 카테고리 감지 → 추가 관광지 제안 준비

응답: "감천문화마을과 해운대 좋은 선택이네요! 
      혹시 태종대나 송정해수욕장도 관심 있으신가요?
      그리고 언제쯤 가실 예정이신가요?"

사용자: "태종대도 좋을 것 같아요. 이번 주말에 1박2일로"

[MainLLMOrchestrator - 정보 업데이트]
✅ 방문 희망 장소: [감천문화마을, 해운대, 태종대]
✅ 날짜: 이번 주말, 1박2일
❌ 인원: ?
❌ 예산: ?
❌ 여행 스타일: 관광 (장소로 유추)

응답: "부산 주요 관광지 코스네요! 혹시 부산에서 
      꼭 먹고 싶은 음식이나 맛집도 있으신가요?"

사용자: "돼지국밥이랑 밀면은 꼭 먹고 싶어요"

[MainLLMOrchestrator - 정보 업데이트]
✅ 방문 희망 장소: 
   - 관광지: [감천문화마을, 해운대, 태종대]
   - 맛집: [돼지국밥, 밀면]
❌ 인원: ?
❌ 예산: ?

응답: "돼지국밥 맛집으로 '쌍둥이돼지국밥', 밀면은 '개금밀면' 추천드려요!
      몇 분이서 가시나요?"

사용자: "친구랑 둘이서, 예산은 30만원 정도예요"

[MainLLMOrchestrator - 모든 필수 정보 수집 완료]
✅ 목적지: 부산
✅ 날짜: 이번 주말, 1박2일
✅ 인원: 2명
✅ 예산: 30만원
✅ 여행 스타일: 관광 + 맛집
✅ 방문 희망 장소: 
   - 관광지: 감천문화마을, 해운대, 태종대
   - 맛집: 돼지국밥(쌍둥이돼지국밥), 밀면(개금밀면)

→ generateTravelPlan() 호출
→ PlanningStrategy: USER_PLACES_CENTERED (사용자 지정 장소 중심)
→ 희망 장소 3개 + 추가 추천 4개 = 총 7개 장소/일

생성된 일정:
[Day 1]
09:00-10:30 감천문화마을 (사용자 지정, 여유시간 포함)
10:30-11:00 이동 (30분)
11:00-12:30 태종대 (사용자 지정, 여유시간 포함)  
12:30-14:00 자갈치시장 점심 (추천 - 동선 고려)
14:00-15:30 송도해상케이블카 (추천 - 근처)
15:30-16:30 송도해수욕장 카페 (휴식)
16:30-18:00 해운대해수욕장 (사용자 지정)
18:00-19:30 해운대 저녁 (밀면/돼지국밥)

[Day 2]
10:00-11:30 광안리해수욕장
11:30-13:00 광안리 브런치
13:00-14:30 부산시립미술관
14:30-16:00 센텀시티 쇼핑
16:00 귀가

응답: "부산 1박2일 일정 만들어드렸어요! 
      원하시는 감천문화마을, 태종대, 해운대를 모두 포함하고
      동선에 맞춰 자갈치시장, 송도케이블카도 추천드렸어요.
      각 장소마다 충분한 시간을 배정해서 여유롭게 둘러보실 수 있어요!"
```

#### 예시 4: 대화 속에서 정보 자연스럽게 수집
```
사용자: "제주도 가고 싶은데 예산 30만원으로 가능할까?"

[MainLLMOrchestrator - 정보 추출]
✅ 목적지: 제주도
✅ 예산: 30만원
❌ 날짜: ?
❌ 인원: ?
❌ 여행 스타일: ?

→ startAdaptiveFollowUp() 호출
→ 남은 필수 정보: 날짜, 인원, 스타일

응답: "30만원으로 제주도 충분히 가능해요! 언제, 몇 명이서 가실 건가요?"

사용자: "아 근데 30만원이면 숙소가 괜찮을까? 혼자 가는건데"

[MainLLMOrchestrator - 정보 업데이트]
✅ 인원: 1명
❌ 날짜: ?
❌ 여행 스타일: ?

응답: "혼자 여행이시면 게스트하우스나 호스텔로 충분해요! 
      언제 출발하실 예정이신가요?"

사용자: "이번 주말 1박2일로, 자연 경관 위주로 보고 싶어"

[MainLLMOrchestrator - 모든 필수 정보 수집 완료]
✅ 목적지: 제주도
✅ 날짜: 이번 주말, 1박2일
✅ 인원: 1명
✅ 예산: 30만원
✅ 여행 스타일: 자연 경관

→ generateTravelPlan() 호출
→ PlanningStrategy: BALANCED_SCHEDULE (기본 7개 장소/일)
→ 테마: 자연 경관 중심

생성된 일정:
[Day 1] - 서부/남부 코스
09:00-10:30 한라산 어리목 코스 입구 (가벼운 트레킹)
10:30-12:00 산굼부리 (분화구)
12:00-13:30 흑돼지 점심
13:30-15:00 천지연폭포
15:00-16:00 오설록 티뮤지엄 (휴식)
16:00-17:30 주상절리대
17:30-19:00 서귀포 저녁
19:00-20:00 서귀포 올레시장 산책

[Day 2] - 동부 코스
09:00-10:30 성산일출봉
10:30-12:00 섭지코지
12:00-13:30 성산 해물 점심
13:30-15:00 만장굴
15:00-16:30 비자림
16:30 공항으로 이동

응답: "제주도 1박2일 자연 중심 여행 계획 완성! 
      서부-남부-동부 순으로 동선 짜서 이동 최소화했어요.
      예산 30만원이면 렌터카, 숙박, 식사 모두 충분해요!"
```

#### 예시 5: 타이트한 일정 선호
```
사용자: "서울 당일치기로 최대한 많이 보고 싶어요"

[MainLLMOrchestrator]
✅ 목적지: 서울
✅ 날짜: 당일치기
✅ 여행 스타일: 타이트 (많이 보고 싶다고 표현)

→ generateTravelPlan() 호출
→ PlanningStrategy: PACKED_SCHEDULE (9-10개 장소)

생성된 일정:
08:00-09:00 경복궁
09:00-09:30 북촌한옥마을
09:30-10:30 인사동
10:30-11:30 명동
11:30-12:30 남산타워
12:30-13:30 명동 점심
13:30-14:30 동대문디자인플라자
14:30-15:30 광장시장
15:30-16:30 청계천
16:30-17:30 홍대
17:30-18:30 홍대 저녁
18:30-19:30 한강공원
19:30-21:00 강남/가로수길
21:00-22:00 동대문 야시장

응답: "서울 당일치기 타이트 코스 준비했어요!
      14시간 동안 서울 주요 명소 14곳 방문 가능해요.
      체력적으로 힘들 수 있으니 편한 신발 필수!"
```

#### 예시 6: 여유로운 일정 선호
```
사용자: "강릉 2박3일 여유롭게 쉬다 오고 싶어요"

[MainLLMOrchestrator]
✅ 목적지: 강릉
✅ 날짜: 2박3일
✅ 여행 스타일: 여유로운 (쉬다 오고 싶다고 표현)

→ generateTravelPlan() 호출
→ PlanningStrategy: RELAXED_SCHEDULE (5-6개 장소/일)

생성된 일정:
[Day 1]
14:00 강릉 도착
15:00-16:30 안목해변 카페거리
17:00-18:30 정동진 일몰
19:00-20:30 저녁 (회센터)

[Day 2]
10:00-11:30 늦은 아침/브런치
12:00-14:00 오죽헌 + 선교장
14:30-16:00 경포대
16:30-18:00 경포해변 산책
19:00-20:30 저녁 (순두부/막국수)

[Day 3]
10:00-11:30 강릉커피박물관
12:00-13:30 중앙시장 점심
14:00 귀가

응답: "강릉 여유로운 힐링 여행 일정이에요!
      오전 10시 시작, 충분한 휴식 시간 포함,
      카페와 바다 중심으로 느긋하게 즐기실 수 있어요."
```

#### 예시 7: 빠른 입력 인터페이스 활용
```
사용자: "여행 가고 싶어"

[MainLLMOrchestrator]
→ showQuickInputForm() 호출
→ 빠른 입력 폼 UI 제시

사용자가 폼 작성:
- 목적지: [미정] → 추천 받기 클릭
- 날짜: 3/15-3/17 선택
- 인원: 2명
- 예산: 100만원
- 스타일: 관광, 맛집 선택

→ submitQuickTravelForm() 호출
→ 목적지 추천 필요 감지

응답: "선택하신 조건에 맞는 추천 목적지입니다:
      🌸 경주 (봄 벚꽃 + 역사 관광)
      🏖️ 부산 (바다 + 맛집)
      🏔️ 강릉 (자연 + 카페)
      원하시는 곳을 선택해주세요!"

사용자: "부산 선택"

→ analyzeDetailedNeeds() 호출
→ 기본 정보는 수집됨, 세부 니즈 파악 시작

응답: "부산으로 결정하셨군요! 더 맞춤형 여행을 위해 몇 가지만 더 여쭤볼게요.
      아침 일찍 활동하시는 편인가요, 늦게까지 즐기시는 편인가요?"

사용자: "저희는 늦잠 자는 편이라 10시쯤 시작하면 좋겠어요"

→ analyzeDetailedNeeds() 호출 (다음 질문)

응답: "알겠습니다! 그럼 로컬 맛집과 유명 맛집 중 어떤 걸 더 좋아하세요?"

사용자: "현지인들이 가는 진짜 맛집이요!"

→ analyzeDetailedNeeds() 호출 (다음 질문)

응답: "좋아요! 렌터카를 이용하실 건가요, 대중교통을 선호하세요?"

사용자: "대중교통이요"

→ analyzeDetailedNeeds() 완료
→ generateTravelPlan() 호출 (모든 니즈 파악 완료)

생성된 맞춤 일정:
- 10시 시작하는 여유로운 일정
- 현지인 맛집 위주 (쌍둥이돼지국밥, 할매재첩국, 동래파전)
- 대중교통 동선 최적화 (지하철역 근처 중심)

응답: "부산 2박3일 맞춤 여행 계획 완성!
      늦은 아침형 일정으로 10시 시작, 현지 맛집 중심,
      지하철로 편하게 이동할 수 있도록 짰어요!"
```

#### 예시 8: 대화형과 폼의 혼합 사용
```
사용자: "부산 여행 계획 좀 짜줘"

[MainLLMOrchestrator]
→ 부산은 언급됨, 나머지 정보 필요
→ showQuickInputForm() 호출 (부산 자동 입력됨)

UI 표시:
┌─────────────────────────────────────────────┐
│ 🧳 여행 정보를 입력해주세요                    │
├─────────────────────────────────────────────┤
│ 📍 목적지: [부산] ✓ (자동 입력됨)             │
│ 📅 날짜: [____] ~ [____]                    │
│ 👥 인원: [2명 ▼]                            │
│ 💰 예산: [────●────] 50만원                 │
│ ✨ 여행 스타일: [ ] 관광 [ ] 휴양 [ ] 맛집   │
└─────────────────────────────────────────────┘

사용자: "아 그냥 대화로 할게"

→ startAdaptiveFollowUp() 호출
→ 대화형 모드로 전환

응답: "네, 편하게 대화로 진행할게요! 
      부산은 언제쯤 가실 예정이신가요?"
```

#### 예시 9: 대화 속에서 정보 자연스럽게 수집
Function 호출: startAdaptiveFollowUp({
    "userId": "user456",
    "initialMessage": "다음 달쯤 제주도 여행 가려고 생각중이에요",
    "context": {"timeframe": "next_month"}
})

내부 처리:
- '다음 달쯤', '생각중' → 여유있는 사용자
- 전략 결정: DETAILED_FRIENDLY
- 목적지(제주도) 이미 언급 → 날짜부터 물어보기

응답: "제주도 여행 좋은 선택이네요! 🏝️ 
      다음 달 구체적으로 언제쯤 출발하실 예정이신가요?
      평일과 주말 중 어느 쪽을 선호하시나요?"

[빠른 선택 옵션 제공]
• 첫째 주 (3/1-3/7)
• 둘째 주 (3/8-3/14)  
• 셋째 주 (3/15-3/21)
• 넷째 주 (3/22-3/31)
```

### 예시 3: 맥락 인식 - 이미 언급된 정보 건너뛰기
```
사용자: "가족 4명이서 부산 2박 3일 여행 계획 짜줘"

LLM 판단: 이미 많은 정보 제공, startAdaptiveFollowUp() 호출
Function 호출: startAdaptiveFollowUp({
    "userId": "user789",
    "initialMessage": "가족 4명이서 부산 2박 3일 여행 계획 짜줘",
    "context": {}
})

내부 처리:
- 이미 수집된 정보: 목적지(부산), 기간(2박3일), 동행자(가족 4명)
- 전략: EXPERT_EFFICIENT
- 날짜와 예산만 추가로 필요

응답: "부산 2박 3일 가족 여행이시군요! 
      언제 출발하실 예정이신가요?"

사용자: "이번 주말"

Function 호출: getNextAdaptiveQuestion({
    "sessionId": "session789",
    "currentContext": {"dates": "이번 주말"}
})

내부 처리:
- LLM이 다음 질문 결정
- 가족 여행 → 예산이 중요
- 하지만 충분한 정보 있음 → 선택적 질문

응답: "좋습니다! 가족 여행 예산은 대략 어느 정도로 생각하시나요?
      예산을 알려주시면 더 정확한 계획을 만들 수 있어요.
      아니면 바로 계획을 만들어드릴 수도 있습니다!"

[빠른 선택 옵션]
• 100만원 이하
• 100-200만원
• 200만원 이상  
• 바로 계획 생성
```

### 예시 4: 사용자 피로도 감지
```
사용자: "여행 가고 싶어"

LLM 판단: 모호한 요청, startAdaptiveFollowUp() 호출
응답: "어디로 여행 가고 싶으신가요? 😊"

사용자: "몰라"

Function 호출: continueAdaptiveFollowUp({
    "sessionId": "session999",
    "userResponse": "몰라",
    "responseTime": 500  // 매우 빠른 짧은 답변
})

내부 처리:
- 짧고 모호한 답변 → 피로하거나 결정 못하는 상태
- 전략 변경: CASUAL_EXPLORATORY
- 템플릿 추천으로 전환

Function 호출: recommendTemplates({
    "userMessage": "여행 추천",
    "context": {"userMood": "undecided"}
})

응답: "그럼 제가 인기 있는 여행 테마를 추천해드릴게요!
      마음에 드는 걸 선택해보세요 🎯
      
      [9개 템플릿 그리드 표시]"
```

### 예시 5: 정보 검색 요청
```
사용자: "부산에 지금 축제하는 거 있어?"

LLM 판단: 실시간 정보 필요, searchWithPerplexity() 호출
Function 호출: searchWithPerplexity({
    "query": "부산 현재 진행중인 축제 2024",
    "type": "real-time"
})
응답: "부산에서 현재 진행 중인 축제를 확인해봤어요! 🎉
      [검색 결과 표시]"
```

### 예시 6: 날씨 자동 확인 (1주일 이내 여행)
```
사용자: "이번 주 토요일에 제주도 여행 가려고 해요"

[MainLLMOrchestrator 내부 처리]
- 날짜 파싱: 3일 후 (1주일 이내 ✓)
- 자동으로 날씨 확인 결정

→ getWeatherInfo() 자동 호출
Function 호출: getWeatherInfo({
    "destination": "제주도",
    "date": "2024-12-21",
    "includeForecast": true
})

날씨 API 응답:
- 기온: 12°C
- 날씨: 흐리고 비
- 강수확률: 80%
- 풍속: 20m/s (강풍)

[날씨 기반 제약사항 생성]
- INDOOR_PRIORITY: 실내 활동 위주
- RAIN_GEAR: 우산/우비 필수
- WIND_WARNING: 강풍 주의
- AVOID_COASTAL: 해안가 활동 자제

응답: "토요일 제주도 날씨를 확인해봤어요! 🌧️
      
      📍 날씨 정보
      - 기온: 12°C (쌀쌀해요)
      - 비 예보 (강수확률 80%)
      - 강풍 주의보 (풍속 20m/s)
      
      🎒 짐 싸기 추천
      - 우비/우산 필수
      - 방풍 자켓
      - 여벌 양말
      
      📋 일정 추천 (날씨 고려)
      - 실내 관광지 위주로 계획
      - 제주 아쿠아리움, 박물관
      - 실내 카페, 맛집 투어
      - 해안도로는 피하시는 게 좋겠어요
      
      날씨가 좋지 않지만 실내에서 즐길 수 있는 
      멋진 장소들로 일정을 짜드릴게요!"
```

### 예시 7: 출발/도착 시간 반영한 일정 생성
```
사용자: "부산 당일치기 여행 가려고 해요"

[빠른 입력 폼 표시]
- 날짜: 12월 25일
- 출발 시간: 오전 7시 (서울 출발)
- 도착 시간: 오후 10시 (서울 도착)
- 인원: 2명
- 예산: 30만원

[시간 계산]
- 서울→부산: KTX 3시간
- 07:00 출발 → 10:00 부산 도착
- 19:00 부산 출발 → 22:00 서울 도착
- 실제 관광 가능 시간: 9시간 (10:00-19:00)

→ generateTravelPlan() 호출 (시간 제약 반영)

생성된 당일치기 일정:
10:00 부산역 도착
10:30-12:00 감천문화마을 (대표 관광지)
12:00-13:30 자갈치시장 점심 (회 또는 해산물)
13:30-15:00 용두산공원 & 부산타워
15:00-16:30 해운대 해수욕장
16:30-18:00 센텀시티 or 광안리
18:00-19:00 부산역으로 이동 & 기념품
19:00 KTX 탑승

응답: "서울에서 당일치기로 9시간 동안 부산을 즐기는 일정이에요!
      주요 명소 5곳을 효율적으로 둘러볼 수 있도록 동선을 짰어요."
```

### 예시 8: 2박3일 여행 - 첫날 늦게 도착, 마지막날 일찍 출발
```
사용자: "제주도 2박3일 가족여행"

[빠른 입력 폼]
- 날짜: 3/15-3/17
- 출발: 3/15 오후 2시 (서울 집 출발)
- 도착: 3/17 오후 6시 (서울 집 도착)
- 인원: 4명 (어른 2, 아이 2)

[시간 계산]
- Day 1: 14:00 출발 → 17:00 제주 도착 (관광 5시간)
- Day 2: 전일 관광 가능 (13시간)
- Day 3: 15:00 공항 출발 필요 (관광 6시간)

생성된 일정:

[Day 1 - 3/15 (5시간)]
17:00 제주공항 도착 & 렌터카 픽업
17:30 숙소 체크인 (제주시)
18:30-20:00 동문시장 저녁 (흑돼지)
20:00-21:30 용두암 야경 & 용연구름다리

[Day 2 - 3/16 (전일)]
09:00-10:30 한라산 어리목 탐방로 (가족 트레킹)
10:30-12:00 산굼부리
12:00-13:30 점심 (고기국수)
13:30-15:00 에코랜드 (아이들 좋아함)
15:00-16:30 성산일출봉
16:30-17:30 섭지코지
17:30-19:00 성산 저녁 (해물)
19:00-20:30 숙소 이동 & 휴식

[Day 3 - 3/17 (6시간)]
09:00 체크아웃
09:30-11:00 천지연폭포
11:00-12:30 중문관광단지 (아이들 놀이)
12:30-13:30 점심
13:30-14:30 공항 이동 & 렌터카 반납
15:00 제주공항 출발

응답: "첫날 저녁과 마지막날 오전을 활용한 알찬 2박3일 일정입니다!
      아이들과 함께 즐길 수 있는 장소 위주로 구성했어요."
```

### 예시 9: OCR로 예약 정보 자동 입력
```
사용자: "제주도 여행 계획 짜줘"

[빠른 입력 폼 표시]
사용자: "항공권 이미지 업로드" 버튼 클릭
→ 항공권 사진 업로드

[OCR 처리]
→ processReservationOCR() 호출

추출된 정보:
- 항공사: 대한항공
- 출발: 김포공항 12/25 09:00
- 도착: 제주공항 12/25 10:20
- 복귀: 제주공항 12/27 18:00
- 좌석: 15A, 15B

사용자: "호텔 예약 확인서" 업로드

[OCR 처리]
→ processReservationOCR() 호출

추출된 정보:
- 호텔: 신라호텔 제주
- 체크인: 12/25 15:00
- 체크아웃: 12/27 11:00
- 객실: 오션뷰 디럭스
- 조식 포함

[자동 입력된 정보]
✅ 날짜: 12/25-12/27
✅ 출발 시간: 09:00 (김포공항)
✅ 도착 시간: 18:00 (제주공항)
✅ 숙소: 신라호텔 (서귀포)
✅ 인원: 2명

응답: "항공권과 호텔 정보를 확인했어요!
      김포에서 9시 출발, 신라호텔 숙박이시군요.
      나머지 정보만 알려주시면 완벽한 일정을 만들어드릴게요."

사용자: "예산 200만원, 휴양 위주로"

→ generateTravelPlan() 호출 (예약 정보 반영)

생성된 일정:
[Day 1 - 12/25]
10:20 제주공항 도착
11:00 렌터카 픽업
12:00 중문 맛집 점심
13:30 중문관광단지 관람
15:00 신라호텔 체크인 & 휴식
17:00 호텔 수영장/스파
19:00 호텔 레스토랑 저녁

[Day 2 - 12/26]
08:00 호텔 조식
09:30 성산일출봉
11:00 섭지코지
12:30 성산 해물 점심
14:00 만장굴
15:30 비자림
17:00 호텔 복귀 & 휴식
19:00 서귀포 매일올레시장

[Day 3 - 12/27]
08:00 호텔 조식
09:30 체크아웃
10:00 천지연폭포
11:30 점심
13:00 공항 이동
15:00 면세점 쇼핑
17:00 체크인
18:00 제주공항 출발
```

### 예시 10: 여행 계획 대화형 수정
```
[이미 생성된 여행 계획이 있는 상태]

사용자: "둘째 날 성산일출봉 대신 한라산 가고 싶어"

→ modifyTravelPlan() 호출
→ 수정 의도: CHANGE_PLACE (장소 변경)

응답: "성산일출봉을 한라산으로 변경할게요!
      한라산 어리목 코스는 왕복 3-4시간이 걸려서
      오후 일정을 조정했어요."

수정된 Day 2:
08:00 호텔 조식
09:00 한라산 어리목 코스
13:00 점심 (한라산 휴게소)
14:30 산굼부리
16:00 에코랜드
18:00 호텔 복귀

---

사용자: "전체적으로 일정이 너무 빡빡한 것 같아. 좀 여유롭게 해줘"

→ modifyTravelPlan() 호출
→ 수정 의도: ADJUST_PACE (속도 조정 - 느리게)

응답: "더 여유로운 일정으로 조정했어요! 
      장소를 줄이고 각 장소 체류 시간을 늘렸습니다."

수정 후:
- 일일 장소: 7개 → 5개
- 체류 시간: 1.5시간 → 2시간
- 휴식 추가: 카페/휴게 시간 추가

---

사용자: "첫날 저녁에 흑돼지 먹고 싶어"

→ modifyTravelPlan() 호출
→ 수정 의도: ADD_MEAL (특정 식사 추가)

응답: "첫날 저녁을 흑돼지 맛집으로 변경했어요!
      호텔에서 15분 거리의 '돈사돈' 추천드려요."

수정된 Day 1 저녁:
18:30 호텔 출발
19:00 돈사돈 (흑돼지 맛집)
20:30 호텔 복귀 & 야경 감상

---

사용자: "마지막 날 공항 가기 전에 기념품 살 시간 있을까?"

→ chatAboutPlan() 호출
→ 질문 유형: TIME_INQUIRY

응답: "네, 충분해요! 현재 일정상 13시에 공항으로 출발하는데,
      11:30-13:00 사이에 중문면세점이나 제주관광공사 면세점에
      들르실 수 있어요. 공항에도 2시간 여유가 있어서
      공항 면세점도 이용 가능합니다."

---

사용자: "섭지코지 대신 우도 가는 건 어때?"

→ modifyTravelPlan() 호출
→ 수정 의도: SWAP_PLACES (장소 교체)

분석 결과: "우도는 반나절 이상 필요해서 일정 대폭 수정 필요"

응답: "우도는 왕복 페리와 섬 관광으로 최소 5시간이 필요해요.
      둘째 날 오전 일정을 모두 우도로 변경하시겠어요?
      
      [변경 제안]
      09:00 성산항 출발 (페리)
      09:30 우도 도착
      - 우도봉, 하고수동해수욕장, 검멸레
      13:00 우도 출발
      13:30 성산항 도착
      14:00 점심 후 오후 일정 진행
      
      확인 버튼 / 취소 버튼"
```

### 예시 11: 날씨 영향으로 일정 자동 조정
```
사용자: "내일 부산 해운대 가서 수영하고 싶어요"

[MainLLMOrchestrator 내부 처리]
- 날짜: 내일 (1주일 이내 ✓)
- 장소: 부산 해운대
- 활동: 수영 (야외 활동)

→ getWeatherInfo() 자동 호출
Function 호출: getWeatherInfo({
    "destination": "부산",
    "date": "2024-12-19",
    "includeForecast": true
})

날씨 API 응답:
- 기온: 5°C  
- 날씨: 맑음
- 체감온도: 2°C
- 파도높이: 1.5m

[날씨 영향도: MODERATE]
- 수영하기에는 너무 추움
- 대체 활동 필요

응답: "내일 해운대 날씨를 확인했어요! ☀️❄️
      
      맑은 날씨지만 기온이 5°C로 매우 춥습니다.
      바다 수영은 어려울 것 같아요.
      
      🏊 대신 이런 활동은 어떠세요?
      - 스파랜드 온천 (실내 수영장)
      - 해운대 해변 산책 & 사진
      - SEA LIFE 부산 아쿠아리움
      - 더베이101 야경 감상
      
      따뜻한 옷 꼭 챙기세요!"
```

---

## 💰 토큰 최적화 전략

### 토큰 사용량 비교

| 기능 | 기존 방식 | 시스템 프롬프트 방식 | 절감률 |
|-----|---------|---------|-------|
| 시스템 프롬프트 | 0 (매번 포함) | 500 토큰 (1회만) | - |
| 장소 분류 | 700 토큰/회 | 100 토큰/회 | 86% |
| 유사 장소 추천 | 600 토큰/회 | 50 토큰/회 | 92% |
| Follow-up 결정 | 800 토큰/회 | 200 토큰/회 | 75% |
| **세션당 총합** | 8,000-10,000 | 1,500-2,000 | 80% |

### 최적화 기법

#### 1. 시스템 프롬프트 활용
- **시스템 프롬프트**: 세션당 1회만 설정 (500 토큰)
- **사용자 프롬프트**: 각 요청마다 50-200 토큰만 사용
- **효과**: 
  - 반복되는 설명 제거
  - 일관된 동작 보장
  - 토큰 80% 절감

#### 2. 스마트 캐싱
```java
@Cacheable(value = "placeCategories", ttl = 3600)  // 1시간
@Cacheable(value = "placeRecommendations", ttl = 7200)  // 2시간
```
- 동일 질문은 캐시에서 즉시 반환
- Redis 활용으로 서버 간 공유
- 인기 장소는 warmup 시 미리 캐싱

#### 3. 배치 처리
- 여러 장소를 한 번의 LLM 호출로 처리
- 5개 장소 개별 호출 → 1회 배치 호출
- API 호출 80% 감소

#### 4. 모델 선택 최적화
- 간단한 분류: Gemini Flash (빠르고 저렴)
- 복잡한 추천: GPT-4o-mini (정확하고 경제적)
- 실시간 응답: 캐시 우선

### 비용 분석 (GPT-4o-mini 기준)

**최적화 전**:
- 세션당: 10,000 토큰
- 비용: $0.003 (약 4원)
- 월 10만 세션: $300 (약 40만원)

**최적화 후**:
- 세션당: 5,000 토큰 (정확성 유지)
- 비용: $0.0015 (약 2원)
- 월 10만 세션: $150 (약 20만원)

**절감액**: 월 20만원 (50% 절감)

### 품질 유지 전략

1. **핵심 기능은 LLM 유지**
   - 애매한 장소 분류
   - 복잡한 여행 계획
   - 개인화된 추천

2. **성능 모니터링**
   - 캐시 히트율 추적
   - LLM 호출 빈도 분석
   - 사용자 만족도 측정

3. **점진적 최적화**
   - 초기: 풀 LLM 사용
   - 데이터 수집 후 규칙 추가
   - 지속적 개선

---

# Part 4: 프로젝트 관리

## 6. 구현 계획

### 6.1 팀 구성 및 역할

**📅 개발 일정**: 1주일 (4인 팀, 5개 도메인)

### 6.2 개발 일정

#### **도메인 1: 메인 오케스트레이터 (CHAT2 팀 담당)**
**담당자**: CHAT2 Developer 1 (주말 완료 예정)
**책임**: LLM 중앙 통제 시스템
- [ ] MainLLMOrchestrator 구현
- [ ] FunctionCallingConfig 설정  
- [ ] UnifiedChatController 구현
- [ ] 시스템 프롬프트 최적화
- [ ] Function Calling 라우팅 로직
- [ ] 전체 시스템 통합 테스트

#### **도메인 2: Adaptive Follow-up (CHAT2 팀 담당)**
**담당자**: CHAT2 Developer 2
**책임**: 지능형 꼬리질문 시스템
- [ ] startAdaptiveFollowUp/continueAdaptiveFollowUp 도구 구현
- [ ] 사용자 상태 기반 질문 전략 결정 (피로도, 긴급도)
- [ ] 동적 질문 순서 및 세션 관리

#### **도메인 3: 여행 계획 생성 (TRIP 팀 담당)**
**담당자**: TRIP Developer
**책임**: 핵심 여행 계획 생성 및 필요 도구 구현
- [ ] generateTravelPlan 도구 구현 (메인 기능)
- [ ] 기존 Function 활용 또는 신규 구현:
  - [ ] searchTourAPI 활용/구현 (관광지 정보)
  - [ ] getWeatherInfo 활용/구현 (날씨 정보)
  - [ ] recommendTemplates 활용/구현 (템플릿 추천)
- [ ] 일정 최적화 알고리즘
- [ ] 여행 경로 계획 로직
- [ ] searchWithPerplexity 연동 (실시간 정보)

#### **도메인 4: 검색 및 데이터 수집 (TRIP 팀 담당)**
**담당자**: TRIP Developer
**책임**: 검색, 추천 도구 및 데이터 수집
- [ ] **여행지 데이터 수집 및 DB 구축**
  - [ ] Tour API 크롤링 배치 작업
  - [ ] LLM 파싱을 통한 여행지 정보 추출
  - [ ] 여행지 데이터 DB 스키마 설계
  - [ ] 데이터 정제 및 정규화
  - [ ] 주기적 업데이트 스케줄러
- [ ] **검색 및 추천 도구**
  - [ ] searchRegions 도구 구현
  - [ ] searchAttractions 도구 구현
  - [ ] 지역별 관광 정보 인덱싱
  - [ ] 추천 알고리즘 구현
  - [ ] 검색 결과 랭킹 로직

#### **도메인 5: 데이터베이스 및 대화 관리 (USER 팀 담당)**
**담당자**: USER Developer
**책임**: DB 관리, 대화 컨텍스트, 성능 최적화
- [ ] **ChatThread 관리**
  - [ ] ChatThread 생성/조회/업데이트
  - [ ] ChatMessage 저장 및 인덱싱
  - [ ] 대화 히스토리 쿼리 최적화
- [ ] **컨텍스트 관리**
  - [ ] getConversationContext 구현
  - [ ] updateConversationContext 구현
  - [ ] 세션 캐싱 전략
- [ ] **사용자 데이터**
  - [ ] saveUserPreference/queryUserTravelData 구현
  - [ ] saveTravelHistory/updateTravelPlanStatus 구현
  - [ ] analyzeUserTravelPatterns 구현
- [ ] **성능 최적화**
  - [ ] DB 인덱싱 전략
  - [ ] 쿼리 최적화
  - [ ] Redis 캐싱 구현


### 6.3 성공 지표

#### 통합 테스트 체크리스트
- [ ] 전체 Function Calling 플로우 테스트
- [ ] 도메인 간 데이터 연동 확인
- [ ] 에러 처리 및 폴백 메커니즘 검증
- [ ] 성능 및 응답 시간 측정
- [ ] 프론트엔드 연동 테스트

#### 성능 목표
- 모든 Function 도구 독립적 동작 확인
- LLM의 도구 선택 정확도 90% 이상
- 평균 응답 시간 3초 이내
- 전체 시나리오 테스트 통과율 95% 이상

---

## 7. 부록

### 7.1 장점 및 특징

#### 시스템 장점
1. **단순한 아키텍처**: 모든 요청이 하나의 진입점 통과
2. **유연한 확장**: 새 기능을 Function으로 추가만 하면 됨
3. **자연스러운 대화**: LLM이 상황에 맞게 도구 선택
4. **투명한 동작**: Function 호출 로그로 추적 가능
5. **유지보수 용이**: 각 기능이 독립적으로 구현됨

### 7.2 주의사항

#### 개발 시 주의사항
1. **Function 이름**: 명확하고 직관적인 이름 사용
2. **에러 처리**: 각 Function에서 적절한 에러 처리
3. **타임아웃**: Function 실행 시간 제한 설정
4. **로깅**: 모든 Function 호출 로깅
5. **테스트**: 각 Function 단위 테스트 필수

### 7.3 참고자료

- Spring AI Documentation
- OpenAI Function Calling Guide
- Tour API Documentation
- Weather API Documentation

---

**끝**

이 구조로 구현하면 메인 LLM이 모든 것을 통제하면서 필요할 때만 특정 기능을 호출하는 깔끔한 시스템이 완성됩니다.