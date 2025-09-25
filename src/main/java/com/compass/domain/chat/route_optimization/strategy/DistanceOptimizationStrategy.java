package com.compass.domain.chat.route_optimization.strategy;

import com.compass.domain.chat.function.processing.phase3.date_selection.model.TourPlace;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class DistanceOptimizationStrategy implements OptimizationStrategy {

    @Override
    public List<TourPlace> optimize(List<TourPlace> places) {
        if (places.size() <= 2) return new ArrayList<>(places);

        log.info("거리 최적화 전략으로 경로 최적화 시작: {} 장소", places.size());

        // Nearest Neighbor + 2-opt
        List<TourPlace> optimized = nearestNeighbor(places);
        optimized = twoOptImprovement(optimized);

        double originalDistance = calculateTotalCost(places);
        double optimizedDistance = calculateTotalCost(optimized);
        log.info("거리 최적화 완료: {:.2f}km -> {:.2f}km (개선율: {:.1f}%)",
            originalDistance, optimizedDistance,
            (1 - optimizedDistance / originalDistance) * 100);

        return optimized;
    }

    @Override
    public double calculateCost(TourPlace from, TourPlace to) {
        // 순수 거리만 계산
        return calculateDistance(from, to);
    }

    @Override
    public String getStrategyName() {
        return "DISTANCE";
    }

    // Nearest Neighbor 알고리즘
    private List<TourPlace> nearestNeighbor(List<TourPlace> places) {
        List<TourPlace> ordered = new ArrayList<>();
        Set<TourPlace> unvisited = new HashSet<>(places);

        // 시작점 선택
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

    // 2-opt 알고리즘
    private List<TourPlace> twoOptImprovement(List<TourPlace> route) {
        if (route.size() < 4) return route;

        List<TourPlace> improved = new ArrayList<>(route);
        boolean improvement = true;
        int iterations = 0;

        while (improvement && iterations < 100) {
            improvement = false;
            iterations++;

            for (int i = 1; i < improved.size() - 2; i++) {
                for (int j = i + 1; j < improved.size(); j++) {
                    double currentCost = calculateCost(improved.get(i-1), improved.get(i)) +
                                        calculateCost(improved.get(j), improved.get((j+1) % improved.size()));

                    double newCost = calculateCost(improved.get(i-1), improved.get(j)) +
                                    calculateCost(improved.get(i), improved.get((j+1) % improved.size()));

                    if (newCost < currentCost) {
                        reverseSubRoute(improved, i, j);
                        improvement = true;
                    }
                }
            }
        }

        return improved;
    }

    private void reverseSubRoute(List<TourPlace> route, int start, int end) {
        while (start < end) {
            Collections.swap(route, start, end);
            start++;
            end--;
        }
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