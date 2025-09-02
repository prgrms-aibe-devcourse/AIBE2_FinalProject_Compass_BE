#!/bin/bash

# 개발 환경 초기 설정 스크립트
# 이 스크립트는 GitHub Secrets를 자동으로 가져와 설정합니다

set -e

echo "🚀 Compass Backend 개발 환경 초기 설정"
echo "==========================================="

# 1. GitHub CLI 설치 확인
if ! command -v gh &> /dev/null; then
    echo "⚠️  GitHub CLI가 설치되어 있지 않습니다."
    echo "설치 방법:"
    echo "  Mac: brew install gh"
    echo "  Linux: https://github.com/cli/cli#installation"
    exit 1
fi

# 2. GitHub 로그인 확인
if ! gh auth status &> /dev/null; then
    echo "🔐 GitHub에 로그인합니다..."
    gh auth login
fi

# 3. Repository 정보 확인
REPO="prgrms-aibe-devcourse/AIBE2_FinalProject_Compass_BE"
echo "📦 Repository: $REPO"

# 4. .env 파일 생성
if [ ! -f .env ]; then
    if [ -f .env.example ]; then
        echo "📝 .env 파일을 생성합니다..."
        cp .env.example .env
    else
        echo "❌ .env.example 파일을 찾을 수 없습니다."
        exit 1
    fi
else
    echo "✅ .env 파일이 이미 존재합니다."
fi

# 5. GitHub Secrets 가져오기
echo "🔑 GitHub Secrets를 가져옵니다..."

# GOOGLE_CLOUD_PROJECT_ID
PROJECT_ID=$(gh secret list --repo $REPO | grep GOOGLE_CLOUD_PROJECT_ID | awk '{print $1}')
if [ ! -z "$PROJECT_ID" ]; then
    echo "  - GOOGLE_CLOUD_PROJECT_ID 발견"
    # 실제 값은 GitHub API로 직접 가져올 수 없으므로 수동 입력 필요
    echo ""
    echo "⚠️  보안상의 이유로 Secret 값은 직접 입력해야 합니다."
    echo ""
    echo "다음 단계를 따라주세요:"
    echo "1. 브라우저에서 GitHub Secrets 페이지 열기:"
    echo "   https://github.com/$REPO/settings/secrets/actions"
    echo ""
    echo "2. 다음 값들을 복사하여 .env 파일에 붙여넣기:"
    echo "   - GOOGLE_CREDENTIALS_BASE64"
    echo "   - OPENAI_API_KEY (선택사항)"
    echo ""
    read -p "값을 복사했으면 Enter를 누르세요..."
fi

# 6. Google Cloud 인증 파일 생성
echo "🔐 Google Cloud 인증 파일을 생성합니다..."

# .env 파일에서 GOOGLE_CREDENTIALS_BASE64 읽기
source .env

if [ ! -z "$GOOGLE_CREDENTIALS_BASE64" ]; then
    # Base64 디코딩
    if [[ "$OSTYPE" == "darwin"* ]]; then
        echo "$GOOGLE_CREDENTIALS_BASE64" | base64 -D > gcp-key.json
    else
        echo "$GOOGLE_CREDENTIALS_BASE64" | base64 -d > gcp-key.json
    fi
    
    chmod 600 gcp-key.json
    echo "✅ gcp-key.json 파일 생성 완료"
    
    # .env 파일에 경로 추가
    echo "" >> .env
    echo "# Auto-generated path" >> .env
    echo "GOOGLE_APPLICATION_CREDENTIALS=$(pwd)/gcp-key.json" >> .env
else
    echo "⚠️  GOOGLE_CREDENTIALS_BASE64가 설정되지 않았습니다."
    echo "   .env 파일에 값을 추가해주세요."
fi

# 7. Docker 환경 준비
echo "🐳 Docker 환경을 준비합니다..."
if command -v docker &> /dev/null; then
    docker-compose up -d postgres redis
    echo "✅ PostgreSQL과 Redis가 시작되었습니다."
else
    echo "⚠️  Docker가 설치되지 않았습니다."
fi

# 8. 완료
echo ""
echo "🎉 개발 환경 설정이 완료되었습니다!"
echo ""
echo "다음 명령어로 애플리케이션을 실행하세요:"
echo "  source .env"
echo "  ./gradlew bootRun"
echo ""
echo "테스트 실행:"
echo "  curl http://localhost:8080/health"