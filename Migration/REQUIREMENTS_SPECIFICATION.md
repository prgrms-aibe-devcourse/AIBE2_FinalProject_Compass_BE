# 📋 LLM 오케스트레이터 요구사항 명세서

## 🎯 프로젝트 개요

### 목적
분산된 서비스 구조를 중앙 집중형 LLM 오케스트레이터 패턴으로 전환하여 CHAT2 여행 계획 워크플로우 구현

### 범위
- 39개 Java 파일 구현
- 5명의 개발자 역할 분담
- 5단계 Phase 기반 워크플로우
- 9개 Intent 처리

---

## 🏗️ Epic / Story / Task 구조

## Epic 1: 오케스트레이터 핵심 구조 구현
**담당**: Chat2 개발자
**우선순위**: P0 (최우선)
**예상 공수**: 3일

### Story 1.1: 메인 오케스트레이터 구현
**설명**: 전체 워크플로우를 제어하는 중앙 오케스트레이터 구현

#### Task 1.1.1: MainLLMOrchestrator.java 구현
- **요구사항**:
  - Spring AI ChatModel 통합
  - Intent 분류 호출
  - Phase 관리 호출
  - Function 선택 및 실행
  - 컨텍스트 관리
- **입력**: ChatRequest
- **출력**: ChatResponse
- **의존성**: IntentClassifier, PhaseManager, FunctionConfiguration
- **워크플로우 역할**: 전체 시스템의 중앙 지휘자로서 모든 사용자 요청을 받아 Intent 분류 → Phase 확인 → Function 실행 → 응답 생성을 수행. UnifiedChatController에서 받은 요청을 처리하는 메인 엔진 역할

#### Task 1.1.2: IntentClassifier.java 구현
- **요구사항**:
  - 9개 Intent 분류 (TRAVEL_PLANNING, INFORMATION_COLLECTION 등)
  - LLM 기반 의도 파악
  - Intent별 Function 매핑
- **입력**: String (사용자 메시지)
- **출력**: Intent enum
- **검증**: 각 Intent별 테스트 케이스
- **워크플로우 역할**: 사용자 의도 파악기로서 메시지를 9개 Intent 중 하나로 분류하고, 각 Intent에 맞는 Function 리스트를 선택. 예: "항공권 예약서 있어" → IMAGE_UPLOAD Intent → OCR Function들 선택

#### Task 1.1.3: PhaseManager.java 구현
- **요구사항**:
  - 5개 Phase 관리 (INITIALIZATION → COMPLETION)
  - Phase 전환 로직
  - Thread별 Phase 상태 저장
- **입력**: threadId, Intent
- **출력**: TravelPhase
- **상태 관리**: Redis 또는 DB
- **워크플로우 역할**: 대화 단계 관리자로서 현재 대화가 5단계 중 어느 Phase에 있는지 추적하고 Phase 전환 조건 확인 및 전환 실행. 예: 정보 수집 완료 → INFORMATION_COLLECTION에서 PLAN_GENERATION으로 전환

### Story 1.2: 컨트롤러 및 설정 구현

#### Task 1.2.1: UnifiedChatController.java 구현
- **요구사항**:
  - `/api/chat/unified` 단일 엔드포인트
  - JWT 인증 통합
  - Thread-Id 헤더 처리
- **입력**: ChatRequest, UserDetails, threadId
- **출력**: ResponseEntity<ChatResponse>
- **보안**: @AuthenticationPrincipal 활용
- **워크플로우 역할**: API 진입점으로서 `/api/chat/unified` 엔드포인트를 제공하고, 모든 채팅 요청을 받아 MainLLMOrchestrator로 전달하며 HTTP 요청/응답 처리

#### Task 1.2.2: FunctionConfiguration.java 구현
- **요구사항**:
  - 모든 Function Bean 등록
  - Spring AI Function 설정
  - 의존성 주입 설정
- **Bean 등록**: 18개 Function
- **설정**: @Configuration, @Bean
- **워크플로우 역할**: Function Bean 설정자로서 모든 Function을 Spring Bean으로 등록하여 DI를 통한 Function 주입을 가능하게 함

### Story 1.3: 데이터 모델 정의

