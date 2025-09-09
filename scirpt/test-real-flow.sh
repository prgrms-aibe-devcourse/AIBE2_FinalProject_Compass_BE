#!/bin/bash

# API 엔드포인트 기본 URL
BASE_URL="http://localhost:8081/api"

# 색상 코드
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  여행 정보 수집 플로우 실제 테스트${NC}"
echo -e "${BLUE}========================================${NC}\n"

# 1. 회원가입
echo -e "${YELLOW}[1/7] 테스트 사용자 회원가입...${NC}"
SIGNUP_RESPONSE=$(curl -s -X POST "${BASE_URL}/auth/signup" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test.flow@example.com",
    "password": "Test1234!",
    "nickname": "플로우테스터"
  }')
echo "회원가입 응답: $SIGNUP_RESPONSE"
echo ""

# 2. 로그인
echo -e "${YELLOW}[2/7] 로그인 및 JWT 토큰 획득...${NC}"
LOGIN_RESPONSE=$(curl -s -X POST "${BASE_URL}/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test.flow@example.com",
    "password": "Test1234!"
  }')

JWT_TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.accessToken')
echo "JWT 토큰: ${JWT_TOKEN:0:50}..."
echo ""

# 3. 정보 수집 시작
echo -e "${YELLOW}[3/7] 여행 정보 수집 시작...${NC}"
START_RESPONSE=$(curl -s -X POST "${BASE_URL}/chat/collect-info" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "chatThreadId": null,
    "initialMessage": "여행 계획을 세우고 싶어요"
  }')

SESSION_ID=$(echo $START_RESPONSE | jq -r '.sessionId')
echo "세션 ID: $SESSION_ID"
echo "첫 질문: $(echo $START_RESPONSE | jq -r '.primaryQuestion')"
echo "현재 단계: $(echo $START_RESPONSE | jq -r '.currentStep')"
echo ""

# 4. 출발지 응답
echo -e "${YELLOW}[4/7] 출발지 응답 (서울)...${NC}"
ORIGIN_RESPONSE=$(curl -s -X POST "${BASE_URL}/chat/follow-up" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"sessionId\": \"$SESSION_ID\",
    \"userResponse\": \"서울\"
  }")

echo "다음 질문: $(echo $ORIGIN_RESPONSE | jq -r '.primaryQuestion')"
echo "진행률: $(echo $ORIGIN_RESPONSE | jq -r '.progressPercentage')%"
echo ""

# 5. 목적지 응답
echo -e "${YELLOW}[5/7] 목적지 응답 (제주도)...${NC}"
DEST_RESPONSE=$(curl -s -X POST "${BASE_URL}/chat/follow-up" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"sessionId\": \"$SESSION_ID\",
    \"userResponse\": \"제주도\"
  }")

echo "다음 질문: $(echo $DEST_RESPONSE | jq -r '.primaryQuestion')"
echo "진행률: $(echo $DEST_RESPONSE | jq -r '.progressPercentage')%"
echo ""

# 6. 날짜 응답
echo -e "${YELLOW}[6/7] 날짜 응답 (2024-03-15 ~ 2024-03-17)...${NC}"
DATE_RESPONSE=$(curl -s -X POST "${BASE_URL}/chat/follow-up" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"sessionId\": \"$SESSION_ID\",
    \"userResponse\": \"2024-03-15 ~ 2024-03-17\"
  }")

echo "다음 질문: $(echo $DATE_RESPONSE | jq -r '.primaryQuestion')"
echo "진행률: $(echo $DATE_RESPONSE | jq -r '.progressPercentage')%"
echo ""

# 7. 동행자 응답
echo -e "${YELLOW}[7/7] 동행자 응답 (가족 4명)...${NC}"
COMPANION_RESPONSE=$(curl -s -X POST "${BASE_URL}/chat/follow-up" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"sessionId\": \"$SESSION_ID\",
    \"userResponse\": \"가족 4명이서 갈 예정이에요\"
  }")

echo "다음 질문: $(echo $COMPANION_RESPONSE | jq -r '.primaryQuestion')"
echo "진행률: $(echo $COMPANION_RESPONSE | jq -r '.progressPercentage')%"
echo ""

# 8. 예산 응답
echo -e "${YELLOW}[8/8] 예산 응답 (1인당 100만원)...${NC}"
BUDGET_RESPONSE=$(curl -s -X POST "${BASE_URL}/chat/follow-up" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"sessionId\": \"$SESSION_ID\",
    \"userResponse\": \"1인당 100만원 정도 생각하고 있어요\"
  }")

echo "응답: $(echo $BUDGET_RESPONSE | jq '.')"
echo ""

# 9. 수집 상태 확인
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  최종 수집 상태 확인${NC}"
echo -e "${GREEN}========================================${NC}"

STATUS_RESPONSE=$(curl -s -X GET "${BASE_URL}/chat/collection-status/$SESSION_ID" \
  -H "Authorization: Bearer $JWT_TOKEN")

echo -e "${BLUE}수집된 정보:${NC}"
echo "- 출발지: $(echo $STATUS_RESPONSE | jq -r '.collectedInfo.origin')"
echo "- 목적지: $(echo $STATUS_RESPONSE | jq -r '.collectedInfo.destination')"
echo "- 시작일: $(echo $STATUS_RESPONSE | jq -r '.collectedInfo.startDate')"
echo "- 종료일: $(echo $STATUS_RESPONSE | jq -r '.collectedInfo.endDate')"
echo "- 기간: $(echo $STATUS_RESPONSE | jq -r '.collectedInfo.durationNights')박"
echo "- 인원: $(echo $STATUS_RESPONSE | jq -r '.collectedInfo.numberOfTravelers')명"
echo "- 동행자 유형: $(echo $STATUS_RESPONSE | jq -r '.collectedInfo.companionType')"
echo "- 예산: $(echo $STATUS_RESPONSE | jq -r '.collectedInfo.budgetPerPerson')원"
echo "- 완료율: $(echo $STATUS_RESPONSE | jq -r '.completionPercentage')%"

echo -e "\n${GREEN}✅ 테스트 완료!${NC}"