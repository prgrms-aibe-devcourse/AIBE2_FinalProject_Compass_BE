# 미디어 도메인 아키텍처 및 의존성 매핑

## 📋 개요

이 문서는 Compass 백엔드 프로젝트의 **미디어 도메인** 아키텍처와 의존성 관계를 시각적으로 표현하고 분석합니다.

**생성일**: 2024년 1월  
**담당자**: 미디어 도메인 전담 개발자  
**범위**: `com.compass.domain.media` 패키지 전체

---

## 🏗️ 전체 아키텍처 다이어그램

```mermaid
graph TB
    %% External Services
    S3[AWS S3<br/>파일 저장소]
    GCV[Google Cloud Vision<br/>OCR API]
    DB[(PostgreSQL<br/>데이터베이스)]
    
    %% Controller Layer
    MC[MediaController<br/>REST API]
    
    %% Service Layer
    MS[MediaService<br/>핵심 비즈니스 로직]
    OS[OCRService<br/>텍스트 추출]
    S3S[S3Service<br/>파일 업로드/다운로드]
    TS[ThumbnailService<br/>썸네일 생성]
    FVS[FileValidationService<br/>파일 검증]
    
    %% Repository Layer
    MR[MediaRepository<br/>데이터 접근]
    UR[UserRepository<br/>사용자 정보]
    
    %% Entity Layer
    ME[Media Entity<br/>미디어 정보]
    UE[User Entity<br/>사용자 정보]
    
    %% Configuration
    MVP[MediaValidationProperties<br/>검증 설정]
    S3C[S3Configuration<br/>S3 설정]
    
    %% Exception Handling
    MEH[MediaExceptionHandler<br/>예외 처리]
    
    %% Request Flow
    CLIENT[클라이언트] --> MC
    MC --> MS
    MS --> FVS
    MS --> S3S
    MS --> TS
    MS --> OS
    MS --> MR
    MS --> UR
    
    %% Service Dependencies
    S3S --> S3
    OS --> GCV
    MR --> DB
    UR --> DB
    
    %% Entity Relationships
    MR --> ME
    UR --> UE
    ME -.-> UE
    
    %% Configuration Dependencies
    FVS --> MVP
    S3S --> S3C
    
    %% Exception Handling
    MC --> MEH
    
    %% Styling
    classDef controller fill:#e1f5fe
    classDef service fill:#f3e5f5
    classDef repository fill:#e8f5e8
    classDef entity fill:#fff3e0
    classDef external fill:#ffebee
    classDef config fill:#f1f8e9
    
    class MC controller
    class MS,OS,S3S,TS,FVS service
    class MR,UR repository
    class ME,UE entity
    class S3,GCV,DB external
    class MVP,S3C,MEH config
```

---

## 🔄 계층별 상세 아키텍처

### 1. Presentation Layer (프레젠테이션 계층)

```mermaid
graph LR
    CLIENT[클라이언트<br/>웹/모바일] --> JWT[JWT 인증]
    JWT --> MC[MediaController]
    
    MC --> UPLOAD[POST /upload<br/>파일 업로드]
    MC --> GET[GET /{id}<br/>파일 조회]
    MC --> LIST[GET /list<br/>목록 조회]
    MC --> DELETE[DELETE /{id}<br/>파일 삭제]
    MC --> OCR_POST[POST /{id}/ocr<br/>OCR 처리]
    MC --> OCR_GET[GET /{id}/ocr<br/>OCR 결과]
    MC --> HEALTH[GET /health<br/>헬스 체크]
    
    classDef endpoint fill:#e3f2fd
    class UPLOAD,GET,LIST,DELETE,OCR_POST,OCR_GET,HEALTH endpoint
```

### 2. Business Logic Layer (비즈니스 로직 계층)

```mermaid
graph TB
    MS[MediaService<br/>핵심 서비스]
    
    %% Core Operations
    MS --> UPLOAD_OP[파일 업로드 처리]
    MS --> RETRIEVE_OP[파일 조회 처리]
    MS --> DELETE_OP[파일 삭제 처리]
    MS --> OCR_OP[OCR 처리 관리]
    
    %% Service Dependencies
    UPLOAD_OP --> FVS[FileValidationService<br/>파일 검증]
    UPLOAD_OP --> S3S[S3Service<br/>S3 업로드]
    UPLOAD_OP --> TS[ThumbnailService<br/>썸네일 생성]
    
    OCR_OP --> OS[OCRService<br/>텍스트 추출]
    OCR_OP --> S3S
    
    RETRIEVE_OP --> S3S
    DELETE_OP --> S3S
    
    %% Data Access
    MS --> MR[MediaRepository]
    MS --> UR[UserRepository]
    
    classDef core fill:#f3e5f5
    classDef operation fill:#e8eaf6
    classDef support fill:#e0f2f1
    
    class MS core
    class UPLOAD_OP,RETRIEVE_OP,DELETE_OP,OCR_OP operation
    class FVS,S3S,TS,OS,MR,UR support
```

