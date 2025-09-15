package com.compass.domain.trip.enums;

/**
 * 크롤링 상태 타입
 * REQ-CRAWL-002: Phase별 크롤링 상태 관리
 */
public enum CrawlStatusType {
    
    /**
     * 대기 중
     */
    PENDING("대기 중"),
    
    /**
     * 진행 중
     */
    IN_PROGRESS("진행 중"),
    
    /**
     * 완료
     */
    COMPLETED("완료"),
    
    /**
     * 실패
     */
    FAILED("실패"),
    
    /**
     * 일시 중지
     */
    PAUSED("일시 중지"),
    
    /**
     * 취소됨
     */
    CANCELLED("취소됨");

    private final String description;

    CrawlStatusType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

