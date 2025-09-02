# 📋 단계별 요구사항 - 팀원별 할당 (V3)

## 🎯 개발 단계별 목표

### Week 1 (MVP) - AI 여행 계획 채팅 서비스
**목표**: 로그인 + 기본 채팅 + Gemini 연동 + **AI 여행 계획 생성** 기능 구현

### Week 2 (1차 고도화) - Multi-LLM + Lambda MCP
**목표**: 의도별 LLM 라우팅 + Redis 캐싱 + Lambda MCP 구현 (Tour/Weather/Hotel)

### Week 3 (2차 고도화) - 개인화 + 에이전트 패턴
**목표**: 개인화 추천 시스템 + Multi-Agent System + 성능 최적화

---

## 👥 팀 구성 (5명)
- **USER**: 인증/인가, 사용자 프로필 관리
- **TRIP1**: 여행 계획 기본 기능, 개인화 알고리즘
- **TRIP2**: AI 여행 계획 생성, Lambda MCP 구현
- **CHAT1**: 채팅 기본 기능, 인텐트 라우팅
- **CHAT2**: LLM 통합, 컨텍스트 관리, 개인화

---

## 📝 팀원별 요구사항 할당

### 🔐 USER (인증 + 사용자 도메인)

#### MVP (Week 1) - 6개 작업
| 요구사항ID | 요구사항명 | 난이도 | 설명 |
|------------|------------|--------|------|
| REQ-AUTH-001 | 회원가입 API | 낮음 | POST /api/auth/signup, BCrypt 암호화 |
| REQ-AUTH-002 | 로그인 API | 중간 | JWT 토큰 발급 (access + refresh) |
| REQ-AUTH-003 | 토큰 검증 필터 | 중간 | Spring Security JWT 필터 구현 |
| REQ-USER-001 | 사용자 프로필 조회 | 낮음 | GET /api/users/profile |
| REQ-USER-003 | 여행 스타일 설정 | 낮음 | 휴양/관광/액티비티 선호도 저장 |
| REQ-USER-004 | 예산 수준 설정 | 낮음 | BUDGET/STANDARD/LUXURY 설정 |

#### 1차 고도화 (Week 2) - 6개 작업
| 요구사항ID | 요구사항명 | 난이도 | 설명 |
|------------|------------|--------|------|
| REQ-AUTH-004 | 토큰 갱신 API | 중간 | Refresh token rotation |
| REQ-AUTH-005 | 로그아웃 API | 낮음 | Redis 블랙리스트 관리 |
| REQ-USER-002 | 프로필 수정 API | 낮음 | PUT /api/users/profile |
| REQ-USER-005 | 관심 카테고리 설정 | 낮음 | 최대 3개 카테고리 선택 |
| REQ-USER-008 | 선호도 조회 API | 낮음 | 채팅에서 활용할 선호도 조회 |
| REQ-USER-009 | 선호도 업데이트 API | 중간 | 채팅 중 파악된 선호도 업데이트 |

#### 2차 고도화 (Week 3) - 5개 작업
| 요구사항ID | 요구사항명 | 난이도 | 설명 |
|------------|------------|--------|------|
| REQ-USER-006 | 비밀번호 변경 | 중간 | 현재 비밀번호 확인 후 변경 |
| REQ-USER-007 | 탈퇴 처리 | 중간 | Soft delete 처리 |
| REQ-USER-010 | 여행 히스토리 저장 | 중간 | Trip 도메인과 연동 |
| REQ-USER-011 | 즐겨찾기 관리 | 낮음 | 여행지 즐겨찾기 |
| REQ-USER-012 | 알림 설정 관리 | 낮음 | 알림 on/off 설정 |

---

### 🗺️ TRIP1 (여행 계획 기본 기능)

#### MVP (Week 1) - 5개 작업
| 요구사항ID | 요구사항명 | 난이도 | 설명 |
|------------|------------|--------|------|
| REQ-TRIP-000 | Trip 테이블 설계 | 낮음 | trips, trip_details 테이블 생성 |
| REQ-TRIP-001 | 여행 계획 생성 API | 중간 | POST /api/trips - AI 기반 여행 계획 생성 |
| REQ-TRIP-002 | 여행 계획 조회 API | 낮음 | GET /api/trips/{id} |
| REQ-USER-003 | 여행 스타일 설정 | 낮음 | 휴양/관광/액티비티 선호도 저장 |
| REQ-USER-004 | 예산 수준 설정 | 낮음 | BUDGET/STANDARD/LUXURY 설정 |

