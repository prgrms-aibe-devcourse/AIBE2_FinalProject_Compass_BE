package com.compass.domain.chat.constant;

/**
 * 여행 정보 수집 관련 상수 정의
 * 중복된 상수 정의를 방지하고 중앙화된 관리를 위한 클래스
 */
public final class TravelConstants {
    
    private TravelConstants() {
        // 인스턴스 생성 방지
    }
    
    // 세션 관련 상수
    public static final int SESSION_TIMEOUT_HOURS = 24;
    public static final long CACHE_TTL_MINUTES = 30;
    public static final String CACHE_KEY_PREFIX = "travel:collection:";
    
    // 질문 플로우 관련 상수
    public static final int REQUIRED_QUESTIONS_WITHOUT_ORIGIN = 5;
    public static final int REQUIRED_QUESTIONS_WITH_ORIGIN = 6;
    
    // 컨텍스트 관리 관련 상수
    public static final int MAX_CONVERSATION_MESSAGES = 10;
    public static final int MAX_CONTEXT_TOKENS = 8000;
    public static final int AVG_CHARS_PER_TOKEN = 4;
    
    // 기본값
    public static final String DEFAULT_DESTINATION = "Seoul";
    public static final String DEFAULT_BUDGET_LEVEL = "moderate";
    public static final String DEFAULT_COMPANION_TYPE = "solo";
    public static final int DEFAULT_DURATION_NIGHTS = 2;
    public static final int DEFAULT_NUMBER_OF_TRAVELERS = 1;
    public static final String DEFAULT_CURRENCY = "KRW";
    
    // 여행 스타일
    public static final String TRAVEL_STYLE_RELAXED = "relaxed";
    public static final String TRAVEL_STYLE_ACTIVE = "active";
    public static final String TRAVEL_STYLE_ADVENTUROUS = "adventurous";
    public static final String TRAVEL_STYLE_LUXURY = "luxury";
    
    // 예산 레벨
    public static final String BUDGET_LEVEL_BUDGET = "budget";
    public static final String BUDGET_LEVEL_MODERATE = "moderate";
    public static final String BUDGET_LEVEL_LUXURY = "luxury";
    
    // 여행 목적
    public static final String TRIP_PURPOSE_LEISURE = "leisure";
    public static final String TRIP_PURPOSE_BUSINESS = "business";
    public static final String TRIP_PURPOSE_HONEYMOON = "honeymoon";
    public static final String TRIP_PURPOSE_FAMILY = "family vacation";
    
    // 그룹 타입
    public static final String GROUP_TYPE_SOLO = "solo";
    public static final String GROUP_TYPE_COUPLE = "couple";
    public static final String GROUP_TYPE_FAMILY = "family";
    public static final String GROUP_TYPE_FRIENDS = "friends";
    
    // 검증 관련 상수
    public static final int MIN_STRING_LENGTH = 2;
    public static final int MAX_STRING_LENGTH = 100;
    public static final int MAX_DURATION_DAYS = 365;
    public static final int MIN_DURATION_DAYS = 0;  // 당일치기 허용
    public static final int MAX_TRAVELERS = 50;
    public static final int MIN_TRAVELERS = 1;
    public static final int MIN_BUDGET = 10000;       // 1만원
    public static final int MAX_BUDGET = 100000000;    // 1억원
    public static final int MAX_ADVANCE_BOOKING_DAYS = 365;  // 최대 1년 후까지 예약 가능
    
    // 진행률 관련
    public static final int TOTAL_REQUIRED_FIELDS = 6;  // 출발지, 목적지, 날짜, 기간, 동행자, 예산
    
    // 한국어 날짜 패턴
    public static final String[] KOREAN_DURATION_PATTERNS = {
        "당일치기", "1박2일", "2박3일", "3박4일", "4박5일", "5박6일", "6박7일"
    };
    
    // 한국어 목적지 키워드
    public static final String[] KOREAN_DESTINATIONS = {
        "서울", "부산", "제주", "경주", "전주", "강릉", "여수", "속초", "인천", "대구",
        "대전", "광주", "울산", "춘천", "안동", "통영", "거제", "남해", "목포", "순천"
    };
    
    // 예산 키워드
    public static final String[] BUDGET_KEYWORDS_LOW = {
        "저렴", "싸게", "가성비", "절약", "저예산", "백패킹"
    };
    
    public static final String[] BUDGET_KEYWORDS_HIGH = {
        "럭셔리", "호화", "프리미엄", "고급", "특급", "5성급"
    };
    
    // 동행자 키워드
    public static final String[] COMPANION_KEYWORDS_SOLO = {
        "혼자", "나홀로", "솔로", "혼행", "1인"
    };
    
    public static final String[] COMPANION_KEYWORDS_COUPLE = {
        "커플", "연인", "신혼", "둘이", "2인", "부부", "애인"
    };
    
    public static final String[] COMPANION_KEYWORDS_FAMILY = {
        "가족", "아이", "부모님", "효도", "아기", "자녀", "식구"
    };
    
    public static final String[] COMPANION_KEYWORDS_FRIENDS = {
        "친구", "동료", "모임", "단체", "동창", "우정"
    };
}