#!/bin/bash

# Test Flow Script: 회원가입 → 로그인 → 대화 시작 → 정보수집

echo "=== Compass Backend Full Flow Test ==="
echo

BASE_URL="http://localhost:8080"
EMAIL="testuser1219_1758273509@test.com"
PASSWORD="test1234"
THREAD_ID="flow-test-$(date +%s)"

echo "1. 로그인 중..."
LOGIN_RESPONSE=$(curl -s -X POST $BASE_URL/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"$EMAIL\",
    \"password\": \"$PASSWORD\"
  }")

echo "로그인 응답: $LOGIN_RESPONSE"
echo

TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
    echo "Error: 토큰을 받지 못했습니다"
    exit 1
fi

echo "토큰 받음: ${TOKEN:0:50}..."
echo

# 2. 첫 대화 시작 (Thread 생성 확인)
echo "2. 대화 시작 (안녕하세요)..."
CHAT1_RESPONSE=$(curl -s -X POST $BASE_URL/api/chat/unified \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{
    \"message\": \"안녕하세요! 여행 계획을 도와주세요.\",
    \"threadId\": \"$THREAD_ID\",
    \"userId\": \"$EMAIL\"
  }")

echo "첫 번째 응답: $CHAT1_RESPONSE"
echo

# 3. DB 상태 확인 (Docker logs)
echo "3. DB 저장 확인 (Docker logs)..."
docker logs compass-backend --tail 20 | grep -E "ChatThread|ChatMessage|저장"
echo

# 4. 시시콜콜한 대화
echo "4. 시시콜콜한 대화 진행..."
CHAT2_RESPONSE=$(curl -s -X POST $BASE_URL/api/chat/unified \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{
    \"message\": \"저는 20대 후반이고, 친구들과 함께 가고 싶어요. 바다 보는 것을 좋아합니다.\",
    \"threadId\": \"$THREAD_ID\",
    \"userId\": \"$EMAIL\"
  }")

echo "두 번째 응답: $CHAT2_RESPONSE"
echo

# 5. 여행지 언급하여 정보수집 트리거
echo "5. 여행지 언급 (정보수집 트리거)..."
CHAT3_RESPONSE=$(curl -s -X POST $BASE_URL/api/chat/unified \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{
    \"message\": \"제주도로 3박 4일 여행을 가려고 합니다. 서울에서 출발해요.\",
    \"threadId\": \"$THREAD_ID\",
    \"userId\": \"$EMAIL\"
  }")

echo "세 번째 응답 (정보수집 폼): $CHAT3_RESPONSE"
echo

# 6. 최종 DB 상태 확인
echo "6. 최종 DB 상태 확인..."
docker logs compass-backend --tail 30 | grep -E "Function|정보수집|QUICK_FORM|ChatMessage"
echo

echo "=== 테스트 완료 ==="
echo "Thread ID: $THREAD_ID"
echo "User Email: $EMAIL"