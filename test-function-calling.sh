#!/bin/bash

# Test Function Calling API
# This script tests the Function Calling endpoints

echo "=== Function Calling API Test ==="
echo ""

# Base URL
BASE_URL="http://localhost:8080"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Function to test an endpoint
test_endpoint() {
    local endpoint=$1
    local data=$2
    local description=$3
    
    echo "Testing: $description"
    echo "Endpoint: $endpoint"
    echo "Request: $data"
    echo ""
    
    response=$(curl -s -X POST "$BASE_URL$endpoint" \
        -H "Content-Type: application/json" \
        -d "$data" 2>&1)
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ Response received:${NC}"
        echo "$response" | jq . 2>/dev/null || echo "$response"
    else
        echo -e "${RED}❌ Request failed${NC}"
        echo "$response"
    fi
    
    echo ""
    echo "---"
    echo ""
}

# Test 1: Check if available functions endpoint works
echo "1. Checking available functions..."
curl -s "$BASE_URL/api/test/function/available" | jq . 2>/dev/null || echo "Endpoint not available"
echo ""
echo "---"
echo ""

# Test 2: Natural language travel request - 당일치기
test_endpoint "/api/test/function/travel/natural" \
    '{"message": "성수로 당일치기 여행 가고 싶어"}' \
    "Natural Language - 당일치기 (Day Trip)"

# Test 3: 1박2일 travel request
test_endpoint "/api/test/function/travel/natural" \
    '{"message": "제주도로 이틀동안 여행 가려고 해. 맛집이랑 카페 추천해줘"}' \
    "Natural Language - 1박2일 (2 Days)"

# Test 4: 2박3일 travel request with budget
test_endpoint "/api/test/function/travel/natural" \
    '{"message": "부산 2박3일 여행 계획 짜줘. 예산은 50만원이야"}' \
    "Natural Language - 2박3일 with Budget"

# Test 5: Weather function
test_endpoint "/api/test/function/gemini" \
    '{"message": "오늘 서울 날씨 어때?"}' \
    "Gemini - Weather Query"

# Test 6: Flight search
test_endpoint "/api/test/function/gemini" \
    '{"message": "서울에서 제주도 가는 항공편 찾아줘"}' \
    "Gemini - Flight Search"

# Test 7: Hotel search
test_endpoint "/api/test/function/gemini" \
    '{"message": "제주도의 호텔을 추천해줘"}' \
    "Gemini - Hotel Recommendation"

echo "=== Test Complete ==="