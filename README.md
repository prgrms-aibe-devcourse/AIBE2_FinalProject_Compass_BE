# 🧭 Compass - LLM 오케스트레이터 스켈레톤

## 📋 프로젝트 개요
Compass 여행 계획 서비스의 LLM 오케스트레이터 패턴 구현을 위한 스켈레톤 프로젝트

## 🏗️ 프로젝트 구조
```
src/main/java/com/compass/
├── config/              # 설정 파일
├── common/              # 공통 모듈
└── domain/
    ├── auth/            # 인증/인가
    └── chat/
        ├── controller/  # API 엔드포인트
        ├── orchestrator/# LLM 오케스트레이터
        ├── function/    # Spring AI Functions
        ├── model/       # 데이터 모델
        ├── entity/      # JPA 엔티티
        ├── repository/  # Repository
        └── service/     # 비즈니스 로직
```

## 👥 팀 역할 분담

### Chat2 개발자 (오케스트레이터)
- MainLLMOrchestrator.java
- IntentClassifier.java
- PhaseManager.java
- UnifiedChatController.java
- Intent.java, TravelPhase.java
- FunctionConfiguration.java
- TravelContext.java

### User 개발자 (정보 수집)
- ShowQuickInputFormFunction.java
- SubmitTravelFormFunction.java
- AnalyzeUserInputFunction.java
- Follow-up Functions

### Trip 개발자 (여행 계획)
- GenerateTravelPlanFunction.java
- SearchWithPerplexityFunction.java
- ChatRequest.java, ChatResponse.java
- TravelPlanRequest/Response.java

### Media 개발자 (이미지 처리)
- ProcessImageFunction.java
- ProcessOCRFunction.java
- S3Client.java
- OCRClient.java

### Chat 개발자 (일반 대화)
- ChatThreadService.java
- AuthController.java
- HandleGeneralQuestionFunction.java

## 🚀 시작하기
```bash
# 의존성 설치
./gradlew build

# 애플리케이션 실행
./gradlew bootRun
```

## 📝 개발 가이드
1. 담당 파일의 TODO 주석 확인
2. 요구사항에 따라 구현
3. 테스트 코드 작성
4. PR 생성

## 🔗 관련 문서
- [요구사항 명세서](Migration/REQUIREMENTS_SPECIFICATION.md)
- [워크플로우 매핑](Migration/WORKFLOW_FILE_MAPPING.md)
- [스켈레톤 셋업 가이드](Migration/SKELETON_SETUP.md)