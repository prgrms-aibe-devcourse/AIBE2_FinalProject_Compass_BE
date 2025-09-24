package com.compass.domain.chat.controller;

import com.compass.domain.chat.entity.TravelPlaceEntity;
import com.compass.domain.chat.service.PlaceSelectionService;
import com.compass.domain.chat.service.TravelPlaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Stage 1 테스트용 컨트롤러
 * 
 * RDS 연결 및 Stage 1 파이프라인 테스트를 위한 임시 컨트롤러입니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/test/stage1")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.datasource.url")
public class Stage1TestController {

    private final PlaceSelectionService placeSelectionService;
    private final TravelPlaceService travelPlaceService;

    /**
     * Stage 1 전체 파이프라인 테스트
     */
    @PostMapping("/test-pipeline")
    public ResponseEntity<?> testStage1Pipeline(@RequestBody TestRequest request) {
        log.info("=== Stage 1 파이프라인 테스트 시작 ===");
        log.info("목적지: {}, 스타일: {}, 일수: {}일", request.destination, request.travelStyle, request.tripDays);

        try {
            // 테스트용 여행 정보 생성
            PlaceSelectionService.TravelInfo travelInfo = new PlaceSelectionService.TravelInfo(
                request.travelStyle,
                request.budget,
                request.interests,
                LocalDate.now().plusDays(30), // 30일 후 시작
                LocalDate.now().plusDays(30 + request.tripDays - 1), // 여행 일수만큼
                request.tripDays,
                request.companions
            );

            // Stage 1 실행
            long startTime = System.currentTimeMillis();
            PlaceSelectionService.Stage1Output stage1Result = placeSelectionService.selectPlaces(
                request.destination, travelInfo);
            long endTime = System.currentTimeMillis();

            // 결과 검증
            if (stage1Result == null || stage1Result.places().isEmpty()) {
                return ResponseEntity.ok(TestResponse.builder()
                    .success(false)
                    .message("장소 검색 결과가 없습니다")
                    .executionTimeMs(endTime - startTime)
                    .build());
            }

            // 데이터베이스에 저장
            String testThreadId = "test-" + UUID.randomUUID().toString();
            String testUserId = "test-user-" + UUID.randomUUID().toString();

            List<TravelPlaceEntity> savedEntities = travelPlaceService.saveStage1Results(
                testThreadId, testUserId, request.destination, stage1Result,
                request.travelStyle, request.budget
            );

            // 저장된 데이터 조회하여 검증
            List<TravelPlaceEntity> retrievedPlaces = travelPlaceService.getPlacesByThreadId(testThreadId);
            TravelPlaceService.TravelPlaceStatistics statistics = travelPlaceService.getStatistics(testThreadId);

            log.info("=== Stage 1 테스트 완료 ===");
            log.info("실행 시간: {}ms", endTime - startTime);
            log.info("검색된 장소: {} 개", stage1Result.places().size());
            log.info("저장된 장소: {} 개", savedEntities.size());
            log.info("조회된 장소: {} 개", retrievedPlaces.size());

            return ResponseEntity.ok(TestResponse.builder()
                .success(true)
                .message("Stage 1 파이프라인 테스트 성공")
                .executionTimeMs(endTime - startTime)
                .searchedPlaceCount(stage1Result.places().size())
                .savedPlaceCount(savedEntities.size())
                .retrievedPlaceCount(retrievedPlaces.size())
                .averageRating(stage1Result.statistics().averageRating())
                .categoryDistribution(stage1Result.statistics().categoryDistribution())
                .warnings(stage1Result.warnings())
                .threadId(testThreadId)
                .places(stage1Result.places().stream()
                    .limit(10) // 처음 10개만 반환
                    .map(place -> PlaceInfo.builder()
                        .name(place.name())
                        .category(place.category())
                        .rating(place.rating())
                        .address(place.address())
                        .source(place.source())
                        .build())
                    .toList())
                .dbStatistics(DbStatistics.builder()
                    .totalCount(statistics.totalCount())
                    .averageRating(statistics.averageRating())
                    .categoryDistribution(statistics.categoryDistribution())
                    .build())
                .build());

        } catch (Exception e) {
            log.error("Stage 1 테스트 실패", e);
            return ResponseEntity.ok(TestResponse.builder()
                .success(false)
                .message("테스트 실패: " + e.getMessage())
                .build());
        }
    }

    /**
     * 저장된 여행 장소 조회
     */
    @GetMapping("/places/{threadId}")
    public ResponseEntity<?> getPlacesByThreadId(@PathVariable String threadId) {
        try {
            List<TravelPlaceEntity> places = travelPlaceService.getPlacesByThreadId(threadId);
            TravelPlaceService.TravelPlaceStatistics statistics = travelPlaceService.getStatistics(threadId);

            return ResponseEntity.ok(PlaceQueryResponse.builder()
                .success(true)
                .threadId(threadId)
                .placeCount(places.size())
                .places(places.stream()
                    .map(entity -> PlaceInfo.builder()
                        .name(entity.getPlaceName())
                        .category(entity.getCategory())
                        .rating(entity.getRating())
                        .address(entity.getAddress())
                        .source(entity.getSource())
                        .build())
                    .toList())
                .statistics(DbStatistics.builder()
                    .totalCount(statistics.totalCount())
                    .averageRating(statistics.averageRating())
                    .categoryDistribution(statistics.categoryDistribution())
                    .build())
                .build());

        } catch (Exception e) {
            log.error("장소 조회 실패", e);
            return ResponseEntity.ok(PlaceQueryResponse.builder()
                .success(false)
                .message("조회 실패: " + e.getMessage())
                .build());
        }
    }

    /**
     * 데이터베이스 연결 테스트
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        try {
            // 간단한 데이터베이스 쿼리로 연결 확인
            long count = travelPlaceService.getPlacesByDestination("서울").size();
            
            return ResponseEntity.ok(HealthResponse.builder()
                .success(true)
                .message("데이터베이스 연결 정상")
                .timestamp(System.currentTimeMillis())
                .sampleDataCount(count)
                .build());

        } catch (Exception e) {
            log.error("헬스체크 실패", e);
            return ResponseEntity.ok(HealthResponse.builder()
                .success(false)
                .message("데이터베이스 연결 실패: " + e.getMessage())
                .timestamp(System.currentTimeMillis())
                .build());
        }
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
        Integer searchedPlaceCount,
        Integer savedPlaceCount,
        Integer retrievedPlaceCount,
        Double averageRating,
        java.util.Map<String, Long> categoryDistribution,
        List<String> warnings,
        String threadId,
        List<PlaceInfo> places,
        DbStatistics dbStatistics
    ) {}

    @lombok.Builder
    public record PlaceQueryResponse(
        boolean success,
        String message,
        String threadId,
        Integer placeCount,
        List<PlaceInfo> places,
        DbStatistics statistics
    ) {}

    @lombok.Builder
    public record HealthResponse(
        boolean success,
        String message,
        Long timestamp,
        Long sampleDataCount
    ) {}

    @lombok.Builder
    public record PlaceInfo(
        String name,
        String category,
        Double rating,
        String address,
        String source
    ) {}

    @lombok.Builder
    public record DbStatistics(
        int totalCount,
        double averageRating,
        java.util.Map<String, Long> categoryDistribution
    ) {}
}
