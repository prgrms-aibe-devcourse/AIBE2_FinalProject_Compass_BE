package com.compass.domain.chat.model.enums;

// 여행 계획 워크플로우의 5단계 정의
public enum TravelPhase {

    // Phase 1: 초기화 - 인사, 소개, 여행 계획 시작 유도
    INITIALIZATION,

    // Phase 2: 정보 수집 - 목적지, 날짜, 예산, 인원 수집
    INFORMATION_COLLECTION,

    // Phase 3: 계획 생성 - AI가 맞춤형 여행 일정 작성
    PLAN_GENERATION,

    // Phase 4: 피드백 반영 - 사용자 요청으로 계획 수정 (반복 가능)
    FEEDBACK_REFINEMENT,

    // Phase 5: 완료 - 최종 확정, 저장, 공유
    COMPLETION
}
