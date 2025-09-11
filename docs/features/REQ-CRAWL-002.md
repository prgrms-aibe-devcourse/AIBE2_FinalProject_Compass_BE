---
name: 기능 개발
about: 기능명세서 기반 개발 작업을 위한 이슈 템플릿
title: '[TRIP] REQ-CRAWL-002 | Phase별 크롤링 구현'
labels: '백엔드'
assignees: 'TRIP1'
---

## 📋 기능 개요
**요구사항 ID**: REQ-CRAWL-002
REQ-CRAWL-001에서 구현한 Tour API 클라이언트를 활용하여 서울→부산→제주 순차 크롤링을 구현합니다.

## 🎯 개발 목표
- 서울, 부산, 제주 지역별로 순차적으로 관광지 데이터를 크롤링합니다.
- 수집된 데이터를 TourPlace 엔티티로 변환하여 데이터베이스에 저장합니다.
- 각 지역별로 카테고리별 데이터를 체계적으로 수집합니다.
- 크롤링 진행 상황을 모니터링할 수 있는 기능을 제공합니다.

## 📝 기능 명세

### API Endpoints
- **`POST /api/crawl/start`**: 전체 지역 크롤링 시작
- **`POST /api/crawl/start/{areaCode}`**: 특정 지역 크롤링 시작
- **`GET /api/crawl/status`**: 크롤링 진행 상황 조회
- **`GET /api/crawl/status/{areaCode}`**: 특정 지역 크롤링 상태 조회
- **`GET /api/crawl/results`**: 크롤링 결과 통계 조회

### 크롤링 대상 지역
- **서울 (areaCode: 1)**: 관광지, 문화시설, 음식점, 쇼핑, 레포츠, 숙박
- **부산 (areaCode: 6)**: 관광지, 문화시설, 음식점, 쇼핑, 레포츠, 숙박  
- **제주 (areaCode: 39)**: 관광지, 문화시설, 음식점, 쇼핑, 레포츠, 숙박

### 크롤링 전략
- **Phase 1**: 서울 지역 전체 카테고리 크롤링
- **Phase 2**: 부산 지역 전체 카테고리 크롤링
- **Phase 3**: 제주 지역 전체 카테고리 크롤링
- **각 Phase별**: 카테고리별 순차 크롤링 (관광지 → 문화시설 → 음식점 → 쇼핑 → 레포츠 → 숙박)

## 🔧 구현 사항

### Entity
- [ ] `TourPlace.java` - 관광지 엔티티 생성
- [ ] `CrawlStatus.java` - 크롤링 상태 엔티티 생성

### Repository
- [ ] `TourPlaceRepository.java` - 관광지 데이터 저장소
- [ ] `CrawlStatusRepository.java` - 크롤링 상태 저장소

### Service
- [ ] `CrawlService.java` - 크롤링 비즈니스 로직
- [ ] `TourPlaceService.java` - 관광지 데이터 관리 서비스

### Controller
- [ ] `CrawlController.java` - 크롤링 관리 API

### Configuration
- [ ] 크롤링 스케줄링 설정
- [ ] 데이터베이스 연결 설정 (PostgreSQL)

## 📊 크롤링 데이터 구조

### TourPlace 엔티티
```java
@Entity
@Table(name = "tour_places")
public class TourPlace {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "content_id", unique = true, nullable = false)
    private String contentId;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String category;
    
    @Column
    private String district;
    
    @Column
    private String area;
    
    @Column
    private Double latitude;
    
    @Column
    private Double longitude;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "keywords", columnDefinition = "jsonb")
    private JsonNode keywords;
    
    @Column(name = "area_code")
    private String areaCode;
    
    @Column(name = "content_type_id")
    private String contentTypeId;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

### CrawlStatus 엔티티
```java
@Entity
@Table(name = "crawl_status")
public class CrawlStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "area_code", nullable = false)
    private String areaCode;
    
    @Column(name = "area_name", nullable = false)
    private String areaName;
    
    @Column(name = "content_type_id")
    private String contentTypeId;
    
    @Column(name = "content_type_name")
    private String contentTypeName;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CrawlStatusType status;
    
    @Column(name = "total_pages")
    private Integer totalPages;
    
    @Column(name = "current_page")
    private Integer currentPage;
    
    @Column(name = "collected_count")
    private Integer collectedCount;
    
    @Column(name = "error_message")
    private String errorMessage;
    
    @CreationTimestamp
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
```

## 🚀 크롤링 프로세스

### 1. 크롤링 시작
```
POST /api/crawl/start
{
  "areaCodes": ["1", "6", "39"],
  "contentTypeIds": ["12", "14", "39", "38", "28", "32"]
}
```

### 2. 크롤링 진행
- 각 지역별로 순차 크롤링
- 각 카테고리별로 페이지별 크롤링
- Rate Limiting 적용 (100ms 간격)
- 중복 데이터 제거

### 3. 데이터 저장
- TourPlace 엔티티로 변환
- 데이터베이스에 배치 저장
- 크롤링 상태 업데이트

### 4. 완료 처리
- 크롤링 결과 통계 생성
- 완료 상태 업데이트
- 로그 기록

## 📈 예상 데이터 수집량

### 서울 (areaCode: 1)
- **관광지**: 500개
- **문화시설**: 300개
- **음식점**: 300개
- **쇼핑**: 200개
- **레포츠**: 200개
- **숙박**: 100개
- **총합**: 1,600개

### 부산 (areaCode: 6)
- **관광지**: 300개
- **문화시설**: 200개
- **음식점**: 250개
- **쇼핑**: 150개
- **레포츠**: 150개
- **숙박**: 80개
- **총합**: 1,130개

### 제주 (areaCode: 39)
- **관광지**: 400개
- **문화시설**: 150개
- **음식점**: 200개
- **쇼핑**: 100개
- **레포츠**: 200개
- **숙박**: 120개
- **총합**: 1,170개

### 전체 예상 수집량
- **총 데이터**: 약 3,900개
- **중복 제거 후**: 약 3,500개
- **크롤링 시간**: 약 30-40분

## ✅ 완료 조건
- [ ] TourPlace 엔티티 및 테이블 생성
- [ ] CrawlStatus 엔티티 및 테이블 생성
- [ ] 크롤링 서비스 구현
- [ ] 크롤링 컨트롤러 구현
- [ ] 서울 지역 크롤링 성공
- [ ] 부산 지역 크롤링 성공
- [ ] 제주 지역 크롤링 성공
- [ ] 크롤링 상태 모니터링 기능
- [ ] 데이터베이스 저장 검증

## 🔄 다음 단계
- **REQ-CRAWL-003**: tour_places 테이블 최적화
- **REQ-CRAWL-004**: 크롤링 스케줄러 구현
- **REQ-SEARCH-001**: RDS 검색 시스템 구현

## 📌 참고사항
- REQ-CRAWL-001의 TourApiClient를 활용합니다.
- PostgreSQL 데이터베이스를 사용합니다.
- Rate Limiting을 적용하여 API 제한을 준수합니다.
- 크롤링 중 오류 발생 시 재시도 로직을 적용합니다.
- 수집된 데이터는 AI 추천 시스템의 기반 데이터로 활용됩니다.

