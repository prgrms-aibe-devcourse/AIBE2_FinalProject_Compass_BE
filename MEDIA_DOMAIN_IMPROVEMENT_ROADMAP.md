# 미디어 도메인 개선 로드맵 및 실행 계획

## 📋 개요

이 문서는 Compass 백엔드 프로젝트의 **미디어 도메인** 개선 사항에 대한 우선순위와 단계별 실행 계획을 제시합니다.

**생성일**: 2024년 1월  
**담당자**: 미디어 도메인 전담 개발자  
**범위**: `com.compass.domain.media` 패키지 전체  
**목표**: 성능, 보안, 유지보수성 향상

---

## 🎯 개선 목표 및 성공 지표

### 핵심 목표
1. **성능 향상**: 파일 업로드 처리 시간 50% 단축
2. **보안 강화**: 고급 위협 탐지 및 방어 체계 구축
3. **유지보수성**: 코드 복잡도 감소 및 테스트 커버리지 90% 달성
4. **확장성**: 동시 사용자 10배 증가 대응

### 성공 지표 (KPI)
| 지표 | 현재 | 목표 | 측정 방법 |
|------|------|------|----------|
| **파일 업로드 응답 시간** | 5-15초 | 2-7초 | API 응답 시간 모니터링 |
| **OCR 처리 시간** | 10-30초 | 5-15초 | 비동기 처리 도입 |
| **업로드 성공률** | 95% | 99% | 에러율 모니터링 |
| **보안 위협 탐지율** | 80% | 95% | 고급 스캔 도입 |
| **코드 커버리지** | 60% | 90% | 단위/통합 테스트 |
| **동시 처리 용량** | 100명 | 1,000명 | 부하 테스트 |

---

## 🚦 우선순위 매트릭스

### 영향도 vs 구현 난이도

```mermaid
quadrantChart
    title 개선 사항 우선순위 매트릭스
    x-axis 구현 난이도 낮음 --> 높음
    y-axis 영향도 낮음 --> 높음
    
    quadrant-1 Quick Wins (즉시 실행)
    quadrant-2 Major Projects (계획적 실행)
    quadrant-3 Fill-ins (여유시 실행)
    quadrant-4 Questionable (재검토)
    
    비동기 처리: [0.3, 0.9]
    상수 통합: [0.1, 0.4]
    OCR 리팩토링: [0.4, 0.7]
    연결 풀링: [0.5, 0.8]
    보안 강화: [0.7, 0.9]
    이벤트 아키텍처: [0.9, 0.8]
    마이크로서비스: [0.95, 0.6]
    메트릭 시스템: [0.6, 0.5]
```

### 우선순위 분류

#### 🔴 P0 (Critical) - 즉시 실행 필요
1. **비동기 처리 도입** - 사용자 경험 직접 영향
2. **OCR 서비스 리팩토링** - 코드 중복 제거
3. **상수 통합** - 유지보수성 향상

#### 🟡 P1 (High) - 1-2개월 내 실행
4. **연결 풀링 구현** - 성능 최적화
5. **고급 보안 검증** - 보안 강화
6. **에러 처리 개선** - 안정성 향상

#### 🟢 P2 (Medium) - 2-4개월 내 실행
7. **이벤트 기반 아키텍처** - 확장성 향상
8. **메트릭 및 모니터링** - 운영 효율성
9. **테스트 커버리지 향상** - 품질 보증

#### 🔵 P3 (Low) - 장기 계획
10. **마이크로서비스 분리** - 아키텍처 진화
11. **다중 스토리지 지원** - 가용성 향상

---

## 📅 단계별 실행 계획

### Phase 1: 즉시 개선 (1-2주)

#### 🎯 목표: 코드 품질 및 유지보수성 향상

**1.1 상수 통합 (`MediaConstants.java`)**
- **소요 시간**: 2일
- **담당자**: 미디어 도메인 개발자
- **작업 내용**:
  ```java
  // 기존 - 분산된 상수들
  private static final long MAX_OCR_FILE_SIZE = 50 * 1024 * 1024;
  private static final int THUMBNAIL_WIDTH = 300;
  
  // 개선 - 중앙 집중화
  public final class MediaConstants {
      public static final long MAX_OCR_FILE_SIZE = 50L * 1024 * 1024;
      public static final int THUMBNAIL_WIDTH = 300;
      public static final int THUMBNAIL_HEIGHT = 300;
  }
  ```
