package com.compass.domain.chat.constant;

/**
 * 꼬리질문 플로우 관련 상수 정의
 * REQ-FOLLOW 요구사항 구현을 위한 중앙화된 상수 관리
 */
public final class FollowUpConstants {
    
    private FollowUpConstants() {
        throw new AssertionError("Utility class should not be instantiated");
    }
    
    // Session Management Constants
    public static final String SESSION_PREFIX = "follow_up_session:";
    public static final int SESSION_TTL_MINUTES = 30;
    public static final String SESSION_ID_PREFIX = "session_";
    
    // Progress Calculation Constants
    public static final int TOTAL_REQUIRED_FIELDS = 6;
    public static final int PROGRESS_PERCENTAGE_MULTIPLIER = 100;
    
    // Question Types
    public static final String QUESTION_TYPE_FOLLOW_UP = "follow-up";
    public static final String QUESTION_TYPE_CLARIFICATION = "clarification";
    public static final String QUESTION_TYPE_COMPLETE = "complete";
    
    // Input Types
    public static final String INPUT_TYPE_TEXT = "text";
    public static final String INPUT_TYPE_DATE = "date";
    public static final String INPUT_TYPE_NUMBER = "number";
    public static final String INPUT_TYPE_SELECT = "select";
    
    // Validation Thresholds
    public static final int MIN_FIELDS_FOR_PLAN_GENERATION = 3; // 목적지, 날짜/기간, 동행자
    public static final double COMPLETION_THRESHOLD_FOR_PLAN = 0.5; // 50% 이상 완료 시 계획 생성 가능
    
    // Message Templates
    public static final String SESSION_EXPIRED_MESSAGE = "세션이 만료되었습니다. 새로 시작해주세요.";
    public static final String COMPLETION_MESSAGE = "여행 정보 수집이 완료되었습니다! 🎉";
    public static final String VALIDATION_SUCCESS_MESSAGE = "모든 필수 정보가 올바르게 입력되었습니다.";
    
    // Field Display Names (Korean)
    public static final String FIELD_ORIGIN = "출발지";
    public static final String FIELD_DESTINATION = "목적지";
    public static final String FIELD_START_DATE = "출발일";
    public static final String FIELD_END_DATE = "도착일";
    public static final String FIELD_DURATION = "여행 기간";
    public static final String FIELD_COMPANIONS = "동행자";
    public static final String FIELD_BUDGET = "예산";
    
    // Companion Types
    public static final String COMPANION_SOLO = "solo";
    public static final String COMPANION_COUPLE = "couple";
    public static final String COMPANION_FAMILY = "family";
    public static final String COMPANION_FRIENDS = "friends";
    public static final String COMPANION_BUSINESS = "business";
    
    // Budget Levels
    public static final String BUDGET_ECONOMY = "budget";
    public static final String BUDGET_MODERATE = "moderate";
    public static final String BUDGET_LUXURY = "luxury";
}