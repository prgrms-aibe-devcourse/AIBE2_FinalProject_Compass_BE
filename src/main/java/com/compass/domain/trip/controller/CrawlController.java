package com.compass.domain.trip.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.compass.domain.trip.entity.CrawlStatus;
import com.compass.domain.trip.entity.TourPlace;
import com.compass.domain.trip.repository.CrawlStatusRepository;
import com.compass.domain.trip.repository.TourPlaceRepository;
import com.compass.domain.trip.service.CrawlService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 크롤링 관리 컨트롤러
 * REQ-CRAWL-002: Phase별 크롤링 API
 */
@RestController
@RequestMapping("/api/crawl")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Crawl Management", description = "크롤링 관리 API")
public class CrawlController {

    private final CrawlService crawlService;
    private final CrawlStatusRepository crawlStatusRepository;
    private final TourPlaceRepository tourPlaceRepository;

    /**
     * 전체 지역 크롤링 시작
     */
    @PostMapping("/start")
    @Operation(summary = "전체 지역 크롤링 시작", 
               description = "서울, 부산, 제주 지역의 모든 카테고리 데이터를 크롤링합니다.")
    public ResponseEntity<Map<String, Object>> startFullCrawling() {
        log.info("전체 지역 크롤링 시작 요청");
        
        try {
            CompletableFuture<Void> future = crawlService.startFullCrawling();
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "전체 지역 크롤링이 시작되었습니다.");
            response.put("status", "STARTED");
            response.put("areas", List.of("서울", "부산", "제주"));
            response.put("contentTypes", List.of("관광지", "문화시설", "음식점", "쇼핑", "레포츠", "숙박"));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("전체 지역 크롤링 시작 실패", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "크롤링 시작에 실패했습니다: " + e.getMessage());
            response.put("status", "FAILED");
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 특정 지역 크롤링 시작
     */
    @PostMapping("/start/{areaCode}")
    @Operation(summary = "특정 지역 크롤링 시작", 
               description = "지정된 지역의 모든 카테고리 데이터를 크롤링합니다.")
    public ResponseEntity<Map<String, Object>> startCrawlingByArea(
            @Parameter(description = "지역 코드 (1: 서울, 6: 부산, 39: 제주)") 
            @PathVariable String areaCode) {
        
        log.info("지역 {} 크롤링 시작 요청", areaCode);
        
        // 지역 코드 유효성 검사
        Map<String, String> areaMap = Map.of("1", "서울", "6", "부산", "39", "제주");
        if (!areaMap.containsKey(areaCode)) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "유효하지 않은 지역 코드입니다. (1: 서울, 6: 부산, 39: 제주)");
            response.put("status", "INVALID_AREA_CODE");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            CompletableFuture<Void> future = crawlService.startCrawlingByArea(areaCode);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", areaMap.get(areaCode) + " 지역 크롤링이 시작되었습니다.");
            response.put("status", "STARTED");
            response.put("areaCode", areaCode);
            response.put("areaName", areaMap.get(areaCode));
            response.put("contentTypes", List.of("관광지", "문화시설", "음식점", "쇼핑", "레포츠", "숙박"));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("지역 {} 크롤링 시작 실패", areaCode, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "크롤링 시작에 실패했습니다: " + e.getMessage());
            response.put("status", "FAILED");
            response.put("areaCode", areaCode);
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 크롤링 진행 상황 조회
     */
    @GetMapping("/status")
    @Operation(summary = "크롤링 진행 상황 조회", 
               description = "전체 크롤링 진행 상황을 조회합니다.")
    public ResponseEntity<Map<String, Object>> getCrawlStatus() {
        log.info("크롤링 진행 상황 조회 요청");
        
        try {
            // 전체 통계 조회
            Object[] overallStats = crawlStatusRepository.getOverallCrawlStatistics();
            
            // 지역별 통계 조회
            List<Object[]> areaStats = crawlStatusRepository.getCrawlStatisticsByArea();
            
            // 컨텐츠 타입별 통계 조회
            List<Object[]> contentTypeStats = crawlStatusRepository.getCrawlStatisticsByContentType();
            
            // 진행 중인 크롤링 조회
            List<CrawlStatus> inProgressCrawls = crawlStatusRepository.findInProgressCrawls();
            
            // 최근 완료된 크롤링 조회
            List<CrawlStatus> recentCompleted = crawlStatusRepository.findCompletedCrawls();
            
            Map<String, Object> response = new HashMap<>();
            response.put("overall", createOverallStats(overallStats));
            response.put("byArea", createAreaStats(areaStats));
            response.put("byContentType", createContentTypeStats(contentTypeStats));
            response.put("inProgress", inProgressCrawls);
            response.put("recentCompleted", recentCompleted.stream().limit(10).toList());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("크롤링 진행 상황 조회 실패", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "크롤링 진행 상황 조회에 실패했습니다: " + e.getMessage());
            response.put("status", "ERROR");
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 특정 지역 크롤링 상태 조회
     */
    @GetMapping("/status/{areaCode}")
    @Operation(summary = "특정 지역 크롤링 상태 조회", 
               description = "지정된 지역의 크롤링 상태를 조회합니다.")
    public ResponseEntity<Map<String, Object>> getCrawlStatusByArea(
            @Parameter(description = "지역 코드 (1: 서울, 6: 부산, 39: 제주)") 
            @PathVariable String areaCode) {
        
        log.info("지역 {} 크롤링 상태 조회 요청", areaCode);
        
        try {
            // 지역별 크롤링 상태 조회
            List<CrawlStatus> crawlStatuses = crawlStatusRepository.findByAreaCode(areaCode);
            
            // 지역별 통계 조회
            Long completedCount = crawlStatusRepository.countCompletedCrawlsByAreaCode(areaCode);
            Long totalCount = crawlStatusRepository.countTotalCrawlsByAreaCode(areaCode);
            
            // 최근 크롤링된 관광지 조회
            List<TourPlace> recentPlaces = tourPlaceRepository.findRecentlyCrawledByAreaCode(areaCode);
            
            Map<String, Object> response = new HashMap<>();
            response.put("areaCode", areaCode);
            response.put("crawlStatuses", crawlStatuses);
            response.put("statistics", Map.of(
                "completed", completedCount,
                "total", totalCount,
                "progress", totalCount > 0 ? (completedCount * 100) / totalCount : 0
            ));
            response.put("recentPlaces", recentPlaces.stream().limit(10).toList());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("지역 {} 크롤링 상태 조회 실패", areaCode, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "크롤링 상태 조회에 실패했습니다: " + e.getMessage());
            response.put("status", "ERROR");
            response.put("areaCode", areaCode);
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 간단한 크롤링 결과 조회 (H2 호환)
     */
    @GetMapping("/simple-results")
    @Operation(summary = "간단한 크롤링 결과 조회", 
               description = "H2 데이터베이스 호환 간단한 통계 정보를 조회합니다.")
    public ResponseEntity<Map<String, Object>> getSimpleCrawlResults() {
        log.info("간단한 크롤링 결과 조회 요청");
        
        try {
            // 전체 관광지 개수 (기본 메서드)
            long totalPlaces = tourPlaceRepository.count();
            
            // 모든 관광지 조회 (기본 메서드)
            List<TourPlace> allPlaces = tourPlaceRepository.findAll();
            
            Map<String, Object> response = new HashMap<>();
            response.put("totalPlaces", totalPlaces);
            response.put("message", "크롤링 결과 조회 성공");
            response.put("samplePlaces", allPlaces.stream().limit(5).toList());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("간단한 크롤링 결과 조회 실패", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "크롤링 결과 조회 실패: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 크롤링 결과 통계 조회
     */
    @GetMapping("/results")
    @Operation(summary = "크롤링 결과 통계 조회", 
               description = "크롤링된 데이터의 통계 정보를 조회합니다.")
    public ResponseEntity<Map<String, Object>> getCrawlResults() {
        log.info("크롤링 결과 통계 조회 요청");
        
        try {
            // 전체 관광지 개수
            long totalPlaces = tourPlaceRepository.count();
            
            // 지역별 관광지 개수
            List<Object[]> placesByArea = tourPlaceRepository.countByAreaCode();
            
            // 카테고리별 관광지 개수
            List<Object[]> placesByCategory = tourPlaceRepository.countByCategory();
            
            // 컨텐츠 타입별 관광지 개수
            List<Object[]> placesByContentType = tourPlaceRepository.countByContentTypeId();
            
            // 데이터 소스별 관광지 개수
            List<Object[]> placesByDataSource = tourPlaceRepository.countByDataSource();
            
            // 최근 크롤링된 관광지
            List<TourPlace> recentPlaces = tourPlaceRepository.findRecentlyCrawled();
            
            Map<String, Object> response = new HashMap<>();
            response.put("totalPlaces", totalPlaces);
            response.put("byArea", createPlacesByAreaStats(placesByArea));
            response.put("byCategory", createPlacesByCategoryStats(placesByCategory));
            response.put("byContentType", createPlacesByContentTypeStats(placesByContentType));
            response.put("byDataSource", createPlacesByDataSourceStats(placesByDataSource));
            response.put("recentPlaces", recentPlaces.stream().limit(20).toList());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("크롤링 결과 통계 조회 실패", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "크롤링 결과 통계 조회에 실패했습니다: " + e.getMessage());
            response.put("status", "ERROR");
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 전체 통계 생성
     */
    private Map<String, Object> createOverallStats(Object[] stats) {
        Map<String, Object> result = new HashMap<>();
        result.put("total", stats[0]);
        result.put("completed", stats[1]);
        result.put("failed", stats[2]);
        result.put("inProgress", stats[3]);
        result.put("pending", stats[4]);
        return result;
    }

    /**
     * 지역별 통계 생성
     */
    private List<Map<String, Object>> createAreaStats(List<Object[]> stats) {
        return stats.stream()
                .map(stat -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("areaCode", stat[0]);
                    result.put("areaName", stat[1]);
                    result.put("total", stat[2]);
                    result.put("completed", stat[3]);
                    result.put("failed", stat[4]);
                    result.put("inProgress", stat[5]);
                    return result;
                })
                .toList();
    }

    /**
     * 컨텐츠 타입별 통계 생성
     */
    private List<Map<String, Object>> createContentTypeStats(List<Object[]> stats) {
        return stats.stream()
                .map(stat -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("contentTypeId", stat[0]);
                    result.put("contentTypeName", stat[1]);
                    result.put("total", stat[2]);
                    result.put("completed", stat[3]);
                    result.put("failed", stat[4]);
                    result.put("inProgress", stat[5]);
                    return result;
                })
                .toList();
    }

    /**
     * 지역별 관광지 통계 생성
     */
    private List<Map<String, Object>> createPlacesByAreaStats(List<Object[]> stats) {
        return stats.stream()
                .map(stat -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("areaCode", stat[0]);
                    result.put("count", stat[1]);
                    return result;
                })
                .toList();
    }

    /**
     * 카테고리별 관광지 통계 생성
     */
    private List<Map<String, Object>> createPlacesByCategoryStats(List<Object[]> stats) {
        return stats.stream()
                .map(stat -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("category", stat[0]);
                    result.put("count", stat[1]);
                    return result;
                })
                .toList();
    }

    /**
     * 컨텐츠 타입별 관광지 통계 생성
     */
    private List<Map<String, Object>> createPlacesByContentTypeStats(List<Object[]> stats) {
        return stats.stream()
                .map(stat -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("contentTypeId", stat[0]);
                    result.put("count", stat[1]);
                    return result;
                })
                .toList();
    }

    /**
     * 데이터 소스별 관광지 통계 생성
     */
    private List<Map<String, Object>> createPlacesByDataSourceStats(List<Object[]> stats) {
        return stats.stream()
                .map(stat -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("dataSource", stat[0]);
                    result.put("count", stat[1]);
                    return result;
                })
                .toList();
    }
}

