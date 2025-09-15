package com.compass.domain.chat2.model;

/**
 * 사용자 의도 분류를 위한 Intent 열거형
 * REQ-CHAT2-004: Intent 분류 시스템 (여행/일반 구분)
 */
public enum Intent {
    
    /**
     * 여행 계획 생성 요청
     */
    TRAVEL_PLANNING("여행 계획 생성"),
    
    /**
     * 정보 수집 (Follow-up 질문)
     */
    INFORMATION_COLLECTION("정보 수집"),
    
    /**
     * 이미지 업로드 및 OCR 처리
     */
    IMAGE_UPLOAD("이미지 업로드"),
    
    /**
     * 일반적인 질문 (날씨, 환율 등)
     */
    GENERAL_QUESTION("일반 질문"),
    
    /**
     * 빠른 입력 폼 처리
     */
    QUICK_INPUT("빠른 입력"),
    
    /**
     * 여행지 검색
     */
    DESTINATION_SEARCH("여행지 검색"),
    
    /**
     * 예약 정보 처리
     */
    RESERVATION_PROCESSING("예약 처리"),
    
    /**
     * API 사용량 확인
     */
    API_USAGE_CHECK("API 사용량 확인"),
    
    /**
     * 분류 불가능한 의도
     */
    UNKNOWN("알 수 없음");
    
    private final String description;
    
    Intent(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 여행 관련 Intent인지 확인
     */
    public boolean isTravelRelated() {
        return this == TRAVEL_PLANNING || 
               this == INFORMATION_COLLECTION || 
               this == DESTINATION_SEARCH || 
               this == RESERVATION_PROCESSING;
    }
    
    /**
     * 일반 대화 Intent인지 확인
     */
    public boolean isGeneralConversation() {
        return this == GENERAL_QUESTION || 
               this == QUICK_INPUT || 
               this == API_USAGE_CHECK;
    }
}