#### Task 1.3.1: Intent.java enum 구현
- **요구사항**: 9개 Intent 값 정의
- **값**: TRAVEL_PLANNING, INFORMATION_COLLECTION, IMAGE_UPLOAD 등
- **워크플로우 역할**: 의도 분류 체계로서 9개 Intent enum을 정의하여 IntentClassifier에서 사용하는 분류 기준 제공

#### Task 1.3.2: TravelPhase.java enum 구현
- **요구사항**: 5개 Phase 값 정의
- **값**: INITIALIZATION, INFORMATION_COLLECTION, PLAN_GENERATION 등
- **워크플로우 역할**: 대화 단계 정의로서 5개 Phase enum을 정의하여 PhaseManager에서 사용하는 단계 체계 제공

#### Task 1.3.3: TravelContext.java 구현
- **요구사항**:
  - 대화 컨텍스트 관리
  - 수집된 정보 저장
  - 여행 계획 저장
- **필드**: threadId, userId, currentPhase, collectedInfo, travelPlan
- **워크플로우 역할**: 대화 문맥 저장소로서 Thread별 대화 상태 및 수집된 정보를 저장하고 여행 정보, Phase, Intent 등 컨텍스트 관리

---

## Epic 2: 정보 수집 시스템 구현
**담당**: User 개발자
**우선순위**: P0 (최우선)
**예상 공수**: 2일

### Story 2.1: 빠른 입력 폼 시스템

#### Task 2.1.1: ShowQuickInputFormFunction.java 구현
- **요구사항**:
  - 빠른 입력 폼 구조 생성
  - 7개 필드 정의 (목적지, 출발지, 날짜, 동행자, 예산, 스타일, 예약서)
  - 목적지 미정 옵션 포함
- **입력**: ChatRequest
- **출력**: QuickInputFormDto
- **UI 타입**: tag-input, date-picker, select 등
- **워크플로우 역할**: 빠른 입력 폼 생성기로서 사용자가 "여행 계획 짜줘"라고 하면 입력 폼 UI를 생성하여 목적지, 날짜, 예산 등 필드를 정의하고 프론트엔드에서 렌더링할 폼 구조 반환

#### Task 2.1.2: QuickInputFormDto.java 구현
- **요구사항**:
  - 폼 구조 정의
  - 필드 타입 정의
  - 검증 규칙 포함
- **필드**: formType, formFields, validationRules
- **워크플로우 역할**: 폼 데이터 구조로서 빠른 입력 폼의 필드 구조를 정의하고 프론트엔드와 백엔드 간 데이터 교환 형식 제공

#### Task 2.1.3: SubmitTravelFormFunction.java 구현
- **요구사항**:
  - 폼 데이터 처리
  - 필수 정보 검증
  - Phase 전환 트리거
- **입력**: TravelFormSubmitRequest
- **출력**: ChatResponse
- **검증**: 필수 필드 확인
- **워크플로우 역할**: 폼 제출 처리기로서 빠른 입력 폼에서 제출된 데이터를 처리하고 입력 완성도 확인 후 미완성시 Follow-up 시작, 완성시 Phase를 PLAN_GENERATION으로 전환

### Story 2.2: Follow-up 질문 시스템

#### Task 2.2.1: AnalyzeUserInputFunction.java 구현
- **요구사항**:
  - 사용자 입력 분석
  - 누락 정보 파악
  - 정보 완성도 평가
- **입력**: UserInput
- **출력**: TravelInfo
- **분석**: NLP 기반 정보 추출
- **워크플로우 역할**: 입력 분석기로서 자연어 입력에서 여행 정보를 추출. 예: "12월 25일 제주도 3박" → 날짜, 목적지, 기간 파싱

#### Task 2.2.2: StartFollowUpFunction.java 구현
- **요구사항**:
  - Follow-up 질문 생성
  - 우선순위 기반 질문 순서
- **입력**: TravelInfo (불완전)
- **출력**: FollowUpQuestion
- **워크플로우 역할**: Follow-up 시작기로서 필수 정보 부족시 Follow-up 질문을 시작하고 첫 번째 누락 정보에 대한 질문 생성

#### Task 2.2.3: ContinueFollowUpFunction.java 구현
- **요구사항**:
  - Follow-up 응답 처리
  - 정보 업데이트
  - 완성도 재평가
- **입력**: FollowUpResponse
- **출력**: UpdatedTravelInfo
- **워크플로우 역할**: Follow-up 진행기로서 사용자 응답 처리 후 다음 질문을 생성하고 모든 정보 수집 완료시 Phase 전환

