#!/bin/bash
set -euo pipefail

if [[ -n "${GCP_SERVICE_ACCOUNT_JSON:-}" ]]; then
  STAGING_DIR="/var/app/staging"
  CREDENTIALS_PATH="$STAGING_DIR/google-credentials.json"
  mkdir -p "$STAGING_DIR"
  if ! printf '%s' "$GCP_SERVICE_ACCOUNT_JSON" | base64 -d > "$CREDENTIALS_PATH" 2>/dev/null; then
    printf '%s' "$GCP_SERVICE_ACCOUNT_JSON" | base64 --decode > "$CREDENTIALS_PATH"
  fi
  chmod 600 "$CREDENTIALS_PATH"
  chown webapp:webapp "$CREDENTIALS_PATH"
  CURRENT_DIR="/var/app/current"
  mkdir -p "$CURRENT_DIR"
  cp "$CREDENTIALS_PATH" "$CURRENT_DIR/google-credentials.json"
  chmod 600 "$CURRENT_DIR/google-credentials.json"
  chown webapp:webapp "$CURRENT_DIR/google-credentials.json"
  echo "GCP service account credentials restored to $CREDENTIALS_PATH"
else
  echo "GCP_SERVICE_ACCOUNT_JSON is not set; skipping credential restoration."
fi
