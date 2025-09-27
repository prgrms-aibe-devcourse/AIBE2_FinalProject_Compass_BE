#!/bin/bash

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

require_env() {
    local var_name="$1"
    local message="$2"

    if [ -z "${!var_name:-}" ]; then
        echo -e "${RED}❌ 환경 변수 ${var_name}가 설정되지 않았습니다.${NC}"
        if [ -n "$message" ]; then
            echo -e "${YELLOW}${message}${NC}"
        fi
        exit 1
    fi
}

decode_base64_to_file() {
    local data="$1"
    local destination="$2"

    if command -v python3 >/dev/null 2>&1; then
        if ! printf '%s' "$data" | python3 - "$destination" <<'PY'
import base64
import pathlib
import sys

payload = sys.stdin.read()
path = pathlib.Path(sys.argv[1])
path.write_bytes(base64.b64decode(payload))
PY
        then
            echo -e "${RED}❌ Base64 데이터를 python3로 디코딩하는 데 실패했습니다.${NC}"
            exit 1
        fi
        return
    fi

    if printf '%s' "$data" | base64 --decode > "$destination" 2>/dev/null; then
        return
    fi

    if printf '%s' "$data" | base64 -d > "$destination" 2>/dev/null; then
        return
    fi

    if printf '%s' "$data" | base64 -D > "$destination" 2>/dev/null; then
        return
    fi

    echo -e "${RED}❌ Base64 디코딩에 사용할 수 있는 도구가 없습니다. python3 또는 GNU base64를 설치하세요.${NC}"
    exit 1
}

optional_env() {
    local var_name="$1"
    local message="$2"

    if [ -z "${!var_name:-}" ]; then
        echo -e "${YELLOW}⚠️ 선택 환경 변수 ${var_name}가 설정되지 않았습니다.${NC}"
        if [ -n "$message" ]; then
            echo -e "    ↳ ${message}"
        fi
        return 1
    fi

    export "${var_name}"="${!var_name}"
    return 0
}

echo -e "${BLUE}🚀 Compass BE 안전 실행 스크립트${NC}"
echo "====================================="

# 1. 기존 프로세스 정리
echo -e "${YELLOW}1. 기존 프로세스 정리 중...${NC}"
./cleanup-processes.sh

echo ""
echo -e "${YELLOW}2. 환경 변수 설정 중...${NC}"

# 필수 환경 변수 설정
require_env DATABASE_PASSWORD "예: export DATABASE_PASSWORD=your-db-password"
require_env JWT_SECRET "예: export JWT_SECRET=base64-encoded-secret"
require_env JWT_ACCESS_SECRET "예: export JWT_ACCESS_SECRET=base64-encoded-secret"
require_env JWT_REFRESH_SECRET "예: export JWT_REFRESH_SECRET=base64-encoded-secret"
# Google Cloud 서비스 계정은 선택적으로 설정

export DATABASE_PASSWORD
export JWT_SECRET
export JWT_ACCESS_SECRET
export JWT_REFRESH_SECRET

# Kakao API Keys
require_env KAKAO_JS_KEY "카카오 개발자 콘솔에서 발급받은 JS 키를 설정하세요."
require_env KAKAO_REST_KEY "카카오 REST API 키를 설정하세요."

export KAKAO_JS_KEY
export KAKAO_REST_KEY

# Google Cloud 설정
export GOOGLE_CLOUD_PROJECT_ID=${GOOGLE_CLOUD_PROJECT_ID:-travelagent-468611}
export GOOGLE_CLOUD_LOCATION=${GOOGLE_CLOUD_LOCATION:-us-central1}
export GEMINI_MODEL=${GEMINI_MODEL:-gemini-2.0-flash}

# AWS 설정
export AWS_S3_BUCKET_NAME=${AWS_S3_BUCKET_NAME:-compass-travel-images}
export AWS_S3_REGION=${AWS_S3_REGION:-ap-northeast-2}

# 기타 API Keys (선택)
optional_env OPENAI_API_KEY "미설정 시 OpenAI 관련 기능이 비활성화됩니다."
optional_env PERPLEXITY_API_KEY "미설정 시 Perplexity 기반 검색 기능이 제한됩니다."
optional_env TOUR_API_KEY "Tour API를 사용하지 않는다면 비워둬도 됩니다."
optional_env GOOGLE_PLACES_API_KEY "Google Places 확장 기능을 사용하려면 설정하세요."

# Google 서비스 계정 파일 복원 (선택)
DEFAULT_GOOGLE_CREDENTIALS_PATH="$PROJECT_ROOT/google-credentials.json"
if optional_env GCP_SERVICE_ACCOUNT_JSON "Base64로 인코딩한 서비스 계정이 없으면 OCR/Gemini 기능이 제한됩니다."; then
    GOOGLE_CREDENTIALS_PATH="${GOOGLE_APPLICATION_CREDENTIALS:-$DEFAULT_GOOGLE_CREDENTIALS_PATH}"
    mkdir -p "$(dirname "$GOOGLE_CREDENTIALS_PATH")"
    if ! python3 - "$GOOGLE_CREDENTIALS_PATH" <<'PY'
import base64
import os
import pathlib
import sys

dest = pathlib.Path(sys.argv[1])
value = os.environ.get("GCP_SERVICE_ACCOUNT_JSON", "")
if not value.strip():
    sys.exit("GCP_SERVICE_ACCOUNT_JSON is empty")

try:
    if value.lstrip().startswith('{'):
        data = value.encode('utf-8')
    else:
        data = base64.b64decode(value)
except Exception as exc:
    sys.stderr.write(f"Failed to decode GCP_SERVICE_ACCOUNT_JSON: {exc}\n")
    sys.exit(1)

dest.write_bytes(data)
PY
    then
        echo -e "${RED}❌ Google 서비스 계정 정보를 디코딩하지 못했습니다. 값이 올바른 Base64인지 확인하세요.${NC}"
        exit 1
    fi
    chmod 600 "$GOOGLE_CREDENTIALS_PATH"
    export GOOGLE_APPLICATION_CREDENTIALS="$GOOGLE_CREDENTIALS_PATH"
    echo -e "${GREEN}✓ Google 서비스 계정 정보를 ${GOOGLE_APPLICATION_CREDENTIALS} 경로에 복원했습니다.${NC}"
    ls -l "$GOOGLE_APPLICATION_CREDENTIALS"
else
    export GOOGLE_APPLICATION_CREDENTIALS=""
    if [ -f "$DEFAULT_GOOGLE_CREDENTIALS_PATH" ]; then
        rm -f "$DEFAULT_GOOGLE_CREDENTIALS_PATH"
    fi
    echo -e "${YELLOW}⚠️ Google 서비스 계정이 설정되지 않아 OCR/Gemini 기능이 비활성화됩니다.${NC}"
fi

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
