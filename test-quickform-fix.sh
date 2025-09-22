#!/bin/bash

echo "=== Testing Quick Form Fix ==="
echo "Testing Epic 2 + LLM Orchestrator integration"
echo ""

# Server URL (using port 8081 where the server is running)
BASE_URL="http://localhost:8081"

# Test user credentials
USER_EMAIL="test_quickform@test.com"
USER_PASSWORD="password123"

# 1. Register test user
echo "1. Registering test user..."
REGISTER_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "'$USER_EMAIL'",
    "password": "'$USER_PASSWORD'",
    "username": "QuickFormTester"
  }')
echo "Register response: $REGISTER_RESPONSE"
echo ""

# 2. Login to get token
echo "2. Logging in..."
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "'$USER_EMAIL'",
    "password": "'$USER_PASSWORD'"
  }')
TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"accessToken":"[^"]*' | sed 's/"accessToken":"//')
echo "Got token: ${TOKEN:0:20}..."
echo ""

# 3. Start conversation (should trigger INITIALIZATION phase)
echo "3. Starting travel conversation..."
INIT_RESPONSE=$(curl -s -X POST "$BASE_URL/api/chat/message" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "제주도 여행 계획 짜고 싶어"
  }')
echo "Initial response:"
echo "$INIT_RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$INIT_RESPONSE"
echo ""

# 4. Confirm travel planning (should move to INFORMATION_COLLECTION and show QUICK_FORM)
echo "4. Confirming travel planning..."
CONFIRM_RESPONSE=$(curl -s -X POST "$BASE_URL/api/chat/message" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "네, 좋아요"
  }')
echo "Confirmation response (should show QUICK_FORM):"
echo "$CONFIRM_RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$CONFIRM_RESPONSE"
echo ""

# Extract response type
RESPONSE_TYPE=$(echo "$CONFIRM_RESPONSE" | grep -o '"responseType":"[^"]*' | sed 's/"responseType":"//')
echo "Response type: $RESPONSE_TYPE"
echo ""

if [ "$RESPONSE_TYPE" = "QUICK_FORM" ]; then
  echo "✅ Quick form correctly shown on first entry to INFORMATION_COLLECTION"
else
  echo "❌ Expected QUICK_FORM but got: $RESPONSE_TYPE"
fi
echo ""

# 5. Submit complete quick form data
echo "5. Submitting complete quick form..."
FORM_RESPONSE=$(curl -s -X POST "$BASE_URL/api/chat/message" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "폼 제출",
    "formData": {
      "destination": "제주도",
      "startDate": "2024-12-20",
      "endDate": "2024-12-23",
      "budget": "1000000",
      "travelStyle": "relaxation",
      "companions": "couple"
    }
  }')
echo "Form submission response:"
echo "$FORM_RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$FORM_RESPONSE"
echo ""

# Extract response type after form submission
RESPONSE_TYPE_AFTER=$(echo "$FORM_RESPONSE" | grep -o '"responseType":"[^"]*' | sed 's/"responseType":"//')
echo "Response type after form submission: $RESPONSE_TYPE_AFTER"
echo ""

if [ "$RESPONSE_TYPE_AFTER" = "TEXT" ]; then
  echo "✅ Quick form NOT shown after submission - Fixed!"
  echo "✅ The infinite loop issue has been resolved!"
else
  echo "❌ Still showing $RESPONSE_TYPE_AFTER after form submission"
  echo "❌ The infinite loop issue persists"
fi

echo ""
echo "=== Test Complete ==="
