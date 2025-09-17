package com.compass.domain.chat.model.enums;

// 사용자 의도 분류
public enum Intent {
    // 기본 대화
    GENERAL_CHAT,           // 일반 대화
    TRAVEL_QUESTION,        // 여행 관련 질문 (정보 제공)
    UNKNOWN,                // 분류 불가

    // Phase 1: 초기화
    TRAVEL_PLANNING,        // 여행 계획 시작

    // Phase 2: 정보 수집
    TRAVEL_INFO_COLLECTION, // 여행 계획을 위한 정보 수집
    IMAGE_UPLOAD,          // 이미지 업로드 (여행 관련)

    // Phase 3: 계획 생성
    ITINERARY_GENERATION,   // 여행 일정 생성
    DESTINATION_SEARCH,     // 목적지 검색

    // Phase 4: 피드백 및 수정
    ITINERARY_ADJUSTMENT,   // 일정 수정
    FEEDBACK_REFINEMENT,    // 피드백 기반 개선

    // Phase 5: 완료
    PLAN_FINALIZATION,      // 계획 최종 확정
    SAVE_AND_EXPORT        // 저장 및 내보내기
}
