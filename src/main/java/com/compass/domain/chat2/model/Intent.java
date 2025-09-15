package com.compass.domain.chat2.model;

/**
 * Intent - 사용자 요청의 의도 분류
 *
 * REQ-CHAT2-004: Intent 분류를 위한 열거형
 */
public enum Intent {
    /**
     * 여행 계획 생성 요청
     */
    TRAVEL_PLANNING,

    /**
     * 정보 수집 (Follow-up 필요)
     */
    INFORMATION_COLLECTION,

    /**
     * 이미지 업로드 (OCR 처리)
     */
    IMAGE_UPLOAD,

    /**
     * 일반 질문 (여행과 무관)
     */
    GENERAL_QUESTION,

    /**
     * 빠른 입력 폼
     */
    QUICK_INPUT,

    /**
     * 여행지 검색
     */
    DESTINATION_SEARCH,

    /**
     * 예약 정보 처리
     */
    RESERVATION_PROCESSING,

    /**
     * API 사용량 조회
     */
    API_USAGE_CHECK,

    /**
     * 알 수 없는 의도
     */
    UNKNOWN
}