### Story 2.3: 목적지 추천 시스템

#### Task 2.3.1: RecommendDestinationsFunction.java 구현
- **요구사항**:
  - 목적지 미정 시 추천
  - 거리 기반 옵션 제시
  - 스타일 매칭 점수 계산
- **입력**: 출발지, 여행스타일, 예산
- **출력**: List<DestinationRecommendation>
- **API**: Perplexity 연동
- **워크플로우 역할**: 목적지 추천기로서 "목적지 미정" 선택시 추천 목적지를 제시하고 Perplexity API로 여행 스타일별 도시 검색 및 거리별 옵션 제공

#### Task 2.3.2: TravelFormSubmitRequest.java 구현
- **요구사항**:
  - 폼 제출 데이터 구조
  - 검증 어노테이션
- **필드**: destinations[], departureLocation, travelDates 등
- **워크플로우 역할**: 폼 제출 요청 객체로서 사용자가 제출한 폼 데이터를 담는 DTO로 검증 및 처리를 위한 구조화된 데이터 제공

---

## Epic 3: 여행 계획 생성 시스템
**담당**: Trip 개발자
**우선순위**: P0 (최우선)
**예상 공수**: 3일

### Story 3.1: 계획 생성 엔진

#### Task 3.1.1: GenerateTravelPlanFunction.java 구현
- **요구사항**:
  - 병렬 데이터 수집
  - LLM 기반 일정 생성
  - 복수 목적지 처리
- **입력**: TravelPlanRequest
- **출력**: TravelPlanResponse
- **데이터 소스**: DB, Perplexity, 관광공사 API
- **워크플로우 역할**: 여행 계획 생성 엔진으로서 DB, Perplexity, Tour API에서 병렬로 데이터를 수집하고 LLM으로 수집된 데이터 기반 일정을 생성하며 날씨, 거리, 선호도를 고려한 최적화 수행

#### Task 3.1.2: TravelPlanRequest.java 구현
- **요구사항**:
  - 계획 생성 요청 데이터
- **필드**: destinations, dates, budget, style, companions
- **워크플로우 역할**: 계획 생성 요청 객체로서 여행 계획 생성에 필요한 모든 정보(목적지, 날짜, 예산, 스타일 등)를 포함하는 데이터 구조

#### Task 3.1.3: TravelPlanResponse.java 구현
- **요구사항**:
  - 생성된 계획 응답 데이터
- **필드**: planId, itinerary, totalCost, recommendations
- **워크플로우 역할**: 계획 응답 객체로서 생성된 여행 계획 데이터를 구조화하여 일정, 장소, 경로 정보를 포함한 응답 제공

### Story 3.2: 외부 API 통합

#### Task 3.2.1: SearchWithPerplexityFunction.java 구현
- **요구사항**:
  - Perplexity API 검색
  - 실시간 트렌드 반영
- **입력**: SearchQuery
- **출력**: List<SearchResult>
- **워크플로우 역할**: 실시간 검색 엔진으로서 Perplexity API로 최신 여행 정보를 검색하고 트렌디한 장소, 맛집, 관광지 정보 수집

#### Task 3.2.2: SearchTourAPIFunction.java 구현
- **요구사항**:
  - 한국관광공사 API 연동
  - 관광지 정보 조회
- **입력**: Location
- **출력**: List<TourPlace>
- **워크플로우 역할**: 관광 API 검색기로서 한국관광공사 Tour API를 호출하여 공식 관광지 정보를 수집하고 검증된 정보 제공

#### Task 3.2.3: PerplexityClient.java 구현
- **요구사항**:
  - Perplexity API 클라이언트
  - 재시도 로직
  - 에러 처리
- **설정**: API Key, Timeout
- **워크플로우 역할**: Perplexity API 클라이언트로서 API 통신을 처리하고 검색 쿼리 전송 및 결과 파싱을 담당하며 안정적인 통신을 위한 재시도 로직 구현

### Story 3.3: 검색 및 수정 기능

#### Task 3.3.1: SearchDestinationsFunction.java 구현
- **요구사항**:
  - 목적지 검색
  - 복수 도시 경로 최적화
- **입력**: List<String> destinations
- **출력**: OptimizedRoute
- **워크플로우 역할**: 복수 목적지 처리기로서 여러 도시 입력시 최적 경로를 계산하고 도시간 이동 시간/거리를 고려한 순서 최적화

