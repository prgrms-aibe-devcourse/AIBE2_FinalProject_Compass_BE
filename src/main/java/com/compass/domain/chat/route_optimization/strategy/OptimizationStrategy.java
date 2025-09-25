package com.compass.domain.chat.route_optimization.strategy;

import com.compass.domain.chat.function.processing.phase3.date_selection.model.TourPlace;
import java.util.List;

// 경로 최적화 전략 인터페이스
public interface OptimizationStrategy {

    // 경로 최적화
    List<TourPlace> optimize(List<TourPlace> places);

    // 두 장소 간 비용 계산 (거리, 시간 등을 고려)
    double calculateCost(TourPlace from, TourPlace to);

    // 전체 경로 비용 계산
    default double calculateTotalCost(List<TourPlace> route) {
        if (route.size() < 2) return 0;

        double totalCost = 0;
        for (int i = 0; i < route.size() - 1; i++) {
            totalCost += calculateCost(route.get(i), route.get(i + 1));
        }
        return totalCost;
    }

    // 전략 이름
    String getStrategyName();
}