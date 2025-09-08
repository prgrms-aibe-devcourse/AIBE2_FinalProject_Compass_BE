# 📋 팀원별 개발 계획서 (V3 - 통합)

## 👥 팀 구성 (5명)
- **USER**: 인증/인가, 사용자 프로필 관리
- **TRIP**: 여행 계획 기본 기능, Tour API 통합
- **CHAT1**: 채팅 기본 기능, 인텐트 라우팅
- **CHAT2**: LLM 통합, 꼬리질문 시스템
- **MEDIA**: 이미지 업로드, S3 저장, OCR 처리

---

## 🚀 WEEK 1 - MVP

### 🔐 USER 도메인
| 요구사항ID | 기능명 | 설명 | 우선순위 |
|------------|--------|------|---------|
| REQ-SYS-001 | Spring Boot 초기 설정 | PostgreSQL 15 + JPA + HikariCP 연결 풀 설정, application.yml 환경별 프로파일 구성 | 1 |
| REQ-SYS-002 | 핵심 테이블 생성 | users(id, email, password, name), chat_threads(id, user_id, title), messages(id, thread_id, content, role) 테이블 DDL | 1 |
| REQ-AUTH-001 | 회원가입 API | POST /api/auth/signup - 이메일 중복 검증, BCrypt 암호화, 유효성 검사(이메일 형식, 비밀번호 8자 이상) | 2 |
| REQ-AUTH-002 | 로그인 API | POST /api/auth/login - 이메일/비밀번호 검증, Access Token(1시간) + Refresh Token(7일) 발급 | 2 |
| REQ-AUTH-003 | JWT 인증 필터 | Spring Security OncePerRequestFilter 구현, Bearer 토큰 검증, SecurityContext 설정 | 3 |
| REQ-AUTH-004 | 로그아웃 API | POST /api/auth/logout - Refresh Token 무효화, Redis 블랙리스트 등록 | 3 |

### 🗺️ TRIP 도메인
| 요구사항ID | 기능명 | 설명 | 우선순위 |
|------------|--------|------|---------|
| REQ-TRIP-000 | Trip 테이블 설계 | trips(id, user_id, destination, start_date, end_date), trip_details(day, places JSONB, activities JSONB) | 1 |
| REQ-TRIP-001 | 여행 계획 생성 API | POST /api/trips - 목적지, 날짜, 인원, 예산 입력받아 기본 일정 생성 | 1 |
| REQ-TRIP-002 | 여행 계획 조회 API | GET /api/trips/{id} - 일자별 장소, 이동경로, 예상비용 포함 상세 일정 반환 | 2 |
| REQ-PREF-001 | 여행 스타일 설정 | 휴양형(호텔/리조트), 관광형(명소/박물관), 액티비티형(스포츠/체험) 가중치 설정 | 2 |
| REQ-PREF-002 | 예산 수준 설정 | BUDGET(10만원/일), STANDARD(20만원/일), LUXURY(50만원+/일) 기준 설정 | 3 |

### 💬 CHAT1 도메인
| 요구사항ID | 기능명 | 설명 | 우선순위 |
|------------|--------|------|---------|
| REQ-CHAT-001 | 채팅방 생성 API | POST /api/chat/threads - 제목, 첫 메시지로 새 대화 스레드 생성, UUID 반환 | 1 |
| REQ-CHAT-002 | 채팅 목록 조회 | GET /api/chat/threads - 페이징(20개), 최근 메시지 미리보기, 시간순 정렬 | 2 |
| REQ-CHAT-003 | 메시지 전송 API | POST /api/chat/threads/{id}/messages - role(user/assistant), content 저장, 타임스탬프 자동 기록 | 1 |
| REQ-CHAT-004 | 대화 조회 API | GET /api/chat/threads/{id}/messages - 페이징(50개), 시간순 정렬, 읽음 상태 업데이트 | 2 |
| REQ-CHAT-006 | 메시지 입력 검증 | 최대 1000자, XSS 방지 sanitization, 욕설 필터링, 빈 메시지 차단 | 3 |

