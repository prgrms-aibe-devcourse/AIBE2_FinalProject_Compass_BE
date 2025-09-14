package com.compass.domain.trip.controller;

import com.compass.domain.trip.entity.TourPlace;
import com.compass.domain.trip.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * 관광지 검색 컨트롤러
 * REQ-SEARCH-001: RDS 검색 시스템 (PostgreSQL 전문검색)
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Search", description = "관광지 검색 API")
public class SearchController {

    private final SearchService searchService;

    /**
     * PostgreSQL 전문검색 - 기본 검색
     */
    @GetMapping("/places/fulltext")
    @Operation(summary = "전문검색", description = "PostgreSQL 전문검색을 통한 관광지 검색")
    public ResponseEntity<Page<TourPlace>> fullTextSearch(
            @Parameter(description = "검색 키워드", required = true)
            @RequestParam String query,
            @PageableDefault(size = 20) Pageable pageable) {
        
        log.info("전문검색 API 호출: query={}, page={}, size={}", 
                query, pageable.getPageNumber(), pageable.getPageSize());
        
        Page<TourPlace> results = searchService.fullTextSearch(query, pageable);
        
        return ResponseEntity.ok(results);
    }

    /**
     * PostgreSQL 전문검색 - 카테고리 필터
     */
    @GetMapping("/places/fulltext/category")
    @Operation(summary = "카테고리 필터 검색", description = "카테고리 필터를 적용한 전문검색")
    public ResponseEntity<Page<TourPlace>> fullTextSearchWithCategory(
            @Parameter(description = "검색 키워드", required = true)
            @RequestParam String query,
            @Parameter(description = "카테고리", required = true)
            @RequestParam String category,
            @PageableDefault(size = 20) Pageable pageable) {
        
        log.info("카테고리 필터 검색 API 호출: query={}, category={}, page={}, size={}", 
                query, category, pageable.getPageNumber(), pageable.getPageSize());
        
        Page<TourPlace> results = searchService.fullTextSearchWithCategory(query, category, pageable);
        
        return ResponseEntity.ok(results);
    }

    /**
     * PostgreSQL 전문검색 - 지역 필터
     */
    @GetMapping("/places/fulltext/area")
    @Operation(summary = "지역 필터 검색", description = "지역 필터를 적용한 전문검색")
    public ResponseEntity<Page<TourPlace>> fullTextSearchWithArea(
            @Parameter(description = "검색 키워드", required = true)
            @RequestParam String query,
            @Parameter(description = "지역 코드", required = true)
            @RequestParam String areaCode,
            @PageableDefault(size = 20) Pageable pageable) {
        
        log.info("지역 필터 검색 API 호출: query={}, areaCode={}, page={}, size={}", 
                query, areaCode, pageable.getPageNumber(), pageable.getPageSize());
        
        Page<TourPlace> results = searchService.fullTextSearchWithArea(query, areaCode, pageable);
        
        return ResponseEntity.ok(results);
    }

    /**
     * PostgreSQL 전문검색 - 복합 필터 (카테고리 + 지역)
     */
    @GetMapping("/places/fulltext/filters")
    @Operation(summary = "복합 필터 검색", description = "카테고리와 지역 필터를 모두 적용한 전문검색")
    public ResponseEntity<Page<TourPlace>> fullTextSearchWithFilters(
            @Parameter(description = "검색 키워드", required = true)
            @RequestParam String query,
            @Parameter(description = "카테고리")
            @RequestParam(required = false) String category,
            @Parameter(description = "지역 코드")
            @RequestParam(required = false) String areaCode,
            @PageableDefault(size = 20) Pageable pageable) {
        
        log.info("복합 필터 검색 API 호출: query={}, category={}, areaCode={}, page={}, size={}", 
                query, category, areaCode, pageable.getPageNumber(), pageable.getPageSize());
        
        Page<TourPlace> results = searchService.fullTextSearchWithFilters(query, category, areaCode, pageable);
        
        return ResponseEntity.ok(results);
    }

    /**
     * 근거리 검색 - PostgreSQL earthdistance 확장 활용
     */
    @GetMapping("/places/nearby")
    @Operation(summary = "근거리 검색", description = "위치 기반 근거리 관광지 검색")
    public ResponseEntity<Page<TourPlace>> searchNearby(
            @Parameter(description = "위도", required = true)
            @RequestParam Double latitude,
            @Parameter(description = "경도", required = true)
            @RequestParam Double longitude,
            @Parameter(description = "검색 반경 (km)", required = true)
            @RequestParam Double radiusKm,
            @PageableDefault(size = 20) Pageable pageable) {
        
        log.info("근거리 검색 API 호출: lat={}, lng={}, radius={}km, page={}, size={}", 
                latitude, longitude, radiusKm, pageable.getPageNumber(), pageable.getPageSize());
        
        Page<TourPlace> results = searchService.searchNearby(latitude, longitude, radiusKm, pageable);
        
        return ResponseEntity.ok(results);
    }

