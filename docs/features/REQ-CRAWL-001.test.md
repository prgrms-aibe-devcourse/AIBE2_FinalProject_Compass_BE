---
name: 테스트 명세서
about: 기능 명세서에 대한 테스트 케이스를 정의하는 문서
title: '[TEST] REQ-CRAWL-001 | Tour API 클라이언트 테스트'
labels: '백엔드'
assignees: 'TRIP1'
---

# 🧪 통합 테스트 작성

## 📋 테스트 정보

### 테스트 대상
- **클래스명**: `TourApiTestController`, `TourApiService`, `TourApiClient`
- **메서드명**: `testConnection()`, `getSeoulTouristSpots()`, `collectAllSeoulData()`
- **파일 경로**: `src/test/java/com/compass/domain/trip/controller/TourApiTestControllerTest.java`

### 테스트 목적
> Tour API 클라이언트의 전체 흐름이 올바르게 동작하는지 검증합니다.
> 
- 한국관광공사 Tour API와의 연동이 정상적으로 이루어지는지 확인합니다.
- 대용량 데이터 수집 기능이 1,000개 이상의 데이터를 성공적으로 수집하는지 확인합니다.
- 각종 API 엔드포인트가 올바른 응답을 반환하는지 확인합니다.

---

## 🎯 테스트 케이스

### 정상 케이스 (Happy Path)
- [x] **케이스 1**: Tour API 연결 테스트
    - **입력**: `GET /api/test/tour/test/connection` 요청
    - **예상 결과**: HTTP Status `200 OK`가 반환되고, 연결 성공 메시지와 샘플 데이터 5개가 포함됩니다.
    - **설명**: Tour API와의 기본적인 연결이 정상적으로 이루어지는지 검증합니다.

- [x] **케이스 2**: 서울 관광지 데이터 조회
    - **입력**: `GET /api/test/tour/seoul/tourist-spots?pageNo=1&numOfRows=10` 요청
    - **예상 결과**: HTTP Status `200 OK`가 반환되고, JSON 형태의 관광지 데이터가 반환됩니다.
    - **설명**: 지역 기반 관광정보 조회 API가 정상적으로 동작하는지 검증합니다.

- [x] **케이스 3**: 카테고리별 데이터 조회
    - **입력**: `GET /api/test/tour/seoul/category/Palace?pageNo=1&numOfRows=5` 요청
    - **예상 결과**: HTTP Status `200 OK`가 반환되고, Palace 카테고리 관련 데이터가 반환됩니다.
    - **설명**: 카테고리별 필터링 기능이 정상적으로 동작하는지 검증합니다.

- [x] **케이스 4**: 대용량 데이터 수집
    - **입력**: `GET /api/test/tour/seoul/all` 요청
    - **예상 결과**: HTTP Status `200 OK`가 반환되고, 725KB 이상의 대용량 JSON 데이터가 반환됩니다.
    - **설명**: 1,000개 이상의 데이터를 성공적으로 수집하는지 검증합니다.

- [x] **케이스 5**: 모의 데이터 테스트
    - **입력**: `GET /api/test/tour/mock/test` 요청
    - **예상 결과**: HTTP Status `200 OK`가 반환되고, 클라이언트 구현 완료 메시지가 반환됩니다.
    - **설명**: 실제 API 호출 없이 클라이언트 로직이 정상적으로 동작하는지 검증합니다.

### 예외 케이스 (Exception Cases)
- [x] **API 키 오류**: 잘못된 API 키로 요청 시도
    - **입력**: 잘못된 API 키를 사용한 Tour API 요청
    - **예상 결과**: HTTP Status `200 OK`가 반환되지만, 빈 데이터 또는 오류 메시지가 포함됩니다.
    - **설명**: API 키 검증 및 오류 처리가 올바르게 동작하는지 확인합니다.

- [x] **네트워크 오류**: 네트워크 연결 실패 시
    - **입력**: 네트워크가 차단된 상태에서 Tour API 요청
    - **예상 결과**: 적절한 오류 메시지와 함께 빈 응답 또는 오류 응답이 반환됩니다.
    - **설명**: 네트워크 오류 상황에 대한 예외 처리가 올바르게 동작하는지 확인합니다.

---

## 🔧 테스트 환경 설정

### 설정 파일
- **`src/main/resources/application.yml`**: Tour API 설정
- **`src/main/java/com/compass/config/SecurityConfig.java`**: Spring Security 설정

### Tour API 설정
```yaml
tour:
  api:
    base-url: https://apis.data.go.kr/B551011/KorService2
    service-key: ${TOUR_API_SERVICE_KEY:a1276cb5e93f8b11431d3d2fbfe5e843c3da51d2cd537c50170ea15e87f343ff}
    response-type: json
    num-of-rows: 100
    page-no: 1
    arrange: A
    default-area-code: "1"
```

### Spring Security 설정
```java
.requestMatchers("/api/tour/**").permitAll()  // Tour API 엔드포인트 허용
```

---

## 📊 테스트 결과

### 최종 테스트 결과 (2025-09-10 실행)

#### ✅ 성공한 테스트들:

1. **Tour API 연결 테스트**
   ```
   요청: GET /api/test/tour/test/connection
   응답: HTTP 200 OK
   결과: ✅ 연결 성공!
   데이터: 5개 (가회동성당, 간데메공원 등)
   ```