- **검증 방법**: 모든 하드코딩된 값이 상수로 대체되었는지 확인
- **위험도**: 🟢 낮음

**1.2 OCR 서비스 리팩토링**
- **소요 시간**: 3일
- **담당자**: 미디어 도메인 개발자
- **작업 내용**:
  - `extractTextFromImage()`와 `extractTextFromBytes()` 중복 로직 제거
  - 공통 처리 메서드 `processOCRRequest()` 추출
  - 에러 처리 로직 통합
- **검증 방법**: 기존 테스트 케이스 통과, 코드 중복도 측정
- **위험도**: 🟡 중간

**1.3 유틸리티 클래스 추출**
- **소요 시간**: 2일
- **담당자**: 미디어 도메인 개발자
- **작업 내용**:
  - `TextAnalysisUtils.java` 생성
  - 파일명 생성, 확장자 추출 등 공통 로직 분리
- **검증 방법**: 단위 테스트 작성 및 통과
- **위험도**: 🟢 낮음

#### 📊 Phase 1 성과 지표
- 코드 중복도: 15% → 5%
- 매직 넘버: 20개 → 0개
- 유지보수 지수: 60 → 80

### Phase 2: 성능 최적화 (2-4주)

#### 🎯 목표: 응답 시간 50% 단축

**2.1 비동기 처리 도입 (`AsyncMediaService.java`)**
- **소요 시간**: 1주
- **담당자**: 미디어 도메인 개발자
- **작업 내용**:
  ```java
  @Async("mediaTaskExecutor")
  public CompletableFuture<Map<String, Object>> processOCRAsync(Long mediaId, byte[] imageBytes) {
      return CompletableFuture.supplyAsync(() -> {
          // OCR 처리 로직
      }).thenApply(result -> {
          // 메타데이터 업데이트
          return result;
      });
  }
  ```
- **설정 추가**:
  ```java
  @Configuration
  @EnableAsync
  public class AsyncConfig {
      @Bean("mediaTaskExecutor")
      public TaskExecutor mediaTaskExecutor() {
          ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
          executor.setCorePoolSize(5);
          executor.setMaxPoolSize(20);
          executor.setQueueCapacity(100);
          return executor;
      }
  }
  ```
- **검증 방법**: 부하 테스트로 응답 시간 측정
- **위험도**: 🟡 중간

**2.2 Google Vision API 연결 풀링**
- **소요 시간**: 3일
- **담당자**: 미디어 도메인 개발자
- **작업 내용**:
  ```java
  @Component
  public class GoogleVisionClientFactory {
      private final ObjectPool<ImageAnnotatorClient> clientPool;
      
      @PostConstruct
      public void initializePool() {
          clientPool = new GenericObjectPool<>(
              new ImageAnnotatorClientFactory(), 
              createPoolConfig()
          );
      }
  }
  ```
- **검증 방법**: 연결 재사용률 모니터링
- **위험도**: 🟡 중간

**2.3 데이터베이스 쿼리 최적화**
- **소요 시간**: 2일
- **담당자**: 미디어 도메인 개발자
- **작업 내용**:
  - N+1 쿼리 문제 해결
  - 인덱스 최적화
  - 배치 업데이트 구현
- **검증 방법**: 쿼리 실행 계획 분석
- **위험도**: 🟢 낮음

#### 📊 Phase 2 성과 지표
- 파일 업로드 시간: 10초 → 5초
- OCR 처리 시간: 20초 → 10초 (비동기)
- 동시 처리 용량: 100명 → 300명

### Phase 3: 보안 강화 (3-4주)

#### 🎯 목표: 고급 위협 탐지 및 방어

**3.1 고급 보안 검증 (`EnhancedSecurityService.java`)**
- **소요 시간**: 1주
- **담당자**: 미디어 도메인 개발자 + 보안 전문가
- **작업 내용**:
  - 스테가노그래피 탐지
  - 파일 평판 검사
  - 고급 폴리글롯 탐지
  - 엔트로피 분석
- **검증 방법**: 악성 파일 샘플로 탐지율 테스트
- **위험도**: 🔴 높음

