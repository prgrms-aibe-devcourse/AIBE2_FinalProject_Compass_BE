#!/bin/sh
set -eu

if [ -n "${GCP_SERVICE_ACCOUNT_JSON:-}" ]; then
  CREDENTIALS_PATH="${GOOGLE_APPLICATION_CREDENTIALS:-/app/google-credentials.json}"
  CREDENTIALS_DIR="$(dirname "$CREDENTIALS_PATH")"
  mkdir -p "$CREDENTIALS_DIR"
  if ! printf '%s' "$GCP_SERVICE_ACCOUNT_JSON" | base64 -d > "$CREDENTIALS_PATH" 2>/dev/null; then
    printf '%s' "$GCP_SERVICE_ACCOUNT_JSON" | base64 --decode > "$CREDENTIALS_PATH"
  fi
  chmod 600 "$CREDENTIALS_PATH"
  export GOOGLE_APPLICATION_CREDENTIALS="$CREDENTIALS_PATH"
  echo "Google service account credentials restored to $CREDENTIALS_PATH"
else
  echo "GCP_SERVICE_ACCOUNT_JSON is not set; skipping credential restoration."
fi

# Elastic Beanstalk 포트 설정 (PORT 환경 변수 또는 기본값 8080)
JAVA_OPTS="-Dserver.port=${PORT:-8080}"

# DNS 및 네트워크 설정 개선 (RDS 연결 안정화)
JAVA_OPTS="$JAVA_OPTS -Djava.net.preferIPv4Stack=true"
JAVA_OPTS="$JAVA_OPTS -Dnetworkaddress.cache.ttl=60"

exec java ${JAVA_OPTS} -jar app.jar "$@"