### 🤖 CHAT2 도메인
| 요구사항ID | 기능명 | 설명 | 우선순위 |
|------------|--------|------|---------|
| REQ-LLM-001 | Spring AI 설정 | spring-ai-openai, spring-ai-vertex-ai-gemini, spring-ai-redis-store dependency 추가 및 BOM 설정 | 1 |
| REQ-LLM-002 | Gemini 연동 | Vertex AI Gemini 2.0 Flash 모델 연결, API 키 설정, 토큰 제한(8192) 구성 | 1 |
| REQ-AI-003 | 기본 일정 템플릿 | DayTripTemplate, OneNightTemplate, TwoNightTemplate, ThreeNightTemplate 클래스 구현 | 1 |
| REQ-PROMPT-001 | 프롬프트 엔지니어링 | PromptEngineeringService - 컨텍스트 주입, 변수 치환, 응답 포맷 지정 | 1 |
| REQ-PROMPT-002 | 키워드 감지 시스템 | SimpleKeywordDetector - HashMap 기반 키워드→템플릿 매핑, 가중치 스코어링 | 1 |
| REQ-PROMPT-003 | 템플릿 라이브러리 | TravelPlanning, BudgetOptimization, LocalExperience 등 20개 이상 템플릿 클래스 | 2 |
| REQ-LLM-004 | 개인화 컨텍스트 | UserContext(선호도, 이력), TravelHistory(과거 여행) 모델로 프롬프트 enrichment | 2 |
| REQ-LLM-006 | 대화 컨텍스트 관리 | ConversationContext - 최근 10개 메시지 슬라이딩 윈도우, 2000토큰 제한 | 2 |
| REQ-PERS-007 | 콜드 스타트 해결 | UserOnboardingService - 초기 5개 질문으로 선호도 수집, 프로필 자동 생성 | 3 |
| REQ-MON-001 | API 호출 로깅 | Logback + @Slf4j - 요청/응답 시간, 토큰 사용량, 에러율 기록 | 3 |
| REQ-MON-002 | 에러 로깅 | GlobalExceptionHandler - LLM 타임아웃, 토큰 초과, API 오류 처리 | 3 |

---

## 🔄 WEEK 2 - 핵심 기능 개발

### 🔐 USER 도메인
| 요구사항ID | 기능명 | 설명 | 우선순위 |
|------------|--------|------|---------|
| REQ-USER-002 | 프로필 조회 API | GET /api/users/profile - JWT로 사용자 식별, 개인정보 + 선호도 반환 | 1 |
| REQ-USER-003 | 프로필 수정 API | PUT /api/users/profile - 이름, 연령대, 여행 스타일 업데이트 | 1 |
| REQ-USER-004 | 여행 선호도 저장 | user_preferences 테이블 - 휴양(30%), 관광(40%), 액티비티(30%) 가중치 저장 | 2 |
| REQ-USER-005 | 예산 레벨 설정 | BUDGET(<10만원), STANDARD(10-30만원), LUXURY(30만원+) 일일 예산 | 2 |
| REQ-USER-006 | 선호도 분석 | PreferenceAnalyzer - 최근 10개 여행 기록 기반 ML 클러스터링, 타입 자동 분류 | 2 |
| REQ-USER-007 | 피드백 수집 | POST /api/users/feedback - 5점 만족도, 개선사항 텍스트, 재방문 의향 | 3 |

### 🗺️ TRIP 도메인
| 요구사항ID | 기능명 | 설명 | 우선순위 |
|------------|--------|------|---------|
| **크롤링 시스템** |
| REQ-CRAWL-001 | Tour API 클라이언트 | 한국관광공사 API 연동 | 1 |
| REQ-CRAWL-002 | Phase별 크롤링 | 서울→부산→제주 순차 크롤링 | 1 |
| REQ-CRAWL-003 | tour_places 테이블 | id, name, address, category[], keywords[], images[], pet_friendly, details JSONB | 1 |
| REQ-CRAWL-004 | 크롤링 스케줄러 | 6시간마다 자동 실행 | 2 |
| **검색 시스템** |
| REQ-SEARCH-001 | RDS 검색 | PostgreSQL 전문검색 (1순위) | 1 |
| REQ-SEARCH-002 | Tour API 검색 | 실시간 API 호출 (2순위) | 1 |
| REQ-SEARCH-003 | Kakao Map API | 폴백 검색 (3순위) | 2 |
| REQ-SEARCH-004 | 통합 검색 서비스 | 3단계 검색 우선순위 | 1 |

### 💬 CHAT1 도메인
| 요구사항ID | 기능명 | 설명 | 우선순위 |
|------------|--------|------|---------|
| REQ-INTENT-001 | 키워드 분류기 | 여행/추천/일반 3가지 분류 | 1 |
| REQ-INTENT-002 | 라우팅 처리 | 의도별 서비스 호출 | 1 |
| REQ-INTENT-003 | 키워드 사전 관리 | HashMap 기반 10-15개 키워드 | 2 |
| REQ-INTENT-004 | 로깅 시스템 | 분류 결과 통계 수집 | 3 |

