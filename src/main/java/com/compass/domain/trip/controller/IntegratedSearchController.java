package com.compass.domain.trip.controller;

import com.compass.domain.trip.dto.IntegratedSearchRequest;
import com.compass.domain.trip.dto.IntegratedSearchResponse;
import com.compass.domain.trip.service.IntegratedSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 통합 검색 컨트롤러
 * REQ-SEARCH-004: 통합 검색 서비스
 */
@RestController
@RequestMapping("/api/search/integrated")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Integrated Search", description = "통합 검색 API")
public class IntegratedSearchController {

    private final IntegratedSearchService integratedSearchService;

    /**
     * 통합 검색 실행
     */
    @PostMapping
    @Operation(summary = "통합 검색", description = "모든 검색 시스템을 통합한 검색 실행")
    public ResponseEntity<IntegratedSearchResponse> search(@RequestBody IntegratedSearchRequest request) {
        log.info("통합 검색 요청: keyword={}, type={}, priority={}", 
                request.getKeyword(), request.getSearchType(), request.getPriority());
        
        try {
            IntegratedSearchResponse response = integratedSearchService.search(request);
            log.info("통합 검색 성공: keyword={}, totalCount={}, searchTime={}ms", 
                    request.getKeyword(), response.getTotalCount(), response.getSearchTimeMs());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("통합 검색 실패: keyword={}", request.getKeyword(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 간편 통합 검색 (GET 방식)
     */
    @GetMapping
    @Operation(summary = "간편 통합 검색", description = "GET 방식의 간편 통합 검색")
    public ResponseEntity<IntegratedSearchResponse> simpleSearch(
            @Parameter(description = "검색 키워드", required = true)
            @RequestParam String keyword,
            @Parameter(description = "검색 타입 (ALL, RDS, TOUR_API, KAKAO_MAP)")
            @RequestParam(defaultValue = "ALL") String searchType,
            @Parameter(description = "카테고리")
            @RequestParam(required = false) String category,
            @Parameter(description = "지역 코드")
            @RequestParam(required = false) String areaCode,
            @Parameter(description = "경도")
            @RequestParam(required = false) Double longitude,
            @Parameter(description = "위도")
            @RequestParam(required = false) Double latitude,
            @Parameter(description = "검색 반경 (미터)")
            @RequestParam(defaultValue = "5000") Integer radius,
            @Parameter(description = "페이지 번호")
            @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "페이지 크기")
            @RequestParam(defaultValue = "15") Integer size,
            @Parameter(description = "정렬 방식 (ACCURACY, DISTANCE, POPULARITY)")
            @RequestParam(defaultValue = "ACCURACY") String sort,
            @Parameter(description = "검색 우선순위 (RDS_FIRST, TOUR_API_FIRST, KAKAO_MAP_FIRST)")
            @RequestParam(defaultValue = "RDS_FIRST") String priority) {
        
        log.info("간편 통합 검색 요청: keyword={}, type={}, priority={}", keyword, searchType, priority);
        
        try {
            IntegratedSearchRequest request = IntegratedSearchRequest.builder()
                    .keyword(keyword)
                    .searchType(IntegratedSearchRequest.SearchType.valueOf(searchType))
                    .category(category)
                    .areaCode(areaCode)
                    .longitude(longitude)
                    .latitude(latitude)
                    .radius(radius)
                    .page(page)
                    .size(size)
                    .sort(IntegratedSearchRequest.SortType.valueOf(sort))
                    .priority(IntegratedSearchRequest.SearchPriority.valueOf(priority))
                    .build();
            
            IntegratedSearchResponse response = integratedSearchService.search(request);
            log.info("간편 통합 검색 성공: keyword={}, totalCount={}, searchTime={}ms", 
                    keyword, response.getTotalCount(), response.getSearchTimeMs());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("간편 통합 검색 실패: keyword={}", keyword, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * RDS 우선 통합 검색
     */
    @GetMapping("/rds-first")
    @Operation(summary = "RDS 우선 통합 검색", description = "RDS 검색을 우선으로 하는 통합 검색")
    public ResponseEntity<IntegratedSearchResponse> rdsFirstSearch(
            @Parameter(description = "검색 키워드", required = true)
            @RequestParam String keyword,
            @Parameter(description = "카테고리")
            @RequestParam(required = false) String category,
            @Parameter(description = "지역 코드")
            @RequestParam(required = false) String areaCode,
            @Parameter(description = "페이지 번호")
            @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "페이지 크기")
            @RequestParam(defaultValue = "15") Integer size) {
        
        log.info("RDS 우선 통합 검색 요청: keyword={}", keyword);
        
        try {
            IntegratedSearchRequest request = IntegratedSearchRequest.builder()
                    .keyword(keyword)
                    .searchType(IntegratedSearchRequest.SearchType.ALL)
                    .category(category)
                    .areaCode(areaCode)
                    .page(page)
                    .size(size)
                    .priority(IntegratedSearchRequest.SearchPriority.RDS_FIRST)
                    .build();
            
            IntegratedSearchResponse response = integratedSearchService.search(request);
            log.info("RDS 우선 통합 검색 성공: keyword={}, totalCount={}", keyword, response.getTotalCount());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("RDS 우선 통합 검색 실패: keyword={}", keyword, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Tour API 우선 통합 검색
     */
    @GetMapping("/tour-api-first")
    @Operation(summary = "Tour API 우선 통합 검색", description = "Tour API 검색을 우선으로 하는 통합 검색")
    public ResponseEntity<IntegratedSearchResponse> tourApiFirstSearch(
            @Parameter(description = "검색 키워드", required = true)
            @RequestParam String keyword,
            @Parameter(description = "지역 코드")
            @RequestParam(required = false) String areaCode,
            @Parameter(description = "페이지 번호")
            @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "페이지 크기")
            @RequestParam(defaultValue = "15") Integer size) {
        
        log.info("Tour API 우선 통합 검색 요청: keyword={}", keyword);
        
        try {
            IntegratedSearchRequest request = IntegratedSearchRequest.builder()
                    .keyword(keyword)
                    .searchType(IntegratedSearchRequest.SearchType.ALL)
                    .areaCode(areaCode)
                    .page(page)
                    .size(size)
                    .priority(IntegratedSearchRequest.SearchPriority.TOUR_API_FIRST)
                    .build();
            
            IntegratedSearchResponse response = integratedSearchService.search(request);
            log.info("Tour API 우선 통합 검색 성공: keyword={}, totalCount={}", keyword, response.getTotalCount());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Tour API 우선 통합 검색 실패: keyword={}", keyword, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Kakao Map API 우선 통합 검색
     */
    @GetMapping("/kakao-map-first")
    @Operation(summary = "Kakao Map API 우선 통합 검색", description = "Kakao Map API 검색을 우선으로 하는 통합 검색")
    public ResponseEntity<IntegratedSearchResponse> kakaoMapFirstSearch(
            @Parameter(description = "검색 키워드", required = true)
            @RequestParam String keyword,
            @Parameter(description = "경도")
            @RequestParam(required = false) Double longitude,
            @Parameter(description = "위도")
            @RequestParam(required = false) Double latitude,
            @Parameter(description = "검색 반경 (미터)")
            @RequestParam(defaultValue = "5000") Integer radius,
            @Parameter(description = "페이지 번호")
            @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "페이지 크기")
            @RequestParam(defaultValue = "15") Integer size) {
        
        log.info("Kakao Map API 우선 통합 검색 요청: keyword={}", keyword);
        
        try {
            IntegratedSearchRequest request = IntegratedSearchRequest.builder()
                    .keyword(keyword)
                    .searchType(IntegratedSearchRequest.SearchType.ALL)
                    .longitude(longitude)
                    .latitude(latitude)
                    .radius(radius)
                    .page(page)
                    .size(size)
                    .priority(IntegratedSearchRequest.SearchPriority.KAKAO_MAP_FIRST)
                    .build();
            
            IntegratedSearchResponse response = integratedSearchService.search(request);
            log.info("Kakao Map API 우선 통합 검색 성공: keyword={}, totalCount={}", keyword, response.getTotalCount());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Kakao Map API 우선 통합 검색 실패: keyword={}", keyword, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 검색 통계 조회
     */
    @GetMapping("/statistics")
    @Operation(summary = "검색 통계", description = "통합 검색 시스템 통계 정보")
    public ResponseEntity<String> getSearchStatistics() {
        log.info("통합 검색 통계 조회 요청");
        
        String statistics = """
                통합 검색 시스템 통계:
                
                지원 검색 시스템:
                - RDS 검색 (PostgreSQL 전문검색)
                - Tour API 검색 (실시간 API 호출)
                - Kakao Map API 검색 (폴백 검색)
                
                검색 타입:
                - ALL: 모든 검색 시스템 사용
                - RDS: RDS 검색만
                - TOUR_API: Tour API 검색만
                - KAKAO_MAP: Kakao Map API 검색만
                
                검색 우선순위:
                - RDS_FIRST: RDS 우선, 나머지 폴백
                - TOUR_API_FIRST: Tour API 우선, 나머지 폴백
                - KAKAO_MAP_FIRST: Kakao Map API 우선, 나머지 폴백
                
                정렬 방식:
                - ACCURACY: 정확도 순
                - DISTANCE: 거리 순
                - POPULARITY: 인기도 순
                """;
        
        return ResponseEntity.ok(statistics);
    }
}