**3.2 보안 감사 로깅**
- **소요 시간**: 3일
- **담당자**: 미디어 도메인 개발자
- **작업 내용**:
  ```java
  @Component
  public class SecurityAuditLogger {
      public void logSecurityEvent(String eventType, Long userId, String filename, String details) {
          SecurityEvent event = SecurityEvent.builder()
              .eventType(eventType)
              .userId(userId)
              .filename(filename)
              .details(details)
              .timestamp(LocalDateTime.now())
              .build();
          
          auditRepository.save(event);
          alertService.checkThresholds(event);
      }
  }
  ```
- **검증 방법**: 보안 이벤트 로그 수집 및 분석
- **위험도**: 🟢 낮음

**3.3 Rate Limiting 구현**
- **소요 시간**: 2일
- **담당자**: 미디어 도메인 개발자
- **작업 내용**:
  - 사용자별 업로드 제한
  - IP 기반 제한
  - Redis를 활용한 분산 Rate Limiting
- **검증 방법**: 부하 테스트로 제한 동작 확인
- **위험도**: 🟡 중간

#### 📊 Phase 3 성과 지표
- 보안 위협 탐지율: 80% → 95%
- 보안 이벤트 응답 시간: 10분 → 1분
- 악성 파일 차단율: 90% → 99%

### Phase 4: 아키텍처 개선 (4-8주)

#### 🎯 목표: 확장성 및 유지보수성 향상

**4.1 이벤트 기반 아키텍처 도입**
- **소요 시간**: 3주
- **담당자**: 미디어 도메인 개발자 + 아키텍트
- **작업 내용**:
  ```java
  // 도메인 이벤트 정의
  public class MediaUploadedEvent extends ApplicationEvent {
      private final Long mediaId;
      private final Long userId;
      private final String mimeType;
  }
  
  // 이벤트 핸들러
  @EventListener
  @Async
  public void handleMediaUploaded(MediaUploadedEvent event) {
      if (isImageFile(event.getMimeType())) {
          thumbnailService.generateThumbnailAsync(event.getMediaId());
      }
  }
  ```
- **검증 방법**: 이벤트 발행/구독 정상 동작 확인
- **위험도**: 🔴 높음

**4.2 Circuit Breaker 패턴 적용**
- **소요 시간**: 1주
- **담당자**: 미디어 도메인 개발자
- **작업 내용**:
  ```java
  @Component
  public class ResilientOCRService {
      @CircuitBreaker(name = "google-vision", fallbackMethod = "fallbackOCR")
      @Retry(name = "google-vision")
      @TimeLimiter(name = "google-vision")
      public CompletableFuture<Map<String, Object>> extractTextAsync(byte[] imageBytes) {
          return CompletableFuture.supplyAsync(() -> ocrService.extractText(imageBytes));
      }
      
      public CompletableFuture<Map<String, Object>> fallbackOCR(Exception ex) {
          return CompletableFuture.completedFuture(Map.of(
              "success", false,
              "error", "OCR service temporarily unavailable"
          ));
      }
  }
  ```
- **검증 방법**: 외부 서비스 장애 시뮬레이션
- **위험도**: 🟡 중간

**4.3 메트릭 및 모니터링 시스템**
- **소요 시간**: 2주
- **담당자**: 미디어 도메인 개발자 + DevOps
- **작업 내용**:
  ```java
  @Component
  public class MediaMetrics {
      private final Counter uploadCounter;
      private final Timer ocrProcessingTimer;
      private final Counter securityThreatsCounter;
      
      public void recordUpload(boolean success) {
          uploadCounter.increment(Tags.of("status", success ? "success" : "failure"));
      }
      
      public void recordOCRProcessing(Duration duration) {
          ocrProcessingTimer.record(duration);
      }
  }
  ```
- **검증 방법**: Grafana 대시보드에서 메트릭 확인
- **위험도**: 🟢 낮음

#### 📊 Phase 4 성과 지표
- 시스템 가용성: 99.5% → 99.9%
- 장애 복구 시간: 30분 → 5분
- 모니터링 커버리지: 50% → 90%

### Phase 5: 장기 개선 (6개월+)

#### 🎯 목표: 차세대 아키텍처 구축

**5.1 마이크로서비스 분리 검토**
- **소요 시간**: 3개월
- **담당자**: 아키텍처 팀 + 미디어 도메인 개발자
- **작업 내용**:
  - Media Upload Service
  - OCR Processing Service
  - File Storage Service
  - Notification Service
