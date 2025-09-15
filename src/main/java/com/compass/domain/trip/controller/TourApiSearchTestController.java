package com.compass.domain.trip.controller;

import com.compass.domain.trip.dto.TourApiResponse;
import com.compass.domain.trip.service.TourApiSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Tour API 검색 테스트 컨트롤러
 * REQ-SEARCH-002: Tour API 검색 테스트용 엔드포인트
 */
@RestController
@RequestMapping("/api/test/tour/search")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Tour API Search Test", description = "Tour API 검색 테스트용 API")
public class TourApiSearchTestController {

    private final TourApiSearchService tourApiSearchService;

    /**
     * 키워드 검색 테스트
     */
    @GetMapping("/keyword/{keyword}")
    @Operation(summary = "키워드 검색 테스트", description = "Tour API 키워드 검색 테스트")
    public ResponseEntity<TourApiResponse> testKeywordSearch(@PathVariable String keyword) {
        log.info("Tour API 키워드 검색 테스트: keyword={}", keyword);
        
        Optional<TourApiResponse> response = tourApiSearchService.searchByKeyword(keyword, "1", "12");
        
        if (response.isPresent()) {
            String statistics = tourApiSearchService.getSearchStatistics(response.get());
            log.info("Tour API 키워드 검색 테스트 성공: {}", statistics);
            return ResponseEntity.ok(response.get());
        } else {
            log.warn("Tour API 키워드 검색 테스트 실패: keyword={}", keyword);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 지역 검색 테스트
     */
    @GetMapping("/area/{areaCode}")
    @Operation(summary = "지역 검색 테스트", description = "Tour API 지역 검색 테스트")
    public ResponseEntity<TourApiResponse> testAreaSearch(@PathVariable String areaCode) {
        log.info("Tour API 지역 검색 테스트: areaCode={}", areaCode);
        
        Optional<TourApiResponse> response = tourApiSearchService.searchByArea(areaCode, "12", 1, 10);
        
        if (response.isPresent()) {
            String statistics = tourApiSearchService.getSearchStatistics(response.get());
            log.info("Tour API 지역 검색 테스트 성공: {}", statistics);
            return ResponseEntity.ok(response.get());
        } else {
            log.warn("Tour API 지역 검색 테스트 실패: areaCode={}", areaCode);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 위치 검색 테스트 (서울 시청 기준)
     */
    @GetMapping("/location/seoul")
    @Operation(summary = "위치 검색 테스트", description = "Tour API 위치 검색 테스트 (서울 시청 기준)")
    public ResponseEntity<TourApiResponse> testLocationSearch() {
        log.info("Tour API 위치 검색 테스트: 서울 시청 기준");
        
        // 서울 시청 좌표: 126.9780, 37.5665
        Optional<TourApiResponse> response = tourApiSearchService.searchByLocation("126.9780", "37.5665", 5000, "12");
        
        if (response.isPresent()) {
            String statistics = tourApiSearchService.getSearchStatistics(response.get());
            log.info("Tour API 위치 검색 테스트 성공: {}", statistics);
            return ResponseEntity.ok(response.get());
        } else {
            log.warn("Tour API 위치 검색 테스트 실패");
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 통합 검색 테스트
     */
    @GetMapping("/integrated/{keyword}")
    @Operation(summary = "통합 검색 테스트", description = "Tour API 통합 검색 테스트")
    public ResponseEntity<TourApiResponse> testIntegratedSearch(@PathVariable String keyword) {
        log.info("Tour API 통합 검색 테스트: keyword={}", keyword);
        
        Optional<TourApiResponse> response = tourApiSearchService.integratedSearch(keyword, "1", "12");
        
        if (response.isPresent()) {
            String statistics = tourApiSearchService.getSearchStatistics(response.get());
            log.info("Tour API 통합 검색 테스트 성공: {}", statistics);
            return ResponseEntity.ok(response.get());
        } else {
            log.warn("Tour API 통합 검색 테스트 실패: keyword={}", keyword);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 검색 통계 테스트
     */
    @GetMapping("/statistics/{keyword}")
    @Operation(summary = "검색 통계 테스트", description = "Tour API 검색 통계 테스트")
    public ResponseEntity<String> testSearchStatistics(@PathVariable String keyword) {
        log.info("Tour API 검색 통계 테스트: keyword={}", keyword);
        
        Optional<TourApiResponse> response = tourApiSearchService.searchByKeyword(keyword, "1", "12");
        
        if (response.isPresent()) {
            String statistics = tourApiSearchService.getSearchStatistics(response.get());
            log.info("Tour API 검색 통계 테스트 성공: {}", statistics);
            return ResponseEntity.ok(statistics);
        } else {
            log.warn("Tour API 검색 통계 테스트 실패: keyword={}", keyword);
            return ResponseEntity.ok("검색 결과 없음");
        }
    }
}
