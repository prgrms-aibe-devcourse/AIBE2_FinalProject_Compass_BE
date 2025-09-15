# 📋 요구사항 명세서 및 팀원별 역할 분배

## 목차

1. [MVP 요구사항 명세서](#1-mvp-요구사항-명세서)
2. [도메인별 역할 분배](#2-도메인별-역할-분배)
3. [개발 일정](#3-개발-일정)
4. [Function 매핑 테이블](#4-function-매핑-테이블)
5. [성공 지표](#5-성공-지표)

---

## 1. MVP 요구사항 명세서

### 1.1 핵심 요구사항 (3일 MVP 기준)

#### 🎯 필수 구현 항목 (27개)

##### CHAT1 도메인 요구사항 (4개)
- **REQ-CHAT1-001**: Intent Router 구현 (사용자 의도 분류)
- **REQ-CHAT1-002**: 메시지 라우팅 로직 (여행/일반 채팅 구분)
- **REQ-CHAT1-003**: 기본 채팅 기능 (Thread/Message 관리)
- **REQ-CHAT1-004**: 대화 컨텍스트 유지

##### CHAT2 도메인 요구사항 (10개) - 팀 리더
- **REQ-CHAT2-001**: Spring AI 프레임워크 설정 및 통합
- **REQ-CHAT2-002**: MainLLMOrchestrator 구현 (Gemini 2.0 Flash)
- **REQ-CHAT2-003**: Function Calling 기본 구조 구현
- **REQ-CHAT2-004**: 빠른 입력 폼 시스템 (`quickInputForm`)
- **REQ-CHAT2-005**: 부족 정보 확인/채우기 Functions
- **REQ-CHAT2-006**: 여행 정보 수집 상태 관리
- **REQ-CHAT2-007**: 시스템 프롬프트 최적화
- **REQ-CHAT2-008**: 전체 시스템 통합 테스트
- **REQ-CHAT2-009**: API 사용량 모니터링 및 제한
- **REQ-CHAT2-010**: 프로젝트 총괄 및 진도 관리

##### TRIP 도메인 요구사항 (10개)
- **REQ-TRIP-001**: 여행 계획 생성 Function (`generateTravelPlan`)
- **REQ-TRIP-002**: PostgreSQL 장소 데이터베이스 구축
- **REQ-TRIP-003**: Perplexity API 통합 (`searchWithPerplexity`)
- **REQ-TRIP-004**: 하이브리드 장소 선택 Function (`selectOptimalPlaces`)
- **REQ-TRIP-005**: OpenWeatherMap API 통합 (`getWeatherInfo`)
- **REQ-TRIP-006**: 출발/도착 시간 기반 일정 조정
- **REQ-TRIP-007**: Tour API 크롤링 및 DB 저장
- **REQ-TRIP-008**: 시간대별 목적지 검색 Function (`searchDestinationsByTimeRange`)
- **REQ-TRIP-009**: 거리 계산 Function (`calculateDistance`)
- **REQ-TRIP-010**: 여행 계획 수정 Function (`modifyTravelPlan`)

##### MEDIA 도메인 요구사항 (3개)
- **REQ-MEDIA-001**: OCR Function 구현 (`processOCR`)
- **REQ-MEDIA-002**: 항공권 정보 추출 로직
- **REQ-MEDIA-003**: 호텔 예약서 정보 추출 로직

##### USER 도메인 요구사항 (3개)
- **REQ-USER-001**: JWT 기반 인증/인가 (완료)
- **REQ-USER-002**: API 사용량 추적 서비스
- **REQ-USER-003**: 사용자 프로필 및 선호도 관리

### 1.2 제외 항목 (시간 부족)
- ❌ Redis 캐싱 (DB로 대체)
- ❌ 복잡한 최적화 (병렬 처리 최소화)
- ❌ 8가지 적응형 전략 (2개로 축소)
- ❌ 복잡한 UI (기본 폼만 구현)

---

## 2. 도메인별 역할 분배

### 💡 중요: DB 관련 책임 분담
- **각 도메인은 자체 DB 테이블 관리**
- **Function Calling 개발자 = 해당 DB 조작도 담당**
- **LLM 프롬프트만 작성하는 것이 아니라 DB 연동까지 포함**

### 2.1 CHAT1 도메인 (Intent Router & 기본 채팅 담당)

#### 담당 DB 테이블
- `chat_threads` (Thread 관리)
- `chat_messages` (메시지 저장)
- `chat_contexts` (대화 컨텍스트)

#### 담당 Function & 요구사항
```java
// [예시] IntentRouter.java
@Configuration
public class IntentRouter {
    // REQ-CHAT1-001: Intent 분류
    @Bean("classifyIntent")
    public Function<UserMessage, IntentType> classifyIntent() {
        // 여행 관련 vs 일반 채팅 분류
        // 여행 키워드: 여행, 계획, 여행지, 호텔, 항공 등
    }
    
    // REQ-CHAT1-002: 메시지 라우팅
    @Bean("routeMessage")
    public Function<MessageRequest, RoutingDecision> routeMessage() {
        // Intent에 따라 CHAT2 또는 일반 처리로 라우팅
    }
    
    // REQ-CHAT1-003: Thread/Message 관리
    @Bean("manageThread")
    public Function<ThreadRequest, ThreadResponse> manageThread() {
        // DB: chat_threads, chat_messages CRUD
    }
}
```

#### 개발 체크리스트
- [ ] REQ-CHAT1-001: Intent 분류 로직 구현
- [ ] REQ-CHAT1-002: 메시지 라우팅 로직
- [ ] REQ-CHAT1-003: Thread/Message DB 연동
- [ ] REQ-CHAT1-004: 컨텍스트 유지 로직

### 2.2 CHAT2 도메인 (Spring AI & Function Calling 총괄 - 팀 리더)

#### 담당 DB 테이블
- `chat_threads` (Thread 관리)
- `chat_messages` (메시지 저장)
- `travel_info_collection_states` (정보 수집 상태)

#### 담당 Function & 요구사항
```java
// [예시] MainLLMOrchestrator.java
@Configuration
public class MainLLMOrchestrator {
    // REQ-CHAT2-001: Spring AI 프레임워크 설정
    // REQ-CHAT2-002: 메인 오케스트레이터
    // REQ-CHAT2-003: Function Calling 기본 구조
    
    @Bean("quickInputForm")  // REQ-CHAT2-004 ★ 핵심
    public Function<QuickFormRequest, QuickFormResponse> quickInputForm() {
        // 빠른 입력 폼 처리
        // 필수 정보: 목적지, 날짜, 인원, 예산
        // DB: travel_info_collection_states 저장
    }
    
    @Bean("checkMissingInfo")  // REQ-CHAT2-005
    public Function<CheckRequest, MissingInfoResponse> checkMissingInfo() {
        // 부족한 필수 정보 확인
    }
    
    @Bean("fillMissingInfo")  // REQ-CHAT2-005
    public Function<FillRequest, TravelInfoStatus> fillMissingInfo() {
        // 부족한 정보 채우기 (1-2개 질문만)
    }
    
    // REQ-CHAT2-010: 프로젝트 총괄 기능
    @Bean("monitorSystem")
    public Function<MonitorRequest, SystemStatus> monitorSystem() {
        // 전체 시스템 상태 모니터링
        // API 사용량, 에러율, 성능 체크
    }
}
```

#### 개발 체크리스트 (팀 리더 총괄)
- [ ] REQ-CHAT2-001: Spring AI 프레임워크 설정
- [ ] REQ-CHAT2-002: MainLLMOrchestrator 구현
- [ ] REQ-CHAT2-003: Function Calling 기본 구조
- [ ] REQ-CHAT2-004: 빠른 입력 폼 시스템 ★
- [ ] REQ-CHAT2-005: Follow-up 최소화 (1-2회만)
- [ ] REQ-CHAT2-006: 여행 정보 상태 관리
- [ ] REQ-CHAT2-007: 프롬프트 최적화 (50토큰)
- [ ] REQ-CHAT2-008: 전체 통합 테스트
- [ ] REQ-CHAT2-009: API 사용량 제한 구현
- [ ] REQ-CHAT2-010: 프로젝트 진도 관리

### 2.3 TRIP 도메인 (여행 계획 생성 전담)

#### 담당 DB 테이블
- `travel_plans` (여행 계획)
- `tour_places` (관광지 정보)
- `place_categories` (장소 카테고리)
- `user_preferences` (사용자 선호)

#### 담당 Function & 요구사항
```java
// [예시] TravelPlanGenerator.java
@Configuration
public class TravelPlanGenerator {
    
    @Bean("generateTravelPlan")  // REQ-TRIP-001
    public Function<PlanRequest, PlanResponse> generateTravelPlan() {
        // 여행 계획 생성 (DB + Perplexity + 날씨)
        // DB: travel_plans, tour_places 조회/저장
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
    public Function<WeatherRequest, WeatherResponse> getWeatherInfo() {
        // OpenWeatherMap API 호출
    }
    
    @Bean("searchDestinationsByTimeRange")  // REQ-TRIP-008
    public Function<TimeRangeRequest, List<Destination>> searchDestinationsByTimeRange() {
        // 시간대별 도달 가능 목적지 검색
        // DB: tour_places에서 거리 기반 필터링
    }
    
    @Bean("modifyTravelPlan")  // REQ-TRIP-010
    public Function<ModifyRequest, PlanResponse> modifyTravelPlan() {
        // 여행 계획 수정
        // DB: travel_plans 업데이트
    }
}
```

#### 개발 체크리스트
- [ ] REQ-TRIP-001: generateTravelPlan Function 구현
- [ ] REQ-TRIP-002: PostgreSQL 장소 DB 스키마 생성
- [ ] REQ-TRIP-003: Perplexity API 연동 (사용 제한 포함)
- [ ] REQ-TRIP-004: 하이브리드 검색 로직 구현
- [ ] REQ-TRIP-005: 날씨 API 연동
- [ ] REQ-TRIP-006: 시간 기반 일정 조정 로직
- [ ] REQ-TRIP-007: Tour API 데이터 크롤링
- [ ] REQ-TRIP-008: 시간대별 목적지 검색
- [ ] REQ-TRIP-009: 거리 계산 로직
- [ ] REQ-TRIP-010: 계획 수정 Function

### 2.4 MEDIA 도메인 (OCR 처리 담당)

#### 담당 DB 테이블
- `ocr_results` (OCR 결과 저장)
- `image_metadata` (이미지 메타데이터)

#### 담당 Function & 요구사항
```java
// [예시] OCRProcessor.java
@Configuration
public class OCRProcessor {
    
    @Bean("processOCR")  // REQ-MEDIA-001
    @Description("이미지에서 예약 정보 추출 - 프로덕션 환경에서 일 10회 제한")
    public Function<OCRRequest, ReservationInfo> processOCR() {
        return request -> {
            // API 사용 제한 확인
            if (isProduction() && !apiUsagePolicy.canUseOCR(request.userId())) {
                return new ReservationInfo(null, "OCR 한도 초과", true);
            }
            
            // Google Vision API 호출
            String text = ocrService.extractText(request.imageData());
            
            // REQ-MEDIA-002: 항공권 정보 추출
            // REQ-MEDIA-003: 호텔 정보 추출
            // DB: ocr_results에 결과 저장
            
            return extractReservationInfo(text);
        };
    }
}
```

#### 개발 체크리스트
- [ ] REQ-MEDIA-001: Google Vision API 연동
- [ ] REQ-MEDIA-002: 항공권 텍스트 파싱 로직
- [ ] REQ-MEDIA-003: 호텔 예약서 파싱 로직
- [ ] OCR 사용 제한 구현 (10회/일)

### 2.5 USER 도메인 (인증/인가 담답)

#### 담당 DB 테이블
- `users` (사용자 정보)
- `user_sessions` (세션 관리)
- `api_usage_logs` (API 사용 로그)
- `user_preferences` (사용자 선호도)

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
}
```

#### 담당 DB 테이블
- `users` (사용자 정보)
- `user_sessions` (세션 관리)
- `api_usage_logs` (API 사용 로그)

#### 개발 체크리스트
- [✓] REQ-USER-001: JWT 인증 (완료)
- [ ] REQ-USER-002: API 사용량 추적 서비스
- [ ] REQ-USER-003: 사용자 프로필 및 선호도 관리

---

## 3. 개발 일정

### Day 1 (기본 구조)
**오전 (4시간)**:
- [ ] Spring AI 설정 및 Gemini 2.0 Flash 연동
- [ ] Function Calling 기본 구조 구현
- [ ] DB 스키마 생성

**오후 (4시간)**:
- [ ] CHAT2: 정보 수집 Functions 구현
- [ ] TRIP: 장소 검색 Functions 구현
- [ ] 통합 테스트 환경 구축

### Day 2 (핵심 기능)
**오전 (4시간)**:
- [ ] TRIP: 여행 계획 생성 Function
- [ ] TRIP: 외부 API 연동 (날씨, Perplexity)
- [ ] MEDIA: OCR 기본 구현

**오후 (4시간)**:
- [ ] 하이브리드 검색 로직 구현
- [ ] 목적지 미정 사용자 처리
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

## 4. Function 매핑 테이블

| Function 이름 | 도메인 | 요구사항 ID | 설명 |
|--------------|---------|------------|------|
| `analyzeUserInput` | CHAT2 | REQ-CHAT2-003 | 사용자 입력에서 여행 정보 추출 |
| `checkMissingInfo` | CHAT2 | REQ-CHAT2-004 | 부족한 필수 정보 확인 |
| `fillMissingInfo` | CHAT2 | REQ-CHAT2-005 | 부족한 정보 채우기 |
| `detectUndecidedDestination` | CHAT2 | REQ-CHAT2-008 | 목적지 미정 감지 |
| `getReachableAreas` | CHAT2 | REQ-CHAT2-009 | 도달 가능 지역 계산 |
| `generateTravelPlan` | TRIP | REQ-TRIP-001 | 여행 계획 생성 (통합) |
| `searchWithPerplexity` | TRIP | REQ-TRIP-003 | Perplexity로 트렌디 장소 검색 |
| `selectOptimalPlaces` | TRIP | REQ-TRIP-004 | 하이브리드 장소 선택 |
| `getWeatherInfo` | TRIP | REQ-TRIP-005 | 날씨 정보 조회 |
| `searchDestinationsByTimeRange` | TRIP | REQ-TRIP-008 | 시간대별 목적지 검색 |
| `calculateDistance` | TRIP | REQ-TRIP-009 | 거리 계산 |
| `modifyTravelPlan` | TRIP | REQ-TRIP-010 | 여행 계획 수정 |
| `processOCR` | MEDIA | REQ-MEDIA-001 | 이미지에서 텍스트 추출 |

---

## 5. 성공 지표

### 5.1 기능적 성공 지표
- ✅ 모든 필수 Function 동작
- ✅ 정보 수집 → 계획 생성 플로우 완성
- ✅ 외부 API 정상 연동
- ✅ DB CRUD 작업 정상 동작

### 5.2 성능 지표
- 응답 시간: 평균 3초 이내
- 토큰 사용량: 세션당 2,000토큰 이내
- API 비용: 사용자당 일 $0.5 이내

### 5.3 품질 지표
- 테스트 커버리지: 80% 이상
- 주요 시나리오 5개 모두 통과
- 에러율: 5% 이하

---

## 테스트 시나리오

### 시나리오 1: 기본 정보로 계획 생성
```
사용자: "제주도 3박4일 여행 계획 짜줘"
→ 빠른 입력 폼 제시
→ 필수 정보 입력
→ 여행 계획 생성
```

### 시나리오 2: 목적지 미정 사용자
```
사용자: "이번 주말에 어디 갈까?"
→ 출발지 확인
→ 이동 수단 확인
→ 시간대별 추천
→ 목적지 선택
→ 계획 생성
```

### 시나리오 3: OCR 활용
```
사용자: 항공권 사진 업로드
→ OCR 정보 추출
→ 부족한 정보만 추가 입력
→ 계획 생성
```

### 시나리오 4: 하이브리드 검색
```
사용자: "부산 핫플레이스 포함해서 계획 짜줘"
→ DB 검색 (기본 장소)
→ Perplexity 검색 (트렌디 장소)
→ 조합하여 계획 생성
```

### 시나리오 5: 계획 수정
```
사용자: "첫날 일정 좀 여유롭게 바꿔줘"
→ 기존 계획 조회
→ 수정 요청 분석
→ 계획 업데이트
```

---

## 커뮤니케이션 가이드

### GitHub Discussion 활용
1. 이 문서를 Discussion에 업로드
2. 각 도메인별 담당자 태그
3. 의견 수렴 및 수정
4. 최종 확정 후 개발 시작

### Daily Sync
- 매일 오전 10시 진행 상황 공유
- 블로커 이슈 즉시 공유
- PR 리뷰는 당일 처리

### 코드 리뷰 규칙
- Function 구현 시 테스트 코드 필수
- DB 스키마 변경 시 팀 전체 리뷰
- 외부 API 사용 시 비용 검토 필수

---

**이 문서는 팀원 간 역할 분담과 구체적인 요구사항을 명시합니다.**
**프로젝트 전반적인 이해는 PROJECT_OVERVIEW.md를 참조하세요.**