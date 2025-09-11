# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with this repository.

## Project Overview

Compass is an AI-powered personalized travel planning service built with Spring Boot and Spring AI. The backend provides REST APIs for authentication, chat functionality, and personalized travel recommendations using LLM integration.

## Essential Commands

### Docker 실행 (필수)
```bash
# ⚠️ 백엔드는 반드시 Docker로 실행
docker-compose up -d

# 로그 확인
docker-compose logs -f app

# 중지
docker-compose down
```

### 테스트 실행
```bash
# Unit tests (Redis 불필요)
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.16/libexec/openjdk.jdk/Contents/Home ./gradlew unitTest

# 전체 테스트
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.16/libexec/openjdk.jdk/Contents/Home ./gradlew test

# 빌드 (테스트 제외)
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.16/libexec/openjdk.jdk/Contents/Home ./gradlew clean build -x test
```

### Frontend 실행
```bash
cd /Users/kmj/Documents/GitHub/AIBE2_FinalProject_Compass_BE/FE
npm install
npm start  # http://localhost:3000
```

## Architecture Overview

### Domain Structure
프로젝트는 5개 도메인으로 구성되어 있으며, 각 팀이 독립적으로 개발합니다:

1. **USER Domain** - 인증/인가, 프로필 관리
2. **CHAT1 Domain** - 기본 채팅 기능
3. **CHAT2 Domain** - LLM 통합, Function Calling, Follow-up 질문 ⭐ **(현재 담당)**
4. **MEDIA Domain** - 이미지 처리, OCR
5. **TRIP Domain** - 여행 계획, RAG 추천

**⚠️ 중요**: 
- 현재 개발자는 **CHAT2 팀** 담당입니다
- 다른 도메인 코드는 가능한 수정하지 않습니다
- CHAT2 책임 영역: LLM 통합, Function Calling, Follow-up 시스템

### Technology Stack
- **Framework**: Spring Boot 3.x, Java 17
- **AI/ML**: Spring AI 1.0.0-M5 (Gemini 2.0 Flash, GPT-4o-mini)
- **Database**: AWS RDS PostgreSQL, Redis 7
- **Security**: JWT 인증
- **Deployment**: Docker, AWS

## Configuration

### 환경 변수
```bash
# 필수 파일 (프로젝트 루트에 위치)
.env  # API 키 및 설정값
/Users/kmj/Documents/GitHub/AIBE2_FinalProject_Compass_BE/travelagent-468611-1ae0c9d4e187.json  # Google Cloud 인증
```

**주의**: `.env` 파일은 절대 커밋하지 마세요 (`.gitignore`에 포함됨)

### Spring Profiles
- **docker-rds**: Docker 컨테이너 + AWS RDS (권장)
- **test**: 테스트 환경
- **test-no-redis**: Redis 없는 단위 테스트

## Development Guidelines

### Branch Strategy
- Main: `main`
- Feature: `feature/domain-feature` (예: `feature/chat2-followup`)
- Fix: `fix/domain-issue`

### Commit Convention
- `feat:` 새 기능
- `fix:` 버그 수정
- `refactor:` 리팩토링
- `test:` 테스트
- `docs:` 문서

### Testing
- 모든 테스트에 `@Tag("unit")` 또는 `@Tag("integration")` 추가
- 최소 커버리지: 80%
- 테스트 위치: `src/test/java/com/compass/domain/[domain]/`

## Clean Code Guidelines

### 읽기 쉬운 코드
- 명확한 변수/메서드명 사용
- 메서드는 20줄 이내
- 한 메서드는 한 가지 일만
- 중첩 깊이 최대 3레벨

### 구조화
- 일관된 코드 포맷팅
- 관련 기능은 함께 그룹화
- 논리적 섹션 간 빈 줄 추가

### 의존성 관리
- 의존성 주입 일관되게 사용
- 순환 의존성 방지
- 인터페이스로 추상화

### 에러 처리
- 구체적인 예외 사용
- 의미 있는 에러 메시지
- Optional 활용

## CHAT2 Domain Workflow

### 여행 정보 수집 플로우
모든 사용자 입력은 여행 계획 요청으로 처리됩니다:

1. **사용자 입력** → LLM이 정보 추출
2. **필수 정보 체크** (순차적):
   - 출발지 (origin)
   - 목적지 (destination)
   - 날짜 (dates)
   - 기간 (duration)
   - 동행자 (companions)
   - 예산 (budget)
   - 여행 스타일 (travelStyle)

3. **누락 정보 수집** → Follow-up 질문 생성
4. **정보 완료** → 여행 계획 생성

### 핵심 서비스
- **ChatServiceImpl**: 메인 채팅 서비스
- **TravelInfoCollectionService**: 정보 수집 관리
- **FollowUpQuestionGenerator**: 후속 질문 생성
- **TravelQuestionFlowEngine**: 플로우 제어
- **NaturalLanguageParsingService**: 사용자 입력 파싱

### API Endpoints
- POST `/api/chat/follow-up/question` - Follow-up 질문 생성
- POST `/api/chat/follow-up/response` - 사용자 응답 처리
- GET `/api/chat/follow-up/state/{threadId}` - 수집 상태 조회

## Important Notes

1. **Git 작업 금지**: 모든 git 작업은 개발자가 직접 수행
2. **Docker 필수**: 백엔드는 반드시 Docker로 실행
3. **CHAT2 팀 역할**:
   - LLM 통합 (Gemini, GPT-4)
   - Function Calling 구현
   - Follow-up 질문 시스템
   - 여행 정보 수집 플로우
4. **Spring AI 사용**: 직접 API 호출 대신 Spring AI 추상화 사용