package com.compass.domain.trip.service;

import com.compass.domain.trip.entity.TourPlace;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

/**
 * 임시 SearchService (REQ-SEARCH-001에서 구현됨)
 * REQ-SEARCH-004 통합 검색을 위한 임시 구현체
 */
@Service
@Slf4j
public class SearchService {

    /**
     * 전문검색 (임시 구현)
     */
    public Page<TourPlace> fullTextSearch(String query, int offset, int limit) {
        log.info("임시 RDS 전문검색: query={}, offset={}, limit={}", query, offset, limit);
        // TODO: REQ-SEARCH-001에서 실제 구현
        return Page.empty();
    }

    /**
     * 카테고리 필터 전문검색 (임시 구현)
     */
    public Page<TourPlace> fullTextSearchWithCategory(String query, String category, int offset, int limit) {
        log.info("임시 RDS 카테고리 전문검색: query={}, category={}, offset={}, limit={}", query, category, offset, limit);
        // TODO: REQ-SEARCH-001에서 실제 구현
        return Page.empty();
    }

    /**
     * 지역 필터 전문검색 (임시 구현)
     */
    public Page<TourPlace> fullTextSearchWithArea(String query, String areaCode, int offset, int limit) {
        log.info("임시 RDS 지역 전문검색: query={}, areaCode={}, offset={}, limit={}", query, areaCode, offset, limit);
        // TODO: REQ-SEARCH-001에서 실제 구현
        return Page.empty();
    }

    /**
     * 복합 필터 전문검색 (임시 구현)
     */
    public Page<TourPlace> fullTextSearchWithFilters(String query, String category, String areaCode, int offset, int limit) {
        log.info("임시 RDS 복합 필터 전문검색: query={}, category={}, areaCode={}, offset={}, limit={}", 
                query, category, areaCode, offset, limit);
        // TODO: REQ-SEARCH-001에서 실제 구현
        return Page.empty();
    }
}
