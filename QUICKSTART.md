# 🚀 Compass Backend - 빠른 시작 가이드

> **5분 안에 개발 환경 구축하고 코딩 시작하기!**

## 📋 사전 준비사항

- Java 17
- Docker Desktop
- Git
- IntelliJ IDEA (추천) 또는 VS Code

---

## 🎯 가장 쉬운 방법 - 5분 만에 시작! ⭐

### 단 3단계로 개발 환경 구축:

```bash
# 1. 프로젝트 클론
git clone https://github.com/prgrms-aibe-devcourse/AIBE2_FinalProject_Compass_BE.git
cd AIBE2_FinalProject_Compass_BE

# 2. .env 파일 다운로드 및 설치
# 💡 Discord #compass-backend 채널에서 .env 파일 다운로드
# 다운로드한 .env 파일을 프로젝트 루트에 복사

# 3. Docker 서비스 시작 & 애플리케이션 실행
docker-compose up -d postgres redis
./gradlew bootRun
```

**완료! 🎉** 이제 http://localhost:8080/health 접속해서 확인

### 📥 .env 파일 받는 방법:
1. **Discord #compass-backend 채널** 접속
2. 고정 메시지에서 `.env` 파일 다운로드
3. 다운로드한 파일을 프로젝트 루트 디렉토리에 복사
4. **주의**: `.env` 파일은 절대 Git에 커밋하지 마세요!

---

## 🎯 IntelliJ IDEA 사용자 (수동 설정)

### 1️⃣ 프로젝트 열기
```bash
git clone https://github.com/prgrms-aibe-devcourse/AIBE2_FinalProject_Compass_BE.git
```
- IntelliJ IDEA 실행 → **File → Open** → 프로젝트 폴더 선택

### 2️⃣ .env 파일 설치
```bash
# Discord #compass-backend 채널에서 .env 파일 다운로드 후
# 프로젝트 루트에 복사
```

### 3️⃣ Docker 서비스 시작
```bash
docker-compose up -d postgres redis
```

### 4️⃣ 실행
- `CompassApplication.java` 파일에서 ▶️ 버튼 클릭
- 또는 상단 툴바 Run 버튼

### 5️⃣ 테스트
IntelliJ에서 `/http-requests/test-api.http` 파일 열고:
- 각 요청 옆 ▶️ 버튼 클릭하여 API 테스트

---

## 💻 간단 설정 - 모든 OS 공통

```bash
# 1. 프로젝트 클론
git clone https://github.com/prgrms-aibe-devcourse/AIBE2_FinalProject_Compass_BE.git
cd AIBE2_FinalProject_Compass_BE

# 2. .env 파일 설치
# Discord #compass-backend 채널에서 .env 파일 다운로드
# 프로젝트 루트에 복사 (AIBE2_FinalProject_Compass_BE 폴더)

# 3. DB 시작
docker-compose up -d postgres redis

# 4. 애플리케이션 실행
./gradlew bootRun
```

## 💻 터미널/명령줄 사용자 (고급)

### Windows (PowerShell)

```powershell
# 1. 클론
git clone https://github.com/prgrms-aibe-devcourse/AIBE2_FinalProject_Compass_BE.git
cd AIBE2_FinalProject_Compass_BE

# 2. .env 파일 설치
# Discord #compass-backend 채널에서 .env 파일 다운로드 후 프로젝트 루트에 복사

# 3. Docker 서비스 시작
docker-compose up -d postgres redis

# 4. 애플리케이션 실행
.\gradlew.bat bootRun
```

### Mac/Linux

```bash
# 1. 클론
git clone https://github.com/prgrms-aibe-devcourse/AIBE2_FinalProject_Compass_BE.git
cd AIBE2_FinalProject_Compass_BE

# 2. .env 파일 설치
# Discord #compass-backend 채널에서 .env 파일 다운로드 후 프로젝트 루트에 복사

# 3. Docker 서비스 시작 & 실행
make setup  # Docker 시작
make run    # 애플리케이션 실행
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

**.env 파일 확인**:
```bash
# .env 파일이 프로젝트 루트에 있는지 확인
ls -la .env

# .env 파일에 GOOGLE_CREDENTIALS_BASE64가 있는지 확인
grep GOOGLE_CREDENTIALS_BASE64 .env
```

**해결 방법**:
- Discord에서 최신 .env 파일 다시 다운로드
- 프로젝트 루트에 정확히 복사되었는지 확인

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
- [ ] Discord에서 .env 파일 다운로드
- [ ] .env 파일을 프로젝트 루트에 복사
- [ ] DB/Redis 시작
- [ ] Spring Boot 실행
- [ ] API 테스트 성공

**모든 항목이 체크되었다면 개발 준비 완료! 🎉**