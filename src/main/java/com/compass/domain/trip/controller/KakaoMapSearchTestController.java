package com.compass.domain.trip.controller;

import com.compass.domain.trip.dto.KakaoMapApiResponse;
import com.compass.domain.trip.service.KakaoMapSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Kakao Map API 검색 테스트 컨트롤러
 * REQ-SEARCH-003: Kakao Map API 검색 테스트용 엔드포인트
 */
@RestController
@RequestMapping("/api/test/kakao/search")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Kakao Map API Search Test", description = "Kakao Map API 검색 테스트용 API")
public class KakaoMapSearchTestController {

    private final KakaoMapSearchService kakaoMapSearchService;

    /**
     * 키워드 검색 테스트
     */
    @GetMapping("/keyword/{keyword}")
    @Operation(summary = "키워드 검색 테스트", description = "Kakao Map API 키워드 검색 테스트")
    public ResponseEntity<KakaoMapApiResponse> testKeywordSearch(@PathVariable String keyword) {
        log.info("Kakao Map API 키워드 검색 테스트: keyword={}", keyword);
        
        Optional<KakaoMapApiResponse> response = kakaoMapSearchService.searchByKeywordDefault(keyword);
        
        if (response.isPresent()) {
            String statistics = kakaoMapSearchService.getSearchStatistics(response.get());
            log.info("Kakao Map API 키워드 검색 테스트 성공: {}", statistics);
            return ResponseEntity.ok(response.get());
        } else {
            log.warn("Kakao Map API 키워드 검색 테스트 실패: keyword={}", keyword);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 카테고리 검색 테스트 (서울 시청 기준)
     */
    @GetMapping("/category/{categoryGroupCode}")
    @Operation(summary = "카테고리 검색 테스트", description = "Kakao Map API 카테고리 검색 테스트 (서울 시청 기준)")
    public ResponseEntity<KakaoMapApiResponse> testCategorySearch(@PathVariable String categoryGroupCode) {
        log.info("Kakao Map API 카테고리 검색 테스트: category={}", categoryGroupCode);
        
        // 서울 시청 좌표: 126.9780, 37.5665
        Optional<KakaoMapApiResponse> response = kakaoMapSearchService.searchByCategoryDefault(
                categoryGroupCode, "126.9780", "37.5665", 5000);
        
        if (response.isPresent()) {
            String statistics = kakaoMapSearchService.getSearchStatistics(response.get());
            log.info("Kakao Map API 카테고리 검색 테스트 성공: {}", statistics);
            return ResponseEntity.ok(response.get());
        } else {
            log.warn("Kakao Map API 카테고리 검색 테스트 실패: category={}", categoryGroupCode);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 주소 검색 테스트
     */
    @GetMapping("/address/{address}")
    @Operation(summary = "주소 검색 테스트", description = "Kakao Map API 주소 검색 테스트")
    public ResponseEntity<KakaoMapApiResponse> testAddressSearch(@PathVariable String address) {
        log.info("Kakao Map API 주소 검색 테스트: address={}", address);
        
        Optional<KakaoMapApiResponse> response = kakaoMapSearchService.searchByAddress(address, 1, 15);
        
        if (response.isPresent()) {
            String statistics = kakaoMapSearchService.getSearchStatistics(response.get());
            log.info("Kakao Map API 주소 검색 테스트 성공: {}", statistics);
            return ResponseEntity.ok(response.get());
        } else {
            log.warn("Kakao Map API 주소 검색 테스트 실패: address={}", address);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 좌표->주소 변환 테스트 (서울 시청 기준)
     */
    @GetMapping("/coord-to-address/seoul")
    @Operation(summary = "좌표->주소 변환 테스트", description = "Kakao Map API 좌표->주소 변환 테스트 (서울 시청 기준)")
    public ResponseEntity<KakaoMapApiResponse> testCoordToAddress() {
        log.info("Kakao Map API 좌표->주소 변환 테스트: 서울 시청 기준");
        
        // 서울 시청 좌표: 126.9780, 37.5665
        Optional<KakaoMapApiResponse> response = kakaoMapSearchService.coordToAddress(
                "126.9780", "37.5665", "WGS84", "WGS84");
        
        if (response.isPresent()) {
            int resultCount = response.get().getDocuments().size();
            log.info("Kakao Map API 좌표->주소 변환 테스트 성공: {}개 결과", resultCount);
            return ResponseEntity.ok(response.get());
        } else {
            log.warn("Kakao Map API 좌표->주소 변환 테스트 실패");
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 주소->좌표 변환 테스트
     */
    @GetMapping("/address-to-coord/{address}")
    @Operation(summary = "주소->좌표 변환 테스트", description = "Kakao Map API 주소->좌표 변환 테스트")
    public ResponseEntity<KakaoMapApiResponse> testAddressToCoord(@PathVariable String address) {
        log.info("Kakao Map API 주소->좌표 변환 테스트: address={}", address);
        
        Optional<KakaoMapApiResponse> response = kakaoMapSearchService.addressToCoord(address);
        
        if (response.isPresent()) {
            int resultCount = response.get().getDocuments().size();
            log.info("Kakao Map API 주소->좌표 변환 테스트 성공: {}개 결과", resultCount);
            return ResponseEntity.ok(response.get());
        } else {
            log.warn("Kakao Map API 주소->좌표 변환 테스트 실패: address={}", address);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 통합 검색 테스트
     */
    @GetMapping("/integrated/{keyword}")
    @Operation(summary = "통합 검색 테스트", description = "Kakao Map API 통합 검색 테스트")
    public ResponseEntity<KakaoMapApiResponse> testIntegratedSearch(@PathVariable String keyword) {
        log.info("Kakao Map API 통합 검색 테스트: keyword={}", keyword);
        
        // 서울 시청 좌표 기준으로 통합 검색
        Optional<KakaoMapApiResponse> response = kakaoMapSearchService.integratedSearch(
                keyword, "126.9780", "37.5665", 5000, "MT1", 1, 15, "accuracy");
        
        if (response.isPresent()) {
            String statistics = kakaoMapSearchService.getSearchStatistics(response.get());
            log.info("Kakao Map API 통합 검색 테스트 성공: {}", statistics);
            return ResponseEntity.ok(response.get());
        } else {
            log.warn("Kakao Map API 통합 검색 테스트 실패: keyword={}", keyword);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 검색 통계 테스트
     */
    @GetMapping("/statistics/{keyword}")
    @Operation(summary = "검색 통계 테스트", description = "Kakao Map API 검색 통계 테스트")
    public ResponseEntity<String> testSearchStatistics(@PathVariable String keyword) {
        log.info("Kakao Map API 검색 통계 테스트: keyword={}", keyword);
        
        Optional<KakaoMapApiResponse> response = kakaoMapSearchService.searchByKeywordDefault(keyword);
        
        if (response.isPresent()) {
            String statistics = kakaoMapSearchService.getSearchStatistics(response.get());
            log.info("Kakao Map API 검색 통계 테스트 성공: {}", statistics);
            return ResponseEntity.ok(statistics);
        } else {
            log.warn("Kakao Map API 검색 통계 테스트 실패: keyword={}", keyword);
            return ResponseEntity.ok("검색 결과 없음");
        }
    }

    /**
     * 카테고리 코드 목록 조회
     */
    @GetMapping("/categories")
    @Operation(summary = "카테고리 코드 목록", description = "Kakao Map API 카테고리 그룹 코드 목록")
    public ResponseEntity<String> getCategoryCodes() {
        log.info("Kakao Map API 카테고리 코드 목록 조회");
        
        String categories = """
                MT1: 대형마트
                CS2: 편의점
                PS3: 어린이집, 유치원
                SC4: 학교
                AC5: 학원
                PK6: 주차장
                OL7: 주유소, 충전소
                SW8: 지하철역
                BK9: 은행
                CT1: 문화시설
                AG2: 중개업소
                PO3: 공공기관
                AT4: 관광명소
                AD5: 숙박
                FD6: 음식점
                CE7: 카페
                HP8: 병원
                PM9: 약국
                """;
        
        return ResponseEntity.ok(categories);
    }
}
