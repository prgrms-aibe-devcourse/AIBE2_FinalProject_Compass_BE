# 미디어 도메인 코드 인덱스

## 📋 개요

이 문서는 Compass 백엔드 프로젝트의 **미디어 도메인** 전체 코드베이스에 대한 상세한 인덱스입니다. 미디어 도메인 전담 담당자를 위한 완전한 코드 가이드를 제공합니다.

**생성일**: 2024년 1월
**담당자**: 미디어 도메인 전담 개발자
**범위**: `com.compass.domain.media` 패키지 전체

---

## 🏗️ 아키텍처 개요

### 패키지 구조
```
com.compass.domain.media/
├── config/           # 설정 및 구성
├── controller/       # REST API 컨트롤러
├── dto/             # 데이터 전송 객체
├── entity/          # JPA 엔티티
├── exception/       # 예외 처리
├── repository/      # 데이터 접근 계층
└── service/         # 비즈니스 로직
```

### 핵심 기능
- **파일 업로드**: 이미지 파일 업로드 및 S3 저장
- **썸네일 생성**: 300x300 WebP 포맷 썸네일 자동 생성
- **OCR 처리**: Google Cloud Vision API를 통한 텍스트 추출
- **파일 검증**: 보안 및 형식 검증
- **메타데이터 관리**: JSON 형태의 파일 메타데이터

---

## 📁 상세 파일 인덱스

### 1. Entity Layer (엔티티 계층)

#### 📄 `Media.java` - 핵심 미디어 엔티티
**위치**: `src/main/java/com/compass/domain/media/entity/Media.java`
**라인 수**: 86줄

**주요 필드**:
- `id`: 미디어 고유 식별자 (Long, PK)
- `user`: 업로드한 사용자 (ManyToOne)
- `originalFilename`: 원본 파일명 (String, 500자)
- `storedFilename`: 저장된 파일명 (String, 500자)
- `s3Url`: S3 저장 URL (String, 1000자)
- `fileSize`: 파일 크기 (Long)
- `mimeType`: MIME 타입 (String, 100자)
- `status`: 파일 상태 (FileStatus enum)
- `metadata`: JSON 메타데이터 (Map<String, Object>)
- `deleted`: 삭제 여부 (Boolean)

**주요 메서드**:
- `updateS3Url()`: S3 URL 업데이트
- `updateStatus()`: 파일 상태 업데이트
- `updateMetadata()`: 메타데이터 업데이트
- `markAsDeleted()`: 논리적 삭제 처리

**특징**:
- BaseEntity 상속 (생성일/수정일 자동 관리)
- JSONB 타입으로 메타데이터 저장
- 논리적 삭제 지원

#### 📄 `FileStatus.java` - 파일 상태 열거형
**위치**: `src/main/java/com/compass/domain/media/entity/FileStatus.java`
**라인 수**: 9줄

**상태 값**:
- `UPLOADED`: 업로드 완료
- `PROCESSING`: 처리 중
- `COMPLETED`: 처리 완료
- `FAILED`: 처리 실패
- `DELETED`: 삭제됨

### 2. Service Layer (서비스 계층)

#### 📄 `MediaService.java` - 핵심 미디어 서비스
**위치**: `src/main/java/com/compass/domain/media/service/MediaService.java`
**라인 수**: 333줄

**주요 의존성**:
- `MediaRepository`: 데이터 접근
- `UserRepository`: 사용자 정보
- `FileValidationService`: 파일 검증
- `S3Service`: S3 업로드
- `ThumbnailService`: 썸네일 생성

**핵심 메서드**:
- `uploadFile()`: 파일 업로드 (109줄) - 메인 업로드 로직
- `deleteFile()`: 파일 삭제
- `getMediaById()`: 미디어 조회 및 Presigned URL 생성
- `getMediaListByUser()`: 사용자별 미디어 목록
- `processOCRForMedia()`: OCR 처리 실행
- `getOCRResult()`: OCR 결과 조회
- `createMediaHeaders()`: 캐시 헤더 생성

**특징**:
- 트랜잭션 관리 (`@Transactional`)
- 상세한 로깅
- 권한 검증 로직
- 메타데이터 자동 생성

#### 📄 `OCRService.java` - OCR 처리 서비스
**위치**: `src/main/java/com/compass/domain/media/service/OCRService.java`
**라인 수**: 310줄

**주요 기능**:
- Google Cloud Vision API 클라이언트 관리
- 이미지에서 텍스트 추출
- OCR 결과 메타데이터 생성