#### Task 3.3.2: ModifyTravelPlanFunction.java 구현
- **요구사항**:
  - 생성된 계획 수정
  - 피드백 반영
- **입력**: ModifyRequest
- **출력**: UpdatedTravelPlan
- **워크플로우 역할**: 계획 수정기로서 생성된 계획에 대한 수정 요청을 처리하고 "둘째날 일정 변경" 같은 피드백을 반영하여 계획 업데이트

### Story 3.4: 요청/응답 모델

#### Task 3.4.1: ChatRequest.java 구현
- **요구사항**: 통합 요청 모델
- **필드**: message, threadId, userId, metadata
- **워크플로우 역할**: 요청 진입 객체로서 모든 채팅 요청의 기본 구조를 제공하고 메시지, threadId, userId를 포함한 통일된 요청 형식

#### Task 3.4.2: ChatResponse.java 구현
- **요구사항**: 통합 응답 모델
- **필드**: content, type, data, nextAction
- **워크플로우 역할**: 응답 종료 객체로서 모든 응답의 통일된 구조를 제공하고 메시지, 데이터, 상태 정보를 포함한 응답 형식

---

## Epic 4: 이미지 처리 시스템
**담당**: Media 개발자
**우선순위**: P1 (높음)
**예상 공수**: 2일

### Story 4.1: 이미지 업로드 및 OCR

#### Task 4.1.1: ProcessImageFunction.java 구현
- **요구사항**:
  - 이미지 업로드 처리
  - S3 저장
  - OCR 트리거
- **입력**: ImageUploadRequest
- **출력**: ImageProcessResult
- **워크플로우 역할**: 이미지 처리 총괄로서 업로드된 이미지를 S3에 저장하고 OCR 처리를 요청하며 항공권/호텔 예약서를 분류하여 처리

#### Task 4.1.2: ProcessOCRFunction.java 구현
- **요구사항**:
  - Google Vision OCR 실행
  - 텍스트 추출
  - 문서 타입 판별
- **입력**: ImageUrl
- **출력**: OCRResult
- **워크플로우 역할**: OCR 실행기로서 Google Vision API를 통해 텍스트를 추출하고 추출된 텍스트를 구조화하여 문서 타입 판별

### Story 4.2: 예약 정보 추출

#### Task 4.2.1: ExtractFlightInfoFunction.java 구현
- **요구사항**:
  - 항공권 정보 추출
  - 구조화된 데이터 변환
- **입력**: OCRText
- **출력**: FlightReservation
- **워크플로우 역할**: 항공권 정보 추출기로서 OCR 결과에서 항공편, 날짜, 출발/도착지를 추출하고 FlightReservation 객체로 변환

#### Task 4.2.2: ExtractHotelInfoFunction.java 구현
- **요구사항**:
  - 호텔 예약 정보 추출
  - 체크인/체크아웃 파싱
- **입력**: OCRText
- **출력**: HotelReservation
- **워크플로우 역할**: 호텔 정보 추출기로서 OCR 결과에서 호텔명, 체크인/아웃, 주소를 추출하고 HotelReservation 객체로 변환

### Story 4.3: 외부 서비스 클라이언트

#### Task 4.3.1: S3Client.java 구현
- **요구사항**:
  - AWS S3 업로드
  - 파일명 생성
  - URL 반환
- **메서드**: upload(), delete(), getUrl()
- **워크플로우 역할**: AWS S3 클라이언트로서 이미지 파일을 S3 버킷에 업로드하고 공개 URL을 생성 및 반환하여 이미지 저장소 역할

#### Task 4.3.2: OCRClient.java 구현
- **요구사항**:
  - Google Vision API 호출
  - 에러 처리
- **메서드**: extractText(), detectDocument()
- **워크플로우 역할**: Google Vision 클라이언트로서 Google Vision API를 호출하여 이미지에서 텍스트를 추출하는 OCR 기능 제공

### Story 4.4: 데이터 모델

#### Task 4.4.1: FlightReservation.java 구현
- **필드**: flightNumber, departure, arrival, date, passenger
- **워크플로우 역할**: 항공권 데이터 모델로서 항공권 정보 저장 구조를 제공하고 항공사, 편명, 시간 등 필드 정의