- **검증 방법**: 서비스 간 통신 성능 및 안정성 테스트
- **위험도**: 🔴 높음

**5.2 다중 스토리지 지원**
- **소요 시간**: 2개월
- **담당자**: 미디어 도메인 개발자 + 인프라 팀
- **작업 내용**:
  - AWS S3 + Google Cloud Storage
  - 자동 failover 구현
  - 데이터 동기화 전략
- **검증 방법**: 스토리지 장애 시나리오 테스트
- **위험도**: 🔴 높음

---

## 🛠️ 구현 가이드라인

### 개발 원칙
1. **점진적 개선**: 기존 기능에 영향을 주지 않는 범위에서 단계적 개선
2. **테스트 우선**: 모든 변경사항에 대해 테스트 케이스 작성
3. **문서화**: 변경사항에 대한 상세한 문서 작성
4. **모니터링**: 성능 지표 지속적 모니터링
5. **롤백 계획**: 각 단계별 롤백 시나리오 준비

### 코드 품질 기준
- **테스트 커버리지**: 최소 80% (목표 90%)
- **코드 복잡도**: Cyclomatic Complexity < 10
- **중복도**: 5% 미만
- **문서화**: 모든 public 메서드 JavaDoc 작성

### 성능 기준
- **API 응답 시간**: 95th percentile < 5초
- **메모리 사용량**: 힙 사용률 < 80%
- **CPU 사용률**: 평균 < 70%
- **에러율**: < 1%

---

## 🧪 테스트 전략

### 단위 테스트
```java
@ExtendWith(MockitoExtension.class)
class MediaServiceTest {
    
    @Mock
    private S3Service s3Service;
    
    @Mock
    private FileValidationService fileValidationService;
    
    @InjectMocks
    private MediaService mediaService;
    
    @Test
    void uploadFile_Success() {
        // Given
        MultipartFile file = createMockFile();
        when(s3Service.uploadFile(any(), any(), any())).thenReturn("s3-url");
        
        // When
        MediaUploadResponse response = mediaService.uploadFile(createRequest(file), 1L);
        
        // Then
        assertThat(response.getS3Url()).isEqualTo("s3-url");
        verify(fileValidationService).validateFile(file);
    }
}
```

### 통합 테스트
```java
@SpringBootTest
@Testcontainers
class MediaIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("compass_test")
            .withUsername("test")
            .withPassword("test");
    
    @Test
    void uploadAndRetrieveFile_Success() {
        // 전체 플로우 테스트
    }
}
```

### 성능 테스트
```java
@Test
void loadTest_ConcurrentUploads() {
    int numberOfThreads = 100;
    int numberOfRequestsPerThread = 10;
    
    ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
    CountDownLatch latch = new CountDownLatch(numberOfThreads * numberOfRequestsPerThread);
    
    // 동시 업로드 테스트
}
```

---

## 📊 모니터링 및 알림

### 핵심 메트릭
```yaml
# Prometheus 메트릭 설정
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: compass-media
      environment: ${ENVIRONMENT:dev}
```

### 알림 규칙
```yaml
# AlertManager 규칙
groups:
  - name: media-domain
    rules:
      - alert: HighUploadFailureRate
        expr: rate(media_upload_failures_total[5m]) > 0.05
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "High upload failure rate detected"
          
      - alert: OCRProcessingTimeout
        expr: histogram_quantile(0.95, rate(ocr_processing_duration_seconds_bucket[5m])) > 30
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "OCR processing taking too long"
```

### 대시보드 구성
1. **비즈니스 메트릭**
   - 일일 업로드 수
   - 성공률 추이
   - 사용자별 사용량

2. **기술 메트릭**
   - API 응답 시간
   - 에러율
   - 리소스 사용량

3. **보안 메트릭**
   - 위협 탐지 수
   - 차단된 파일 수
   - 의심스러운 활동

---

## 🚨 위험 관리

### 주요 위험 요소

| 위험 | 확률 | 영향도 | 완화 방안 |
|------|------|--------|----------|
| **외부 API 장애** | 중간 | 높음 | Circuit Breaker, Fallback 구현 |
| **데이터 손실** | 낮음 | 매우 높음 | 백업 전략, 트랜잭션 관리 |
| **성능 저하** | 높음 | 중간 | 점진적 배포, 모니터링 |
| **보안 취약점** | 중간 | 높음 | 보안 테스트, 코드 리뷰 |
| **호환성 문제** | 낮음 | 중간 | 버전 관리, 테스트 자동화 |

