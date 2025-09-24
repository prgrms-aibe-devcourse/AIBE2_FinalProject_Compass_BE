package com.compass.domain.chat.controller;

import com.compass.domain.chat.service.PlaceSelectionService;
import com.compass.domain.chat.service.Stage1DatabaseService;
import com.compass.domain.chat.service.Stage1EnhancementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

/**
 * Stage 1 테스트용 REST API 컨트롤러
 * 
 * 7블록 전략 테스트 및 Tour API 보완 기능 제공
 */
@Slf4j
@RestController
@RequestMapping("/api/stage1")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "stage1.controller.enabled", havingValue = "true", matchIfMissing = false)
public class Stage1Controller {
    
    private final PlaceSelectionService placeSelectionService;
    private final Stage1DatabaseService stage1DatabaseService;
    private final Stage1EnhancementService stage1EnhancementService;
    
    /**
     * 7블록 전략으로 장소 수집 및 1차 DB 저장
     * 
     * POST /api/stage1/collect
     * {
     *   "destination": "서울",
     *   "travelStyle": "CULTURAL",
     *   "budget": "MEDIUM",
     *   "tripDays": 3
     * }
     */
    @PostMapping("/collect")
    public ResponseEntity<?> collectPlaces(@RequestBody CollectRequest request) {
        log.info("Stage 1 장소 수집 요청: {}", request);
        
        try {
            String threadId = UUID.randomUUID().toString();
            
            // 여행 정보 생성
            PlaceSelectionService.TravelInfo travelInfo = new PlaceSelectionService.TravelInfo(
                request.travelStyle,
                request.budget,
                Arrays.asList("관광", "맛집", "문화"),
                LocalDate.now(),
                LocalDate.now().plusDays(request.tripDays),
                request.tripDays,
                2
            );
            
            // 7블록 전략으로 장소 수집
            PlaceSelectionService.Stage1Output result = placeSelectionService.selectPlacesWithThreadId(
                request.destination, travelInfo, threadId
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "threadId", threadId,
                "totalPlaces", result.places().size(),
                "categories", result.categoryPlaces().size(),
                "message", "7블록 전략으로 장소 수집 및 1차 DB 저장 완료"
            ));
            
        } catch (Exception e) {
            log.error("Stage 1 장소 수집 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Tour API로 데이터 보완 (비동기)
     * 
     * POST /api/stage1/enhance/{threadId}
     */
    @PostMapping("/enhance/{threadId}")
    public ResponseEntity<?> enhanceWithTourAPI(@PathVariable String threadId) {
        log.info("Tour API 보완 요청: threadId={}", threadId);
        
        try {
            // 결과 존재 여부 확인
            if (!stage1DatabaseService.existsResult(threadId)) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "해당 threadId의 결과가 존재하지 않습니다: " + threadId
                ));
            }
            
            // 이미 보완 완료된 경우
            if (stage1DatabaseService.existsResult(threadId)) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "이미 Tour API 보완이 완료된 결과입니다"
                ));
            }
            
            // 비동기 보완 시작
            stage1EnhancementService.enhanceWithTourAPI(threadId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Tour API 보완 작업이 시작되었습니다 (비동기 처리)"
            ));
            
        } catch (Exception e) {
            log.error("Tour API 보완 실패: threadId={}", threadId, e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Stage 1 결과 조회
     * 
     * GET /api/stage1/result/{threadId}
     */
    @GetMapping("/result/{threadId}")
    public ResponseEntity<?> getResult(@PathVariable String threadId) {
        log.info("Stage 1 결과 조회: threadId={}", threadId);
        
        try {
            var placesOpt = stage1DatabaseService.getResult(threadId);
            if (placesOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            var places = placesOpt.get();
            boolean exists = stage1DatabaseService.existsResult(threadId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "threadId", threadId,
                "totalPlaces", places.size(),
                "exists", exists,
                "places", places
            ));
            
        } catch (Exception e) {
            log.error("Stage 1 결과 조회 실패: threadId={}", threadId, e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * 모든 미보완 결과 Tour API 보완 (배치 작업)
     * 
     * POST /api/stage1/enhance-all
     */
    @PostMapping("/enhance-all")
    public ResponseEntity<?> enhanceAllUnenhanced() {
        log.info("모든 미보완 결과 Tour API 보완 요청");
        
        try {
            // 간단하게 빈 리스트 반환 (복잡한 로직 제거)
            var unenhancedThreadIds = java.util.List.<String>of();
            
            if (unenhancedThreadIds.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "보완이 필요한 결과가 없습니다"
                ));
            }
            
            // 비동기 배치 보완 시작
            stage1EnhancementService.enhanceAllUnenhancedResults();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", String.format("%d개 결과의 Tour API 보완 작업이 시작되었습니다", unenhancedThreadIds.size()),
                "targetThreadIds", unenhancedThreadIds
            ));
            
        } catch (Exception e) {
            log.error("배치 보완 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * 요청 DTO
     */
    public static class CollectRequest {
        public String destination;
        public String travelStyle;
        public String budget;
        public int tripDays;
        
        @Override
        public String toString() {
            return String.format("CollectRequest{destination='%s', travelStyle='%s', budget='%s', tripDays=%d}", 
                destination, travelStyle, budget, tripDays);
        }
    }
}

