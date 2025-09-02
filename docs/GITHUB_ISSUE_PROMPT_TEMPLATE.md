---
name: 기능 개발
about: 기능명세서 기반 개발 작업을 위한 이슈 템플릿
title: '[CHAT] REQ-LLM-004 | 프롬프트 템플릿 시스템 구현'
labels: '백엔드'
assignees: 'CHAT2팀'
---

## 📋 기능 개요
**요구사항 ID**: REQ-LLM-004

여행 계획 생성을 위한 프롬프트 템플릿 시스템 구현. 
다양한 여행 시나리오에 맞는 구조화된 프롬프트 템플릿을 제공하여 일관성 있고 품질 높은 AI 응답을 생성합니다.

## 🎯 개발 목표
- 여행 계획 생성용 프롬프트 템플릿 시스템 구축
- 템플릿 변수 치환 및 동적 프롬프트 생성 기능
- 여행 유형별 (국내/해외, 가족/커플/혼자) 맞춤형 템플릿 제공
- 프롬프트 버전 관리 및 A/B 테스트 지원

## 📝 기능 명세
### API Endpoints
- [ ] `GET /api/chat/prompts/templates` - 사용 가능한 템플릿 목록 조회
- [ ] `GET /api/chat/prompts/templates/{type}` - 특정 유형의 템플릿 조회
- [ ] `POST /api/chat/prompts/generate` - 템플릿 기반 프롬프트 생성

### 요청/응답 형식
```json
// Request - POST /api/chat/prompts/generate
{
  "templateType": "TRAVEL_PLAN",
  "variables": {
    "destination": "제주도",
    "duration": "3박 4일",
    "travelStyle": "휴양",
    "budget": "100만원",
    "travelers": "커플",
    "season": "여름"
  }
}

// Response
{
  "prompt": "당신은 전문 여행 플래너입니다. 다음 조건에 맞는 제주도 3박 4일 여행 계획을 작성해주세요:\n- 여행 스타일: 휴양\n- 예산: 100만원\n- 여행자: 커플\n- 계절: 여름\n\n일정은 일자별로 구체적인 시간과 장소를 포함하여 작성하고, 맛집과 숙소 추천도 포함해주세요.",
  "templateId": "TRAVEL_PLAN_V1",
  "generatedAt": "2025-01-02T10:00:00Z"
}
```

## 🔧 구현 사항
### Entity
- [ ] PromptTemplate Entity 클래스 생성
- [ ] PromptVariable Entity 클래스 생성
- [ ] 템플릿-변수 관계 설정

### Repository
- [ ] PromptTemplateRepository 인터페이스 생성
- [ ] 템플릿 타입별 조회 쿼리 구현
- [ ] 활성 템플릿 조회 메서드 구현

### Service
- [ ] PromptTemplateService 클래스 생성
- [ ] 템플릿 변수 치환 로직 구현
- [ ] 프롬프트 생성 및 검증 로직 구현
- [ ] 템플릿 캐싱 메커니즘 구현

### Controller
- [ ] PromptTemplateController 클래스 생성
- [ ] 템플릿 조회 API 구현
- [ ] 프롬프트 생성 API 구현
- [ ] PromptGenerateRequest/Response DTO 생성

### Configuration
- [ ] PromptTemplateConfig 클래스 생성
- [ ] 기본 템플릿 초기화 설정
- [ ] 템플릿 캐시 설정
- [ ] application.yml에 템플릿 관련 설정 추가

### Testing
- [ ] PromptTemplateServiceTest 단위 테스트 작성
- [ ] PromptTemplateControllerTest 통합 테스트 작성
- [ ] 템플릿 변수 치환 테스트
- [ ] 다양한 여행 시나리오 테스트

