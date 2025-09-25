package com.compass.domain.chat.route_optimization.controller;

import com.compass.domain.chat.route_optimization.model.CategoryBasedSelectionRequest;
import com.compass.domain.chat.route_optimization.model.CategoryBasedSelectionRequest.*;
import com.compass.domain.chat.route_optimization.service.CategoryBasedDistributionService;
import com.compass.domain.chat.route_optimization.service.RouteOptimizationOrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 카테고리 기반 동선 최적화 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/route-optimization/category")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5500", "http://localhost:5501", "http://127.0.0.1:5500", "http://127.0.0.1:5501"})
@Slf4j
public class CategoryBasedOptimizationController {

    private final CategoryBasedDistributionService distributionService;
    private final RouteOptimizationOrchestrationService orchestrationService;

    /**
     * Stage 1: 카테고리별 장소 그룹화
     */
    @PostMapping("/group-by-category")
    public ResponseEntity<Map<String, Object>> groupPlacesByCategory(
            @RequestBody List<PlaceCandidate> places) {

        log.info("Grouping {} places by category", places.size());

        Map<String, List<PlaceCandidate>> grouped = places.stream()
            .collect(Collectors.groupingBy(PlaceCandidate::category));

        Map<String, Object> response = new HashMap<>();
        response.put("total", places.size());
        response.put("categories", grouped.keySet());
        response.put("groupedPlaces", grouped);

        // 카테고리별 통계
        Map<String, Integer> stats = new HashMap<>();
        grouped.forEach((category, list) -> stats.put(category, list.size()));
        response.put("statistics", stats);

        return ResponseEntity.ok(response);
    }

    /**
     * Stage 2: 카테고리 기반 일자별 분배
     */
    @PostMapping("/distribute")
    public ResponseEntity<Map<String, Object>> distributeWithCategories(
            @RequestBody CategoryBasedSelectionRequest request) {

        log.info("Distributing places for {} days with category constraints",
                request.tripDays());

        // 카테고리 기반 분배 수행
        Map<Integer, List<PlaceCandidate>> distribution =
            distributionService.distributeWithConstraints(request);

        Map<String, Object> response = new HashMap<>();
        response.put("threadId", request.threadId());
        response.put("sessionId", request.sessionId());
        response.put("tripDays", request.tripDays());
        response.put("dailyItinerary", convertToItinerary(distribution));

        // 통계 정보 추가
        Map<String, Object> stats = calculateStatistics(distribution);
        response.put("statistics", stats);

        return ResponseEntity.ok(response);
    }

