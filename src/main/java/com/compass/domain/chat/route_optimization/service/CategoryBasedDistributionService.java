package com.compass.domain.chat.route_optimization.service;

import com.compass.domain.chat.route_optimization.model.CategoryBasedSelectionRequest;
import com.compass.domain.chat.route_optimization.model.CategoryBasedSelectionRequest.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 카테고리 기반 일자별 분배 서비스 (Stage 2)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryBasedDistributionService {

    /**
     * 카테고리별 필수 장소를 포함하여 일자별 분배 (호텔 체크인/체크아웃 고려)
     */
    public Map<Integer, List<PlaceCandidate>> distributeWithConstraints(
            CategoryBasedSelectionRequest request) {

        log.info("Starting category-based distribution for {} days", request.tripDays());

        Map<Integer, List<PlaceCandidate>> dailyDistribution = new HashMap<>();
        List<PlaceCandidate> requiredPlaces = request.getRequiredPlaces();
        List<PlaceCandidate> optionalPlaces = getOptionalPlaces(request.allPlaces());

        // 0. 호텔 정보 추출 (있는 경우)
        PlaceCandidate hotel = extractHotel(request.allPlaces());

        // 1. 필수 장소를 먼저 분배
        distributeRequiredPlaces(dailyDistribution, requiredPlaces, request.tripDays());

        // 2. 카테고리 균형을 고려하여 추가 장소 분배
        balanceCategoriesPerDay(dailyDistribution, optionalPlaces, request);

        // 3. 호텔 기반 시간대 최적화
        if (hotel != null) {
            optimizeTimeSlotsWithHotel(dailyDistribution, hotel, request.tripDays());
        } else {
            optimizeTimeSlots(dailyDistribution);
        }

        // 4. 지역 클러스터링 (선택사항)
        if (request.constraints().preferSameAreaClustering()) {
            applyGeographicClustering(dailyDistribution, hotel);
        }

        return dailyDistribution;
    }

    /**
     * 필수 장소 균등 분배
     */
    private void distributeRequiredPlaces(
            Map<Integer, List<PlaceCandidate>> distribution,
            List<PlaceCandidate> requiredPlaces,
            int tripDays) {

        // K-means 클러스터링으로 지리적으로 가까운 장소끼리 묶기
        Map<Integer, List<PlaceCandidate>> clusters = performKMeansClustering(
            requiredPlaces, tripDays
        );

        // 각 클러스터를 일자별로 할당
        for (Map.Entry<Integer, List<PlaceCandidate>> entry : clusters.entrySet()) {
            int day = entry.getKey() + 1;  // 1-indexed
            distribution.computeIfAbsent(day, k -> new ArrayList<>())
                       .addAll(entry.getValue());
        }

        log.info("Distributed {} required places across {} days",
                requiredPlaces.size(), tripDays);
    }

    /**
     * K-means 클러스터링
     */
    private Map<Integer, List<PlaceCandidate>> performKMeansClustering(
            List<PlaceCandidate> places, int k) {

        if (places.isEmpty()) {
            return new HashMap<>();
        }

        // 간단한 K-means 구현
        Map<Integer, List<PlaceCandidate>> clusters = new HashMap<>();

        // 초기 중심점 선택 (K-means++ 알고리즘)
        List<PlaceCandidate> centroids = selectInitialCentroids(places, k);

        boolean changed = true;
        int iteration = 0;
        int maxIterations = 100;

        while (changed && iteration < maxIterations) {
            changed = false;
            clusters.clear();

            // 각 장소를 가장 가까운 중심점에 할당
            for (PlaceCandidate place : places) {
                int nearestCluster = findNearestCentroid(place, centroids);
                clusters.computeIfAbsent(nearestCluster, key -> new ArrayList<>())
                       .add(place);
            }

            // 새로운 중심점 계산
            List<PlaceCandidate> newCentroids = new ArrayList<>();
            for (int i = 0; i < k; i++) {
                List<PlaceCandidate> clusterPlaces = clusters.getOrDefault(i, new ArrayList<>());
                if (!clusterPlaces.isEmpty()) {
                    PlaceCandidate newCentroid = calculateCentroid(clusterPlaces);
                    newCentroids.add(newCentroid);
                } else {
                    // 빈 클러스터의 경우 기존 중심점 유지
                    newCentroids.add(centroids.get(i));
                }
            }

            // 중심점 변화 확인
            if (!centroids.equals(newCentroids)) {
                changed = true;
                centroids = newCentroids;
            }

            iteration++;
        }

        log.info("K-means clustering completed in {} iterations", iteration);
        return clusters;
    }

    /**
     * K-means++ 초기 중심점 선택
     */
    private List<PlaceCandidate> selectInitialCentroids(List<PlaceCandidate> places, int k) {
        List<PlaceCandidate> centroids = new ArrayList<>();
        Random random = new Random();

        // 첫 번째 중심점은 무작위 선택
        centroids.add(places.get(random.nextInt(places.size())));

        // 나머지 중심점은 기존 중심점과의 거리에 비례하여 선택
        for (int i = 1; i < k && i < places.size(); i++) {
            PlaceCandidate nextCentroid = selectNextCentroid(places, centroids);
            if (nextCentroid != null) {
                centroids.add(nextCentroid);
            }
        }

        return centroids;
    }

    /**
     * 거리 기반 다음 중심점 선택
     */
    private PlaceCandidate selectNextCentroid(
            List<PlaceCandidate> places,
            List<PlaceCandidate> existingCentroids) {

        double maxMinDistance = 0;
        PlaceCandidate bestCandidate = null;

        for (PlaceCandidate candidate : places) {
            if (existingCentroids.contains(candidate)) continue;

            double minDistance = Double.MAX_VALUE;
            for (PlaceCandidate centroid : existingCentroids) {
                double distance = calculateDistance(candidate, centroid);
                minDistance = Math.min(minDistance, distance);
            }

            if (minDistance > maxMinDistance) {
                maxMinDistance = minDistance;
                bestCandidate = candidate;
            }
        }

        return bestCandidate;
    }

    /**
     * 가장 가까운 중심점 찾기
     */
    private int findNearestCentroid(PlaceCandidate place, List<PlaceCandidate> centroids) {
        int nearest = 0;
        double minDistance = Double.MAX_VALUE;

        for (int i = 0; i < centroids.size(); i++) {
            double distance = calculateDistance(place, centroids.get(i));
            if (distance < minDistance) {
                minDistance = distance;
                nearest = i;
            }
        }

        return nearest;
    }

    /**
     * 클러스터 중심점 계산
     */
    private PlaceCandidate calculateCentroid(List<PlaceCandidate> places) {
        double avgLat = places.stream()
            .mapToDouble(PlaceCandidate::latitude)
            .average()
            .orElse(0);

        double avgLng = places.stream()
            .mapToDouble(PlaceCandidate::longitude)
            .average()
            .orElse(0);

        // 가상의 중심점 생성
        return new PlaceCandidate(
            "centroid",
            "Centroid",
            "centroid",
            avgLat,
            avgLng,
            "AFTERNOON_ACTIVITY",
            3,
            0
        );
    }

    /**
     * 두 장소 간 거리 계산 (Haversine formula)
     */
    private double calculateDistance(PlaceCandidate p1, PlaceCandidate p2) {
        double lat1 = Math.toRadians(p1.latitude());
        double lat2 = Math.toRadians(p2.latitude());
        double deltaLat = lat2 - lat1;
        double deltaLng = Math.toRadians(p2.longitude() - p1.longitude());

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                   Math.cos(lat1) * Math.cos(lat2) *
                   Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return 6371 * c;  // 지구 반지름 (km)
    }

    /**
     * 카테고리 균형 맞추기
     */
    private void balanceCategoriesPerDay(
            Map<Integer, List<PlaceCandidate>> distribution,
            List<PlaceCandidate> optionalPlaces,
            CategoryBasedSelectionRequest request) {

        Map<String, CategoryConstraint> categoryConstraints =
            request.constraints().categoryBalance();

        for (int day = 1; day <= request.tripDays(); day++) {
            List<PlaceCandidate> dayPlaces = distribution.get(day);
            if (dayPlaces == null) continue;

            // 현재 카테고리별 개수 계산
            Map<String, Long> currentCounts = dayPlaces.stream()
                .collect(Collectors.groupingBy(
                    PlaceCandidate::category,
                    Collectors.counting()
                ));

            // 부족한 카테고리 채우기
            for (Map.Entry<String, CategoryConstraint> entry : categoryConstraints.entrySet()) {
                String category = entry.getKey();
                CategoryConstraint constraint = entry.getValue();
                long current = currentCounts.getOrDefault(category, 0L);

                if (current < constraint.minPerDay()) {
                    // 해당 카테고리 장소 추가
                    int needed = constraint.minPerDay() - (int) current;
                    List<PlaceCandidate> candidates = optionalPlaces.stream()
                        .filter(p -> p.category().equals(category))
                        .limit(needed)
                        .toList();

                    dayPlaces.addAll(candidates);
                    optionalPlaces.removeAll(candidates);
                }
            }
        }
    }

    /**
     * 시간대별 최적화
     */
    private void optimizeTimeSlots(Map<Integer, List<PlaceCandidate>> distribution) {
        List<String> timeOrder = List.of(
            "MORNING_ACTIVITY",
            "LUNCH",
            "AFTERNOON_ACTIVITY",
            "DINNER",
            "EVENING_ACTIVITY"
        );

        for (List<PlaceCandidate> dayPlaces : distribution.values()) {
            dayPlaces.sort((a, b) -> {
                int indexA = timeOrder.indexOf(a.timeBlock());
                int indexB = timeOrder.indexOf(b.timeBlock());
                return Integer.compare(indexA, indexB);
            });
        }
    }

    /**
     * 호텔 기반 시간대 최적화
     */
    private void optimizeTimeSlotsWithHotel(
            Map<Integer, List<PlaceCandidate>> distribution,
            PlaceCandidate hotel,
            int tripDays) {

        for (int day = 1; day <= tripDays; day++) {
            List<PlaceCandidate> dayPlaces = distribution.get(day);
            if (dayPlaces == null) {
                dayPlaces = new ArrayList<>();
                distribution.put(day, dayPlaces);
            }

            // 호텔 체크인/체크아웃 블록 추가
            if (day == 1) {
                // 첫째 날: 체크인
                PlaceCandidate checkIn = new PlaceCandidate(
                    hotel.id() + "_checkin",
                    hotel.name() + " (체크인)",
                    "숙소",
                    hotel.latitude(),
                    hotel.longitude(),
                    "HOTEL_CHECKIN",
                    1,  // 필수
                    30  // 30분 소요
                );
                dayPlaces.add(3, checkIn);  // 오후에 체크인

                // 호텔 복귀
                PlaceCandidate hotelReturn = new PlaceCandidate(
                    hotel.id() + "_return_" + day,
                    hotel.name() + " (복귀)",
                    "숙소",
                    hotel.latitude(),
                    hotel.longitude(),
                    "HOTEL_RETURN",
                    1,
                    0
                );
                dayPlaces.add(hotelReturn);

            } else if (day == tripDays) {
                // 마지막 날: 체크아웃
                PlaceCandidate checkOut = new PlaceCandidate(
                    hotel.id() + "_checkout",
                    hotel.name() + " (체크아웃)",
                    "숙소",
                    hotel.latitude(),
                    hotel.longitude(),
                    "HOTEL_CHECKOUT",
                    1,
                    30
                );
                dayPlaces.add(0, checkOut);  // 아침에 체크아웃

            } else {
                // 중간 날들: 호텔 출발 + 호텔 복귀
                PlaceCandidate hotelStart = new PlaceCandidate(
                    hotel.id() + "_start_" + day,
                    hotel.name() + " (출발)",
                    "숙소",
                    hotel.latitude(),
                    hotel.longitude(),
                    "HOTEL_START",
                    1,
                    0
                );
                dayPlaces.add(0, hotelStart);

                PlaceCandidate hotelReturn = new PlaceCandidate(
                    hotel.id() + "_return_" + day,
                    hotel.name() + " (복귀)",
                    "숙소",
                    hotel.latitude(),
                    hotel.longitude(),
                    "HOTEL_RETURN",
                    1,
                    0
                );
                dayPlaces.add(hotelReturn);
            }

            // 시간대별 정렬
            sortByTimeBlocks(dayPlaces);
        }
    }

    /**
     * 시간 블록에 따라 정렬
     */
    private void sortByTimeBlocks(List<PlaceCandidate> places) {
        List<String> timeOrder = List.of(
            "HOTEL_START",
            "HOTEL_CHECKOUT",
            "MORNING_ACTIVITY",
            "LUNCH",
            "AFTERNOON_ACTIVITY",
            "HOTEL_CHECKIN",
            "DINNER",
            "EVENING_ACTIVITY",
            "HOTEL_RETURN"
        );

        places.sort((a, b) -> {
            int indexA = timeOrder.indexOf(a.timeBlock());
            int indexB = timeOrder.indexOf(b.timeBlock());
            if (indexA == -1) indexA = 5;  // 기본값
            if (indexB == -1) indexB = 5;
            return Integer.compare(indexA, indexB);
        });
    }

    /**
     * 호텔 추출
     */
    private PlaceCandidate extractHotel(List<PlaceCandidate> places) {
        return places.stream()
            .filter(p -> p.category().equals("숙소") ||
                        p.category().equals("호텔") ||
                        p.name().contains("호텔") ||
                        p.name().contains("Hotel"))
            .findFirst()
            .orElse(null);
    }

    /**
     * 지리적 클러스터링 적용 (호텔 고려)
     */
    private void applyGeographicClustering(
            Map<Integer, List<PlaceCandidate>> distribution,
            PlaceCandidate hotel) {

        // TSP 알고리즘으로 각 일자별 최적 경로 계산
        for (Map.Entry<Integer, List<PlaceCandidate>> entry : distribution.entrySet()) {
            List<PlaceCandidate> optimized;

            if (hotel != null) {
                // 호텔을 시작점과 종료점으로 고정
                optimized = optimizeRouteWithHotelAnchors(entry.getValue(), hotel);
            } else {
                optimized = optimizeRouteWithTSP(entry.getValue());
            }

            entry.setValue(optimized);
        }
    }

    /**
     * 호텔을 고정점으로 한 경로 최적화
     */
    private List<PlaceCandidate> optimizeRouteWithHotelAnchors(
            List<PlaceCandidate> places,
            PlaceCandidate hotel) {

        if (places.size() <= 2) {
            return new ArrayList<>(places);
        }

        // 호텔 관련 장소와 일반 장소 분리
        List<PlaceCandidate> hotelPlaces = new ArrayList<>();
        List<PlaceCandidate> regularPlaces = new ArrayList<>();

        for (PlaceCandidate place : places) {
            if (place.timeBlock().contains("HOTEL")) {
                hotelPlaces.add(place);
            } else {
                regularPlaces.add(place);
            }
        }

        // 일반 장소만 TSP 최적화
        List<PlaceCandidate> optimizedRegular = optimizeRouteWithTSP(regularPlaces);

        // 호텔 장소를 적절한 위치에 삽입
        List<PlaceCandidate> result = new ArrayList<>();

        // 호텔 출발/체크아웃 추가 (있는 경우)
        hotelPlaces.stream()
            .filter(p -> p.timeBlock().equals("HOTEL_START") ||
                        p.timeBlock().equals("HOTEL_CHECKOUT"))
            .forEach(result::add);

        // 최적화된 일반 장소들 추가
        result.addAll(optimizedRegular);

        // 호텔 체크인/복귀 추가 (있는 경우)
        hotelPlaces.stream()
            .filter(p -> p.timeBlock().equals("HOTEL_CHECKIN") ||
                        p.timeBlock().equals("HOTEL_RETURN"))
            .forEach(result::add);

        return result;
    }

    /**
     * TSP 알고리즘으로 경로 최적화 (2-opt improvement)
     */
    private List<PlaceCandidate> optimizeRouteWithTSP(List<PlaceCandidate> places) {
        if (places.size() <= 2) {
            return new ArrayList<>(places);
        }

        List<PlaceCandidate> currentRoute = new ArrayList<>(places);
        boolean improved = true;

        while (improved) {
            improved = false;

            for (int i = 1; i < currentRoute.size() - 1; i++) {
                for (int j = i + 1; j < currentRoute.size(); j++) {
                    double currentDistance = calculateTotalDistance(currentRoute);

                    // 2-opt swap
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

    /**
     * 2-opt swap
     */
    private List<PlaceCandidate> twoOptSwap(List<PlaceCandidate> route, int i, int j) {
        List<PlaceCandidate> newRoute = new ArrayList<>(route.subList(0, i));

        // Reverse the segment between i and j
        for (int k = j; k >= i; k--) {
            newRoute.add(route.get(k));
        }

        // Add the rest
        if (j + 1 < route.size()) {
            newRoute.addAll(route.subList(j + 1, route.size()));
        }

        return newRoute;
    }

    /**
     * 전체 경로 거리 계산
     */
    private double calculateTotalDistance(List<PlaceCandidate> route) {
        double totalDistance = 0;

        for (int i = 0; i < route.size() - 1; i++) {
            totalDistance += calculateDistance(route.get(i), route.get(i + 1));
        }

        return totalDistance;
    }

    /**
     * 선택적 장소 추출
     */
    private List<PlaceCandidate> getOptionalPlaces(List<PlaceCandidate> allPlaces) {
        return allPlaces.stream()
            .filter(place -> place.priority() > 1)
            .collect(Collectors.toList());
    }
}