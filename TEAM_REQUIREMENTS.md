# 📋 단계별 요구사항 - 팀원별 할당 (V2)

## 🎯 개발 단계별 목표

### Week 1 (MVP) - 핵심 채팅 기능
**목표**: 로그인 + 기본 채팅 + Gemini 연동으로 동작하는 최소 기능 제품

### Week 2 (1차 고도화) - Multi-LLM + Lambda MCP
**목표**: 의도별 LLM 라우팅 + Redis 캐싱 + Lambda MCP 구현 (Tour/Weather/Hotel)

### Week 3 (2차 고도화) - 개인화 + 성능
**목표**: 개인화 추천 + 에이전트 패턴 + 성능 최적화

---

## 👥 팀 구성 (5명)
- **USER**: 1명 (인증/인가, 사용자 프로필)
- **CHAT1**: 1명 (채팅 기본, 인텐트 라우팅)
- **CHAT2**: 1명 (LLM 통합, 컨텍스트, 개인화)
- **PLAN1**: 1명 (여행 계획, 개인화 일정)
- **PLAN2**: 1명 (Lambda MCP - Tour/Weather/Hotel API)

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

### 💬 CHAT1 (채팅 기본 + 인텐트 라우팅)

#### MVP (Week 1) - 5개 작업
| 요구사항ID | 요구사항명 | 난이도 | 설명 |
|------------|------------|--------|------|
| REQ-CHAT-001 | 채팅방 생성 API | 낮음 | POST /api/chat/threads |
| REQ-CHAT-002 | 채팅 목록 조회 API | 낮음 | 페이징 처리 포함 |
| REQ-CHAT-003 | 메시지 전송 API | 중간 | DB 저장 + LLM 호출 |
| REQ-CHAT-004 | 대화 조회 API | 낮음 | GET /api/chat/threads/{id}/messages |
| REQ-CHAT-006 | 메시지 입력 검증 | 낮음 | @Valid 어노테이션, 최대 1000자 |

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

#### MVP (Week 1) - 5개 작업
| 요구사항ID | 요구사항명 | 난이도 | 설명 | 상태 |
|------------|------------|--------|------|------|
| REQ-LLM-001 | Spring AI 설정 | 중간 | Spring AI 라이브러리 설정 | ✅ 완료 |
| REQ-LLM-002 | Gemini 연동 | 중간 | Gemini API 연결 | ✅ 완료 |
| REQ-LLM-004 | 프롬프트 템플릿 관리 | 낮음 | resources/prompts/ 파일 관리 | |
| REQ-LLM-006 | 대화 컨텍스트 관리 | 중간 | 최근 10개 메시지 유지 | |
| REQ-PERS-007 | 콜드 스타트 해결 | 높음 | 신규 사용자 온보딩 | |

#### 1차 고도화 (Week 2) - 9개 작업
| 요구사항ID | 요구사항명 | 난이도 | 설명 | 상태 |
|------------|------------|--------|------|------|
| REQ-LLM-003 | OpenAI 연동 | 중간 | GPT-4 모델 연결 | ✅ 완료 |
| REQ-LLM-005 | LLM 라우팅 로직 | 중간 | Spring AI Function Calling | ✅ 완료 |
| REQ-LLM-007 | 토큰 사용량 추적 | 중간 | API 사용량 DB 기록 | |
| REQ-LLM-008 | LLM 폴백 처리 | 중간 | 실패 시 대체 모델 사용 | |
| REQ-CTX-001 | 사용자 프로필 로드 | 중간 | USER 도메인 연동 | |
| REQ-CTX-002 | 대화 컨텍스트 저장 | 중간 | HttpSession 활용 | |
| REQ-CTX-003 | Redis 캐싱 | 중간 | 컨텍스트 캐싱 30분 | |
| REQ-CTX-004 | 컨텍스트 병합 | 중간 | 프로필 + 대화 통합 | |
| REQ-PERS-008 | 암묵적 선호도 수집 | 중간 | 대화 기반 선호도 추출 | |

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
| REQ-PERS-001 | 선호도 벡터 저장 | 중간 | Redis Vector Store | ✅ 완료 |
| REQ-PERS-002 | 키워드 빈도 계산 | 중간 | 가중치 적용 | ✅ 완료 |
| REQ-PERS-003 | RAG 기반 개인화 추천 | 중간 | 3단계 파이프라인 | ✅ 완료 |
| REQ-PERS-004 | 부정 선호 제외 | 중간 | 블랙리스트 관리 | |
| REQ-PERS-005 | 추천 카드 표시 | 낮음 | 시작 화면 추천 | |
| REQ-PERS-006 | 추천 피드백 | 중간 | 좋아요/싫어요 수집 | |
| REQ-PERS-009 | Perplexity API 통합 | 높음 | Spring AI 내부 직접 통합 | |