**핵심 메서드**:
- `createImageAnnotatorClient()`: Vision API 클라이언트 생성
- `extractTextFromImage()`: MultipartFile에서 텍스트 추출
- `extractTextFromBytes()`: 바이트 배열에서 텍스트 추출
- `calculateAverageConfidence()`: 신뢰도 계산
- `countWords()`: 단어 수 계산
- `countLines()`: 줄 수 계산

**특징**:
- Google Credentials 자동 로드
- 배치 처리 지원
- 상세한 OCR 결과 메타데이터
- 에러 처리 및 로깅

#### 📄 `S3Service.java` - AWS S3 서비스
**위치**: `src/main/java/com/compass/domain/media/service/S3Service.java`
**라인 수**: 256줄

**설정 값**:
- `bucketName`: S3 버킷명 (기본값: compass-media-bucket)
- `s3BaseUrl`: S3 기본 URL
- `region`: AWS 리전 (기본값: ap-northeast-2)

**핵심 메서드**:
- `uploadFile()`: 일반 파일 업로드
- `uploadThumbnail()`: 썸네일 업로드
- `deleteFile()`: 파일 삭제
- `generatePresignedUrl()`: Presigned URL 생성
- `downloadFile()`: 파일 다운로드
- `bucketExists()`: 버킷 존재 확인

**특징**:
- 메타데이터 자동 설정
- 사용자별 디렉토리 구조
- 상세한 에러 처리

#### 📄 `ThumbnailService.java` - 썸네일 생성 서비스
**위치**: `src/main/java/com/compass/domain/media/service/ThumbnailService.java`
**라인 수**: 157줄

**설정 값**:
- `THUMBNAIL_WIDTH`: 300px
- `THUMBNAIL_HEIGHT`: 300px
- `THUMBNAIL_FORMAT`: WebP
- `THUMBNAIL_PREFIX`: "thumbnail_"

**핵심 메서드**:
- `generateThumbnail()`: MultipartFile에서 썸네일 생성
- `generateThumbnailFromBytes()`: 바이트 배열에서 썸네일 생성
- `generateThumbnailFilename()`: 썸네일 파일명 생성
- `isImageFile()`: 이미지 파일 여부 확인
- `createThumbnailMetadata()`: 썸네일 메타데이터 생성

**특징**:
- Thumbnailator 라이브러리 사용
- 중앙 크롭 방식
- WebP 포맷 85% 품질
- 상세한 로깅

#### 📄 `FileValidationService.java` - 파일 검증 서비스
**위치**: `src/main/java/com/compass/domain/media/service/FileValidationService.java`
**라인 수**: 388줄

**검증 항목**:
- 파일 크기 검증
- MIME 타입 검증
- 파일 확장자 검증
- 파일 헤더 검증
- 보안 파일명 검증
- 악성 콘텐츠 스캔
- 이미지 메타데이터 검증

**보안 기능**:
- Path Traversal 공격 방지
- 스크립트 인젝션 방지
- Polyglot 파일 탐지
- 이미지 폭탄 탐지
- 악성 시그니처 검사

**핵심 메서드**:
- `validateFile()`: 종합 파일 검증
- `validateSecureFilename()`: 안전한 파일명 검증
- `scanForMaliciousContent()`: 악성 콘텐츠 스캔
- `validateImageMetadata()`: 이미지 메타데이터 검증
- `isSupportedImageFile()`: 지원 이미지 형식 확인

### 3. Controller Layer (컨트롤러 계층)

#### 📄 `MediaController.java` - REST API 컨트롤러
**위치**: `src/main/java/com/compass/domain/media/controller/MediaController.java`
**라인 수**: 233줄

**API 엔드포인트**:

1. **POST /api/media/upload** - 파일 업로드
   - 요청: MultipartFile + metadata
   - 응답: MediaUploadResponse
   - 최대 10MB 이미지 파일 지원

2. **GET /api/media/{id}** - 파일 조회
   - 응답: MediaGetResponse (Presigned URL 포함)
   - 15분 만료 URL
   - 캐시 헤더 설정

3. **GET /api/media/list** - 파일 목록 조회
   - 응답: List<MediaDto.ListResponse>
   - 사용자별 파일 목록

4. **DELETE /api/media/{id}** - 파일 삭제
   - 논리적 삭제 처리

5. **POST /api/media/{id}/ocr** - OCR 처리
   - 이미지에서 텍스트 추출
   - Google Cloud Vision API 사용

6. **GET /api/media/{id}/ocr** - OCR 결과 조회
   - 처리된 OCR 결과 반환

7. **GET /api/media/health** - 헬스 체크
   - 서비스 상태 확인

**특징**:
- JWT 토큰 기반 인증
- Swagger 문서화 완료
- 상세한 API 응답 코드
- 권한 검증 로직

