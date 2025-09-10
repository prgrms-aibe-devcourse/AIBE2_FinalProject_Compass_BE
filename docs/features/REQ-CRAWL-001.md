# REQ-CRAWL-001: Tour API 클라이언트

## 📋 요구사항 정의

**기능명**: 한국관광공사 Tour API 연동 클라이언트  
**우선순위**: 1 (최고)  
**담당자**: TRIP 도메인  
**상태**: ✅ **구현 완료**

## 🎯 목표

Seoul JSON 데이터를 보완하고 실시간 관광 정보를 제공하는 Tour API 클라이언트 구현

## 🏗️ 아키텍처

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  Seoul JSON     │    │   Tour API      │    │   TourPlace     │
│  (177개 장소)    │───▶│   Client        │───▶│   Entity        │
│  - 기본 정보     │    │  - 상세 정보     │    │  - 통합 데이터   │
│  - 좌표/태그     │    │  - 실시간 데이터  │    │  - AI 추천 최적화 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## 📁 구현된 컴포넌트

### 1. **Configuration** 
- `TourApiProperties.java`: API 설정 및 카테고리 매핑
- `application.yml`: Tour API 연동 설정

### 2. **Client Layer**
- `TourApiClient.java`: 한국관광공사 API 직접 호출
- `TourApiResponse.java`: API 응답 DTO

### 3. **Service Layer** 
- `TourApiService.java`: 비즈니스 로직 및 Seoul JSON 매핑

### 4. **Controller Layer**
- `TourApiTestController.java`: API 연동 테스트 엔드포인트

## 🔗 Seoul JSON ↔ Tour API 매핑

### 카테고리 매핑
| Seoul JSON Category | Tour API ContentTypeId | 설명 |
|-------------------|----------------------|------|
| Palace, Historic Gate, UNESCO Site | 12 | 관광지 |
| Museum, Theater, Arts Complex | 14 | 문화시설 |
| Shopping Mall, Market | 38 | 쇼핑 |
| Food Alley, Food Street | 39 | 음식점 |
| Sports Venue, Theme Park | 28 | 레포츠 |

### 데이터 필드 매핑
| Seoul JSON | Tour API | TourPlace Entity |
|-----------|----------|------------------|
| `id` | `contentid` | `contentId` |
| `name` | `title` | `name` |
| `category` | `contenttypeid` | `category` |
| `district` | `addr1` (파싱) | `district` |
| `area` | `addr2` | `area` |
| `lat` | `mapy` | `latitude` |
| `lng` | `mapx` | `longitude` |
| `tags[]` | 키워드 검색 | `keywords` |

## 🚀 주요 기능

### 1. **지역 기반 조회**
```java
// 서울 관광지 조회
tourApiService.getSeoulTouristSpots(1, 100);

// 서울 음식점 조회  
tourApiService.getSeoulRestaurants(1, 100);

// 카테고리별 조회
tourApiService.getSeoulByCategory("Palace", 1, 50);
```

### 2. **위치 기반 검색** 
```java
// Seoul JSON 좌표로 근처 관광지 검색
tourApiService.getNearbyPlaces(37.579617, 126.977041, 1000, "12");
```

### 3. **키워드 검색**
```java
// Seoul JSON tags로 관련 정보 검색
tourApiService.searchByKeyword("Joseon", "12");
```

### 4. **상세 정보 보완**
```java
// Seoul JSON에 없는 상세 정보 조회
tourApiService.getPlaceDetail("contentId", "12");
```

### 5. **데이터 수집**
```java
// Phase별 크롤링을 위한 전체 데이터 수집
tourApiService.collectAllSeoulData();
```

## 🧪 테스트 API 엔드포인트

### 기본 조회
- `GET /api/test/tour/seoul/tourist-spots` - 서울 관광지
- `GET /api/test/tour/seoul/restaurants` - 서울 음식점  
- `GET /api/test/tour/seoul/shopping` - 서울 쇼핑

