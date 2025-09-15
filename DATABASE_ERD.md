# 📊 Compass 프로젝트 ERD (Entity Relationship Diagram)

## ERD 다이어그램

```mermaid
erDiagram
    %% USER 도메인
    users ||--o{ chat_threads : "has"
    users ||--o{ trips : "creates"
    users ||--o{ place_reviews : "writes"
    users ||--o{ reservations : "makes"
    users ||--o| user_preferences : "has"
    users ||--o{ travel_histories : "has"
    users ||--o| user_contexts : "has"

    %% CHAT 도메인
    chat_threads ||--o{ chat_messages : "contains"
    chat_threads ||--o| travel_info_collection_states : "tracks"
    chat_threads ||--o{ follow_up_questions : "generates"
    chat_threads ||--o| trips : "results_in"

    %% TRIP 도메인
    trips ||--o{ trip_days : "contains"
    trips ||--o{ trip_details : "has"
    trips ||--o{ place_reviews : "associated"
    trips ||--o{ reservations : "linked"
    trip_days ||--o{ trip_details : "includes"
    
    %% PLACE 관계
    places ||--o{ trip_details : "visited_in"
    places ||--o{ place_reviews : "reviewed"
    
    %% MEDIA 도메인
    media ||--o{ chat_messages : "attached_to"
    media ||--o{ place_reviews : "included_in"

    %% 엔티티 정의
    users {
        bigint id PK
        string email UK
        string password
        string nickname
        string profile_image_url
        string social_type
        string social_id
        string role
        timestamp created_at
        timestamp updated_at
    }

    user_preferences {
        bigint id PK
        bigint user_id FK
        json preferred_travel_style
        json preferred_food_types
        json preferred_accommodation_types
        integer budget_range_min
        integer budget_range_max
        text special_requirements
    }

    user_contexts {
        bigint id PK
        bigint user_id FK
        text special_requirements
        timestamp created_at
        timestamp updated_at
    }

    chat_threads {
        uuid id PK
        bigint user_id FK
        string title
        timestamp last_message_at
        timestamp created_at
        timestamp updated_at
    }

    chat_messages {
        bigint id PK
        uuid thread_id FK
        string sender_type
        text content
        json metadata
        timestamp timestamp
    }

    travel_info_collection_states {
        bigint id PK
        uuid thread_id FK
        bigint user_id FK
        text origin_raw
        string origin
        text destination_raw
        string destination
        text dates_raw
        date start_date
        date end_date
        text duration_raw
        integer duration
        text companions_raw
        json companions
        text budget_raw
        integer budget
        json travel_style
        json main_interests
        json additional_preferences
        time departure_time
        time return_time
        integer collection_progress
        string collection_status
        string strategy_type
        integer fatigue_score
        integer question_count
        text last_question_asked
        timestamp created_at
        timestamp updated_at
    }

    follow_up_questions {
        bigint id PK
        uuid thread_id FK
        text question_text
        string question_type
        json options
        boolean is_answered
        text user_response
        timestamp response_timestamp
        integer question_order
        boolean is_required
        integer retry_count
        string skip_reason
        timestamp created_at
    }

    trips {
        bigint id PK
        uuid trip_uuid UK
        bigint user_id FK
        uuid thread_id FK
        string title
        string destination
        date start_date
        date end_date
        integer number_of_people
        integer total_budget
        string status
        json trip_metadata
        timestamp created_at
        timestamp updated_at
        timestamp deleted_at
    }

    trip_days {
        bigint id PK
        bigint trip_id FK
        integer day_number
        date date
        time start_time
        time end_time
        string theme
        text summary
        integer total_cost_estimate
        double total_distance
        text weather_info
        text notes
        timestamp created_at
        timestamp updated_at
    }

    trip_details {
        bigint id PK
        bigint trip_id FK
        bigint trip_day_id FK
        bigint place_id FK
        integer day_number
        date activity_date
        time activity_time
        time start_time
        time end_time
        string place_name
        string category
        string activity_type
        text description
        integer estimated_cost
        string address
        double latitude
        double longitude
        text tips
        text additional_info
        integer display_order
    }

    places {
        bigint id PK
        string place_code UK
        string name
        string name_en
        string category
        string sub_category
        string destination
        text address
        double latitude
        double longitude
        string phone
        string website
        json business_hours
        integer price_range
        double rating
        integer review_count
        text description
        json image_urls
        json tags
        boolean is_trendy
        string data_source
        timestamp last_updated
        timestamp created_at
        timestamp updated_at
    }

    place_reviews {
        bigint id PK
        bigint place_id FK
        bigint user_id FK
        bigint trip_id FK
        integer rating
        text review_text
        date visit_date
        json images
        json tags
        integer helpful_count
        integer report_count
        boolean is_verified
        boolean is_visible
        integer taste_rating
        integer service_rating
        integer price_rating
        integer ambience_rating
        integer cleanliness_rating
        timestamp created_at
        timestamp updated_at
    }

    travel_histories {
        bigint id PK
        bigint user_id FK
        bigint trip_id FK
        string destination
        date travel_date
        string companions
        text travel_notes
        json preferred_activities
        integer rating
    }

    reservations {
        bigint id PK
        bigint user_id FK
        bigint trip_id FK
        string type
        string confirmation_number
        string provider
        string departure_location
        string arrival_location
        timestamp departure_time
        timestamp arrival_time
        string flight_number
        string seat_number
        string accommodation_name
        string room_type
        date check_in_date
        date check_out_date
        integer number_of_nights
        decimal price
        string currency
        integer number_of_passengers
        string reservation_status
        boolean ocr_extracted
        string original_image_url
        double ocr_confidence
        json reservation_details
        text notes
        timestamp created_at
        timestamp updated_at
    }

    media {
        bigint id PK
        bigint user_id FK
        string file_name
        string file_type
        bigint file_size
        string s3_key
        string s3_url
        string status
        timestamp created_at
        timestamp updated_at
    }
```