#### 1차 고도화 (Week 2) - 4개 작업
| 요구사항ID | 요구사항명 | 난이도 | 설명 |
|------------|------------|--------|------|
| REQ-TRIP-003 | 내 여행 목록 조회 | 낮음 | GET /api/trips, 페이징 |
| REQ-TRIP-004 | 여행 계획 수정 API | 중간 | PUT /api/trips/{id} |
| REQ-TRIP-014 | 상세 일정 추가 | 중간 | 관광지, 식당, 숙박 상세 정보 |
| REQ-TRIP-015 | 일정 최적화 | 높음 | 이동 경로, 시간 최적화 |

#### 2차 고도화 (Week 3) - 7개 작업
| 요구사항ID | 요구사항명 | 난이도 | 설명 |
|------------|------------|--------|------|
| REQ-TRIP-016 | 사용자 선호도 반영 | 높음 | Spring AI RAG 활용 개인화 |
| REQ-TRIP-017 | 선호 활동 매칭 | 중간 | 시간대별 최적화 |
| REQ-TRIP-013 | 예산 기반 최적화 | 중간 | 예산별 조정 |
| REQ-TRIP-018 | 시간대별 선호 반영 | 중간 | 아침/저녁형 반영 |
| REQ-TRIP-021 | 음식 선호 반영 | 중간 | 맛집 추천 통합 |
| REQ-TRIP-024 | 계절별 최적화 | 중간 | 시즌 활동 추천 |
| REQ-TRIP-029 | 꼬리질문 생성 | 중간 | 정보 수집 질문 |

---

### 🤖 TRIP2 (AI 여행 계획 생성 + Lambda MCP)

#### MVP (Week 1) - 5개 작업
| 요구사항ID | 요구사항명 | 난이도 | 설명 |
|------------|------------|--------|------|
| REQ-AI-001 | AI 여행 계획 Function | 높음 | Spring AI Function Calling 여행 계획 함수 |
| REQ-AI-002 | 사용자 입력 파싱 | 중간 | 목적지, 날짜, 예산, 인원 추출 |
| REQ-AI-003 | 기본 일정 템플릿 | 중간 | 당일치기, 2박 3일, 3박 4일 기본 템플릿 |
| REQ-MON-001 | API 호출 로깅 | 낮음 | Logback 설정, 요청/응답 로깅 |
| REQ-MON-002 | 에러 로깅 | 낮음 | 예외 처리 및 스택 트레이스 로깅 |

#### 1차 고도화 (Week 2) - 6개 작업
| 요구사항ID | 요구사항명 | 난이도 | 설명 |
|------------|------------|--------|------|
| REQ-MCP-001 | Lambda 프로젝트 설정 | 높음 | Serverless Framework 설정 |
| REQ-MCP-002 | Tour API MCP | 높음 | 5개 함수 (관광지/맛집/숙박) |
| REQ-MCP-003 | Weather API MCP | 중간 | 3개 함수 (현재/예보/경보) |
| REQ-MCP-004 | Hotel API MCP | 높음 | 4개 함수 (검색/예약/가격/리뷰) |
| REQ-MCP-005 | DynamoDB 캐싱 | 중간 | TTL 기반 캐싱 설정 |
| REQ-MCP-006 | Spring AI 통합 | 높음 | Function Calling 연동 |

#### 2차 고도화 (Week 3) - 13개 작업

##### 여행 관리 기능 (7개)
| 요구사항ID | 요구사항명 | 난이도 | 설명 |
|------------|------------|--------|------|
| REQ-TRIP-005 | 여행 계획 삭제 API | 낮음 | DELETE /api/trips/{id} |
| REQ-TRIP-006 | 일정별 상세 정보 | 중간 | JSONB 구조 관리 |
| REQ-TRIP-007 | 여행 공유 기능 | 중간 | UUID 공유 링크 |
| REQ-TRIP-008 | 여행 복사 기능 | 중간 | 템플릿 활용 |
| REQ-TRIP-009 | 여행 상태 관리 | 낮음 | 계획/진행/완료 상태 |
| REQ-TRIP-010 | 여행 체크리스트 | 중간 | 준비물 관리 |
| REQ-TRIP-030 | 개인화 템플릿 | 중간 | 유형별 템플릿 |

##### Lambda MCP 최적화 (6개)
| 요구사항ID | 요구사항명 | 난이도 | 설명 |
|------------|------------|--------|------|
| REQ-MCP-007 | Cold Start 최적화 | 높음 | Provisioned Concurrency |
| REQ-MCP-008 | 에러 핸들링 | 중간 | Exponential Backoff |
| REQ-MCP-009 | CloudWatch 모니터링 | 중간 | 메트릭 및 알람 |
| REQ-MCP-010 | API Gateway 보안 | 높음 | API Key, Rate Limiting |
| REQ-MCP-011 | 병렬 처리 최적화 | 높음 | CompletableFuture |
| REQ-MCP-012 | 배포 자동화 | 중간 | GitHub Actions 연동 |

---

