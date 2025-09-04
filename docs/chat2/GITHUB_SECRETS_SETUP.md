# GitHub Secrets를 활용한 Google Cloud 인증 설정 가이드

## 🔐 왜 GitHub Secrets를 사용해야 하나요?

- ✅ **보안**: 서비스 계정 키를 안전하게 보관
- ✅ **팀 협업**: 팀원들이 개별적으로 키를 관리할 필요 없음
- ✅ **CI/CD 통합**: GitHub Actions에서 자동으로 사용 가능
- ✅ **버전 관리**: 키 순환 시 한 곳에서만 업데이트

## 📋 설정 방법

### 1단계: 서비스 계정 키를 Base64로 인코딩

```bash
# Mac/Linux
base64 -i travelagent-468611-1ae0c9d4e187.json | tr -d '\n' > key-base64.txt

# 또는 한 줄로
cat travelagent-468611-1ae0c9d4e187.json | base64 | tr -d '\n'
```

### 2단계: GitHub Repository Secrets 설정

1. GitHub Repository 페이지로 이동
2. Settings → Secrets and variables → Actions 클릭
3. "New repository secret" 버튼 클릭
4. 다음 시크릿들을 추가:

| Secret Name | Value | 설명 |
|------------|-------|------|
| `GOOGLE_CREDENTIALS_BASE64` | Base64 인코딩된 키 | 서비스 계정 키 파일 전체 |
| `GOOGLE_CLOUD_PROJECT_ID` | `travelagent-468611` | GCP 프로젝트 ID |
| `GOOGLE_CLOUD_LOCATION` | `us-central1` | Vertex AI 리전 |
| `OPENAI_API_KEY` | `sk-proj-...` | OpenAI API 키 (옵션) |

### 3단계: GitHub Actions 워크플로우 설정

`.github/workflows/ci.yml`에 이미 설정되어 있음:

```yaml
env:
  GOOGLE_CLOUD_PROJECT_ID: ${{ secrets.GOOGLE_CLOUD_PROJECT_ID }}
  GOOGLE_CLOUD_LOCATION: ${{ secrets.GOOGLE_CLOUD_LOCATION }}
  GOOGLE_CREDENTIALS_BASE64: ${{ secrets.GOOGLE_CREDENTIALS_BASE64 }}

- name: Setup Google Cloud Credentials
  run: |
    if [ ! -z "$GOOGLE_CREDENTIALS_BASE64" ]; then
      echo "$GOOGLE_CREDENTIALS_BASE64" | base64 -d > /tmp/gcp-key.json
      export GOOGLE_APPLICATION_CREDENTIALS=/tmp/gcp-key.json
    fi
```

## 🖥️ 로컬 개발 환경 설정

### 방법 1: GitHub CLI를 통한 자동 설정 (추천)

```bash
# GitHub CLI 설치 (Mac)
brew install gh

# 로그인
gh auth login

# Secrets를 로컬 환경변수로 가져오기
gh secret list --repo prgrms-aibe-devcourse/AIBE2_FinalProject_Compass_BE
```

### 방법 2: 팀 전용 키 관리 스크립트 생성

`scripts/setup-credentials.sh` 생성:

```bash
#!/bin/bash

# GitHub Secrets에서 가져온 Base64 키를 디코딩
echo "Setting up Google Cloud credentials..."

# 팀원들은 이 스크립트를 실행하여 자동으로 설정
if [ -z "$GOOGLE_CREDENTIALS_BASE64" ]; then
    echo "❌ GOOGLE_CREDENTIALS_BASE64 환경변수가 설정되지 않았습니다."
    echo "팀 리더에게 문의하거나 GitHub Secrets를 확인하세요."
    exit 1
fi

# Base64 디코딩하여 파일 생성
echo "$GOOGLE_CREDENTIALS_BASE64" | base64 -d > ./gcp-key-temp.json

# 환경변수 설정
export GOOGLE_APPLICATION_CREDENTIALS="$(pwd)/gcp-key-temp.json"
export GOOGLE_CLOUD_PROJECT_ID="travelagent-468611"
export GOOGLE_CLOUD_LOCATION="us-central1"

echo "✅ Google Cloud 인증 설정 완료!"
echo "   Project ID: $GOOGLE_CLOUD_PROJECT_ID"
echo "   Location: $GOOGLE_CLOUD_LOCATION"
```

### 방법 3: Docker Compose를 통한 설정

`docker-compose.override.yml` (gitignore에 추가):

