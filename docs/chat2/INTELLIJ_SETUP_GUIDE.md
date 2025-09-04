# IntelliJ IDEA 개발 환경 설정 가이드

## 🎯 5분 안에 IntelliJ로 개발 시작하기!

### 1단계: 프로젝트 열기
1. IntelliJ IDEA 실행
2. **File → Open** → 프로젝트 폴더 선택
3. Gradle 프로젝트로 자동 인식 → Import

### 2단계: GitHub Secrets 값 가져오기
1. 브라우저에서 [GitHub Secrets 페이지](https://github.com/prgrms-aibe-devcourse/AIBE2_FinalProject_Compass_BE/settings/secrets/actions) 열기
2. 다음 값들 복사:
   - `GOOGLE_CREDENTIALS_BASE64`
   - `OPENAI_API_KEY`

### 3단계: IntelliJ 환경 변수 설정

#### 방법 A: Run Configuration에 직접 설정 (권장) ⭐
1. 상단 메뉴: **Run → Edit Configurations...**
2. **Spring Boot → CompassApplication** 선택 (없으면 + 버튼으로 추가)
3. **Environment variables** 섹션 클릭
4. 다음 환경 변수 추가:

```properties
# 필수 환경 변수
GOOGLE_CREDENTIALS_BASE64=<GitHub Secrets에서 복사한 값>
GOOGLE_CLOUD_PROJECT_ID=travelagent-468611
GOOGLE_CLOUD_LOCATION=us-central1
OPENAI_API_KEY=<GitHub Secrets에서 복사한 값>

# DB 설정 (Docker 사용 시)
DB_HOST=localhost
DB_PORT=5432
DB_NAME=compass
DB_USERNAME=compass_user
DB_PASSWORD=compass_password

# Redis 설정
REDIS_HOST=localhost
REDIS_PORT=6379
```

5. **Apply** → **OK**

#### 방법 B: .env 파일 + EnvFile 플러그인 사용
1. **Settings → Plugins** → "EnvFile" 검색 → Install
2. 프로젝트 루트에 `.env` 파일 생성:
```bash
cp .env.example .env
```
3. `.env` 파일에 GitHub Secrets 값 추가
4. **Run → Edit Configurations...**
5. **EnvFile** 탭에서 `.env` 파일 추가

### 4단계: Google Cloud 인증 파일 생성

#### IntelliJ Terminal에서 실행:

**Windows (PowerShell)**:
```powershell
# .env 파일에서 Base64 값 읽기 (방법 B 사용 시)
$base64 = (Get-Content .env | Select-String "GOOGLE_CREDENTIALS_BASE64=").Line.Split("=")[1]

# 또는 환경 변수에서 직접 (방법 A 사용 시)
$base64 = $env:GOOGLE_CREDENTIALS_BASE64

# Base64 디코딩하여 JSON 파일 생성
[System.Convert]::FromBase64String($base64) | Set-Content gcp-key.json -Encoding Byte
```

**Mac/Linux**:
```bash
# Base64 디코딩
echo "$GOOGLE_CREDENTIALS_BASE64" | base64 -d > gcp-key.json
```

### 5단계: IntelliJ에서 Spring Boot 실행

#### Docker 서비스 먼저 시작 (Terminal):
```bash
# IntelliJ Terminal에서 실행
docker-compose up -d postgres redis
```

#### Spring Boot 실행:
1. `CompassApplication.java` 파일 열기
2. 클래스명 옆 ▶️ 버튼 클릭 → **Run 'CompassApplication'**
3. 또는 상단 툴바에서 ▶️ Run 버튼

### 6단계: 동작 확인

IntelliJ Terminal 또는 HTTP Client에서:

#### IntelliJ HTTP Client 사용 (추천):
1. 프로젝트에 `http-requests` 폴더 생성
2. `test-api.http` 파일 생성:

```http
### Health Check
GET http://localhost:8080/health

### Test Gemini API
POST http://localhost:8080/api/test/gemini
Content-Type: application/json

{
  "prompt": "안녕하세요"
}

### Test Configuration
GET http://localhost:8080/api/test/config
```

3. 각 요청 옆 ▶️ 버튼 클릭하여 실행

## 🔧 IntelliJ 추가 설정 (선택사항)

### Lombok 플러그인
1. **Settings → Plugins** → "Lombok" 검색 → Install
2. **Settings → Build → Compiler → Annotation Processors**
3. **Enable annotation processing** 체크

### Spring Boot DevTools 활성화
1. **Settings → Build → Compiler**
2. **Build project automatically** 체크
3. **Settings → Advanced Settings**
4. **Allow auto-make to start even if developed application is currently running** 체크

### Database Tools 연결
1. 우측 **Database** 탭 클릭
2. **+ → Data Source → PostgreSQL**
3. 연결 정보:
   - Host: `localhost`
   - Port: `5432`
   - Database: `compass`
   - User: `compass_user`
   - Password: `compass_password`

## 🚨 자주 발생하는 문제

### 1. "GOOGLE_APPLICATION_CREDENTIALS not found" 오류
**해결방법**:
1. Run Configuration 환경 변수에 추가:
```
GOOGLE_APPLICATION_CREDENTIALS=C:\path\to\project\gcp-key.json
```
또는 상대 경로:
```
GOOGLE_APPLICATION_CREDENTIALS=${PROJECT_DIR}/gcp-key.json
```

### 2. "Connection refused" DB 연결 오류
**해결방법**:
```bash
# Docker가 실행 중인지 확인
docker ps

# PostgreSQL, Redis 재시작
docker-compose restart postgres redis
```

### 3. Gradle Sync 실패
**해결방법**:
1. **File → Invalidate Caches and Restart**
2. **View → Tool Windows → Gradle** → Refresh 버튼

### 4. Java 버전 문제
**해결방법**:
1. **File → Project Structure → Project**
2. **SDK**: Java 17 선택
3. **Language level**: 17 선택

## 💡 IntelliJ 개발 팁

### 단축키 (Windows/Linux)
- `Shift + F10`: 실행
- `Shift + F9`: 디버그
- `Ctrl + Shift + F10`: 현재 파일 실행
- `Alt + Enter`: 빠른 수정
- `Ctrl + Alt + L`: 코드 포맷팅

### 단축키 (Mac)
- `Ctrl + R`: 실행
- `Ctrl + D`: 디버그
- `Ctrl + Shift + R`: 현재 파일 실행
- `Option + Enter`: 빠른 수정
- `Cmd + Option + L`: 코드 포맷팅

### Live Templates
- `psvm`: public static void main
- `sout`: System.out.println
- `iter`: for-each 루프
- `nn`: null 체크

## 📝 체크리스트

- [ ] IntelliJ에서 프로젝트 열기
- [ ] GitHub Secrets 값 복사
- [ ] Run Configuration 환경 변수 설정
- [ ] Google Cloud 인증 파일 생성 (gcp-key.json)
- [ ] Docker로 DB/Redis 시작
- [ ] Spring Boot 애플리케이션 실행
- [ ] API 테스트로 동작 확인

## 🎉 완료!

이제 IntelliJ에서 개발할 준비가 완료되었습니다!
디버깅, 코드 자동완성, 리팩토링 등 IntelliJ의 강력한 기능들을 활용하세요! 🚀