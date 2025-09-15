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
- [x] `TourPlace.java` - 관광지 엔티티 생성 ✅
- [x] `CrawlStatus.java` - 크롤링 상태 엔티티 생성 ✅

### Repository
- [x] `TourPlaceRepository.java` - 관광지 데이터 저장소 ✅
- [x] `CrawlStatusRepository.java` - 크롤링 상태 저장소 ✅

### Service
- [x] `CrawlService.java` - 크롤링 비즈니스 로직 ✅
- [ ] `TourPlaceService.java` - 관광지 데이터 관리 서비스 (추후 구현)

### Controller
- [x] `CrawlController.java` - 크롤링 관리 API ✅

### Configuration
- [x] 크롤링 스케줄링 설정 ✅
- [x] 데이터베이스 연결 설정 (H2/PostgreSQL) ✅

## 📊 크롤링 데이터 구조

### TourPlace 엔티티 (최적화됨)
```java
@Entity
@Table(name = "tour_places", indexes = {
    @Index(name = "idx_tour_places_content_id", columnList = "content_id"),
    @Index(name = "idx_tour_places_area_code", columnList = "area_code"),
    @Index(name = "idx_tour_places_category", columnList = "category"),
    @Index(name = "idx_tour_places_content_type_id", columnList = "content_type_id")
})
public class TourPlace {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "content_id", unique = true, nullable = false, length = 50)
    private String contentId;
    
    @Column(nullable = false, length = 200)
    private String name;
    
    @Column(nullable = false, length = 50)
    private String category;
    
    @Column(length = 50)
    private String district;
    
    @Column(length = 200)
    private String area;
    
    @Column
    private Double latitude;
    
    @Column
    private Double longitude;
    
    @Column(name = "area_code", nullable = false, length = 10)
    private String areaCode;
    
    @Column(name = "content_type_id", nullable = false, length = 10)
    private String contentTypeId;
    
    @Column(length = 500)
    private String address;
    
    @Column(name = "image_url", length = 500)
    private String imageUrl;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details", columnDefinition = "jsonb")
    private JsonNode details;
    
    @Column(name = "data_source", length = 50)
    private String dataSource;
    
    @Column(name = "crawled_at")
    private LocalDateTime crawledAt;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

### CrawlStatus 엔티티 (최적화됨)
```java
@Entity
@Table(name = "crawl_status", indexes = {
    @Index(name = "idx_crawl_status_area_code", columnList = "area_code"),
    @Index(name = "idx_crawl_status_content_type_id", columnList = "content_type_id"),
    @Index(name = "idx_crawl_status_status", columnList = "status")
})
public class CrawlStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "area_code", nullable = false, length = 10)
    private String areaCode;
    
    @Column(name = "area_name", nullable = false, length = 50)
    private String areaName;
    
    @Column(name = "content_type_id", length = 10)
    private String contentTypeId;
    
    @Column(name = "content_type_name", length = 50)
    private String contentTypeName;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CrawlStatusType status;
    
    @Column(name = "total_pages")
    private Integer totalPages;
    
    @Column(name = "current_page")
    private Integer currentPage;
    
    @Column(name = "collected_count")
    private Integer collectedCount;
    
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
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

## 📈 실제 데이터 수집량 (샘플링 적용)

### 샘플링 전략
- **각 지역당 최대 200개씩 제한**: AWS RDS 비용 절약을 위한 샘플링 적용
- **6개 컨텐츠 타입**: 관광지, 문화시설, 음식점, 쇼핑, 레포츠, 숙박
- **3개 지역**: 서울, 부산, 제주

### 서울 (areaCode: 1)
- **관광지**: 200개 (제한)
- **문화시설**: 200개 (제한)
- **음식점**: 200개 (제한)
- **쇼핑**: 200개 (제한)
- **레포츠**: 200개 (제한)
- **숙박**: 200개 (제한)
- **총합**: 1,200개

### 부산 (areaCode: 6)
- **관광지**: 200개 (제한)
- **문화시설**: 200개 (제한)
- **음식점**: 200개 (제한)
- **쇼핑**: 200개 (제한)
- **레포츠**: 200개 (제한)
- **숙박**: 200개 (제한)
- **총합**: 1,200개

