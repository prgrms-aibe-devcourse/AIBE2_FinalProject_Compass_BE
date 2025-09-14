---
name: 기능 개발
about: 기능명세서 기반 개발 작업을 위한 이슈 템플릿
title: '[SEARCH] REQ-SEARCH-001 | RDS 검색 (PostgreSQL 전문검색)'
labels: '백엔드'
assignees: 'SEARCH1'
---

## 📋 기능 개요
**요구사항 ID**: REQ-SEARCH-001
PostgreSQL의 전문검색 기능을 활용한 RDS 기반 장소 검색 시스템을 구현합니다.

## 🎯 개발 목표
- PostgreSQL의 `to_tsvector`와 `plainto_tsquery`를 활용한 전문검색 기능을 제공합니다.
- 지리적 검색을 위한 `earth_distance`, `earth_box` 함수를 활용합니다.
- 카테고리, 지역별 필터링을 지원합니다.
- 페이지네이션을 지원합니다.

## 📝 기능 명세
### API Endpoints
- **`GET /api/search/places/fulltext`**: 기본 전문검색
- **`GET /api/search/places/fulltext/category`**: 카테고리 필터 전문검색
- **`GET /api/search/places/fulltext/area`**: 지역 필터 전문검색
- **`GET /api/search/places/fulltext/filters`**: 복합 필터 전문검색
- **`GET /api/search/places/nearby`**: 지리적 검색
- **`GET /api/search/places/name`**: 이름 검색
- **`GET /api/search/places/{contentId}`**: ID 검색
- **`GET /api/search/places/category/{category}`**: 카테고리별 검색
- **`GET /api/search/places/area/{areaCode}`**: 지역별 검색
- **`GET /api/search/places/stats/categories`**: 인기 카테고리 통계
- **`GET /api/search/places/stats/areas`**: 지역별 분포 통계

### Request Parameters
```json
{
  "query": "경복궁",
  "category": "문화시설",
  "areaCode": "1",
  "latitude": 37.5665,
  "longitude": 126.9780,
  "radius": 5000,
  "page": 1,
  "size": 15
}
```

### Response Body
```json
{
  "content": [
    {
      "id": 1,
      "contentId": "126508",
      "name": "경복궁",
      "address": "서울특별시 종로구 사직로 161",
      "category": "문화시설",
      "latitude": 37.579617,
      "longitude": 126.977041,
      "overview": "조선 왕조의 법궁"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "currentPage": 0,
  "size": 15
}
```

## 🔧 구현 사항
### Entity
- [x] `TourPlace.java` 생성 (관광지 정보 엔티티)

### Repository
- [x] `TourPlaceRepository.java` 생성 (JPA 리포지토리)
- [x] 전문검색을 위한 네이티브 쿼리 메서드 구현
- [x] 지리적 검색을 위한 공간 쿼리 메서드 구현

### Service
- [x] `SearchService.java` 생성 (검색 비즈니스 로직)
- [x] 전문검색 메서드 구현
- [x] 지리적 검색 메서드 구현
- [x] 필터링 메서드 구현

### Controller
- [x] `SearchController.java` 생성 (REST API 엔드포인트)
- [x] Swagger 문서화 적용

### Database
- [x] `data.sql` 생성 (H2 테스트용 샘플 데이터)
- [x] `application.yml` 설정 (H2 데이터베이스 설정)

## ✅ 완료 조건
- [x] PostgreSQL 전문검색 기능 구현
- [x] 지리적 검색 기능 구현
- [x] 카테고리, 지역별 필터링 구현
- [x] 페이지네이션 구현
- [x] REST API 엔드포인트 구현
- [x] Swagger 문서화 완료

## 📌 참고사항
- PostgreSQL의 `to_tsvector`와 `plainto_tsquery`를 활용한 전문검색
- `earth_distance`, `earth_box` 함수를 활용한 지리적 검색
- H2 데이터베이스로 테스트 가능하도록 구현
- 향후 PostgreSQL로 전환 시 `jsonb` 타입 활용 예정