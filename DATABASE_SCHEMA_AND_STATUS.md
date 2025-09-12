# 📊 Compass 프로젝트 데이터베이스 스키마 및 현황

## 개요
TRAVEL_RECOMMENDATION_SYSTEM_PLAN_V2_RESTRUCTURED.md 기반으로 필요한 데이터베이스 테이블과 현재 RDS 상태를 정리한 문서입니다.

---

## 📋 전체 테이블 현황

### 요약
- **총 필요 테이블**: 15개
- **현재 존재**: 10개
- **추가 필요**: 5개
- **수정 필요**: 3개

---

## ✅ 이미 존재하는 테이블 (10개)

### 1. users (USER 도메인) ✅
- **상태**: 존재함
- **수정 필요**: ❌
- **주요 컬럼**:
  - `id` (PK)
  - `email` (Unique)
  - `password`
  - `nickname`
  - `profile_image_url`
  - `social_type` (kakao, google, naver)
  - `social_id`
  - `role` (USER, ADMIN)
  - `created_at`, `updated_at`

### 2. chat_threads (CHAT 도메인) ✅
- **상태**: 존재함
- **수정 필요**: ❌
- **주요 컬럼**:
  - `id` (UUID, PK)
  - `user_id` (FK → users)
  - `title`
  - `created_at`, `updated_at`
  - `last_message_at`

### 3. chat_messages (CHAT 도메인) ✅
- **상태**: 존재함
- **수정 필요**: ❌
- **주요 컬럼**:
  - `id` (PK)
  - `thread_id` (FK → chat_threads)
  - `sender_type` (USER, ASSISTANT)
  - `content` (TEXT)
  - `timestamp`
  - `metadata` (JSON: function_calls, attachments 등)

### 4. travel_info_collection_states (CHAT 도메인) ✅
- **상태**: 존재함
- **수정 필요**: ⚠️ 컬럼 추가 필요
- **현재 컬럼**: 기본 정보 수집 관련
- **추가 필요 컬럼**:
  - `departure_time` (TIME) - 출발 시간
  - `return_time` (TIME) - 도착 시간
  - `strategy_type` (VARCHAR) - 적응형 전략 타입
  - `fatigue_score` (INTEGER) - 피로도 점수
  - `question_count` (INTEGER) - 질문 횟수

### 5. trips (TRIP 도메인) ✅
- **상태**: 존재함
- **수정 필요**: ⚠️ thread_id 타입 변경
- **현재**: `Long threadId`
- **필요**: `UUID threadId` with FK to chat_threads
- **주요 컬럼**:
  - `id` (PK)
  - `trip_uuid` (UUID, Unique)
  - `user_id` (FK → users)
  - `thread_id` (FK → chat_threads) ⚠️
  - `title`
  - `destination`
  - `start_date`, `end_date`
  - `number_of_people`
  - `total_budget`
  - `status` (DRAFT, CONFIRMED, COMPLETED, CANCELLED)
  - `trip_metadata` (JSON)
  - `deleted_at` (Soft Delete)

### 6. trip_details (TRIP 도메인) ✅
- **상태**: 존재함
- **수정 필요**: ⚠️ 컬럼 추가/변경
- **추가 필요**:
  - `place_id` (FK to places) - 장소 참조
  - `start_time`, `end_time` - 시간 범위
  - `activity_type` (ENUM) - 활동 유형
- **변경 필요**:
  - `placeName` → 실제 장소 참조로 변경

### 7. user_preferences (TRIP 도메인) ✅
- **상태**: 존재함 (UserPreference 엔티티)
- **수정 필요**: ⚠️ 컬럼 추가
- **추가 필요**:
  - `preferred_food_types` (JSON)
  - `preferred_accommodation_types` (JSON)
  - `special_requirements` (TEXT)

### 8. user_contexts (TRIP 도메인) ✅
- **상태**: 존재함
- **수정 필요**: ❌

### 9. travel_histories (TRIP 도메인) ✅
- **상태**: 존재함
- **수정 필요**: ❌

### 10. media (MEDIA 도메인) ✅
- **상태**: 존재함
- **수정 필요**: ❌

---

## 🆕 추가해야 하는 테이블 (5개)

### 1. places (TRIP 도메인) ❌ 🔴 최우선
- **용도**: 여행지 장소 마스터 데이터
- **우선순위**: 🔴 매우 높음 (없으면 여행 계획 생성 불가)
- **주요 컬럼**:
  - `id` (PK)
  - `place_code` (Unique: Tour API 코드)
  - `name`, `name_en`
  - `category` (ATTRACTION, RESTAURANT, CAFE, HOTEL 등)
  - `sub_category`
  - `destination` (지역: 제주, 부산 등)
  - `address`
  - `latitude`, `longitude`
  - `phone`, `website`
  - `business_hours` (JSON)
  - `price_range` (1-5)
  - `rating`, `review_count`
  - `description` (TEXT)
  - `image_urls` (JSON Array)
  - `tags` (JSON: 해시태그, 키워드)
  - `is_trendy` (Boolean: Perplexity로 찾은 트렌디 장소)
  - `data_source` (TOUR_API, PERPLEXITY, MANUAL)
  - `created_at`, `updated_at`

