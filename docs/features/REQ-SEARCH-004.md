---
name: 기능 개발
about: 기능명세서 기반 개발 작업을 위한 이슈 템플릿
title: '[SEARCH] REQ-SEARCH-004 | 통합 검색 서비스'
labels: '백엔드'
assignees: 'SEARCH4'
---

## 📋 기능 개요
**요구사항 ID**: REQ-SEARCH-004
RDS 검색, Tour API 검색, Kakao Map API 검색을 통합한 종합 검색 시스템을 구현합니다. 사용자의 검색 요청에 따라 최적의 검색 결과를 제공하는 통합 검색 서비스입니다.

## 🎯 개발 목표
- 3가지 검색 시스템을 통합한 종합 검색 기능을 제공합니다.
- 검색 우선순위 설정 및 폴백 메커니즘을 구현합니다.
- 비동기 검색 실행으로 성능을 최적화합니다.
- 통합된 검색 결과를 제공하고 정렬합니다.

## 📝 기능 명세
### API Endpoints
- **`POST /api/search/integrated`**: 통합 검색 (POST 방식)
- **`GET /api/search/integrated`**: 간편 통합 검색 (GET 방식)
- **`GET /api/search/integrated/rds-first`**: RDS 우선 통합 검색
- **`GET /api/search/integrated/tour-api-first`**: Tour API 우선 통합 검색
- **`GET /api/search/integrated/kakao-map-first`**: Kakao Map API 우선 통합 검색
- **`GET /api/search/integrated/statistics`**: 검색 통계

### Request Body (POST)
```json
{
  "keyword": "경복궁",
  "searchType": "ALL",
  "category": "문화시설",
  "areaCode": "1",
  "longitude": 126.9780,
  "latitude": 37.5665,
  "radius": 5000,
  "page": 1,
  "size": 15,
  "sort": "ACCURACY",
  "priority": "RDS_FIRST"
}
```

### Response Body
```json
{
  "keyword": "경복궁",
  "totalCount": 3,
  "currentPage": 1,
  "totalPages": 1,
  "pageSize": 15,
  "searchTimeMs": 150,
  "results": [
    {
      "id": "126508",
      "name": "경복궁",
      "address": "서울특별시 종로구 사직로 161",
      "category": "문화시설",
      "longitude": 126.977041,
      "latitude": 37.579617,
      "distance": 500.0,
      "searchSystem": "RDS",
      "confidenceScore": 0.9
    }
  ],
  "systemStats": {
    "RDS": {
      "systemName": "RDS",
      "resultCount": 1,
      "searchTimeMs": 50,
      "success": true
    },
    "TOUR_API": {
      "systemName": "TOUR_API",
      "resultCount": 1,
      "searchTimeMs": 80,
      "success": true
    },
    "KAKAO_MAP": {
      "systemName": "KAKAO_MAP",
      "resultCount": 1,
      "searchTimeMs": 70,
      "success": true
    }
  },
  "metadata": {
    "searchType": "ALL",
    "priority": "RDS_FIRST",
    "usedSystems": ["RDS", "TOUR_API", "KAKAO_MAP"],
    "filters": {
      "category": "문화시설",
      "areaCode": "1"
    },
    "searchHints": [
      "통합 검색으로 다양한 검색 시스템의 결과를 확인하세요",
      "검색 우선순위를 변경하여 원하는 결과를 우선적으로 볼 수 있습니다"
    ]
  }
}
```

## 🔧 구현 사항
### DTO
- [x] `IntegratedSearchRequest.java` 생성 (통합 검색 요청 DTO)
- [x] `IntegratedSearchResponse.java` 생성 (통합 검색 응답 DTO)
- [x] 검색 타입, 우선순위, 정렬 방식 열거형 구현

### Service
- [x] `IntegratedSearchService.java` 생성 (통합 검색 비즈니스 로직)
- [x] CompletableFuture를 활용한 비동기 검색 구현
- [x] 검색 시스템별 결과 통합 및 정렬 구현
- [x] 검색 우선순위 처리 구현

### Controller
- [x] `IntegratedSearchController.java` 생성 (REST API 엔드포인트)
- [x] `IntegratedSearchTestController.java` 생성 (테스트용 엔드포인트)
- [x] Swagger 문서화 적용

### Integration
- [x] RDS 검색 시스템 연동
- [x] Tour API 검색 시스템 연동
- [x] Kakao Map API 검색 시스템 연동

## ✅ 완료 조건
- [x] 3가지 검색 시스템 통합 구현
- [x] 검색 우선순위 설정 구현
- [x] 비동기 검색 실행 구현
- [x] 검색 결과 통합 및 정렬 구현
- [x] 검색 통계 및 메타데이터 제공
- [x] REST API 엔드포인트 구현
- [x] 테스트용 엔드포인트 구현
- [x] Swagger 문서화 완료

## 📌 참고사항
- RDS, Tour API, Kakao Map API 3가지 검색 시스템 통합
- 검색 타입: ALL, RDS, TOUR_API, KAKAO_MAP
- 검색 우선순위: RDS_FIRST, TOUR_API_FIRST, KAKAO_MAP_FIRST
- 정렬 방식: ACCURACY, DISTANCE, POPULARITY
- CompletableFuture를 활용한 비동기 처리로 성능 최적화
- 향후 검색 품질 향상 및 개인화 기능 확장 예정