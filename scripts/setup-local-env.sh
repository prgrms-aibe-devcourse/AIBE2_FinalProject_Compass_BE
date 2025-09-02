#!/bin/bash

# Compass Backend 로컬 환경 자동 설정 스크립트

set -e  # 에러 발생 시 스크립트 중단

echo "🚀 Compass Backend 로컬 환경 설정을 시작합니다..."

# 1. .env 파일 확인
if [ ! -f .env ]; then
    if [ -f .env.example ]; then
        echo "📝 .env.example에서 .env 파일을 생성합니다..."
        cp .env.example .env
        echo "⚠️  .env 파일이 생성되었습니다. GitHub Secrets 값을 추가해주세요."
        exit 1
    else
        echo "❌ .env.example 파일을 찾을 수 없습니다."
        exit 1
    fi
fi

# 2. 환경 변수 로드
echo "📦 환경 변수를 로드합니다..."
source .env

# 3. Google Cloud 인증 설정
if [ ! -z "$GOOGLE_CREDENTIALS_BASE64" ]; then
    echo "🔐 Google Cloud 인증 파일을 생성합니다..."
    
    # Base64 디코딩 (Mac과 Linux 모두 지원)
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS
        echo "$GOOGLE_CREDENTIALS_BASE64" | base64 -D > gcp-key-temp.json
    else
        # Linux
        echo "$GOOGLE_CREDENTIALS_BASE64" | base64 -d > gcp-key-temp.json
    fi
    
    # 파일 권한 설정
    chmod 600 gcp-key-temp.json
    
    # 환경 변수 설정
    export GOOGLE_APPLICATION_CREDENTIALS="$(pwd)/gcp-key-temp.json"
    
    echo "✅ Google Cloud 인증 설정 완료!"
    echo "   Project ID: ${GOOGLE_CLOUD_PROJECT_ID}"
    echo "   Location: ${GOOGLE_CLOUD_LOCATION}"
    
elif [ ! -z "$GOOGLE_APPLICATION_CREDENTIALS" ]; then
    echo "📁 기존 Google Cloud 인증 파일을 사용합니다: $GOOGLE_APPLICATION_CREDENTIALS"
    
    if [ ! -f "$GOOGLE_APPLICATION_CREDENTIALS" ]; then
        echo "❌ 인증 파일을 찾을 수 없습니다: $GOOGLE_APPLICATION_CREDENTIALS"
        exit 1
    fi
else
    echo "⚠️  Google Cloud 인증이 설정되지 않았습니다."
    echo "   .env 파일에 다음 중 하나를 설정해주세요:"
    echo "   - GOOGLE_CREDENTIALS_BASE64 (권장)"
    echo "   - GOOGLE_APPLICATION_CREDENTIALS"
fi

# 4. Docker 상태 확인
if command -v docker &> /dev/null; then
    echo "🐳 Docker 상태를 확인합니다..."
    
    if docker ps &> /dev/null; then
        echo "✅ Docker가 실행 중입니다."
        
        # PostgreSQL과 Redis 실행 여부 확인
        if ! docker ps | grep -q "compass-postgres"; then
            echo "🗄️  PostgreSQL을 시작합니다..."
            docker-compose up -d postgres
        else
            echo "✅ PostgreSQL이 이미 실행 중입니다."
        fi
        
        if ! docker ps | grep -q "compass-redis"; then
            echo "💾 Redis를 시작합니다..."
            docker-compose up -d redis
        else
            echo "✅ Redis가 이미 실행 중입니다."
        fi
    else
        echo "⚠️  Docker가 실행되지 않았습니다. Docker Desktop을 시작해주세요."
    fi
else
    echo "⚠️  Docker가 설치되지 않았습니다."
fi

# 5. Java 버전 확인
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -ge 17 ]; then
        echo "✅ Java $JAVA_VERSION 확인됨"
    else
        echo "⚠️  Java 17 이상이 필요합니다. 현재 버전: $JAVA_VERSION"
    fi
else
    echo "❌ Java가 설치되지 않았습니다. Java 17을 설치해주세요."
    exit 1
fi

# 6. 완료 메시지
echo ""
echo "🎉 로컬 환경 설정이 완료되었습니다!"
echo ""
echo "다음 명령어로 애플리케이션을 실행하세요:"
echo "  ./gradlew bootRun"
echo ""
echo "또는 현재 셸에서 환경 변수를 적용하려면:"
echo "  source .env"
echo "  export GOOGLE_APPLICATION_CREDENTIALS=$(pwd)/gcp-key-temp.json"
echo ""