### 2. follow_up_questions (CHAT 도메인) ❌ 🔴 높음
- **용도**: 생성된 Follow-up 질문 기록
- **우선순위**: 🔴 높음 (CHAT2 팀 핵심 기능)
- **주요 컬럼**:
  - `id` (PK)
  - `thread_id` (FK → chat_threads)
  - `question_text`
  - `question_type` (ORIGIN, DESTINATION, DATES, BUDGET 등)
  - `options` (JSON: 선택지가 있는 경우)
  - `is_answered`
  - `user_response`
  - `created_at`

### 3. trip_days (TRIP 도메인) ❌ 🔴 높음
- **용도**: 일자별 여행 일정
- **우선순위**: 🔴 높음
- **주요 컬럼**:
  - `id` (PK)
  - `trip_id` (FK → trips)
  - `day_number`
  - `date`
  - `start_time`, `end_time`
  - `theme` (오늘의 테마)
  - `created_at`, `updated_at`

### 4. reservations (TRIP 도메인) ❌ 🟡 중간
- **용도**: 항공권, 숙소 등 예약 정보
- **우선순위**: 🟡 중간
- **주요 컬럼**:
  - `id` (PK)
  - `user_id` (FK → users)
  - `trip_id` (FK → trips, Nullable)
  - `type` (FLIGHT, HOTEL, TRAIN, BUS 등)
  - `confirmation_number`
  - `provider`
  - `departure_location`, `arrival_location`
  - `departure_time`, `arrival_time`
  - `check_in_date`, `check_out_date`
  - `price`
  - `reservation_details` (JSON)
  - `ocr_extracted` (Boolean: OCR로 추출됨)
  - `original_image_url`
  - `created_at`, `updated_at`

### 5. place_reviews (TRIP 도메인) ❌ 🟡 중간
- **용도**: 장소 리뷰 (사용자 생성)
- **우선순위**: 🟡 중간
- **주요 컬럼**:
  - `id` (PK)
  - `place_id` (FK → places)
  - `user_id` (FK → users)
  - `trip_id` (FK → trips, Nullable)
  - `rating` (1-5)
  - `review_text` (TEXT)
  - `visit_date`
  - `images` (JSON Array)
  - `created_at`, `updated_at`

### 6. weather_cache (지원) ❌ 🟡 중간
- **용도**: 날씨 정보 캐싱
- **우선순위**: 🟡 중간
- **주요 컬럼**:
  - `id` (PK)
  - `location`
  - `date`
  - `temperature_min`, `temperature_max`
  - `weather_condition`
  - `precipitation_probability`
  - `wind_speed`, `humidity`
  - `cached_at`, `expires_at`

### 7. prompt_templates (지원) ❌ 🟢 낮음
- **용도**: LLM 프롬프트 템플릿 관리
- **우선순위**: 🟢 낮음 (현재 코드로 관리 중)
- **주요 컬럼**:
  - `id` (PK)
  - `template_key` (Unique)
  - `template_name`
  - `template_content` (TEXT)
  - `variables` (JSON)
  - `category`
  - `is_active`
  - `version`

---

## 📋 작업 우선순위

### 🔴 즉시 작업 필요 (높음)
1. **places 테이블 생성** 
   - Tour API 데이터 저장
   - Perplexity 검색 결과 저장
   - 없으면 여행 계획 생성 불가!

2. **follow_up_questions 테이블 생성**
   - CHAT2 팀 핵심 기능
   - 질문-응답 추적 필요

3. **trip_days 테이블 생성**
   - 일자별 일정 관리 필요

### ⚠️ 기존 테이블 수정
1. **travel_info_collection_states**
   - 시간 정보 컬럼 추가
   - 피로도 관리 컬럼 추가

2. **trip_details**
   - place_id FK 추가
   - 시간 범위 컬럼 추가

3. **trips**
   - thread_id 타입 변경

### 🟡 추후 작업 가능 (중간)
1. **reservations** - OCR 기능 구현 시
2. **place_reviews** - 리뷰 기능 구현 시
3. **weather_cache** - 날씨 API 연동 시

### 🟢 선택적 (낮음)
1. **prompt_templates** - 현재 코드로 관리 중

---

## 💡 권장사항

### 즉시 생성 필요한 엔티티 파일
```
src/main/java/com/compass/domain/trip/entity/
├── Place.java           // 🔴 최우선
├── TripDay.java         // 🔴 높음
└── PlaceReview.java     // 🟡 중간

src/main/java/com/compass/domain/chat/entity/
└── FollowUpQuestion.java // 🔴 높음

src/main/java/com/compass/domain/trip/entity/
└── Reservation.java      // 🟡 중간
```

---

## 🔗 핵심 관계

### 도메인별 테이블 분류
- **USER 도메인 (2개)**: users, user_preferences
- **CHAT 도메인 (4개)**: chat_threads, chat_messages, travel_info_collection_states, follow_up_questions
- **TRIP 도메인 (6개)**: trips, trip_days, trip_details, places, place_reviews, travel_histories
- **공통/지원 (3개)**: reservations, weather_cache, prompt_templates

### 주요 관계
1. **사용자 중심**: users → chat_threads → trips
2. **대화 플로우**: chat_threads → chat_messages → travel_info_collection_states
3. **여행 계획**: trips → trip_days → trip_details → places
4. **정보 수집**: travel_info_collection_states ↔ follow_up_questions

### 주요 특징
- UUID 사용 (chat_threads, trips)
- JSON 컬럼 활용 (선호도, 메타데이터)
- Soft Delete 지원 (trips)
- 캐싱 전략 (weather_cache)
- OCR 정보 추적 (reservations)
- 데이터 소스 구분 (places: TOUR_API vs PERPLEXITY)