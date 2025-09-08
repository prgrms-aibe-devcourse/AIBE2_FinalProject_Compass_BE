---
name: 기능 개발
about: 기능명세서 기반 개발 작업을 위한 이슈 템플릿
title: '[TRIP] REQ-TRIP-003 | 내 여행 목록 조회 API'
labels: '백엔드'
assignees: 'TRIP1'
---

## 📋 기능 개요
**요구사항 ID**: REQ-TRIP-003
현재 로그인한 사용자의 여행 계획 목록을 JWT 인증을 통해 안전하게 조회하는 API를 구현합니다.

## 🎯 개발 목표
- `GET /api/trips` 엔드포인트를 통해 현재 로그인한 사용자의 여행 계획 목록을 조회하는 기능을 제공합니다.
- JWT 인증을 통한 보안 강화로 사용자별 데이터 격리를 보장합니다.
- 페이징을 통한 효율적인 데이터 조회 기능을 제공합니다.
- 인증되지 않은 접근을 차단하여 보안을 강화합니다.

## 📝 기능 명세
### API Endpoints
- **`GET /api/trips`**: 현재 로그인한 사용자의 여행 계획 목록을 조회합니다.

### Request
- **Headers**: `Authorization: Bearer <JWT_TOKEN>` (필수)
- **Query Parameter**: `page` (Integer, optional, default=0) - 페이지 번호
- **Query Parameter**: `size` (Integer, optional, default=10) - 페이지 크기
- **Query Parameter**: `sort` (String, optional, default=createdAt,desc) - 정렬 기준

### Response Body (성공)
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

### Response Body (실패 - 인증 실패)
```json
{
  "error": "UNAUTHORIZED",
  "message": "인증이 필요합니다.",
  "timestamp": "2024-08-01T10:00:00"
}
```

## 🔧 구현 사항
### DTO
- [x] 기존 `TripList.java` 활용 (목록 조회용 Response DTO)

### Service
- [x] `TripService.getTripsByUserEmail()`: JWT 인증된 사용자의 여행 계획 목록 조회 비즈니스 로직 구현

### Repository
- [x] `TripRepository.findByUserEmailOrderByCreatedAtDesc()`: 사용자 이메일 기반 여행 계획 조회 쿼리 구현

### Controller
- [x] `TripController`: `GET /api/trips` 엔드포인트를 JWT 인증 기반으로 수정
- [x] `Authentication` 객체를 통한 사용자 정보 추출

### Security
- [x] JWT 인증 필터를 통한 자동 인증 처리
- [x] 인증되지 않은 요청 차단 (302 리다이렉트)

### Test
- [x] `TripControllerTest`: JWT 인증 기반 여행 목록 조회 테스트 케이스 추가
- [x] 인증 없는 접근 차단 테스트 케이스 추가

## ✅ 완료 조건
- [x] `GET /api/trips` API JWT 인증 기반으로 정상 동작
- [x] 현재 로그인한 사용자의 데이터만 조회 가능
- [x] 페이징 기능 정상 동작
- [x] 인증되지 않은 접근 차단
- [x] 보안 취약점 해결 (userId 파라미터 제거)

## 📌 참고사항
- REQ-TRIP-002에서 구현된 여행 계획 목록 조회 API의 보안 강화 버전입니다.
- 기존의 `userId` 쿼리 파라미터를 제거하고 JWT 인증을 통해 사용자를 식별합니다.
- Spring Security의 `Authentication` 객체를 활용하여 현재 로그인한 사용자 정보를 추출합니다.
- 여행 계획 목록 조회는 생성일 기준 내림차순으로 정렬됩니다.