### 고급 검색
- `GET /api/test/tour/seoul/category/{category}` - 카테고리별 검색
- `GET /api/test/tour/nearby` - 위치 기반 검색
- `GET /api/test/tour/search` - 키워드 검색

### 데이터 보완
- `GET /api/test/tour/detail/{contentId}` - 상세 정보
- `GET /api/test/tour/enrich` - Seoul JSON 데이터 보완
- `GET /api/test/tour/seoul/all` - 전체 데이터 수집

### 유틸리티
- `GET /api/test/tour/mapping/category/{seoulCategory}` - 카테고리 매핑 확인

## ⚙️ 설정

### application.yml
```yaml
tour:
  api:
    base-url: http://apis.data.go.kr/B551011/KorService1
    service-key: ${TOUR_API_SERVICE_KEY}
    response-type: json
    num-of-rows: 100
    page-no: 1
    arrange: A # A=제목순, B=조회순, C=수정일순, D=생성일순, E=거리순
    default-area-code: "1" # 서울
```

### 환경 변수
```bash
TOUR_API_SERVICE_KEY=your-tour-api-service-key
```

## 📊 데이터 통계

### Seoul JSON (177개 장소)
- 궁궐: 5개 → Tour API 관광지(12) 매핑
- 박물관: 15개 → Tour API 문화시설(14) 매핑  
- 시장: 8개 → Tour API 쇼핑(38) 매핑
- 음식 거리: 6개 → Tour API 음식점(39) 매핑

### Tour API 보완 데이터
- 상세 설명 (Seoul JSON에 없음)
- 전화번호, 영업시간
- 대표 이미지 URL
- 입장료, 주차 정보
- 최신 수정일시

## 🔄 다음 단계 (REQ-CRAWL-002)

1. **Phase별 크롤링 구현**
   - 서울 → 부산 → 제주 순차 크롤링
   - `collectAllSeoulData()` 확장

2. **TourPlace 엔티티 연동**
   - Tour API 데이터 → TourPlace 매핑
   - Seoul JSON + Tour API 하이브리드 저장

3. **스케줄러 연동** (REQ-CRAWL-004)
   - 6시간마다 자동 데이터 갱신
   - 변경 사항 감지 및 업데이트

## 🧪 테스트 방법

### 1. API 키 설정
```bash
# .env 파일 또는 환경변수
TOUR_API_SERVICE_KEY=your-actual-api-key
```

### 2. 애플리케이션 실행
```bash
./gradlew bootRun
```

### 3. Swagger UI 접속
```
http://localhost:8080/swagger-ui.html
```

### 4. 테스트 API 호출
```bash
# 서울 관광지 조회
curl "http://localhost:8080/api/test/tour/seoul/tourist-spots?pageNo=1&numOfRows=5"

# 카테고리 매핑 테스트
curl "http://localhost:8080/api/test/tour/mapping/category/Palace"

# 경복궁 근처 검색 (Seoul JSON 좌표)
curl "http://localhost:8080/api/test/tour/nearby?latitude=37.579617&longitude=126.977041&radiusMeters=1000"
```

## ✅ 완료 조건

- [x] TourApiProperties 설정 클래스 구현
- [x] TourApiClient API 호출 클래스 구현  
- [x] TourApiService 비즈니스 로직 구현
- [x] Seoul JSON ↔ Tour API 카테고리 매핑
- [x] 지역/위치/키워드 기반 검색 기능
- [x] 테스트 컨트롤러 및 API 엔드포인트
- [x] 단위 테스트 작성
- [x] 설정 파일 업데이트

## 📌 참고사항

- Seoul JSON의 177개 장소는 **기본 데이터**로 활용
- Tour API는 **실시간 보완 데이터** 및 **새로운 장소 발굴**에 활용
- AI 추천 시스템은 두 데이터를 통합하여 최적의 여행 계획 생성
- Phase별 크롤링으로 서울 → 부산 → 제주 순차 확장 예정
