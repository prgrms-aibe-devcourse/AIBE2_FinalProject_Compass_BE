# 🔄 LLM 오케스트레이터 패턴 마이그레이션 가이드

## 📋 목차
1. [개요](#개요)
2. [현재 상태 분석](#현재-상태-분석)
3. [목표 아키텍처](#목표-아키텍처)
4. [디렉토리 구조](#디렉토리-구조)
5. [마이그레이션 계획](#마이그레이션-계획)
6. [구현 상세](#구현-상세)

---

## 개요

### 목적
분산된 서비스 구조에서 **중앙 집중형 LLM 오케스트레이터 패턴**으로 전환하여 CHAT2 워크플로우 구현

### 핵심 원칙
- **단일 진입점**: `/api/chat/unified` 하나로 모든 요청 처리
- **Function 기반**: 모든 기능을 Function으로 모듈화
- **Phase 관리**: 5단계 Phase로 대화 흐름 제어
- **Intent 분류**: 사용자 의도 파악 후 적절한 Function 실행

---

## 현재 상태 분석

### 현재 구조의 문제점
```
❌ 분산된 서비스 구조
- 40개의 독립적인 Service 클래스
- 각 Service가 직접 LLM 호출
- 복잡한 의존성 체인
- 상태 관리 분산

❌ 다중 엔드포인트
- /api/chat/message
- /api/chat/collection
- /api/media/upload
- 클라이언트가 여러 API 호출 필요
```

### 유지할 코드 (21%)
- **인증/인가**: JWT, OAuth 관련 전체
- **Entity/Repository**: ChatThread, ChatMessage, User
- **기본 설정**: SecurityConfig, AiConfig, RedisConfig

### 버릴 코드 (68%)
- **모든 Service 클래스**: 40개 Service
- **기존 Controller**: ChatController 등
- **복잡한 상태 관리**: TravelInfoCollectionState

---

## 목표 아키텍처

### 오케스트레이터 패턴
```
사용자 요청
    ↓
UnifiedChatController (/api/chat/unified)
    ↓
MainLLMOrchestrator
    ├── Intent 분류
    ├── Phase 관리
    └── Function 실행
        ├── showQuickInputForm
        ├── processImage (OCR)
        ├── generateTravelPlan
        └── modifyPlan
```

### 5단계 Phase 시스템
1. **INITIALIZATION**: 초기화 및 Intent 분류
2. **INFORMATION_COLLECTION**: 정보 수집 (폼, OCR)
3. **PLAN_GENERATION**: 계획 생성
4. **FEEDBACK_REFINEMENT**: 피드백 및 수정
5. **COMPLETION**: 완료

---

## 디렉토리 구조

```
/src/main/java/com/compass/
├── config/                        # ✅ 전역 설정 (유지)
│   ├── SecurityConfig.java
│   ├── jwt/
│   │   ├── JwtTokenProvider.java
│   │   └── JwtAuthenticationFilter.java
│   ├── oauth/
│   │   └── OAuth2AuthenticationSuccessHandler.java
│   ├── AiConfig.java
│   └── RedisConfig.java
│
├── domain/
│   ├── auth/                      # 🔐 인증/인가 (독립 유지)
│   │   ├── controller/
│   │   │   └── AuthController.java
│   │   │       @PostMapping("/api/auth/login")
│   │   │       @PostMapping("/api/auth/signup")
│   │   ├── service/
│   │   │   └── AuthService.java
│   │   ├── entity/
│   │   │   └── User.java
│   │   └── repository/
│   │       └── UserRepository.java
│   │
│   └── chat/                      # 💬 메인 도메인 (리팩토링)
│       ├── controller/
│       │   └── UnifiedChatController.java  # 🔥 단일 진입점 [Chat2 개발자]
│       │
│       ├── orchestrator/           # [Chat2 개발자 전담]
│       │   ├── MainLLMOrchestrator.java    # 🧠 중앙 제어
│       │   ├── IntentClassifier.java       # Intent 분류
│       │   └── PhaseManager.java           # Phase 관리
│       │
│       ├── function/               # 📦 Function 구현 (17개)
│       │   ├── config/
│       │   │   └── FunctionConfiguration.java  # [공통 - Chat2 개발자]
│       │   │
│       │   ├── collection/         # [User 개발자 전담]
│       │   │   ├── ShowQuickInputFormFunction.java      # 빠른 입력 폼 표시
│       │   │   ├── SubmitTravelFormFunction.java        # 폼 제출 처리
│       │   │   ├── AnalyzeUserInputFunction.java        # 사용자 입력 분석
│       │   │   ├── StartFollowUpFunction.java           # Follow-up 시작
│       │   │   └── ContinueFollowUpFunction.java        # Follow-up 계속
│       │   │
│       │   ├── processing/         # [Media & Chat 개발자 분담]
│       │   │   ├── ProcessImageFunction.java            # 이미지 처리 [Media]
│       │   │   ├── ProcessOCRFunction.java              # OCR 처리 [Media]
│       │   │   ├── ExtractFlightInfoFunction.java       # 항공권 추출 [Media]
│       │   │   ├── ExtractHotelInfoFunction.java        # 호텔 추출 [Media]
│       │   │   ├── HandleGeneralQuestionFunction.java   # 일반 질문 [Chat]
│       │   │   ├── ProvideGeneralInfoFunction.java      # 일반 정보 [Chat]
│       │   │   └── HandleUnknownFunction.java           # 미분류 처리 [Chat]
│       │   │
│       │   ├── planning/           # [User & Trip 개발자 분담]
│       │   │   ├── GenerateTravelPlanFunction.java      # 여행 계획 생성 [Trip]
│       │   │   ├── RecommendDestinationsFunction.java   # 목적지 추천 [User]
│       │   │   └── SearchDestinationsFunction.java      # 목적지 검색 [Trip]
│       │   │
│       │   ├── external/           # [Trip 개발자 전담]
│       │   │   ├── SearchWithPerplexityFunction.java    # Perplexity 검색
│       │   │   └── SearchTourAPIFunction.java           # 관광공사 API
│       │   │
│       │   └── refinement/         # [Trip 개발자]
│       │       └── ModifyTravelPlanFunction.java        # 계획 수정
│       │
│       ├── service/                # 🔧 헬퍼 서비스
│       │   ├── internal/
│       │   │   └── ChatThreadService.java       # [Chat 개발자]
│       │   └── external/
│       │       ├── S3Client.java                # [Media 개발자]
│       │       ├── OCRClient.java               # [Media 개발자]
│       │       └── PerplexityClient.java        # [Trip 개발자]
│       │
│       ├── model/
│       │   ├── enums/              # [Chat2 개발자]
│       │   │   ├── Intent.java          # 9개 Intent
│       │   │   └── TravelPhase.java     # 5개 Phase
│       │   │
│       │   ├── request/            # [Trip 개발자]
│       │   │   ├── ChatRequest.java
│       │   │   ├── TravelPlanRequest.java
│       │   │   └── TravelFormSubmitRequest.java  # [User 개발자]
│       │   │
│       │   ├── response/           # [Trip 개발자]
│       │   │   ├── ChatResponse.java
│       │   │   └── TravelPlanResponse.java
│       │   │
│       │   ├── dto/                # [개발자별 분담]
│       │   │   ├── QuickInputFormDto.java       # [User 개발자]
│       │   │   ├── FlightReservation.java       # [Media 개발자]
│       │   │   └── HotelReservation.java        # [Media 개발자]
│       │   │
│       │   └── context/
│       │       └── TravelContext.java           # [Chat2 개발자]
│       │
│       ├── entity/                 # ✅ 유지
│       │   ├── ChatThread.java
│       │   └── ChatMessage.java
│       │
│       └── repository/             # ✅ 유지
│           ├── ChatThreadRepository.java
│           └── ChatMessageRepository.java
```

## 📋 담당자별 구현 파일 정리

### 🧠 Chat2 개발자 (8개 파일)
```
orchestrator/
├── MainLLMOrchestrator.java      # 메인 오케스트레이터 (최우선)
├── IntentClassifier.java          # Intent 분류기
└── PhaseManager.java              # Phase 관리자

controller/
└── UnifiedChatController.java     # 통합 컨트롤러

model/enums/
├── Intent.java                    # 9개 Intent 정의
└── TravelPhase.java               # 5개 Phase 정의

function/config/
└── FunctionConfiguration.java     # Function Bean 설정

model/context/
└── TravelContext.java             # 대화 컨텍스트 관리
```

### 👤 User 개발자 (8개 파일)
```
function/collection/
├── ShowQuickInputFormFunction.java       # 빠른 입력 폼 (최우선)
├── SubmitTravelFormFunction.java         # 폼 제출
├── AnalyzeUserInputFunction.java         # 입력 분석
├── StartFollowUpFunction.java            # Follow-up 시작
└── ContinueFollowUpFunction.java         # Follow-up 계속

function/planning/
└── RecommendDestinationsFunction.java    # 목적지 추천

model/
├── dto/QuickInputFormDto.java            # 폼 구조 정의
└── request/TravelFormSubmitRequest.java  # 폼 제출 데이터
```

### 💬 Chat 개발자 (5개 파일)
```
function/processing/
├── HandleGeneralQuestionFunction.java    # 일반 질문 처리 (최우선)
├── ProvideGeneralInfoFunction.java       # 일반 정보 제공
└── HandleUnknownFunction.java            # 미분류 처리

service/internal/
└── ChatThreadService.java                # 대화 스레드 관리

domain/auth/controller/
└── AuthController.java                   # 인증 컨트롤러
```

### 📷 Media 개발자 (8개 파일)
```
function/processing/
├── ProcessImageFunction.java             # 이미지 처리 (최우선)
├── ProcessOCRFunction.java               # OCR 처리
├── ExtractFlightInfoFunction.java        # 항공권 정보 추출
└── ExtractHotelInfoFunction.java         # 호텔 정보 추출

service/external/
├── S3Client.java                         # S3 업로드
└── OCRClient.java                        # OCR API 클라이언트

model/dto/
├── FlightReservation.java                # 항공권 데이터
└── HotelReservation.java                 # 호텔 데이터
```

### ✈️ Trip 개발자 (10개 파일)
```
function/planning/
├── GenerateTravelPlanFunction.java       # 여행 계획 생성 (최우선)
└── SearchDestinationsFunction.java       # 목적지 검색

function/external/
├── SearchWithPerplexityFunction.java     # Perplexity 검색
└── SearchTourAPIFunction.java            # 관광공사 API

function/refinement/
└── ModifyTravelPlanFunction.java         # 계획 수정

service/external/
└── PerplexityClient.java                 # Perplexity 클라이언트

model/request/
├── ChatRequest.java                      # 채팅 요청
└── TravelPlanRequest.java                # 여행 계획 요청

model/response/
├── ChatResponse.java                     # 채팅 응답
└── TravelPlanResponse.java               # 여행 계획 응답
```

---

## 마이그레이션 계획

### Phase 1: 기반 구조 정리 (Day 1)
```bash
# 1. 백업 생성
git checkout -b backup/before-migration
git push origin backup/before-migration

# 2. 새 브랜치 생성
git checkout -b feat/orchestrator-migration

# 3. Migration 스크립트 실행
cd Migration/scripts
chmod +x *.sh

# 디렉토리 구조 생성
./create_skeleton.sh

# 기존 파일 복사 (JWT, Entity, Repository 등)
./copy_existing_files.sh

# 37개 스켈레톤 파일 생성
./create_empty_files.sh
```

### Phase 2: 핵심 컴포넌트 구현 (Day 2)

#### 2.1 UnifiedChatController
```java
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class UnifiedChatController {

    private final MainLLMOrchestrator orchestrator;

    @PostMapping("/unified")
    public ResponseEntity<ChatResponse> handleChat(
            @RequestBody ChatRequest request,
            @AuthenticationPrincipal UserDetails user,
            @RequestHeader(value = "X-Thread-Id", required = false) String threadId
    ) {
        log.info("Chat request received - User: {}, Thread: {}",
                user.getUsername(), threadId);

        try {
            ChatResponse response = orchestrator.orchestrate(
                request,
                user.getUsername(),
                threadId
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Chat processing error", e);
            return ResponseEntity.internalServerError()
                .body(ChatResponse.error("처리 중 오류가 발생했습니다"));
        }
    }
}
```

#### 2.2 MainLLMOrchestrator
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class MainLLMOrchestrator {

    private final ChatModel chatModel;
    private final IntentClassifier intentClassifier;
    private final PhaseManager phaseManager;
    private final Map<String, FunctionCallback> functions;

    public ChatResponse orchestrate(
            ChatRequest request,
            String userId,
            String threadId
    ) {
        // 1. Intent 분류
        Intent intent = intentClassifier.classify(request.getMessage());
        log.info("Intent classified: {}", intent);

        // 2. Phase 결정
        TravelPhase phase = phaseManager.getCurrentPhase(threadId);
        log.info("Current phase: {}", phase);

        // 3. Function 선택
        List<String> selectedFunctions = selectFunctions(intent, phase);
        log.info("Selected functions: {}", selectedFunctions);

        // 4. LLM 호출 with Functions
        FunctionCallingOptions options = FunctionCallingOptions.builder()
            .functions(selectedFunctions)
            .build();

        ChatResponse response = chatModel.call(
            new Prompt(buildPrompt(request, phase), options)
        );

        // 5. Phase 업데이트
        phaseManager.updatePhase(threadId, response);

        return response;
    }

    private List<String> selectFunctions(Intent intent, TravelPhase phase) {
        return switch (intent) {
            case TRAVEL_PLANNING -> List.of(
                "showQuickInputForm",
                "analyzeUserInput"
            );
            case IMAGE_UPLOAD -> List.of(
                "processImage",
                "extractReservationInfo"
            );
            default -> List.of("handleGeneralQuestion");
        };
    }
}
```

### Phase 3: Function 구현 (Day 3)

#### 3.1 Quick Input Form Function
```java
@Configuration
public class FunctionConfiguration {

    @Bean("showQuickInputForm")
    public Function<QuickFormRequest, QuickFormResponse> showQuickInputForm() {
        return request -> {
            return QuickFormResponse.builder()
                .formType("QUICK_INPUT_V2")
                .fields(List.of(
                    FormField.builder()
                        .name("destinations")
                        .type("tag-input")
                        .label("목적지")
                        .placeholder("목적지 입력 후 Enter")
                        .options(List.of("목적지 미정"))
                        .required(true)
                        .build(),
                    FormField.builder()
                        .name("departureLocation")
                        .type("text-input")
                        .label("출발지")
                        .required(true)
                        .build(),
                    FormField.builder()
                        .name("travelDates")
                        .type("date-range-picker")
                        .label("여행 날짜")
                        .required(true)
                        .build()
                ))
                .build();
        };
    }

    @Bean("processImage")
    public Function<ImageRequest, ImageResponse> processImage(
            S3Client s3Client,
            OCRClient ocrClient
    ) {
        return request -> {
            // S3 업로드
            String url = s3Client.upload(request.getFile());

            // OCR 처리
            String text = ocrClient.extractText(request.getFile());

            // 여행 정보 추출
            TravelInfo info = parseTravelInfo(text);

            return ImageResponse.builder()
                .imageUrl(url)
                .extractedText(text)
                .travelInfo(info)
                .build();
        };
    }
}
```

#### 3.2 간소화된 OCR Service
```java
@Service
@Slf4j
public class OCRClient {

    public String extractText(MultipartFile file) {
        try {
            // Google Vision API 간단 호출
            ImageAnnotatorClient vision = ImageAnnotatorClient.create();
            ByteString imgBytes = ByteString.copyFrom(file.getBytes());

            AnnotateImageResponse response = vision.annotateImage(
                AnnotateImageRequest.newBuilder()
                    .setImage(Image.newBuilder().setContent(imgBytes))
                    .addFeatures(Feature.newBuilder()
                        .setType(Feature.Type.TEXT_DETECTION))
                    .build()
            );

            return response.getFullTextAnnotation().getText();
        } catch (Exception e) {
            log.error("OCR 실패", e);
            return "";
        }
    }
}
```

### Phase 4: 테스트 및 검증 (Day 4)

#### 통합 테스트
```java
@SpringBootTest
@AutoConfigureMockMvc
class UnifiedChatControllerTest {

    @Test
    void 여행_계획_요청_처리() {
        // given
        ChatRequest request = ChatRequest.builder()
            .message("제주도 여행 계획 짜줘")
            .build();

        // when
        ResponseEntity<ChatResponse> response = controller.handleChat(
            request,
            mockUser(),
            "thread-123"
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getFormType()).isEqualTo("QUICK_INPUT_V2");
    }
}
```

---

## 구현 상세

### Intent 열거형 (9개)
```java
public enum Intent {
    TRAVEL_PLANNING,           // 여행 계획 생성
    INFORMATION_COLLECTION,    // 정보 수집
    IMAGE_UPLOAD,             // 이미지 업로드
    GENERAL_QUESTION,         // 일반 질문
    QUICK_INPUT,              // 빠른 입력 폼
    DESTINATION_SEARCH,       // 목적지 검색
    RESERVATION_PROCESSING,   // 예약 정보 처리
    API_USAGE_CHECK,         // API 사용량 조회
    UNKNOWN                  // 알 수 없음
}
```

### TravelPhase 열거형 (5개)
```java
public enum TravelPhase {
    INITIALIZATION("초기화"),
    INFORMATION_COLLECTION("정보 수집"),
    PLAN_GENERATION("계획 생성"),
    FEEDBACK_REFINEMENT("피드백 처리"),
    COMPLETION("완료");

    private final String koreanName;
}
```

### API 요청/응답 예시

#### 요청
```json
POST /api/chat/unified
{
    "message": "제주도 여행 계획 짜줘",
    "threadId": "thread-123",
    "attachments": [
        {
            "type": "image",
            "url": "reservation.jpg"
        }
    ]
}
```

#### 응답 (Quick Form)
```json
{
    "type": "FORM",
    "formType": "QUICK_INPUT_V2",
    "fields": [
        {
            "name": "destinations",
            "type": "tag-input",
            "label": "목적지",
            "options": ["목적지 미정"]
        }
    ]
}
```

---

## 체크리스트

### 삭제할 코드
- [ ] chat/service/*.java (40개 Service)
- [ ] chat/controller/*.java (기존 Controller)
- [ ] chat/parser/*.java
- [ ] media/service/*.java (OCR 제외하고)

### 유지할 코드
- [x] config/jwt/*
- [x] config/oauth/*
- [x] domain/auth/* (전체)
- [x] chat/entity/ChatThread.java
- [x] chat/entity/ChatMessage.java
- [x] chat/repository/*

### 새로 구현할 코드
- [ ] UnifiedChatController
- [ ] MainLLMOrchestrator
- [ ] IntentClassifier
- [ ] PhaseManager
- [ ] FunctionConfiguration
- [ ] 각종 Function 구현

---

## 예상 일정

| 단계 | 작업 내용 | 예상 시간 |
|------|----------|-----------|
| Day 1 | 기반 구조 정리 및 코드 삭제 | 0.5일 |
| Day 2 | 오케스트레이터 핵심 구현 | 1일 |
| Day 3 | Function 구현 | 1.5일 |
| Day 4 | 테스트 및 버그 수정 | 1일 |
| **Total** | **전체 마이그레이션** | **4일** |

---

## 주의사항

1. **백업 필수**: 마이그레이션 전 반드시 백업 브랜치 생성
2. **점진적 전환**: 기존 API와 병행 운영 후 점진적 전환
3. **테스트 우선**: 각 Function 단위 테스트 필수
4. **AI 코드 제거**: 너무 완벽한 주석, 과도한 로깅 제거

---

## 📊 생성 파일 통계

### 스켈레톤 파일 (39개)
- **오케스트레이터**: 3개 (MainLLMOrchestrator, IntentClassifier, PhaseManager)
- **Function 클래스**: 18개 (17개 + ModifyTravelPlanFunction)
- **Function 설정**: 1개 (FunctionConfiguration)
- **컨트롤러**: 2개 (UnifiedChatController, AuthController)
- **Enum**: 2개 (Intent 9개 값, TravelPhase 5개 값)
- **데이터 모델**: 9개 (Request, Response, DTO, Context)
- **서비스**: 1개 (ChatThreadService)
- **외부 클라이언트**: 3개 (S3, OCR, Perplexity)

### 팀원별 담당
- **Chat2 개발자**: 8개 파일
- **User 개발자**: 8개 파일
- **Chat 개발자**: 5개 파일
- **Media 개발자**: 8개 파일
- **Trip 개발자**: 10개 파일
- **총합**: 39개 파일

✅ **워크플로우 100% 준수 확인 완료**

---

작성일: 2024-12-30
버전: 1.1.0