### 🤖 CHAT2 도메인
| 요구사항ID | 기능명 | 설명 | 우선순위 |
|------------|--------|------|---------|
| REQ-FOLLOW-001 | 질문 플로우 엔진 | 5개 필수 질문 순차 진행 | 1 |
| REQ-FOLLOW-002 | 정보 수집 정의 | 목적지/날짜/기간/동행/예산 | 1 |
| REQ-FOLLOW-003 | 답변 파싱 | LLM 활용 자연어 처리 | 1 |
| REQ-FOLLOW-004 | Redis 저장 | TravelContext 30분 TTL | 1 |
| REQ-FOLLOW-005 | 완성도 검증 | 필수 필드 입력 체크 | 2 |
| REQ-FOLLOW-006 | 재질문 로직 | 파싱 실패시 다른 표현 | 3 |

### 🖼️ MEDIA 도메인
| 요구사항ID | 기능명 | 설명 | 우선순위 |
|------------|--------|------|---------|
| REQ-MEDIA-001 | 파일 업로드 설정 | MultipartFile, 10MB 제한 | 1 |
| REQ-MEDIA-002 | S3 연동 설정 | AWS SDK, 버킷 설정 | 1 |
| REQ-MEDIA-003 | 이미지 업로드 API | POST /api/media/upload | 2 |
| REQ-MEDIA-004 | 이미지 조회 API | GET /api/media/{id} | 2 |
| REQ-MEDIA-005 | 파일 유효성 검증 | 이미지 포맷/크기 검증 | 3 |
| REQ-MEDIA-006 | OCR 텍스트 추출 | Google Vision API 연동 | 2 |
| REQ-MEDIA-007 | 썸네일 생성 | 300x300 WebP 포맷 | 2 |

---

## 🎯 WEEK 3 - 통합 및 최적화

### 🔐 USER 도메인
| 요구사항ID | 기능명 | 설명 | 우선순위 |
|------------|--------|------|---------|
| REQ-USER-008 | JMeter 부하 테스트 | 시나리오: 회원가입→로그인→채팅→여행계획, 1000 VU, Ramp-up 60초, Duration 10분 | 1 |
| REQ-USER-009 | k6 성능 테스트 | JavaScript 기반 테스트 스크립트, Cloud 실행, 실시간 메트릭, Grafana 대시보드 연동 | 1 |
| REQ-USER-010 | 부하 테스트 자동화 | CI/CD 파이프라인 통합, 성능 회귀 테스트, 임계값 알림, 일일 리포트 생성 | 2 |

### 🗺️ TRIP 도메인  
| 요구사항ID | 기능명 | 설명 | 우선순위 |
|------------|--------|------|---------|
| REQ-TRIP-011 | 여행 계획 저장 | POST /api/trips/{id}/save - 버전관리, 공유 URL 생성, PDF 내보내기 | 1 |
| REQ-TRIP-012 | 캐싱 최적화 | Redis Cache - 인기 장소 30분 TTL, 검색결과 10분 TTL, LRU 정책 | 1 |
| REQ-TRIP-013 | 경로 최적화 | RouteOptimizer - Google Directions API, TSP 알고리즘, 이동시간 최소화 | 2 |
| REQ-TRIP-014 | 시간 배분 | TimeAllocator - 장소별 평균 체류시간, 이동시간 고려, 식사시간 할당 | 2 |

### 💬 CHAT1 도메인
| 요구사항ID | 기능명 | 설명 | 우선순위 |
|------------|--------|------|---------|
| REQ-CHAT-011 | 응답 포맷팅 | ResponseFormatter - Markdown, 이모지, 구조화된 JSON, 매크로 버튼 | 1 |
| REQ-CHAT-012 | 에러 메시지 | UserFriendlyError - 오류코드별 메시지, 해결방법 제시, 재시도 버튼 | 2 |
| REQ-CHAT-013 | 병렬 처리 | CompletableFuture - LLM 호출, DB 조회, API 호출 병렬화, 40% 성능 향상 | 2 |
| REQ-CHAT-014 | API 문서화 | SpringDoc OpenAPI 3.0 - 자동 스키마 생성, 예제 요청/응답, Try it out | 1 |

### 🤖 CHAT2 도메인
| 요구사항ID | 기능명 | 설명 | 우선순위 |
|------------|--------|------|---------|
| REQ-CHAT2-011 | Gemini 최적화 | 프롬프트 체인, 스트리밍 응답, 컨텍스트 압축, 토큰 절약 30% | 1 |
| REQ-CHAT2-012 | 응답 시간 단축 | 스트리밍 SSE, 청크 단위 전송, 초기 응답 <1초, 전체 <3초 | 1 |
| REQ-CHAT2-013 | 토큰 사용량 추적 | TokenUsageMonitor - 요청별 토큰, 일/월 집계, 비용 알림($100 초과시) | 2 |
| REQ-CHAT2-014 | 폴백 처리 | FallbackStrategy - Gemini→GPT-4 자동 전환, 캐시 응답, 오프라인 모드 | 2 |