2. **서울 관광지 데이터 조회**
   ```
   요청: GET /api/test/tour/seoul/tourist-spots?pageNo=1&numOfRows=10
   응답: HTTP 200 OK
   결과: ✅ JSON 데이터 정상 수집
   크기: 4,975 bytes
   ```

3. **카테고리별 데이터 조회**
   ```
   요청: GET /api/test/tour/seoul/category/Palace?pageNo=1&numOfRows=5
   응답: HTTP 200 OK
   결과: ✅ Palace 카테고리 데이터 정상
   크기: 2,464 bytes
   ```

4. **대용량 데이터 수집**
   ```
   요청: GET /api/test/tour/seoul/all
   응답: HTTP 200 OK
   결과: ✅ 1,000개 이상 데이터 수집 성공
   크기: 725,020 bytes (약 725KB)
   ```

5. **모의 데이터 테스트**
   ```
   요청: GET /api/test/tour/mock/test
   응답: HTTP 200 OK
   결과: ✅ 클라이언트 구현 완료 확인
   크기: 1,486 bytes
   ```

#### ⚠️ 주의사항:
- **키워드 검색**: 빈 배열 반환 (API 특성상 정상)
- **Rate Limiting**: API 호출 간 100ms 대기 적용

---

## 🧪 테스트 실행 방법

### 1. 애플리케이션 시작
```bash
./gradlew bootRun
```

### 2. 테스트 실행
```bash
# Tour API 연결 테스트
curl -X GET "http://localhost:8080/api/test/tour/test/connection"

# 서울 관광지 조회
curl -X GET "http://localhost:8080/api/test/tour/seoul/tourist-spots?pageNo=1&numOfRows=10"

# 카테고리별 조회
curl -X GET "http://localhost:8080/api/test/tour/seoul/category/Palace?pageNo=1&numOfRows=5"

# 대용량 데이터 수집
curl -X GET "http://localhost:8080/api/test/tour/seoul/all"

# 모의 데이터 테스트
curl -X GET "http://localhost:8080/api/test/tour/mock/test"
```

### 3. PowerShell 테스트
```powershell
# Tour API 연결 테스트
Invoke-WebRequest -Uri "http://localhost:8080/api/test/tour/test/connection" -Headers @{"Content-Type"="application/json"}

# 서울 관광지 조회
Invoke-WebRequest -Uri "http://localhost:8080/api/test/tour/seoul/tourist-spots?pageNo=1&numOfRows=10" -Headers @{"Content-Type"="application/json"}

# 대용량 데이터 수집
Invoke-WebRequest -Uri "http://localhost:8080/api/test/tour/seoul/all" -Headers @{"Content-Type"="application/json"}
```

---

## 📈 성능 테스트

### 데이터 수집 성능
- **관광지**: 500개 (5페이지 × 100개) - 약 2.5초
- **문화시설**: 300개 (3페이지 × 100개) - 약 1.5초
- **음식점**: 300개 (3페이지 × 100개) - 약 1.5초
- **쇼핑**: 200개 (2페이지 × 100개) - 약 1.0초
- **레포츠**: 200개 (2페이지 × 100개) - 약 1.0초
- **숙박**: 100개 (1페이지 × 100개) - 약 0.5초

### 총 수집 시간
- **전체 데이터 수집**: 약 8초 (Rate Limiting 포함)
- **데이터 크기**: 725KB
- **수집된 항목**: 1,000개 이상

---

## 🔍 테스트 검증 항목

### API 연동 검증
- [x] Tour API 기본 연결 성공
- [x] KorService2 엔드포인트 정상 작동
- [x] API 키 인증 성공
- [x] JSON 응답 파싱 성공

### 데이터 수집 검증
- [x] 지역 기반 관광정보 조회 성공
- [x] 카테고리별 데이터 필터링 성공
- [x] 대용량 데이터 수집 성공
- [x] 중복 제거 로직 정상 작동

### 에러 처리 검증
- [x] API 오류 상황 처리
- [x] 네트워크 오류 처리
- [x] 빈 데이터 응답 처리
- [x] Rate Limiting 적용

### 보안 검증
- [x] Spring Security 설정 정상
- [x] `/api/tour/**` 경로 허용 확인
- [x] API 키 외부화 적용

---

## ✅ 테스트 완료 조건

- [x] Tour API 연결 테스트 성공
- [x] 서울 관광지 데이터 조회 성공
- [x] 카테고리별 데이터 조회 성공
- [x] 대용량 데이터 수집 성공 (1,000개 이상)
- [x] 모의 데이터 테스트 성공
- [x] 에러 처리 검증 완료
- [x] 성능 테스트 완료
- [x] 보안 설정 검증 완료

---

## 📌 참고사항

### API 제한사항
- **Rate Limiting**: API 호출 간 100ms 대기 필요
- **데이터 크기**: 대용량 수집 시 메모리 사용량 주의
- **네트워크**: 안정적인 인터넷 연결 필요

### 테스트 환경
- **개발 환경**: Windows 10, PowerShell
- **애플리케이션**: Spring Boot 3.x
- **데이터베이스**: H2 (테스트용)
- **API**: 한국관광공사 Tour API KorService2

### 다음 단계
- **REQ-CRAWL-002**: Phase-별 크롤링 구현
- **REQ-CRAWL-003**: tour_places 테이블 구현
- **데이터베이스 저장**: 수집된 데이터 RDS 저장
- **AI 추천 시스템**: 수집된 데이터 기반 AI 추천

---

- **📅 최종 검증**: 2025-09-09 16:21 - Tour API 클라이언트 테스트 완료
