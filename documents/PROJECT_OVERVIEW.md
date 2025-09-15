# 🎯 LLM 중앙 통제 기반 여행 계획 추천 시스템 - 프로젝트 개요

## 📋 목차

1. [Spring AI 소개 및 핵심 개념](#1-spring-ai-소개-및-핵심-개념)
2. [시스템 개요](#2-시스템-개요)
3. [아키텍처](#3-아키텍처)
4. [Function Calling 도구](#4-function-calling-도구)
5. [핵심 구현](#5-핵심-구현)
6. [대화 플로우](#6-대화-플로우)
7. [기술 스택](#7-기술-스택)

---

## 1. Spring AI 소개 및 핵심 개념

### 1.1 Spring AI란?
Spring AI는 스프링 생태계에서 **AI/LLM을 쉽게 통합**할 수 있도록 제공하는 공식 프레임워크입니다.
- OpenAI, Gemini, Claude 등 다양한 LLM을 **통일된 인터페이스**로 사용
- Spring의 의존성 주입(DI)과 자동 구성(Auto-configuration) 활용
- 프로덕션 레벨의 AI 애플리케이션 개발 지원

### 1.2 우리 프로젝트에서 Spring AI를 사용하는 이유

#### 일반 API 호출의 문제점
```java
// [예시] ❌ 일반 API 호출 방식 - 복잡하고 반복적
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
// [예시] ✅ Spring AI 방식 - 간단하고 강력
@Autowired
private ChatModel chatModel;

public String askQuestion(String question) {
    return chatModel.call(question);  // 한 줄로 끝!
}
```

### 1.3 Function Calling이란?

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

---

## 2. 시스템 개요

### 2.1 목적 및 비전
메인 LLM이 중앙 오케스트레이터 역할을 하며, 모든 기능을 Function Calling으로 호출하는 지능형 여행 계획 시스템

**핵심 철학**:
- **자유로운 대화**: MainLLMOrchestrator가 어떤 질문이든 자연스럽게 처리
- **자연스러운 유도**: 강요하지 않고 대화를 여행 계획으로 부드럽게 이끌기
- **예약 정보 자동 통합**: OCR로 항공권/호텔 정보 자동 반영
- **효율적 정보 수집**: 빠른 입력으로 불필요한 대화 턴 최소화 (토큰 절약)

### 2.2 핵심 특징
✅ **단일 진입점**: 모든 사용자 요청이 메인 LLM을 통해 처리  
✅ **도구 기반 아키텍처**: 모든 기능이 독립적인 Function으로 구현  
✅ **동적 의사결정**: LLM이 상황에 따라 필요한 도구 선택  
✅ **확장 가능**: 새로운 기능을 Function으로 추가하기 쉬움
✅ **비용 효율**: 토큰 사용량 최적화로 운영 비용 절감

---

## 3. 아키텍처

### 3.1 시스템 아키텍처
```
┌─────────────────────────────────────────────────────┐
│                     사용자 인터페이스                    │
└──────────────────────┬──────────────────────────────┘
                       ↓
┌─────────────────────────────────────────────────────┐
│            MainLLMOrchestrator (Gemini 2.0)          │
│                  중앙 오케스트레이터                    │
└──────────────────────┬──────────────────────────────┘
                       ↓
         Function Calling으로 필요한 도구 호출
                       ↓
    ┌──────────────────┼──────────────────┐
    ↓                  ↓                  ↓
┌────────────┐ ┌────────────┐ ┌────────────┐
│ 정보 수집   │ │ 장소 검색   │ │ 계획 생성   │
│ Functions  │ │ Functions  │ │ Functions  │
└────────────┘ └────────────┘ └────────────┘
```

### 3.2 Function Calling 구조
```
사용자 입력 → Gemini 2.0 Flash (오케스트레이터)
                    ↓
            자동으로 필요한 Function 선택
                    ↓
    ┌──────────────┼──────────────┐
    ↓              ↓              ↓
여행정보수집    장소검색      계획생성
(정보 수집)   (Search)    (Generate)
```

### 3.3 케이스별 데이터 흐름

#### Case 1: 여행 계획 준비 단계 (정보 수집)
- 빠른 입력 폼을 통한 기본 정보 수집
- OCR로 항공권/호텔 예약 정보 자동 추출
- 예약 정보에서 날짜, 목적지 자동 채워지기
- 부족한 필수 정보만 간단한 Follow-up으로 보완
- 목적지 미정 사용자를 위한 추천 시스템

#### Case 2: 여행 계획 생성 단계
- 수집된 정보 기반 여행 계획 생성
- **기존 예약 정보 고려**:
  - 항공 도착/출발 시간에 맞춰 일정 조정
  - 호텔 체크인/아웃 시간 고려
  - 행사 시간을 필수 일정으로 포함
- 하이브리드 장소 검색 (RDS + Perplexity)
- 날씨 정보 자동 반영

#### Case 3: 여행 계획 수정
- 생성된 계획의 대화형 수정
- 사용자 피드백 반영

---

## 4. Function Calling 도구

### 4.1 정보 수집 도구 (빠른 입력 & OCR & 간단한 Follow-up)

#### 방식 1: 빠른 입력 인터페이스 (주요 방식)
- 한 번에 여행 정보를 입력할 수 있는 구조화된 인터페이스
- 목적지, 날짜, 인원, 예산 등 필수 정보 수집
- **예약서 이미지 업로드 기능** (선택)
- 목적지 미정 사용자를 위한 거리/시간 기반 추천

#### 방식 2: OCR 기반 자동 정보 추출
- S3에 이미지 업로드 → Gemini Vision OCR
- 이미지 유형 분류 (항공권/호텔/티켓)
- 규격화된 예약 정보 추출 및 DB 저장
- 여행 날짜, 목적지 자동 채워지기

#### 방식 3: 간단한 Follow-up (보조용)
- 빠른 입력과 OCR 후에도 부족한 필수 정보만 1-2개 질문으로 보완
- 복잡한 대화 없이 간단히 처리

### 4.2 검색 및 추천 도구

#### searchWithPerplexity ⚠️ 비용 주의
- 실시간 트렌드 및 최신 정보 검색
- 프로덕션 환경: 일 10회 제한/사용자

#### searchPlacesFromDB
- PostgreSQL RDS 기반 장소 검색
- Tour API 크롤링 데이터 활용

### 4.3 여행 계획 도구

#### generateTravelPlan
- 모든 정보를 종합한 여행 계획 생성
- **기존 예약 정보 자동 반영**:
  - 항공 시간에 맞춰 첫날/마지막날 일정 조정
  - 호텔 체크인/아웃 시간 고려
  - 행사 시간을 필수 일정으로 포함
- 하이브리드 장소 선택 (DB + Perplexity)

#### modifyTravelPlan
- 생성된 계획의 유연한 수정

### 4.4 외부 API 연동 도구

#### getWeatherInfo
- OpenWeatherMap API 연동
- 1주일 이내 여행 시 자동 날씨 확인

#### OCR 및 예약 정보 추출 ⚠️ 비용 주의
- **uploadToS3AndOCR**: S3 업로드 + Gemini Vision OCR
- **classifyImageType**: 이미지 유형 분류
- **extractFlightInfo**: 항공권 정보 추출
- **extractHotelInfo**: 호텔 바우처 정보 추출
- **extractEventInfo**: 공연/행사 티켓 정보 추출
- 프로덕션 환경: 일 10회 제한/사용자

---

## 5. 핵심 구현

### 5.1 MainLLMOrchestrator
```java
// [예시] 메인 오케스트레이터 구현
@Service
public class MainLLMOrchestrator {
    
    @Autowired
    private ChatModel chatModel;
    
    public String processUserInput(String input, String threadId) {
        // 시스템 프롬프트 설정 (최소화)
        String systemPrompt = """
            당신은 여행 계획을 도와주는 AI 어시스턴트입니다.
            사용자와 자연스러운 대화를 하며 여행 계획을 수립합니다.
            필요한 경우 제공된 Function을 호출하세요.
            """;
        
        // Function Calling 자동 처리
        ChatResponse response = chatModel.call(
            new ChatRequest(input)
                .withSystemPrompt(systemPrompt)
                .withFunctions(getAllFunctions())
        );
        
        return response.getContent();
    }
}
```

### 5.2 프롬프트 엔지니어링
- **시스템 프롬프트**: 50토큰 이내로 최소화
- **사용자 프롬프트**: 구체적이고 간결하게
- **응답 형식**: JSON으로 구조화

---

## 6. 대화 플로우

### 6.1 기본 대화 플로우
1. 사용자가 아무 질문이나 입력
2. MainLLMOrchestrator가 의도 파악
3. 여행 관련 시 자연스럽게 유도
4. 빠른 입력 폼 제시 또는 대화 계속

### 6.2 간단한 Follow-up 플로우
- 빠른 입력 후 부족한 필수 정보만 채우기
- 1-2회 질문으로 완료
- 목적지, 날짜, 인원, 예산만 확인

### 6.3 여행 계획 생성 플로우
1. 정보 수집 완료 확인
2. DB 우선 검색
3. 필요시 Perplexity 활용
4. 날씨 정보 자동 확인
5. 최종 계획 생성

---

## 7. 기술 스택

### 7.1 Backend
- **Framework**: Spring Boot 3.x, Java 17
- **AI/ML**: Spring AI 1.0.0-M5
- **LLM**: Gemini 2.0 Flash (메인), GPT-4o-mini (보조)
- **Database**: PostgreSQL (AWS RDS)
- **Cache**: 세션 관리는 DB로 (Redis 제외)

### 7.2 External APIs
- **Perplexity**: 실시간 트렌드 검색 (비용 주의)
- **Google Vision**: OCR 처리 (비용 주의)
- **OpenWeatherMap**: 날씨 정보
- **Tour API**: 관광지 정보

### 7.3 API 사용 제한 정책

#### 💰 비용 관리 (중요)
**비용 발생 주요 서비스**:
- **Gemini 2.0 Flash**: 기본 대화 처리 (저비용, 제한 없음)
- **Perplexity API**: 실시간 트렌드 검색 (고비용)
- **Google Vision OCR**: 예약서 텍스트 추출 (중간 비용)

**환경별 사용 제한**:
- **개발 환경**: 제한 없음 (자유로운 테스트)
- **프로덕션 환경**: 
  - Perplexity: 10회/일/사용자
  - OCR: 10회/일/사용자

**비용 절감 전략**:
1. DB 우선 검색 (Perplexity 전에 RDS 조회)
2. 캐싱 활용
3. 빠른 입력으로 대화 턴 최소화
4. 선택적 OCR 사용

---

## 주의사항

1. **토큰 효율성**: 시스템 프롬프트 최소화, 간결한 응답
2. **API 비용**: Perplexity와 OCR은 제한적 사용
3. **DB 우선**: 외부 API 전에 항상 DB 검색
4. **자연스러운 대화**: 강요하지 않고 유도
5. **코드 예시**: 모든 코드는 이해를 돕기 위한 [예시]

---

**이 문서는 프로젝트의 전반적인 개요와 기술적 접근 방법을 설명합니다.**
**구체적인 요구사항과 역할 분담은 별도 문서(REQUIREMENTS_AND_ROLES.md)를 참조하세요.**