### 3. Data Access Layer (데이터 접근 계층)

```mermaid
graph TB
    %% Repository Layer
    MR[MediaRepository<br/>미디어 데이터 접근]
    UR[UserRepository<br/>사용자 데이터 접근]
    
    %% Entity Layer
    ME[Media Entity<br/>미디어 정보]
    UE[User Entity<br/>사용자 정보]
    FS[FileStatus Enum<br/>파일 상태]
    
    %% Database
    DB[(PostgreSQL<br/>데이터베이스)]
    MEDIA_TABLE[media 테이블]
    USER_TABLE[users 테이블]
    
    %% Relationships
    MR --> ME
    UR --> UE
    ME --> FS
    ME -.->|ManyToOne| UE
    
    MR --> DB
    UR --> DB
    DB --> MEDIA_TABLE
    DB --> USER_TABLE
    
    %% Repository Methods
    MR --> FIND_BY_USER[findByUserAndDeletedFalse]
    MR --> FIND_BY_ID[findByIdAndDeletedFalse]
    MR --> COUNT_BY_USER[countByUserAndDeletedFalse]
    
    classDef repository fill:#e8f5e8
    classDef entity fill:#fff3e0
    classDef database fill:#ffebee
    
    class MR,UR repository
    class ME,UE,FS entity
    class DB,MEDIA_TABLE,USER_TABLE database
```

---

## 🔗 의존성 매트릭스

### 서비스 간 의존성

| 서비스 | MediaService | OCRService | S3Service | ThumbnailService | FileValidationService |
|--------|--------------|------------|-----------|------------------|-----------------------|
| **MediaService** | - | ✅ | ✅ | ✅ | ✅ |
| **OCRService** | ❌ | - | ❌ | ❌ | ❌ |
| **S3Service** | ❌ | ❌ | - | ❌ | ❌ |
| **ThumbnailService** | ❌ | ❌ | ❌ | - | ❌ |
| **FileValidationService** | ❌ | ❌ | ❌ | ❌ | - |

**범례**: ✅ 의존함, ❌ 의존하지 않음

### 외부 서비스 의존성

| 컴포넌트 | AWS S3 | Google Vision | PostgreSQL | JWT |
|----------|--------|---------------|------------|-----|
| **MediaController** | ❌ | ❌ | ❌ | ✅ |
| **MediaService** | ❌ | ❌ | ❌ | ❌ |
| **OCRService** | ❌ | ✅ | ❌ | ❌ |
| **S3Service** | ✅ | ❌ | ❌ | ❌ |
| **MediaRepository** | ❌ | ❌ | ✅ | ❌ |

---

## 📊 데이터 플로우 다이어그램

### 1. 파일 업로드 플로우

```mermaid
sequenceDiagram
    participant C as 클라이언트
    participant MC as MediaController
    participant MS as MediaService
    participant FVS as FileValidationService
    participant S3S as S3Service
    participant TS as ThumbnailService
    participant MR as MediaRepository
    participant S3 as AWS S3
    participant DB as PostgreSQL
    
    C->>MC: POST /upload (파일)
    MC->>MS: uploadFile()
    MS->>FVS: validateFile()
    FVS-->>MS: 검증 완료
    
    MS->>S3S: uploadFile()
    S3S->>S3: putObject()
    S3-->>S3S: S3 URL
    S3S-->>MS: S3 URL
    
    alt 이미지 파일인 경우
        MS->>TS: generateThumbnail()
        TS-->>MS: 썸네일 데이터
        MS->>S3S: uploadThumbnail()
        S3S->>S3: putObject(썸네일)
        S3-->>S3S: 썸네일 URL
        S3S-->>MS: 썸네일 URL
    end
    
    MS->>MR: save(Media)
    MR->>DB: INSERT
    DB-->>MR: 저장 완료
    MR-->>MS: Media 엔티티
    MS-->>MC: MediaUploadResponse
    MC-->>C: 업로드 완료
```

