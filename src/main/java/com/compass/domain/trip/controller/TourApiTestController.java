package com.compass.domain.trip.controller;

import com.compass.domain.trip.dto.TourApiResponse;
import com.compass.domain.trip.service.TourApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Tour API 테스트 컨트롤러
 * REQ-CRAWL-001: Tour API 클라이언트 연동 테스트
 */
@RestController
@RequestMapping("/api/test/tour")
@Tag(name = "Tour API Test", description = "한국관광공사 Tour API 연동 테스트")
public class TourApiTestController {
    
    private final TourApiService tourApiService;
    
    public TourApiTestController(TourApiService tourApiService) {
        this.tourApiService = tourApiService;
    }
    
    @GetMapping("/seoul/tourist-spots")
    @Operation(summary = "서울 관광지 조회", description = "서울 지역 관광지 정보를 Tour API에서 조회합니다.")
    public ResponseEntity<List<TourApiResponse.TourItem>> getSeoulTouristSpots(
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "1") int pageNo,
            @Parameter(description = "페이지당 결과 수") @RequestParam(defaultValue = "10") int numOfRows) {
        
        List<TourApiResponse.TourItem> items = tourApiService.getSeoulTouristSpots(pageNo, numOfRows);
        return ResponseEntity.ok(items);
    }
    
    @GetMapping("/seoul/restaurants")
    @Operation(summary = "서울 음식점 조회", description = "서울 지역 음식점 정보를 Tour API에서 조회합니다.")
    public ResponseEntity<List<TourApiResponse.TourItem>> getSeoulRestaurants(
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "1") int pageNo,
            @Parameter(description = "페이지당 결과 수") @RequestParam(defaultValue = "10") int numOfRows) {
        
        List<TourApiResponse.TourItem> items = tourApiService.getSeoulRestaurants(pageNo, numOfRows);
        return ResponseEntity.ok(items);
    }
    
    @GetMapping("/seoul/shopping")
    @Operation(summary = "서울 쇼핑 조회", description = "서울 지역 쇼핑 정보를 Tour API에서 조회합니다.")
    public ResponseEntity<List<TourApiResponse.TourItem>> getSeoulShopping(
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "1") int pageNo,
            @Parameter(description = "페이지당 결과 수") @RequestParam(defaultValue = "10") int numOfRows) {
        
        List<TourApiResponse.TourItem> items = tourApiService.getSeoulShopping(pageNo, numOfRows);
        return ResponseEntity.ok(items);
    }
    
    @GetMapping("/seoul/category/{category}")
    @Operation(summary = "카테고리별 서울 관광지 조회", 
               description = "Seoul JSON 카테고리를 기반으로 Tour API에서 관련 정보를 조회합니다.")
    public ResponseEntity<List<TourApiResponse.TourItem>> getSeoulByCategory(
            @Parameter(description = "Seoul JSON 카테고리 (예: Palace, Museum, Market)") 
            @PathVariable String category,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "1") int pageNo,
            @Parameter(description = "페이지당 결과 수") @RequestParam(defaultValue = "10") int numOfRows) {
        
        List<TourApiResponse.TourItem> items = tourApiService.getSeoulByCategory(category, pageNo, numOfRows);
        return ResponseEntity.ok(items);
    }
    
    @GetMapping("/detail/{contentId}")
    @Operation(summary = "관광지 상세 정보 조회", description = "특정 관광지의 상세 정보를 조회합니다.")
    public ResponseEntity<TourApiResponse.TourItem> getPlaceDetail(
            @Parameter(description = "컨텐츠 ID") @PathVariable String contentId,
            @Parameter(description = "컨텐츠 타입 ID") @RequestParam(defaultValue = "12") String contentTypeId) {
        
        Optional<TourApiResponse.TourItem> item = tourApiService.getPlaceDetail(contentId, contentTypeId);
        
        if (item.isPresent()) {
            return ResponseEntity.ok(item.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/nearby")
    @Operation(summary = "근처 관광지 조회", 
               description = "Seoul JSON 좌표를 기반으로 근처 관광지를 Tour API에서 조회합니다.")
    public ResponseEntity<List<TourApiResponse.TourItem>> getNearbyPlaces(
            @Parameter(description = "위도 (Seoul JSON: lat)") @RequestParam double latitude,
            @Parameter(description = "경도 (Seoul JSON: lng)") @RequestParam double longitude,
            @Parameter(description = "반경 (미터)") @RequestParam(defaultValue = "1000") int radiusMeters,
            @Parameter(description = "컨텐츠 타입 ID") @RequestParam(defaultValue = "12") String contentTypeId) {
        
        List<TourApiResponse.TourItem> items = tourApiService.getNearbyPlaces(
                latitude, longitude, radiusMeters, contentTypeId);
        return ResponseEntity.ok(items);
    }
    
    @GetMapping("/search")
    @Operation(summary = "키워드 검색", description = "Seoul JSON tags를 기반으로 Tour API에서 관련 정보를 검색합니다.")
    public ResponseEntity<List<TourApiResponse.TourItem>> searchByKeyword(
            @Parameter(description = "검색 키워드 (Seoul JSON: tags)") @RequestParam String keyword,
            @Parameter(description = "컨텐츠 타입 ID") @RequestParam(required = false) String contentTypeId) {
        
        List<TourApiResponse.TourItem> items = tourApiService.searchByKeyword(keyword, contentTypeId);
        return ResponseEntity.ok(items);
    }
    
    @GetMapping("/seoul/all")
    @Operation(summary = "서울 전체 데이터 수집", 
               description = "Phase별 크롤링을 위한 서울 전체 관광 데이터를 수집합니다.")
    public ResponseEntity<List<TourApiResponse.TourItem>> collectAllSeoulData() {
        
        List<TourApiResponse.TourItem> items = tourApiService.collectAllSeoulData();
        return ResponseEntity.ok(items);
    }
    
    @GetMapping("/enrich")
    @Operation(summary = "Seoul JSON 데이터 보완", 
               description = "Seoul JSON 좌표와 카테고리를 기반으로 Tour API에서 추가 정보를 검색합니다.")
    public ResponseEntity<List<TourApiResponse.TourItem>> enrichSeoulJsonData(
            @Parameter(description = "Seoul JSON 위도") @RequestParam double seoulLat,
            @Parameter(description = "Seoul JSON 경도") @RequestParam double seoulLng,
            @Parameter(description = "Seoul JSON 카테고리") @RequestParam String seoulCategory) {
        
        List<TourApiResponse.TourItem> items = tourApiService.enrichSeoulJsonData(
                seoulLat, seoulLng, seoulCategory);
        return ResponseEntity.ok(items);
    }
    
    @GetMapping("/mapping/category/{seoulCategory}")
    @Operation(summary = "카테고리 매핑 테스트", 
               description = "Seoul JSON 카테고리가 Tour API ContentTypeId로 어떻게 매핑되는지 확인합니다.")
    public ResponseEntity<String> testCategoryMapping(
            @Parameter(description = "Seoul JSON 카테고리") @PathVariable String seoulCategory) {
        
        String contentTypeId = tourApiService.mapCategoryToContentTypeId(seoulCategory);
        return ResponseEntity.ok("Seoul Category: " + seoulCategory + " → Tour API ContentTypeId: " + contentTypeId);
    }
}
