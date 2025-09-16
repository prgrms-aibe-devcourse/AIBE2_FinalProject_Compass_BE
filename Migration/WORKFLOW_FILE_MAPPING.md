# 📋 워크플로우 파일 매핑 가이드

> CHAT2 워크플로우의 각 단계에서 실제로 사용되는 파일들을 보여주는 가이드

## 🎯 개요

이 문서는 `/documents/CHAT2_TRAVEL_WORKFLOW_DETAILED.md`의 워크플로우를 따르면서, 각 단계에서 실제로 어떤 Java 파일이 실행되는지 보여줍니다.

---

## 🏗️ 시스템 아키텍처 & 파일 매핑

```
사용자 로그인/회원가입
    ↓
[AuthController.java] - /api/auth/* 엔드포인트 [Chat 개발자]
    ↓
사용자 요청
    ↓
[UnifiedChatController.java] - /api/chat/unified 엔드포인트
    ↓
[MainLLMOrchestrator.java] - 중앙 오케스트레이터
    ├── [IntentClassifier.java] - Intent 분류
    ├── [PhaseManager.java] - Phase 관리
    └── [FunctionConfiguration.java] - Function Bean 설정
```

---

## 📌 Phase 1: INITIALIZATION (초기화)

### 1.1 사용자 요청 수신

```java
// UnifiedChatController.java [Chat2 개발자]
@PostMapping("/unified")
public ResponseEntity<ChatResponse> handleChat(
    @RequestBody ChatRequest request  // ChatRequest.java [Trip 개발자]
) {
    // 요청을 MainLLMOrchestrator로 전달
    return orchestrator.orchestrate(request);
}
```

### 1.2 Intent 분류

```java
// MainLLMOrchestrator.java [Chat2 개발자]
public ChatResponse orchestrate(ChatRequest request) {
    // 1. Intent 분류
    Intent intent = intentClassifier.classify(request.getMessage());
    // Intent.java [Chat2 개발자] - 9개 Intent enum

    // 2. Phase 확인
    TravelPhase phase = phaseManager.getCurrentPhase(threadId);
    // TravelPhase.java [Chat2 개발자] - 5개 Phase enum

    // 3. Context 관리
    TravelContext context = getOrCreateContext(threadId);
    // TravelContext.java [Chat2 개발자]
}
```

### 1.3 Intent별 Function 선택

```java
// IntentClassifier.java [Chat2 개발자]
private List<Function> selectFunctionsForIntent(Intent intent) {
    return switch (intent) {
        case TRAVEL_PLANNING -> List.of(
            showQuickInputFormFunction,      // ShowQuickInputFormFunction.java [User]
            analyzeUserInputFunction,         // AnalyzeUserInputFunction.java [User]
            generateTravelPlanFunction        // GenerateTravelPlanFunction.java [Trip]
        );
        case INFORMATION_COLLECTION -> List.of(
            analyzeUserInputFunction,         // AnalyzeUserInputFunction.java [User]
            startFollowUpFunction,            // StartFollowUpFunction.java [User]
            continueFollowUpFunction          // ContinueFollowUpFunction.java [User]
        );
        case IMAGE_UPLOAD -> List.of(
            processImageFunction,             // ProcessImageFunction.java [Media]
            processOCRFunction,               // ProcessOCRFunction.java [Media]
            extractFlightInfoFunction,        // ExtractFlightInfoFunction.java [Media]
            extractHotelInfoFunction          // ExtractHotelInfoFunction.java [Media]
        );
        // ... 더 많은 케이스
    };
}
```

---

## 📌 Phase 2: INFORMATION_COLLECTION (정보 수집)

### 2.1 빠른 입력 폼 표시

#### Case A: 여행 계획 요청 시

```java
// ShowQuickInputFormFunction.java [User 개발자]
public QuickInputFormDto apply(ChatRequest request) {
    // QuickInputFormDto.java [User 개발자] 생성
    return QuickInputFormDto.builder()
        .formType("QUICK_INPUT_V2")
        .fields(createFormFields())
        .build();
}

// 폼 필드 정의
private List<FormField> createFormFields() {
    // 목적지 (태그 입력)
    // 출발지 (직접 입력)
    // 날짜 (달력 선택)
    // 동행자 (선택)
    // 예산 (선택 + 직접입력)
    // 여행스타일 (다중선택)
    // 예약서 업로드 (선택)
}
```