### 💬 CHAT1 (채팅 기본 + 인텐트 라우팅)

#### MVP (Week 1) - 5개 작업
| 요구사항ID | 요구사항명 | 난이도 | 설명 |
|------------|------------|--------|------|
| REQ-CHAT-001 | 채팅방 생성 API | 낮음 | POST /api/chat/threads, UUID 생성 |
| REQ-CHAT-002 | 채팅 목록 조회 | 낮음 | GET /api/chat/threads, 페이징 처리 |
| REQ-CHAT-003 | 메시지 전송 API | 중간 | POST /api/chat/threads/{id}/messages |
| REQ-CHAT-004 | 대화 조회 API | 낮음 | GET /api/chat/threads/{id}/messages |
| REQ-CHAT-006 | 메시지 입력 검증 | 낮음 | @Valid, 최대 1000자 제한 |

#### 1차 고도화 (Week 2) - 5개 작업
| 요구사항ID | 요구사항명 | 난이도 | 설명 |
|------------|------------|--------|------|
| REQ-CHAT-005 | 채팅 삭제 API | 낮음 | Soft delete 처리 |
| REQ-CHAT-007 | 채팅 제목 자동 생성 | 중간 | 첫 메시지 기반 제목 생성 |
| REQ-INTENT-001 | 의도 분류 기능 | 중간 | 여행계획/추천/정보 분류 |
| REQ-INTENT-002 | 키워드 매칭 | 낮음 | 키워드 사전 관리 |
| REQ-INTENT-003 | 의도별 프롬프트 | 낮음 | 템플릿 선택 로직 |

#### 2차 고도화 (Week 3) - 10개 작업
| 요구사항ID | 요구사항명 | 난이도 | 설명 |
|------------|------------|--------|------|
| REQ-CHAT-008 | 채팅 제목 수정 API | 낮음 | PUT /api/chat/threads/{id}/title |
| REQ-CHAT-009 | 메시지 검색 기능 | 중간 | 전문 검색 구현 |
| REQ-CHAT-010 | 채팅 내보내기 | 중간 | CSV/JSON 형식 다운로드 |
| REQ-INTENT-004 | 라우터 에이전트 | 높음 | 에이전트 패턴 구현 |
| REQ-INTENT-005 | 라우터 꼬리질문 | 중간 | 의도 명확화 질문 |
| REQ-INTENT-006 | 플래너 에이전트 | 높음 | 여행 계획 전문 에이전트 |
| REQ-INTENT-007 | 플래너 꼬리질문 | 중간 | 세부사항 수집 |
| REQ-INTENT-008 | 추천 에이전트 | 높음 | 개인화 추천 에이전트 |
| REQ-INTENT-009 | 추천 꼬리질문 | 중간 | 선호도 파악 질문 |
| REQ-INTENT-010 | 정보 알리미 에이전트 | 높음 | 날씨/환율 정보 제공 |

---

### 🤖 CHAT2 (LLM 통합 + 컨텍스트 + 개인화)

#### MVP (Week 1) - 6개 작업
| 요구사항ID | 요구사항명 | 난이도 | 설명 | 상태 |
|------------|------------|--------|------|------|
| REQ-LLM-001 | Spring AI 설정 | 중간 | dependency 추가, 기본 설정 | ✅ 완료 |
| REQ-LLM-002 | Gemini 연동 | 중간 | Vertex AI Gemini 2.0 Flash 연결 | ✅ 완료 |
| REQ-LLM-004 | 프롬프트 템플릿 | 낮음 | 여행 계획 프롬프트 포함 | |
| REQ-LLM-005 | Function Calling 설정 | 높음 | 여행 계획 생성 함수 등록 | |
| REQ-LLM-006 | 대화 컨텍스트 관리 | 중간 | 최근 10개 메시지 유지 | |
| REQ-PERS-007 | 콜드 스타트 해결 | 중간 | 신규 사용자 온보딩 메시지 | |

#### 1차 고도화 (Week 2) - 9개 작업
| 요구사항ID | 요구사항명 | 난이도 | 설명 | 상태 |
|------------|------------|--------|------|------|
| REQ-LLM-003 | OpenAI 연동 | 중간 | GPT-4o-mini 모델 연결 | |
| REQ-LLM-007 | 토큰 사용량 추적 | 중간 | API 사용량 DB 기록 | |
| REQ-LLM-008 | LLM 폴백 처리 | 중간 | 실패 시 대체 모델 | |
| REQ-CTX-001 | 사용자 프로필 로드 | 중간 | USER 도메인 연동 | |
| REQ-CTX-002 | 대화 컨텍스트 저장 | 중간 | HttpSession 활용 | |
| REQ-CTX-003 | Redis 캐싱 | 중간 | 컨텍스트 30분 캐싱 | |
| REQ-CTX-004 | 컨텍스트 병합 | 중간 | 프로필 + 대화 통합 | |
| REQ-PERS-008 | 암묵적 선호도 수집 | 높음 | 대화 기반 선호도 추출 | |
| REQ-AI-004 | Lambda MCP 호출 통합 | 높음 | Tour/Weather/Hotel API 호출 | |

