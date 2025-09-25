package com.compass.domain.chat.route_optimization.strategy;

import com.compass.domain.chat.function.processing.phase3.date_selection.model.TourPlace;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class TimeOptimizationStrategy implements OptimizationStrategy {

    // 시간대별 교통 혼잡도 (0.5 ~ 2.0)
    private static final Map<String, Double> TIME_CONGESTION = Map.of(
        "BREAKFAST", 1.5,      // 출근 시간
        "MORNING_ACTIVITY", 1.0,
        "LUNCH", 1.3,
        "CAFE", 1.0,
        "AFTERNOON_ACTIVITY", 1.2,
        "DINNER", 1.8,         // 퇴근 시간
        "EVENING_ACTIVITY", 1.0
    );

    @Override
    public List<TourPlace> optimize(List<TourPlace> places) {
        if (places.size() <= 2) return new ArrayList<>(places);

        log.info("시간 최적화 전략으로 경로 최적화 시작: {} 장소", places.size());

        // 시간대별 그룹화
        Map<String, List<TourPlace>> timeGroups = groupByTimeBlock(places);

        // 각 시간대 내에서 최적화
        List<TourPlace> optimized = new ArrayList<>();
        for (String timeBlock : getOrderedTimeBlocks()) {
            List<TourPlace> group = timeGroups.getOrDefault(timeBlock, new ArrayList<>());
            if (!group.isEmpty()) {
                List<TourPlace> optimizedGroup = optimizeWithinTimeBlock(group, timeBlock);
                optimized.addAll(optimizedGroup);
            }
        }

        double originalTime = calculateTotalTime(places);
        double optimizedTime = calculateTotalTime(optimized);
        log.info("시간 최적화 완료: {}분 -> {}분 (개선율: {:.1f}%)",
            originalTime, optimizedTime,
            (1 - optimizedTime / originalTime) * 100);

        return optimized;
    }

    @Override
    public double calculateCost(TourPlace from, TourPlace to) {
        double distance = calculateDistance(from, to);

        // 시간대별 혼잡도 고려
        double congestionFactor = TIME_CONGESTION.getOrDefault(to.timeBlock(), 1.0);

        // 거리를 시간으로 변환 (평균 속도 30km/h 가정)
        double baseTime = (distance / 30.0) * 60; // 분 단위

        return baseTime * congestionFactor;
    }

    @Override
    public String getStrategyName() {
        return "TIME";
    }

    // 시간대별 그룹화
    private Map<String, List<TourPlace>> groupByTimeBlock(List<TourPlace> places) {
        Map<String, List<TourPlace>> groups = new HashMap<>();
        for (TourPlace place : places) {
            groups.computeIfAbsent(place.timeBlock(), k -> new ArrayList<>()).add(place);
        }
        return groups;
    }

    // 순서가 있는 시간 블록 목록
    private List<String> getOrderedTimeBlocks() {
        return List.of(
            "BREAKFAST",
            "MORNING_ACTIVITY",
            "LUNCH",
            "CAFE",
            "AFTERNOON_ACTIVITY",
            "DINNER",
            "EVENING_ACTIVITY"
        );
    }

    // 시간대 내에서 최적화
    private List<TourPlace> optimizeWithinTimeBlock(List<TourPlace> places, String timeBlock) {
        if (places.size() <= 1) return places;

        // 혼잡 시간대는 더 적극적인 최적화
        double congestion = TIME_CONGESTION.getOrDefault(timeBlock, 1.0);
        if (congestion > 1.3) {
            return aggressiveOptimization(places);
        } else {
            return standardOptimization(places);
        }
    }

    // 표준 최적화 (Nearest Neighbor)
    private List<TourPlace> standardOptimization(List<TourPlace> places) {
        if (places.size() <= 1) return places;

        List<TourPlace> ordered = new ArrayList<>();
        Set<TourPlace> unvisited = new HashSet<>(places);

        TourPlace current = places.get(0);
        ordered.add(current);
        unvisited.remove(current);

        while (!unvisited.isEmpty()) {
            final TourPlace currentPlace = current;
            TourPlace nearest = unvisited.stream()
                .min(Comparator.comparing(p -> calculateCost(currentPlace, p)))
                .orElse(unvisited.iterator().next());

            ordered.add(nearest);
            unvisited.remove(nearest);
            current = nearest;
        }

        return ordered;
    }

    // 적극적 최적화 (클러스터링 + 최단 경로)
    private List<TourPlace> aggressiveOptimization(List<TourPlace> places) {
        // 지역별 클러스터링
        List<List<TourPlace>> clusters = clusterByProximity(places, 2.0); // 2km 반경

        // 각 클러스터 내에서 최적화
        List<TourPlace> result = new ArrayList<>();
        for (List<TourPlace> cluster : clusters) {
            result.addAll(standardOptimization(cluster));
        }

        return result;
    }

    // 근접성 기반 클러스터링
    private List<List<TourPlace>> clusterByProximity(List<TourPlace> places, double radius) {
        List<List<TourPlace>> clusters = new ArrayList<>();
        Set<TourPlace> assigned = new HashSet<>();

        for (TourPlace place : places) {
            if (assigned.contains(place)) continue;

            List<TourPlace> cluster = new ArrayList<>();
            cluster.add(place);
            assigned.add(place);

            // 반경 내 장소들을 클러스터에 추가
            for (TourPlace other : places) {
                if (!assigned.contains(other) && calculateDistance(place, other) <= radius) {
                    cluster.add(other);
                    assigned.add(other);
                }
            }

            clusters.add(cluster);
        }

        return clusters;
    }

    // 총 소요 시간 계산
    private double calculateTotalTime(List<TourPlace> route) {
        if (route.size() < 2) return 0;

        double totalTime = 0;
        for (int i = 0; i < route.size() - 1; i++) {
            totalTime += calculateCost(route.get(i), route.get(i + 1));
        }

        // 각 장소에서의 체류 시간 추가 (기본 60분)
        totalTime += route.size() * 60;

        return totalTime;
    }

    // Haversine 거리 계산
    private double calculateDistance(TourPlace from, TourPlace to) {
        if (from.latitude() == null || to.latitude() == null) return 5.0;

        double lat1 = from.latitude();
        double lon1 = from.longitude();
        double lat2 = to.latitude();
        double lon2 = to.longitude();

        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                  Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                  Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }
}