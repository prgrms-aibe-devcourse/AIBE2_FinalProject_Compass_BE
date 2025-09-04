---
name: 기능 개발
about: 기능명세서 기반 개발 작업을 위한 이슈 템플릿
title: '[TRIP] REQ-PREF-001 | 여행 스타일 설정'
labels: '백엔드'
assignees: 'TRIP1'
---

## 📋 기능 개요
**요구사항 ID**: REQ-PREF-001
사용자의 여행 스타일 선호도(휴양/관광/액티비티)를 저장하고 관리하는 기능을 구현합니다.

## 🎯 개발 목표
- `user_preferences` 테이블에 ENUM 타입으로 여행 스타일을 저장합니다.
- 각 스타일별 가중치를 관리하여 개인화된 추천의 기반을 마련합니다.
- 휴양/관광/액티비티 3가지 스타일에 대한 선호도를 0.0~1.0 범위로 설정합니다.
- TRIP2의 개인화 알고리즘에서 활용할 수 있는 데이터 구조를 제공합니다.

## 📝 기능 명세
### API Endpoints
- **`POST /api/users/{userId}/preferences/travel-style`**: 여행 스타일 선호도를 설정합니다.
- **`GET /api/users/{userId}/preferences/travel-style`**: 여행 스타일 선호도를 조회합니다.
- **`PUT /api/users/{userId}/preferences/travel-style`**: 여행 스타일 선호도를 수정합니다.

### 1. 여행 스타일 설정 API

#### Request
- **Path Variable**: `userId` (Long) - 설정할 사용자의 ID

#### Request Body
```json
{
  "preferences": [
    {
      "travelStyle": "RELAXATION",
      "weight": 0.5
    },
    {
      "travelStyle": "SIGHTSEEING", 
      "weight": 0.3
    },
    {
      "travelStyle": "ACTIVITY",
      "weight": 0.2
    }
  ]
}
```

#### Response Body (성공)
```json
{
  "userId": 1,
  "preferences": [
    {
      "travelStyle": "RELAXATION",
      "weight": 0.5,
      "description": "휴양 및 힐링을 중심으로 한 여행",
      "createdAt": "2024-08-01T10:00:00",
      "updatedAt": "2024-08-01T10:00:00"
    },
    {
      "travelStyle": "SIGHTSEEING", 
      "weight": 0.3,
      "description": "관광지 방문 및 문화 체험 중심 여행",
      "createdAt": "2024-08-01T10:00:00",
      "updatedAt": "2024-08-01T10:00:00"
    },
    {
      "travelStyle": "ACTIVITY",
      "weight": 0.2,
      "description": "액티비티 및 체험 중심 여행",
      "createdAt": "2024-08-01T10:00:00",
      "updatedAt": "2024-08-01T10:00:00"
    }
  ],
  "totalWeight": 1.0,
  "message": "여행 스타일 선호도가 성공적으로 설정되었습니다."
}
```

### 2. 여행 스타일 조회 API

#### Request
- **Path Variable**: `userId` (Long) - 조회할 사용자의 ID

#### Response Body (성공)
```json
{
  "userId": 1,
  "preferences": [
    {
      "travelStyle": "RELAXATION",
      "weight": 0.5,
      "description": "휴양 및 힐링을 중심으로 한 여행",
      "createdAt": "2024-08-01T10:00:00",
      "updatedAt": "2024-08-01T10:00:00"
    },
    {
      "travelStyle": "SIGHTSEEING", 
      "weight": 0.3,
      "description": "관광지 방문 및 문화 체험 중심 여행",
      "createdAt": "2024-08-01T10:00:00",
      "updatedAt": "2024-08-01T10:00:00"
    },
    {
      "travelStyle": "ACTIVITY",
      "weight": 0.2,
      "description": "액티비티 및 체험 중심 여행",
      "createdAt": "2024-08-01T10:00:00",
      "updatedAt": "2024-08-01T10:00:00"
    }
  ],
  "totalWeight": 1.0
}
```

#### Response Body (선호도 미설정)
```json
{
  "userId": 1,
  "preferences": [],
  "totalWeight": 0.0,
  "message": "설정된 여행 스타일 선호도가 없습니다."
}
```

