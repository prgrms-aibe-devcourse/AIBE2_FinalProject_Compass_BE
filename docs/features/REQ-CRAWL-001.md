---
name: 기능 개발
about: 기능명세서 기반 개발 작업을 위한 이슈 템플릿
title: '[CRAWL] REQ-CRAWL-001 | Tour API 클라이언트 구현'
labels: '백엔드'
assignees: 'TRIP1'
---

## 📋 기능 개요
**요구사항 ID**: REQ-CRAWL-001
한국관광공사 Tour API를 활용한 관광지 데이터 수집 클라이언트를 구현합니다.

## 🎯 개발 목표
- 한국관광공사 Tour API를 연동하여 실시간 관광지 데이터를 수집합니다.
- KorService2 엔드포인트를 활용하여 서울 지역 관광지 정보를 1,000개 이상 수집합니다.
- AI 추천 시스템을 위한 구조화된 데이터를 제공합니다.
- 대용량 데이터 처리를 위한 효율적인 클라이언트를 구현합니다.

## 📝 기능 명세

### API Endpoints
- **`GET /api/test/tour/test/connection`**: Tour API 연결 테스트
- **`GET /api/test/tour/seoul/tourist-spots`**: 서울 관광지 조회
- **`GET /api/test/tour/seoul/category/{category}`**: 카테고리별 서울 관광지 조회
- **`GET /api/test/tour/search?keyword={keyword}`**: 키워드 검색
- **`GET /api/test/tour/seoul/all`**: 서울 전체 데이터 수집 (1,000개 이상)
- **`GET /api/test/tour/mock/test`**: 모의 데이터 테스트

### Tour API 연동 기능
- **지역 기반 관광정보 조회** (`areaBasedList2`)
- **위치 기반 관광정보 조회** (`locationBasedList1`)
- **키워드 검색** (`searchKeyword1`)
- **상세정보 조회** (`detailCommon1`)

## 🔧 구현 사항

### Configuration
- [x] `TourApiProperties.java` - API 설정 관리 클래스 생성

### DTO
- [x] `TourApiResponse.java` - JSON 응답 매핑 DTO 생성

### Client
- [x] `TourApiClient.java` - HTTP 클라이언트 구현

### Service
- [x] `TourApiService.java` - 데이터 수집 비즈니스 로직 구현

### Controller
- [x] `TourApiTestController.java` - 테스트 엔드포인트 구현

### Configuration
- [x] Spring Security 설정 수정 (`/api/tour/**` 경로 허용)
- [x] `application.yml` Tour API 설정 추가

## 📊 데이터 수집 전략

### 1. Seoul JSON 보완
- **기존**: 177개 샘플 데이터
- **목표**: 1,000개 이상 실시간 데이터
- **방법**: Tour API 실시간 수집

### 2. 카테고리별 수집
- **관광지**: 500개 (5페이지 × 100개)
- **문화시설**: 300개 (3페이지 × 100개)
- **음식점**: 300개 (3페이지 × 100개)
- **쇼핑**: 200개 (2페이지 × 100개)
- **레포츠**: 200개 (2페이지 × 100개)
- **숙박**: 100개 (1페이지 × 100개)

### 3. 중복 제거 및 검증
- **ContentId 기반 중복 제거**
- **데이터 품질 검증**
- **좌표 유효성 검사**

## ✅ 완료 조건
- [x] Tour API 클라이언트 구현 및 연동
- [x] KorService2 엔드포인트 정상 작동
- [x] 대용량 데이터 수집 기능 (1,000개 이상)
- [x] Spring Security 설정 수정
- [x] 모든 테스트 엔드포인트 정상 작동
- [x] 에러 핸들링 및 로깅 구현

## 🧪 테스트 결과

### 최종 테스트 결과
1. **Tour API 연결 테스트** ✅
   - StatusCode: 200
   - 수집된 데이터: 5개
   - 샘플: 가회동성당, 간데메공원 등

2. **서울 관광지 데이터 조회** ✅
   - StatusCode: 200
   - RawContentLength: 4,975 bytes
   - JSON 데이터 정상 수집

3. **카테고리별 데이터 조회** ✅
   - StatusCode: 200
   - RawContentLength: 2,464 bytes
   - Palace 카테고리 데이터 정상

4. **대용량 데이터 수집** ✅
   - StatusCode: 200
   - RawContentLength: 725,020 bytes (약 725KB)
   - 1,000개 이상 데이터 수집 성공

## 🎉 구현 완료 상태

**구현 완료일**: 2025년 9월 10일  
**구현자**: TRIP1 팀  
**상태**: ✅ **완료**

### 📁 구현된 파일 목록
```
src/main/java/com/compass/domain/trip/
├── config/
│   └── TourApiProperties.java              # API 설정 관리
├── dto/
│   └── TourApiResponse.java                # JSON 응답 매핑 DTO
├── client/
│   └── TourApiClient.java                  # HTTP 클라이언트
├── service/
│   └── TourApiService.java                 # 비즈니스 로직 서비스
└── controller/
    └── TourApiTestController.java          # 테스트 엔드포인트

src/main/resources/
└── application.yml                         # Tour API 설정 추가

src/main/java/com/compass/config/
└── SecurityConfig.java                     # Spring Security 설정 수정
```

### 🔄 다음 단계 준비
- **REQ-CRAWL-002**: Phase-별 크롤링 구현 ✅ (완료)
- **REQ-CRAWL-003**: tour_places 테이블 구현 ✅ (완료)
- **데이터베이스 저장**: 수집된 데이터 RDS 저장 ✅ (완료)
- **엔티티 최적화**: 불필요한 null 필드 제거로 효율성 향상 ✅ (완료)
- **AI 추천 시스템**: 수집된 데이터 기반 AI 추천 (다음 단계)

## 📌 참고사항
- 한국관광공사 Tour API KorService2 엔드포인트를 사용합니다.
- API 키는 환경변수 `TOUR_API_SERVICE_KEY`로 관리됩니다.
- 대용량 데이터 수집 시 Rate Limiting(100ms)을 적용합니다.
- 수집된 데이터는 AI 추천 시스템의 기반 데이터로 활용됩니다.
- **후속 작업**: REQ-CRAWL-002에서 엔티티 최적화 완료 (불필요한 null 필드 제거)
- **데이터베이스**: AWS RDS PostgreSQL 연동으로 실제 운영 환경 구축