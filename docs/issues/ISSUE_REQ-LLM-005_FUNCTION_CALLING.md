---
name: 기능 개발
about: 기능명세서 기반 개발 작업을 위한 이슈 템플릿
title: '[CHAT] REQ-LLM-005 | Function Calling 설정'
labels: '백엔드'
assignees: 'CHAT2'
---

## 📋 기능 개요
**요구사항 ID**: REQ-LLM-005

Spring AI Function 프레임워크를 사용하여 LLM이 외부 함수를 호출할 수 있는 Function Calling 기능을 구현했습니다. 여행 계획과 관련된 9개의 전문 함수를 추가하여 AI가 실시간 정보를 제공할 수 있도록 했습니다.

## 🎯 개발 목표
- Spring AI Function 프레임워크 설정 및 통합
- LLM(Gemini 2.0 Flash, GPT-4o-mini)에 함수 스키마 전달
- 자동 호출 체인 구성으로 AI가 필요시 자동으로 함수 호출
- 여행 관련 다양한 정보 제공 함수 구현

## 📝 기능 명세
### API Endpoints
- [x] `POST /api/chat/function-calling` - Function Calling을 사용한 채팅
- [x] `GET /api/chat/functions` - 사용 가능한 함수 목록 조회

### 요청/응답 형식
```json
// Request - POST /api/chat/function-calling
{
  "message": "서울에서 제주까지 항공편을 검색해줘",
  "threadId": "thread-123",
  "model": "gemini" // or "openai"
}

// Response
{
  "message": "서울에서 제주까지 다음과 같은 항공편을 찾았습니다...",
  "functionsCalled": ["searchFlights"],
  "data": {
    "flights": [...]
  }
}
```

## 🔧 구현 사항
### Entity
- N/A (Function Calling은 엔티티 없이 동작)

### Repository
- N/A (외부 API 호출 Mock 구현)

### Service
- [x] FunctionCallingChatService 클래스 생성
- [x] OpenAI/Gemini 모델별 Function Calling 로직 구현
- [x] 함수 실행 결과를 LLM 응답에 통합

### Controller
- [x] FunctionCallingController 클래스 생성
- [x] Function Calling 엔드포인트 구현
- [x] 함수 목록 조회 엔드포인트 구현

### Configuration
- [x] FunctionCallingConfiguration 클래스 생성
- [x] 9개 FunctionCallback Bean 등록
- [x] application.yml에 Gemini 2.0 Flash 모델 설정
- [x] 환경변수 설정 (OPENAI_API_KEY, VERTEX_AI_PROJECT_ID)

### Function Implementation
- [x] **searchFlights**: 항공편 검색 함수
- [x] **searchHotels**: 호텔 검색 함수
- [x] **getWeather**: 날씨 정보 조회 함수
- [x] **searchAttractions**: 관광지 검색 함수
- [x] **searchCafes**: 카페 검색 함수
- [x] **searchRestaurants**: 레스토랑 검색 함수
- [x] **searchLeisureActivities**: 레저 활동 검색 함수
- [x] **searchCulturalExperiences**: 문화 체험 검색 함수
- [x] **searchExhibitions**: 전시회 검색 함수

### Request/Response Models
각 함수별 Request/Response DTO 생성:
- [x] FlightSearchRequest/Response
- [x] HotelSearchRequest/Response
- [x] WeatherRequest/Response
- [x] AttractionSearchRequest/Response
- [x] CafeSearchRequest/Response
- [x] RestaurantSearchRequest/Response
- [x] LeisureActivityRequest/Response
- [x] CulturalExperienceRequest/Response
- [x] ExhibitionSearchRequest/Response

### Testing
- [x] 단위 테스트 작성 (FunctionCallingUnitTest.java)
- [x] 확장 함수 단위 테스트 작성 (ExtendedFunctionCallingUnitTest.java)
- [x] 모든 9개 함수에 대한 테스트 케이스 작성
- [x] Request 모델 기본값 테스트

## 📊 데이터베이스 스키마
Function Calling은 데이터베이스 변경사항 없음 (외부 API Mock 구현)

## 🔗 관련 이슈
- 관련 요구사항: REQ-LLM-005 (Function Calling 설정)
- 연관 요구사항: REQ-LLM-001 (Gemini API 연동)

## ✅ 완료 조건
- [x] 모든 Function Calling 엔드포인트 구현 완료
- [x] 9개 여행 관련 함수 구현 완료
- [x] 테스트 코드 작성 및 통과
- [x] Spring AI Function 프레임워크 통합
- [x] Gemini 2.0 Flash 모델 설정
- [x] FunctionCallbackWrapper를 통한 함수 등록

## 📌 참고사항

### 기술 스펙
- Spring AI 1.0.0-M5
- Gemini 2.0 Flash (정확한 모델명: "gemini-2.0-flash")
- GPT-4o-mini
- Spring Boot 3.x
- Java 17

### 환경변수 설정 필요
```bash
# OpenAI API 설정
OPENAI_API_KEY=your_openai_api_key

# Google Vertex AI 설정
VERTEX_AI_PROJECT_ID=your_project_id
VERTEX_AI_LOCATION=us-central1
```

### 주요 구현 내용
1. **TravelFunctions.java**: 9개의 여행 관련 함수를 @Component로 구현
2. **FunctionCallingConfiguration.java**: 각 함수를 FunctionCallback으로 래핑하여 Spring AI에 등록
3. **FunctionCallingChatService.java**: OpenAI/Gemini 모델별 Function Calling 처리 로직
4. **단위 테스트**: 모든 함수의 동작을 검증하는 테스트 케이스 작성

### 함수별 특징
- **검색 함수들**: 위치, 날짜, 필터 옵션을 받아 관련 정보 반환
- **날씨 함수**: 현재 날씨 또는 예보 정보 제공
- **문화/레저 함수**: 체험 활동, 전시회 등 문화 관련 정보 제공
- 모든 함수는 Mock 데이터를 반환하도록 구현 (실제 API 연동은 추후 구현)

### 주의사항
- Gemini 모델 이름은 반드시 "gemini-2.0-flash" 사용 (exp 버전 아님)
- Spring AI dependencies는 build.gradle에서 주석 해제 필요
- Function Calling은 LLM이 자동으로 필요한 함수를 판단하여 호출
- 각 함수는 @JsonClassDescription과 @JsonPropertyDescription으로 LLM이 이해할 수 있도록 설명 추가