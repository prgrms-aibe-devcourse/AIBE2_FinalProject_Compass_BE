# Chat Input Parser API Documentation

## Overview
REQ-AI-002: NER(Named Entity Recognition) 기반 자연어 여행 정보 추출 기능

CHAT 도메인에서 Spring AI를 활용하여 사용자의 채팅 메시지에서 여행 관련 정보를 자동으로 추출하고 구조화합니다.

## Features
- 🗓️ **날짜 파싱**: 절대/상대 날짜, 한국어 날짜 형식, N박M일 형식 지원
- 💰 **예산 정규화**: 원화/달러, 만원/천원 단위 자동 변환
- 📍 **목적지 추출**: 한국 주요 도시 및 지역명 인식
- 👥 **인원 파싱**: 숫자 및 설명적 표현 (혼자, 커플, 가족 등)
- 🎯 **관심사 추출**: 문화, 음식, 자연, 쇼핑, 모험 등
- 🏨 **여행 스타일**: 예산/보통/고급 자동 분류

## API Endpoints

### 1. Parse Chat Message
**Endpoint**: `POST /api/chat/parse`

채팅 메시지의 자연어 입력을 분석하여 구조화된 여행 계획 정보를 반환합니다.

#### Request
```json
{
  "text": "다음달 15일부터 3박4일로 제주도 여행을 가려고 해요. 2명이서 가는데 예산은 1인당 100만원입니다."
}
```

#### Response
```json
{
  "planId": "CHAT_1234567890",
  "destination": "제주",
  "origin": "서울",
  "startDate": "2024-04-15",
  "endDate": "2024-04-18",
  "numberOfTravelers": 2,
  "travelStyle": "moderate",
  "interests": ["nature"],
  "budget": {
    "perPerson": 1000000,
    "total": 2000000,
    "currency": "KRW"
  },
  "preferences": {
    "accommodation": "hotel_3star",
    "mealBudget": 100000,
    "transportation": "public_transport"
  },
  "summary": "2명이서 04월 15일부터 04월 18일까지 제주 여행 (예산: 1인당 100만원)",
  "metadata": {
    "parsedAt": "2024-03-10",
    "parsingMethod": "NER_PATTERN_MATCHING",
    "confidence": 0.95
  }
}
```

### 2. Raw Parsing (Structured Request Only)
**Endpoint**: `POST /api/chat/parse/raw`

파싱된 결과만 TripPlanningRequest 형식으로 반환합니다.

#### Request
```json
{
  "text": "3박4일로 부산 맛집 탐방하려고 해요"
}
```

#### Response
```json
{
  "destination": "부산",
  "origin": "서울",
  "startDate": "2024-03-17",
  "endDate": "2024-03-20",
  "numberOfTravelers": 1,
  "travelStyle": "moderate",
  "interests": ["food"],
  "budgetPerPerson": null,
  "currency": "KRW",
  "preferences": {}
}
```

### 3. Get Parsing Examples
**Endpoint**: `GET /api/chat/parse/examples`

미리 정의된 예제들의 파싱 결과를 보여줍니다.

#### Response
```json
{
  "다음달 15일부터 3박4일로 제주도 여행을 가려고 해요. 예산은 100만원입니다.": {
    "destination": "제주",
    "startDate": "2024-04-15",
    "endDate": "2024-04-18",
    "budgetPerPerson": 1000000,
    ...
  },
  "2명이서 부산 맛집 탐방하고 싶어요. 이번 주말에 1박2일로": {
    "destination": "부산",
    "numberOfTravelers": 2,
    "interests": ["food"],
    ...
  }
}
```

## Supported Patterns

### Date Formats
- **절대 날짜**: 
  - `2024년 3월 25일`
  - `2024-03-25`
  - `2024/03/25`
  - `3월 25일`
- **상대 날짜**:
  - `오늘`, `내일`, `모레`
  - `이번주`, `다음주`
  - `이번달`, `다음달`
- **기간 표현**:
  - `3박4일`, `2박3일`

### Budget Formats
- **원화**: `100만원`, `50만원`, `500000원`
- **달러**: `1000달러`, `1000USD`
- **설명적**: `저렴하게`, `고급스럽게`, `럭셔리`

### People Count
- **숫자**: `2명`, `4인`
- **설명적**: `혼자`, `둘이`, `커플`, `가족`, `단체`

### Interests Keywords
- **문화**: 문화, 박물관, 미술관, 역사, 전통, 궁, 사찰
- **음식**: 맛집, 음식, 먹거리, 카페, 레스토랑, 요리
- **모험**: 모험, 액티비티, 스포츠, 등산, 서핑, 다이빙
- **쇼핑**: 쇼핑, 면세, 아울렛, 시장, 기념품
- **자연**: 자연, 산, 바다, 해변, 공원, 트레킹

## Implementation Details

### Technology Stack
- **Spring AI 1.0.0-M5**: AI/LLM integration framework
- **Pattern Matching**: Regular expression-based entity extraction
- **Gemini/GPT-4**: Fallback for complex natural language understanding

### Processing Flow
1. **Pattern Extraction**: Use regex patterns to extract basic entities
2. **Date Normalization**: Convert relative dates to absolute dates
3. **Budget Conversion**: Normalize all currencies to KRW
4. **AI Enhancement**: Use LLM for complex cases when pattern matching fails
5. **Default Values**: Apply sensible defaults for missing information
6. **Validation**: Ensure logical consistency (e.g., end date after start date)

### Confidence Score
The parser calculates a confidence score (0.0 - 1.0) based on:
- Destination extracted: +1.0
- Origin specified: +0.5
- Start date found: +1.0
- End date found: +1.0
- Traveler count: +0.5
- Budget specified: +1.0
- Travel style: +0.5
- Interests identified: +1.0

### Error Handling
- **Invalid Dates**: Automatically swap if end date is before start date
- **Missing Origin**: Default to "서울"
- **Missing Dates**: Default to 7 days from today for start, +2 days for end
- **Missing Budget**: No default, left as null
- **Missing People**: Default to 1 traveler

## Usage Examples

### Example 1: Complete Input
```
Input: "다음달 15일부터 3박4일로 제주도 여행을 가려고 해요. 2명이서 가는데 예산은 1인당 50만원 정도로 생각하고 있어요. 맛집 탐방이랑 해변에서 휴양하는 걸 좋아해요."

Extracted:
- Destination: 제주
- Dates: Next month 15th + 3 nights
- People: 2
- Budget: 500,000 KRW per person
- Interests: food, nature
- Travel Style: moderate
```

### Example 2: Minimal Input
```
Input: "부산 여행 가고 싶어요"

Extracted:
- Destination: 부산
- Origin: 서울 (default)
- Dates: 7 days from today (default start), +2 days (default duration)
- People: 1 (default)
- Travel Style: moderate (default)
```

### Example 3: Budget-focused Input
```
Input: "저렴하게 강릉 바다 보러 가고 싶어요"

Extracted:
- Destination: 강릉
- Interests: nature
- Travel Style: budget
- Accommodation preference: hostel (inferred from budget style)
```

## Testing

Run unit tests:
```bash
./gradlew test --tests "ChatInputParserTest"
```

Run integration tests:
```bash
./gradlew test --tests "ChatParsingControllerIntegrationTest"
```

## Future Enhancements
- [ ] Multi-language support (English, Chinese, Japanese)
- [ ] International destination recognition
- [ ] Flight schedule integration
- [ ] Seasonal activity recommendations
- [ ] Group travel preferences
- [ ] Travel insurance options
- [ ] Visa requirement checking