### 🖼️ MEDIA 도메인
| 요구사항ID | 기능명 | 설명 | 우선순위 |
|------------|--------|------|---------|
| REQ-MEDIA-008 | 배치 업로드 | MultipartBatch - 최대 10개 파일, 병렬 S3 업로드, 진행률 WebSocket | 3 |
| REQ-MEDIA-009 | 이미지 삭제 | DELETE /api/media/{id} - S3 삭제, DB soft delete, 썸네일 정리 | 3 |
| REQ-MEDIA-010 | OCR 정확도 개선 | ImagePreprocessor - 노이즈 제거, 대비 향상, 기울기 보정, 95%→98% | 2 |
| REQ-MEDIA-011 | 다국어 OCR | Google Vision LANGUAGE_HINTS - ko, en, zh, ja 지원, 자동 언어 감지 | 3 |
| REQ-MEDIA-012 | 중복 이미지 감지 | DuplicateDetector - SHA-256 해시, perceptual hash, 95% 이상 유사도 | 3 |

### 🔗 통합 작업 (전체 팀)
| 요구사항ID | 기능명 | 설명 | 담당 |
|------------|--------|------|------|
| REQ-INTEG-001 | 전체 플로우 통합 | 인텐트 라우터 → 꼬리질문 엔진 → 통합 검색 → Gemini 생성 → 응답 포맷팅 | 전체 |
| REQ-INTEG-002 | 통합 테스트 | Postman/Newman E2E - 회원가입 → 로그인 → 채팅 → 여행계획 시나리오 | 전체 |
| REQ-INTEG-003 | 성능 측정 | Prometheus + Grafana - p95 응답시간 <500ms, TPS >100, CPU <70% | USER |
| REQ-INTEG-004 | 배포 준비 | Docker Compose, GitHub Actions CI/CD, AWS EB 배포 스크립트 | USER |

---

## 💡 핵심 통합 플로우

### WEEK 2 플로우
```
[사용자 입력]
    ↓
[CHAT1: 인텐트 라우터]
    ↓
[CHAT2: 꼬리질문 시스템] → Redis 저장
    ↓
[TRIP: 통합 검색]
  1. RDS 검색
  2. Tour API 검색
  3. Kakao Map API
    ↓
[CHAT2: 응답 생성]
```

### WEEK 3 플로우
```
[WEEK 2 완성]
    ↓
[CHAT2: Gemini 최적화]
    ↓
[TRIP: 여행 계획 저장]
    ↓
[CHAT1: 응답 포맷팅]
    ↓
[전체: 통합 테스트]
```

---

## 📊 작업량 요약

### WEEK 1
| 도메인 | 작업 개수 | 핵심 역할 |
|--------|----------|-----------|
| USER | 6개 | 인증 시스템 |
| TRIP | 5개 | 여행 계획 기본 |
| CHAT1 | 5개 | 채팅 CRUD |
| CHAT2 | 11개 | LLM 통합 |

### WEEK 2
| 도메인 | 작업 개수 | 핵심 역할 |
|--------|----------|-----------|
| USER | 6개 | 프로필 관리, 선호도 분석 |
| TRIP | 8개 | 크롤링 + 검색 |
| CHAT1 | 4개 | 인텐트 라우팅 |
| CHAT2 | 6개 | 꼬리질문 시스템 |
| MEDIA | 7개 | 이미지 업로드 + OCR |

### WEEK 3
| 도메인 | 작업 개수 | 핵심 역할 |
|--------|----------|-----------|
| USER | 3개 | JMeter/k6 부하테스트, 성능 자동화 |
| TRIP | 4개 | 저장/최적화 |
| CHAT1 | 4개 | 포맷팅/문서화 |
| CHAT2 | 4개 | Gemini 최적화 |
| MEDIA | 5개 | OCR 고도화 |
| 통합 | 4개 | 전체 통합 |

---

## 📝 일일 체크리스트

### WEEK 2
- [ ] Tour API 크롤링 진행률 확인
- [ ] 꼬리질문 플로우 테스트
- [ ] 통합 검색 동작 확인
- [ ] Redis 데이터 확인
- [ ] 이미지 업로드 테스트

### WEEK 3
- [ ] 통합 테스트 실행
- [ ] 응답 시간 측정
- [ ] Swagger 문서 업데이트
- [ ] 배포 준비 상태 확인