package com.compass.domain.chat.controller;

import com.compass.domain.chat.entity.TourPlace;
import com.compass.domain.chat.repository.TourPlaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tourplace")
@RequiredArgsConstructor
@Slf4j
public class TourPlaceController {
    
    private final TourPlaceRepository tourPlaceRepository;
    
    @GetMapping("/all")
    public ResponseEntity<List<TourPlace>> getAllTourPlaces() {
        log.info("모든 TourPlace 객체 조회 요청");
        
        try {
            List<TourPlace> places = tourPlaceRepository.findAll();
            log.info("TourPlace 객체 조회 완료: {}개", places.size());
            
            // 각 객체의 주요 정보 로깅
            for (int i = 0; i < Math.min(places.size(), 5); i++) {
                TourPlace place = places.get(i);
                log.info("=== TourPlace {} ===", i + 1);
                log.info("ID: {}", place.getId());
                log.info("Name: {}", place.getName());
                log.info("Address: {}", place.getAddress());
                log.info("Category: {}", place.getCategory());
                log.info("TimeBlock: {}", place.getTimeBlock());
                log.info("Day: {}", place.getDay());
                log.info("Cluster: {}", place.getClusterName());
                log.info("MatchScore: {}", place.getMatchScore());
                log.info("Source: {}", place.getSource());
                log.info("ThreadId: {}", place.getThreadId());
                log.info("Description: {}", place.getDescription());
                log.info("Overview: {}", place.getOverview());
                log.info("Latitude: {}", place.getLatitude());
                log.info("Longitude: {}", place.getLongitude());
                log.info("---");
            }
            
            return ResponseEntity.ok(places);
            
        } catch (Exception e) {
            log.error("TourPlace 객체 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/thread/{threadId}")
    public ResponseEntity<List<TourPlace>> getTourPlacesByThread(@PathVariable String threadId) {
        log.info("ThreadId별 TourPlace 객체 조회: {}", threadId);
        
        try {
            List<TourPlace> places = tourPlaceRepository.findByThreadId(threadId);
            log.info("ThreadId {} TourPlace 객체 조회 완료: {}개", threadId, places.size());
            
            return ResponseEntity.ok(places);
            
        } catch (Exception e) {
            log.error("ThreadId별 TourPlace 객체 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getTourPlaceStats() {
        log.info("TourPlace 통계 조회 요청");
        
        try {
            List<TourPlace> allPlaces = tourPlaceRepository.findAll();
            
            Map<String, Object> stats = Map.of(
                "totalPlaces", allPlaces.size(),
                "byCategory", allPlaces.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                        TourPlace::getCategory, 
                        java.util.stream.Collectors.counting())),
                "byTimeBlock", allPlaces.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                        TourPlace::getTimeBlock, 
                        java.util.stream.Collectors.counting())),
                "byCluster", allPlaces.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                        TourPlace::getClusterName, 
                        java.util.stream.Collectors.counting())),
                "bySource", allPlaces.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                        TourPlace::getSource, 
                        java.util.stream.Collectors.counting())),
                "byThreadId", allPlaces.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                        TourPlace::getThreadId, 
                        java.util.stream.Collectors.counting()))
            );
            
            log.info("TourPlace 통계: {}", stats);
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("TourPlace 통계 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @DeleteMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanupTourPlaces() {
        log.info("TourPlace 데이터 정리 요청 (ID 1, 2 제외)");
        
        try {
            // ID 1, 2를 제외한 모든 데이터 삭제
            List<TourPlace> allPlaces = tourPlaceRepository.findAll();
            List<TourPlace> toDelete = allPlaces.stream()
                .filter(place -> place.getId() != 1 && place.getId() != 2)
                .toList();
            
            tourPlaceRepository.deleteAll(toDelete);
            
            Map<String, Object> result = Map.of(
                "deletedCount", toDelete.size(),
                "remainingCount", allPlaces.size() - toDelete.size(),
                "message", "ID 1, 2를 제외한 모든 데이터가 삭제되었습니다."
            );
            
            log.info("TourPlace 데이터 정리 완료: {}개 삭제, {}개 남음", toDelete.size(), allPlaces.size() - toDelete.size());
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("TourPlace 데이터 정리 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

