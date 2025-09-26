package com.compass.domain.chat.common.utils;

import com.compass.domain.chat.model.TravelPlace;
import org.springframework.stereotype.Component;

import java.util.List;

// 거리 계산 유틸리티 클래스
@Component
public class DistanceCalculator {

    private static final double EARTH_RADIUS_KM = 6371.0; // 지구 반경

    // 두 지점 간 거리 계산 (Haversine 공식)
    public double calculate(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                  Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                  Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    // TravelPlace 객체 간 거리 계산
    public double calculate(TravelPlace place1, TravelPlace place2) {
        if (place1 == null || place2 == null) {
            return 0.0;
        }

        return calculate(
            place1.getLatitude(), place1.getLongitude(),
            place2.getLatitude(), place2.getLongitude()
        );
    }

    // 한 장소와 여러 기준 장소들 간의 최소 거리 계산
    public double findMinDistance(TravelPlace place, List<TravelPlace> references) {
        if (place == null || references == null || references.isEmpty()) {
            return 0.0;
        }

        double minDistance = Double.MAX_VALUE;

        for (TravelPlace reference : references) {
            double distance = calculate(place, reference);
            minDistance = Math.min(minDistance, distance);
        }

        return minDistance;
    }

    // 한 장소와 여러 기준 장소들 간의 평균 거리 계산
    public double findAverageDistance(TravelPlace place, List<TravelPlace> references) {
        if (place == null || references == null || references.isEmpty()) {
            return 0.0;
        }

        double totalDistance = 0.0;

        for (TravelPlace reference : references) {
            totalDistance += calculate(place, reference);
        }

        return totalDistance / references.size();
    }

    // 경로의 총 거리 계산 (순차적 이동)
    public double calculateTotalDistance(List<TravelPlace> places) {
        if (places == null || places.size() < 2) {
            return 0.0;
        }

        double totalDistance = 0.0;

        for (int i = 0; i < places.size() - 1; i++) {
            totalDistance += calculate(places.get(i), places.get(i + 1));
        }

        return totalDistance;
    }

    // 이동 시간 계산 (거리와 속도 기반)
    public int calculateTravelTime(double distanceKm, double speedKmh) {
        if (distanceKm <= 0 || speedKmh <= 0) {
            return 0;
        }

        return (int) Math.ceil(distanceKm / speedKmh * 60); // 분 단위로 반환
    }

    // 거리 범주화 (도보/가까운/먼)
    public DistanceCategory categorizeDistance(double distanceKm) {
        if (distanceKm <= 2.0) {
            return DistanceCategory.WALKABLE;
        } else if (distanceKm <= 5.0) {
            return DistanceCategory.NEAR;
        } else {
            return DistanceCategory.FAR;
        }
    }

    // 거리 카테고리 열거형
    public enum DistanceCategory {
        WALKABLE("도보 가능", 0, 2.0),
        NEAR("가까운 거리", 2.0, 5.0),
        FAR("먼 거리", 5.0, Double.MAX_VALUE);

        private final String description;
        private final double minKm;
        private final double maxKm;

        DistanceCategory(String description, double minKm, double maxKm) {
            this.description = description;
            this.minKm = minKm;
            this.maxKm = maxKm;
        }

        public String getDescription() {
            return description;
        }

        public double getMinKm() {
            return minKm;
        }

        public double getMaxKm() {
            return maxKm;
        }
    }
}