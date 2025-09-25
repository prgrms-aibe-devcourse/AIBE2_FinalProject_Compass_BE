# Phase3 Stage2: 알고리즘 기반 클러스터링 + LLM 검수

## 📋 개요
Stage2는 Stage1에서 수집한 관광지 데이터를 K-Means 알고리즘으로 클러스터링한 후, LLM 검수를 통해 최적의 일정을 생성합니다.

## 🏗️ 아키텍처

### 주요 컴포넌트
1. **Stage2HybridDistributor**: 메인 오케스트레이터
2. **KMeansClusteringService**: K-Means 클러스터링 알고리즘 구현
3. **MockDataGenerator**: Stage1 Mock 데이터 생성
4. **DistanceUtils**: 거리 계산 유틸리티

### 처리 흐름
```
Stage1 Data → K-Means Clustering → Region Assignment → Daily Itinerary → LLM Review → Final Output
```

## 🚀 실행 방법

### 1. 애플리케이션 실행
```bash
./gradlew bootRun
```

### 2. API 테스트

#### Mock 데이터로 테스트
```bash
# 3일 여행 일정 생성
curl -X GET "http://localhost:8080/api/phase3/stage2/test?tripDays=3"

# 5일 여행 일정 생성
curl -X GET "http://localhost:8080/api/phase3/stage2/test?tripDays=5"
```

#### 특정 threadId로 테스트
```bash
curl -X POST "http://localhost:8080/api/phase3/stage2/distribute?threadId=test123&tripDays=3"
```

### 3. 테스트 스크립트 실행
```bash
./script/test-stage2.sh
```

## 📊 알고리즘 상세

### K-Means 클러스터링
- **Elbow Method**: 최적 클러스터 수 자동 결정
- **K-Means++ 초기화**: 더 나은 초기 중심점 선택
- **거리 기반 그룹화**: Haversine 공식으로 실제 거리 계산

### 지역 할당 전략
- **큰 지역 (20개 이상)**: 단독 일자 할당
- **중간 지역 (10-19개)**: 최적 일자에 배치
- **작은 지역 (10개 미만)**: 가까운 지역과 병합

### LLM 검수 기준
1. 하루 이동거리 20km 초과 시
2. 필수 시간 블록(점심/저녁) 누락 시
3. 일일 장소 수가 4개 미만 또는 10개 초과 시

## 📝 출력 형식

### Stage2Output 구조
```json
{
  "dailyItineraries": {
    "1": {
      "dayNumber": 1,
      "date": "2024-01-20",
      "regions": ["홍대"],
      "places": [...],
      "totalDistance": 8.5
    }
  },
  "summary": {
    "totalDays": 3,
    "totalPlaces": 21,
    "totalRegions": 5,
    "averageRegionsPerDay": 1.7,
    "categoryDistribution": {...},
    "estimatedTotalDistance": 36.5,
    "llmReviewApplied": false
  }
}
```

## 🔧 설정 파라미터

### 거리 임계값
- `MAX_DISTANCE_THRESHOLD`: 5.0km (같은 지역 판단)
- `COMFORTABLE_DAILY_DISTANCE`: 15.0km (적정 일일 이동거리)
- `WARNING_DAILY_DISTANCE`: 20.0km (LLM 검수 트리거)

### 클러스터링 파라미터
- `MAX_ITERATIONS`: 100 (K-Means 최대 반복)
- `ELBOW_THRESHOLD`: 0.2 (20% 감소율 기준)

## 🛠️ Mock 데이터 설정

### 서울 주요 지역 좌표
- 홍대: (37.5563, 126.9220)
- 강남: (37.4979, 127.0276)
- 명동: (37.5636, 126.9869)
- 성수: (37.5446, 127.0565)
- 종로: (37.5729, 126.9793)
- 이태원: (37.5346, 126.9945)
- 잠실: (37.5113, 127.0980)
- 신촌: (37.5585, 126.9390)

### 시간 블록
- BREAKFAST (08:00-10:00)
- MORNING_ACTIVITY (10:00-12:00)
- LUNCH (12:00-14:00)
- CAFE (14:00-16:00)
- AFTERNOON_ACTIVITY (16:00-18:00)
- DINNER (18:00-20:00)
- EVENING_ACTIVITY (20:00-22:00)

## 📌 주의사항
- Mock 데이터를 사용하므로 실제 Stage1과 연동하려면 `MockDataGenerator` 대신 실제 Repository 사용
- LLM 검수는 Gemini Flash 모델 사용 (Spring AI 설정 필요)
- 대량 데이터 처리 시 메모리 사용량 주의