### 2. OCR 처리 플로우

```mermaid
sequenceDiagram
    participant C as 클라이언트
    participant MC as MediaController
    participant MS as MediaService
    participant S3S as S3Service
    participant OS as OCRService
    participant MR as MediaRepository
    participant S3 as AWS S3
    participant GCV as Google Vision
    participant DB as PostgreSQL
    
    C->>MC: POST /{id}/ocr
    MC->>MS: processOCRForMedia()
    
    MS->>MR: findById()
    MR->>DB: SELECT
    DB-->>MR: Media 엔티티
    MR-->>MS: Media 엔티티
    
    MS->>S3S: downloadFile()
    S3S->>S3: getObject()
    S3-->>S3S: 파일 데이터
    S3S-->>MS: 바이트 배열
    
    MS->>OS: extractTextFromBytes()
    OS->>GCV: batchAnnotateImages()
    GCV-->>OS: OCR 결과
    OS-->>MS: OCR 메타데이터
    
    MS->>MR: save(업데이트된 Media)
    MR->>DB: UPDATE
    DB-->>MR: 업데이트 완료
    MR-->>MS: 완료
    MS-->>MC: OCR 결과
    MC-->>C: OCR 완료
```

### 3. 파일 조회 플로우

```mermaid
sequenceDiagram
    participant C as 클라이언트
    participant MC as MediaController
    participant MS as MediaService
    participant S3S as S3Service
    participant MR as MediaRepository
    participant DB as PostgreSQL
    
    C->>MC: GET /{id}
    MC->>MS: getMediaById()
    
    MS->>MR: findByIdAndUserAndDeletedFalse()
    MR->>DB: SELECT
    DB-->>MR: Media 엔티티
    MR-->>MS: Media 엔티티
    
    MS->>S3S: generatePresignedUrl()
    S3S-->>MS: Presigned URL (15분)
    
    MS-->>MC: MediaGetResponse
    MC-->>C: 파일 정보 + Presigned URL
```

---

## 🔧 기술 스택 의존성

### 1. 핵심 프레임워크

```mermaid
graph TB
    SB[Spring Boot 3.x]
    SB --> SW[Spring Web]
    SB --> SD[Spring Data JPA]
    SB --> ST[Spring Transaction]
    SB --> SS[Spring Security]
    
    SW --> MC[MediaController]
    SD --> MR[MediaRepository]
    ST --> MS[MediaService]
    SS --> JWT[JWT 인증]
```

### 2. 외부 라이브러리

```mermaid
graph LR
    %% AWS SDK
    AWS[AWS SDK for Java]
    AWS --> S3C[S3Client]
    S3C --> S3S[S3Service]
    
    %% Google Cloud
    GC[Google Cloud Vision]
    GC --> IAC[ImageAnnotatorClient]
    IAC --> OS[OCRService]
    
    %% Image Processing
    TN[Thumbnailator]
    TN --> TS[ThumbnailService]
    
    %% JSON Processing
    HU[Hypersistence Utils]
    HU --> JSON[JsonType]
    JSON --> ME[Media Entity]
    
    %% Validation
    JV[Jakarta Validation]
    JV --> DTO[MediaDto]
```

### 3. 데이터베이스 의존성

```mermaid
graph TB
    PG[PostgreSQL 15+]
    PG --> JSONB[JSONB 지원]
    PG --> JPA[JPA/Hibernate]
    
    JSONB --> META[메타데이터 저장]
    JPA --> MR[MediaRepository]
    JPA --> ME[Media Entity]
    
    META --> OCR_DATA[OCR 결과]
    META --> THUMB_DATA[썸네일 정보]
    META --> FILE_DATA[파일 메타데이터]
```

---

## 🚦 의존성 분석

### 1. 강한 결합 (Strong Coupling)

**MediaService ↔ 다른 서비스들**
- **위험도**: 🔴 높음
- **이유**: MediaService가 모든 서비스에 의존
- **개선 방안**: 이벤트 기반 아키텍처 도입

```java
// 현재 - 강한 결합
@Service
public class MediaService {
    private final OCRService ocrService;
    private final S3Service s3Service;
    private final ThumbnailService thumbnailService;
    private final FileValidationService fileValidationService;
    // ...
}

// 개선 - 이벤트 기반
@EventListener
public void handleFileUploaded(FileUploadedEvent event) {
    // 비동기 처리
}
```

