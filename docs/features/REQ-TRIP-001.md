---
name: 기능 개발
about: 기능명세서 기반 개발 작업을 위한 이슈 템플릿
title: '[TRIP] REQ-TRIP-001 | 여행 계획 생성 API'
labels: '백엔드'
assignees: 'TRIP1'
---

## 📋 기능 개요
**요구사항 ID**: REQ-TRIP-001
AI 기반으로 생성된 여행 계획 데이터를 받아 데이터베이스에 저장하는 API를 구현합니다.

## 🎯 개발 목표
- `POST /api/trips` 엔드포인트를 통해 여행 계획을 생성하는 기능을 제공합니다.
- 요청 데이터를 검증하고, 유효한 경우에만 데이터를 저장합니다.
- 생성된 여행 계획의 고유 ID를 응답으로 반환합니다.
- Swagger를 통해 API 명세를 명확하게 문서화합니다.

## 📝 기능 명세
### API Endpoints
- **`POST /api/trips`**: 새로운 여행 계획을 생성합니다.

### Request Body
```json
{
  "userId": 1,
  "threadId": 101,
  "title": "서울 3박 4일 여행",
  "destination": "서울",
  "startDate": "2024-09-01",
  "endDate": "2024-09-04",
  "numberOfPeople": 2,
  "totalBudget": 1000000,
  "dailyPlans": [
    {
      "dayNumber": 1,
      "activityDate": "2024-09-01",
      "activities": [
        {
          "activityTime": "09:00",
          "placeName": "경복궁",
          "category": "관광지",
          "description": "조선 왕조의 법궁",
          "estimatedCost": 3000,
          "address": "서울특별시 종로구 사직로 161",
          "latitude": 37.579617,
          "longitude": 126.977041,
          "tips": "한복을 입으면 무료 입장",
          "displayOrder": 1
        }
      ]
    }
  ]
}
```

### Response Body
```json
{
  "id": 1,
  "tripUuid": "a1b2c3d4-e5f6-7890-1234-567890abcdef"
}
```

## 🔧 구현 사항
### DTO
- [x] `TripCreate.java` 생성 (`Request`, `Response` 포함)

### Service
- [x] `TripService.createTrip()`: 여행 계획 생성 비즈니스 로직 구현

### Controller
- [x] `TripController`: `POST /api/trips` 엔드포인트 구현

### Test
- [x] H2 데이터베이스 의존성 추가 및 테스트용 `application.yml` 설정

## ✅ 완료 조건
- [x] `POST /api/trips` API 정상 동작
- [x] 요청 데이터에 대한 유효성 검증(@Valid) 적용
- [x] Swagger 문서화 완료

## 📌 참고사항
- `TRIP2`의 AI Function 호출 결과로 생성된 데이터가 이 API를 통해 최종적으로 저장됩니다.
- 초기 구현에서는 사용자 인증(Authentication)을 직접 처리하지 않으며, `userId`를 요청 DTO에 포함하여 받습니다. 추후 Spring Security 적용 시 인증된 사용자 정보를 활용하도록 수정될 예정입니다.