### 2.2 폼 제출 처리

```java
// SubmitTravelFormFunction.java [User 개발자]
public ChatResponse apply(TravelFormSubmitRequest request) {
    // TravelFormSubmitRequest.java [User 개발자]
    // ChatResponse.java [Trip 개발자] - 응답 객체

    // 1. 입력 분석
    AnalyzeUserInputFunction analyzeFunction; // [User 개발자]
    TravelInfo info = analyzeFunction.apply(request);

    // 2. 필수 정보 확인
    if (!info.isComplete()) {
        // Follow-up 시작
        StartFollowUpFunction followUpFunction; // [User 개발자]
        return followUpFunction.apply(info);
    }

    // 3. Phase 전환
    phaseManager.transitionPhase(threadId, PLAN_GENERATION);
}
```

### 2.3 목적지 미정 처리

```java
// RecommendDestinationsFunction.java [User 개발자]
public DestinationRecommendation apply(TravelInfo info) {
    if (info.getDestination().equals("목적지 미정")) {
        // 1. 출발지 기반 거리 옵션 제시
        // 2. Perplexity로 도시 검색
        PerplexityClient perplexityClient; // [Trip 개발자]
        List<City> recommendations = perplexityClient.searchCities(
            info.getDepartureLocation(),
            info.getTravelStyle()
        );

        // 3. 추천 결과 반환
        return buildRecommendations(recommendations);
    }
}
```

### 2.4 이미지 업로드 처리 (예약서)

```java
// ProcessImageFunction.java [Media 개발자]
public ImageProcessResult apply(ImageUploadRequest request) {
    // 1. S3 업로드
    S3Client s3Client; // [Media 개발자]
    String imageUrl = s3Client.upload(request.getImage());

    // 2. OCR 처리
    ProcessOCRFunction ocrFunction; // [Media 개발자]
    OCRClient ocrClient; // [Media 개발자] - Google Vision API 클라이언트
    OCRResult ocrResult = ocrFunction.apply(imageUrl);

    // 3. 정보 추출
    if (ocrResult.isFlightTicket()) {
        ExtractFlightInfoFunction flightFunction; // [Media 개발자]
        FlightReservation flight = flightFunction.apply(ocrResult);
        // FlightReservation.java [Media 개발자]
    }

    if (ocrResult.isHotelReservation()) {
        ExtractHotelInfoFunction hotelFunction; // [Media 개발자]
        HotelReservation hotel = hotelFunction.apply(ocrResult);
        // HotelReservation.java [Media 개발자]
    }
}
```

---

## 📌 Phase 3: PLAN_GENERATION (계획 생성)

### 3.1 여행 계획 생성

```java
// GenerateTravelPlanFunction.java [Trip 개발자]
public TravelPlanResponse apply(TravelPlanRequest request) {
    // TravelPlanRequest.java [Trip 개발자]
    // TravelPlanResponse.java [Trip 개발자]

    // 1. 데이터 수집 (병렬 처리)
    CompletableFuture<List<Place>> dbPlaces = searchInDatabase();
    CompletableFuture<List<Place>> trendyPlaces = searchTrendy();
    CompletableFuture<Weather> weather = getWeather();

    // 2. 외부 API 검색
    SearchWithPerplexityFunction perplexitySearch; // [Trip 개발자]
    PerplexityClient perplexityClient; // [Trip 개발자] - Perplexity API 클라이언트
    List<Place> perplexityPlaces = perplexitySearch.apply(request);

    SearchTourAPIFunction tourAPISearch; // [Trip 개발자]
    List<Place> tourAPIPlaces = tourAPISearch.apply(request);

    // 3. LLM으로 일정 생성
    TravelPlan plan = generateWithLLM(allPlaces, weather);

    // 4. 응답 생성
    return TravelPlanResponse.builder()
        .plan(plan)
        .message("여행 계획이 생성되었습니다!")
        .build();
}
```