### 2. 외부 서비스 의존성

**OCRService → Google Cloud Vision**
- **위험도**: 🟡 중간
- **이유**: 외부 API 장애 시 전체 OCR 기능 중단
- **개선 방안**: Circuit Breaker 패턴, 대체 서비스

**S3Service → AWS S3**
- **위험도**: 🟡 중간
- **이유**: S3 장애 시 파일 업로드/다운로드 불가
- **개선 방안**: 다중 스토리지 지원, 로컬 백업

### 3. 순환 의존성 검사

✅ **순환 의존성 없음** - 모든 의존성이 단방향으로 구성됨

```
Controller → Service → Repository → Entity
     ↓         ↓          ↓
   DTO    External   Database
```

---

## 📈 성능 영향 분석

### 1. 병목 지점 (Bottlenecks)

```mermaid
graph LR
    REQ[요청] --> SYNC[동기 처리]
    SYNC --> OCR[OCR 처리<br/>⏱️ 3-10초]
    SYNC --> THUMB[썸네일 생성<br/>⏱️ 1-3초]
    SYNC --> S3[S3 업로드<br/>⏱️ 1-5초]
    
    OCR --> BLOCK[스레드 블로킹]
    THUMB --> BLOCK
    S3 --> BLOCK
    
    classDef bottleneck fill:#ffcdd2
    class OCR,THUMB,S3,BLOCK bottleneck
```

### 2. 리소스 사용량

| 컴포넌트 | CPU | 메모리 | 네트워크 | 디스크 |
|----------|-----|--------|----------|--------|
| **FileValidationService** | 🔴 높음 | 🟡 중간 | 🟢 낮음 | 🟢 낮음 |
| **OCRService** | 🟡 중간 | 🔴 높음 | 🔴 높음 | 🟢 낮음 |
| **ThumbnailService** | 🔴 높음 | 🔴 높음 | 🟢 낮음 | 🟢 낮음 |
| **S3Service** | 🟢 낮음 | 🟡 중간 | 🔴 높음 | 🟢 낮음 |

---

## 🔒 보안 아키텍처

### 1. 보안 계층

```mermaid
graph TB
    %% Security Layers
    AUTH[인증 계층<br/>JWT Token]
    AUTHZ[인가 계층<br/>사용자 권한]
    VALID[검증 계층<br/>파일 검증]
    ENCRYPT[암호화 계층<br/>전송 보안]
    
    %% Components
    MC[MediaController] --> AUTH
    AUTH --> AUTHZ
    AUTHZ --> MS[MediaService]
    MS --> VALID
    VALID --> FVS[FileValidationService]
    
    %% Security Features
    FVS --> MIME[MIME 타입 검증]
    FVS --> HEADER[파일 헤더 검증]
    FVS --> MALWARE[악성코드 스캔]
    FVS --> PATH[Path Traversal 방지]
    
    %% External Security
    S3S[S3Service] --> ENCRYPT
    ENCRYPT --> HTTPS[HTTPS 통신]
    ENCRYPT --> PRESIGNED[Presigned URL]
    
    classDef security fill:#e8f5e8
    classDef validation fill:#fff3e0
    
    class AUTH,AUTHZ,VALID,ENCRYPT security
    class MIME,HEADER,MALWARE,PATH validation
```

### 2. 보안 검증 플로우

```mermaid
sequenceDiagram
    participant F as 파일
    participant FVS as FileValidationService
    participant MVP as MediaValidationProperties
    
    F->>FVS: 파일 업로드
    FVS->>FVS: validateFileSize()
    FVS->>MVP: 최대 크기 확인
    FVS->>FVS: validateMimeType()
    FVS->>MVP: 허용 MIME 타입 확인
    FVS->>FVS: validateFileExtension()
    FVS->>MVP: 허용 확장자 확인
    FVS->>FVS: validateFileHeader()
    FVS->>FVS: validateSecureFilename()
    FVS->>FVS: scanForMaliciousContent()
    FVS->>MVP: 악성 시그니처 확인
    FVS->>FVS: validateImageMetadata()
    FVS-->>F: 검증 완료/실패
```

---

## 🔄 개선된 아키텍처 제안

### 1. 이벤트 기반 아키텍처