```yaml
version: '3.8'
services:
  app:
    environment:
      GOOGLE_CREDENTIALS_BASE64: ${GOOGLE_CREDENTIALS_BASE64}
    command: >
      sh -c "
        echo '$${GOOGLE_CREDENTIALS_BASE64}' | base64 -d > /app/credentials.json &&
        export GOOGLE_APPLICATION_CREDENTIALS=/app/credentials.json &&
        java -jar app.jar
      "
```

## 🤝 팀원 온보딩 가이드

### 신규 팀원 설정 절차

1. **Repository 접근 권한 부여**
   - Settings → Manage access → Add people

2. **Secrets 접근 권한 확인**
   - 기본적으로 Repository 접근 권한이 있으면 Actions에서 Secrets 사용 가능

3. **로컬 환경 설정**
   ```bash
   # 1. 프로젝트 클론
   git clone https://github.com/prgrms-aibe-devcourse/AIBE2_FinalProject_Compass_BE.git
   
   # 2. 환경 변수 설정 (.env 파일 생성)
   cp .env.example .env
   
   # 3. 팀 리더에게 GOOGLE_CREDENTIALS_BASE64 값 요청
   # 또는 GitHub Secrets 페이지에서 확인 (권한 필요)
   
   # 4. .env 파일에 추가
   GOOGLE_CREDENTIALS_BASE64=<받은 Base64 값>
   
   # 5. 애플리케이션 실행
   source .env
   ./gradlew bootRun
   ```

## 🔄 키 순환 (Key Rotation)

### 키 업데이트 절차

1. **새 서비스 계정 키 생성**
   ```bash
   gcloud iam service-accounts keys create new-key.json \
     --iam-account=compass-gemini-service@travelagent-468611.iam.gserviceaccount.com
   ```

2. **Base64 인코딩**
   ```bash
   base64 -i new-key.json | tr -d '\n'
   ```

3. **GitHub Secrets 업데이트**
   - Settings → Secrets → `GOOGLE_CREDENTIALS_BASE64` → Update

4. **팀원 알림**
   - Slack/Discord로 키 업데이트 알림
   - 로컬 환경 재설정 요청

## 🚨 보안 주의사항

### DO ✅
- GitHub Secrets 사용
- Base64 인코딩으로 저장
- 정기적인 키 순환 (3개월마다)
- 최소 권한 원칙 적용

### DON'T ❌
- 절대 plain text로 키 저장 금지
- 키를 코드에 하드코딩 금지
- 이메일/메신저로 키 전송 금지
- 공개 저장소에 키 커밋 금지

## 📝 .env.example 파일

프로젝트 루트에 `.env.example` 생성:

```bash
# OpenAI Configuration
OPENAI_API_KEY=your-openai-api-key

# Google Cloud Vertex AI Configuration
GOOGLE_CLOUD_PROJECT_ID=travelagent-468611
GOOGLE_CLOUD_LOCATION=us-central1
# 다음 중 하나 사용:
# 1. 파일 경로 방식
GOOGLE_APPLICATION_CREDENTIALS=/path/to/your/key.json
# 2. Base64 인코딩 방식 (추천)
GOOGLE_CREDENTIALS_BASE64=your-base64-encoded-key

# Database Configuration
DB_HOST=localhost
DB_PORT=5432
DB_NAME=compass
DB_USERNAME=compass_user
DB_PASSWORD=compass_password

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379

# JWT Configuration
JWT_ACCESS_SECRET=your-256-bit-secret-key-for-access-token
JWT_REFRESH_SECRET=your-256-bit-secret-key-for-refresh-token
```

## 🛠️ 자동화 스크립트

`scripts/decode-credentials.sh`:

```bash
#!/bin/bash

# GitHub Secrets의 Base64 키를 자동으로 디코딩하는 스크립트

if [ -z "$1" ]; then
    echo "Usage: ./decode-credentials.sh <base64-encoded-key>"
    exit 1
fi

# Base64 디코딩
echo "$1" | base64 -d > ./gcp-credentials.json

# 권한 설정
chmod 600 ./gcp-credentials.json

echo "✅ Credentials decoded to ./gcp-credentials.json"
echo "🔒 File permissions set to 600 (owner read/write only)"

# 환경변수 설정 안내
echo ""
echo "다음 명령어로 환경변수를 설정하세요:"
echo "export GOOGLE_APPLICATION_CREDENTIALS=$(pwd)/gcp-credentials.json"
```

## 📚 참고 자료

- [GitHub Encrypted Secrets 문서](https://docs.github.com/en/actions/security-guides/encrypted-secrets)
- [Google Cloud 서비스 계정 키 관리](https://cloud.google.com/iam/docs/keys-best-practices)
- [Spring Boot 환경 변수 설정](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)