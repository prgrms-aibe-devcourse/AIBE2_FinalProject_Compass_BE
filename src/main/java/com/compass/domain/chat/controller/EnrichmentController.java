package com.compass.domain.chat.controller;

import com.compass.domain.chat.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/enrichment")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class EnrichmentController {

    private final TravelCandidateFullEnrichmentService fullEnrichmentService;
    private final EnrichmentOrchestrationService orchestrationService;
    private final GooglePlacesEnrichmentService googlePlacesService;
    private final KakaoMapEnrichmentService kakaoMapService;
    private final TourApiEnrichmentService tourApiService;
    private final AiEnrichmentService aiService;
    private final GeminiDescriptionService geminiDescriptionService;

    // ========== 새로운 전체 보강 (리팩토링된 서비스) ==========

    @PostMapping("/execute-full-refactored")
    public ResponseEntity<Map<String, Object>> executeFullEnrichmentRefactored() {
        log.info("리팩토링된 전체 데이터 보강 실행");

        CompletableFuture<Map<String, Object>> future = fullEnrichmentService.executeFullEnrichmentAsync();

        return ResponseEntity.accepted().body(Map.of(
            "status", "processing",
            "message", "전체 보강 프로세스가 시작되었습니다 (리팩토링 버전)",
            "estimatedTime", "약 60-70분",
            "phases", List.of(
                "Phase 1: Google Places API (약 17분)",
                "Phase 2: 카카오맵 API (약 6분)",
                "Phase 3: Tour API (약 10분)",
                "Phase 4: AI 보강 (약 33분)"
            )
        ));
    }

    @PostMapping("/execute-fast-refactored")
    public ResponseEntity<Map<String, Object>> executeFastEnrichmentRefactored() {
        log.info("리팩토링된 빠른 데이터 보강 실행");

        Map<String, Object> result = fullEnrichmentService.executeFastEnrichment();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/execute-essential-refactored")
    public ResponseEntity<Map<String, Object>> executeEssentialEnrichmentRefactored() {
        log.info("리팩토링된 필수 데이터 보강 실행");

        Map<String, Object> result = fullEnrichmentService.executeEssentialEnrichment();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/statistics-refactored")
    public ResponseEntity<Map<String, Object>> getEnrichmentStatisticsRefactored() {
        log.info("리팩토링된 보강 상태 통계 조회");

        Map<String, Object> stats = fullEnrichmentService.getEnrichmentStatistics();
        return ResponseEntity.ok(stats);
    }

    // ========== 전체 보강 (기존) ==========

    // 전체 보강 프로세스 시작
    @PostMapping("/start-full")
    public ResponseEntity<Map<String, Object>> startFullEnrichment() {
        log.info("전체 보강 프로세스 시작 요청");

        if (!orchestrationService.canStartEnrichment()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "status", "conflict",
                "message", "보강 프로세스가 이미 실행 중입니다",
                "currentStatus", orchestrationService.getCurrentStatus()
            ));
        }

        CompletableFuture<Map<String, Object>> future = orchestrationService.executeFullEnrichment();

        return ResponseEntity.accepted().body(Map.of(
            "status", "started",
            "message", "전체 보강 프로세스가 백그라운드에서 시작되었습니다",
            "estimatedTime", "약 3-4시간",
            "phases", List.of(
                "Phase 1: Google Places API",
                "Phase 2: 카카오맵 API",
                "Phase 3: Tour API",
                "Phase 4: AI 보강"
            )
        ));
    }

    // 보강 프로세스 상태 조회
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getEnrichmentStatus() {
        Map<String, Object> status = orchestrationService.getCurrentStatus();
        return ResponseEntity.ok(status);
    }

    // 보강 프로세스 중지
    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stopEnrichment() {
        orchestrationService.stopEnrichment();
        return ResponseEntity.ok(Map.of(
            "status", "stopped",
            "message", "보강 프로세스 중지 요청이 처리되었습니다"
        ));
    }

    // ========== 선택적 API 보강 ==========

    // 선택적 API 보강 시작
    @PostMapping("/selective")
    public ResponseEntity<Map<String, Object>> startSelectiveEnrichment(
            @RequestParam String api,
            @RequestParam(required = false) String region,
            @RequestParam(defaultValue = "100") int limit) {

        log.info("선택적 보강 시작: API={}, Region={}, Limit={}", api, region, limit);

        if (!orchestrationService.canStartEnrichment()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "status", "conflict",
                "message", "다른 보강 프로세스가 실행 중입니다"
            ));
        }

        CompletableFuture<Map<String, Object>> future = orchestrationService.executeSelectiveEnrichment(api);

        return ResponseEntity.accepted().body(Map.of(
            "status", "started",
            "api", api,
            "message", api + " API 보강이 시작되었습니다"
        ));
    }

    // ========== Google Places API ==========

    @PostMapping("/google/all")
    public ResponseEntity<Map<String, Object>> enrichAllWithGoogle() {
        log.info("Google Places 전체 보강 요청");

        int enriched = googlePlacesService.enrichAllWithGooglePlaces();

        return ResponseEntity.ok(Map.of(
            "api", "google",
            "enriched", enriched,
            "message", String.format("Google Places API로 %d개 데이터 보강 완료", enriched)
        ));
    }

    @PostMapping("/google/page")
    public ResponseEntity<Map<String, Object>> enrichGoogleByPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {

        int enriched = googlePlacesService.enrichByPage(page, size);

        return ResponseEntity.ok(Map.of(
            "api", "google",
            "page", page,
            "size", size,
            "enriched", enriched,
            "message", String.format("페이지 %d의 %d개 데이터 중 %d개 보강 완료", page, size, enriched)
        ));
    }

    @PostMapping("/google/batch")
    public ResponseEntity<Map<String, Object>> enrichGoogleBatch(@RequestBody List<Long> candidateIds) {
        CompletableFuture<Integer> future = googlePlacesService.enrichBatchAsync(candidateIds);

        return ResponseEntity.accepted().body(Map.of(
            "api", "google",
            "status", "processing",
            "count", candidateIds.size(),
            "message", "비동기 Google Places 보강이 시작되었습니다"
        ));
    }

    @PostMapping("/google/missing")
    public ResponseEntity<Map<String, Object>> enrichGoogleMissingColumns() {
        int enriched = googlePlacesService.enrichMissingGoogleDetails();

        return ResponseEntity.ok(Map.of(
            "api", "google",
            "enriched", enriched,
            "message", String.format("Google Places로 빈 필드가 있는 %d개 장소를 업데이트했습니다", enriched)
        ));
    }

    @PostMapping("/gemini/descriptions")
    public ResponseEntity<Map<String, Object>> regenerateDescriptions(
        @RequestParam(defaultValue = "false") boolean onlyIfEmpty
    ) {
        int updated = geminiDescriptionService.regenerateDescriptions(onlyIfEmpty);

        return ResponseEntity.ok(Map.of(
            "api", "gemini",
            "updated", updated,
            "onlyIfEmpty", onlyIfEmpty,
            "message", String.format("Gemini로 %d개 장소의 설명을 갱신했습니다", updated)
        ));
    }

    @PostMapping("/google/refresh-ratings")
    public ResponseEntity<Map<String, Object>> refreshGoogleRatings() {
        int updated = googlePlacesService.refreshRatingsAndReviews();

        return ResponseEntity.ok(Map.of(
            "api", "google",
            "updated", updated,
            "message", String.format("Google Place ID 기반으로 %d개 장소의 위도·경도, 평점, 리뷰수를 갱신했습니다", updated)
        ));
    }

    // ========== 카카오맵 API ==========

    @PostMapping("/kakao/all")
    public ResponseEntity<Map<String, Object>> enrichAllWithKakao() {
        log.info("카카오맵 전체 보강 요청");

        int enriched = kakaoMapService.enrichAllWithKakaoMap();

        return ResponseEntity.ok(Map.of(
            "api", "kakao",
            "enriched", enriched,
            "message", String.format("카카오맵 API로 %d개 데이터 보강 완료", enriched)
        ));
    }

    @PostMapping("/kakao/region/{region}")
    public ResponseEntity<Map<String, Object>> enrichKakaoByRegion(@PathVariable String region) {
        int enriched = kakaoMapService.enrichByRegion(region);

        return ResponseEntity.ok(Map.of(
            "api", "kakao",
            "region", region,
            "enriched", enriched,
            "message", String.format("%s 지역 %d개 데이터 보강 완료", region, enriched)
        ));
    }

    @PostMapping("/kakao/batch")
    public ResponseEntity<Map<String, Object>> enrichKakaoBatch(@RequestBody List<Long> candidateIds) {
        CompletableFuture<Integer> future = kakaoMapService.enrichBatchAsync(candidateIds);

        return ResponseEntity.accepted().body(Map.of(
            "api", "kakao",
            "status", "processing",
            "count", candidateIds.size(),
            "message", "비동기 카카오맵 보강이 시작되었습니다"
        ));
    }

    // ========== Tour API ==========

    @PostMapping("/tour/all")
    public ResponseEntity<Map<String, Object>> enrichAllWithTour() {
        log.info("Tour API 전체 보강 요청");

        int enriched = tourApiService.enrichAllWithTourApi();

        return ResponseEntity.ok(Map.of(
            "api", "tour",
            "enriched", enriched,
            "message", String.format("Tour API로 %d개 데이터 보강 완료", enriched)
        ));
    }

    @PostMapping("/tour/attractions")
    public ResponseEntity<Map<String, Object>> enrichTouristAttractions() {
        int enriched = tourApiService.enrichTouristAttractions();

        return ResponseEntity.ok(Map.of(
            "api", "tour",
            "type", "attractions",
            "enriched", enriched,
            "message", String.format("관광지 %d개 데이터 보강 완료", enriched)
        ));
    }

    // ========== AI 보강 ==========

    @PostMapping("/ai/top")
    public ResponseEntity<Map<String, Object>> enrichTopWithAI(
            @RequestParam(defaultValue = "50") int limit) {

        log.info("AI 상위 {}개 보강 요청", limit);

        int enriched = aiService.enrichTopPlacesWithAI(limit);

        return ResponseEntity.ok(Map.of(
            "api", "ai",
            "limit", limit,
            "enriched", enriched,
            "message", String.format("상위 %d개 장소 AI 보강 완료", enriched)
        ));
    }

    @PostMapping("/ai/batch")
    public ResponseEntity<Map<String, Object>> enrichAIBatch(@RequestBody List<Long> candidateIds) {
        CompletableFuture<Integer> future = aiService.enrichBatchWithAIAsync(candidateIds);

        return ResponseEntity.accepted().body(Map.of(
            "api", "ai",
            "status", "processing",
            "count", candidateIds.size(),
            "message", "비동기 AI 보강이 시작되었습니다 (약 20초/개)",
            "estimatedTime", String.format("약 %d분", candidateIds.size() / 3)
        ));
    }

    // ========== 통계 정보 ==========

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getEnrichmentStatistics() {
        Map<String, Object> statistics = Map.of(
            "google", googlePlacesService.getEnrichmentStatistics(),
            "kakao", kakaoMapService.getKakaoEnrichmentStatistics(),
            "tour", tourApiService.getTourApiStatistics(),
            "ai", aiService.getAIEnrichmentStatistics()
        );

        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/statistics/{api}")
    public ResponseEntity<Map<String, Object>> getApiStatistics(@PathVariable String api) {
        Map<String, Object> stats;

        switch (api.toLowerCase()) {
            case "google":
                stats = googlePlacesService.getEnrichmentStatistics();
                break;
            case "kakao":
                stats = kakaoMapService.getKakaoEnrichmentStatistics();
                break;
            case "tour":
                stats = tourApiService.getTourApiStatistics();
                break;
            case "ai":
                stats = aiService.getAIEnrichmentStatistics();
                break;
            default:
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid API type",
                    "validTypes", List.of("google", "kakao", "tour", "ai")
                ));
        }

        return ResponseEntity.ok(stats);
    }

    // ========== 테스트용 엔드포인트 ==========

    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testEnrichment() {
        return ResponseEntity.ok(Map.of(
            "status", "ready",
            "apis", Map.of(
                "google", "Google Places API",
                "kakao", "카카오맵 API",
                "tour", "한국관광공사 Tour API",
                "ai", "Perplexity + OpenAI"
            ),
            "endpoints", List.of(
                "/api/v1/admin/enrichment/start-full",
                "/api/v1/admin/enrichment/selective?api={api}",
                "/api/v1/admin/enrichment/status",
                "/api/v1/admin/enrichment/statistics"
            )
        ));
    }
}
