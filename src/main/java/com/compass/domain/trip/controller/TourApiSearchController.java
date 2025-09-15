package com.compass.domain.trip.controller;

import com.compass.domain.trip.dto.TourApiResponse;
import com.compass.domain.trip.service.TourApiSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Tour API 검색 컨트롤러
 * REQ-SEARCH-002: Tour API 검색 (실시간 API 호출)
 */
@RestController
@RequestMapping("/api/search/tour")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Tour API Search", description = "Tour API 실시간 검색 API")
public class TourApiSearchController {

    private final TourApiSearchService tourApiSearchService;

    /**
     * 키워드 기반 검색
     */
    @GetMapping("/keyword")
    @Operation(summary = "키워드 검색", description = "Tour API를 통한 키워드 기반 관광지 검색")
    public ResponseEntity<TourApiResponse> searchByKeyword(
            @Parameter(description = "검색 키워드", required = true)
            @RequestParam String keyword,
            @Parameter(description = "지역코드 (1=서울, 6=부산, 39=제주)")
            @RequestParam(required = false) String areaCode,
            @Parameter(description = "컨텐츠 타입 (12=관광지, 39=음식점, 38=쇼핑)")
            @RequestParam(required = false) String contentTypeId) {
        
        log.info("Tour API 키워드 검색 요청: keyword={}, areaCode={}, contentTypeId={}", 
                keyword, areaCode, contentTypeId);
        
        Optional<TourApiResponse> response = tourApiSearchService.searchByKeyword(keyword, areaCode, contentTypeId);
        
        if (response.isPresent()) {
            String statistics = tourApiSearchService.getSearchStatistics(response.get());
            log.info("Tour API 키워드 검색 성공: {}", statistics);
            return ResponseEntity.ok(response.get());
        } else {
            log.warn("Tour API 키워드 검색 실패: keyword={}", keyword);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 지역 기반 관광지 검색
     */
    @GetMapping("/area")
    @Operation(summary = "지역 기반 검색", description = "Tour API를 통한 지역별 관광지 검색")
    public ResponseEntity<TourApiResponse> searchByArea(
            @Parameter(description = "지역코드 (1=서울, 6=부산, 39=제주)", required = true)
            @RequestParam String areaCode,
            @Parameter(description = "컨텐츠 타입 (12=관광지, 39=음식점, 38=쇼핑)")
            @RequestParam(required = false) String contentTypeId,
            @Parameter(description = "페이지 번호", required = true)
            @RequestParam(defaultValue = "1") int pageNo,
            @Parameter(description = "한 페이지 결과 수", required = true)
            @RequestParam(defaultValue = "20") int numOfRows) {
        
        log.info("Tour API 지역 검색 요청: areaCode={}, contentTypeId={}, pageNo={}, numOfRows={}", 
                areaCode, contentTypeId, pageNo, numOfRows);
        
        Optional<TourApiResponse> response = tourApiSearchService.searchByArea(areaCode, contentTypeId, pageNo, numOfRows);
        
        if (response.isPresent()) {
            String statistics = tourApiSearchService.getSearchStatistics(response.get());
            log.info("Tour API 지역 검색 성공: {}", statistics);
            return ResponseEntity.ok(response.get());
        } else {
            log.warn("Tour API 지역 검색 실패: areaCode={}", areaCode);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 위치 기반 근거리 검색
     */
    @GetMapping("/location")
    @Operation(summary = "위치 기반 검색", description = "Tour API를 통한 위치 기반 근거리 관광지 검색")
    public ResponseEntity<TourApiResponse> searchByLocation(
            @Parameter(description = "경도", required = true)
            @RequestParam String mapX,
            @Parameter(description = "위도", required = true)
            @RequestParam String mapY,
            @Parameter(description = "반경 (미터 단위, 최대 20000)", required = true)
            @RequestParam(defaultValue = "5000") int radius,
            @Parameter(description = "컨텐츠 타입 (12=관광지, 39=음식점, 38=쇼핑)")
            @RequestParam(required = false) String contentTypeId) {
        
        log.info("Tour API 위치 검색 요청: mapX={}, mapY={}, radius={}m, contentTypeId={}", 
                mapX, mapY, radius, contentTypeId);
        
        Optional<TourApiResponse> response = tourApiSearchService.searchByLocation(mapX, mapY, radius, contentTypeId);
        
        if (response.isPresent()) {
            String statistics = tourApiSearchService.getSearchStatistics(response.get());
            log.info("Tour API 위치 검색 성공: {}", statistics);
            return ResponseEntity.ok(response.get());
        } else {
            log.warn("Tour API 위치 검색 실패: mapX={}, mapY={}", mapX, mapY);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 관광지 상세 정보 조회
     */
    @GetMapping("/detail/{contentId}")
    @Operation(summary = "상세 정보 조회", description = "Tour API를 통한 관광지 상세 정보 조회")
    public ResponseEntity<TourApiResponse> getDetailInfo(
            @Parameter(description = "컨텐츠 ID", required = true)
            @PathVariable String contentId,
            @Parameter(description = "컨텐츠 타입 ID", required = true)
            @RequestParam String contentTypeId) {
        
        log.info("Tour API 상세 정보 조회 요청: contentId={}, contentTypeId={}", contentId, contentTypeId);
        
        Optional<TourApiResponse> response = tourApiSearchService.getDetailInfo(contentId, contentTypeId);
        
        if (response.isPresent()) {
            log.info("Tour API 상세 정보 조회 성공: contentId={}", contentId);
            return ResponseEntity.ok(response.get());
        } else {
            log.warn("Tour API 상세 정보 조회 실패: contentId={}", contentId);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 통합 검색 - 키워드 우선, 지역 필터 적용
     */
    @GetMapping("/integrated")
    @Operation(summary = "통합 검색", description = "Tour API를 통한 통합 검색 (키워드 우선, 지역 필터 적용)")
    public ResponseEntity<TourApiResponse> integratedSearch(
            @Parameter(description = "검색 키워드", required = true)
            @RequestParam String keyword,
            @Parameter(description = "지역코드 (1=서울, 6=부산, 39=제주)")
            @RequestParam(required = false) String areaCode,
            @Parameter(description = "컨텐츠 타입 (12=관광지, 39=음식점, 38=쇼핑)")
            @RequestParam(required = false) String contentTypeId) {
        
        log.info("Tour API 통합 검색 요청: keyword={}, areaCode={}, contentTypeId={}", 
                keyword, areaCode, contentTypeId);
        
        Optional<TourApiResponse> response = tourApiSearchService.integratedSearch(keyword, areaCode, contentTypeId);
        
        if (response.isPresent()) {
            String statistics = tourApiSearchService.getSearchStatistics(response.get());
            log.info("Tour API 통합 검색 성공: {}", statistics);
            return ResponseEntity.ok(response.get());
        } else {
            log.warn("Tour API 통합 검색 실패: keyword={}", keyword);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 검색 통계 조회
     */
    @GetMapping("/statistics")
    @Operation(summary = "검색 통계", description = "Tour API 검색 결과 통계 정보")
    public ResponseEntity<String> getSearchStatistics(
            @Parameter(description = "검색 키워드", required = true)
            @RequestParam String keyword,
            @Parameter(description = "지역코드")
            @RequestParam(required = false) String areaCode,
            @Parameter(description = "컨텐츠 타입")
            @RequestParam(required = false) String contentTypeId) {
        
        log.info("Tour API 검색 통계 요청: keyword={}, areaCode={}, contentTypeId={}", 
                keyword, areaCode, contentTypeId);
        
        Optional<TourApiResponse> response = tourApiSearchService.searchByKeyword(keyword, areaCode, contentTypeId);
        
        if (response.isPresent()) {
            String statistics = tourApiSearchService.getSearchStatistics(response.get());
            log.info("Tour API 검색 통계 조회 성공: {}", statistics);
            return ResponseEntity.ok(statistics);
        } else {
            log.warn("Tour API 검색 통계 조회 실패: keyword={}", keyword);
            return ResponseEntity.ok("검색 결과 없음");
        }
    }
}