### 3.2 복수 목적지 처리

```java
// SearchDestinationsFunction.java [Trip 개발자]
public MultiCityPlan apply(List<String> destinations) {
    // 복수 도시 경로 최적화
    // ["경주", "포항", "울산"] → 최적 경로 계산

    for (String city : destinations) {
        // 각 도시별 장소 검색
        SearchWithPerplexityFunction searchFunction; // [Trip 개발자]
        List<Place> places = searchFunction.searchCity(city);
    }

    return optimizeRoute(destinations, places);
}
```

---

## 📌 Phase 4: FEEDBACK_REFINEMENT (피드백 처리)

### 4.1 계획 수정

```java
// ModifyTravelPlanFunction.java [Trip 개발자]
public TravelPlanResponse apply(ModifyRequest request) {
    // 기존 계획 로드
    TravelPlan currentPlan = loadPlan(request.getPlanId());

    // 수정 사항 적용
    TravelPlan modifiedPlan = applyModifications(
        currentPlan,
        request.getModifications()
    );

    // 저장 및 반환
    savePlan(modifiedPlan);
    return new TravelPlanResponse(modifiedPlan);
}
```

### 4.2 일반 질문 처리

```java
// HandleGeneralQuestionFunction.java [Chat 개발자]
public ChatResponse apply(ChatRequest request) {
    // 여행과 무관한 일반 질문 처리

    if (isWeatherQuestion(request)) {
        ProvideGeneralInfoFunction infoFunction; // ProvideGeneralInfoFunction.java [Chat 개발자]
        return infoFunction.provideWeatherInfo();
    }

    // 분류 불가능한 경우
    HandleUnknownFunction unknownFunction; // HandleUnknownFunction.java [Chat 개발자]
    return unknownFunction.apply(request);
}
```

---

## 📌 Phase 5: COMPLETION (완료)

### 5.1 최종 저장

```java
// ChatThreadService.java [Chat 개발자]
public void saveFinalPlan(String threadId, TravelPlan plan) {
    // 1. ChatThread 업데이트
    ChatThread thread = chatThreadRepository.findById(threadId);
    thread.setStatus("COMPLETED");
    thread.setCompletedAt(LocalDateTime.now());

    // 2. 최종 메시지 저장
    ChatMessage finalMessage = ChatMessage.builder()
        .threadId(threadId)
        .content("여행 계획이 완성되었습니다!")
        .plan(plan)
        .build();
    chatMessageRepository.save(finalMessage);
}
```

---

## 🔄 전체 흐름 요약

### 요청 처리 흐름
```
1. UnifiedChatController [Chat2]
   → 2. MainLLMOrchestrator [Chat2]
   → 3. IntentClassifier [Chat2]
   → 4. Function 선택 및 실행
   → 5. PhaseManager [Chat2] Phase 전환
   → 6. ChatResponse [Trip] 반환
```

### Phase별 주요 파일

| Phase | 주요 파일 | 담당자 |
|-------|----------|--------|
| **INITIALIZATION** | MainLLMOrchestrator, IntentClassifier | Chat2 |
| **INFORMATION_COLLECTION** | ShowQuickInputForm, SubmitTravelForm, Follow-up Functions | User |
| **이미지 처리** | ProcessImage, ProcessOCR, Extract Functions | Media |
| **PLAN_GENERATION** | GenerateTravelPlan, Search Functions | Trip |
| **FEEDBACK_REFINEMENT** | ModifyTravelPlan | Trip |
| **일반 대화** | HandleGeneralQuestion, ProvideGeneralInfo | Chat |
| **COMPLETION** | ChatThreadService | Chat |

---

## 📊 파일 사용 매트릭스

### Intent → Function 매핑

