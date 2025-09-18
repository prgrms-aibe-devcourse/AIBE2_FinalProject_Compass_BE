package com.compass.domain.chat.model.enums;

// 사용자 의도 분류 체계 - 9개 카테고리
public enum Intent {
    // Phase 1: 초기화
    TRAVEL_PLANNING,         // 새로운 여행 계획 시작 요청

    // Phase 2: 정보 수집
    INFORMATION_COLLECTION,  // 여행 정보 입력 및 수집
    IMAGE_UPLOAD,           // 이미지 업로드 및 OCR 처리

    // Phase 3: 계획 생성
    DESTINATION_SEARCH,     // 목적지 검색 및 추천

    // Phase 4: 피드백 및 수정
    PLAN_MODIFICATION,      // 기존 여행 계획 수정 요청
    FEEDBACK,              // 사용자 피드백 및 개선 요청

    // Phase 5: 완료
    COMPLETION,            // 여행 계획 완료 및 저장

    // Phase 무관: 일반 기능
    GENERAL_QUESTION,       // 여행 관련 일반적인 질문
    WEATHER_INQUIRY        // 날씨 관련 문의
}
