package com.compass.domain.trip.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * 통합 검색 요청 DTO
 * REQ-SEARCH-004: 통합 검색 서비스
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegratedSearchRequest {
    
    /**
     * 검색 키워드
     */
    private String keyword;
    
    /**
     * 검색 타입 (ALL, RDS, TOUR_API, KAKAO_MAP)
     */
    @Builder.Default
    private SearchType searchType = SearchType.ALL;
    
    /**
     * 카테고리 필터
     */
    private String category;
    
    /**
     * 지역 코드
     */
    private String areaCode;
    
    /**
     * 경도 (위치 기반 검색용)
     */
    private Double longitude;
    
    /**
     * 위도 (위치 기반 검색용)
     */
    private Double latitude;
    
    /**
     * 검색 반경 (미터)
     */
    @Builder.Default
    private Integer radius = 5000;
    
    /**
     * 페이지 번호
     */
    @Builder.Default
    private Integer page = 1;
    
    /**
     * 페이지 크기
     */
    @Builder.Default
    private Integer size = 15;
    
    /**
     * 정렬 방식
     */
    @Builder.Default
    private SortType sort = SortType.ACCURACY;
    
    /**
     * 검색 우선순위 설정
     */
    @Builder.Default
    private SearchPriority priority = SearchPriority.RDS_FIRST;
    
    /**
     * 검색 타입 열거형
     */
    public enum SearchType {
        ALL,        // 모든 검색 시스템 사용
        RDS,        // RDS 검색만
        TOUR_API,   // Tour API 검색만
        KAKAO_MAP   // Kakao Map API 검색만
    }
    
    /**
     * 정렬 타입 열거형
     */
    public enum SortType {
        ACCURACY,   // 정확도 순
        DISTANCE,   // 거리 순
        POPULARITY  // 인기도 순
    }
    
    /**
     * 검색 우선순위 열거형
     */
    public enum SearchPriority {
        RDS_FIRST,      // RDS 우선, 나머지 폴백
        TOUR_API_FIRST, // Tour API 우선, 나머지 폴백
        KAKAO_MAP_FIRST // Kakao Map API 우선, 나머지 폴백
    }
}