### 3. 여행 스타일 수정 API

#### Request
- **Path Variable**: `userId` (Long) - 수정할 사용자의 ID

#### Request Body
```json
{
  "preferences": [
    {
      "travelStyle": "RELAXATION",
      "weight": 0.4
    },
    {
      "travelStyle": "SIGHTSEEING", 
      "weight": 0.4
    },
    {
      "travelStyle": "ACTIVITY",
      "weight": 0.2
    }
  ]
}
```

#### Response Body (성공)
```json
{
  "userId": 1,
  "preferences": [
    {
      "travelStyle": "RELAXATION",
      "weight": 0.4,
      "description": "휴양 및 힐링을 중심으로 한 여행",
      "updatedAt": "2024-08-01T11:00:00"
    },
    {
      "travelStyle": "SIGHTSEEING", 
      "weight": 0.4,
      "description": "관광지 방문 및 문화 체험 중심 여행",
      "updatedAt": "2024-08-01T11:00:00"
    },
    {
      "travelStyle": "ACTIVITY",
      "weight": 0.2,
      "description": "액티비티 및 체험 중심 여행",
      "updatedAt": "2024-08-01T11:00:00"
    }
  ],
  "totalWeight": 1.0,
  "message": "여행 스타일 선호도가 성공적으로 수정되었습니다."
}
```

## 🔧 구현 사항
### Entity
- [x] `UserPreference.java` Entity 클래스 생성
- [x] `TravelStyle` ENUM 클래스 생성

### DTO
- [x] `TravelStylePreferenceRequest.java` 생성
- [x] `TravelStylePreferenceResponse.java` 생성
- [x] `TravelStyleItem.java` 생성

### Repository
- [x] `UserPreferenceRepository.java` 인터페이스 생성

### Service
- [x] `UserPreferenceService.java` 클래스 생성
  - [x] `setTravelStylePreferences()`: 여행 스타일 선호도 설정
  - [x] `getTravelStylePreferences()`: 여행 스타일 선호도 조회
  - [x] `updateTravelStylePreferences()`: 여행 스타일 선호도 수정
  - [x] `validateWeights()`: 가중치 합계 검증 (총합 1.0)

### Controller
- [x] `UserPreferenceController.java` 클래스 생성
  - [x] `POST /api/users/{userId}/preferences/travel-style` 엔드포인트 구현
  - [x] `GET /api/users/{userId}/preferences/travel-style` 엔드포인트 구현
  - [x] `PUT /api/users/{userId}/preferences/travel-style` 엔드포인트 구현

### Exception Handling
- [x] `InvalidWeightSumException` - 가중치 합계 오류 예외
- [x] `InvalidWeightRangeException` - 가중치 범위 오류 예외
- [x] `DuplicateTravelStyleException` - 중복 여행 스타일 예외
- [x] `GlobalExceptionHandler` 예외 처리 추가

### Validation
- [x] 가중치 합계가 1.0인지 검증
- [x] 가중치가 0.0 ~ 1.0 범위 내인지 검증
- [x] 중복된 여행 스타일이 없는지 검증

### Test
- [x] `UserPreferenceServiceTest.java` - 서비스 단위 테스트
- [x] `UserPreferenceControllerTest.java` - 컨트롤러 통합 테스트
- [x] `REQ-PREF-001.test.md` - 테스트 케이스 문서

## 📊 데이터베이스 스키마

```sql
-- user_preferences: 사용자 선호도 테이블
CREATE TABLE user_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL, -- REFERENCES users(id)
    preference_type VARCHAR(50) NOT NULL, -- 'TRAVEL_STYLE'
    preference_key VARCHAR(50) NOT NULL,  -- 'RELAXATION', 'SIGHTSEEING', 'ACTIVITY'
    preference_value DECIMAL(3,2) NOT NULL, -- 가중치 (0.00 ~ 1.00)
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, preference_type, preference_key)
);

-- 인덱스 생성
CREATE INDEX idx_user_preferences_user_type ON user_preferences(user_id, preference_type);
```

## 🎯 여행 스타일 정의

### RELAXATION (휴양)
- **설명**: 휴식과 힐링을 중심으로 한 여행
- **특징**: 스파, 온천, 해변 리조트, 요가 리트리트 등
- **추천 장소**: 리조트, 스파, 해변, 온천, 공원

