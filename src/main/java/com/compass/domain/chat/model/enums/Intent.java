package com.compass.domain.chat.model.enums;

// 사용자 의도 분류
public enum Intent {
    GENERAL_CHAT,           // 일반 대화
    TRAVEL_QUESTION,        // 여행 관련 질문 (정보 제공)
    TRAVEL_INFO_COLLECTION, // 여행 계획을 위한 정보 수집 시작
    UNKNOWN                 // 분류 불가
}