---

### ✈️ PLAN1 (여행 계획 도메인)

#### MVP (Week 1)
*MVP 단계에서는 TRIP 도메인 작업 없음*

#### 1차 고도화 (Week 2) - 3개 작업
| 요구사항ID | 요구사항명 | 난이도 | 설명 |
|------------|------------|--------|------|
| REQ-TRIP-001 | 여행 계획 생성 API | 중간 | POST /api/trips |
| REQ-TRIP-002 | 여행 계획 조회 API | 낮음 | GET /api/trips/{id} |
| REQ-TRIP-003 | 내 여행 목록 조회 | 낮음 | 페이징 처리 포함 |

#### 2차 고도화 (Week 3) - 15개 작업
| 요구사항ID | 요구사항명 | 난이도 | 설명 | 상태 |
|------------|------------|--------|------|------|
| REQ-TRIP-004 | 여행 계획 수정 API | 중간 | 버전 관리 포함 | |
| REQ-TRIP-005 | 여행 계획 삭제 API | 낮음 | Soft delete | |
| REQ-TRIP-006 | 일정별 상세 정보 | 중간 | JSONB 구조 | |
| REQ-TRIP-007 | 여행 공유 기능 | 중간 | UUID 공유 링크 | |
| REQ-TRIP-008 | 여행 복사 기능 | 중간 | 템플릿 활용 | |
| REQ-TRIP-009 | 여행 상태 관리 | 낮음 | 상태 업데이트 | |
| REQ-TRIP-010 | 여행 체크리스트 | 중간 | 준비물 관리 | |
| REQ-TRIP-011 | 개인화 일정 생성 | 높음 | Spring AI RAG | ✅ 완료 |
| REQ-TRIP-012 | 선호 활동 매칭 | 중간 | 시간대별 최적화 | ✅ 완료 |
| REQ-TRIP-013 | 예산 기반 최적화 | 중간 | 예산별 조정 | |
| REQ-TRIP-016 | 시간대별 선호 반영 | 중간 | 아침/저녁형 반영 | |
| REQ-TRIP-021 | 음식 선호 반영 | 중간 | 맛집 추천 | |
| REQ-TRIP-024 | 계절별 최적화 | 중간 | 시즌 활동 | |
| REQ-TRIP-029 | 꼬리질문 생성 | 중간 | 정보 수집 | |
| REQ-TRIP-030 | 개인화 템플릿 | 중간 | 유형별 템플릿 | |

---

### 🌐 PLAN2 (Lambda MCP - Tour/Weather/Hotel API)

#### MVP (Week 1) - 3개 작업 (기본 로깅)
| 요구사항ID | 요구사항명 | 난이도 | 설명 |
|------------|------------|--------|------|
| REQ-MONITOR-001 | API 호출 로깅 | 낮음 | Logback 설정 |
| REQ-MONITOR-002 | 에러 로깅 | 낮음 | 예외 로깅 |
| REQ-MONITOR-003 | 헬스 체크 API | 낮음 | GET /api/health |

