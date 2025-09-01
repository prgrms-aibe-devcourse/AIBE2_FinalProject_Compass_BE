---
name: 기능 개발
about: 기능명세서 기반 개발 작업을 위한 이슈 템플릿
title: '[TRIP] REQ-TRIP-000 | Trip 테이블 설계'
labels: '백엔드'
assignees: 'TRIP1'
---

## 📋 기능 개요
**요구사항 ID**: REQ-TRIP-000
AI가 생성한 여행 계획 데이터를 저장하기 위한 `trips`와 `trip_details` 테이블의 JPA 엔티티를 설계하고 구현합니다.

## 🎯 개발 목표
- `Trip` 및 `TripDetail` 엔티티 클래스를 생성하여 데이터베이스 테이블과 매핑합니다.
- 두 엔티티 간의 1:N 관계(OneToMany, ManyToOne)를 설정합니다.
- 향후 기능 확장을 고려한 기본 필드를 포함하여 테이블 스키마를 정의합니다.

## 📝 기능 명세
### API Endpoints
- 해당 없음 (엔티티 설계 작업)

## 🔧 구현 사항
### Entity
- [x] `Trip.java` Entity 클래스 생성
- [x] `TripDetail.java` Entity 클래스 생성
- [x] `Trip`과 `TripDetail` 간 1:N 연관관계 설정

### Repository
- [x] `TripRepository.java` 인터페이스 생성
- [x] `TripDetailRepository.java` 인터페이스 생성

### Service
- [x] `TripService.java` 클래스 생성

### Controller
- [x] `TripController.java` 클래스 생성

## 📊 데이터베이스 스키마
생성될 테이블의 DDL은 `DATABASE_ERD.md`의 명세를 따릅니다.
```sql
-- trips: 여행 계획 테이블
CREATE TABLE trips (
    id BIGSERIAL PRIMARY KEY,
    trip_uuid UUID UNIQUE DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL, -- REFERENCES users(id)
    thread_id BIGINT, -- REFERENCES chat_threads(id)
    title VARCHAR(255) NOT NULL,
    destination VARCHAR(255) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    number_of_people INTEGER,
    total_budget INTEGER,
    status VARCHAR(20) DEFAULT 'PLANNING',
    trip_metadata JSONB,
    version INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP
);

-- trip_details: 여행 일정 상세 테이블
CREATE TABLE trip_details (
    id BIGSERIAL PRIMARY KEY,
    trip_id BIGINT NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    day_number INTEGER NOT NULL,
    activity_date DATE,
    activity_time TIME,
    place_name VARCHAR(255) NOT NULL,
    category VARCHAR(50),
    description TEXT,
    estimated_cost INTEGER,
    address VARCHAR(500),
    latitude DECIMAL(10,8),
    longitude DECIMAL(11,8),
    tips TEXT,
    additional_info JSONB,
    display_order INTEGER
);
```

## 🔗 관련 이슈
- 관련 요구사항: `REQ-TRIP-000`

## ✅ 완료 조건
- [x] `Trip` 도메인 기본 구조 (Entity, Repository, Service, Controller) 생성 완료
- [ ] 코드 리뷰 완료
- [ ] `DATABASE_ERD.md` 업데이트

## 📌 참고사항
- 초기 단계에서는 `User` 엔티티와의 직접적인 연관관계 대신 `userId` 필드만 유지합니다. 사용자 관련 기능이 구체화되면 연관관계를 설정할 예정입니다.