## 🗄️ 데이터베이스 스키마
```sql
-- 프롬프트 템플릿 테이블
CREATE TABLE prompt_templates (
    id BIGSERIAL PRIMARY KEY,
    template_type VARCHAR(50) NOT NULL,
    template_name VARCHAR(100) NOT NULL,
    template_content TEXT NOT NULL,
    variables JSONB,
    version VARCHAR(20) DEFAULT 'V1',
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 프롬프트 사용 로그 테이블
CREATE TABLE prompt_usage_logs (
    id BIGSERIAL PRIMARY KEY,
    template_id BIGINT REFERENCES prompt_templates(id),
    user_id BIGINT,
    generated_prompt TEXT,
    variables_used JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 인덱스 생성
CREATE INDEX idx_prompt_templates_type ON prompt_templates(template_type);
CREATE INDEX idx_prompt_templates_active ON prompt_templates(is_active);
```

## 🔗 관련 이슈
- Related to #CHAT-Domain-Development
- Depends on: REQ-LLM-002 (Gemini 연동 완료)
- 관련 요구사항: REQ-LLM-004 (TEAM_REQUIREMENTS.md Line 181)
- 후속 작업: REQ-LLM-005 (Function Calling 설정)

## ✅ 완료 조건
- [ ] 모든 API 엔드포인트 구현 완료
- [ ] 최소 5개 이상의 여행 템플릿 등록
- [ ] 테스트 코드 작성 및 통과 (커버리지 80% 이상)
- [ ] API 문서 업데이트
- [ ] 코드 리뷰 완료
- [ ] DATABASE_ERD.md 업데이트

## 📌 참고사항

### 기술 스펙
- **프레임워크**: Spring Boot 3.x, Spring AI
- **데이터베이스**: PostgreSQL (JSONB 타입 활용)
- **캐싱**: Spring Cache (Redis 추후 적용 가능)
- **템플릿 엔진**: StringTemplate 또는 Mustache

### 기본 템플릿 예시
```yaml
travel_plan_template:
  system_prompt: |
    당신은 10년 경력의 전문 여행 플래너입니다.
    사용자의 요구사항에 맞춰 구체적이고 실용적인 여행 계획을 제공합니다.
    
  user_prompt_template: |
    다음 조건에 맞는 {{destination}} {{duration}} 여행 계획을 작성해주세요:
    - 여행 스타일: {{travelStyle}}
    - 예산: {{budget}}
    - 여행자: {{travelers}}
    - 계절: {{season}}
    {{#preferences}}
    - 선호사항: {{preferences}}
    {{/preferences}}
    
    다음 내용을 포함해주세요:
    1. 일자별 상세 일정 (시간, 장소, 이동 수단)
    2. 숙소 추천 (위치, 가격대, 특징)
    3. 맛집 추천 (지역별, 메뉴, 예상 비용)
    4. 주요 관광지 정보
    5. 여행 팁과 주의사항
```

### 환경변수 설정 필요
```bash
# 프롬프트 템플릿 설정
PROMPT_TEMPLATE_CACHE_ENABLED=true
PROMPT_TEMPLATE_CACHE_TTL=3600
PROMPT_TEMPLATE_VERSION=V1
```

### 주의사항
1. **템플릿 버전 관리**: 기존 템플릿 수정 시 새 버전으로 생성하여 하위 호환성 유지
2. **변수 검증**: 필수 변수 누락 시 명확한 에러 메시지 제공
3. **성능 최적화**: 자주 사용되는 템플릿은 캐싱하여 DB 부하 감소
4. **다국어 지원**: 향후 다국어 템플릿 지원을 위한 구조 고려
5. **A/B 테스트**: 템플릿 효과 측정을 위한 로깅 구조 포함

### 템플릿 유형
- `TRAVEL_PLAN`: 여행 계획 생성
- `RESTAURANT_RECOMMENDATION`: 맛집 추천
- `ACCOMMODATION_SEARCH`: 숙소 검색
- `ACTIVITY_SUGGESTION`: 액티비티 제안
- `ITINERARY_OPTIMIZATION`: 일정 최적화

### 구현 우선순위
1. **Phase 1**: 기본 여행 계획 템플릿 구현
2. **Phase 2**: 변수 치환 시스템 구현
3. **Phase 3**: 다양한 템플릿 추가
4. **Phase 4**: 캐싱 및 성능 최적화