### SIGHTSEEING (관광)
- **설명**: 관광지 방문 및 문화 체험 중심 여행
- **특징**: 유명 관광지, 박물관, 역사적 장소, 문화 체험
- **추천 장소**: 박물관, 궁궐, 사찰, 랜드마크, 전통 마을

### ACTIVITY (액티비티)
- **설명**: 체험과 활동 중심의 적극적인 여행
- **특징**: 등산, 스포츠, 어드벤처, 체험 활동
- **추천 장소**: 테마파크, 등산로, 체험관, 스포츠 시설

## 🔗 관련 이슈
- 관련 요구사항: `REQ-PREF-001`
- 의존성: 추후 User 엔티티와 연관관계 설정 예정

## ✅ 완료 조건
- [x] 여행 스타일 선호도 설정 API 정상 동작
- [x] 여행 스타일 선호도 조회 API 정상 동작  
- [x] 여행 스타일 선호도 수정 API 정상 동작
- [x] 가중치 합계 검증 로직 구현
- [x] Swagger 문서화 완료
- [x] 단위 테스트 및 통합 테스트 작성

## 🎉 구현 완료 상태

**구현 완료일**: 2024년 12월 26일  
**구현자**: TRIP1 팀  
**상태**: ✅ **완료**

### 📁 구현된 파일 목록
```
src/main/java/com/compass/domain/trip/
├── enums/
│   └── TravelStyle.java                    # 여행 스타일 ENUM
├── entity/
│   └── UserPreference.java                # 사용자 선호도 엔티티
├── repository/
│   └── UserPreferenceRepository.java      # Repository 인터페이스
├── dto/
│   ├── TravelStyleItem.java               # 여행 스타일 항목 DTO
│   ├── TravelStylePreferenceRequest.java  # 요청 DTO
│   └── TravelStylePreferenceResponse.java # 응답 DTO
├── exception/
│   ├── InvalidWeightSumException.java     # 가중치 합계 예외
│   ├── InvalidWeightRangeException.java   # 가중치 범위 예외
│   └── DuplicateTravelStyleException.java # 중복 스타일 예외
├── service/
│   └── UserPreferenceService.java         # 비즈니스 로직 서비스
└── controller/
    └── UserPreferenceController.java      # REST API 컨트롤러

src/test/java/com/compass/domain/trip/
├── service/
│   └── UserPreferenceServiceTest.java     # 서비스 단위 테스트
└── controller/
    └── UserPreferenceControllerTest.java  # 컨트롤러 통합 테스트

docs/features/
├── REQ-PREF-001.md                        # 기능 명세서 (본 문서)
└── REQ-PREF-001.test.md                   # 테스트 케이스 문서
```

### 🔄 TRIP2 연동 준비 완료
```java
// TRIP2에서 사용자 선호도 조회 예시
@Service
public class TripPlanningService {
    
    private final UserPreferenceService userPreferenceService;
    
    public TravelPlan generatePersonalizedPlan(Long userId, TravelRequest request) {
        // 1. 사용자 여행 스타일 선호도 조회
        TravelStylePreferenceResponse preferences = 
            userPreferenceService.getTravelStylePreferences(userId);
        
        // 2. AI 프롬프트에 선호도 반영
        String personalizedPrompt = buildPromptWithPreferences(request, preferences);
        
        // 3. 개인화된 여행 계획 생성
        return aiService.generateTravelPlan(personalizedPrompt);
    }
}
```

## 📌 참고사항
- 초기 구현에서는 `userId`를 Path Variable로 받습니다. 추후 Spring Security 적용 시 인증된 사용자 정보를 활용하도록 수정될 예정입니다.
- 여행 스타일은 확장 가능하도록 ENUM으로 관리하되, 추후 새로운 스타일 추가 시 코드 수정 없이 확장할 수 있도록 설계합니다.
- 가중치의 총합은 반드시 1.0이어야 하며, 이를 검증하는 로직을 포함합니다.
- TRIP2의 개인화 알고리즘에서 이 선호도 데이터를 활용하여 맞춤형 여행 계획을 생성합니다.
