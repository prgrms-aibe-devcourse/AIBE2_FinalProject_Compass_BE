package com.compass.domain.chat.controller;

import com.compass.domain.chat.service.PlaceSelectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Stage 1 간단 테스트 컨트롤러 (데이터베이스 없이)
 * 
 * 메모리 기반으로 Stage 1 파이프라인을 테스트합니다.
 * - 카테고리별 장소 수집
 * - 여행 스타일별 필터링
 * - Stage 2, 3 전달용 데이터 구조 확인
 */
@Slf4j
@RestController
@RequestMapping("/api/test/stage1-simple")
@RequiredArgsConstructor
public class Stage1SimpleTestController {

    private final PlaceSelectionService placeSelectionService;

    /**
     * Stage 1 파이프라인 테스트 (메모리 기반)
     */
    @PostMapping("/test")
    public ResponseEntity<?> testStage1(@RequestBody TestRequest request) {
        log.info("=== Stage 1 간단 테스트 시작 ===");
        log.info("목적지: {}, 스타일: {}, 일수: {}일", request.destination, request.travelStyle, request.tripDays);

        try {
            // 여행 정보 생성
            PlaceSelectionService.TravelInfo travelInfo = new PlaceSelectionService.TravelInfo(
                request.travelStyle,
                request.budget,
                request.interests,
                LocalDate.now().plusDays(30),
                LocalDate.now().plusDays(30 + request.tripDays - 1),
                request.tripDays,
                request.companions
            );

            // Stage 1 실행
            long startTime = System.currentTimeMillis();
            PlaceSelectionService.Stage1Output result = placeSelectionService.selectPlaces(request.destination, travelInfo);
            long endTime = System.currentTimeMillis();

            // 결과 분석
            TestResponse response = TestResponse.builder()
                .success(true)
                .message("Stage 1 테스트 성공")
                .executionTimeMs(endTime - startTime)
                .destination(request.destination)
                .travelStyle(request.travelStyle)
                .tripDays(request.tripDays)
                .totalPlaces(result.places().size())
                .categoryBreakdown(result.categoryPlaces().entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().size()
                    )))
                .averageRating(result.statistics().averageRating())
                .warnings(result.warnings())
                .samplePlaces(result.places().stream()
                    .limit(20) // 처음 20개만 샘플로 반환
                    .map(place -> PlaceInfo.builder()
                        .name(place.name())
                        .category(place.category())
                        .rating(place.rating())
                        .address(place.address())
                        .source(place.source())
                        .tags(place.tags())
                        .build())
                    .toList())
                .categoryDetails(result.categoryPlaces().entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> CategoryDetail.builder()
                            .count(entry.getValue().size())
                            .averageRating(entry.getValue().stream()
                                .filter(p -> p.rating() != null)
                                .mapToDouble(p -> p.rating())
                                .average()
                                .orElse(0.0))
                            .topPlaces(entry.getValue().stream()
                                .limit(5)
                                .map(place -> PlaceInfo.builder()
                                    .name(place.name())
                                    .category(place.category())
                                    .rating(place.rating())
                                    .address(place.address())
                                    .source(place.source())
                                    .tags(place.tags())
                                    .build())
                                .toList())
                            .build()
                    )))
                .build();

            log.info("=== Stage 1 테스트 완료 ===");
            log.info("총 장소 수: {} 개", result.places().size());
            log.info("카테고리별 분포:");
            result.categoryPlaces().forEach((category, places) -> 
                log.info("  - {}: {} 개", category, places.size()));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Stage 1 테스트 실패", e);
            return ResponseEntity.ok(new TestResponse(
                false,
                "테스트 실패: " + e.getMessage(),
                0L,
                "",
                "",
                0,
                0,
                Map.of(),
                0.0,
                List.of(),
                List.of(),
                Map.of()));
        }
    }

    /**
     * 미리 정의된 테스트 케이스들
     */
    @GetMapping("/presets")
    public ResponseEntity<?> getTestPresets() {
        List<TestRequest> presets = Arrays.asList(
            new TestRequest("서울", "CULTURAL", "MEDIUM", 
                Arrays.asList("박물관", "궁궐", "전통", "문화재"), 3, 2),
            new TestRequest("부산", "FOODIE", "HIGH", 
                Arrays.asList("맛집", "시장", "카페", "해산물"), 2, 4),
            new TestRequest("제주", "NATURE", "UNLIMITED", 
                Arrays.asList("자연", "바다", "산", "공원", "해변"), 4, 2),
            new TestRequest("강릉", "RELAXATION", "MEDIUM", 
                Arrays.asList("바다", "카페", "휴양", "자연"), 2, 3),
            new TestRequest("경주", "CULTURAL", "LOW", 
                Arrays.asList("유적지", "역사", "문화재", "전통"), 1, 2)
        );

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "미리 정의된 테스트 케이스들",
            "presets", presets
        ));
    }

    /**
     * 헬스체크 (데이터베이스 없이)
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Stage 1 서비스 정상 동작",
            "timestamp", System.currentTimeMillis(),
            "mode", "메모리 기반 테스트"
        ));
    }

    // ========== DTO 클래스들 ==========

    public record TestRequest(
        String destination,
        String travelStyle,
        String budget,
        List<String> interests,
        int tripDays,
        int companions
    ) {}

    @lombok.Builder
    public record TestResponse(
        boolean success,
        String message,
        Long executionTimeMs,
        String destination,
        String travelStyle,
        Integer tripDays,
        Integer totalPlaces,
        Map<String, Integer> categoryBreakdown,
        Double averageRating,
        List<String> warnings,
        List<PlaceInfo> samplePlaces,
        Map<String, CategoryDetail> categoryDetails
    ) {}

    @lombok.Builder
    public record PlaceInfo(
        String name,
        String category,
        Double rating,
        String address,
        String source,
        List<String> tags
    ) {}

    @lombok.Builder
    public record CategoryDetail(
        int count,
        double averageRating,
        List<PlaceInfo> topPlaces
    ) {}
}
