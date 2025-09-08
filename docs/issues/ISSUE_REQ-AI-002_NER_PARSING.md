---
name: 기능 개발
about: 기능명세서 기반 개발 작업을 위한 이슈 템플릿
title: '[CHAT] REQ-AI-002 | NER 기반 자연어 여행 정보 추출 기능'
labels: '백엔드'
assignees: 'CHAT2'
---

## 📋 기능 개요
**요구사항 ID**: REQ-AI-002

사용자의 자연어 입력에서 여행 관련 정보를 자동으로 추출하고 구조화하는 NER(Named Entity Recognition) 기반 파싱 시스템을 구현했습니다. 하이브리드 접근법(Pattern Matching + AI Fallback)을 통해 높은 성능과 신뢰성을 달성했습니다.

## 🎯 개발 목표
- Pattern Matching을 통한 빠른 정보 추출 (<200ms)
- AI Fallback으로 복잡한 케이스 처리
- 날짜, 예산, 인원, 목적지 등 핵심 엔티티 자동 추출
- 85% 이상의 패턴 매칭 성공률 달성

## 📝 기능 명세
### API Endpoints
- [x] `POST /api/chat/parse` - 채팅 메시지 파싱 및 여행 계획 생성
- [x] `POST /api/chat/parse/raw` - 파싱 결과만 반환
- [x] `GET /api/chat/parse/examples` - 예제 파싱 결과 조회

### 요청/응답 형식
```json
// Request - POST /api/chat/parse
{
  "text": "다음달 15일부터 3박4일로 제주도 여행을 가려고 해요. 2명이서 가는데 예산은 1인당 100만원입니다."
}

// Response
{
  "planId": "CHAT_1234567890",
  "destination": "제주",
  "origin": "서울",
  "startDate": "2024-04-15",
  "endDate": "2024-04-18",
  "numberOfTravelers": 2,
  "travelStyle": "moderate",
  "interests": ["nature"],
  "budget": {
    "perPerson": 1000000,
    "total": 2000000,
    "currency": "KRW"
  },
  "summary": "2명이서 04월 15일부터 04월 18일까지 제주 여행 (예산: 1인당 100만원)",
  "metadata": {
    "parsedAt": "2024-03-10",
    "parsingMethod": "NER_PATTERN_MATCHING",
    "confidence": 0.95
  }
}
```

## 🔧 구현 사항
### Entity
- N/A (파싱 기능은 엔티티 없이 동작)

### Repository
- N/A (데이터베이스 저장 없이 메모리에서 처리)

### Service
- [x] ChatInputParser 클래스 생성
- [x] 하이브리드 파싱 로직 구현 (Pattern Matching + AI Fallback)
- [x] 날짜 정규화 및 변환 로직
- [x] 예산 정규화 및 환율 변환
- [x] 신뢰도 점수 계산 로직

### Controller
- [x] ChatParsingController 클래스 생성
- [x] 파싱 엔드포인트 구현
- [x] 입력 검증 (@Valid, @Size)
- [x] Swagger 문서화

### Parser Implementation
- [x] **날짜 파싱**: 절대/상대 날짜, 기간 표현 지원
- [x] **예산 추출**: 원화/달러, 자동 KRW 변환
- [x] **인원 파싱**: 숫자/설명적 표현 지원
- [x] **목적지 추출**: 한국 주요 도시 인식
- [x] **관심사 분류**: 문화, 음식, 자연, 쇼핑, 모험
- [x] **스타일 분류**: budget, moderate, luxury

### Parsing Features
지원하는 패턴들:
- [x] 절대 날짜: `2024년 3월 25일`, `2024-03-25`, `3월 25일`
- [x] 상대 날짜: `오늘`, `내일`, `다음주`, `다음달`
- [x] 기간 표현: `3박4일`, `2박3일`
- [x] 원화 예산: `100만원`, `50만원`
- [x] 달러 예산: `1000달러`, `1000USD`
- [x] 인원 표현: `2명`, `4인`, `혼자`, `커플`, `가족`