    /**
     * Stage 3: 최종 경로 최적화
     */
    @PostMapping("/optimize-route")
    public ResponseEntity<Map<String, Object>> optimizeRoute(
            @RequestBody Map<Integer, List<PlaceCandidate>> dailyDistribution) {

        log.info("Optimizing routes for {} days", dailyDistribution.size());

        Map<String, Object> optimizedItinerary = new HashMap<>();

        for (Map.Entry<Integer, List<PlaceCandidate>> entry : dailyDistribution.entrySet()) {
            int day = entry.getKey();
            List<PlaceCandidate> places = entry.getValue();

            // TSP 알고리즘으로 최적 경로 계산
            List<PlaceCandidate> optimizedRoute = optimizeWithTSP(places);

            Map<String, Object> dayItinerary = new HashMap<>();
            dayItinerary.put("places", optimizedRoute);
            dayItinerary.put("totalDistance", calculateTotalDistance(optimizedRoute));
            dayItinerary.put("estimatedTime", calculateEstimatedTime(optimizedRoute));

            optimizedItinerary.put("day" + day, dayItinerary);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("optimizedItinerary", optimizedItinerary);
        response.put("optimization", "TSP_2OPT");

        return ResponseEntity.ok(response);
    }

    /**
     * 전체 플로우: Stage 1-3 통합
     */
    @PostMapping("/complete-flow")
    public ResponseEntity<Map<String, Object>> completeOptimizationFlow(
            @RequestBody CategoryBasedSelectionRequest request) {

        log.info("Running complete optimization flow for {} days", request.tripDays());

        // Stage 1: 카테고리 그룹화
        Map<String, List<PlaceCandidate>> categorized = request.getPlacesByCategory();

        // Stage 2: 제약 기반 분배
        Map<Integer, List<PlaceCandidate>> distribution =
            distributionService.distributeWithConstraints(request);

        // Stage 3: 경로 최적화
        Map<String, Object> optimizedItinerary = new HashMap<>();
        for (Map.Entry<Integer, List<PlaceCandidate>> entry : distribution.entrySet()) {
            int day = entry.getKey();
            List<PlaceCandidate> optimized = optimizeWithTSP(entry.getValue());

            Map<String, Object> dayData = new HashMap<>();
            dayData.put("places", optimized);
            dayData.put("route", generateRouteInfo(optimized));
            optimizedItinerary.put("day" + day, dayData);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("threadId", request.threadId());
        response.put("sessionId", request.sessionId());
        response.put("stage1_categorization", categorized);
        response.put("stage2_distribution", distribution);
        response.put("stage3_optimization", optimizedItinerary);

        return ResponseEntity.ok(response);
    }

    // 헬퍼 메서드들

    private Map<String, Object> convertToItinerary(
            Map<Integer, List<PlaceCandidate>> distribution) {

        Map<String, Object> itinerary = new HashMap<>();

        for (Map.Entry<Integer, List<PlaceCandidate>> entry : distribution.entrySet()) {
            Map<String, List<PlaceCandidate>> timeBlocks = entry.getValue().stream()
                .collect(Collectors.groupingBy(PlaceCandidate::timeBlock));

            itinerary.put("day" + entry.getKey(), timeBlocks);
        }

        return itinerary;
    }

    private Map<String, Object> calculateStatistics(
            Map<Integer, List<PlaceCandidate>> distribution) {

        Map<String, Object> stats = new HashMap<>();
        int totalPlaces = distribution.values().stream()
            .mapToInt(List::size)
            .sum();

        stats.put("totalPlaces", totalPlaces);
        stats.put("averagePlacesPerDay", totalPlaces / (double) distribution.size());

        // 카테고리별 통계
        Map<String, Integer> categoryCount = new HashMap<>();
        distribution.values().stream()
            .flatMap(List::stream)
            .forEach(place -> categoryCount.merge(place.category(), 1, Integer::sum));
        stats.put("categoryDistribution", categoryCount);

        return stats;
    }

    private List<PlaceCandidate> optimizeWithTSP(List<PlaceCandidate> places) {
        // TSP 알고리즘 구현 (2-opt improvement)
        if (places.size() <= 2) {
            return places;
        }

        List<PlaceCandidate> currentRoute = new ArrayList<>(places);
        boolean improved = true;

        while (improved) {
            improved = false;

            for (int i = 1; i < currentRoute.size() - 1; i++) {
                for (int j = i + 1; j < currentRoute.size(); j++) {
                    double currentDistance = calculateTotalDistance(currentRoute);

                    List<PlaceCandidate> newRoute = twoOptSwap(currentRoute, i, j);
                    double newDistance = calculateTotalDistance(newRoute);

                    if (newDistance < currentDistance) {
                        currentRoute = newRoute;
                        improved = true;
                    }
                }
            }
        }

        return currentRoute;
    }

    private List<PlaceCandidate> twoOptSwap(List<PlaceCandidate> route, int i, int j) {
        List<PlaceCandidate> newRoute = new ArrayList<>(route.subList(0, i));

        for (int k = j; k >= i; k--) {
            newRoute.add(route.get(k));
        }

        if (j + 1 < route.size()) {
            newRoute.addAll(route.subList(j + 1, route.size()));
        }

        return newRoute;
    }

    private double calculateTotalDistance(List<PlaceCandidate> route) {
        double totalDistance = 0;

        for (int i = 0; i < route.size() - 1; i++) {
            totalDistance += calculateDistance(route.get(i), route.get(i + 1));
        }

        return totalDistance;
    }

    private double calculateDistance(PlaceCandidate p1, PlaceCandidate p2) {
        double lat1 = Math.toRadians(p1.latitude());
        double lat2 = Math.toRadians(p2.latitude());
        double deltaLat = lat2 - lat1;
        double deltaLng = Math.toRadians(p2.longitude() - p1.longitude());

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                   Math.cos(lat1) * Math.cos(lat2) *
                   Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return 6371 * c;  // km
    }

    private int calculateEstimatedTime(List<PlaceCandidate> route) {
        int totalTime = route.stream()
            .mapToInt(PlaceCandidate::estimatedMinutes)
            .sum();

        // 이동 시간 추가 (장소당 평균 20분)
        totalTime += (route.size() - 1) * 20;

        return totalTime;
    }

    private Map<String, Object> generateRouteInfo(List<PlaceCandidate> places) {
        Map<String, Object> routeInfo = new HashMap<>();
        routeInfo.put("totalPlaces", places.size());
        routeInfo.put("estimatedTime", calculateEstimatedTime(places));
        routeInfo.put("totalDistance", calculateTotalDistance(places));

        List<Map<String, Object>> waypoints = new ArrayList<>();
        for (int i = 0; i < places.size(); i++) {
            PlaceCandidate place = places.get(i);
            Map<String, Object> waypoint = new HashMap<>();
            waypoint.put("order", i + 1);
            waypoint.put("place", place);
            waypoints.add(waypoint);
        }
        routeInfo.put("waypoints", waypoints);

        return routeInfo;
    }
}