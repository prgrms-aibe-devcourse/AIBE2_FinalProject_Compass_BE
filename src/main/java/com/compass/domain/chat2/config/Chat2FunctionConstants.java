package com.compass.domain.chat2.config;

/**
 * Chat2FunctionConstants - CHAT2 Function 관련 상수
 */
public final class Chat2FunctionConstants {

    private Chat2FunctionConstants() {
        // 유틸리티 클래스 생성 방지
    }

    // 신뢰도 상수
    public static final double DEFAULT_CONFIDENCE = 0.85;

    // 상태 코드
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_ERROR = "ERROR";

    // 에러 코드
    public static final String ERROR_CODE_USER_INPUT = "CHAT2_007";
    public static final String ERROR_CODE_FOLLOW_UP = "CHAT2_009";

    // 에러 메시지
    public static final String ERROR_MSG_USER_INPUT = "입력 분석 중 오류가 발생했습니다";
    public static final String ERROR_MSG_FOLLOW_UP = "Follow-up 질문 생성 중 오류가 발생했습니다";

    // 로그 메시지
    public static final String LOG_ANALYZE_START = "🔍 사용자 입력 분석 시작 - ThreadId: {}";
    public static final String LOG_ANALYZE_ERROR = "사용자 입력 분석 실패";
    public static final String LOG_FOLLOW_UP_START = "🔄 Follow-up 프로세스 시작 - ThreadId: {}";
    public static final String LOG_FOLLOW_UP_ERROR = "Follow-up 시작 실패";
    public static final String LOG_INTENT_FUNCTION = "📊 Intent 분류 Function 호출";
    public static final String LOG_RESPONSE_GENERATION = "📝 최종 응답 생성";

    // Function 설명
    public static final String DESC_ANALYZE_INPUT = "사용자 입력에서 여행 정보(목적지, 날짜, 예산 등)를 추출합니다";
    public static final String DESC_START_FOLLOW_UP = "누락된 여행 정보 수집을 위한 Follow-up 질문을 시작합니다";
    public static final String DESC_CLASSIFY_INTENT = "사용자 입력의 의도(Intent)를 분류합니다";
    public static final String DESC_GENERATE_RESPONSE = "처리 결과를 바탕으로 사용자에게 전달할 최종 응답을 생성합니다";

    // 로거 초기화 메시지
    public static final String LOG_FUNCTIONS_REGISTERED = "✅ CHAT2 Functions 등록 완료:";
    public static final String LOG_FUNCTION_ANALYZE = "  - analyzeUserInput: 사용자 입력 분석";
    public static final String LOG_FUNCTION_FOLLOW_UP = "  - startFollowUp: Follow-up 질문 시작";
    public static final String LOG_FUNCTION_INTENT = "  - classifyIntent: Intent 분류";
    public static final String LOG_FUNCTION_RESPONSE = "  - generateFinalResponse: 최종 응답 생성";

    // 응답 템플릿
    public static final String RESPONSE_TRAVEL_PLAN = "🎉 여행 계획이 완성되었습니다!\n\n%s\n\n즐거운 여행 되세요!";
    public static final String RESPONSE_FOLLOW_UP = "여행 계획을 완성하기 위해 추가 정보가 필요합니다.\n\n%s";
    public static final String RESPONSE_DEFAULT = "요청을 처리했습니다.";
}