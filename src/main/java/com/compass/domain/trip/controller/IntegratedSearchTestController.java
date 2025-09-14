package com.compass.domain.trip.controller;

import com.compass.domain.trip.dto.IntegratedSearchRequest;
import com.compass.domain.trip.dto.IntegratedSearchResponse;
import com.compass.domain.trip.service.IntegratedSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 통합 검색 테스트 컨트롤러
 * REQ-SEARCH-004: 통합 검색 서비스 테스트용 엔드포인트
 */
@RestController
@RequestMapping("/api/test/integrated/search")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Integrated Search Test", description = "통합 검색 테스트용 API")
public class IntegratedSearchTestController {

    private final IntegratedSearchService integratedSearchService;

    /**
     * 기본 통합 검색 테스트
     */
    @GetMapping("/basic/{keyword}")
    @Operation(summary = "기본 통합 검색 테스트", description = "기본 설정으로 통합 검색 테스트")
    public ResponseEntity<IntegratedSearchResponse> testBasicSearch(@PathVariable String keyword) {
        log.info("기본 통합 검색 테스트: keyword={}", keyword);
        
        try {
            IntegratedSearchRequest request = IntegratedSearchRequest.builder()
                    .keyword(keyword)
                    .searchType(IntegratedSearchRequest.SearchType.ALL)
                    .priority(IntegratedSearchRequest.SearchPriority.RDS_FIRST)
                    .page(1)
                    .size(10)
                    .build();
            
            IntegratedSearchResponse response = integratedSearchService.search(request);
            log.info("기본 통합 검색 테스트 성공: keyword={}, totalCount={}, searchTime={}ms", 
                    keyword, response.getTotalCount(), response.getSearchTimeMs());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("기본 통합 검색 테스트 실패: keyword={}", keyword, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * RDS 전용 검색 테스트
     */
    @GetMapping("/rds-only/{keyword}")
    @Operation(summary = "RDS 전용 검색 테스트", description = "RDS 검색만 사용하는 테스트")
    public ResponseEntity<IntegratedSearchResponse> testRdsOnlySearch(@PathVariable String keyword) {
        log.info("RDS 전용 검색 테스트: keyword={}", keyword);
        
        try {
            IntegratedSearchRequest request = IntegratedSearchRequest.builder()
                    .keyword(keyword)
                    .searchType(IntegratedSearchRequest.SearchType.RDS)
                    .page(1)
                    .size(10)
                    .build();
            
            IntegratedSearchResponse response = integratedSearchService.search(request);
            log.info("RDS 전용 검색 테스트 성공: keyword={}, totalCount={}", keyword, response.getTotalCount());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("RDS 전용 검색 테스트 실패: keyword={}", keyword, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Tour API 전용 검색 테스트
     */
    @GetMapping("/tour-api-only/{keyword}")
    @Operation(summary = "Tour API 전용 검색 테스트", description = "Tour API 검색만 사용하는 테스트")
    public ResponseEntity<IntegratedSearchResponse> testTourApiOnlySearch(@PathVariable String keyword) {
        log.info("Tour API 전용 검색 테스트: keyword={}", keyword);
        
        try {
            IntegratedSearchRequest request = IntegratedSearchRequest.builder()
                    .keyword(keyword)
                    .searchType(IntegratedSearchRequest.SearchType.TOUR_API)
                    .page(1)
                    .size(10)
                    .build();
            
            IntegratedSearchResponse response = integratedSearchService.search(request);
            log.info("Tour API 전용 검색 테스트 성공: keyword={}, totalCount={}", keyword, response.getTotalCount());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Tour API 전용 검색 테스트 실패: keyword={}", keyword, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Kakao Map API 전용 검색 테스트
     */
    @GetMapping("/kakao-map-only/{keyword}")
    @Operation(summary = "Kakao Map API 전용 검색 테스트", description = "Kakao Map API 검색만 사용하는 테스트")
    public ResponseEntity<IntegratedSearchResponse> testKakaoMapOnlySearch(@PathVariable String keyword) {
        log.info("Kakao Map API 전용 검색 테스트: keyword={}", keyword);
        
        try {
            IntegratedSearchRequest request = IntegratedSearchRequest.builder()
                    .keyword(keyword)
                    .searchType(IntegratedSearchRequest.SearchType.KAKAO_MAP)
                    .page(1)
                    .size(10)
                    .build();
            
            IntegratedSearchResponse response = integratedSearchService.search(request);
            log.info("Kakao Map API 전용 검색 테스트 성공: keyword={}, totalCount={}", keyword, response.getTotalCount());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Kakao Map API 전용 검색 테스트 실패: keyword={}", keyword, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 위치 기반 통합 검색 테스트 (서울 시청 기준)
     */
    @GetMapping("/location-based/{keyword}")
    @Operation(summary = "위치 기반 통합 검색 테스트", description = "서울 시청 기준 위치 기반 통합 검색 테스트")
    public ResponseEntity<IntegratedSearchResponse> testLocationBasedSearch(@PathVariable String keyword) {
        log.info("위치 기반 통합 검색 테스트: keyword={}", keyword);
        
        try {
            // 서울 시청 좌표: 126.9780, 37.5665
            IntegratedSearchRequest request = IntegratedSearchRequest.builder()
                    .keyword(keyword)
                    .searchType(IntegratedSearchRequest.SearchType.ALL)
                    .longitude(126.9780)
                    .latitude(37.5665)
                    .radius(5000)
                    .sort(IntegratedSearchRequest.SortType.DISTANCE)
                    .priority(IntegratedSearchRequest.SearchPriority.RDS_FIRST)
                    .page(1)
                    .size(10)
                    .build();
            
            IntegratedSearchResponse response = integratedSearchService.search(request);
            log.info("위치 기반 통합 검색 테스트 성공: keyword={}, totalCount={}", keyword, response.getTotalCount());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("위치 기반 통합 검색 테스트 실패: keyword={}", keyword, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 카테고리 필터 통합 검색 테스트
     */
    @GetMapping("/category-filter/{keyword}/{category}")
    @Operation(summary = "카테고리 필터 통합 검색 테스트", description = "카테고리 필터를 적용한 통합 검색 테스트")
    public ResponseEntity<IntegratedSearchResponse> testCategoryFilterSearch(
            @PathVariable String keyword, 
            @PathVariable String category) {
        log.info("카테고리 필터 통합 검색 테스트: keyword={}, category={}", keyword, category);
        
        try {
            IntegratedSearchRequest request = IntegratedSearchRequest.builder()
                    .keyword(keyword)
                    .searchType(IntegratedSearchRequest.SearchType.ALL)
                    .category(category)
                    .priority(IntegratedSearchRequest.SearchPriority.RDS_FIRST)
                    .page(1)
                    .size(10)
                    .build();
            
            IntegratedSearchResponse response = integratedSearchService.search(request);
            log.info("카테고리 필터 통합 검색 테스트 성공: keyword={}, category={}, totalCount={}", 
                    keyword, category, response.getTotalCount());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("카테고리 필터 통합 검색 테스트 실패: keyword={}, category={}", keyword, category, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 우선순위별 통합 검색 테스트
     */
    @GetMapping("/priority-test/{keyword}/{priority}")
    @Operation(summary = "우선순위별 통합 검색 테스트", description = "검색 우선순위를 변경한 통합 검색 테스트")
    public ResponseEntity<IntegratedSearchResponse> testPrioritySearch(
            @PathVariable String keyword, 
            @PathVariable String priority) {
        log.info("우선순위별 통합 검색 테스트: keyword={}, priority={}", keyword, priority);
        
        try {
            IntegratedSearchRequest.SearchPriority searchPriority;
            try {
                searchPriority = IntegratedSearchRequest.SearchPriority.valueOf(priority.toUpperCase());
            } catch (IllegalArgumentException e) {
                searchPriority = IntegratedSearchRequest.SearchPriority.RDS_FIRST;
            }
            
            IntegratedSearchRequest request = IntegratedSearchRequest.builder()
                    .keyword(keyword)
                    .searchType(IntegratedSearchRequest.SearchType.ALL)
                    .priority(searchPriority)
                    .page(1)
                    .size(10)
                    .build();
            
            IntegratedSearchResponse response = integratedSearchService.search(request);
            log.info("우선순위별 통합 검색 테스트 성공: keyword={}, priority={}, totalCount={}", 
                    keyword, priority, response.getTotalCount());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("우선순위별 통합 검색 테스트 실패: keyword={}, priority={}", keyword, priority, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 성능 테스트
     */
    @GetMapping("/performance-test/{keyword}")
    @Operation(summary = "성능 테스트", description = "통합 검색 성능 테스트")
    public ResponseEntity<String> testPerformance(@PathVariable String keyword) {
        log.info("통합 검색 성능 테스트: keyword={}", keyword);
        
        try {
            long startTime = System.currentTimeMillis();
            
            IntegratedSearchRequest request = IntegratedSearchRequest.builder()
                    .keyword(keyword)
                    .searchType(IntegratedSearchRequest.SearchType.ALL)
                    .priority(IntegratedSearchRequest.SearchPriority.RDS_FIRST)
                    .page(1)
                    .size(20)
                    .build();
            
            IntegratedSearchResponse response = integratedSearchService.search(request);
            
            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            
            StringBuilder performanceReport = new StringBuilder(String.format("""
                    통합 검색 성능 테스트 결과:
                    
                    검색 키워드: %s
                    총 소요 시간: %dms
                    서비스 응답 시간: %dms
                    총 결과 수: %d
                    페이지 크기: %d
                    
                    검색 시스템별 통계:
                    """, keyword, totalTime, response.getSearchTimeMs(), 
                    response.getTotalCount(), response.getPageSize()));
            
            if (response.getSystemStats() != null) {
                response.getSystemStats().forEach((system, stats) -> {
                    performanceReport.append(String.format("""
                            - %s: %d개 결과, %dms, 성공: %s
                            """, system, stats.getResultCount(), stats.getSearchTimeMs(), stats.getSuccess()));
                });
            }
            
            log.info("통합 검색 성능 테스트 완료: keyword={}, totalTime={}ms", keyword, totalTime);
            return ResponseEntity.ok(performanceReport.toString());
            
        } catch (Exception e) {
            log.error("통합 검색 성능 테스트 실패: keyword={}", keyword, e);
            return ResponseEntity.ok("성능 테스트 실패: " + e.getMessage());
        }
    }
}
