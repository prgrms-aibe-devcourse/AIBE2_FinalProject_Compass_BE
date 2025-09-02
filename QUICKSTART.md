# 🚀 Compass Backend - 빠른 시작 가이드

> **5분 안에 개발 환경 구축하고 코딩 시작하기!**

## 📋 사전 준비사항

- Java 17
- Docker Desktop
- Git
- IntelliJ IDEA (추천) 또는 VS Code

---

## 🎯 가장 쉬운 방법 - 자동 설정 스크립트 사용! ⭐

### 옵션 1: 로컬 설정 스크립트 (추천!)
```bash
# 1. 프로젝트 클론
git clone https://github.com/prgrms-aibe-devcourse/AIBE2_FinalProject_Compass_BE.git
cd AIBE2_FinalProject_Compass_BE

# 2. 자동 설정 스크립트 실행
./scripts/setup-env.sh

# 3. Docker 서비스 시작
docker-compose up -d postgres redis

# 4. 애플리케이션 실행
./gradlew bootRun
```

스크립트가 자동으로:
- `.env.example`을 복사하여 `.env` 생성
- 필요한 API 키 입력 안내
- 기본값 자동 설정

### 옵션 2: GitHub Actions로 .env 파일 다운로드
1. [GitHub Actions](https://github.com/prgrms-aibe-devcourse/AIBE2_FinalProject_Compass_BE/actions) 페이지 접속
2. **Environment Setup Helper** 워크플로우 선택
3. **Run workflow** 클릭 → setup type 선택 (development)
4. 실행 완료 후 Artifacts에서 `env-file-development` 다운로드
5. 다운로드한 `.env` 파일을 프로젝트 루트에 배치

---

## 🎯 IntelliJ IDEA 사용자 (수동 설정)

### 1️⃣ 프로젝트 열기
```bash
git clone https://github.com/prgrms-aibe-devcourse/AIBE2_FinalProject_Compass_BE.git
```
- IntelliJ IDEA 실행 → **File → Open** → 프로젝트 폴더 선택

### 2️⃣ 환경 변수 자동 설정
```bash
# IntelliJ Terminal에서 실행
./scripts/setup-env.sh
```

### 3️⃣ IntelliJ 환경 변수 설정 (선택사항)
1. **Run → Edit Configurations...**
2. **Spring Boot → CompassApplication** 선택
3. **Environment variables** 클릭
4. 아래 내용 붙여넣기:
```properties
GOOGLE_CREDENTIALS_BASE64=<GitHub Secrets에서 복사한 값>
GOOGLE_CLOUD_PROJECT_ID=travelagent-468611
GOOGLE_CLOUD_LOCATION=us-central1
OPENAI_API_KEY=<GitHub Secrets에서 복사한 값>
DB_HOST=localhost
DB_PORT=5432
DB_NAME=compass
DB_USERNAME=compass_user
DB_PASSWORD=compass_password
REDIS_HOST=localhost
REDIS_PORT=6379
```

### 4️⃣ Google Cloud 인증 파일 생성
IntelliJ Terminal에서:

**Windows (PowerShell)**:
```powershell
$base64 = $env:GOOGLE_CREDENTIALS_BASE64
[System.Convert]::FromBase64String($base64) | Set-Content gcp-key.json -Encoding Byte
```

**Mac/Linux**:
```bash
echo "$GOOGLE_CREDENTIALS_BASE64" | base64 -d > gcp-key.json
```

### 5️⃣ Docker 서비스 시작
```bash
docker-compose up -d postgres redis
```

### 6️⃣ 실행
- `CompassApplication.java` 파일에서 ▶️ 버튼 클릭
- 또는 상단 툴바 Run 버튼

### 7️⃣ 테스트
IntelliJ에서 `/http-requests/test-api.http` 파일 열고:
- 각 요청 옆 ▶️ 버튼 클릭하여 API 테스트

---

## 💻 터미널/명령줄 사용자

### Windows (PowerShell)

```powershell
# 1. 클론
git clone https://github.com/prgrms-aibe-devcourse/AIBE2_FinalProject_Compass_BE.git
cd AIBE2_FinalProject_Compass_BE

# 2. 설정 (처음 한 번만)
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser

# 3. 실행
.\scripts\setup.ps1
.\run.ps1
```

### Windows (CMD)

```cmd
# 1. 클론 후
setup.bat

# 2. GitHub Secrets 값을 .env에 추가

# 3. 인증 파일 생성
setup-credentials.bat

# 4. 실행
run.bat
```

### Mac/Linux

```bash
# 1. 클론
git clone https://github.com/prgrms-aibe-devcourse/AIBE2_FinalProject_Compass_BE.git
cd AIBE2_FinalProject_Compass_BE

# 2. 설정
make setup

# 3. GitHub Secrets 값을 .env에 추가

# 4. 실행
make run
```

---

## 🔑 GitHub Secrets 접근 방법

### 옵션 1: 웹에서 직접 복사
1. [GitHub Secrets 페이지](https://github.com/prgrms-aibe-devcourse/AIBE2_FinalProject_Compass_BE/settings/secrets/actions) 접속
2. Repository 접근 권한 필요 (없으면 팀 리더에게 요청)

### 옵션 2: 팀 리더에게 요청
Slack/Discord로 다음 메시지 전송:
```
Compass 프로젝트 GitHub Secrets 값 요청:
- GOOGLE_CREDENTIALS_BASE64
- OPENAI_API_KEY
```

---

## 🧪 API 테스트

### IntelliJ HTTP Client 사용
`/http-requests/test-api.http` 파일 열고 실행

### cURL 사용
```bash
# Health Check
curl http://localhost:8080/health

# Gemini API 테스트
curl -X POST http://localhost:8080/api/test/gemini \
  -H "Content-Type: application/json" \
  -d '{"prompt": "안녕하세요"}'
```

### Postman 사용
- Import: `http://localhost:8080`
- Endpoints:
  - GET `/health`
  - GET `/api/test/config`
  - POST `/api/test/gemini`

---

## 🚨 문제 해결

### "GOOGLE_APPLICATION_CREDENTIALS not found" 오류

**IntelliJ 사용자**:
Run Configuration 환경 변수에 추가:
```
GOOGLE_APPLICATION_CREDENTIALS=${PROJECT_DIR}/gcp-key.json
```

**터미널 사용자**:
```bash
export GOOGLE_APPLICATION_CREDENTIALS=$(pwd)/gcp-key.json
```

### "Connection refused" DB 연결 오류

```bash
# Docker 상태 확인
docker ps

# 서비스 재시작
docker-compose restart postgres redis
```

### Java 버전 문제

**IntelliJ**:
- File → Project Structure → Project → SDK: Java 17 선택

**터미널**:
```bash
java -version  # 17 이상 확인
```

### Docker가 실행되지 않음

- Windows: Docker Desktop 실행
- Mac: Docker.app 실행
- Linux: `sudo systemctl start docker`

---

## 📁 프로젝트 구조

```
├── src/main/java/com/compass/
│   ├── domain/
│   │   ├── user/        # 사용자 인증
│   │   ├── chat/        # 채팅 기능
│   │   └── trip/        # 여행 계획
│   └── config/          # 설정 (AI, Security 등)
├── docker-compose.yml   # DB/Redis 설정
├── .env.example        # 환경변수 템플릿
└── http-requests/      # API 테스트 파일
```

---

## 🎯 다음 단계

1. **기능 구현 시작**
   - `/docs/TEAM_REQUIREMENTS.md` 확인
   - 담당 도메인 선택 (USER/CHAT/TRIP)

2. **Git 브랜치 생성**
   ```bash
   git checkout -b feature/chat-message
   ```

3. **API 개발**
   - Controller → Service → Repository 순서로 구현
   - 테스트 코드 작성 필수

---

## 💡 유용한 명령어

### Docker
```bash
docker-compose up -d        # 서비스 시작
docker-compose down         # 서비스 중지
docker-compose logs -f app  # 로그 확인
docker ps                   # 실행 중인 컨테이너
```

### Gradle
```bash
./gradlew bootRun          # 애플리케이션 실행
./gradlew test             # 테스트 실행
./gradlew clean build      # 클린 빌드
```

### Git
```bash
git status                 # 상태 확인
git add .                  # 변경사항 추가
git commit -m "message"    # 커밋
git push origin branch     # 푸시
```

---

## 📞 도움이 필요하신가요?

- **Slack**: #compass-backend
- **GitHub Issues**: [이슈 생성](https://github.com/prgrms-aibe-devcourse/AIBE2_FinalProject_Compass_BE/issues/new)
- **문서**: `/docs` 폴더 참조
- **팀 리더**: @CHAT2팀

---

## ✅ 체크리스트

- [ ] Java 17 설치 확인
- [ ] Docker Desktop 실행
- [ ] 프로젝트 클론
- [ ] GitHub Secrets 값 설정
- [ ] Google Cloud 인증 파일 생성
- [ ] DB/Redis 시작
- [ ] Spring Boot 실행
- [ ] API 테스트 성공

**모든 항목이 체크되었다면 개발 준비 완료! 🎉**