### 제주 (areaCode: 39)
- **관광지**: 200개 (제한)
- **문화시설**: 200개 (제한)
- **음식점**: 200개 (제한)
- **쇼핑**: 200개 (제한)
- **레포츠**: 200개 (제한)
- **숙박**: 200개 (제한)
- **총합**: 1,200개

### 전체 실제 수집량
- **이론적 최대**: 3,600개 (3개 지역 × 6개 타입 × 200개)
- **실제 수집**: 3,115개 (일부 컨텐츠 타입에서 데이터 부족)
- **비용 절약**: 이전 10,079개에서 69% 감소
- **크롤링 시간**: 약 15-20분

## ✅ 완료 조건
- [x] TourPlace 엔티티 및 테이블 생성 ✅
- [x] CrawlStatus 엔티티 및 테이블 생성 ✅
- [x] 크롤링 서비스 구현 ✅
- [x] 크롤링 컨트롤러 구현 ✅
- [x] 서울 지역 크롤링 성공 ✅
- [x] 부산 지역 크롤링 성공 ✅
- [x] 제주 지역 크롤링 성공 ✅
- [x] 크롤링 상태 모니터링 기능 ✅
- [x] 데이터베이스 저장 검증 ✅

## 🔄 다음 단계
- **REQ-CRAWL-003**: tour_places 테이블 최적화
- **REQ-CRAWL-004**: 크롤링 스케줄러 구현
- **REQ-SEARCH-001**: RDS 검색 시스템 구현

## 🎉 구현 완료 상태

### ✅ **완료된 기능들**
- **TourPlace 엔티티**: 관광지 데이터 저장을 위한 최적화된 엔티티 구현 (불필요한 null 필드 제거)
- **CrawlStatus 엔티티**: 크롤링 진행 상황 추적을 위한 최적화된 엔티티 구현 (BaseEntity 상속 제거)
- **Repository 계층**: TourPlaceRepository, CrawlStatusRepository 완전 구현
- **CrawlService**: Phase별 크롤링 비즈니스 로직 완전 구현 (최적화된 엔티티 반영)
- **CrawlController**: REST API 엔드포인트 완전 구현
- **데이터베이스 연동**: H2/PostgreSQL 지원, 테이블 자동 생성
- **Spring Security**: 크롤링 API 경로 허용 설정 완료
- **엔티티 최적화**: 불필요한 필드 제거로 데이터베이스 효율성 향상

### 🧪 **테스트 결과**
- **크롤링 시작**: `/api/crawl/start` 정상 작동 (HTTP 200)
- **지역별 크롤링**: 서울, 부산, 제주 지역 크롤링 성공
- **데이터 저장**: TourPlace 엔티티로 변환하여 데이터베이스 저장 성공
- **상태 추적**: CrawlStatus를 통한 크롤링 진행 상황 추적 성공
- **API 응답**: 모든 엔드포인트 정상 응답 확인

### 📊 **실제 수집 데이터 (샘플링 적용)**
- **서울 지역**: 관광지, 문화시설, 음식점, 쇼핑, 레포츠, 숙박 데이터 수집 (각 200개 제한)
- **부산 지역**: 관광지, 문화시설, 음식점, 쇼핑, 레포츠, 숙박 데이터 수집 (각 200개 제한)
- **제주 지역**: 관광지, 문화시설, 음식점, 쇼핑, 레포츠, 숙박 데이터 수집 (각 200개 제한)
- **총 수집량**: 3,115개 (이론적 최대 3,600개에서 일부 데이터 부족으로 감소)
- **비용 절약**: 이전 10,079개에서 69% 감소로 AWS RDS 비용 대폭 절약
- **데이터 형식**: JSON 형태로 Tour API에서 수집, TourPlace 엔티티로 변환

## 📌 참고사항
- REQ-CRAWL-001의 TourApiClient를 활용합니다.
- H2/PostgreSQL 데이터베이스를 지원합니다.
- Rate Limiting을 적용하여 API 제한을 준수합니다.
- 크롤링 중 오류 발생 시 재시도 로직을 적용합니다.
- 수집된 데이터는 AI 추천 시스템의 기반 데이터로 활용됩니다.
- **엔티티 최적화**: 불필요한 null 필드들 제거로 데이터베이스 효율성 향상
- **BaseEntity 상속 제거**: H2 데이터베이스 호환성 문제 해결

## 📅 **완료 일시**: 2025-09-11 15:35 (엔티티 최적화 완료)