### 4. DTO Layer (데이터 전송 객체)

#### 📄 `MediaDto.java` - 미디어 DTO 집합
**위치**: `src/main/java/com/compass/domain/media/dto/MediaDto.java`
**라인 수**: 100줄

**내부 클래스**:
- `UploadRequest`: 업로드 요청 DTO
- `UploadResponse`: 업로드 응답 DTO
- `GetResponse`: 조회 응답 DTO
- `ListResponse`: 목록 응답 DTO

#### 📄 `MediaUploadResponse.java` - 업로드 응답 DTO
**위치**: `src/main/java/com/compass/domain/media/dto/MediaUploadResponse.java`
**라인 수**: 37줄

#### 📄 `MediaGetResponse.java` - 조회 응답 DTO
#### 📄 `OCRResultDto.java` - OCR 결과 DTO

### 5. Repository Layer (데이터 접근 계층)

#### 📄 `MediaRepository.java` - 미디어 리포지토리
**위치**: `src/main/java/com/compass/domain/media/repository/MediaRepository.java`
**라인 수**: 33줄

**주요 메서드**:
- `findByUserAndDeletedFalseOrderByCreatedAtDesc()`: 사용자별 활성 미디어 조회
- `findByIdAndDeletedFalse()`: ID로 활성 미디어 조회
- `findByIdAndUserAndDeletedFalse()`: 사용자 권한 확인 조회
- `findActiveMediaByUser()`: 활성 미디어 목록 (JPQL)
- `countByUserAndDeletedFalse()`: 사용자별 미디어 수 계산

**특징**:
- 논리적 삭제 지원
- 사용자별 권한 검증
- 생성일 기준 정렬

### 6. Configuration Layer (설정 계층)

#### 📄 `MediaValidationProperties.java` - 검증 설정
**위치**: `src/main/java/com/compass/domain/media/config/MediaValidationProperties.java`
**라인 수**: 61줄

**설정 항목**:
- `supportedExtensions`: 지원 확장자 (.jpg, .jpeg, .png, .webp, .gif)
- `supportedMimeTypes`: 지원 MIME 타입
- `maxFileSize`: 최대 파일 크기 (10MB)
- `maliciousSignatures`: 악성 파일 시그니처
- `forbiddenChars`: 금지된 파일명 문자
- `imageMimeTypes`: 이미지 MIME 타입

#### 📄 `S3Configuration.java` - S3 설정

### 7. Exception Layer (예외 처리 계층)

#### 📄 `MediaExceptionHandler.java` - 미디어 예외 핸들러
**위치**: `src/main/java/com/compass/domain/media/exception/MediaExceptionHandler.java`
**라인 수**: 49줄

**처리 예외**:
- `FileValidationException`: 파일 검증 실패
- `MaxUploadSizeExceededException`: 파일 크기 초과
- `S3UploadException`: S3 업로드 오류
- `OCRProcessingException`: OCR 처리 오류
- `Exception`: 일반 예외

**특징**:
- 도메인별 예외 처리 (`@Order(1)`)
- 상세한 에러 로깅
- 사용자 친화적 에러 메시지

#### 📄 `FileValidationException.java` - 파일 검증 예외
#### 📄 `S3UploadException.java` - S3 업로드 예외
#### 📄 `OCRProcessingException.java` - OCR 처리 예외

---

## 🔄 데이터 플로우

### 1. 파일 업로드 플로우
```
1. MediaController.uploadFile()
   ↓
2. FileValidationService.validateFile()
   ↓
3. S3Service.uploadFile()
   ↓
4. ThumbnailService.generateThumbnail() (이미지인 경우)
   ↓
5. S3Service.uploadThumbnail()
   ↓
6. MediaRepository.save()
```

### 2. OCR 처리 플로우
```
1. MediaController.processOCR()
   ↓
2. MediaService.processOCRForMedia()
   ↓
3. S3Service.downloadFile()
   ↓
4. OCRService.extractTextFromBytes()
   ↓
5. MediaRepository.save() (메타데이터 업데이트)
```

### 3. 파일 조회 플로우
```
1. MediaController.getMedia()
   ↓
2. MediaService.getMediaById()
   ↓
3. S3Service.generatePresignedUrl()
   ↓
4. MediaService.createMediaHeaders()
```

---

## 🔧 기술 스택

### 핵심 라이브러리
- **Spring Boot**: 웹 프레임워크
- **Spring Data JPA**: 데이터 접근
- **AWS SDK**: S3 연동
- **Google Cloud Vision**: OCR 처리
- **Thumbnailator**: 썸네일 생성
- **Hypersistence Utils**: JSON 타입 지원

