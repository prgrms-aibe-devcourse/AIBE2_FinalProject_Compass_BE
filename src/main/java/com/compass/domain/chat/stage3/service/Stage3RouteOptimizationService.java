package com.compass.domain.chat.stage3.service;

import com.compass.domain.chat.model.TravelPlace;
import com.compass.domain.chat.stage3.dto.OptimizedRoute;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class Stage3RouteOptimizationService {

    // TSP + 2-opt 알고리즘을 사용한 경로 최적화
    public OptimizedRoute optimize(List<TravelPlace> places, String transportMode) {
        if (places == null || places.isEmpty()) {
            return createEmptyRoute(transportMode);
        }

        log.info("Optimizing route for {} places with transport mode: {}", places.size(), transportMode);

        // 고정 일정과 자유 일정 분리
        List<TravelPlace> fixedPlaces = new ArrayList<>();
        List<TravelPlace> flexiblePlaces = new ArrayList<>();

        for (TravelPlace place : places) {
            if (Boolean.TRUE.equals(place.getIsFixed())) {
                fixedPlaces.add(place);
            } else {
                flexiblePlaces.add(place);
            }
        }

        // 고정 일정은 시간 순서대로 정렬
        fixedPlaces.sort((a, b) -> {
            if (a.getFixedTime() != null && b.getFixedTime() != null) {
                return a.getFixedTime().compareTo(b.getFixedTime());
            }
            return 0;
        });

        // 자유 일정을 고정 일정 사이에 최적 배치
        List<TravelPlace> optimizedPlaces = arrangeWithFixedConstraints(fixedPlaces, flexiblePlaces);

        // 2-opt 최적화 적용 (자유 일정 구간만)
        optimizedPlaces = apply2OptOptimization(optimizedPlaces);

        // 경로 정보 계산
        double totalDistance = calculateTotalDistance(optimizedPlaces);
        long totalDuration = calculateTotalDuration(optimizedPlaces, transportMode);
        List<OptimizedRoute.RouteSegment> segments = createRouteSegments(optimizedPlaces, transportMode);

        return OptimizedRoute.builder()
            .places(optimizedPlaces)
            .totalDistance(totalDistance)
            .totalDuration(totalDuration)
            .segments(segments)
            .transportMode(transportMode)
            .statistics(createStatistics(optimizedPlaces, totalDistance, totalDuration))
            .build();
    }

    // 고정 일정을 고려한 장소 배치
    private List<TravelPlace> arrangeWithFixedConstraints(
            List<TravelPlace> fixedPlaces,
            List<TravelPlace> flexiblePlaces) {

        if (fixedPlaces.isEmpty()) {
            // 고정 일정이 없으면 TSP로 최적화
            return nearestNeighborTSP(flexiblePlaces);
        }

        List<TravelPlace> result = new ArrayList<>();
        List<TravelPlace> remainingFlexible = new ArrayList<>(flexiblePlaces);

        for (int i = 0; i < fixedPlaces.size(); i++) {
            TravelPlace fixed = fixedPlaces.get(i);
            result.add(fixed);

            // 다음 고정 일정까지 자유 일정 배치
            if (i < fixedPlaces.size() - 1) {
                TravelPlace nextFixed = fixedPlaces.get(i + 1);
                List<TravelPlace> segment = selectPlacesForSegment(
                    fixed, nextFixed, remainingFlexible
                );
                result.addAll(segment);
                remainingFlexible.removeAll(segment);
            }
        }

        // 마지막 고정 일정 이후 남은 자유 일정 추가
        if (!remainingFlexible.isEmpty()) {
            List<TravelPlace> lastSegment = nearestNeighborFromStart(
                fixedPlaces.get(fixedPlaces.size() - 1),
                remainingFlexible
            );
            result.addAll(lastSegment);
        }

        return result;
    }

    // 구간에 적합한 장소 선택
    private List<TravelPlace> selectPlacesForSegment(
            TravelPlace start,
            TravelPlace end,
            List<TravelPlace> candidates) {

        // 시간 간격 계산
        long hours = 3; // 기본 3시간
        if (start.getFixedTime() != null && end.getFixedTime() != null) {
            hours = java.time.Duration.between(
                start.getFixedTime(),
                end.getFixedTime()
            ).toHours();
        }

        // 시간에 따라 배치 가능한 장소 수 결정
        int maxPlaces = (int) Math.min(hours / 2, 3); // 2시간당 1개, 최대 3개

        // 거리 기준으로 가까운 장소 선택
        List<TravelPlace> selected = new ArrayList<>();
        List<TravelPlace> sorted = new ArrayList<>(candidates);
        sorted.sort((a, b) -> {
            double distA = calculateDistance(start, a) + calculateDistance(a, end);
            double distB = calculateDistance(start, b) + calculateDistance(b, end);
            return Double.compare(distA, distB);
        });

        for (int i = 0; i < Math.min(maxPlaces, sorted.size()); i++) {
            selected.add(sorted.get(i));
        }

        return selected;
    }

    // Nearest Neighbor TSP
    private List<TravelPlace> nearestNeighborTSP(List<TravelPlace> places) {
        if (places.size() <= 2) {
            return new ArrayList<>(places);
        }

        List<TravelPlace> result = new ArrayList<>();
        Set<TravelPlace> unvisited = new HashSet<>(places);

        // 첫 번째 장소 선택
        TravelPlace current = places.get(0);
        result.add(current);
        unvisited.remove(current);

        // 가장 가까운 미방문 장소 선택
        while (!unvisited.isEmpty()) {
            TravelPlace nearest = null;
            double minDistance = Double.MAX_VALUE;

            for (TravelPlace place : unvisited) {
                double distance = calculateDistance(current, place);
                if (distance < minDistance) {
                    minDistance = distance;
                    nearest = place;
                }
            }

            if (nearest != null) {
                result.add(nearest);
                unvisited.remove(nearest);
                current = nearest;
            }
        }

        return result;
    }

    // 특정 시작점에서 Nearest Neighbor
    private List<TravelPlace> nearestNeighborFromStart(
            TravelPlace start,
            List<TravelPlace> places) {

        List<TravelPlace> result = new ArrayList<>();
        Set<TravelPlace> unvisited = new HashSet<>(places);
        TravelPlace current = start;

        while (!unvisited.isEmpty()) {
            TravelPlace nearest = null;
            double minDistance = Double.MAX_VALUE;

            for (TravelPlace place : unvisited) {
                double distance = calculateDistance(current, place);
                if (distance < minDistance) {
                    minDistance = distance;
                    nearest = place;
                }
            }

            if (nearest != null) {
                result.add(nearest);
                unvisited.remove(nearest);
                current = nearest;
            }
        }

        return result;
    }

    // 2-opt 최적화
    private List<TravelPlace> apply2OptOptimization(List<TravelPlace> places) {
        if (places.size() < 4) {
            return places;
        }

        List<TravelPlace> improved = new ArrayList<>(places);
        boolean improvement = true;

        while (improvement) {
            improvement = false;

            for (int i = 0; i < improved.size() - 2; i++) {
                // 고정 일정은 건너뛰기
                if (Boolean.TRUE.equals(improved.get(i).getIsFixed())) {
                    continue;
                }

                for (int j = i + 2; j < improved.size(); j++) {
                    // 고정 일정은 건너뛰기
                    if (Boolean.TRUE.equals(improved.get(j).getIsFixed())) {
                        continue;
                    }

                    // 2-opt swap
                    double currentDistance = calculateSegmentDistance(improved, i, j);
                    List<TravelPlace> swapped = perform2OptSwap(improved, i + 1, j);
                    double newDistance = calculateSegmentDistance(swapped, i, j);

                    if (newDistance < currentDistance) {
                        improved = swapped;
                        improvement = true;
                        break;
                    }
                }
                if (improvement) break;
            }
        }

        return improved;
    }

    // 2-opt swap 수행
    private List<TravelPlace> perform2OptSwap(List<TravelPlace> places, int i, int j) {
        List<TravelPlace> result = new ArrayList<>();

        // 0 to i-1
        result.addAll(places.subList(0, i));

        // i to j in reverse
        for (int k = j; k >= i; k--) {
            result.add(places.get(k));
        }

        // j+1 to end
        if (j + 1 < places.size()) {
            result.addAll(places.subList(j + 1, places.size()));
        }

        return result;
    }

    // 구간 거리 계산
    private double calculateSegmentDistance(List<TravelPlace> places, int start, int end) {
        double distance = 0;
        for (int i = start; i < Math.min(end, places.size() - 1); i++) {
            distance += calculateDistance(places.get(i), places.get(i + 1));
        }
        return distance;
    }

    // 두 장소 간 거리 계산 (Haversine formula)
    private double calculateDistance(TravelPlace from, TravelPlace to) {
        if (from.getLatitude() == null || from.getLongitude() == null ||
            to.getLatitude() == null || to.getLongitude() == null) {
            return 0;
        }

        double earthRadius = 6371; // km
        double lat1Rad = Math.toRadians(from.getLatitude());
        double lat2Rad = Math.toRadians(to.getLatitude());
        double deltaLat = Math.toRadians(to.getLatitude() - from.getLatitude());
        double deltaLon = Math.toRadians(to.getLongitude() - from.getLongitude());

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                  Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                  Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadius * c;
    }

    // 전체 거리 계산
    private double calculateTotalDistance(List<TravelPlace> places) {
        double total = 0;
        for (int i = 0; i < places.size() - 1; i++) {
            total += calculateDistance(places.get(i), places.get(i + 1));
        }
        return total;
    }

    // 전체 소요 시간 계산
    private long calculateTotalDuration(List<TravelPlace> places, String transportMode) {
        // 간단한 추정: 거리와 이동 수단에 따른 평균 속도
        double totalDistance = calculateTotalDistance(places);
        double averageSpeed = "자차".equals(transportMode) ? 40 : 25; // km/h

        // 이동 시간 + 각 장소 체류 시간 (평균 1.5시간)
        long travelTime = (long) ((totalDistance / averageSpeed) * 60); // 분
        long stayTime = places.size() * 90L; // 각 장소당 90분

        return travelTime + stayTime;
    }

    // 경로 구간 생성
    private List<OptimizedRoute.RouteSegment> createRouteSegments(
            List<TravelPlace> places,
            String transportMode) {

        List<OptimizedRoute.RouteSegment> segments = new ArrayList<>();

        for (int i = 0; i < places.size() - 1; i++) {
            TravelPlace from = places.get(i);
            TravelPlace to = places.get(i + 1);

            segments.add(OptimizedRoute.RouteSegment.builder()
                .from(from)
                .to(to)
                .distance(calculateDistance(from, to))
                .duration(calculateSegmentDuration(from, to, transportMode))
                .transportMode(transportMode)
                .build());
        }

        return segments;
    }

    // 구간 소요 시간 계산
    private long calculateSegmentDuration(TravelPlace from, TravelPlace to, String transportMode) {
        double distance = calculateDistance(from, to);
        double averageSpeed = "자차".equals(transportMode) ? 40 : 25; // km/h
        return (long) ((distance / averageSpeed) * 60); // 분
    }

    // 통계 생성
    private OptimizedRoute.RouteStatistics createStatistics(
            List<TravelPlace> places,
            double totalDistance,
            long totalDuration) {

        int segments = Math.max(places.size() - 1, 0);

        return OptimizedRoute.RouteStatistics.builder()
            .averageDistanceBetweenPlaces(segments > 0 ? totalDistance / segments : 0)
            .averageDurationBetweenPlaces(segments > 0 ? totalDuration / segments : 0)
            .totalSegments(segments)
            .optimizationRate(0.8) // 예시 값
            .build();
    }

    // 빈 경로 생성
    private OptimizedRoute createEmptyRoute(String transportMode) {
        return OptimizedRoute.builder()
            .places(new ArrayList<>())
            .totalDistance(0)
            .totalDuration(0)
            .segments(new ArrayList<>())
            .transportMode(transportMode)
            .statistics(createStatistics(new ArrayList<>(), 0, 0))
            .build();
    }
}