| Intent | 실행되는 Function | 담당자 |
|--------|------------------|--------|
| TRAVEL_PLANNING | ShowQuickInputForm, AnalyzeUserInput, GenerateTravelPlan | User, Trip |
| INFORMATION_COLLECTION | AnalyzeUserInput, StartFollowUp, ContinueFollowUp | User |
| IMAGE_UPLOAD | ProcessImage, ProcessOCR, ExtractFlightInfo, ExtractHotelInfo | Media |
| GENERAL_QUESTION | HandleGeneralQuestion, ProvideGeneralInfo | Chat |
| QUICK_INPUT | ShowQuickInputForm, SubmitTravelForm | User |
| DESTINATION_SEARCH | SearchDestinations, SearchWithPerplexity, SearchTourAPI | Trip |
| RESERVATION_PROCESSING | ProcessOCR, ExtractFlightInfo, ExtractHotelInfo | Media |
| API_USAGE_CHECK | (별도 구현 필요) | - |
| UNKNOWN | HandleUnknown | Chat |

### 데이터 모델 사용처

| 모델 | 사용처 | 담당자 |
|------|--------|--------|
| ChatRequest | 모든 요청의 시작점 | Trip |
| ChatResponse | 모든 응답의 종료점 | Trip |
| TravelPlanRequest | 여행 계획 생성 시 | Trip |
| TravelPlanResponse | 여행 계획 응답 | Trip |
| QuickInputFormDto | 빠른 입력 폼 | User |
| TravelFormSubmitRequest | 폼 제출 데이터 | User |
| FlightReservation | 항공권 정보 | Media |
| HotelReservation | 호텔 정보 | Media |
| TravelContext | 대화 컨텍스트 | Chat2 |

### 외부 클라이언트 사용처

| 클라이언트 | 사용처 | 담당자 |
|------------|--------|--------|
| S3Client.java | 이미지 업로드 (ProcessImageFunction에서 사용) | Media |
| OCRClient.java | OCR 처리 (ProcessOCRFunction에서 사용) | Media |
| PerplexityClient.java | 실시간 검색 (SearchWithPerplexityFunction에서 사용) | Trip |

---

## 🎯 핵심 포인트

1. **단일 진입점**: 모든 요청은 `UnifiedChatController`를 통해 들어옴
2. **중앙 제어**: `MainLLMOrchestrator`가 전체 워크플로우 제어
3. **Function 기반**: 모든 기능이 Function으로 모듈화되어 있음
4. **Phase 관리**: 5단계 Phase로 대화 흐름 체계화
5. **담당자 명확**: 각 파일마다 담당 개발자가 지정됨

---

## ✅ 39개 파일 체크리스트

### Chat2 개발자 (8개)
- [x] MainLLMOrchestrator.java
- [x] IntentClassifier.java
- [x] PhaseManager.java
- [x] UnifiedChatController.java
- [x] Intent.java
- [x] TravelPhase.java
- [x] FunctionConfiguration.java
- [x] TravelContext.java

### User 개발자 (8개)
- [x] ShowQuickInputFormFunction.java
- [x] SubmitTravelFormFunction.java
- [x] AnalyzeUserInputFunction.java
- [x] StartFollowUpFunction.java
- [x] ContinueFollowUpFunction.java
- [x] RecommendDestinationsFunction.java
- [x] QuickInputFormDto.java
- [x] TravelFormSubmitRequest.java

### Chat 개발자 (5개)
- [x] HandleGeneralQuestionFunction.java
- [x] ProvideGeneralInfoFunction.java
- [x] HandleUnknownFunction.java
- [x] ChatThreadService.java
- [x] AuthController.java

### Media 개발자 (8개)
- [x] ProcessImageFunction.java
- [x] ProcessOCRFunction.java
- [x] ExtractFlightInfoFunction.java
- [x] ExtractHotelInfoFunction.java
- [x] S3Client.java
- [x] OCRClient.java
- [x] FlightReservation.java
- [x] HotelReservation.java

### Trip 개발자 (10개)
- [x] GenerateTravelPlanFunction.java
- [x] SearchDestinationsFunction.java
- [x] SearchWithPerplexityFunction.java
- [x] SearchTourAPIFunction.java
- [x] ModifyTravelPlanFunction.java
- [x] PerplexityClient.java
- [x] ChatRequest.java
- [x] TravelPlanRequest.java
- [x] ChatResponse.java
- [x] TravelPlanResponse.java

**총 39개 파일 모두 워크플로우에 매핑 완료** ✅

---

작성일: 2024-12-30
버전: 1.0.0