#### Task 4.4.2: HotelReservation.java 구현
- **필드**: hotelName, checkIn, checkOut, roomType, guests
- **워크플로우 역할**: 호텔 데이터 모델로서 호텔 예약 정보 저장 구조를 제공하고 호텔명, 날짜, 위치 등 필드 정의

---

## Epic 5: 일반 대화 및 인증 시스템
**담당**: Chat 개발자
**우선순위**: P1 (높음)
**예상 공수**: 2일

### Story 5.1: 일반 대화 처리

#### Task 5.1.1: HandleGeneralQuestionFunction.java 구현
- **요구사항**:
  - 여행 외 일반 질문 처리
  - 적절한 응답 생성
- **입력**: GeneralQuestion
- **출력**: GeneralResponse
- **워크플로우 역할**: 일반 질문 처리기로서 여행과 무관한 일반 대화를 처리하고 날씨, 환율 등 정보성 질문에 응답

#### Task 5.1.2: ProvideGeneralInfoFunction.java 구현
- **요구사항**:
  - 일반 정보 제공
  - 날씨, 환율 등
- **입력**: InfoRequest
- **출력**: InfoResponse
- **워크플로우 역할**: 일반 정보 제공기로서 날씨, 환율, 시간대 등 정보를 제공하고 외부 API를 호출하여 실시간 정보 제공

#### Task 5.1.3: HandleUnknownFunction.java 구현
- **요구사항**:
  - 분류 불가 요청 처리
  - 기본 응답 생성
- **입력**: UnknownRequest
- **출력**: DefaultResponse
- **워크플로우 역할**: 미분류 처리기로서 Intent 분류 불가능한 요청을 처리하고 기본 응답 또는 도움말 제공

### Story 5.2: 대화 관리

#### Task 5.2.1: ChatThreadService.java 구현
- **요구사항**:
  - 대화 스레드 관리
  - 메시지 저장/조회
  - 상태 관리
- **메서드**: createThread(), saveMessage(), getHistory()
- **워크플로우 역할**: 대화 저장 관리자로서 ChatThread 생성/조회/업데이트를 담당하고 대화 히스토리 관리 및 최종 계획 저장

### Story 5.3: 인증 컨트롤러

#### Task 5.3.1: AuthController.java 구현
- **요구사항**:
  - 로그인/회원가입 엔드포인트
  - JWT 토큰 발급
- **엔드포인트**: /api/auth/login, /api/auth/signup
- **워크플로우 역할**: 인증 진입점으로서 `/api/auth/login`, `/api/auth/register` 엔드포인트를 제공하고 JWT 토큰 발급 및 검증을 통한 사용자 인증 상태 관리

---

## 📊 담당자별 Epic-Story-Task 계층 구조

### 전체 요약
- **5개 Epic** → **15개 Story** → **39개 Task** (39개 파일)
- 각 개발자는 하나의 Epic을 담당하여 완성

---

### 🧠 Chat2 개발자
**담당 Epic**: Epic 1 - 오케스트레이터 핵심 구조
**우선순위**: P0 (최우선) | **예상 공수**: 3일 | **파일 수**: 8개

```
Epic 1: 오케스트레이터 핵심 구조 구현
├── Story 1.1: 메인 오케스트레이터 구현 (3 Tasks)
│   ├── Task 1.1.1: MainLLMOrchestrator.java (1일)
│   ├── Task 1.1.2: IntentClassifier.java (0.5일)
│   └── Task 1.1.3: PhaseManager.java (0.5일)
├── Story 1.2: 컨트롤러 및 설정 구현 (2 Tasks)
│   ├── Task 1.2.1: UnifiedChatController.java (0.5일)
│   └── Task 1.2.2: FunctionConfiguration.java (0.5일)
└── Story 1.3: 데이터 모델 정의 (3 Tasks)
    ├── Task 1.3.1: Intent.java (2시간)
    ├── Task 1.3.2: TravelPhase.java (2시간)
    └── Task 1.3.3: TravelContext.java (4시간)
```

### 👤 User 개발자
**담당 Epic**: Epic 2 - 정보 수집 시스템
**우선순위**: P0 (Chat2 완성 후) | **예상 공수**: 2일 | **파일 수**: 8개

