# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with this repository.

## Project Overview

Compass는 Spring AI와 LLM을 활용한 AI 여행 계획 서비스입니다.
- **아키텍처**: MainLLMOrchestrator가 모든 요청을 Function Calling으로 처리
- **내 역할**: CHAT2 도메인 담당 - LLM 오케스트레이션과 정보 수집 플로우

## Essential Commands

### 빠른 실행
```bash
# Docker 실행 (필수)
docker-compose up -d

# 테스트 실행
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.16/libexec/openjdk.jdk/Contents/Home ./gradlew unitTest
```

## CHAT2 도메인 주요 책임 (10개 요구사항)

### 핵심 Function Calling 도구
```java
// REQ-CHAT2-001: MainLLMOrchestrator - 모든 요청의 중앙 처리
// REQ-CHAT2-003: analyzeUserInput - 사용자 입력에서 여행 정보 추출
// REQ-CHAT2-004: classifyIntent - Intent 분류 및 라우팅
// REQ-CHAT2-005~006: startFollowUp/continueFollowUp - Follow-up 질문 관리
// REQ-CHAT2-007: calculateProgress - 정보 수집 진행률 계산
// REQ-CHAT2-010: integrateProject - 프로젝트 관리 도구 연동
```

### 개발 우선순위
1. **Day 1**: MainLLMOrchestrator 구현, Function Calling 설정
2. **Day 2**: Follow-up 플로우 고도화, Intent Classification
3. **Day 3**: 통합 테스트, 다른 도메인과 연동 확인

## 핵심 서비스 파일
```
src/main/java/com/compass/
├── config/
│   ├── FunctionCallingConfig.java    # Function 도구 설정
│   └── SpringAIConfig.java           # LLM 클라이언트 설정
├── domain/
│   └── chat2/
│       ├── service/
│       │   ├── MainLLMOrchestrator.java        # 중앙 오케스트레이터
│       │   ├── TravelInfoCollectionService.java # 정보 수집
│       │   ├── FollowUpQuestionGenerator.java   # Follow-up 생성
│       │   └── IntentClassificationService.java # Intent 분류
│       └── controller/
│           └── UnifiedChatController.java       # 통합 API 엔드포인트
```

## 협업 규칙

### 도메인 경계
- **내가 수정 가능**: `com.compass.domain.chat2.*`
- **수정 금지**: 다른 도메인 코드 (chat1, media, trip, user)
- **공통 영역**: `com.compass.config.*` (신중하게 수정)

### Git 규칙
- Branch: `feat/CHAT2/기능명` (예: `feat/CHAT2/REQ-FOLLOW-통합`)
- Commit: `feat: [CHAT2] Follow-up 질문 생성 로직 구현`
- **중요**: Git 명령은 개발자가 직접 실행 (자동 커밋 금지)

## Function Calling 플로우

### 요청 처리 흐름
```
사용자 입력
    ↓
MainLLMOrchestrator (Gemini 2.0)
    ↓
Intent 분류 → Function 선택
    ↓
실행 및 응답
```

### 필수 정보 수집 체크리스트
- [ ] 출발지 (origin)
- [ ] 목적지 (destination)
- [ ] 날짜 (dates)
- [ ] 기간 (duration)
- [ ] 동행자 (companions)
- [ ] 예산 (budget)
- [ ] 여행 스타일 (travelStyle)

### API Endpoints
```
POST /api/chat/unified          # 통합 채팅 엔드포인트
GET  /api/chat/thread/{id}      # 대화 내역 조회
GET  /api/chat/collection-state # 수집 상태 확인
```

## 테스트 전략

### 단위 테스트 (필수)
```java
@Test
@Tag("unit")
void testFollowUpQuestionGeneration() {
    // REQ-CHAT2-005: Follow-up 질문 생성 테스트
}
```

### 통합 테스트 시나리오
1. 일반 대화 → 여행 계획으로 전환
2. 정보 수집 → Follow-up → 완료
3. Function Calling 정확도 검증

## 주의사항

- **Spring AI 사용**: 직접 API 호출 대신 Spring AI 추상화 활용
- **Function 명명**: 명확하고 직관적인 이름 (예: `analyzeUserInput`)
- **에러 처리**: 각 Function에서 적절한 fallback 구현
- **Docker 필수**: 백엔드는 반드시 Docker로 실행

