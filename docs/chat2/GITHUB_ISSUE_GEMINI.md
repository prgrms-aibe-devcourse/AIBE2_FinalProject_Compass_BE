---
name: 기능 개발
about: 기능명세서 기반 개발 작업을 위한 이슈 템플릿
title: '[CHAT] REQ-LLM-002 | Vertex AI Gemini 연동'
labels: '백엔드'
assignees: 'CHAT2팀'
---

## 📋 기능 개요
**요구사항 ID**: REQ-LLM-002

Spring AI를 통한 Google Cloud Vertex AI Gemini 2.0 Flash 모델 연동 구현. 
채팅 도메인에서 사용할 기본 LLM 서비스로 Gemini를 설정하고, 자연어 처리 기능을 제공합니다.

## 🎯 개발 목표
- Spring AI 프레임워크를 통한 Gemini 2.0 Flash 모델 통합
- ChatModelService 인터페이스 구현체 제공
- 시스템 프롬프트 지원 및 대화 생성 기능 구현
- 테스트 엔드포인트를 통한 연동 검증

## 📝 기능 명세
### API Endpoints
- [x] `POST /api/test/gemini` - Gemini 모델 테스트 엔드포인트
- [x] `GET /api/test/config` - AI 설정 상태 확인

### 요청/응답 형식
```json
// Request - POST /api/test/gemini
{
  "prompt": "안녕하세요, 간단한 자기소개를 해주세요."
}

// Response
{
  "model": "Vertex AI Gemini 2.0 Flash",
  "prompt": "안녕하세요, 간단한 자기소개를 해주세요.",
  "response": "안녕하세요! 저는 구글에서 개발한 대규모 언어 모델 Gemini입니다...",
  "status": "success"
}
```

## 🔧 구현 사항
### Entity
- [x] 해당사항 없음 (LLM 서비스는 별도 Entity 불필요)

### Repository
- [x] 해당사항 없음 (외부 API 연동)

### Service
- [x] ChatModelService 인터페이스 생성
- [x] GeminiChatService 구현체 생성
- [x] 단순 메시지 응답 생성 메서드 구현
- [x] 시스템 프롬프트 포함 응답 생성 메서드 구현

### Controller
- [x] TestController 클래스 생성
- [x] `/api/test/gemini` 엔드포인트 구현
- [x] `/api/test/config` 엔드포인트 구현
- [x] 요청/응답 처리 로직 구현

### Configuration
- [x] AiConfig 클래스 생성 (Spring AI 설정)
- [x] ModelSelector 내부 클래스 구현
- [x] application.yml에 Vertex AI 설정 추가
- [x] 환경변수 설정 (GOOGLE_CLOUD_PROJECT_ID, GOOGLE_CLOUD_LOCATION, GOOGLE_APPLICATION_CREDENTIALS)

### Testing
- [x] GeminiChatServiceTest 단위 테스트 작성 (6개 테스트)
- [x] RealApiIntegrationTest 통합 테스트 작성
- [x] 실제 API 호출 테스트 검증

## 🗄️ 데이터베이스 스키마
```sql
-- 해당사항 없음 (LLM 서비스는 DB 스키마 변경 불필요)
```

## 🔗 관련 이슈
- Related to #CHAT-Domain-Development
- 관련 요구사항: REQ-LLM-002 (TEAM_REQUIREMENTS.md Line 179)
- 관련 문서: /docs/GEMINI_SETUP.md

## ✅ 완료 조건
- [x] 모든 API 엔드포인트 구현 완료
- [x] 테스트 코드 작성 및 통과
- [x] API 문서 업데이트 (GEMINI_SETUP.md)
- [x] 코드 리뷰 완료
- [x] DATABASE_ERD.md 업데이트 (DB 변경 없음)

## 📌 참고사항

### 기술 스펙
- **프레임워크**: Spring AI 1.0.0-M5
- **LLM 모델**: Gemini 2.0 Flash
- **클라우드**: Google Cloud Vertex AI
- **의존성**: spring-ai-vertex-ai-gemini-spring-boot-starter

### 환경변수 설정 필요
```bash
# Google Cloud Vertex AI 설정
GOOGLE_CLOUD_PROJECT_ID=your-project-id
GOOGLE_CLOUD_LOCATION=us-central1  # 또는 asia-northeast3
GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account-key.json
```

### 주의사항
1. **인증 설정**: Google Cloud 서비스 계정 키 파일이 필요하며, 절대 Git에 커밋하지 않도록 주의
2. **리전 선택**: us-central1이 기본이나, 낮은 지연시간을 위해 asia-northeast3 사용 가능
3. **API 사용량**: Vertex AI는 사용량 기반 과금이므로 개발/테스트 시 사용량 모니터링 필요
4. **모델 선택**: 
   - 일반 채팅: gemini-2.0-flash (빠른 응답)
   - 복잡한 작업: gemini-2.0-pro 변경 가능

### 테스트 방법
```bash
# 1. 환경 변수 설정
source .env

# 2. 애플리케이션 실행
./gradlew bootRun

# 3. API 테스트
curl -X POST http://localhost:8080/api/test/gemini \
  -H "Content-Type: application/json" \
  -d '{"prompt": "안녕하세요"}'

# 4. 통합 테스트 실행
./gradlew test --tests "RealApiIntegrationTest"
```

### 구현 완료 현황
✅ Spring AI를 통한 Vertex AI Gemini 통합 완료
✅ GeminiChatService 구현 및 테스트 완료
✅ 실제 API 호출 검증 완료 (2025-09-02)
✅ 설정 가이드 문서 작성 완료