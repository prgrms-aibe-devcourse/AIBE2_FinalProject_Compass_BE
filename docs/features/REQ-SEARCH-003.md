---
name: 기능 개발
about: 기능명세서 기반 개발 작업을 위한 이슈 템플릿
title: '[SEARCH] REQ-SEARCH-003 | Kakao Map API 검색 (폴백 검색)'
labels: '백엔드'
assignees: 'SEARCH3'
---

## 📋 기능 개요
**요구사항 ID**: REQ-SEARCH-003
Kakao Map API를 활용한 폴백 검색 시스템을 구현합니다. RDS 검색과 Tour API 검색이 실패하거나 결과가 부족할 때 사용되는 보조 검색 시스템입니다.

## 🎯 개발 목표
- Kakao Map API를 통한 장소 검색 기능을 제공합니다.
- 키워드, 카테고리, 주소 기반 검색을 지원합니다.
- 좌표/주소 변환 기능을 제공합니다.
- 통합 검색을 위한 폴백 역할을 수행합니다.

## 📝 기능 명세
### API Endpoints
- **`GET /api/search/kakao/keyword`**: 키워드 검색
- **`GET /api/search/kakao/category`**: 카테고리 검색
- **`GET /api/search/kakao/address`**: 주소 검색
- **`GET /api/search/kakao/coord-to-address`**: 좌표를 주소로 변환
- **`GET /api/search/kakao/address-to-coord`**: 주소를 좌표로 변환
- **`GET /api/search/kakao/integrated`**: 통합 검색
- **`GET /api/search/kakao/statistics`**: 검색 통계

### Request Parameters
```json
{
  "keyword": "경복궁",
  "categoryGroupCode": "MT1",
  "x": "126.9780",
  "y": "37.5665",
  "radius": 5000,
  "page": 1,
  "size": 15,
  "sort": "accuracy"
}
```

### Response Body
```json
{
  "meta": {
    "totalCount": 1,
    "pageableCount": 1,
    "isEnd": true
  },
  "documents": [
    {
      "id": "1234567890",
      "placeName": "경복궁",
      "categoryName": "문화,예술 > 문화시설 > 궁궐",
      "categoryGroupCode": "CT1",
      "phone": "02-3700-3900",
      "addressName": "서울 종로구 사직로 161",
      "roadAddressName": "서울 종로구 사직로 161",
      "x": "126.977041",
      "y": "37.579617",
      "placeUrl": "http://place.map.kakao.com/1234567890",
      "distance": "500"
    }
  ]
}
```

## 🔧 구현 사항
### Configuration
- [x] `KakaoMapApiProperties.java` 생성 (API 설정 관리)
- [x] `@ConfigurationProperties`를 활용한 자동 바인딩
- [x] `application.yml`에 Kakao Map API 설정 추가

### DTO
- [x] `KakaoMapApiResponse.java` 생성 (API 응답 DTO)
- [x] Meta, Document, Address, RoadAddress 내부 클래스 구현

### Client
- [x] `KakaoMapApiClient.java` 생성 (Kakao Map API HTTP 클라이언트)
- [x] RestTemplate을 활용한 API 통신 구현
- [x] Authorization 헤더 자동 설정

### Service
- [x] `KakaoMapSearchService.java` 생성 (Kakao Map API 검색 비즈니스 로직)
- [x] 키워드 검색 메서드 구현
- [x] 카테고리 검색 메서드 구현
- [x] 주소 검색 메서드 구현
- [x] 좌표 변환 메서드 구현
- [x] 통합 검색 메서드 구현

### Controller
- [x] `KakaoMapSearchController.java` 생성 (REST API 엔드포인트)
- [x] `KakaoMapSearchTestController.java` 생성 (테스트용 엔드포인트)
- [x] Swagger 문서화 적용

## ✅ 완료 조건
- [x] Kakao Map API 연동 구현
- [x] 키워드 검색 기능 구현
- [x] 카테고리 검색 기능 구현
- [x] 주소 검색 기능 구현
- [x] 좌표/주소 변환 기능 구현
- [x] 통합 검색 기능 구현
- [x] 검색 통계 기능 구현
- [x] REST API 엔드포인트 구현
- [x] 테스트용 엔드포인트 구현
- [x] Swagger 문서화 완료

## 📌 참고사항
- Kakao Map API 활용
- 폴백 검색 시스템으로 활용
- API 키는 환경 변수로 관리
- 카테고리 그룹 코드 활용 (MT1, CS2, CT1, AT4, FD6, AD5 등)
- 향후 통합 검색 시스템과 연동 예정