## 주요 관계 설명

### 1. 사용자 중심 관계
- **users** → **chat_threads**: 1:N (한 사용자가 여러 대화 스레드 생성)
- **users** → **trips**: 1:N (한 사용자가 여러 여행 계획 생성)
- **users** → **user_preferences**: 1:1 (사용자별 선호도 설정)

### 2. 대화 플로우 관계
- **chat_threads** → **chat_messages**: 1:N (한 스레드에 여러 메시지)
- **chat_threads** → **travel_info_collection_states**: 1:1 (스레드별 정보 수집 상태)
- **chat_threads** → **follow_up_questions**: 1:N (스레드별 여러 후속 질문)
- **chat_threads** → **trips**: 1:1 (대화 결과로 여행 계획 생성)

### 3. 여행 계획 관계
- **trips** → **trip_days**: 1:N (여행 계획의 일자별 일정)
- **trip_days** → **trip_details**: 1:N (일자별 세부 일정)
- **places** → **trip_details**: 1:N (장소가 여러 일정에 포함)
- **places** → **place_reviews**: 1:N (장소별 여러 리뷰)

### 4. 예약 관계
- **users** → **reservations**: 1:N (사용자별 여러 예약)
- **trips** → **reservations**: 1:N (여행별 여러 예약, Nullable)

## 데이터 흐름

```
사용자 입력
    ↓
ChatThread 생성
    ↓
ChatMessage 저장 + TravelInfoCollectionState 업데이트
    ↓
FollowUpQuestion 생성 및 응답 수집
    ↓
정보 수집 완료
    ↓
Trip 생성
    ↓
TripDay 생성 (일자별)
    ↓
TripDetail 생성 (세부 일정)
    ↓
Place 참조 (Tour API + Perplexity)
    ↓
Reservation 연결 (OCR 추출 포함)
    ↓
여행 완료 후 PlaceReview 작성
```

## 인덱스 전략

### 성능 최적화 인덱스
1. **places**: destination + category 복합 인덱스 (빈번한 조회)
2. **trip_details**: trip_id + day_number (일정 조회)
3. **follow_up_questions**: thread_id + is_answered (미응답 질문 조회)
4. **place_reviews**: place_id + rating (장소별 리뷰 조회)
5. **reservations**: user_id + type + departure_time (예약 조회)

## 주의사항

1. **UUID 사용**: chat_threads, trips는 UUID 사용으로 보안 강화
2. **Soft Delete**: trips 테이블은 deleted_at으로 소프트 삭제
3. **JSON 컬럼**: PostgreSQL의 jsonb 타입 활용으로 유연한 데이터 저장
4. **OCR 추적**: reservations 테이블에서 OCR 추출 정보 관리
5. **데이터 소스**: places 테이블에서 Tour API vs Perplexity 구분