```
Epic 2: 정보 수집 시스템 구현
├── Story 2.1: 빠른 입력 폼 시스템 (3 Tasks)
│   ├── Task 2.1.1: ShowQuickInputFormFunction.java (0.5일)
│   ├── Task 2.1.2: QuickInputFormDto.java (4시간)
│   └── Task 2.1.3: SubmitTravelFormFunction.java (0.5일)
├── Story 2.2: Follow-up 질문 시스템 (3 Tasks)
│   ├── Task 2.2.1: AnalyzeUserInputFunction.java (0.5일)
│   ├── Task 2.2.2: StartFollowUpFunction.java (4시간)
│   └── Task 2.2.3: ContinueFollowUpFunction.java (4시간)
└── Story 2.3: 목적지 추천 시스템 (2 Tasks)
    ├── Task 2.3.1: RecommendDestinationsFunction.java (0.5일)
    └── Task 2.3.2: TravelFormSubmitRequest.java (2시간)
```

### ✈️ Trip 개발자
**담당 Epic**: Epic 3 - 여행 계획 생성 시스템
**우선순위**: P0 (Chat2 완성 후) | **예상 공수**: 3일 | **파일 수**: 10개

```
Epic 3: 여행 계획 생성 시스템 구현
├── Story 3.1: 계획 생성 엔진 (3 Tasks)
│   ├── Task 3.1.1: GenerateTravelPlanFunction.java (1일)
│   ├── Task 3.1.2: TravelPlanRequest.java (2시간)
│   └── Task 3.1.3: TravelPlanResponse.java (2시간)
├── Story 3.2: 외부 API 통합 (3 Tasks)
│   ├── Task 3.2.1: SearchWithPerplexityFunction.java (0.5일)
│   ├── Task 3.2.2: SearchTourAPIFunction.java (0.5일)
│   └── Task 3.2.3: PerplexityClient.java (0.5일)
├── Story 3.3: 검색 및 수정 기능 (2 Tasks)
│   ├── Task 3.3.1: SearchDestinationsFunction.java (0.5일)
│   └── Task 3.3.2: ModifyTravelPlanFunction.java (0.5일)
└── Story 3.4: 요청/응답 모델 (2 Tasks)
    ├── Task 3.4.1: ChatRequest.java (2시간)
    └── Task 3.4.2: ChatResponse.java (2시간)
```

### 📷 Media 개발자
**담당 Epic**: Epic 4 - 이미지 처리 시스템
**우선순위**: P1 (핵심 기능 완성 후) | **예상 공수**: 2일 | **파일 수**: 8개

```
Epic 4: 이미지 처리 시스템 구현
├── Story 4.1: 이미지 업로드 및 OCR (2 Tasks)
│   ├── Task 4.1.1: ProcessImageFunction.java (0.5일)
│   └── Task 4.1.2: ProcessOCRFunction.java (0.5일)
├── Story 4.2: 예약 정보 추출 (2 Tasks)
│   ├── Task 4.2.1: ExtractFlightInfoFunction.java (0.5일)
│   └── Task 4.2.2: ExtractHotelInfoFunction.java (0.5일)
├── Story 4.3: 외부 서비스 클라이언트 (2 Tasks)
│   ├── Task 4.3.1: S3Client.java (4시간)
│   └── Task 4.3.2: OCRClient.java (4시간)
└── Story 4.4: 데이터 모델 (2 Tasks)
    ├── Task 4.4.1: FlightReservation.java (2시간)
    └── Task 4.4.2: HotelReservation.java (2시간)
```

### 💬 Chat 개발자
**담당 Epic**: Epic 5 - 일반 대화 및 인증 시스템
**우선순위**: P1 (핵심 기능 완성 후) | **예상 공수**: 2일 | **파일 수**: 5개

```
Epic 5: 일반 대화 및 인증 시스템 구현
├── Story 5.1: 일반 대화 처리 (3 Tasks)
│   ├── Task 5.1.1: HandleGeneralQuestionFunction.java (4시간)
│   ├── Task 5.1.2: ProvideGeneralInfoFunction.java (4시간)
│   └── Task 5.1.3: HandleUnknownFunction.java (2시간)
├── Story 5.2: 대화 관리 (1 Task)
│   └── Task 5.2.1: ChatThreadService.java (0.5일)
└── Story 5.3: 인증 컨트롤러 (1 Task)
    └── Task 5.3.1: AuthController.java (4시간)
```

---

