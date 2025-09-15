package com.compass.domain.trip.controller;

import com.compass.domain.trip.dto.KakaoMapApiResponse;
import com.compass.domain.trip.service.KakaoMapSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Kakao Map API 검색 컨트롤러
 * REQ-SEARCH-003: Kakao Map API 검색 (폴백 검색)
 */
@RestController
@RequestMapping("/api/search/kakao")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Kakao Map API Search", description = "Kakao Map API 검색 API")
public class KakaoMapSearchController {

    private final KakaoMapSearchService kakaoMapSearchService;

    /**
     * 키워드로 장소 검색
     */
    @GetMapping("/keyword")
    @Operation(summary = "키워드 검색", description = "Kakao Map API를 통한 키워드 기반 장소 검색")
    public ResponseEntity<KakaoMapApiResponse> searchByKeyword(
            @Parameter(description = "검색 키워드", required = true)
            @RequestParam String keyword,
            @Parameter(description = "경도 (선택)")
            @RequestParam(required = false) String x,
            @Parameter(description = "위도 (선택)")
            @RequestParam(required = false) String y,
            @Parameter(description = "반경 (미터, 최대 20000)")
            @RequestParam(defaultValue = "0") int radius,
            @Parameter(description = "페이지 번호")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "페이지 크기")
            @RequestParam(defaultValue = "15") int size,
            @Parameter(description = "정렬 방식 (accuracy, distance)")
            @RequestParam(defaultValue = "accuracy") String sort) {
        
        log.info("Kakao Map API 키워드 검색 요청: keyword={}, x={}, y={}, radius={}m, page={}, size={}, sort={}", 
                keyword, x, y, radius, page, size, sort);
        
        Optional<KakaoMapApiResponse> response = kakaoMapSearchService.searchByKeyword(
                keyword, x, y, radius, page, size, sort);
        
        if (response.isPresent()) {
            String statistics = kakaoMapSearchService.getSearchStatistics(response.get());
            log.info("Kakao Map API 키워드 검색 성공: {}", statistics);
            return ResponseEntity.ok(response.get());
        } else {
            log.warn("Kakao Map API 키워드 검색 실패: keyword={}", keyword);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 카테고리로 장소 검색
     */
    @GetMapping("/category")
    @Operation(summary = "카테고리 검색", description = "Kakao Map API를 통한 카테고리 기반 장소 검색")
    public ResponseEntity<KakaoMapApiResponse> searchByCategory(
            @Parameter(description = "카테고리 그룹 코드", required = true)
            @RequestParam String categoryGroupCode,
            @Parameter(description = "경도", required = true)
            @RequestParam String x,
            @Parameter(description = "위도", required = true)
            @RequestParam String y,
            @Parameter(description = "반경 (미터, 최대 20000)", required = true)
            @RequestParam int radius,
            @Parameter(description = "페이지 번호")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "페이지 크기")
            @RequestParam(defaultValue = "15") int size,
            @Parameter(description = "정렬 방식 (accuracy, distance)")
            @RequestParam(defaultValue = "accuracy") String sort) {
        
        log.info("Kakao Map API 카테고리 검색 요청: category={}, x={}, y={}, radius={}m, page={}, size={}, sort={}", 
                categoryGroupCode, x, y, radius, page, size, sort);
        
        Optional<KakaoMapApiResponse> response = kakaoMapSearchService.searchByCategory(
                categoryGroupCode, x, y, radius, page, size, sort);
        
        if (response.isPresent()) {
            String statistics = kakaoMapSearchService.getSearchStatistics(response.get());
            log.info("Kakao Map API 카테고리 검색 성공: {}", statistics);
            return ResponseEntity.ok(response.get());
        } else {
            log.warn("Kakao Map API 카테고리 검색 실패: category={}", categoryGroupCode);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 주소로 장소 검색
     */
    @GetMapping("/address")
    @Operation(summary = "주소 검색", description = "Kakao Map API를 통한 주소 기반 장소 검색")
    public ResponseEntity<KakaoMapApiResponse> searchByAddress(
            @Parameter(description = "검색할 주소", required = true)
            @RequestParam String address,
            @Parameter(description = "페이지 번호")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "페이지 크기")
            @RequestParam(defaultValue = "15") int size) {
        
        log.info("Kakao Map API 주소 검색 요청: address={}, page={}, size={}", address, page, size);
        
        Optional<KakaoMapApiResponse> response = kakaoMapSearchService.searchByAddress(address, page, size);
        
        if (response.isPresent()) {
            String statistics = kakaoMapSearchService.getSearchStatistics(response.get());
            log.info("Kakao Map API 주소 검색 성공: {}", statistics);
            return ResponseEntity.ok(response.get());
        } else {
            log.warn("Kakao Map API 주소 검색 실패: address={}", address);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 좌표를 주소로 변환
     */
    @GetMapping("/coord-to-address")
    @Operation(summary = "좌표->주소 변환", description = "Kakao Map API를 통한 좌표를 주소로 변환")
    public ResponseEntity<KakaoMapApiResponse> coordToAddress(
            @Parameter(description = "경도", required = true)
            @RequestParam String x,
            @Parameter(description = "위도", required = true)
            @RequestParam String y,
            @Parameter(description = "입력 좌표계 (WGS84, WCONGNAMUL, CONGNAMUL, WTM, TM)")
            @RequestParam(defaultValue = "WGS84") String inputCoord,
            @Parameter(description = "출력 좌표계")
            @RequestParam(defaultValue = "WGS84") String outputCoord) {
        
        log.info("Kakao Map API 좌표->주소 변환 요청: x={}, y={}, inputCoord={}, outputCoord={}", 
                x, y, inputCoord, outputCoord);
        
        Optional<KakaoMapApiResponse> response = kakaoMapSearchService.coordToAddress(x, y, inputCoord, outputCoord);
        
        if (response.isPresent()) {
            int resultCount = response.get().getDocuments().size();
            log.info("Kakao Map API 좌표->주소 변환 성공: {}개 결과", resultCount);
            return ResponseEntity.ok(response.get());
        } else {
            log.warn("Kakao Map API 좌표->주소 변환 실패: x={}, y={}", x, y);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 주소를 좌표로 변환
     */
    @GetMapping("/address-to-coord")
    @Operation(summary = "주소->좌표 변환", description = "Kakao Map API를 통한 주소를 좌표로 변환")
    public ResponseEntity<KakaoMapApiResponse> addressToCoord(
            @Parameter(description = "검색할 주소", required = true)
            @RequestParam String address) {
        
        log.info("Kakao Map API 주소->좌표 변환 요청: address={}", address);
        
        Optional<KakaoMapApiResponse> response = kakaoMapSearchService.addressToCoord(address);
        
        if (response.isPresent()) {
            int resultCount = response.get().getDocuments().size();
            log.info("Kakao Map API 주소->좌표 변환 성공: {}개 결과", resultCount);
            return ResponseEntity.ok(response.get());
        } else {
            log.warn("Kakao Map API 주소->좌표 변환 실패: address={}", address);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 통합 검색 - 키워드 우선, 카테고리 폴백
     */
    @GetMapping("/integrated")
    @Operation(summary = "통합 검색", description = "Kakao Map API를 통한 통합 검색 (키워드 우선, 카테고리 폴백)")
    public ResponseEntity<KakaoMapApiResponse> integratedSearch(
            @Parameter(description = "검색 키워드", required = true)
            @RequestParam String keyword,
            @Parameter(description = "경도 (선택)")
            @RequestParam(required = false) String x,
            @Parameter(description = "위도 (선택)")
            @RequestParam(required = false) String y,
            @Parameter(description = "반경 (미터, 최대 20000)")
            @RequestParam(defaultValue = "0") int radius,
            @Parameter(description = "카테고리 그룹 코드 (선택)")
            @RequestParam(required = false) String categoryGroupCode,
            @Parameter(description = "페이지 번호")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "페이지 크기")
            @RequestParam(defaultValue = "15") int size,
            @Parameter(description = "정렬 방식 (accuracy, distance)")
            @RequestParam(defaultValue = "accuracy") String sort) {
        
        log.info("Kakao Map API 통합 검색 요청: keyword={}, x={}, y={}, radius={}m, category={}, page={}, size={}, sort={}", 
                keyword, x, y, radius, categoryGroupCode, page, size, sort);
        
        Optional<KakaoMapApiResponse> response = kakaoMapSearchService.integratedSearch(
                keyword, x, y, radius, categoryGroupCode, page, size, sort);
        
        if (response.isPresent()) {
            String statistics = kakaoMapSearchService.getSearchStatistics(response.get());
            log.info("Kakao Map API 통합 검색 성공: {}", statistics);
            return ResponseEntity.ok(response.get());
        } else {
            log.warn("Kakao Map API 통합 검색 실패: keyword={}", keyword);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 검색 통계 조회
     */
    @GetMapping("/statistics")
    @Operation(summary = "검색 통계", description = "Kakao Map API 검색 결과 통계 정보")
    public ResponseEntity<String> getSearchStatistics(
            @Parameter(description = "검색 키워드", required = true)
            @RequestParam String keyword,
            @Parameter(description = "경도 (선택)")
            @RequestParam(required = false) String x,
            @Parameter(description = "위도 (선택)")
            @RequestParam(required = false) String y,
            @Parameter(description = "반경 (미터)")
            @RequestParam(defaultValue = "0") int radius) {
        
        log.info("Kakao Map API 검색 통계 요청: keyword={}, x={}, y={}, radius={}m", keyword, x, y, radius);
        
        Optional<KakaoMapApiResponse> response = kakaoMapSearchService.searchByKeyword(
                keyword, x, y, radius, 1, 15, "accuracy");
        
        if (response.isPresent()) {
            String statistics = kakaoMapSearchService.getSearchStatistics(response.get());
            log.info("Kakao Map API 검색 통계 조회 성공: {}", statistics);
            return ResponseEntity.ok(statistics);
        } else {
            log.warn("Kakao Map API 검색 통계 조회 실패: keyword={}", keyword);
            return ResponseEntity.ok("검색 결과 없음");
        }
    }
}
