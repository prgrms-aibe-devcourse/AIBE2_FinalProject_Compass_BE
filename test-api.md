# API Test Commands for Function Calling

## Prerequisites
1. Make sure PostgreSQL and Redis are running:
```bash
docker-compose up -d postgres redis
```

2. Run the application with environment variables:
```bash
# Option 1: Using Docker
docker-compose up -d

# Option 2: Running locally (requires .env file)
./gradlew bootRun
```

## Test Endpoints

### 1. Check Available Functions
```bash
curl -X GET http://localhost:8080/api/test/function/available | jq
```

### 2. Test Natural Language Travel Request (당일치기)
```bash
curl -X POST http://localhost:8080/api/test/function/travel/natural \
  -H "Content-Type: application/json" \
  -d '{
    "message": "성수로 당일치기 여행 가고 싶어"
  }' | jq
```

### 3. Test 1박2일 Travel Request
```bash
curl -X POST http://localhost:8080/api/test/function/travel/natural \
  -H "Content-Type: application/json" \
  -d '{
    "message": "제주도로 이틀동안 여행 가려고 해. 맛집이랑 카페 추천해줘"
  }' | jq
```

### 4. Test 2박3일 Travel Request
```bash
curl -X POST http://localhost:8080/api/test/function/travel/natural \
  -H "Content-Type: application/json" \
  -d '{
    "message": "부산 2박3일 여행 계획 짜줘. 예산은 50만원이야"
  }' | jq
```

### 5. Test Weather Function
```bash
curl -X POST http://localhost:8080/api/test/function/gemini \
  -H "Content-Type: application/json" \
  -d '{
    "message": "오늘 서울 날씨 어때?"
  }' | jq
```

### 6. Test Flight Search
```bash
curl -X POST http://localhost:8080/api/test/function/gemini \
  -H "Content-Type: application/json" \
  -d '{
    "message": "서울에서 제주도 가는 항공편 찾아줘"
  }' | jq
```

### 7. Test Hotel Search
```bash
curl -X POST http://localhost:8080/api/test/function/gemini \
  -H "Content-Type: application/json" \
  -d '{
    "message": "제주도의 호텔을 추천해줘"
  }' | jq
```

### 8. Test Restaurant Search
```bash
curl -X POST http://localhost:8080/api/test/function/gemini \
  -H "Content-Type: application/json" \
  -d '{
    "message": "부산의 맛집을 찾아줘"
  }' | jq
```

### 9. Test Complex Travel Planning
```bash
curl -X POST http://localhost:8080/api/test/function/trip/plan \
  -H "Content-Type: application/json" \
  -d '{
    "destination": "제주도",
    "startDate": "2024-12-25",
    "endDate": "2024-12-27",
    "preferences": {
      "budget": "medium",
      "interests": "nature, food",
      "travelStyle": "relaxed"
    }
  }' | jq
```

### 10. Test Date Range Parsing
```bash
curl -X POST http://localhost:8080/api/test/function/travel/natural \
  -H "Content-Type: application/json" \
  -d '{
    "message": "12월 28일부터 30일까지 강릉 여행"
  }' | jq
```

## Using Postman

1. Import this collection to Postman:

### Collection JSON:
```json
{
  "info": {
    "name": "Compass Function Calling Tests",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Check Available Functions",
      "request": {
        "method": "GET",
        "header": [],
        "url": {
          "raw": "http://localhost:8080/api/test/function/available",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "test", "function", "available"]
        }
      }
    },
    {
      "name": "Natural Language - 당일치기",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n    \"message\": \"성수로 당일치기 여행 가고 싶어\"\n}"
        },
        "url": {
          "raw": "http://localhost:8080/api/test/function/travel/natural",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "test", "function", "travel", "natural"]
        }
      }
    },
    {
      "name": "Gemini - Weather",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n    \"message\": \"오늘 서울 날씨 어때?\"\n}"
        },
        "url": {
          "raw": "http://localhost:8080/api/test/function/gemini",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "test", "function", "gemini"]
        }
      }
    }
  ]
}
```

## Expected Responses

### Successful Response:
```json
{
  "success": true,
  "response": "AI가 생성한 여행 계획이나 답변...",
  "model": "gemini-2.0-flash"
}
```

### Error Response:
```json
{
  "success": false,
  "error": "Error message..."
}
```

## Troubleshooting

1. If you get connection refused:
   - Check if the application is running on port 8080
   - Verify PostgreSQL and Redis are running

2. If you get API key errors:
   - Make sure .env file contains valid API keys
   - Check OPENAI_API_KEY and GOOGLE_APPLICATION_CREDENTIALS

3. For detailed logs:
   - Check application console output
   - Look for log messages starting with "=== Testing"