    /**
     * 이름으로 관광지 검색 (LIKE 검색)
     */
    @GetMapping("/places/name")
    @Operation(summary = "이름 검색", description = "관광지명으로 LIKE 검색")
    public ResponseEntity<List<TourPlace>> searchByName(
            @Parameter(description = "관광지명", required = true)
            @RequestParam String name) {
        
        log.info("이름 검색 API 호출: name={}", name);
        
        List<TourPlace> results = searchService.searchByName(name);
        
        return ResponseEntity.ok(results);
    }

    /**
     * Content ID로 관광지 조회
     */
    @GetMapping("/places/{contentId}")
    @Operation(summary = "Content ID 조회", description = "Content ID로 특정 관광지 조회")
    public ResponseEntity<TourPlace> getByContentId(
            @Parameter(description = "Content ID", required = true)
            @PathVariable String contentId) {
        
        log.info("Content ID 조회 API 호출: contentId={}", contentId);
        
        Optional<TourPlace> result = searchService.getByContentId(contentId);
        
        return result.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 카테고리별 관광지 조회
     */
    @GetMapping("/places/category/{category}")
    @Operation(summary = "카테고리별 조회", description = "특정 카테고리의 모든 관광지 조회")
    public ResponseEntity<List<TourPlace>> getByCategory(
            @Parameter(description = "카테고리", required = true)
            @PathVariable String category) {
        
        log.info("카테고리별 조회 API 호출: category={}", category);
        
        List<TourPlace> results = searchService.getByCategory(category);
        
        return ResponseEntity.ok(results);
    }

    /**
     * 지역별 관광지 조회
     */
    @GetMapping("/places/area/{areaCode}")
    @Operation(summary = "지역별 조회", description = "특정 지역의 모든 관광지 조회")
    public ResponseEntity<List<TourPlace>> getByAreaCode(
            @Parameter(description = "지역 코드", required = true)
            @PathVariable String areaCode) {
        
        log.info("지역별 조회 API 호출: areaCode={}", areaCode);
        
        List<TourPlace> results = searchService.getByAreaCode(areaCode);
        
        return ResponseEntity.ok(results);
    }

    /**
     * 통합 검색 - 우선순위 기반 검색
     */
    @GetMapping("/places/integrated")
    @Operation(summary = "통합 검색", description = "우선순위 기반 통합 검색 (전문검색 → 이름검색 → 카테고리검색)")
    public ResponseEntity<Page<TourPlace>> integratedSearch(
            @Parameter(description = "검색 키워드", required = true)
            @RequestParam String query,
            @Parameter(description = "카테고리")
            @RequestParam(required = false) String category,
            @Parameter(description = "지역 코드")
            @RequestParam(required = false) String areaCode,
            @PageableDefault(size = 20) Pageable pageable) {
        
        log.info("통합 검색 API 호출: query={}, category={}, areaCode={}, page={}, size={}", 
                query, category, areaCode, pageable.getPageNumber(), pageable.getPageSize());
        
        Page<TourPlace> results = searchService.integratedSearch(query, category, areaCode, pageable);
        
        return ResponseEntity.ok(results);
    }

    /**
     * 검색 통계 - 인기 카테고리
     */
    @GetMapping("/stats/categories")
    @Operation(summary = "인기 카테고리 통계", description = "카테고리별 관광지 개수 통계")
    public ResponseEntity<List<Object[]>> getPopularCategories() {
        log.info("인기 카테고리 통계 API 호출");
        
        List<Object[]> results = searchService.getPopularCategories();
        
        return ResponseEntity.ok(results);
    }

    /**
     * 검색 통계 - 지역별 관광지 분포
     */
    @GetMapping("/stats/areas")
    @Operation(summary = "지역별 분포 통계", description = "지역별 관광지 개수 분포 통계")
    public ResponseEntity<List<Object[]>> getAreaDistribution() {
        log.info("지역별 분포 통계 API 호출");
        
        List<Object[]> results = searchService.getAreaDistribution();
        
        return ResponseEntity.ok(results);
    }
}
