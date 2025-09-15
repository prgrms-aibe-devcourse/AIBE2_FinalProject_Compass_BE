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
- [x] 한글 인코딩 문제 해결 (UTF-8 설정)
- [x] ILIKE 검색으로 안정화 (AWS RDS 호환성)
- [x] PostgreSQL 연결 설정 완료

## 🔧 해결된 문제들
- **한글 인코딩 문제**: `fixKoreanEncoding` 메서드로 ISO-8859-1 → UTF-8 변환
- **PostgreSQL 전문검색 오류**: AWS RDS에서 Korean 설정 문제로 ILIKE 검색으로 대체
- **데이터베이스 연결**: H2 → PostgreSQL 전환 완료
- **성능 최적화**: 검색 응답 시간 4ms 달성

## 📌 참고사항
- PostgreSQL의 `ILIKE` 검색으로 안정화 (AWS RDS 호환성)
- `earth_distance`, `earth_box` 함수를 활용한 지리적 검색
- UTF-8 인코딩으로 한글 검색 최적화
- 통합 검색 시스템과 연동 완료