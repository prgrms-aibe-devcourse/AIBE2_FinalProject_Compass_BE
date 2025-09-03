---
name: 기능 개발
about: 기능명세서 기반 개발 작업을 위한 이슈 템플릿
title: '[TRIP] REQ-TRIP-002 | 여행 계획 조회 API'
labels: '백엔드'
assignees: 'TRIP1'
---

## 📋 기능 개요
**요구사항 ID**: REQ-TRIP-002
저장된 여행 계획 데이터를 조회하는 API를 구현합니다.

## 🎯 개발 목표
- `GET /api/trips/{tripId}` 엔드포인트를 통해 특정 여행 계획을 조회하는 기능을 제공합니다.
- `GET /api/trips` 엔드포인트를 통해 사용자의 여행 계획 목록을 조회하는 기능을 제공합니다.
- 여행 계획이 존재하지 않을 경우 적절한 에러 응답을 반환합니다.
- Swagger를 통해 API 명세를 명확하게 문서화합니다.

## 📝 기능 명세
### API Endpoints
- **`GET /api/trips/{tripId}`**: 특정 여행 계획의 상세 정보를 조회합니다.
- **`GET /api/trips`**: 사용자의 여행 계획 목록을 조회합니다.

### 1. 여행 계획 상세 조회 API

#### Request
- **Path Variable**: `tripId` (Long) - 조회할 여행 계획의 ID

#### Response Body (성공)
```json
{
  "id": 1,
  "tripUuid": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
  "userId": 1,
  "threadId": 101,
  "title": "서울 3박 4일 여행",
  "destination": "서울",
  "startDate": "2024-09-01",
  "endDate": "2024-09-04",
  "numberOfPeople": 2,
  "totalBudget": 1000000,
  "status": "PLANNING",
  "version": 1,
  "createdAt": "2024-08-01T10:00:00",
  "updatedAt": "2024-08-01T10:00:00",
  "dailyPlans": [
    {
      "dayNumber": 1,
      "activityDate": "2024-09-01",
      "activities": [
        {
          "id": 1,
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

#### Response Body (실패 - 여행 계획 없음)
```json
{
  "error": "TRIP_NOT_FOUND",
  "message": "해당 여행 계획을 찾을 수 없습니다.",
  "timestamp": "2024-08-01T10:00:00"
}
```

### 2. 여행 계획 목록 조회 API

#### Request
- **Query Parameter**: `userId` (Long) - 조회할 사용자의 ID
- **Query Parameter**: `page` (Integer, optional, default=0) - 페이지 번호
- **Query Parameter**: `size` (Integer, optional, default=10) - 페이지 크기

#### Response Body (성공)
```json
{
  "content": [
    {
      "id": 1,
      "tripUuid": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
      "title": "서울 3박 4일 여행",
      "destination": "서울",
      "startDate": "2024-09-01",
      "endDate": "2024-09-04",
      "numberOfPeople": 2,
      "totalBudget": 1000000,
      "status": "PLANNING",
      "createdAt": "2024-08-01T10:00:00"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": {
      "empty": false,
      "sorted": true,
      "unsorted": false
    },
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "totalElements": 1,
  "totalPages": 1,
  "last": true,
  "first": true,
  "numberOfElements": 1,
  "size": 10,
  "number": 0,
  "sort": {
    "empty": false,
    "sorted": true,
    "unsorted": false
  },
  "empty": false
}
```

## 🔧 구현 사항
### DTO
- [x] `TripDetail.java` 수정 (Response DTO 추가)
- [x] `TripList.java` 생성 (목록 조회용 Response DTO)

### Service
- [x] `TripService.getTripById()`: 특정 여행 계획 조회 비즈니스 로직 구현
- [x] `TripService.getTripsByUserId()`: 사용자별 여행 계획 목록 조회 비즈니스 로직 구현

### Controller
- [x] `TripController`: `GET /api/trips/{tripId}` 엔드포인트 구현
- [x] `TripController`: `GET /api/trips` 엔드포인트 구현

### Exception Handling
- [x] `TripNotFoundException` 예외 클래스 생성
- [x] `GlobalExceptionHandler`에 여행 계획 관련 예외 처리 추가

### Test
- [x] `TripControllerTest`: 여행 계획 조회 API 테스트 케이스 추가

## ✅ 완료 조건
- [x] `GET /api/trips/{tripId}` API 정상 동작
- [x] `GET /api/trips` API 정상 동작 (페이징 포함)
- [x] 존재하지 않는 여행 계획 조회 시 적절한 에러 응답
- [x] Swagger 문서화 완료

## 📌 참고사항
- 초기 구현에서는 사용자 인증(Authentication)을 직접 처리하지 않으며, `userId`를 쿼리 파라미터로 받습니다. 추후 Spring Security 적용 시 인증된 사용자 정보를 활용하도록 수정될 예정입니다.
- 여행 계획 목록 조회는 생성일 기준 내림차순으로 정렬됩니다.
- REQ-TRIP-001에서 생성된 여행 계획 데이터를 조회하는 기능입니다.
