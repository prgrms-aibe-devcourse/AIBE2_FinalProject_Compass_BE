package com.compass.domain.trip.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.compass.domain.trip.entity.TourPlace;
import com.compass.domain.trip.repository.TourPlaceRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 관광지 검색 서비스
 * REQ-SEARCH-001: RDS 검색 시스템 (PostgreSQL 전문검색)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SearchService {

    private final TourPlaceRepository tourPlaceRepository;

    /**
     * PostgreSQL 전문검색 - 기본 검색
     */
    public Page<TourPlace> fullTextSearch(String query, Pageable pageable) {
        log.info("전문검색 실행: query={}, page={}, size={}", query, pageable.getPageNumber(), pageable.getPageSize());
        
        int offset = (int) pageable.getOffset();
        int size = pageable.getPageSize();
        
        List<TourPlace> results = tourPlaceRepository.fullTextSearchWithPaging(query, offset, size);
        long total = tourPlaceRepository.countFullTextSearch(query);
        
        log.info("검색 결과: {}개 (전체 {}개)", results.size(), total);
        return new PageImpl<>(results, pageable, total);
    }

    /**
     * PostgreSQL 전문검색 - 카테고리 필터
     */
    public Page<TourPlace> fullTextSearchWithCategory(String query, String category, Pageable pageable) {
        log.info("카테고리 필터 검색: query={}, category={}, page={}, size={}", 
                query, category, pageable.getPageNumber(), pageable.getPageSize());
        
        int offset = (int) pageable.getOffset();
        int size = pageable.getPageSize();
        
        List<TourPlace> results = tourPlaceRepository.fullTextSearchWithCategory(query, category, offset, size);
        
        // 카테고리 필터링된 결과의 총 개수를 정확히 계산하기 위해 별도 쿼리 필요
        long total = tourPlaceRepository.countFullTextSearchWithCategory(query, category);
        
        log.info("카테고리 필터 검색 결과: {}개 (전체 {}개)", results.size(), total);
        return new PageImpl<>(results, pageable, total);
    }

    /**
     * PostgreSQL 전문검색 - 지역 필터
     */
    public Page<TourPlace> fullTextSearchWithArea(String query, String areaCode, Pageable pageable) {
        log.info("지역 필터 검색: query={}, areaCode={}, page={}, size={}", 
                query, areaCode, pageable.getPageNumber(), pageable.getPageSize());
        
        int offset = (int) pageable.getOffset();
        int size = pageable.getPageSize();
        
        List<TourPlace> results = tourPlaceRepository.fullTextSearchWithArea(query, areaCode, offset, size);
        long total = tourPlaceRepository.countFullTextSearchWithArea(query, areaCode);
        
        log.info("지역 필터 검색 결과: {}개 (전체 {}개)", results.size(), total);
        return new PageImpl<>(results, pageable, total);
    }

    /**
     * PostgreSQL 전문검색 - 복합 필터 (카테고리 + 지역)
     */
    public Page<TourPlace> fullTextSearchWithFilters(String query, String category, String areaCode, Pageable pageable) {
        log.info("복합 필터 검색: query={}, category={}, areaCode={}, page={}, size={}", 
                query, category, areaCode, pageable.getPageNumber(), pageable.getPageSize());
        
        int offset = (int) pageable.getOffset();
        int size = pageable.getPageSize();
        
        List<TourPlace> results = tourPlaceRepository.fullTextSearchWithFilters(query, category, areaCode, offset, size);
        long total = tourPlaceRepository.countFullTextSearchWithFilters(query, category, areaCode);
        
        log.info("복합 필터 검색 결과: {}개 (전체 {}개)", results.size(), total);
        return new PageImpl<>(results, pageable, total);
    }

    /**
     * 근거리 검색 - PostgreSQL earthdistance 확장 활용
     */
    public Page<TourPlace> searchNearby(Double latitude, Double longitude, Double radiusKm, Pageable pageable) {
        log.info("근거리 검색: lat={}, lng={}, radius={}km, page={}, size={}", 
                latitude, longitude, radiusKm, pageable.getPageNumber(), pageable.getPageSize());
        
        int offset = (int) pageable.getOffset();
        int size = pageable.getPageSize();
        
        List<TourPlace> results = tourPlaceRepository.findNearbyPlacesWithDistance(latitude, longitude, radiusKm, offset, size);
        
        // 근거리 검색의 총 개수는 별도 계산 필요 (복잡한 거리 계산이므로)
        long total = tourPlaceRepository.countNearbyPlaces(latitude, longitude, radiusKm);
        
        log.info("근거리 검색 결과: {}개 (전체 {}개)", results.size(), total);
        return new PageImpl<>(results, pageable, total);
    }

    /**
     * 이름으로 관광지 검색 (LIKE 검색)
     */
    public List<TourPlace> searchByName(String name) {
        log.info("이름 검색: name={}", name);
        
        List<TourPlace> results = tourPlaceRepository.searchByName(name);
        log.info("이름 검색 결과: {}개", results.size());
        
        return results;
    }

    /**
     * Content ID로 관광지 조회
     */
    public Optional<TourPlace> getByContentId(String contentId) {
        log.info("Content ID 조회: contentId={}", contentId);
        
        Optional<TourPlace> result = tourPlaceRepository.findByContentId(contentId);
        log.info("Content ID 조회 결과: {}", result.isPresent() ? "존재" : "없음");
        
        return result;
    }

    /**
     * 카테고리별 관광지 조회
     */
    public List<TourPlace> getByCategory(String category) {
        log.info("카테고리 조회: category={}", category);
        
        List<TourPlace> results = tourPlaceRepository.findByCategory(category);
        log.info("카테고리 조회 결과: {}개", results.size());
        
        return results;
    }

    /**
     * 지역별 관광지 조회
     */
    public List<TourPlace> getByAreaCode(String areaCode) {
        log.info("지역 조회: areaCode={}", areaCode);
        
        List<TourPlace> results = tourPlaceRepository.findByAreaCode(areaCode);
        log.info("지역 조회 결과: {}개", results.size());
        
        return results;
    }

    /**
     * 검색 통계 - 인기 카테고리
     */
    public List<Object[]> getPopularCategories() {
        log.info("인기 카테고리 통계 조회");
        
        List<Object[]> results = tourPlaceRepository.getPopularCategories();
        log.info("인기 카테고리 통계: {}개 카테고리", results.size());
        
        return results;
    }

    /**
     * 검색 통계 - 지역별 관광지 분포
     */
    public List<Object[]> getAreaDistribution() {
        log.info("지역별 관광지 분포 통계 조회");
        
        List<Object[]> results = tourPlaceRepository.getAreaDistribution();
        log.info("지역별 관광지 분포: {}개 지역", results.size());
        
        return results;
    }

    /**
     * 통합 검색 - 우선순위 기반 검색
     * 1순위: PostgreSQL 전문검색
     * 2순위: 이름 LIKE 검색
     * 3순위: 카테고리 검색
     */
    public Page<TourPlace> integratedSearch(String query, String category, String areaCode, Pageable pageable) {
        log.info("통합 검색 시작: query={}, category={}, areaCode={}", query, category, areaCode);
        
        // 1순위: PostgreSQL 전문검색
        Page<TourPlace> results;
        
        if (category != null && areaCode != null) {
            results = fullTextSearchWithFilters(query, category, areaCode, pageable);
        } else if (category != null) {
            results = fullTextSearchWithCategory(query, category, pageable);
        } else if (areaCode != null) {
            results = fullTextSearchWithArea(query, areaCode, pageable);
        } else {
            results = fullTextSearch(query, pageable);
        }
        
        // 전문검색 결과가 없으면 이름 LIKE 검색으로 폴백
        if (results.isEmpty() && query != null && !query.trim().isEmpty()) {
            log.info("전문검색 결과 없음, 이름 LIKE 검색으로 폴백");
            List<TourPlace> nameResults = searchByName(query);
            
            // 페이징 적용
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), nameResults.size());
            List<TourPlace> pagedResults = nameResults.subList(start, end);
            
            results = new PageImpl<>(pagedResults, pageable, nameResults.size());
        }
        
        log.info("통합 검색 완료: {}개 결과", results.getTotalElements());
        return results;
    }
}
