---
name: 기능 개발
about: 기능명세서 기반 개발 작업을 위한 이슈 템플릿
title: '[TRIP] REQ-PREF-002 | 예산 수준 설정'
labels: '백엔드'
assignees: 'TRIP1'
---

## 📋 기능 개요
**요구사항 ID**: REQ-PREF-002
사용자의 여행 예산 수준(BUDGET/STANDARD/LUXURY)을 저장하고 관리하는 기능을 구현합니다.

## 🎯 개발 목표
- `user_preferences` 테이블을 재사용하여 사용자의 예산 수준을 저장합니다.
- 예산 수준은 `BUDGET`, `STANDARD`, `LUXURY` 세 가지 중 하나로 관리합니다.
- 향후 개인화된 여행 계획 추천 시 비용 수준을 결정하는 기준으로 활용합니다.

## 📝 기능 명세
### API Endpoints
- **`POST /api/users/{userId}/preferences/budget-level`**: 예산 수준을 설정합니다.
- **`GET /api/users/{userId}/preferences/budget-level`**: 예산 수준을 조회합니다.
- **`PUT /api/users/{userId}/preferences/budget-level`**: 예산 수준을 수정합니다.

### 1. 예산 수준 설정/수정 API (`POST`, `PUT`)

#### Request Body
```json
{
  "budgetLevel": "STANDARD"
}
```

#### Response Body (성공)
```json
{
  "userId": 1,
  "budgetLevel": "STANDARD",
  "description": "일반 여행 (일일 약 10만원 ~ 20만원)",
  "message": "예산 수준이 성공적으로 설정되었습니다."
}
```

### 2. 예산 수준 조회 API (`GET`)

#### Response Body (성공 - 설정된 경우)
```json
{
  "userId": 1,
  "budgetLevel": "STANDARD",
  "description": "일반 여행 (일일 약 10만원 ~ 20만원)"
}
```

#### Response Body (성공 - 미설정)
```json
{
  "userId": 1,
  "budgetLevel": null,
  "description": "설정된 예산 수준이 없습니다."
}
```

## 🔧 구현 사항
### Entity & Enum
- [x] `BudgetLevel.java` Enum 클래스 생성
- [x] `UserPreference.java` Entity 클래스 재사용

### DTO
- [x] `BudgetRequest.java` 생성
- [x] `BudgetResponse.java` 생성

### Repository
- [x] `UserPreferenceRepository.java` 인터페이스 재사용

### Service
- [x] `UserPreferenceService.java` 클래스에 로직 추가
  - [x] `setOrUpdateBudgetLevel()`: 예산 수준 설정/수정
  - [x] `getBudgetLevel()`: 예산 수준 조회

### Controller
- [x] `UserPreferenceController.java` 클래스에 엔드포인트 추가
  - [x] `POST /budget-level` 엔드포인트 구현
  - [x] `GET /budget-level` 엔드포인트 구현
  - [x] `PUT /budget-level` 엔드포인트 구현

### Exception Handling
- [x] `IllegalArgumentException` 처리 로직 확인

## 📊 데이터베이스 스키마
- 기존 `user_preferences` 테이블을 사용하며, `preference_type`을 `'BUDGET_LEVEL'`로 지정하여 구분합니다.

```sql
-- user_preferences 테이블 사용
-- preference_type = 'BUDGET_LEVEL'
-- preference_key = 'BUDGET', 'STANDARD', 'LUXURY'
-- preference_value = 1.0 (고정값)
```

## ✅ 완료 조건
- [ ] 예산 수준 설정/수정 API 정상 동작
- [ ] 예산 수준 조회 API 정상 동작
- [ ] 유효하지 않은 `budgetLevel` 값에 대한 예외 처리 정상 동작
- [ ] Swagger 문서화 완료
- [ ] 단위 테스트 및 통합 테스트 작성 및 통과

## 📌 참고사항
- `REQ-PREF-001`과 동일한 `UserPreference` 엔티티 및 테이블을 공유하므로 데이터 저장/조회 로직의 일관성을 유지합니다.
- 추후 Spring Security 적용 시 인증된 사용자 정보를 활용하도록 수정될 예정입니다.