### Testing
- [x] 단위 테스트 작성 (ChatInputParserTest.java - 13 test cases)
- [x] 통합 테스트 작성 (ChatParsingControllerIntegrationTest.java - 7 test cases)
- [x] Edge case 처리 테스트
- [x] Validation 에러 테스트
- [x] Service 실패 케이스 테스트

## 📊 데이터베이스 스키마
NER 파싱은 데이터베이스 변경사항 없음 (메모리 내 처리)

## 🔗 관련 이슈
- 관련 요구사항: REQ-AI-002 (NER 기반 자연어 여행 정보 추출)
- 연관 요구사항: REQ-LLM-002 (Gemini API 연동)
- 후속 작업: REQ-LLM-005 (Function Calling 설정)

## ✅ 완료 조건
- [x] 모든 파싱 엔드포인트 구현 완료
- [x] 10개 이상의 날짜 패턴 지원
- [x] 예산 정규화 및 환율 변환 구현
- [x] 테스트 커버리지 90% 이상
- [x] API 문서화 완료 (CHAT_PARSING_API.md)
- [x] Pattern Matching 성공률 85% 이상
- [x] 처리 시간 200ms 이하 (Pattern Matching)

## 📌 참고사항

### 기술 스펙
- Spring Boot 3.3.13
- Spring AI 1.0.0-M5
- Java 17
- Lombok
- Jackson

### 성능 메트릭
- **Pattern Matching 성공률**: ~85% for common inputs
- **AI Fallback 사용률**: ~15% for complex cases
- **평균 처리 시간**: <200ms (Pattern Matching), <1s (with AI)
- **지원 날짜 포맷**: 10+ variations
- **언어 지원**: Korean (primary), partial English

### 주요 구현 내용
1. **ChatInputParser.java**: 하이브리드 파싱 엔진 구현
2. **ChatParsingController.java**: REST API 엔드포인트 제공
3. **TripPlanningRequest.java**: 파싱 결과를 담는 DTO
4. **신뢰도 계산**: 8개 항목 기반 0-1 스케일 점수

### 파싱 전략
```
Phase 1: Pattern-Based Extraction (Primary)
- Regular expressions로 구조화된 데이터 추출
- 빠른 처리 (<200ms)
- 예측 가능한 결과
- 외부 API 의존성 없음

Phase 2: AI Enhancement (Fallback)
- Spring AI 통합으로 복잡한 케이스 처리
- 모호하거나 비구조화된 입력 처리
- 컨텍스트 기반 해석
- ChatModel (Gemini/GPT-4) 사용
```

### 설계 결정 사항
1. **하이브리드 접근법 선택 이유**:
   - Pattern matching이 LLM 호출보다 5배 빠름
   - API 호출을 85% 감소시켜 비용 절감
   - Pattern matching의 예측 가능한 동작
   - AI가 pattern matching이 놓친 엣지 케이스 처리

2. **도메인 배치**: CHAT 도메인으로 구현 (CHAT2 담당자 책임 영역)
3. **Regex First**: 결정론적 패턴 매칭 우선
4. **AI Fallback**: 패턴 실패시에만 LLM 사용
5. **Default Values**: 누락된 데이터에 합리적인 기본값 적용

### 알려진 제한사항
- 연도 미지정시 현재 연도로 기본 설정
- 국제 목적지는 현재 한국 도시로 제한
- 한국어 입력에 최적화됨
- USD 환율은 1:1300으로 고정

### 향후 개선 계획
- 다국어 지원 (영어, 중국어, 일본어)
- 국제 목적지 인식
- 동적 환율 변환
- 계절별 활동 추천
- 그룹 여행 선호도 감지

### 문서화
- [CHAT_PARSING_API.md](/docs/CHAT_PARSING_API.md) - API 상세 문서
- [TEAM_DEVELOPMENT_PLAN.md](/docs/TEAM_DEVELOPMENT_PLAN.md) - 개발 계획
- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/) - 참고 문서

---

**완료 일자**: 2024-03-10  
**리뷰 상태**: PR 준비 완료  
**개발자**: @kmj (CHAT2 팀)