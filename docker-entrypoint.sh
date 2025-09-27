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

exec java -jar app.jar "$@"