```mermaid
graph TB
    %% Current Architecture
    subgraph "현재 아키텍처"
        MS1[MediaService] --> OS1[OCRService]
        MS1 --> TS1[ThumbnailService]
        MS1 --> S3S1[S3Service]
    end
    
    %% Proposed Architecture
    subgraph "제안 아키텍처"
        MS2[MediaService] --> EB[Event Bus]
        EB --> OCR_HANDLER[OCR Handler]
        EB --> THUMB_HANDLER[Thumbnail Handler]
        EB --> NOTIFICATION[Notification Handler]
        
        OCR_HANDLER --> OS2[OCRService]
        THUMB_HANDLER --> TS2[ThumbnailService]
    end
    
    classDef current fill:#ffcdd2
    classDef proposed fill:#c8e6c9
    
    class MS1,OS1,TS1,S3S1 current
    class MS2,EB,OCR_HANDLER,THUMB_HANDLER,NOTIFICATION,OS2,TS2 proposed
```

### 2. 마이크로서비스 분리 제안

```mermaid
graph TB
    %% API Gateway
    AG[API Gateway]
    
    %% Microservices
    subgraph "Media Upload Service"
        MUS[Upload Controller]
        MUS_S[Upload Service]
        MUS_R[Media Repository]
    end
    
    subgraph "OCR Processing Service"
        OPS[OCR Controller]
        OPS_S[OCR Service]
        OPS_Q[OCR Queue]
    end
    
    subgraph "File Storage Service"
        FSS[Storage Controller]
        FSS_S[Storage Service]
        FSS_S3[S3 Client]
    end
    
    %% Connections
    AG --> MUS
    AG --> OPS
    AG --> FSS
    
    MUS_S --> FSS_S
    MUS_S --> OPS_Q
    OPS_S --> FSS_S
```

---

## 📊 메트릭 및 모니터링

### 1. 핵심 메트릭

```mermaid
graph LR
    %% Business Metrics
    BM[비즈니스 메트릭]
    BM --> UPLOAD_RATE[업로드 성공률]
    BM --> OCR_ACCURACY[OCR 정확도]
    BM --> USER_SATISFACTION[사용자 만족도]
    
    %% Technical Metrics
    TM[기술 메트릭]
    TM --> RESPONSE_TIME[응답 시간]
    TM --> THROUGHPUT[처리량]
    TM --> ERROR_RATE[에러율]
    TM --> RESOURCE_USAGE[리소스 사용량]
    
    %% Security Metrics
    SM[보안 메트릭]
    SM --> THREAT_DETECTION[위협 탐지 수]
    SM --> VALIDATION_FAILURES[검증 실패 수]
    SM --> SUSPICIOUS_ACTIVITY[의심 활동]
```

### 2. 알림 및 대시보드

| 메트릭 | 임계값 | 알림 레벨 | 대응 방안 |
|--------|--------|-----------|----------|
| **업로드 실패율** | > 5% | 🟡 경고 | 로그 분석, 서비스 상태 확인 |
| **OCR 처리 시간** | > 30초 | 🟡 경고 | Google Vision API 상태 확인 |
| **S3 업로드 실패** | > 1% | 🔴 심각 | AWS 상태 확인, 대체 스토리지 |
| **보안 위협 탐지** | > 10건/시간 | 🔴 심각 | 보안팀 알림, IP 차단 검토 |

---

## 🎯 아키텍처 개선 로드맵

### Phase 1: 성능 최적화 (1-2개월)
- [ ] 비동기 처리 도입 (`AsyncMediaService`)
- [ ] 연결 풀링 구현 (`GoogleVisionClientFactory`)
- [ ] 캐싱 전략 수립
- [ ] 데이터베이스 쿼리 최적화

### Phase 2: 아키텍처 개선 (2-3개월)
- [ ] 이벤트 기반 아키텍처 도입
- [ ] 서비스 분리 및 모듈화
- [ ] Circuit Breaker 패턴 적용
- [ ] 메트릭 수집 시스템 구축

### Phase 3: 확장성 강화 (3-4개월)
- [ ] 마이크로서비스 분리 검토
- [ ] 다중 스토리지 지원
- [ ] 글로벌 CDN 연동
- [ ] 자동 스케일링 구현

---

**문서 버전**: 1.0  
**최종 업데이트**: 2024년 1월  
**담당자**: 미디어 도메인 전담 개발자  
**검토 주기**: 분기별 1회