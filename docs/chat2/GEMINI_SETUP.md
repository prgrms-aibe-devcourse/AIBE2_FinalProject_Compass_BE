# Vertex AI Gemini 연동 가이드

## 현재 구현 상태

### ✅ 완료된 구현
- `GeminiChatService`: Vertex AI Gemini 통합 서비스
- `AiConfig`: Spring AI 설정 (Gemini 2.0 Flash)
- `TestController`: API 테스트 엔드포인트 (`/api/test/gemini`)
- `application.yml`: Vertex AI 설정 구조

## 설정 방법

### 1. Google Cloud 프로젝트 설정

1. [Google Cloud Console](https://console.cloud.google.com)에 접속
2. 프로젝트 생성 또는 기존 프로젝트 선택
3. Vertex AI API 활성화:
   ```bash
   gcloud services enable aiplatform.googleapis.com
   ```

### 2. 서비스 계정 생성 및 키 발급

1. Google Cloud Console에서 IAM & Admin → Service Accounts 접속
2. "CREATE SERVICE ACCOUNT" 클릭
3. 서비스 계정 정보 입력:
   - Name: `compass-gemini-service`
   - Role: `Vertex AI User` 또는 `Vertex AI Administrator`
4. 키 생성:
   - 생성된 서비스 계정 클릭
   - "Keys" 탭 → "ADD KEY" → "Create new key"
   - JSON 형식 선택
   - 다운로드된 JSON 파일 안전하게 보관

### 3. 환경 변수 설정

#### 로컬 개발 환경 (.env 파일)
```bash
# Google Cloud 설정
GOOGLE_CLOUD_PROJECT_ID=your-project-id
GOOGLE_CLOUD_LOCATION=asia-northeast3
GOOGLE_APPLICATION_CREDENTIALS=/path/to/your/service-account-key.json
```

#### Docker 환경
```bash
# docker-compose 실행 시
GOOGLE_CLOUD_PROJECT_ID=your-project-id \
GOOGLE_CLOUD_LOCATION=asia-northeast3 \
docker-compose up -d

# 서비스 계정 키 파일 마운트 필요
# docker-compose.yml의 volumes 섹션에 추가:
# - ./path/to/key.json:/app/credentials.json
```

#### IntelliJ IDEA 설정
1. Run/Debug Configurations 열기
2. Environment variables 섹션에 추가:
   ```
   GOOGLE_CLOUD_PROJECT_ID=your-project-id
   GOOGLE_CLOUD_LOCATION=asia-northeast3
   GOOGLE_APPLICATION_CREDENTIALS=/absolute/path/to/key.json
   ```

### 4. 테스트 방법

#### 1) 설정 확인
```bash
curl http://localhost:8080/api/test/config
```

응답 예시:
```json
{
  "vertex_ai_configured": true,
  "gcp_project_id": "your-project-id",
  "status": "Spring AI configuration check"
}
```

#### 2) Gemini API 테스트
```bash
curl -X POST http://localhost:8080/api/test/gemini \
  -H "Content-Type: application/json" \
  -d '{"prompt": "안녕하세요, 간단한 자기소개를 해주세요."}'
```

성공 응답:
```json
{
  "model": "Vertex AI Gemini 2.0 Flash",
  "prompt": "안녕하세요, 간단한 자기소개를 해주세요.",
  "response": "안녕하세요! 저는 구글에서 개발한 대규모 언어 모델 Gemini입니다...",
  "status": "success"
}
```

## 통합 테스트

### JUnit 테스트 실행
```bash
# 특정 테스트 실행
./gradlew test --tests "GeminiChatServiceTest"

# 전체 테스트
./gradlew test
```

### 테스트 코드 위치
- `/src/test/java/com/compass/domain/chat/service/ChatModelServiceTest.java`
- `/src/test/java/com/compass/domain/chat/service/GeminiChatServiceTest.java` (생성 필요)

## 트러블슈팅

### 1. "GOOGLE_APPLICATION_CREDENTIALS not found" 오류
- 서비스 계정 키 파일 경로 확인
- 환경 변수가 올바르게 설정되었는지 확인
- JSON 파일 읽기 권한 확인

### 2. "Vertex AI API not enabled" 오류
```bash
gcloud services enable aiplatform.googleapis.com
```

### 3. "Permission denied" 오류
- 서비스 계정에 `Vertex AI User` 역할이 부여되었는지 확인
- 프로젝트 ID가 올바른지 확인

### 4. Region 관련 오류
- `asia-northeast3` (서울) 리전에서 Gemini 모델 사용 가능 여부 확인
- 필요시 `us-central1`로 변경

## 주의사항

1. **서비스 계정 키 보안**
   - JSON 키 파일을 절대 Git에 커밋하지 마세요
   - `.gitignore`에 키 파일 경로 추가 확인
   - 프로덕션에서는 Secret Manager 사용 권장

2. **API 사용량 및 비용**
   - Vertex AI는 사용량 기반 과금
   - 개발/테스트 시 사용량 모니터링 필요
   - [요금 계산기](https://cloud.google.com/products/calculator)로 예상 비용 확인

3. **모델 선택**
   - 현재 설정: `gemini-2.0-flash` (빠른 응답)
   - 복잡한 작업: `gemini-2.0-pro`로 변경 가능

## 참고 자료

- [Vertex AI Gemini API 문서](https://cloud.google.com/vertex-ai/docs/generative-ai/model-reference/gemini)
- [Spring AI Vertex AI 문서](https://docs.spring.io/spring-ai/reference/api/vertexai-gemini-chat.html)
- [Google Cloud 인증 가이드](https://cloud.google.com/docs/authentication/getting-started)