#### 1차 고도화 (Week 2) - 6개 작업 (Lambda MCP 구현)
| 요구사항ID | 요구사항명 | 난이도 | 설명 |
|------------|------------|--------|------|
| REQ-MCP-001 | Lambda 프로젝트 설정 | 높음 | Serverless Framework 설정 |
| REQ-MCP-002 | Tour API MCP 구현 | 높음 | 관광지/음식점/숙박 검색 (5개 함수) |
| REQ-MCP-003 | Weather API MCP 구현 | 중간 | 날씨 정보 조회 (3개 함수) |
| REQ-MCP-004 | Hotel API MCP 구현 | 높음 | 호텔 검색/예약 (4개 함수) |
| REQ-MCP-005 | DynamoDB 캐싱 설정 | 중간 | TTL 기반 캐싱 |
| REQ-MCP-006 | Spring AI 통합 | 높음 | Function Calling 연동 |

#### 2차 고도화 (Week 3) - 6개 작업 (Lambda MCP 최적화)
| 요구사항ID | 요구사항명 | 난이도 | 설명 |
|------------|------------|--------|------|
| REQ-MCP-007 | Cold Start 최적화 | 높음 | Provisioned Concurrency |
| REQ-MCP-008 | 에러 핸들링 | 중간 | Exponential Backoff |
| REQ-MCP-009 | CloudWatch 모니터링 | 중간 | 메트릭 및 알람 |
| REQ-MCP-010 | API Gateway 보안 | 높음 | API Key, Rate Limiting |
| REQ-MCP-011 | 병렬 처리 최적화 | 높음 | CompletableFuture 활용 |
| REQ-MCP-012 | 배포 자동화 | 중간 | GitHub Actions 연동 |

---

## 📊 요약 통계

### 팀원별 작업량

| 팀원 | MVP | 1차 고도화 | 2차 고도화 | 총계 |
|------|-----|------------|------------|------|
| USER | 6개 | 6개 | 5개 | **17개** |
| CHAT1 | 5개 | 5개 | 10개 | **20개** |
| CHAT2 | 5개 | 9개 | 14개 | **28개** |
| PLAN1 | 0개 | 3개 | 15개 | **18개** |
| PLAN2 | 3개 | 6개 | 6개 | **15개** |
| **합계** | **19개** | **29개** | **50개** | **98개** |

### 난이도별 분포

| 난이도 | MVP | 1차 고도화 | 2차 고도화 | 총계 |
|--------|-----|------------|------------|------|
| 낮음 | 11개 | 7개 | 6개 | 24개 |
| 중간 | 7개 | 16개 | 30개 | 53개 |
| 높음 | 1개 | 6개 | 14개 | 21개 |

### 주요 완료 항목 (✅)
- Spring AI 설정 및 Gemini/OpenAI 연동
- Spring AI Function Calling (17개 함수)
- Redis Vector Store 개인화 시스템
- 3단계 개인화 파이프라인
- 개인화 일정 생성 알고리즘

### 핵심 구현 예정
- **Lambda MCP**: Tour(5), Weather(3), Hotel(4) 함수
- **Perplexity API**: Spring AI 내부 통합 (5개 함수)
- **Multi-Agent System**: 라우터, 플래너, 추천 에이전트

## 🏗️ 하이브리드 MCP 아키텍처

### 외부 API (Lambda MCP)
- **Tour API**: 관광지, 맛집, 숙박 검색
- **Weather API**: 날씨 정보, 예보, 경보
- **Hotel API**: 호텔 검색, 예약, 리뷰

### 내부 API (Spring AI)
- **Perplexity API**: 트렌딩 여행지, 실시간 정보
- **OpenAI/Gemini**: LLM 대화 및 추론

### 다단계 개인화 파이프라인
1. **Stage 1**: Redis Vector Store (사용자 선호도)
2. **Stage 2**: Perplexity API (트렌딩 매칭)
3. **Stage 3**: Tour API Lambda MCP (상세 정보)