### 데이터베이스
- **PostgreSQL**: 메인 데이터베이스
- **JSONB**: 메타데이터 저장

### 외부 서비스
- **AWS S3**: 파일 저장소
- **Google Cloud Vision API**: OCR 서비스

---

## 📊 성능 및 제약사항

### 현재 제약사항
- **최대 파일 크기**: 10MB
- **지원 형식**: JPG, PNG, WebP, GIF
- **썸네일 크기**: 300x300px WebP
- **Presigned URL 만료**: 15분
- **OCR 최대 파일 크기**: 50MB

### 성능 특성
- **동기 처리**: OCR 및 썸네일 생성
- **트랜잭션**: 파일 업로드 시 단일 트랜잭션
- **캐싱**: 15분 브라우저 캐시
- **로깅**: 모든 주요 작업 로깅

---

## 🚨 보안 기능

### 파일 검증
- **MIME 타입 검증**: 허용된 형식만 업로드
- **파일 헤더 검증**: 실제 파일 형식 확인
- **파일명 검증**: Path Traversal 방지
- **악성 콘텐츠 스캔**: 스크립트 및 실행 파일 탐지
- **이미지 폭탄 방지**: 과도한 압축 이미지 차단

### 접근 제어
- **JWT 인증**: 모든 API 엔드포인트
- **사용자별 권한**: 본인 파일만 접근 가능
- **Presigned URL**: 임시 접근 권한

---

## 📈 개선 사항 (analysis_improvements 참조)

### 1. 성능 최적화
- **비동기 처리**: `AsyncMediaService.java` 구현
- **연결 풀링**: `GoogleVisionClientFactory.java`
- **배치 처리**: 대량 파일 처리 최적화

### 2. 코드 품질
- **상수 통합**: `MediaConstants.java`
- **유틸리티 클래스**: `TextAnalysisUtils.java`
- **OCR 리팩토링**: `OCRServiceRefactored.java`

### 3. 보안 강화
- **고급 보안**: `EnhancedSecurityService.java`
- **스테가노그래피 탐지**
- **파일 평판 검사**

### 4. 아키텍처 개선
- **계층화 아키텍처**: `LayeredMediaArchitecture.java`
- **이벤트 기반**: `MediaDomainEvents.java`
- **복원력 향상**: `ResilientMediaService.java`

---

## 🔍 디버깅 및 모니터링

### 로깅 전략
- **INFO 레벨**: 주요 작업 시작/완료
- **WARN 레벨**: 검증 실패, 권한 오류
- **ERROR 레벨**: 시스템 오류, 외부 API 실패
- **DEBUG 레벨**: 상세 처리 과정

### 주요 로그 포인트
- 파일 업로드 시작/완료
- OCR 처리 시작/완료
- S3 업로드/다운로드
- 썸네일 생성
- 보안 검증 실패
- 외부 API 호출

### 메트릭 수집 (개선 예정)
- 업로드 성공/실패 카운터
- OCR 처리 시간
- 보안 위협 탐지 카운터
- 파일 크기 분포

---

## 📚 참고 문서

### 내부 문서
- `MEDIA_DOMAIN_IMPROVEMENT_RECOMMENDATIONS.md`: 상세 개선 사항
- `PROJECT_WORKFLOW.md`: 전체 프로젝트 워크플로우
- `DATABASE_ERD.md`: 데이터베이스 설계

### 외부 문서
- [Google Cloud Vision API](https://cloud.google.com/vision/docs)
- [AWS S3 SDK](https://docs.aws.amazon.com/sdk-for-java/)
- [Thumbnailator](https://github.com/coobird/thumbnailator)

---

## 🎯 미디어 도메인 담당자 체크리스트

### 일상 업무
- [ ] S3 버킷 용량 모니터링
- [ ] Google Cloud Vision API 사용량 확인
- [ ] 에러 로그 정기 검토
- [ ] 파일 업로드 성공률 모니터링

### 개발 작업
- [ ] 새로운 이미지 형식 추가 시 검증 로직 업데이트
- [ ] OCR 정확도 개선 작업
- [ ] 썸네일 품질 최적화
- [ ] 보안 검증 강화

### 장애 대응
- [ ] S3 연결 실패 시 대응 방안
- [ ] Google Vision API 장애 시 대응
- [ ] 대용량 파일 업로드 실패 처리
- [ ] OCR 처리 시간 초과 대응

---

**문서 버전**: 1.0  
**최종 업데이트**: 2024년 1월  
**담당자**: 미디어 도메인 전담 개발자  
**검토 주기**: 월 1회