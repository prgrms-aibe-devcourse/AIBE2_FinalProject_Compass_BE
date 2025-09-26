package com.compass.domain.chat.controller;

import com.compass.domain.chat.service.GooglePlacesDetailEnrichmentService;
import com.compass.domain.chat.service.TravelCandidateEnrichmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/travel-candidates")
@RequiredArgsConstructor
@Slf4j
public class TravelCandidateEnrichmentController {

    private final TravelCandidateEnrichmentService enrichmentService;
    private final GooglePlacesDetailEnrichmentService googlePlacesDetailService;

    /**
     * Google Places API로 반려동물 동반 여부 및 입장료 정보 업데이트
     */
    @PostMapping("/enrich/google-places")
    public ResponseEntity<Map<String, Object>> enrichWithGooglePlaces() {
        log.info("Starting Google Places enrichment for pet-friendly and admission fee info");

        Map<String, Object> response = new HashMap<>();
        try {
            // 비동기로 실행
            new Thread(() -> {
                try {
                    googlePlacesDetailService.enrichCandidatesWithAdditionalInfo();
                } catch (Exception e) {
                    log.error("Error during enrichment: ", e);
                }
            }).start();

            response.put("status", "started");
            response.put("message", "Google Places enrichment process started in background");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to start enrichment: ", e);
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 특정 지역의 데이터만 업데이트
     */
    @PostMapping("/enrich/google-places/{region}")
    public ResponseEntity<Map<String, Object>> enrichRegionWithGooglePlaces(@PathVariable String region) {
        log.info("Starting Google Places enrichment for region: {}", region);

        Map<String, Object> response = new HashMap<>();
        try {
            // 비동기로 실행
            new Thread(() -> {
                try {
                    googlePlacesDetailService.enrichSpecificRegion(region);
                } catch (Exception e) {
                    log.error("Error during regional enrichment: ", e);
                }
            }).start();

            response.put("status", "started");
            response.put("region", region);
            response.put("message", "Google Places enrichment for " + region + " started in background");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to start regional enrichment: ", e);
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Tour API와 Perplexity를 사용한 기존 데이터 보강
     */
    @PostMapping("/enrich/tour-perplexity/{region}")
    public ResponseEntity<Map<String, Object>> enrichWithTourAndPerplexity(@PathVariable String region) {
        log.info("Starting Tour API and Perplexity enrichment for region: {}", region);

        Map<String, Object> response = new HashMap<>();
        try {
            int enrichedCount = enrichmentService.enrichRegionData(region);

            response.put("status", "completed");
            response.put("region", region);
            response.put("enrichedCount", enrichedCount);
            response.put("message", "Enriched " + enrichedCount + " candidates in " + region);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to enrich with Tour/Perplexity: ", e);
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 보강 상태 통계 조회
     */
    @GetMapping("/statistics/{region}")
    public ResponseEntity<Map<String, Object>> getEnrichmentStatistics(@PathVariable String region) {
        try {
            Map<String, Object> stats = enrichmentService.getEnrichmentStatistics(region);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Failed to get statistics: ", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 전체 지역 보강 (모든 API 활용)
     */
    @PostMapping("/enrich/all")
    public ResponseEntity<Map<String, Object>> enrichAllRegions() {
        log.info("Starting comprehensive enrichment for all regions");

        Map<String, Object> response = new HashMap<>();
        try {
            // 비동기로 실행
            new Thread(() -> {
                try {
                    // 1. Tour API와 Perplexity로 기본 정보 수집
                    enrichmentService.enrichAllRegionsAsync();

                    // 2. Google Places로 추가 정보 수집
                    googlePlacesDetailService.enrichCandidatesWithAdditionalInfo();

                    log.info("All enrichment processes completed");
                } catch (Exception e) {
                    log.error("Error during comprehensive enrichment: ", e);
                }
            }).start();

            response.put("status", "started");
            response.put("message", "Comprehensive enrichment process started for all regions");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to start comprehensive enrichment: ", e);
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}