package com.compass.domain.chat.route_optimization.service;

import com.compass.domain.chat.function.processing.phase3.date_selection.model.TourPlace;
import com.compass.domain.chat.route_optimization.client.KakaoMobilityClient;
import com.compass.domain.chat.route_optimization.client.KakaoMobilityClient.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteOptimizationService {

    private final KakaoMobilityClient kakaoMobilityClient;

    // 다양한 최적화 전략
    public enum OptimizationStrategy {
        SHORTEST_DISTANCE,    // 최단 거리
        FASTEST_TIME,        // 최소 시간
        BALANCED,            // 거리/시간 균형
        SCENIC_ROUTE,        // 경치 좋은 길
        AVOID_TRAFFIC        // 교통 혼잡 회피
    }

    // 일정 최적화 (고급 알고리즘)
    public OptimizedRoute optimizeRoute(
        List<TourPlace> places,
        OptimizationStrategy strategy,
        TransportMode transportMode,
        String accommodationAddress
    ) {
        log.info("경로 최적화 시작: {} 장소, strategy={}, mode={}",
            places.size(), strategy, transportMode);

        if (places.isEmpty()) {
            return new OptimizedRoute(List.of(), 0, 0, List.of());
        }

        // 1. 카카오 API용 Waypoint 변환
        List<Waypoint> waypoints = convertToWaypoints(places);

        // 2. 전략별 최적화
        List<Waypoint> optimized = switch (strategy) {
            case SHORTEST_DISTANCE -> optimizeByDistance(waypoints);
            case FASTEST_TIME -> optimizeByTime(waypoints, transportMode);
            case BALANCED -> optimizeBalanced(waypoints, transportMode);
            case SCENIC_ROUTE -> optimizeScenic(waypoints);
            case AVOID_TRAFFIC -> optimizeAvoidTraffic(waypoints, transportMode);
        };

        // 3. 카카오 모빌리티 API로 실제 경로 계산
        RouteRequest request = RouteRequest.of(optimized, transportMode);
        RouteResponse response = kakaoMobilityClient.searchMultiDestinationRoute(request);

        // 4. 결과 변환
        return buildOptimizedRoute(response, places);
    }

    // 거리 기준 최적화 (2-opt 알고리즘)
    private List<Waypoint> optimizeByDistance(List<Waypoint> waypoints) {
        log.debug("최단 거리 최적화");

        if (waypoints.size() <= 3) {
            return waypoints;
        }

        List<Waypoint> current = new ArrayList<>(waypoints);
        boolean improved = true;

        while (improved) {
            improved = false;

            for (int i = 1; i < current.size() - 2; i++) {
                for (int j = i + 1; j < current.size() - 1; j++) {
                    // 2-opt swap
                    double currentDistance = calculateTotalDistance(current);
                    List<Waypoint> swapped = twoOptSwap(current, i, j);
                    double swappedDistance = calculateTotalDistance(swapped);

                    if (swappedDistance < currentDistance) {
                        current = swapped;
                        improved = true;
                        log.debug("2-opt 개선: {:.2f}km -> {:.2f}km", currentDistance, swappedDistance);
                    }
                }
            }
        }

        return current;
    }

    // 시간 기준 최적화
    private List<Waypoint> optimizeByTime(List<Waypoint> waypoints, TransportMode mode) {
        log.debug("최소 시간 최적화");

        // 시간대별 교통 상황 고려
        Map<String, Double> trafficFactors = getTrafficFactors();

        List<Waypoint> optimized = new ArrayList<>();
        Set<Waypoint> unvisited = new HashSet<>(waypoints);

        Waypoint current = waypoints.get(0);
        optimized.add(current);
        unvisited.remove(current);

        while (!unvisited.isEmpty()) {
            final Waypoint currentPoint = current; // lambda에서 사용하기 위해 final 변수로 복사
            Waypoint fastest = unvisited.stream()
                .min(Comparator.comparing(wp -> {
                    DistanceInfo info = kakaoMobilityClient.calculateDistance(
                        currentPoint.latitude(), currentPoint.longitude(),
                        wp.latitude(), wp.longitude(),
                        mode
                    );
                    // 교통 상황 반영
                    double trafficFactor = trafficFactors.getOrDefault(wp.name(), 1.0);
                    return info.duration() * trafficFactor;
                }))
                .orElse(unvisited.iterator().next());

            optimized.add(fastest);
            unvisited.remove(fastest);
            current = fastest;
        }

        return optimized;
    }

    // 균형 최적화 (거리와 시간 모두 고려)
    private List<Waypoint> optimizeBalanced(List<Waypoint> waypoints, TransportMode mode) {
        log.debug("균형 최적화");

        // 거리 최적화 결과
        List<Waypoint> distanceOptimized = optimizeByDistance(waypoints);
        double distanceScore = calculateTotalDistance(distanceOptimized);

        // 시간 최적화 결과
        List<Waypoint> timeOptimized = optimizeByTime(waypoints, mode);
        double timeScore = calculateTotalTime(timeOptimized, mode);

        // 두 결과를 비교하여 균형점 찾기
        double distanceWeight = 0.5;
        double timeWeight = 0.5;

        if (distanceScore < timeScore * 30) {  // 거리가 상대적으로 짧으면
            return distanceOptimized;
        } else {
            return timeOptimized;
        }
    }

    // 경치 좋은 경로 최적화
    private List<Waypoint> optimizeScenic(List<Waypoint> waypoints) {
        log.debug("경치 경로 최적화");

        // 관광지 우선 방문
        List<Waypoint> attractions = waypoints.stream()
            .filter(wp -> isAttraction(wp.name()))
            .collect(Collectors.toList());

        List<Waypoint> others = waypoints.stream()
            .filter(wp -> !isAttraction(wp.name()))
            .collect(Collectors.toList());

        List<Waypoint> result = new ArrayList<>();
        result.addAll(optimizeByDistance(attractions));
        result.addAll(optimizeByDistance(others));

        return result;
    }

    // 교통 혼잡 회피 최적화
    private List<Waypoint> optimizeAvoidTraffic(List<Waypoint> waypoints, TransportMode mode) {
        log.debug("교통 혼잡 회피 최적화");

        // 혼잡 시간대 피하기
        return waypoints.stream()
            .sorted(Comparator.comparing(wp -> getExpectedTraffic(wp.name())))
            .collect(Collectors.toList());
    }

    // 2-opt swap 구현
    private List<Waypoint> twoOptSwap(List<Waypoint> waypoints, int i, int j) {
        List<Waypoint> swapped = new ArrayList<>();

        // 첫 부분 그대로
        for (int k = 0; k <= i - 1; k++) {
            swapped.add(waypoints.get(k));
        }

        // 중간 부분 역순
        for (int k = j; k >= i; k--) {
            swapped.add(waypoints.get(k));
        }

        // 마지막 부분 그대로
        for (int k = j + 1; k < waypoints.size(); k++) {
            swapped.add(waypoints.get(k));
        }

        return swapped;
    }

    // 전체 거리 계산
    private double calculateTotalDistance(List<Waypoint> waypoints) {
        double total = 0;
        for (int i = 0; i < waypoints.size() - 1; i++) {
            DistanceInfo info = kakaoMobilityClient.calculateDistance(
                waypoints.get(i).latitude(),
                waypoints.get(i).longitude(),
                waypoints.get(i + 1).latitude(),
                waypoints.get(i + 1).longitude(),
                TransportMode.CAR
            );
            total += info.distance();
        }
        return total;
    }

    // 전체 시간 계산
    private double calculateTotalTime(List<Waypoint> waypoints, TransportMode mode) {
        double total = 0;
        for (int i = 0; i < waypoints.size() - 1; i++) {
            DistanceInfo info = kakaoMobilityClient.calculateDistance(
                waypoints.get(i).latitude(),
                waypoints.get(i).longitude(),
                waypoints.get(i + 1).latitude(),
                waypoints.get(i + 1).longitude(),
                mode
            );
            total += info.duration();
        }
        return total;
    }

    // TourPlace를 Waypoint로 변환
    private List<Waypoint> convertToWaypoints(List<TourPlace> places) {
        return places.stream()
            .filter(p -> p.latitude() != null && p.longitude() != null)
            .map(p -> new Waypoint(p.name(), p.latitude(), p.longitude()))
            .collect(Collectors.toList());
    }

    // OptimizedRoute 생성
    private OptimizedRoute buildOptimizedRoute(RouteResponse response, List<TourPlace> originalPlaces) {
        // 최적화된 순서대로 장소 재정렬
        Map<String, TourPlace> placeMap = originalPlaces.stream()
            .collect(Collectors.toMap(TourPlace::name, p -> p));

        List<TourPlace> orderedPlaces = response.optimizedWaypoints().stream()
            .map(wp -> placeMap.get(wp.name()))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        return new OptimizedRoute(
            orderedPlaces,
            response.totalDistance(),
            response.totalDuration(),
            response.sections()
        );
    }

    // 교통 상황 예측 (Mock)
    private Map<String, Double> getTrafficFactors() {
        return Map.of(
            "강남", 1.5,
            "종로", 1.3,
            "명동", 1.4,
            "홍대", 1.2
        );
    }

    // 관광지 판별
    private boolean isAttraction(String name) {
        return name.contains("궁") || name.contains("타워") ||
               name.contains("박물관") || name.contains("공원");
    }

    // 예상 교통량
    private double getExpectedTraffic(String name) {
        return getTrafficFactors().getOrDefault(name, 1.0);
    }

    // 최적화 결과
    public record OptimizedRoute(
        List<TourPlace> orderedPlaces,
        double totalDistance,
        int totalDuration,
        List<RouteSection> sections
    ) {}
}