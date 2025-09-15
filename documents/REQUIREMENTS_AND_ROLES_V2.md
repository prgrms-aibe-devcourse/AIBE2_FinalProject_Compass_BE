# 📋 요구사항 명세서 및 팀원별 역할 분배 V2

## 목차

1. [MVP 요구사항 명세서](#1-mvp-요구사항-명세서)
2. [개발 세션별 구분](#2-개발-세션별-구분)
3. [도메인별 역할 분배](#3-도메인별-역할-분배)
4. [개발 일정](#4-개발-일정)
5. [성공 지표](#5-성공-지표)

---

## 1. MVP 요구사항 명세서

### 1.1 핵심 요구사항 (3일 MVP 기준)

#### 🎯 필수 구현 항목 (총 31개)

##### CHAT1 도메인 요구사항 (5개)
- **REQ-CHAT1-001**: Thread 관리 서비스 (생성/조회/업데이트)
- **REQ-CHAT1-002**: 메시지 저장 및 컨텍스트 관리
- **REQ-CHAT1-003**: 대화 컨텍스트 조회 및 유지
- **REQ-CHAT1-004**: 일반 질문 처리 (날씨, 환율, 인사말 등)
- **REQ-CHAT1-005**: 여행으로 대화 자연스럽게 유도

##### CHAT2 도메인 요구사항 (10개) - 팀 리더
- **REQ-CHAT2-001**: Spring AI 프레임워크 설정 및 통합
- **REQ-CHAT2-002**: MainLLMOrchestrator 구현 (Gemini 2.0 Flash)
- **REQ-CHAT2-003**: Function Calling 기본 구조 구현
- **REQ-CHAT2-004**: Intent 분류 시스템 (여행/일반 구분)
- **REQ-CHAT2-005**: 부족 정보 확인 Function (`checkMissingInfo`)
- **REQ-CHAT2-006**: 부족 정보 채우기 Function (`fillMissingInfo`)
- **REQ-CHAT2-007**: 시스템 프롬프트 최적화 (50토큰 목표)
- **REQ-CHAT2-008**: 전체 시스템 통합 테스트
- **REQ-CHAT2-009**: API 사용량 모니터링 및 제한
- **REQ-CHAT2-010**: 프로젝트 총괄 및 진도 관리

##### TRIP 도메인 요구사항 (7개)
- **REQ-TRIP-001**: 여행 계획 생성 Function (`generateTravelPlan`)
- **REQ-TRIP-002**: PostgreSQL 장소 데이터베이스 구축
- **REQ-TRIP-003**: Perplexity API 통합 (`searchWithPerplexity`)
- **REQ-TRIP-004**: 하이브리드 장소 선택 Function (`selectOptimalPlaces`)
- **REQ-TRIP-005**: OpenWeatherMap API 통합 (`getWeatherInfo`)
- **REQ-TRIP-006**: 출발/도착 시간 기반 일정 조정
- **REQ-TRIP-007**: 여행 계획 수정 Function (`modifyTravelPlan`)

##### MEDIA 도메인 요구사항 (5개)
- **REQ-MEDIA-001**: S3 업로드 및 OCR Function 구현 (`processOCR`)
- **REQ-MEDIA-002**: 이미지 유형 분류 (`classifyImageType`)
- **REQ-MEDIA-003**: 항공권 정보 추출 및 DB 저장
- **REQ-MEDIA-004**: 호텔 바우처/예약서 정보 추출 및 DB 저장
- **REQ-MEDIA-005**: 공연/행사 티켓 정보 추출 및 DB 저장

##### USER 도메인 요구사항 (7개)
- **REQ-USER-001**: JWT 기반 인증/인가 (완료)
- **REQ-USER-002**: API 사용량 추적 서비스
- **REQ-USER-003**: 사용자 프로필 및 선호도 관리
- **REQ-USER-004**: 여행 스타일 템플릿 제공 (20+개)
- **REQ-USER-005**: 빠른 입력 폼 처리 (`processQuickInput`)
- **REQ-USER-006**: 사용자 스타일 선택 저장 (3x3 모달 UI)
- **REQ-USER-007**: 예약 정보 통합 처리 (`integrateReservationInfo`)

### 1.2 제외 항목 (시간 부족)
- ❌ Redis 캐싱 (DB로 대체)
- ❌ 복잡한 최적화 (병렬 처리 최소화)
- ❌ 8가지 적응형 전략 (2개로 축소)
- ❌ 복잡한 UI (기본 폼만 구현)
- ❌ Tour API 크롤링 (시간 부족)
- ❌ 거리 계산 최적화 (간단한 버전만)
- ❌ 시간대별 검색 (기본 검색만)

---

## 2. 개발 세션별 구분

### 🎯 세션 1: 여행 정보 사전수집
**목표**: 사용자로부터 여행 계획에 필요한 필수 정보를 효율적으로 수집

#### 핵심 기능
- 빠른 입력 폼으로 한 번에 정보 수집 (토큰 절약)
- OCR로 추출된 예약 정보 자동 반영
- 부족한 정보만 1-2회 추가 질문
- 필수 정보: 출발지, 목적지, 날짜, 기간, 동행자, 예산, 여행 스타일
- 예약 정보: 항공권, 호텔, 행사 티켓 (있을 경우)

#### 담당 도메인
- **USER**: 빠른 입력 폼 처리, 여행 스타일 템플릿 제공, 예약 정보 통합
- **CHAT2**: 정보 파싱 및 검증
- **MEDIA**: OCR로 예약 정보 추출 및 제공

---

### 🎯 세션 2: 여행 계획 생성
**목표**: 수집된 정보 기반으로 개인화된 여행 계획 생성

#### 핵심 기능
- DB + Perplexity 하이브리드 검색
- 날씨 정보 연동
- **기존 예약 정보 고려** (항공 시간, 호텔 체크인/아웃, 행사 시간)
- 예약된 일정에 맞춰 시간대별 최적 경로 생성

#### 담당 도메인
- **TRIP**: 여행 계획 생성 주도 (예약 정보 반영)
- **MEDIA**: 예약 정보 제공 (이미 추출된 경우)
- **USER**: 예약 정보 통합 및 전달

---

### 🎯 세션 3: 여행 계획 수정
**목표**: 생성된 계획을 사용자 피드백에 따라 수정

#### 핵심 기능
- 특정 장소 변경/추가/삭제
- 일정 순서 조정
- 예산 재조정

#### 담당 도메인
- **TRIP**: 수정 로직 구현
- **CHAT2**: 수정 요청 이해 및 파싱

---

### 🎯 세션 4: 여행 계획 외 질문 대답
**목표**: 여행과 무관한 일반 질문에도 친절하게 응답

#### 핵심 기능
- 날씨, 환율, 시사 등 일반 질문 처리
- 여행으로 자연스럽게 유도
- 서비스 사용법 안내

#### 담당 도메인
- **CHAT1**: 일반 질문 처리 및 응답
- **CHAT2**: 여행으로 대화 유도

#### 일반 질문 처리 규칙
```java
// [예시] GeneralQuestionHandler.java
@Configuration
public class GeneralQuestionHandler {
    
    @Bean("handleGeneralQuestions")
    public Function<GeneralQuestion, GeneralAnswer> handleGeneralQuestions() {
        /*
         * 처리 가능한 일반 질문 유형:
         * 1. 날씨 정보: "오늘 날씨 어때?"
         * 2. 환율 정보: "달러 환율 알려줘"
         * 3. 시사/뉴스: "최근 뉴스 알려줘"
         * 4. 서비스 안내: "이 서비스 뭐야?"
         * 5. 인사말: "안녕하세요"
         * 
         * 응답 전략:
         * - 간단하게 답변 후 여행 관련 질문으로 유도
         * - 예: "오늘 서울 날씨는 맑습니다. 여행 계획이 있으신가요?"
         * - 토큰 사용 최소화 (50토큰 이내)
         */
    }
    
    @Bean("redirectToTravel")
    public Function<GeneralAnswer, TravelSuggestion> redirectToTravel() {
        // 일반 대화를 여행 계획으로 자연스럽게 전환
        // 예: "날씨가 좋은 만큼 여행 가기 좋은 날이네요!"
    }
}
```

---

## 3. 도메인별 역할 분배

### 3.1 CHAT1 도메인 (메시지 관리 & 일반 질문 처리)

#### 담당 DB 테이블
- `chat_threads` (Thread 관리)
- `chat_messages` (메시지 저장)
- `chat_contexts` (대화 컨텍스트)
- `general_responses` (일반 질문 응답 템플릿)

#### 담당 Function & 요구사항
```java
// [예시] ChatManagementService.java
@Service
public class ChatManagementService {
    
    @Bean("manageThread")  // REQ-CHAT1-001
    public Function<ThreadRequest, ThreadResponse> manageThread() {
        // Thread 생성/조회/업데이트
        // DB: chat_threads CRUD
    }
    
    @Bean("saveMessage")  // REQ-CHAT1-002
    public Function<MessageRequest, MessageResponse> saveMessage() {
        // 메시지 저장 및 컨텍스트 관리
        // DB: chat_messages, chat_contexts 저장
    }
    
    @Bean("retrieveContext")  // REQ-CHAT1-003
    public Function<ThreadId, ChatContext> retrieveContext() {
        // 대화 컨텍스트 조회 및 관리
        // DB: chat_contexts 조회
    }
    
    @Bean("handleGeneralQuestions")  // REQ-CHAT1-004
    public Function<GeneralQuestion, GeneralAnswer> handleGeneralQuestions() {
        // 여행 외 일반 질문 처리
        // 날씨, 환율, 일반 상식 등
        // DB: general_responses 템플릿 활용
    }
    
    @Bean("redirectToTravel")  // REQ-CHAT1-005
    public Function<GeneralContext, TravelSuggestion> redirectToTravel() {
        // 일반 대화를 여행으로 자연스럽게 유도
        // 상황별 전환 메시지 생성
    }
}
```

#### 개발 체크리스트
- [ ] REQ-CHAT1-001: Thread 관리 서비스
- [ ] REQ-CHAT1-002: 메시지 저장 및 컨텍스트
- [ ] REQ-CHAT1-003: 대화 컨텍스트 조회
- [ ] REQ-CHAT1-004: 일반 질문 처리 (날씨, 환율 등)
- [ ] REQ-CHAT1-005: 여행으로 대화 유도

### 3.2 CHAT2 도메인 (Spring AI & MainLLMOrchestrator - 팀 리더)

#### 담당 DB 테이블
- `llm_prompts` (프롬프트 템플릿)
- `function_calls` (Function 호출 로그)
- `travel_info_states` (정보 수집 상태)

#### 담당 Function & 요구사항
```java
// [예시] MainLLMOrchestrator.java
@Configuration
public class MainLLMOrchestrator {
    
    @Bean("setupSpringAI")  // REQ-CHAT2-001
    public Function<Config, AISetup> setupSpringAI() {
        // Spring AI 프레임워크 설정
        // Gemini 2.0 Flash 연동
    }
    
    @Bean("orchestrateFlow")  // REQ-CHAT2-002
    public Function<UserInput, OrchestratedResponse> orchestrateFlow() {
        // 전체 대화 흐름 조율
        // Function Calling 관리
    }
    
    @Bean("setupFunctionCalling")  // REQ-CHAT2-003
    public Function<FunctionDef, FunctionRegistry> setupFunctionCalling() {
        // Function Calling 기본 구조
        // 자동 Function 선택 로직
    }
    
    @Bean("classifyIntent")  // REQ-CHAT2-004 ★
    public Function<Message, IntentType> classifyIntent() {
        // MainLLMOrchestrator의 의도 분류
        // 여행계획/정보수집/수정/일반질문 구분
    }
    
    @Bean("checkMissingInfo")  // REQ-CHAT2-005
    public Function<TravelInfo, List<String>> checkMissingInfo() {
        // 부족한 정보 확인 (1-2개만)
        // DB: travel_info_states 조회
    }
    
    @Bean("fillMissingInfo")  // REQ-CHAT2-006
    public Function<UserResponse, TravelInfo> fillMissingInfo() {
        // 부족한 정보 채우기
        // DB: travel_info_states 업데이트
    }
    
    @Bean("optimizePrompt")  // REQ-CHAT2-007
    public Function<RawPrompt, OptimizedPrompt> optimizePrompt() {
        // 프롬프트 최적화 (50토큰 목표)
        // DB: llm_prompts 템플릿 활용
    }
    
    @Bean("monitorApiUsage")  // REQ-CHAT2-009
    public Function<ApiCall, UsageStats> monitorApiUsage() {
        // API 사용량 모니터링
        // Perplexity, OCR 10회/일 제한
    }
}
```

#### 개발 체크리스트 (팀 리더 총괄)
- [ ] REQ-CHAT2-001: Spring AI 프레임워크 설정
- [ ] REQ-CHAT2-002: MainLLMOrchestrator 구현
- [ ] REQ-CHAT2-003: Function Calling 기본 구조
- [ ] REQ-CHAT2-004: Intent 분류 (MainLLMOrchestrator) ★
- [ ] REQ-CHAT2-005: Follow-up 최소화 (1-2회만)
- [ ] REQ-CHAT2-006: 여행 정보 상태 관리
- [ ] REQ-CHAT2-007: 프롬프트 최적화 (50토큰)
- [ ] REQ-CHAT2-008: 전체 통합 테스트
- [ ] REQ-CHAT2-009: API 사용량 제한 구현
- [ ] REQ-CHAT2-010: 프로젝트 진도 관리

### 3.3 TRIP 도메인 (여행 계획 생성 전담)

#### 담당 DB 테이블
- `travel_plans` (여행 계획)
- `tour_places` (관광지 정보)
- `place_categories` (장소 카테고리)
- `reservations` (예약 정보 조회용)

#### 담당 Function & 요구사항
```java
// [예시] TravelPlanGenerator.java
@Configuration
public class TravelPlanGenerator {
    
    @Bean("generateTravelPlan")  // REQ-TRIP-001
    public Function<PlanRequest, PlanResponse> generateTravelPlan() {
        // 여행 계획 생성 (DB + Perplexity + 날씨 + 예약정보)
        // 항공 시간, 호텔 체크인/아웃, 행사 시간 고려
        // DB: travel_plans, tour_places, reservations 조회/저장
    }
    
    @Bean("searchWithPerplexity")  // REQ-TRIP-003
    public Function<SearchRequest, List<Place>> searchWithPerplexity() {
        // Perplexity API로 트렌디 장소 검색
        // 사용 제한 체크 필요
    }
    
    @Bean("selectOptimalPlaces")  // REQ-TRIP-004
    public Function<PlaceSelectionRequest, PlaceSelectionResponse> selectOptimalPlaces() {
        // 하이브리드 장소 선택 (DB + Perplexity 조합)
        // DB: tour_places 우선 조회
    }
    
    @Bean("getWeatherInfo")  // REQ-TRIP-005
    public Function<LocationDate, WeatherInfo> getWeatherInfo() {
        // OpenWeatherMap API 연동
        // 날씨 기반 일정 조정
    }
    
    @Bean("modifyTravelPlan")  // REQ-TRIP-010
    public Function<ModifyRequest, ModifiedPlan> modifyTravelPlan() {
        // 여행 계획 수정
        // DB: travel_plans UPDATE
    }
}
```

#### 개발 체크리스트
- [ ] REQ-TRIP-001: 여행 계획 생성 Function
- [ ] REQ-TRIP-002: PostgreSQL 장소 DB 구축
- [ ] REQ-TRIP-003: Perplexity API 통합
- [ ] REQ-TRIP-004: 하이브리드 장소 선택
- [ ] REQ-TRIP-005: 날씨 API 통합
- [ ] REQ-TRIP-006: 시간 기반 일정 조정
- [ ] REQ-TRIP-007: Tour API 크롤링

### 3.4 MEDIA 도메인 (예약 정보 추출 전문)

#### 담당 DB 테이블
- `s3_uploads` (S3 업로드 기록)
- `ocr_results` (OCR 처리 결과)
- `reservations` (예약 정보 - 항공권, 호텔, 티켓)
- `image_classifications` (이미지 분류 결과)

#### 담당 Function & 요구사항
```java
// [예시] ReservationExtractor.java
@Configuration
public class ReservationExtractor {
    
    @Bean("uploadToS3AndOCR")  // REQ-MEDIA-001
    public Function<ImageUpload, OCRResult> uploadToS3AndOCR() {
        // S3 업로드 후 OCR 처리 (Gemini Vision API)
        // 사용 제한: 10회/일
        // DB: s3_uploads, ocr_results 저장
    }
    
    @Bean("classifyImageType")  // REQ-MEDIA-002
    public Function<OCRResult, ImageType> classifyImageType() {
        // 이미지 유형 분류 (항공권/호텔/티켓/기타)
        // DB: image_classifications 저장
    }
    
    @Bean("extractFlightInfo")  // REQ-MEDIA-003
    public Function<OCRText, FlightReservation> extractFlightInfo() {
        // 항공권 정보 추출 (항공사, 편명, 시간, 좌석)
        // DB: reservations 저장 (type='FLIGHT')
    }
    
    @Bean("extractHotelInfo")  // REQ-MEDIA-004
    public Function<OCRText, HotelReservation> extractHotelInfo() {
        // 호텔 바우처/예약서 정보 추출 (호텔명, 체크인/아웃, 객실)
        // DB: reservations 저장 (type='HOTEL')
    }
    
    @Bean("extractEventInfo")  // REQ-MEDIA-005
    public Function<OCRText, EventReservation> extractEventInfo() {
        // 공연/행사 티켓 정보 추출 (행사명, 장소, 시간)
        // DB: reservations 저장 (type='EVENT')
    }
}
```

#### 개발 체크리스트
- [ ] REQ-MEDIA-001: S3 업로드 및 OCR 처리
- [ ] REQ-MEDIA-002: 이미지 유형 분류
- [ ] REQ-MEDIA-003: 항공권 정보 추출 및 DB화
- [ ] REQ-MEDIA-004: 호텔 예약 정보 추출 및 DB화
- [ ] REQ-MEDIA-005: 행사 티켓 정보 추출 및 DB화
- [ ] OCR 사용 제한 구현 (10회/일)

### 3.5 USER 도메인 (사용자 개인화 & 스타일 관리)

#### 담당 DB 테이블
- `users` (사용자 정보)
- `user_sessions` (세션 관리)
- `api_usage_logs` (API 사용 로그)
- `user_preferences` (사용자 선호도)
- `travel_styles` (여행 스타일 템플릿 20+개)
- `user_travel_styles` (선택된 스타일)

#### 담당 Function & 요구사항
```java
// [예시] UserService.java
@Service
public class UserService {
    // REQ-USER-001: JWT 인증 (완료)
    
    // REQ-USER-002: API 사용량 추적
    @Bean("trackApiUsage")
    public Function<UsageRequest, UsageResponse> trackApiUsage() {
        // Perplexity, OCR 사용량 기록
        // DB: api_usage_logs 저장
    }
    
    // REQ-USER-003: 사용자 프로필 관리
    @Bean("manageUserProfile")
    public Function<ProfileRequest, ProfileResponse> manageUserProfile() {
        // 선호 여행 스타일, 관심사 저장
        // DB: user_preferences CRUD
    }
    
    // REQ-USER-004: 여행 스타일 템플릿 제공
    @Bean("getTravelStyleTemplates")
    public Function<StyleRequest, List<TravelStyle>> getTravelStyleTemplates() {
        // 20개 이상의 여행 스타일 템플릿 제공
        // DB: travel_styles 조회
        // 예: 럭셔리, 배낭여행, 가족여행, 먹방투어 등
    }
    
    // REQ-USER-005: 빠른 입력 폼 데이터 처리 ★
    @Bean("processQuickInput")
    public Function<QuickFormData, TravelInfo> processQuickInput() {
        // 빠른 입력 폼 데이터 파싱 및 검증
        // 필수 정보 추출 및 정리
        // DB: travel_info_states 초기화
    }
    
    // REQ-USER-006: 사용자 스타일 선택 저장
    @Bean("saveUserTravelStyle")
    public Function<StyleSelection, StyleResponse> saveUserTravelStyle() {
        // 사용자가 선택한 여행 스타일 저장
        // DB: user_travel_styles CRUD
        // 프론트: 3x3 모달 UI로 표시
    }
    
    // REQ-USER-007: 예약 정보 통합 처리
    @Bean("integrateReservationInfo")
    public Function<ReservationData, IntegratedTravelInfo> integrateReservationInfo() {
        // MEDIA에서 추출한 예약 정보를 여행 정보에 통합
        // 항공 시간 → 여행 날짜 자동 설정
        // 호텔 위치 → 목적지 자동 설정
        // DB: travel_info_states 업데이트
    }
}
```

#### 개발 체크리스트
- [✓] REQ-USER-001: JWT 인증 (완료)
- [ ] REQ-USER-002: API 사용량 추적 서비스
- [ ] REQ-USER-003: 사용자 프로필 및 선호도 관리
- [ ] REQ-USER-004: 여행 스타일 템플릿 제공 (20+개)
- [ ] REQ-USER-005: 빠른 입력 폼 처리 ★
- [ ] REQ-USER-006: 스타일 선택 저장 (3x3 모달 UI)
- [ ] REQ-USER-007: 예약 정보 통합 처리

---

## 4. 개발 일정

### Day 1 (기본 구조)
**오전 (4시간)**:
- [ ] Spring AI 설정 및 Gemini 2.0 Flash 연동
- [ ] Function Calling 기본 구조 구현
- [ ] DB 스키마 생성

**오후 (4시간)**:
- [ ] USER: 빠른 입력 폼 구현
- [ ] USER: 여행 스타일 템플릿 20개 생성
- [ ] CHAT2: Intent 분류 구현

### Day 2 (핵심 기능)
**오전 (4시간)**:
- [ ] TRIP: 여행 계획 생성 Function
- [ ] TRIP: 외부 API 연동 (날씨, Perplexity)
- [ ] MEDIA: OCR 기본 구현

**오후 (4시간)**:
- [ ] CHAT1: 일반 질문 처리 구현
- [ ] CHAT1: 여행으로 대화 유도 로직
- [ ] API 사용 제한 구현

### Day 3 (통합 및 테스트)
**오전 (4시간)**:
- [ ] 전체 플로우 통합 테스트
- [ ] 버그 수정 및 최적화
- [ ] 프롬프트 튜닝

**오후 (4시간)**:
- [ ] 최종 테스트 시나리오 실행
- [ ] 문서화 및 배포 준비
- [ ] 데모 준비

---

## 5. 성공 지표

### 핵심 KPI
- ✅ 여행 계획 생성 성공률 > 90%
- ✅ 평균 응답 시간 < 3초
- ✅ Follow-up 질문 최대 2회
- ✅ 토큰 사용량 < 500토큰/요청
- ✅ API 비용 < $0.1/사용자

### 프론트엔드 UI 요구사항
1. **빠른 입력 폼**: 한 화면에 모든 필수 정보 입력
2. **여행 스타일 선택**: 3x3 그리드 모달 (아티팩트 스타일)
3. **진행 상황 표시**: 정보 수집 진행률 바
4. **채팅 인터페이스**: 자연스러운 대화형 UI

### 일반 질문 응답 예시
```
사용자: "오늘 날씨 어때?"
시스템: "오늘 서울은 맑고 기온은 15도입니다. 날씨가 좋아서 여행 가기 딱 좋은 날이네요! 어디로 여행 가실 계획이신가요?"

사용자: "환율 알려줘"
시스템: "현재 달러 환율은 1,350원입니다. 해외여행 계획이 있으신가요? 예산에 맞는 여행지를 추천해드릴 수 있어요!"

사용자: "안녕하세요"
시스템: "안녕하세요! 저는 여행 계획을 도와드리는 Compass입니다. 어떤 여행을 꿈꾸고 계신가요?"
```