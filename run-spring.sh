#!/bin/bash

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🚀 Compass BE 안전 실행 스크립트${NC}"
echo "====================================="

# 1. 기존 프로세스 정리
echo -e "${YELLOW}1. 기존 프로세스 정리 중...${NC}"
./cleanup-processes.sh

echo ""
echo -e "${YELLOW}2. 환경 변수 설정 중...${NC}"

# 필수 환경 변수 설정
export DATABASE_PASSWORD=${DATABASE_PASSWORD:-compass1004!}
export JWT_SECRET=${JWT_SECRET:-your-very-long-and-secure-jwt-secret-key-that-should-be-at-least-256-bits}
export JWT_ACCESS_SECRET=${JWT_ACCESS_SECRET:-your-very-long-and-secure-jwt-access-secret-key-that-should-be-at-least-256-bits}
export JWT_REFRESH_SECRET=${JWT_REFRESH_SECRET:-your-very-long-and-secure-jwt-refresh-secret-key-that-should-be-at-least-256-bits}

# Kakao API Keys
export KAKAO_JS_KEY=${KAKAO_JS_KEY:-510b624b2b131b82ad7aee34c7864031}
export KAKAO_REST_KEY=${KAKAO_REST_KEY:-e441db4b56f018bdfb43f87db66c216a}

# Google Cloud 설정
export GOOGLE_CLOUD_PROJECT_ID=${GOOGLE_CLOUD_PROJECT_ID:-travelagent-468611}
export GOOGLE_CLOUD_LOCATION=${GOOGLE_CLOUD_LOCATION:-us-central1}
export GEMINI_MODEL=${GEMINI_MODEL:-gemini-2.0-flash}

# AWS 설정
export AWS_S3_BUCKET_NAME=${AWS_S3_BUCKET_NAME:-compass-travel-images}
export AWS_S3_REGION=${AWS_S3_REGION:-ap-northeast-2}

# 기타 API Keys (더미값)
export OPENAI_API_KEY=${OPENAI_API_KEY:-dummy_key}
export PERPLEXITY_API_KEY=${PERPLEXITY_API_KEY:-dummy_key}
export TOUR_API_KEY=${TOUR_API_KEY:-dummy_key}
export GOOGLE_PLACES_API_KEY=${GOOGLE_PLACES_API_KEY:-dummy_key}

echo -e "${GREEN}✓ 환경 변수 설정 완료${NC}"

# 3. Java 버전 확인
echo ""
echo -e "${YELLOW}3. Java 환경 확인 중...${NC}"

# Java Home 설정
export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.16/libexec/openjdk.jdk/Contents/Home

if [ ! -d "$JAVA_HOME" ]; then
    echo -e "${RED}❌ Java 17이 설치되지 않았습니다!${NC}"
    echo "다음 명령어로 설치하세요: brew install openjdk@17"
    exit 1
fi

$JAVA_HOME/bin/java -version 2>&1 | head -n 1
echo -e "${GREEN}✓ Java 환경 확인 완료${NC}"

# 4. Gradle Daemon 설정
echo ""
echo -e "${YELLOW}4. Gradle 최적화 설정 중...${NC}"

# Gradle properties 설정 (Daemon 재사용 및 메모리 최적화)
mkdir -p ~/.gradle
cat > ~/.gradle/gradle.properties << EOF
# Gradle Daemon 설정
org.gradle.daemon=true
org.gradle.daemon.idletimeout=600000
org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configureondemand=true

# 단일 Daemon 사용 강제
org.gradle.workers.max=4
EOF

echo -e "${GREEN}✓ Gradle 최적화 완료${NC}"

# 5. Spring Profile 선택
echo ""
echo -e "${YELLOW}5. Spring Profile 선택${NC}"
echo "1) docker (기본)"
echo "2) local-rds"
echo "3) rds"
read -p "Profile 선택 [1-3] (기본: 1): " -n 1 -r profile_choice
echo

case $profile_choice in
    2)
        SPRING_PROFILE="local-rds"
        ;;
    3)
        SPRING_PROFILE="rds"
        ;;
    *)
        SPRING_PROFILE="docker"
        ;;
esac

echo -e "${GREEN}✓ Profile: $SPRING_PROFILE${NC}"

# 6. 포트 확인
echo ""
echo -e "${YELLOW}6. 포트 8080 확인 중...${NC}"
if lsof -Pi :8080 -sTCP:LISTEN -t >/dev/null ; then
    echo -e "${RED}❌ 포트 8080이 이미 사용 중입니다!${NC}"
    echo "cleanup-processes.sh를 다시 실행하거나 수동으로 종료하세요."
    exit 1
fi
echo -e "${GREEN}✓ 포트 8080 사용 가능${NC}"

# 7. Spring Boot 실행
echo ""
echo -e "${BLUE}🚀 Spring Boot 애플리케이션을 시작합니다...${NC}"
echo "====================================="
echo -e "${YELLOW}프로필: $SPRING_PROFILE${NC}"
echo -e "${YELLOW}포트: 8080${NC}"
echo "====================================="
echo ""
echo -e "${GREEN}종료하려면 Ctrl+C를 누르세요${NC}"
echo ""

# Gradle Daemon으로 실행 (foreground)
JAVA_HOME=$JAVA_HOME \
./gradlew bootRun \
    --args="--spring.profiles.active=$SPRING_PROFILE" \
    --console=plain \
    --no-daemon \
    --max-workers=2 \
    --no-parallel

# 종료 시 정리
echo ""
echo -e "${YELLOW}애플리케이션이 종료되었습니다.${NC}"
echo -e "${YELLOW}Gradle Daemon을 정리하는 중...${NC}"
./gradlew --stop 2>/dev/null
echo -e "${GREEN}✓ 정리 완료${NC}"