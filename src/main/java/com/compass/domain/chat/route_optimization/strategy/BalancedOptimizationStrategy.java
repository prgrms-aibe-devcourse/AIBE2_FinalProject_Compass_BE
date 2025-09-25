package com.compass.domain.chat.route_optimization.strategy;

import com.compass.domain.chat.function.processing.phase3.date_selection.model.TourPlace;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class BalancedOptimizationStrategy implements OptimizationStrategy {

    // 균형 잡힌 가중치
    private static final double DISTANCE_WEIGHT = 0.4;
    private static final double TIME_WEIGHT = 0.3;
    private static final double PREFERENCE_WEIGHT = 0.3;

    @Override
    public List<TourPlace> optimize(List<TourPlace> places) {
        if (places.size() <= 2) return new ArrayList<>(places);

        log.info("균형 최적화 전략으로 경로 최적화 시작: {} 장소", places.size());

        // Multi-criteria 최적화
        List<TourPlace> optimized = multiCriteriaOptimization(places);

        double originalCost = calculateTotalCost(places);
        double optimizedCost = calculateTotalCost(optimized);
        log.info("균형 최적화 완료: {:.2f} -> {:.2f} (개선율: {:.1f}%)",
            originalCost, optimizedCost,
            (1 - optimizedCost / originalCost) * 100);

        return optimized;
    }

    @Override
    public double calculateCost(TourPlace from, TourPlace to) {
        // 거리 비용
        double distanceCost = calculateDistance(from, to);

        // 시간 비용 (교통 상황 고려)
        double timeCost = calculateTimeCost(from, to);

        // 선호도 비용 (평점, 트렌디 여부 등)
        double preferenceCost = calculatePreferenceCost(from, to);

        // 가중 평균
        return DISTANCE_WEIGHT * distanceCost +
               TIME_WEIGHT * timeCost +
               PREFERENCE_WEIGHT * preferenceCost;
    }

    @Override
    public String getStrategyName() {
        return "BALANCED";
    }

    // 다중 기준 최적화
    private List<TourPlace> multiCriteriaOptimization(List<TourPlace> places) {
        // Step 1: 시간대별 그룹화
        Map<String, List<TourPlace>> timeGroups = groupByTimeBlock(places);

        // Step 2: 각 그룹에서 중요 장소 선별
        List<TourPlace> keyPlaces = selectKeyPlaces(timeGroups);

        // Step 3: 나머지 장소들을 최적 위치에 삽입
        List<TourPlace> optimized = buildOptimalRoute(keyPlaces, places);

        // Step 4: 2-opt로 미세 조정
        optimized = twoOptRefinement(optimized);

        return optimized;
    }

    // 시간대별 그룹화
    private Map<String, List<TourPlace>> groupByTimeBlock(List<TourPlace> places) {
        Map<String, List<TourPlace>> groups = new HashMap<>();
        for (TourPlace place : places) {
            groups.computeIfAbsent(place.timeBlock(), k -> new ArrayList<>()).add(place);
        }
        return groups;
    }

    // 중요 장소 선별 (평점 높고 트렌디한 장소)
    private List<TourPlace> selectKeyPlaces(Map<String, List<TourPlace>> timeGroups) {
        List<TourPlace> keyPlaces = new ArrayList<>();

        for (List<TourPlace> group : timeGroups.values()) {
            group.stream()
                .max(Comparator.comparing(this::calculateImportance))
                .ifPresent(keyPlaces::add);
        }

        return keyPlaces;
    }

    // 장소 중요도 계산
    private double calculateImportance(TourPlace place) {
        double importance = 0;

        // 평점 (0-5)
        if (place.rating() != null) {
            importance += place.rating() * 2;
        }

        // 트렌디 여부
        if (Boolean.TRUE.equals(place.isTrendy())) {
            importance += 3;
        }

        // 카테고리별 가중치
        importance += getCategoryWeight(place.category());

        return importance;
    }

    // 카테고리별 가중치
    private double getCategoryWeight(String category) {
        return switch (category) {
            case "관광명소" -> 5.0;
            case "맛집" -> 4.0;
            case "카페" -> 2.0;
            case "쇼핑" -> 3.0;
            default -> 2.5;
        };
    }

    // 최적 경로 구성
    private List<TourPlace> buildOptimalRoute(List<TourPlace> keyPlaces, List<TourPlace> allPlaces) {
        List<TourPlace> route = new ArrayList<>(keyPlaces);
        Set<TourPlace> included = new HashSet<>(keyPlaces);

        // 나머지 장소들을 최적 위치에 삽입
        for (TourPlace place : allPlaces) {
            if (!included.contains(place)) {
                int bestPosition = findBestInsertPosition(route, place);
                route.add(bestPosition, place);
                included.add(place);
            }
        }

        return route;
    }

    // 최적 삽입 위치 찾기
    private int findBestInsertPosition(List<TourPlace> route, TourPlace place) {
        if (route.isEmpty()) return 0;

        int bestPosition = 0;
        double minCostIncrease = Double.MAX_VALUE;

        for (int i = 0; i <= route.size(); i++) {
            double costIncrease = calculateInsertionCost(route, place, i);
            if (costIncrease < minCostIncrease) {
                minCostIncrease = costIncrease;
                bestPosition = i;
            }
        }

        return bestPosition;
    }

    // 삽입 비용 계산
    private double calculateInsertionCost(List<TourPlace> route, TourPlace place, int position) {
        if (route.isEmpty()) return 0;

        double costBefore = 0;
        double costAfter = 0;

        if (position == 0) {
            // 맨 앞에 삽입
            costAfter = calculateCost(place, route.get(0));
        } else if (position == route.size()) {
            // 맨 뒤에 삽입
            costAfter = calculateCost(route.get(route.size() - 1), place);
        } else {
            // 중간에 삽입
            costBefore = calculateCost(route.get(position - 1), route.get(position));
            costAfter = calculateCost(route.get(position - 1), place) +
                       calculateCost(place, route.get(position));
        }

        return costAfter - costBefore;
    }

    // 2-opt 미세 조정
    private List<TourPlace> twoOptRefinement(List<TourPlace> route) {
        if (route.size() < 4) return route;

        List<TourPlace> improved = new ArrayList<>(route);
        boolean improvement = true;
        int iterations = 0;

        while (improvement && iterations < 50) {
            improvement = false;
            iterations++;

            for (int i = 1; i < improved.size() - 2; i++) {
                for (int j = i + 1; j < improved.size(); j++) {
                    if (shouldSwap(improved, i, j)) {
                        reverseSubRoute(improved, i, j);
                        improvement = true;
                    }
                }
            }
        }

        return improved;
    }

    // 교체 여부 판단
    private boolean shouldSwap(List<TourPlace> route, int i, int j) {
        double currentCost = calculateCost(route.get(i-1), route.get(i)) +
                            calculateCost(route.get(j), route.get((j+1) % route.size()));

        double newCost = calculateCost(route.get(i-1), route.get(j)) +
                        calculateCost(route.get(i), route.get((j+1) % route.size()));

        return newCost < currentCost;
    }

    // 부분 경로 역순 처리
    private void reverseSubRoute(List<TourPlace> route, int start, int end) {
        while (start < end) {
            Collections.swap(route, start, end);
            start++;
            end--;
        }
    }

    // 시간 비용 계산
    private double calculateTimeCost(TourPlace from, TourPlace to) {
        double distance = calculateDistance(from, to);
        // 기본 속도 30km/h, 시간대별 혼잡도 고려
        double baseTime = (distance / 30.0) * 60;
        double congestionFactor = getTimeCongestion(to.timeBlock());
        return baseTime * congestionFactor;
    }

    // 선호도 비용 계산
    private double calculatePreferenceCost(TourPlace from, TourPlace to) {
        // 평점 차이 (높은 평점에서 낮은 평점으로 이동 시 비용 증가)
        double ratingDiff = 0;
        if (from.rating() != null && to.rating() != null) {
            ratingDiff = Math.max(0, from.rating() - to.rating());
        }

        // 카테고리 전환 비용
        double categoryTransitionCost = from.category().equals(to.category()) ? 0 : 2;

        return ratingDiff + categoryTransitionCost;
    }

    // 시간대별 혼잡도
    private double getTimeCongestion(String timeBlock) {
        return switch (timeBlock) {
            case "BREAKFAST" -> 1.5;
            case "LUNCH" -> 1.3;
            case "DINNER" -> 1.8;
            default -> 1.0;
        };
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