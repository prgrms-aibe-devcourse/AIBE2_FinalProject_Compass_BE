package com.compass.domain.chat.route_optimization.service;

import com.compass.domain.chat.function.processing.phase3.date_selection.model.TourPlace;
import com.compass.domain.chat.route_optimization.client.KakaoMobilityClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 시간블록별 여러 후보 중 최적 경로를 선택하는 서비스
 * Dynamic Programming with Beam Search 방식 사용
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MultiPathOptimizationService {

    private final KakaoMobilityClient kakaoMobilityClient;

    // Beam Search 파라미터
    private static final int BEAM_WIDTH = 3; // 각 단계에서 유지할 최대 후보 수
    private static final double DISTANCE_WEIGHT = 0.4;
    private static final double TIME_WEIGHT = 0.3;
    private static final double RATING_WEIGHT = 0.3;

    /**
     * 시간블록별 후보들 중에서 최적 경로 선택
     * @param timeBlockCandidates 시간블록별 후보 리스트 맵
     * @param transportMode 이동 수단
     * @return 최적 경로 (각 시간블록에서 선택된 장소들)
     */
    public List<TourPlace> findOptimalPath(
        Map<String, List<TourPlace>> timeBlockCandidates,
        KakaoMobilityClient.TransportMode transportMode
    ) {
        log.info("다중 경로 최적화 시작: {} 개 시간블록", timeBlockCandidates.size());

        // 시간블록 순서 정의
        List<String> timeBlockOrder = List.of(
            "BREAKFAST", "MORNING_ACTIVITY", "LUNCH",
            "AFTERNOON_ACTIVITY", "DINNER", "EVENING_ACTIVITY"
        );

        // 실제로 존재하는 시간블록만 필터링
        List<String> activeTimeBlocks = timeBlockOrder.stream()
            .filter(timeBlockCandidates::containsKey)
            .collect(Collectors.toList());

        if (activeTimeBlocks.isEmpty()) {
            log.warn("활성 시간블록이 없습니다");
            return List.of();
        }

        // Dynamic Programming with Beam Search
        return beamSearchOptimization(
            timeBlockCandidates,
            activeTimeBlocks,
            transportMode
        );
    }

    /**
     * Beam Search를 이용한 경로 최적화
     */
    private List<TourPlace> beamSearchOptimization(
        Map<String, List<TourPlace>> candidates,
        List<String> timeBlocks,
        KakaoMobilityClient.TransportMode transportMode
    ) {
        // 초기 상태: 첫 시간블록의 모든 후보
        List<PathState> currentBeam = new ArrayList<>();

        for (TourPlace place : candidates.get(timeBlocks.get(0))) {
            currentBeam.add(new PathState(List.of(place), 0.0));
        }

        // 각 시간블록을 순회하며 경로 확장
        for (int i = 1; i < timeBlocks.size(); i++) {
            String currentBlock = timeBlocks.get(i);
            List<TourPlace> currentCandidates = candidates.get(currentBlock);

            List<PathState> nextBeam = new ArrayList<>();

            // 현재 beam의 각 경로에서 다음 후보들로 확장
            for (PathState state : currentBeam) {
                TourPlace lastPlace = state.path.get(state.path.size() - 1);

                for (TourPlace candidate : currentCandidates) {
                    // 새로운 경로 생성
                    List<TourPlace> newPath = new ArrayList<>(state.path);
                    newPath.add(candidate);

                    // 비용 계산
                    double transitionCost = calculateTransitionCost(
                        lastPlace, candidate, transportMode
                    );
                    double newCost = state.totalCost + transitionCost;

                    nextBeam.add(new PathState(newPath, newCost));
                }
            }

            // 상위 BEAM_WIDTH 개만 유지
            currentBeam = nextBeam.stream()
                .sorted(Comparator.comparingDouble(s -> s.totalCost))
                .limit(BEAM_WIDTH)
                .collect(Collectors.toList());

            log.debug("시간블록 {} 처리 완료: {} 개 경로 유지", currentBlock, currentBeam.size());
        }

        // 최적 경로 반환
        if (!currentBeam.isEmpty()) {
            PathState bestPath = currentBeam.get(0);
            log.info("최적 경로 선택 완료: 총 비용 {}", bestPath.totalCost);
            return bestPath.path;
        }

        return List.of();
    }

    /**
     * 두 장소 간 전이 비용 계산
     */
    private double calculateTransitionCost(
        TourPlace from,
        TourPlace to,
        KakaoMobilityClient.TransportMode transportMode
    ) {
        // 거리 계산
        KakaoMobilityClient.DistanceInfo distanceInfo = kakaoMobilityClient.calculateDistance(
            from.latitude(), from.longitude(),
            to.latitude(), to.longitude(),
            transportMode
        );

        // 정규화된 비용 계산
        double distanceCost = distanceInfo.distance() / 10.0;  // 10km를 1로 정규화
        double timeCost = distanceInfo.duration() / 30.0;      // 30분을 1로 정규화
        double ratingBonus = (5.0 - to.rating()) / 5.0;        // 높은 평점일수록 낮은 비용

        return DISTANCE_WEIGHT * distanceCost +
               TIME_WEIGHT * timeCost +
               RATING_WEIGHT * ratingBonus;
    }

    /**
     * 경로 상태를 나타내는 내부 클래스
     */
    private static class PathState {
        final List<TourPlace> path;
        final double totalCost;

        PathState(List<TourPlace> path, double totalCost) {
            this.path = path;
            this.totalCost = totalCost;
        }
    }

    /**
     * 전체 경로의 통계 정보 계산
     */
    public RouteStatistics calculateStatistics(
        List<TourPlace> route,
        KakaoMobilityClient.TransportMode transportMode
    ) {
        if (route.size() < 2) {
            return new RouteStatistics(0.0, 0, route.size());
        }

        double totalDistance = 0.0;
        int totalDuration = 0;

        for (int i = 0; i < route.size() - 1; i++) {
            TourPlace from = route.get(i);
            TourPlace to = route.get(i + 1);

            KakaoMobilityClient.DistanceInfo info = kakaoMobilityClient.calculateDistance(
                from.latitude(), from.longitude(),
                to.latitude(), to.longitude(),
                transportMode
            );

            totalDistance += info.distance();
            totalDuration += info.duration();
        }

        return new RouteStatistics(totalDistance, totalDuration, route.size());
    }

    public record RouteStatistics(
        double totalDistance,  // km
        int totalDuration,     // 분
        int placeCount
    ) {}
}