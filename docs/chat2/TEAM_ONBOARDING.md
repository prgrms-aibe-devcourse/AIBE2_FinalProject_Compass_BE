# 팀원 온보딩 가이드 - Compass Backend

## 🚀 빠른 시작 (5분 안에 개발 환경 구축!)

### 1단계: 프로젝트 클론
```bash
git clone https://github.com/prgrms-aibe-devcourse/AIBE2_FinalProject_Compass_BE.git
cd AIBE2_FinalProject_Compass_BE
```

### 2단계: .env 파일 설치

#### Discord에서 .env 파일 다운로드
1. **Discord #compass-backend 채널** 접속
2. **고정 메시지**에서 `.env` 파일 다운로드
3. 다운로드한 파일을 **프로젝트 루트**에 복사

**주의**: `.env` 파일은 절대 Git에 커밋하지 마세요!

### 3단계: Docker 서비스 시작

```bash
# PostgreSQL과 Redis 시작
docker-compose up -d postgres redis
```

### 4단계: 애플리케이션 실행

#### Docker를 사용하는 경우:
```bash
# DB와 Redis만 실행
docker-compose up -d postgres redis

# Spring Boot 실행
source .env
./gradlew bootRun
```

#### 전체 Docker Compose 사용:
```bash
docker-compose up -d
```

### 5단계: 동작 확인
```bash
# Health Check
curl http://localhost:8080/health

# Gemini API 테스트
curl -X POST http://localhost:8080/api/test/gemini \
  -H "Content-Type: application/json" \
  -d '{"prompt": "안녕하세요"}'
```

## 🔍 자주 발생하는 문제 해결

### 1. "GOOGLE_APPLICATION_CREDENTIALS not found" 오류
```bash
# .env 파일 확인
cat .env | grep GOOGLE

# 환경 변수 다시 로드
source .env

# JSON 파일 존재 확인
ls -la gcp-key.json
```

### 2. "Permission denied" 오류
```bash
# 파일 권한 설정
chmod 600 gcp-key.json
```

### 3. Base64 디코딩 오류
```bash
# Mac에서 디코딩
echo "$GOOGLE_CREDENTIALS_BASE64" | base64 -D > gcp-key.json

# Linux에서 디코딩
echo "$GOOGLE_CREDENTIALS_BASE64" | base64 -d > gcp-key.json
```

## 📋 체크리스트

- [ ] GitHub Repository 접근 권한 확인
- [ ] `.env` 파일 생성
- [ ] Discord에서 .env 파일 다운로드
- [ ] Docker/Docker Compose 설치
- [ ] Java 17 설치
- [ ] 애플리케이션 실행 확인

## 🛠️ 개발 도구 추천

### IDE 설정 (IntelliJ IDEA)
1. File → Open → 프로젝트 폴더 선택
2. Run → Edit Configurations
3. Environment variables에 추가:
   ```
   GOOGLE_APPLICATION_CREDENTIALS=/absolute/path/to/gcp-key.json
   GOOGLE_CLOUD_PROJECT_ID=travelagent-468611
   GOOGLE_CLOUD_LOCATION=us-central1
   ```

### VS Code 설정
`.vscode/launch.json`:
```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "Spring Boot App",
      "request": "launch",
      "mainClass": "com.compass.CompassApplication",
      "envFile": "${workspaceFolder}/.env"
    }
  ]
}
```

## 📞 도움이 필요하신가요?

- **Slack 채널**: #compass-backend
- **팀 리더**: @CHAT2팀
- **문서**: `/docs` 폴더 참조
- **이슈 트래커**: [GitHub Issues](https://github.com/prgrms-aibe-devcourse/AIBE2_FinalProject_Compass_BE/issues)

## 🎉 환영합니다!

이제 개발 환경 설정이 완료되었습니다! 
행복한 코딩되세요! 🚀