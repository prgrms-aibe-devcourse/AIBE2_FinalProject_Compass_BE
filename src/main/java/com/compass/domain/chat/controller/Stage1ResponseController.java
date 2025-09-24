package com.compass.domain.chat.controller;

import com.compass.domain.chat.service.PlaceSelectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stage 1 응답 확인용 컨트롤러
 * 
 * 웹 브라우저나 Postman에서 쉽게 응답을 확인할 수 있습니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/stage1")
@RequiredArgsConstructor
public class Stage1ResponseController {

    private final PlaceSelectionService placeSelectionService;

    /**
     * 간단한 Stage 1 테스트 - GET 요청으로 쉽게 확인
     * 
     * 브라우저에서 http://localhost:8080/api/stage1/test-seoul 접속
     */
    @GetMapping("/test-seoul")
    public ResponseEntity<?> testSeoulTrip() {
        log.info("=== Stage 1 서울 여행 테스트 시작 ===");

        try {
            // 기본 여행 정보 설정
            PlaceSelectionService.TravelInfo travelInfo = new PlaceSelectionService.TravelInfo(
                "CULTURAL", "MEDIUM", Arrays.asList("궁궐", "박물관", "전통"),
                LocalDate.now(), LocalDate.now().plusDays(2), 3, 2
            );

            // Stage 1 실행
            PlaceSelectionService.Stage1Output result = placeSelectionService.selectPlaces("서울", travelInfo);

            // 응답 데이터 구성
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Stage 1 서울 문화 여행 결과");
            response.put("destination", "서울");
            response.put("travelStyle", travelInfo.travelStyle());
            response.put("tripDays", result.tripDays());
            
            // 기본 통계
            Map<String, Object> summary = new HashMap<>();
            summary.put("totalPlaces", result.places().size());
            summary.put("categoryCount", result.categoryPlaces().size());
            summary.put("averageRating", Math.round(result.statistics().averageRating() * 10.0) / 10.0);
            response.put("summary", summary);

            // 카테고리별 분포
            Map<String, Integer> categoryDistribution = new HashMap<>();
            result.categoryPlaces().forEach((category, places) -> {
                categoryDistribution.put(category, places.size());
            });
            response.put("categoryDistribution", categoryDistribution);

            // 샘플 장소 데이터 (처음 10개)
            List<Map<String, Object>> samplePlaces = result.places().stream()
                .limit(10)
                .map(place -> {
                    Map<String, Object> placeData = new HashMap<>();
                    placeData.put("id", place.id());
                    placeData.put("name", place.name());
                    placeData.put("address", place.address());
                    placeData.put("category", place.category());
                    placeData.put("rating", place.rating());
                    placeData.put("operatingHours", place.operatingHours());
                    placeData.put("priceRange", place.priceRange());
                    placeData.put("tags", place.tags());
                    placeData.put("source", place.source());
                    placeData.put("travelStyle", place.travelStyle());
                    placeData.put("description", place.description());
                    return placeData;
                })
                .toList();
            response.put("samplePlaces", samplePlaces);

            // 경고 메시지
            response.put("warnings", result.warnings());

            // Stage 2, 3 전달 정보
            Map<String, Object> stage23Info = new HashMap<>();
            stage23Info.put("totalPlacesAvailable", result.places().size());
            stage23Info.put("categoriesAvailable", result.categoryPlaces().keySet());
            stage23Info.put("dataStructure", Map.of(
                "places", "List<TourPlace> - 전체 장소 리스트",
                "categoryPlaces", "Map<String, List<TourPlace>> - 카테고리별 장소",
                "statistics", "PlaceStatistics - 통계 정보",
                "tripDays", "int - 여행 일수",
                "warnings", "List<String> - 경고 메시지"
            ));
            response.put("stage23Info", stage23Info);

            log.info("Stage 1 응답 생성 완료: {} 개 장소", result.places().size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Stage 1 테스트 실패", e);
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "Stage 1 실행 실패: " + e.getMessage()
            ));
        }
    }

    /**
     * 커스텀 여행 정보로 테스트 - POST 요청
     * 
     * POST http://localhost:8080/api/stage1/test-custom
     * Body: {
     *   "destination": "부산",
     *   "travelStyle": "FOODIE", 
     *   "budget": "HIGH",
     *   "interests": ["맛집", "카페", "해산물"],
     *   "tripDays": 2,
     *   "companions": 4
     * }
     */
    @PostMapping("/test-custom")
    public ResponseEntity<?> testCustomTrip(@RequestBody CustomTripRequest request) {
        log.info("=== Stage 1 커스텀 여행 테스트: {} ===", request.destination);

        try {
            PlaceSelectionService.TravelInfo travelInfo = new PlaceSelectionService.TravelInfo(
                request.travelStyle, request.budget, request.interests,
                LocalDate.now(), LocalDate.now().plusDays(request.tripDays - 1),
                request.tripDays, request.companions
            );

            PlaceSelectionService.Stage1Output result = placeSelectionService.selectPlaces(request.destination, travelInfo);

            // 간단한 응답 구성
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("destination", request.destination);
            response.put("travelStyle", request.travelStyle);
            response.put("totalPlaces", result.places().size());
            response.put("categoryCount", result.categoryPlaces().size());
            response.put("averageRating", Math.round(result.statistics().averageRating() * 10.0) / 10.0);
            
            // 카테고리별 상위 3개씩
            Map<String, List<String>> topPlacesByCategory = new HashMap<>();
            result.categoryPlaces().forEach((category, places) -> {
                List<String> topPlaces = places.stream()
                    .limit(3)
                    .map(place -> String.format("%s (평점: %.1f)", 
                        place.name(), 
                        place.rating() != null ? place.rating() : 0.0))
                    .toList();
                topPlacesByCategory.put(category, topPlaces);
            });
            response.put("topPlacesByCategory", topPlacesByCategory);

            response.put("warnings", result.warnings());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("커스텀 여행 테스트 실패", e);
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "실행 실패: " + e.getMessage()
            ));
        }
    }

    /**
     * 헬스체크
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
            "status", "OK",
            "message", "Stage 1 서비스 정상 동작",
            "timestamp", System.currentTimeMillis(),
            "endpoints", Arrays.asList(
                "GET /api/stage1/test-seoul - 서울 여행 테스트",
                "POST /api/stage1/test-custom - 커스텀 여행 테스트",
                "GET /api/stage1/health - 헬스체크"
            )
        ));
    }

    // DTO
    public record CustomTripRequest(
        String destination,
        String travelStyle,
        String budget,
        List<String> interests,
        int tripDays,
        int companions
    ) {}
}

