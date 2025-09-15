package com.compass.domain.chat2.service;

/**
 * IntentClassificationConstants - Intent 분류 관련 상수
 */
public final class IntentClassificationConstants {

    private IntentClassificationConstants() {
        // 유틸리티 클래스 생성 방지
    }

    // LLM 프롬프트 템플릿 (최적화: 50토큰 목표)
    public static final String INTENT_CLASSIFICATION_PROMPT = """
        의도분류: {userInput}

        카테고리:
        TRAVEL_PLANNING(여행계획)
        INFORMATION_COLLECTION(정보수집)
        IMAGE_UPLOAD(이미지/OCR)
        GENERAL_QUESTION(일반질문)
        QUICK_INPUT(빠른입력)
        DESTINATION_SEARCH(여행지검색)
        RESERVATION_PROCESSING(예약처리)
        API_USAGE_CHECK(API확인)
        UNKNOWN(분류불가)

        응답: CATEGORY_NAME만
        """;

    // 로그 메시지
    public static final String LOG_INTENT_START = "Intent 분류 시작: {}";
    public static final String LOG_LLM_BASED_CLASSIFIED = "LLM Intent 분류 결과: {}";
    public static final String LOG_UNKNOWN_INTENT = "LLM이 알 수 없는 Intent 반환: {}";
    public static final String LOG_LLM_CLASSIFICATION_FAILED = "LLM Intent 분류 실패";
}