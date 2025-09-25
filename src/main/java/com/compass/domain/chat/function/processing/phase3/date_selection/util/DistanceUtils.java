package com.compass.domain.chat.function.processing.phase3.date_selection.util;

import com.compass.domain.chat.function.processing.phase3.date_selection.model.TourPlace;
import com.compass.domain.chat.function.processing.phase3.date_selection.model.cluster.Cluster;
import com.compass.domain.chat.function.processing.phase3.date_selection.model.cluster.Point;

import java.util.Comparator;
import java.util.List;

// 거리 계산 유틸리티
public class DistanceUtils {

    private static final double EARTH_RADIUS_KM = 6371; // 지구 반지름 (km)

    // Haversine 공식으로 두 지점 간 거리 계산 (km)
    public static double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double latDistance = Math.toRadians(lat2 - lat1);
        double lngDistance = Math.toRadians(lng2 - lng1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    // 장소와 점 사이의 거리 계산
    public static double calculateDistance(TourPlace place, Point point) {
        return calculateDistance(
            place.latitude(), place.longitude(),
            point.getLat(), point.getLng()
        );
    }

    // 두 장소 사이의 거리 계산
    public static double calculateDistance(TourPlace place1, TourPlace place2) {
        return calculateDistance(
            place1.latitude(), place1.longitude(),
            place2.latitude(), place2.longitude()
        );
    }

    // 가장 가까운 클러스터 찾기
    public static Cluster findNearestCluster(TourPlace place, List<Cluster> clusters) {
        return clusters.stream()
            .min(Comparator.comparing(cluster ->
                calculateDistance(
                    place.latitude(),
                    place.longitude(),
                    cluster.getCenter().getLat(),
                    cluster.getCenter().getLng()
                )
            ))
            .orElse(clusters.get(0));
    }

    // 장소 리스트의 총 이동 거리 계산 (순서대로 이동한다고 가정)
    public static double calculateTotalDistance(List<TourPlace> places) {
        if (places == null || places.size() < 2) {
            return 0.0;
        }

        double totalDistance = 0.0;
        for (int i = 0; i < places.size() - 1; i++) {
            totalDistance += calculateDistance(places.get(i), places.get(i + 1));
        }

        return totalDistance;
    }

    // 장소 리스트에서 가장 가까운 장소 찾기
    public static TourPlace findClosestPlace(List<TourPlace> candidates, List<TourPlace> targetPlaces) {
        if (candidates.isEmpty()) return null;
        if (targetPlaces.isEmpty()) return candidates.get(0);

        TourPlace closestPlace = null;
        double minDistance = Double.MAX_VALUE;

        for (TourPlace candidate : candidates) {
            double totalDistance = 0.0;
            for (TourPlace target : targetPlaces) {
                totalDistance += calculateDistance(candidate, target);
            }
            double avgDistance = totalDistance / targetPlaces.size();

            if (avgDistance < minDistance) {
                minDistance = avgDistance;
                closestPlace = candidate;
            }
        }

        return closestPlace;
    }
}