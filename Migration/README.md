# 🚀 오케스트레이터 마이그레이션 프로젝트

## 📋 개요
분산된 서비스 구조를 **중앙 집중형 LLM 오케스트레이터 패턴**으로 전환하는 프로젝트입니다.

## 🏗️ 핵심 아키텍처
- **단일 진입점**: `/api/chat/unified` 하나로 모든 요청 처리
- **오케스트레이터 패턴**: `MainLLMOrchestrator`가 전체 워크플로우 제어
- **Function Calling**: Spring AI 기반 기능 구현
- **도메인 통합**: auth(독립) + chat(통합) 2개 도메인만 유지

## 📁 디렉토리 구조
```
Migration/
├── README.md                           # 이 파일
├── ORCHESTRATOR_MIGRATION_GUIDE.md    # 전체 마이그레이션 가이드
├── SKELETON_SETUP.md                   # 스켈레톤 프로젝트 셋업 가이드
├── WORKFLOW_VERIFICATION.md            # 워크플로우 준수 검증 보고서
├── 필독.md                             # 프로젝트 주의사항
└── scripts/
    ├── create_skeleton.sh              # 디렉토리 구조 생성
    ├── copy_existing_files.sh          # 기존 파일 복사
    └── create_empty_files.sh           # 스켈레톤 파일 생성 (37개 파일)
```

## 🏃‍♂️ 빠른 시작

### 1. 새 브랜치 생성
```bash
# 현재 브랜치 백업
git checkout main
git pull origin main

# 새로운 빈 브랜치 생성
git checkout --orphan feat/orchestrator-clean

# 모든 파일 제거 (빈 상태로 시작)
git rm -rf .
rm -rf *
```

### 2. 스크립트 실행
```bash
# 1단계: 디렉토리 구조 생성
cd Migration/scripts
chmod +x create_skeleton.sh
./create_skeleton.sh

# 2단계: 기존 파일 복사 (JWT, Entity, Repository 등)
chmod +x copy_existing_files.sh
./copy_existing_files.sh

# 3단계: 스켈레톤 파일 생성 (빈 파일)
chmod +x create_empty_files.sh
./create_empty_files.sh
```

### 3. 커밋 및 푸시
```bash
# 초기 커밋
git add .
git commit -m "feat: 오케스트레이터 스켈레톤 구조 생성

- 디렉토리 구조 생성 완료
- 기존 인증/Entity/Repository 복사 완료
- 새로 작성할 파일 스켈레톤 생성 완료
- 팀 역할 분담 준비 완료"

# 브랜치 푸시
git push origin feat/orchestrator-clean
```

## 👥 팀 역할 분담 (5명)

### Chat2 개발자 - LLM 오케스트레이터 담당
- `orchestrator/MainLLMOrchestrator.java` (최우선)
- `orchestrator/IntentClassifier.java`
- `orchestrator/PhaseManager.java`
- `controller/UnifiedChatController.java`
- `model/enums/Intent.java`, `TravelPhase.java`

### User 개발자 - 빠른입력폼 & 여행스타일 담당
- `function/collection/ShowQuickInputFormFunction.java` (최우선)
- `function/collection/SubmitTravelFormFunction.java`
- `function/collection/AnalyzeUserInputFunction.java`
- `function/collection/StartFollowUpFunction.java`
- `function/collection/ContinueFollowUpFunction.java`
- `function/planning/RecommendDestinationsFunction.java`
- `model/dto/QuickInputFormDto.java`
- `model/request/TravelFormSubmitRequest.java`

### Chat 개발자 - 일반 대화 & CRUD 담당
- `function/processing/HandleGeneralQuestionFunction.java` (최우선)
- `function/processing/ProvideGeneralInfoFunction.java`
- `function/processing/HandleUnknownFunction.java`
- `service/internal/ChatThreadService.java`
- `controller/AuthController.java`

### Media 개발자 - 이미지/OCR 담당
- `function/processing/ProcessImageFunction.java` (최우선)
- `function/processing/ProcessOCRFunction.java`
- `function/processing/ExtractFlightInfoFunction.java`
- `function/processing/ExtractHotelInfoFunction.java`
- `service/external/S3Client.java`
- `service/external/OCRClient.java`
- `model/dto/FlightReservation.java`
- `model/dto/HotelReservation.java`

### Trip 개발자 - 여행계획 생성 담당
- `function/planning/GenerateTravelPlanFunction.java` (최우선)
- `function/planning/SearchDestinationsFunction.java`
- `function/external/SearchWithPerplexityFunction.java`
- `function/external/SearchTourAPIFunction.java`
- `service/external/PerplexityClient.java`
- `model/request/TravelPlanRequest.java`
- `model/response/TravelPlanResponse.java`
- `model/request/ChatRequest.java`
- `model/response/ChatResponse.java`

## 📊 파일 분류

### ✅ 그대로 사용 (복사됨)
- JWT/OAuth 설정 파일
- SecurityConfig
- User, ChatThread, ChatMessage Entity
- Repository 인터페이스
- application.yml
- build.gradle

### 🆕 새로 작성 필요 (스켈레톤 생성됨)
- MainLLMOrchestrator
- IntentClassifier
- PhaseManager
- UnifiedChatController
- 17개 Function 클래스
- 3개 외부 API 클라이언트
- 8개 데이터 모델

## 📅 개발 일정

| Day | 작업 내용 | 담당 |
|-----|---------|------|
| Day 1 | 오케스트레이터 기본 구조 | Chat2 개발자 |
| Day 2 | 입력폼 & CRUD 서비스 | User + Chat 개발자 |
| Day 3 | 이미지 처리 & 여행계획 | Media + Trip 개발자 |
| Day 4 | Function 통합 | 전체 |
| Day 5 | 통합 테스트 | 전체 |

## ⚠️ 주의사항

1. **AI 코드 티 제거**
   - 과도한 주석 금지
   - 너무 완벽한 코드 금지
   - 한국어 주석만 사용 (`//`)

2. **브랜치 관리**
   - 각자 `feat/orchestrator-clean-{name}` 브랜치 생성
   - 매일 메인 브랜치에 머지

3. **커밋 메시지**
   ```
   feat: [담당영역] 구현 내용
   예: feat: 오케스트레이터 Intent 분류 구현
   ```

## 📚 참고 문서
- [오케스트레이터 마이그레이션 가이드](./ORCHESTRATOR_MIGRATION_GUIDE.md)
- [스켈레톤 셋업 가이드](./SKELETON_SETUP.md)
- [워크플로우 상세](../documents/CHAT2_TRAVEL_WORKFLOW_DETAILED.md)
- [워크플로우 준수 검증 보고서](./WORKFLOW_VERIFICATION.md)

## 🆘 문제 발생시

1. 스크립트 실행 권한 문제
   ```bash
   chmod +x scripts/*.sh
   ```

2. 경로 문제
   - 스크립트 내 `PROJECT_ROOT` 변수 확인
   - 실제 프로젝트 경로로 수정

3. 파일 복사 실패
   - 기존 프로젝트 경로 확인
   - `copy_existing_files.sh`의 `CURRENT_PROJECT` 변수 수정

## 📊 생성 파일 통계

- **총 파일 수**: 37개
- **Intent 값**: 9개
- **Phase 값**: 5개
- **Function 클래스**: 17개
- **설정 클래스**: 1개
- **데이터 모델**: 8개
- **오케스트레이터**: 3개
- **컨트롤러**: 2개
- **서비스**: 1개
- **외부 클라이언트**: 3개

✅ **워크플로우 100% 준수 확인 완료**

---

작성일: 2024-12-30
버전: 1.1.0