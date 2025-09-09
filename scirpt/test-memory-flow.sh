#!/bin/bash

# Memory-based Template Collection Flow Test Script
# 메모리 기반 템플릿 수집 테스트 스크립트

API_URL="http://localhost:8081/api"
AUTH_HEADER="Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwiaWF0IjoxNzMzNzI0MzUwLCJleHAiOjE3MzYzMTYzNTB9.Cr2cVlHSvK7DojGkCwekfzPjj-r0TShZ6WGqPy8lXSY"

echo "=========================================="
echo "메모리 기반 여행 정보 수집 테스트"
echo "Memory-based Travel Info Collection Test"
echo "=========================================="

# Step 1: 수집 시작 (Start Collection)
echo -e "\n[1/8] 정보 수집 시작 (Starting collection)..."
START_RESPONSE=$(curl -s -X POST "$API_URL/chat/template/start" \
  -H "$AUTH_HEADER" \
  -H "Content-Type: application/json" \
  -d '{
    "chatThreadId": "test-thread-001",
    "initialMessage": "서울에서 제주도로 3박 4일 여행을 가고 싶어요"
  }')

echo "$START_RESPONSE" | jq '.'
SESSION_ID=$(echo "$START_RESPONSE" | jq -r '.sessionId')
echo "📝 Session ID: $SESSION_ID"

# Step 2: 첫 번째 응답 - 출발지 (First response - Origin)
echo -e "\n[2/8] 출발지 확인 (Confirming origin)..."
UPDATE1=$(curl -s -X POST "$API_URL/chat/template/update" \
  -H "$AUTH_HEADER" \
  -H "Content-Type: application/json" \
  -d "{
    \"sessionId\": \"$SESSION_ID\",
    \"userResponse\": \"서울 강남구에서 출발할 예정입니다\"
  }")

echo "$UPDATE1" | jq '.'
COMPLETION=$(echo "$UPDATE1" | jq -r '.completionPercentage')
echo "📊 완성도: $COMPLETION%"

# Step 3: 두 번째 응답 - 목적지 (Second response - Destination)
echo -e "\n[3/8] 목적지 확인 (Confirming destination)..."
UPDATE2=$(curl -s -X POST "$API_URL/chat/template/update" \
  -H "$AUTH_HEADER" \
  -H "Content-Type: application/json" \
  -d "{
    \"sessionId\": \"$SESSION_ID\",
    \"userResponse\": \"제주도 전체를 둘러보고 싶어요\"
  }")

echo "$UPDATE2" | jq '.'
COMPLETION=$(echo "$UPDATE2" | jq -r '.completionPercentage')
echo "📊 완성도: $COMPLETION%"

# Step 4: 세 번째 응답 - 날짜 (Third response - Dates)
echo -e "\n[4/8] 여행 날짜 입력 (Entering travel dates)..."
UPDATE3=$(curl -s -X POST "$API_URL/chat/template/update" \
  -H "$AUTH_HEADER" \
  -H "Content-Type: application/json" \
  -d "{
    \"sessionId\": \"$SESSION_ID\",
    \"userResponse\": \"2024년 3월 15일부터 18일까지 갈 예정입니다\"
  }")

echo "$UPDATE3" | jq '.'
COMPLETION=$(echo "$UPDATE3" | jq -r '.completionPercentage')
echo "📊 완성도: $COMPLETION%"

# Step 5: 네 번째 응답 - 동행자 (Fourth response - Companions)
echo -e "\n[5/8] 동행자 정보 입력 (Entering companion info)..."
UPDATE4=$(curl -s -X POST "$API_URL/chat/template/update" \
  -H "$AUTH_HEADER" \
  -H "Content-Type: application/json" \
  -d "{
    \"sessionId\": \"$SESSION_ID\",
    \"userResponse\": \"가족 4명이 함께 갑니다\"
  }")

echo "$UPDATE4" | jq '.'
COMPLETION=$(echo "$UPDATE4" | jq -r '.completionPercentage')
echo "📊 완성도: $COMPLETION%"

# Step 6: 다섯 번째 응답 - 예산 (Fifth response - Budget)
echo -e "\n[6/8] 예산 입력 (Entering budget)..."
UPDATE5=$(curl -s -X POST "$API_URL/chat/template/update" \
  -H "$AUTH_HEADER" \
  -H "Content-Type: application/json" \
  -d "{
    \"sessionId\": \"$SESSION_ID\",
    \"userResponse\": \"1인당 50만원 정도 생각하고 있습니다\"
  }")

echo "$UPDATE5" | jq '.'
COMPLETION=$(echo "$UPDATE5" | jq -r '.completionPercentage')
CAN_GENERATE=$(echo "$UPDATE5" | jq -r '.canGeneratePlan')
echo "📊 완성도: $COMPLETION%"
echo "🎯 계획 생성 가능: $CAN_GENERATE"

# Step 7: 상태 확인 (Check status)
echo -e "\n[7/8] 현재 상태 확인 (Checking current status)..."
STATUS=$(curl -s -X GET "$API_URL/chat/template/status/$SESSION_ID" \
  -H "$AUTH_HEADER")

echo "$STATUS" | jq '.'
echo "📋 수집된 정보:"
echo "$STATUS" | jq '.template | {origin, destination, startDate, endDate, numberOfTravelers, companionType, budgetPerPerson}'

# Step 8: 여행 계획 생성 (Generate travel plan)
if [ "$CAN_GENERATE" = "true" ]; then
  echo -e "\n[8/8] 🎉 여행 계획 생성 (Generating travel plan)..."
  PLAN=$(curl -s -X POST "$API_URL/chat/template/generate-plan" \
    -H "$AUTH_HEADER" \
    -H "Content-Type: application/json" \
    -d "{
      \"sessionId\": \"$SESSION_ID\"
    }")
  
  echo "$PLAN" | jq '.'
  SUCCESS=$(echo "$PLAN" | jq -r '.success')
  
  if [ "$SUCCESS" = "true" ]; then
    echo "✅ 여행 계획 생성 완료!"
    echo "📄 계획 내용:"
    echo "$PLAN" | jq -r '.plan'
  else
    echo "❌ 계획 생성 실패:"
    echo "$PLAN" | jq -r '.error'
  fi
else
  echo -e "\n⚠️  아직 필수 정보가 부족하여 계획을 생성할 수 없습니다."
  echo "Missing fields:"
  echo "$STATUS" | jq '.missingFields'
fi

echo -e "\n=========================================="
echo "테스트 완료 (Test completed)"
echo "=========================================="
echo "특징:"
echo "- ✅ DB 접근 최소화 (메모리 기반 수집)"
echo "- ✅ 템플릿 기반 정보 수집"
echo "- ✅ 완성도 추적 및 실시간 상태 확인"
echo "- ✅ 모든 정보 수집 후 한 번에 계획 생성"
echo "=========================================="