#### 2차 고도화 (Week 3) - 14개 작업
| 요구사항ID | 요구사항명 | 난이도 | 설명 | 상태 |
|------------|------------|--------|------|------|
| REQ-LLM-009 | 응답 캐싱 | 중간 | Redis FAQ 캐싱 | |
| REQ-LLM-010 | 컨텍스트 요약 | 중간 | 긴 대화 자동 요약 | |
| REQ-LLM-011 | 이미지 텍스트 추출 | 높음 | OpenAI Vision API OCR | |
| REQ-CTX-005 | 키워드 추출 | 중간 | 여행지/날짜 추출 | |
| REQ-CTX-006 | 선호도 업데이트 | 중간 | 백그라운드 처리 | |
| REQ-CTX-007 | 컨텍스트 요약 | 높음 | 토큰 제한 관리 | |
| REQ-CTX-008 | 개인화 프롬프트 | 중간 | 맞춤형 프롬프트 생성 | |
| REQ-PERS-001 | 선호도 벡터 저장 | 높음 | Redis Vector Store | |
| REQ-PERS-002 | 키워드 빈도 계산 | 중간 | 가중치 적용 | |
| REQ-PERS-003 | RAG 기반 개인화 추천 | 높음 | 3단계 파이프라인 | |
| REQ-PERS-004 | 부정 선호 제외 | 중간 | 블랙리스트 관리 | |
| REQ-PERS-005 | 추천 카드 표시 | 중간 | 시작 화면 추천 | |
| REQ-PERS-006 | 추천 피드백 | 중간 | 좋아요/싫어요 수집 | |
| REQ-PERS-009 | Perplexity API 통합 | 높음 | Spring AI 내부 통합 | |


---

## 📊 요약 통계

### 팀원별 작업량

| 팀원 | MVP | 1차 고도화 | 2차 고도화 | 총계 |
|------|-----|------------|------------|------|
| USER | 6개 | 6개 | 5개 | **17개** |
| TRIP1 | 5개 | 4개 | 7개 | **16개** |
| TRIP2 | 5개 | 6개 | 13개 | **24개** |
| CHAT1 | 5개 | 5개 | 10개 | **20개** |
| CHAT2 | 6개 | 9개 | 14개 | **29개** |
| **합계** | **27개** | **30개** | **49개** | **106개** |

### 난이도별 분포

| 난이도 | MVP | 1차 고도화 | 2차 고도화 | 총계 |
|--------|-----|------------|------------|------|
| 낮음 | 13개 | 8개 | 6개 | 27개 |
| 중간 | 11개 | 14개 | 29개 | 54개 |
| 높음 | 3개 | 8개 | 14개 | 25개 |

### 주요 완료 항목 (✅)
- **Spring AI 설정**: Gemini 2.0 Flash 연동 완료
- **Vertex AI**: us-central1 region 설정 완료
- **Docker 환경**: 전체 시스템 통합 테스트 완료

### MVP 핵심 기능 - AI 여행 계획 생성 Flow

```
사용자 입력 → CHAT2 (LLM 처리) → TRIP2 (입력 파싱/AI 함수)
           → TRIP1 (계획 저장) → 사용자 응답
```

### 주요 구현 예정
- **Week 1 (MVP)**: AI 여행 계획 생성 기능
- **Week 2 (1차)**: Lambda MCP 3개 API 구현
- **Week 3 (2차)**: Multi-Agent System + 개인화

## 🏗️ 기술 스택 및 통합 구조

### LLM Integration (CHAT2 담당)
- **Primary**: Gemini 2.0 Flash (Vertex AI)
- **Secondary**: GPT-4o-mini (OpenAI)
- **Framework**: Spring AI with Function Calling

### Lambda MCP Architecture (TRIP2 담당)
- **Tour API**: 관광지/맛집/숙박 (5개 함수)
- **Weather API**: 날씨/예보/경보 (3개 함수)
- **Hotel API**: 검색/예약/가격/리뷰 (4개 함수)

### 협업 구조
```
CHAT2 (LLM 통합)
  ├── Spring AI 설정
  ├── Gemini/OpenAI 연동
  └── Function Calling 프레임워크
      ↓
TRIP2 (AI Functions)
  ├── createTravelPlan()
  ├── optimizeTravelPlan()
  └── recommendDestinations()
      ↓
TRIP1 (여행 API)
  ├── POST /api/trips
  ├── GET /api/trips/{id}
  └── Trip 도메인 관리
```