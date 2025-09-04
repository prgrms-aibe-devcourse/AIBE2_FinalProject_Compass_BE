# ChatInputParser 아키텍처 개선 문서

## 개요
ChatInputParser의 강한 결합 문제를 해결하기 위해 의존성 역전 원칙을 적용한 리팩토링

## 문제점
1. **도메인-인프라 강한 결합**: ChatInputParser가 ChatModel(AI 인프라)에 직접 의존
2. **테스트 어려움**: AI 서비스 없이 테스트 불가능
3. **Spring Context 로드 실패**: 테스트 환경에서 AI 빈 생성 불가

## 해결 방안

### 새로운 아키텍처
```
domain/chat/parser/
├── core/                          # 도메인 핵심
│   └── TripPlanningParser.java   # 순수 도메인 인터페이스
├── impl/                          # 구현체들
│   ├── PatternBasedParser.java   # 패턴 매칭 (AI 없음)
│   ├── AiEnhancedParser.java     # AI 향상 데코레이터
│   └── HybridParser.java         # 전략 선택기
└── config/
    └── ParserConfiguration.java   # 빈 설정
```

### 핵심 설계 원칙
1. **의존성 역전**: 도메인은 추상화(인터페이스)에만 의존
2. **전략 패턴**: 런타임에 파서 전략 선택 가능
3. **데코레이터 패턴**: AI 기능을 선택적 향상으로 추가
4. **프로파일 기반 설정**: 환경별 다른 파서 사용

## 구현 상세

### 1. TripPlanningParser 인터페이스
- 순수 도메인 인터페이스
- 인프라 의존성 없음
- 테스트 가능한 추상화

### 2. PatternBasedParser
- 정규식 패턴 매칭만 사용
- AI 의존성 완전 제거
- 단위 테스트 100% 가능

### 3. AiEnhancedParser
- 데코레이터 패턴 적용
- 기본 파서를 AI로 향상
- 패턴 매칭 실패 시 AI 폴백

### 4. HybridParser
- 입력 특성에 따라 파서 선택
- 설정 기반 전략 변경 가능
- Primary 빈으로 기본 사용

## 테스트 전략

### 단위 테스트
- PatternBasedParser: Mock 없이 직접 테스트
- ChatParsingController: Mock 파서로 테스트

### 통합 테스트
- test 프로파일: PatternBasedParser만 사용
- dev/prod 프로파일: HybridParser 사용

## 마이그레이션 가이드

### 기존 코드
```java
@Autowired
private ChatInputParser chatInputParser;

TripPlanningRequest result = chatInputParser.parseUserInput(input);
```

### 새 코드
```java
@Autowired
private TripPlanningParser tripPlanningParser;

TripPlanningRequest result = tripPlanningParser.parse(input);
```

## 성과
1. ✅ AI 없이 테스트 가능
2. ✅ Spring Context 로드 문제 해결
3. ✅ 도메인-인프라 분리
4. ✅ 유연한 전략 선택
5. ✅ 테스트 커버리지 향상

## 향후 계획
- ChatInputParser 클래스 제거 (deprecated)
- 추가 파싱 전략 구현 가능
- ML 모델 기반 파서 추가 가능