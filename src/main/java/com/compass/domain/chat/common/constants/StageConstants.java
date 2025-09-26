package com.compass.domain.chat.common.constants;

// Stage 1-2-3 공통 상수 클래스
public final class StageConstants {

    private StageConstants() {
        // 인스턴스 생성 방지
    }

    // 거리 관련 상수
    public static final class Distance {
        public static final double WALKING_DISTANCE_KM = 2.0;      // 도보 가능 거리
        public static final double NEAR_DISTANCE_KM = 5.0;          // 가까운 거리
        public static final double FAR_DISTANCE_KM = 10.0;          // 먼 거리
        public static final double MAX_CLUSTER_RADIUS_KM = 5.0;     // 클러스터 최대 반경
        public static final double MAX_WALKABLE_DISTANCE_KM = 2.0;  // 최대 도보 거리
    }

    // 점수 가중치 상수
    public static final class ScoreWeight {
        public static final double DISTANCE_WEIGHT = 0.4;   // 거리 가중치
        public static final double REVIEW_WEIGHT = 0.3;     // 리뷰수 가중치
        public static final double RATING_WEIGHT = 0.3;     // 평점 가중치
        public static final double BASE_SCORE_WEIGHT = 0.6; // 기본 점수 가중치
        public static final double QUALITY_WEIGHT = 0.4;    // 품질 가중치
        public static final double DIVERSITY_WEIGHT = 0.2;  // 다양성 가중치
    }

    // 시간블록 관련 상수
    public static final class TimeBlock {
        public static final int BREAKFAST_START = 7;
        public static final int BREAKFAST_END = 9;
        public static final int MORNING_ACTIVITY_START = 9;
        public static final int MORNING_ACTIVITY_END = 12;
        public static final int LUNCH_START = 12;
        public static final int LUNCH_END = 14;
        public static final int AFTERNOON_ACTIVITY_START = 14;
        public static final int AFTERNOON_ACTIVITY_END = 18;
        public static final int DINNER_START = 18;
        public static final int DINNER_END = 20;
        public static final int EVENING_ACTIVITY_START = 20;
        public static final int EVENING_ACTIVITY_END = 22;

        public static final int MAX_PLACES_PER_BLOCK = 2;  // 시간블록당 최대 장소 수
        public static final int DEFAULT_ACTIVITY_MINUTES = 90; // 기본 활동 시간(분)
    }

    // Stage별 제한 상수
    public static final class Limits {
        public static final int CANDIDATES_PER_CATEGORY = 10;  // 카테고리당 후보 수
        public static final int PLACES_PER_CLUSTER = 10;       // 클러스터당 추천 장소 수
        public static final int MAX_TRIP_DAYS = 3;             // 최대 여행 일수
        public static final int MIN_TRIP_DAYS = 1;             // 최소 여행 일수
        public static final int MAX_SELECTIONS_PER_CATEGORY = 1; // 카테고리당 최대 선택 수
    }

    // 이동 수단별 속도 (km/h)
    public static final class TransportSpeed {
        public static final double CAR_SPEED_KMH = 30.0;           // 자동차 (도심)
        public static final double PUBLIC_TRANSPORT_SPEED_KMH = 20.0; // 대중교통
        public static final double WALKING_SPEED_KMH = 4.0;        // 도보
        public static final double DEFAULT_SPEED_KMH = 25.0;       // 기본값
    }

    // 카테고리 이름
    public static final class Category {
        public static final String TOURIST_ATTRACTION = "관광명소";
        public static final String RESTAURANT = "맛집";
        public static final String CAFE = "카페";
        public static final String SHOPPING = "쇼핑";
        public static final String ACTIVITY = "액티비티";
        public static final String CULTURE = "문화체험";
        public static final String NATURE = "자연경관";
        public static final String THEME_PARK = "테마파크";
        public static final String NIGHT_VIEW = "야경명소";
        public static final String ACCOMMODATION = "숙박";
    }

    // 로그 스케일 상수
    public static final class Scoring {
        public static final double REVIEW_LOG_BASE = 3.0;  // 리뷰 로그 베이스
        public static final double MAX_RATING = 5.0;       // 최대 평점
        public static final double REVIEW_THRESHOLD = 1000; // 리뷰 임계값
    }
}