### 롤백 계획

**Phase 1-2 (코드 개선)**
- Git 브랜치 전략으로 즉시 롤백 가능
- 기능 플래그를 통한 점진적 적용

**Phase 3-4 (아키텍처 변경)**
- Blue-Green 배포 전략
- 데이터베이스 마이그레이션 롤백 스크립트
- 외부 서비스 연동 설정 백업

**Phase 5 (마이크로서비스)**
- 서비스별 독립 배포
- API Gateway를 통한 트래픽 라우팅 제어
- 데이터 동기화 검증 도구

---

## 📈 ROI 분석

### 투자 비용
| 항목 | 인력 (인월) | 비용 (만원) |
|------|-------------|-------------|
| **Phase 1-2** | 1.5 | 750 |
| **Phase 3** | 1.0 | 500 |
| **Phase 4** | 2.0 | 1,000 |
| **Phase 5** | 6.0 | 3,000 |
| **총계** | 10.5 | 5,250 |

### 예상 효과
| 효과 | 연간 절감 (만원) |
|------|------------------|
| **서버 비용 절감** | 1,200 |
| **개발 생산성 향상** | 2,400 |
| **장애 대응 비용 절감** | 800 |
| **보안 사고 예방** | 1,000 |
| **총계** | 5,400 |

**ROI**: (5,400 - 5,250) / 5,250 × 100 = **2.9%** (1년차)
**누적 ROI**: 2년차부터 연간 5,400만원 순이익

---

## 🎯 실행 체크리스트

### Phase 1 체크리스트
- [ ] `MediaConstants.java` 생성 및 상수 이전
- [ ] OCR 서비스 중복 코드 제거
- [ ] 유틸리티 클래스 추출
- [ ] 단위 테스트 작성 및 실행
- [ ] 코드 리뷰 완료
- [ ] 문서 업데이트

### Phase 2 체크리스트
- [ ] 비동기 처리 설정 추가
- [ ] `AsyncMediaService` 구현
- [ ] Google Vision 연결 풀 구현
- [ ] 데이터베이스 쿼리 최적화
- [ ] 성능 테스트 실행
- [ ] 모니터링 메트릭 확인

### Phase 3 체크리스트
- [ ] `EnhancedSecurityService` 구현
- [ ] 보안 감사 로깅 추가
- [ ] Rate Limiting 구현
- [ ] 보안 테스트 실행
- [ ] 침투 테스트 수행
- [ ] 보안 문서 업데이트

### Phase 4 체크리스트
- [ ] 도메인 이벤트 정의
- [ ] 이벤트 핸들러 구현
- [ ] Circuit Breaker 적용
- [ ] 메트릭 수집 시스템 구축
- [ ] 대시보드 구성
- [ ] 알림 규칙 설정

---

## 📚 참고 자료

### 내부 문서
- [미디어 도메인 코드 인덱스](./MEDIA_DOMAIN_CODE_INDEX.md)
- [미디어 도메인 아키텍처](./MEDIA_DOMAIN_ARCHITECTURE.md)
- [개선 사항 상세 분석](./analysis_improvements/MEDIA_DOMAIN_IMPROVEMENT_RECOMMENDATIONS.md)

### 외부 자료
- [Spring Boot Async Processing](https://spring.io/guides/gs/async-method/)
- [Circuit Breaker Pattern](https://martinfowler.com/bliki/CircuitBreaker.html)
- [Microservices Patterns](https://microservices.io/patterns/)
- [Google Cloud Vision Best Practices](https://cloud.google.com/vision/docs/best-practices)

### 도구 및 라이브러리
- **Resilience4j**: Circuit Breaker, Retry, Rate Limiter
- **Micrometer**: 메트릭 수집
- **Testcontainers**: 통합 테스트
- **JMeter**: 성능 테스트
- **SonarQube**: 코드 품질 분석

---

**문서 버전**: 1.0  
**최종 업데이트**: 2024년 1월  
**담당자**: 미디어 도메인 전담 개발자  
**승인자**: 기술 리더, 프로젝트 매니저  
**검토 주기**: 월 1회 진행 상황 검토