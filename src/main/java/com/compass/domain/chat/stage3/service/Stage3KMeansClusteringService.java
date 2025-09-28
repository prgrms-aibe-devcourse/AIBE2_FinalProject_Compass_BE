package com.compass.domain.chat.stage3.service;

import com.compass.domain.chat.model.TravelPlace;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class Stage3KMeansClusteringService {

    private static final int MAX_ITERATIONS = 100;
    private static final double CONVERGENCE_THRESHOLD = 0.001;

    public Map<Integer, List<TravelPlace>> clusterPlaces(List<TravelPlace> places, int k) {
        if (places.isEmpty() || k <= 0) {
            return new HashMap<>();
        }

        // K가 장소 수보다 크면 조정
        k = Math.min(k, places.size());
        log.info("Clustering {} places into {} clusters", places.size(), k);

        // 유효한 좌표를 가진 장소만 필터링
        List<TravelPlace> validPlaces = places.stream()
            .filter(p -> p.getLatitude() != null && p.getLongitude() != null)
            .collect(Collectors.toList());

        if (validPlaces.isEmpty()) {
            log.warn("No places with valid coordinates");
            return new HashMap<>();
        }

        // 초기 중심점 선택 (K-means++ 알고리즘)
        List<Point> centroids = initializeCentroids(validPlaces, k);
        Map<Integer, List<TravelPlace>> clusters = new HashMap<>();

        boolean converged = false;
        int iteration = 0;

        while (!converged && iteration < MAX_ITERATIONS) {
            // 각 장소를 가장 가까운 중심점에 할당
            clusters.clear();
            for (TravelPlace place : validPlaces) {
                int nearestCluster = findNearestCentroid(place, centroids);
                clusters.computeIfAbsent(nearestCluster, key -> new ArrayList<>()).add(place);
            }

            // 새로운 중심점 계산
            List<Point> newCentroids = new ArrayList<>();
            for (int i = 0; i < k; i++) {
                List<TravelPlace> clusterPlaces = clusters.getOrDefault(i, new ArrayList<>());
                if (!clusterPlaces.isEmpty()) {
                    Point newCentroid = calculateCentroid(clusterPlaces);
                    newCentroids.add(newCentroid);
                } else {
                    // 빈 클러스터인 경우 이전 중심점 유지
                    newCentroids.add(centroids.get(i));
                }
            }

            // 수렴 확인
            converged = checkConvergence(centroids, newCentroids);
            centroids = newCentroids;
            iteration++;
        }

        log.info("K-means clustering completed in {} iterations", iteration);
        return clusters;
    }

    // 클러스터 중심점 계산
    public List<ClusterCenter> getClusterCenters(Map<Integer, List<TravelPlace>> clusters) {
        List<ClusterCenter> centers = new ArrayList<>();

        for (Map.Entry<Integer, List<TravelPlace>> entry : clusters.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                Point centroid = calculateCentroid(entry.getValue());
                ClusterCenter center = new ClusterCenter(
                    entry.getKey(),
                    centroid.lat,
                    centroid.lon,
                    entry.getValue().size()
                );
                centers.add(center);
            }
        }

        return centers;
    }

    // 날짜별 클러스터 할당
    public ClusterAssignmentResult assignClustersTodays(
            Map<Integer, List<TravelPlace>> clusters,
            int numberOfDays) {

        Map<Integer, List<TravelPlace>> dayAssignments = new HashMap<>();
        Map<Integer, Integer> clusterToDayMap = new HashMap<>();

        if (numberOfDays <= 0) {
            return new ClusterAssignmentResult(dayAssignments, clusterToDayMap);
        }

        // 클러스터를 크기 순으로 정렬
        List<Map.Entry<Integer, List<TravelPlace>>> sortedClusters = clusters.entrySet()
            .stream()
            .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
            .collect(Collectors.toList());

        // 균등 분배를 위한 날짜별 장소 카운트 추적
        int[] dayPlaceCounts = new int[numberOfDays];

        // 각 클러스터를 장소 수가 가장 적은 날짜에 할당
        for (Map.Entry<Integer, List<TravelPlace>> clusterEntry : sortedClusters) {
            // 가장 적은 장소를 가진 날짜 찾기
            int minDay = 0;
            int minCount = dayPlaceCounts[0];

            for (int day = 1; day < numberOfDays; day++) {
                if (dayPlaceCounts[day] < minCount) {
                    minCount = dayPlaceCounts[day];
                    minDay = day;
                }
            }

            // 해당 날짜에 클러스터 할당
            List<TravelPlace> dayPlaces = dayAssignments.computeIfAbsent(minDay, k -> new ArrayList<>());
            dayPlaces.addAll(clusterEntry.getValue());
            clusterToDayMap.put(clusterEntry.getKey(), minDay);

            // 날짜별 장소 수 업데이트
            dayPlaceCounts[minDay] += clusterEntry.getValue().size();

            log.debug("Cluster {} with {} places assigned to day {}",
                clusterEntry.getKey(), clusterEntry.getValue().size(), minDay);
        }

        return new ClusterAssignmentResult(dayAssignments, clusterToDayMap);
    }

    // K-means++ 초기화
    private List<Point> initializeCentroids(List<TravelPlace> places, int k) {
        List<Point> centroids = new ArrayList<>();
        Random random = new Random();

        // 첫 번째 중심점은 무작위 선택
        TravelPlace firstPlace = places.get(random.nextInt(places.size()));
        centroids.add(new Point(firstPlace.getLatitude(), firstPlace.getLongitude()));

        // 나머지 중심점은 거리 기반 확률적 선택
        for (int i = 1; i < k; i++) {
            double[] distances = new double[places.size()];
            double totalDistance = 0;

            for (int j = 0; j < places.size(); j++) {
                TravelPlace place = places.get(j);
                double minDistance = Double.MAX_VALUE;

                for (Point centroid : centroids) {
                    double dist = calculateDistance(
                        place.getLatitude(), place.getLongitude(),
                        centroid.lat, centroid.lon
                    );
                    minDistance = Math.min(minDistance, dist);
                }

                distances[j] = minDistance * minDistance; // 제곱하여 멀리 있는 점 선호
                totalDistance += distances[j];
            }

            // 확률적 선택
            double randomValue = random.nextDouble() * totalDistance;
            double cumulativeDistance = 0;

            for (int j = 0; j < places.size(); j++) {
                cumulativeDistance += distances[j];
                if (cumulativeDistance >= randomValue) {
                    TravelPlace selectedPlace = places.get(j);
                    centroids.add(new Point(selectedPlace.getLatitude(), selectedPlace.getLongitude()));
                    break;
                }
            }
        }

        return centroids;
    }

    // 가장 가까운 중심점 찾기
    private int findNearestCentroid(TravelPlace place, List<Point> centroids) {
        int nearestCluster = 0;
        double minDistance = Double.MAX_VALUE;

        for (int i = 0; i < centroids.size(); i++) {
            Point centroid = centroids.get(i);
            double distance = calculateDistance(
                place.getLatitude(), place.getLongitude(),
                centroid.lat, centroid.lon
            );

            if (distance < minDistance) {
                minDistance = distance;
                nearestCluster = i;
            }
        }

        return nearestCluster;
    }

    // 클러스터 중심점 계산
    private Point calculateCentroid(List<TravelPlace> places) {
        double sumLat = 0;
        double sumLon = 0;

        for (TravelPlace place : places) {
            sumLat += place.getLatitude();
            sumLon += place.getLongitude();
        }

        return new Point(sumLat / places.size(), sumLon / places.size());
    }

    // 수렴 확인
    private boolean checkConvergence(List<Point> oldCentroids, List<Point> newCentroids) {
        for (int i = 0; i < oldCentroids.size(); i++) {
            double distance = calculateDistance(
                oldCentroids.get(i).lat, oldCentroids.get(i).lon,
                newCentroids.get(i).lat, newCentroids.get(i).lon
            );

            if (distance > CONVERGENCE_THRESHOLD) {
                return false;
            }
        }
        return true;
    }

    // Haversine 공식으로 거리 계산 (km)
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371; // 지구 반지름 (km)

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    // 내부 클래스들
    private static class Point {
        final double lat;
        final double lon;

        Point(double lat, double lon) {
            this.lat = lat;
            this.lon = lon;
        }
    }

    public static class ClusterAssignmentResult {
        private final Map<Integer, List<TravelPlace>> dayAssignments;
        private final Map<Integer, Integer> clusterToDayMap;

        public ClusterAssignmentResult(Map<Integer, List<TravelPlace>> dayAssignments,
                                       Map<Integer, Integer> clusterToDayMap) {
            this.dayAssignments = dayAssignments;
            this.clusterToDayMap = clusterToDayMap;
        }

        public Map<Integer, List<TravelPlace>> getDayAssignments() {
            return dayAssignments;
        }

        public Map<Integer, Integer> getClusterToDayMap() {
            return clusterToDayMap;
        }
    }

    public static class ClusterCenter {
        private final int clusterId;
        private final double latitude;
        private final double longitude;
        private final int size;

        public ClusterCenter(int clusterId, double latitude, double longitude, int size) {
            this.clusterId = clusterId;
            this.latitude = latitude;
            this.longitude = longitude;
            this.size = size;
        }

        public int getClusterId() { return clusterId; }
        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
        public int getSize() { return size; }
    }
}
