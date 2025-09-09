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
}