## 📊 담당자별 작업 분배 (상세 테이블)

**총 작업 수**: 51 Tasks (39개 파일 구현)
- Chat2: 8 Tasks (8개 파일)
- User: 11 Tasks (8개 파일)
- Trip: 13 Tasks (10개 파일)
- Media: 9 Tasks (8개 파일)
- Chat: 11 Tasks (5개 파일)

### 🧠 Chat2 개발자 (8개 파일, 8 Tasks)
**우선순위**: P0 - 가장 먼저 완성되어야 함

| Task ID | 파일명 | 예상 공수 | 의존성 |
|---------|--------|----------|--------|
| 1.1.1 | MainLLMOrchestrator.java | 1일 | - |
| 1.1.2 | IntentClassifier.java | 0.5일 | 1.3.1 |
| 1.1.3 | PhaseManager.java | 0.5일 | 1.3.2 |
| 1.2.1 | UnifiedChatController.java | 0.5일 | 1.1.1 |
| 1.2.2 | FunctionConfiguration.java | 0.5일 | - |
| 1.3.1 | Intent.java | 2시간 | - |
| 1.3.2 | TravelPhase.java | 2시간 | - |
| 1.3.3 | TravelContext.java | 4시간 | - |

### 👤 User 개발자 (8개 파일, 8 Tasks)
**우선순위**: P0 - Chat2 완성 후 즉시 시작

| Task ID | 파일명 | 예상 공수 | 의존성 |
|---------|--------|----------|--------|
| 2.1.1 | ShowQuickInputFormFunction.java | 0.5일 | 1.2.2 |
| 2.1.2 | QuickInputFormDto.java | 4시간 | - |
| 2.1.3 | SubmitTravelFormFunction.java | 0.5일 | 2.1.2 |
| 2.2.1 | AnalyzeUserInputFunction.java | 0.5일 | - |
| 2.2.2 | StartFollowUpFunction.java | 4시간 | 2.2.1 |
| 2.2.3 | ContinueFollowUpFunction.java | 4시간 | 2.2.2 |
| 2.3.1 | RecommendDestinationsFunction.java | 0.5일 | 3.2.3 |
| 2.3.2 | TravelFormSubmitRequest.java | 2시간 | - |

### ✈️ Trip 개발자 (10개 파일, 10 Tasks)
**우선순위**: P0 - Chat2 완성 후 즉시 시작

| Task ID | 파일명 | 예상 공수 | 의존성 |
|---------|--------|----------|--------|
| 3.1.1 | GenerateTravelPlanFunction.java | 1일 | 1.2.2 |
| 3.1.2 | TravelPlanRequest.java | 2시간 | - |
| 3.1.3 | TravelPlanResponse.java | 2시간 | - |
| 3.2.1 | SearchWithPerplexityFunction.java | 0.5일 | 3.2.3 |
| 3.2.2 | SearchTourAPIFunction.java | 0.5일 | - |
| 3.2.3 | PerplexityClient.java | 0.5일 | - |
| 3.3.1 | SearchDestinationsFunction.java | 0.5일 | 3.2.1 |
| 3.3.2 | ModifyTravelPlanFunction.java | 0.5일 | 3.1.1 |
| 3.4.1 | ChatRequest.java | 2시간 | - |
| 3.4.2 | ChatResponse.java | 2시간 | - |

### 📷 Media 개발자 (8개 파일, 8 Tasks)
**우선순위**: P1 - 핵심 기능 완성 후 시작

| Task ID | 파일명 | 예상 공수 | 의존성 |
|---------|--------|----------|--------|
| 4.1.1 | ProcessImageFunction.java | 0.5일 | 4.3.1 |
| 4.1.2 | ProcessOCRFunction.java | 0.5일 | 4.3.2 |
| 4.2.1 | ExtractFlightInfoFunction.java | 0.5일 | 4.1.2 |
| 4.2.2 | ExtractHotelInfoFunction.java | 0.5일 | 4.1.2 |
| 4.3.1 | S3Client.java | 4시간 | - |
| 4.3.2 | OCRClient.java | 4시간 | - |
| 4.4.1 | FlightReservation.java | 2시간 | - |
| 4.4.2 | HotelReservation.java | 2시간 | - |

### 💬 Chat 개발자 (5개 파일, 11 Tasks)
**우선순위**: P1 - 핵심 기능 완성 후 시작

