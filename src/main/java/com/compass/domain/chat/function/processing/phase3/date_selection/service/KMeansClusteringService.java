package com.compass.domain.chat.function.processing.phase3.date_selection.service;

import com.compass.domain.chat.function.processing.phase3.date_selection.model.TourPlace;
import com.compass.domain.chat.function.processing.phase3.date_selection.model.cluster.Cluster;
import com.compass.domain.chat.function.processing.phase3.date_selection.model.cluster.Point;
import com.compass.domain.chat.function.processing.phase3.date_selection.model.cluster.RegionCluster;
import com.compass.domain.chat.function.processing.phase3.date_selection.util.DistanceUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

// K-Means 클러스터링 서비스
@Slf4j
@Service
public class KMeansClusteringService {

    private static final int MAX_ITERATIONS = 100;
    private static final double ELBOW_THRESHOLD = 0.2;  // 20% 미만 감소시 최적점
    private final Random random = new Random();

    // Elbow Method로 최적 클러스터 수 결정
    public int determineOptimalClusters(List<TourPlace> places, int tripDays) {
        log.info("Elbow Method로 최적 클러스터 수 결정 시작: tripDays={}, places={}",
            tripDays, places.size());

        List<Double> wcss = new ArrayList<>();

        // tripDays부터 tripDays * 2까지 테스트
        for (int k = tripDays; k <= tripDays * 2; k++) {
            List<Cluster> clusters = performKMeansClustering(places, k);
            double totalWCSS = calculateWCSS(clusters);
            wcss.add(totalWCSS);
            log.debug("k={}: WCSS={}", k, totalWCSS);
        }

        // 감소율이 threshold 미만이 되는 지점 찾기
        int optimalK = tripDays;
        for (int i = 1; i < wcss.size(); i++) {
            double decreaseRate = (wcss.get(i - 1) - wcss.get(i)) / wcss.get(i - 1);
            log.debug("k={}: 감소율={}", tripDays + i, decreaseRate);

            if (decreaseRate < ELBOW_THRESHOLD) {
                optimalK = tripDays + i - 1;
                break;
            }
        }

        // 최대 tripDays * 1.5로 제한
        optimalK = Math.min(optimalK, (int)(tripDays * 1.5));
        log.info("최적 클러스터 수 결정: {}", optimalK);

        return optimalK;
    }

    // K-Means 클러스터링 수행
    public List<Cluster> performKMeansClustering(List<TourPlace> places, int k) {
        if (places.isEmpty()) {
            return new ArrayList<>();
        }

        log.debug("K-Means 클러스터링 시작: k={}, places={}", k, places.size());

        // 1. 초기 중심점 설정 (K-Means++ 알고리즘)
        List<Point> centroids = initializeCentroidsKMeansPlusPlus(places, k);

        // 2. 클러스터 초기화
        List<Cluster> clusters = new ArrayList<>();
        for (Point centroid : centroids) {
            clusters.add(new Cluster(centroid));
        }

        // 3. 반복적으로 클러스터 할당 및 중심점 업데이트
        boolean changed = true;
        int iteration = 0;

        while (changed && iteration < MAX_ITERATIONS) {
            // 이전 할당 초기화
            for (Cluster cluster : clusters) {
                cluster.clearPlaces();
            }

            changed = false;

            // 각 장소를 가장 가까운 클러스터에 할당
            for (TourPlace place : places) {
                Cluster nearest = DistanceUtils.findNearestCluster(place, clusters);
                nearest.addPlace(place);
                changed = true;
            }

            // 클러스터 중심점 재계산
            for (Cluster cluster : clusters) {
                cluster.recalculateCenter();
            }

            iteration++;
        }

        log.debug("K-Means 클러스터링 완료: iterations={}", iteration);
        return clusters;
    }

    // K-Means++ 초기화 (더 나은 초기 중심점 선택)
    private List<Point> initializeCentroidsKMeansPlusPlus(List<TourPlace> places, int k) {
        List<Point> centroids = new ArrayList<>();

        // 첫 번째 중심점은 랜덤 선택
        TourPlace first = places.get(random.nextInt(places.size()));
        centroids.add(new Point(first.latitude(), first.longitude()));

        // 나머지 중심점은 거리 비례 확률로 선택
        for (int i = 1; i < k; i++) {
            double[] distances = new double[places.size()];
            double totalDistance = 0;

            // 각 장소에서 가장 가까운 중심점까지의 거리 계산
            for (int j = 0; j < places.size(); j++) {
                TourPlace place = places.get(j);
                double minDist = Double.MAX_VALUE;

                for (Point centroid : centroids) {
                    double dist = DistanceUtils.calculateDistance(
                        place.latitude(), place.longitude(),
                        centroid.getLat(), centroid.getLng()
                    );
                    minDist = Math.min(minDist, dist);
                }

                distances[j] = minDist * minDist; // 거리 제곱
                totalDistance += distances[j];
            }

            // 거리 비례 확률로 다음 중심점 선택
            double randomValue = random.nextDouble() * totalDistance;
            double cumulative = 0;

            for (int j = 0; j < places.size(); j++) {
                cumulative += distances[j];
                if (cumulative >= randomValue) {
                    TourPlace selected = places.get(j);
                    centroids.add(new Point(
                        selected.latitude(),
                        selected.longitude()
                    ));
                    break;
                }
            }
        }

        return centroids;
    }

    // Within-Cluster Sum of Squares 계산
    private double calculateWCSS(List<Cluster> clusters) {
        double totalWCSS = 0.0;

        for (Cluster cluster : clusters) {
            Point center = cluster.getCenter();
            for (TourPlace place : cluster.getPlaces()) {
                double distance = DistanceUtils.calculateDistance(
                    place.latitude(), place.longitude(),
                    center.getLat(), center.getLng()
                );
                totalWCSS += distance * distance;
            }
        }

        return totalWCSS;
    }

    // Cluster를 RegionCluster로 변환
    public List<RegionCluster> convertToRegionClusters(List<Cluster> clusters) {
        List<RegionCluster> regionClusters = new ArrayList<>();
        int regionIndex = 1;

        for (Cluster cluster : clusters) {
            if (cluster.getPlaces().isEmpty()) continue;

            // 평균 평점 계산
            double avgRating = cluster.getPlaces().stream()
                .filter(p -> p.rating() != null)
                .mapToDouble(TourPlace::rating)
                .average()
                .orElse(0.0);

            // 지역명 생성 (가장 많이 언급된 주소 기반)
            String regionName = extractRegionName(cluster.getPlaces(), regionIndex);

            RegionCluster regionCluster = RegionCluster.builder()
                .regionName(regionName)
                .center(cluster.getCenter())
                .places(new ArrayList<>(cluster.getPlaces()))
                .averageRating(avgRating)
                .placeCount(cluster.getPlaces().size())
                .build();

            regionClusters.add(regionCluster);
            regionIndex++;
        }

        // 크기별로 정렬 (큰 것부터)
        regionClusters.sort((a, b) -> Integer.compare(b.getPlaceCount(), a.getPlaceCount()));

        return regionClusters;
    }

    // 지역명 추출 (주소에서 가장 자주 등장하는 지역명)
    private String extractRegionName(List<TourPlace> places, int defaultIndex) {
        Map<String, Long> regionFrequency = places.stream()
            .filter(p -> p.address() != null)
            .map(p -> p.address().split(" ")[0])  // 첫 번째 단어를 지역명으로 가정
            .collect(Collectors.groupingBy(
                region -> region,
                Collectors.counting()
            ));

        return regionFrequency.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("지역" + defaultIndex);
    }
}