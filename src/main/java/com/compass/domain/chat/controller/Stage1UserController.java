package com.compass.domain.chat.controller;

import com.compass.domain.chat.service.PlaceSelectionService;
import com.compass.domain.chat.service.Stage1DatabaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Stage 1 사용자 입력 처리 컨트롤러
 * 
 * 실제 사용자가 여행 정보를 입력하면 7블록 전략으로 장소를 수집하고 저장
 */
@Slf4j
@RestController
@RequestMapping("/api/stage1")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "stage1.user.controller.enabled", havingValue = "true", matchIfMissing = true)
public class Stage1UserController {
    
    private final PlaceSelectionService placeSelectionService;
    private final Stage1DatabaseService stage1DatabaseService;
    
    /**
     * 사용자 여행 정보 입력 및 장소 수집
     * 
     * @param request 사용자 입력 데이터
     * @return 수집된 장소 정보
     */
    @PostMapping("/collect")
    public ResponseEntity<?> collectPlaces(@RequestBody UserTravelRequest request) {
        log.info("사용자 여행 정보 입력: {}", request);
        
        try {
            // TravelInfo 객체 생성 (간단한 버전)
            var travelInfo = new PlaceSelectionService.TravelInfo(
                request.getTravelStyle(),
                "MEDIUM",  // 기본 예산
                List.of("관광"),  // 기본 관심사
                java.time.LocalDate.now(),  // 시작일
                java.time.LocalDate.now().plusDays(request.getDays() - 1),  // 종료일
                request.getDays(),
                1  // 기본 동반자 수
            );
            
            // 7블록 전략으로 장소 수집
            var result = placeSelectionService.selectPlacesWithThreadId(
                request.getDestination(),
                travelInfo,
                request.getThreadId()
            );
            
            log.info("장소 수집 완료: {}개 장소, ThreadId: {}", 
                result.places().size(), request.getThreadId());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "threadId", request.getThreadId(),
                "destination", request.getDestination(),
                "travelStyle", request.getTravelStyle(),
                "days", request.getDays(),
                "totalPlaces", result.places().size(),
                "places", result.places(),
                "statistics", result.statistics(),
                "warnings", result.warnings()
            ));
            
        } catch (Exception e) {
            log.error("장소 수집 실패: {}", request, e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "장소 수집 실패: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 저장된 결과 조회
     * 
     * @param threadId 스레드 ID
     * @return 저장된 장소 정보
     */
    @GetMapping("/result/{threadId}")
    public ResponseEntity<?> getResult(@PathVariable String threadId) {
        log.info("결과 조회 요청: threadId={}", threadId);
        
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
            log.error("결과 조회 실패: threadId={}", threadId, e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "결과 조회 실패: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 사용자 여행 요청 DTO
     */
    public static class UserTravelRequest {
        private String threadId;
        private String destination;
        private String travelStyle;
        private Integer days;
        
        // Getters and Setters
        public String getThreadId() { return threadId; }
        public void setThreadId(String threadId) { this.threadId = threadId; }
        
        public String getDestination() { return destination; }
        public void setDestination(String destination) { this.destination = destination; }
        
        public String getTravelStyle() { return travelStyle; }
        public void setTravelStyle(String travelStyle) { this.travelStyle = travelStyle; }
        
        public Integer getDays() { return days; }
        public void setDays(Integer days) { this.days = days; }
        
        @Override
        public String toString() {
            return "UserTravelRequest{" +
                    "threadId='" + threadId + '\'' +
                    ", destination='" + destination + '\'' +
                    ", travelStyle='" + travelStyle + '\'' +
                    ", days=" + days +
                    '}';
        }
    }
}
