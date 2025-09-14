package com.compass.domain.trip.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 통합 검색 응답 DTO
 * REQ-SEARCH-004: 통합 검색 서비스
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegratedSearchResponse {
    
    /**
     * 검색 키워드
     */
    private String keyword;
    
    /**
     * 전체 검색 결과 수
     */
    private Integer totalCount;
    
    /**
     * 현재 페이지
     */
    private Integer currentPage;
    
    /**
     * 전체 페이지 수
     */
    private Integer totalPages;
    
    /**
     * 페이지 크기
     */
    private Integer pageSize;
    
    /**
     * 검색 소요 시간 (밀리초)
     */
    private Long searchTimeMs;
    
    /**
     * 검색 결과 목록
     */
    private List<SearchResult> results;
    
    /**
     * 검색 시스템별 통계
     */
    private Map<String, SearchSystemStats> systemStats;
    
    /**
     * 검색 메타데이터
     */
    private SearchMetadata metadata;
    
    /**
     * 개별 검색 결과
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchResult {
        
        /**
         * 장소 ID
         */
        private String id;
        
        /**
         * 장소명
         */
        private String name;
        
        /**
         * 주소
         */
        private String address;
        
        /**
         * 카테고리
         */
        private String category;
        
        /**
         * 경도
         */
        private Double longitude;
        
        /**
         * 위도
         */
        private Double latitude;
        
        /**
         * 거리 (미터)
         */
        private Double distance;
        
        /**
         * 검색 시스템 (RDS, TOUR_API, KAKAO_MAP)
         */
        private String searchSystem;
        
        /**
         * 신뢰도 점수 (0.0 ~ 1.0)
         */
        private Double confidenceScore;
        
        /**
         * 추가 정보
         */
        private Map<String, Object> additionalInfo;
    }
    
    /**
     * 검색 시스템별 통계
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchSystemStats {
        
        /**
         * 검색 시스템명
         */
        private String systemName;
        
        /**
         * 검색 결과 수
         */
        private Integer resultCount;
        
        /**
         * 검색 소요 시간 (밀리초)
         */
        private Long searchTimeMs;
        
        /**
         * 검색 성공 여부
         */
        private Boolean success;
        
        /**
         * 오류 메시지 (실패 시)
         */
        private String errorMessage;
    }
    
    /**
     * 검색 메타데이터
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchMetadata {
        
        /**
         * 검색 타입
         */
        private String searchType;
        
        /**
         * 검색 우선순위
         */
        private String priority;
        
        /**
         * 사용된 검색 시스템 목록
         */
        private List<String> usedSystems;
        
        /**
         * 검색 필터 정보
         */
        private Map<String, Object> filters;
        
        /**
         * 검색 힌트
         */
        private List<String> searchHints;
    }
}
