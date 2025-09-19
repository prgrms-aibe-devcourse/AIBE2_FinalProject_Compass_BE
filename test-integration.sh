#!/bin/bash

echo "=== Compass Backend Integration Test ==="
echo

BASE_URL="http://localhost:8080"
EMAIL="test1219@test.com"
PASSWORD="test1234"
THREAD_ID="test-thread-$(date +%s)"

echo "1. Registering user: $EMAIL"
REGISTER_RESPONSE=$(curl -s -X POST $BASE_URL/api/auth/signup \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"$EMAIL\",
    \"password\": \"$PASSWORD\",
    \"nickname\": \"Test User\"
  }")

echo "Register Response: $REGISTER_RESPONSE"
echo

echo "2. Logging in..."
LOGIN_RESPONSE=$(curl -s -X POST $BASE_URL/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"$EMAIL\",
    \"password\": \"$PASSWORD\"
  }")

echo "Login Response: $LOGIN_RESPONSE"
echo

# Extract token from response
TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
    echo "Error: Failed to get token from login response"
    exit 1
fi

echo "3. Got Token: ${TOKEN:0:50}..."
echo

echo "4. Creating conversation with LLM Orchestrator..."
CHAT_RESPONSE=$(curl -s -X POST $BASE_URL/api/chat/unified \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{
    \"message\": \"여행 계획 짜줘\",
    \"threadId\": \"$THREAD_ID\",
    \"userId\": \"$EMAIL\"
  }")

echo "Chat Response: $CHAT_RESPONSE"
echo

echo "5. Sending follow-up message..."
FOLLOWUP_RESPONSE=$(curl -s -X POST $BASE_URL/api/chat/unified \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{
    \"message\": \"서울에서 제주도 3박 4일 여행 계획 세워줘\",
    \"threadId\": \"$THREAD_ID\",
    \"userId\": \"$EMAIL\"
  }")

echo "Follow-up Response: $FOLLOWUP_RESPONSE"
echo

echo "=== Test Complete ==="
echo "Thread ID: $THREAD_ID"
echo "Check the database for stored conversations in chat_threads and chat_messages tables"