| Task ID | 파일명 | 예상 공수 | 의존성 |
|---------|--------|----------|--------|
| 5.1.1 | HandleGeneralQuestionFunction.java | 4시간 | 1.2.2 |
| 5.1.2 | ProcessGeneralQuestionFunction.java | 4시간 | 5.1.1 |
| 5.1.3 | ProvideGeneralInfoFunction.java | 4시간 | - |
| 5.1.4 | HandleUnknownFunction.java | 2시간 | - |
| 5.1.5 | CheckAPIUsageFunction.java | 4시간 | - |
| 5.1.6 | GetContextFunction.java | 4시간 | - |
| 5.2.1 | ChatService.java | 0.5일 | - |
| 5.2.2 | ThreadService.java | 0.5일 | - |
| 5.2.3 | ContextManager.java | 0.5일 | - |
| 5.2.4 | OCRService.java | 0.5일 | 4.3.2 |
| 5.3.1 | AuthController.java | 4시간 | - |

---

## 🚀 구현 순서 (Critical Path)

### Phase 1: 기반 구조 (Day 1)
1. **Chat2**: Intent.java, TravelPhase.java, TravelContext.java
2. **Chat2**: MainLLMOrchestrator.java
3. **Chat2**: IntentClassifier.java, PhaseManager.java
4. **Chat2**: FunctionConfiguration.java

### Phase 2: 핵심 Function (Day 2-3)
5. **User**: ShowQuickInputFormFunction.java, QuickInputFormDto.java
6. **User**: SubmitTravelFormFunction.java
7. **Trip**: GenerateTravelPlanFunction.java
8. **Trip**: 데이터 모델 (Request/Response)

### Phase 3: 외부 연동 (Day 3-4)
9. **Trip**: PerplexityClient.java, 검색 Function들
10. **Media**: S3Client.java, OCRClient.java
11. **Media**: 이미지 처리 Function들

### Phase 4: 보조 기능 (Day 4-5)
12. **User**: Follow-up Function들
13. **Chat**: 일반 대화 Function들
14. **Chat**: ChatThreadService.java
15. **모든 팀**: 통합 테스트

---

## ✅ 완료 기준 (Definition of Done)

### 각 Task 완료 기준
- [ ] 코드 구현 완료
- [ ] 단위 테스트 작성 (80% 커버리지)
- [ ] JavaDoc 주석 작성
- [ ] 코드 리뷰 통과
- [ ] 통합 테스트 통과

### Epic 완료 기준
- [ ] 모든 Story 완료
- [ ] E2E 테스트 통과
- [ ] 성능 테스트 통과 (응답시간 < 2초)
- [ ] 문서화 완료

### 프로젝트 완료 기준
- [ ] 39개 파일 모두 구현
- [ ] 워크플로우 전체 테스트 통과
- [ ] 운영 환경 배포 준비 완료
- [ ] 모니터링 설정 완료

---

## 📈 리스크 관리

### 기술적 리스크
| 리스크 | 확률 | 영향도 | 대응 방안 |
|--------|------|--------|----------|
| LLM API 응답 지연 | 중 | 높음 | 타임아웃 설정, 캐싱 전략 |
| 외부 API 장애 | 낮음 | 높음 | Fallback 메커니즘, 재시도 로직 |
| 토큰 사용량 초과 | 중 | 중 | 토큰 모니터링, 압축 전략 |

### 일정 리스크
| 리스크 | 확률 | 영향도 | 대응 방안 |
|--------|------|--------|----------|
| Chat2 개발 지연 | 낮음 | 매우 높음 | 다른 개발자 지원 투입 |
| 통합 이슈 | 중 | 높음 | 일일 통합 테스트 |
| 요구사항 변경 | 중 | 중 | 변경 관리 프로세스 |

---

## 📅 마일스톤

### M1: 기반 구조 완성 (Day 1)
- Chat2 개발자 핵심 파일 완성
- 기본 워크플로우 동작 확인

### M2: 핵심 기능 구현 (Day 3)
- 정보 수집 시스템 완성
- 여행 계획 생성 완성

### M3: 전체 통합 (Day 5)
- 모든 Function 통합
- E2E 테스트 통과

### M4: 운영 준비 (Day 6)
- 성능 최적화
- 배포 준비 완료

---

작성일: 2024-12-30
버전: 1.0.0