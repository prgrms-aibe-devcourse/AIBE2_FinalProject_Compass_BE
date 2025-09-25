package com.compass.domain.chat.route_optimization.service;

import com.compass.domain.chat.function.processing.phase3.date_selection.model.TourPlace;
import com.compass.domain.chat.route_optimization.client.KakaoMobilityClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 사용자 선택 기반 동선 최적화 서비스
 * 1. 사용자가 선택한 필수 장소를 앵커로 설정
 * 2. 앵커 간 최적 순서 결정
 * 3. 남은 시간대를 동선에 맞춰 자동 채움
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserSelectionRouteOptimizer {

    private final KakaoMobilityClient kakaoMobilityClient;

    /**
     * 사용자 선택 기반 일정 최적화
     *
     * @param allCandidates 전체 후보 장소들
     * @param userSelections 사용자가 선택한 필수 방문 장소들
     * @param dayNumber 여행 일차
     * @return 최적화된 하루 일정
     */
    public DayRoute optimizeWithUserSelection(
        List<TourPlace> allCandidates,
        List<TourPlace> userSelections,
        int dayNumber
    ) {
        log.info("Day {} 사용자 선택 기반 최적화: 선택 {}개, 전체 후보 {}개",
            dayNumber, userSelections.size(), allCandidates.size());

        // 1. 사용자 선택 장소들의 최적 방문 순서 결정
        List<TourPlace> orderedAnchors = determineOptimalOrder(userSelections);

        // 2. 시간대 할당
        Map<String, TourPlace> timeBlockAssignments = assignTimeBlocks(orderedAnchors);

        // 3. 빈 시간대 자동 채우기
        List<TourPlace> completeRoute = fillEmptyTimeSlots(
            timeBlockAssignments,
            allCandidates,
            userSelections
        );

        // 4. 최종 경로 정보 생성
        return createDayRoute(completeRoute, dayNumber);
    }

    /**
     * TSP 알고리즘으로 사용자 선택 장소들의 최적 방문 순서 결정
     */
    private List<TourPlace> determineOptimalOrder(List<TourPlace> selections) {
        if (selections.size() <= 2) {
            return selections;
        }

        // Nearest Neighbor 알고리즘 사용
        List<TourPlace> ordered = new ArrayList<>();
        Set<TourPlace> unvisited = new HashSet<>(selections);

        // 가장 북쪽(또는 서쪽) 장소를 시작점으로
        TourPlace start = findStartingPoint(selections);
        ordered.add(start);
        unvisited.remove(start);

        TourPlace current = start;
        while (!unvisited.isEmpty()) {
            TourPlace nearest = findNearestPlace(current, unvisited);
            ordered.add(nearest);
            unvisited.remove(nearest);
            current = nearest;
        }

        log.info("최적 방문 순서 결정: {}",
            ordered.stream().map(TourPlace::name).collect(Collectors.joining(" → ")));

        return ordered;
    }

    /**
     * 시간대별로 앵커 장소 할당
     */
    private Map<String, TourPlace> assignTimeBlocks(List<TourPlace> orderedAnchors) {
        Map<String, TourPlace> assignments = new HashMap<>();
        List<String> mainTimeBlocks = List.of(
            "MORNING_ACTIVITY", "LUNCH", "AFTERNOON_ACTIVITY", "DINNER", "EVENING_ACTIVITY"
        );

        // 앵커 개수에 따라 균등 분배
        int anchorCount = orderedAnchors.size();
        if (anchorCount == 0) return assignments;

        // 시간대를 균등하게 분배
        int interval = mainTimeBlocks.size() / anchorCount;
        for (int i = 0; i < anchorCount; i++) {
            int timeBlockIndex = Math.min(i * interval, mainTimeBlocks.size() - 1);
            String timeBlock = mainTimeBlocks.get(timeBlockIndex);

            // 식사 시간대는 식당 우선, 아니면 근처 시간대로 조정
            if ((timeBlock.equals("LUNCH") || timeBlock.equals("DINNER"))
                && !isRestaurant(orderedAnchors.get(i))) {
                timeBlock = adjustNonMealTimeBlock(timeBlock, timeBlockIndex, mainTimeBlocks);
            }

            assignments.put(timeBlock, orderedAnchors.get(i));
        }

        return assignments;
    }

    /**
     * 빈 시간대를 동선에 맞춰 자동으로 채우기
     */
    private List<TourPlace> fillEmptyTimeSlots(
        Map<String, TourPlace> anchors,
        List<TourPlace> allCandidates,
        List<TourPlace> userSelections
    ) {
        List<TourPlace> completeRoute = new ArrayList<>();

        // 사용자가 선택하지 않은 후보들
        List<TourPlace> availableCandidates = allCandidates.stream()
            .filter(p -> !userSelections.contains(p))
            .collect(Collectors.toList());

        List<String> allTimeBlocks = List.of(
            "BREAKFAST", "MORNING_ACTIVITY", "LUNCH",
            "AFTERNOON_ACTIVITY", "DINNER", "EVENING_ACTIVITY", "CAFE"
        );

        TourPlace previousPlace = null;

        for (String timeBlock : allTimeBlocks) {
            if (anchors.containsKey(timeBlock)) {
                // 앵커 장소 추가
                TourPlace anchor = anchors.get(timeBlock);
                completeRoute.add(anchor);
                previousPlace = anchor;
            } else {
                // 빈 시간대 - 이전 장소와 다음 앵커를 고려해서 채우기
                TourPlace nextAnchor = findNextAnchor(timeBlock, anchors, allTimeBlocks);
                TourPlace bestCandidate = findBestCandidateForTimeSlot(
                    timeBlock,
                    previousPlace,
                    nextAnchor,
                    availableCandidates
                );

                if (bestCandidate != null) {
                    completeRoute.add(bestCandidate);
                    availableCandidates.remove(bestCandidate);
                    previousPlace = bestCandidate;
                }
            }
        }

        return completeRoute;
    }

    /**
     * 시간대에 맞는 최적 후보 찾기
     */
    private TourPlace findBestCandidateForTimeSlot(
        String timeBlock,
        TourPlace previousPlace,
        TourPlace nextAnchor,
        List<TourPlace> candidates
    ) {
        // 시간대별 카테고리 필터링
        List<TourPlace> filtered = filterByTimeBlock(candidates, timeBlock);
        if (filtered.isEmpty()) {
            filtered = candidates; // 해당 카테고리가 없으면 전체에서 선택
        }

        // 점수 계산: 거리(40%) + 평점(30%) + 카테고리 적합도(30%)
        return filtered.stream()
            .max(Comparator.comparing(place -> calculatePlaceScore(
                place, previousPlace, nextAnchor, timeBlock
            )))
            .orElse(null);
    }

    /**
     * 장소 점수 계산
     */
    private double calculatePlaceScore(
        TourPlace candidate,
        TourPlace previous,
        TourPlace next,
        String timeBlock
    ) {
        double score = 0.0;

        // 1. 거리 점수 (가까울수록 높음)
        if (previous != null) {
            double distance = calculateDistance(previous, candidate);
            score += (10.0 - Math.min(distance, 10.0)) * 0.4; // 최대 4점
        }

        // 2. 다음 앵커와의 거리 (중간 위치 선호)
        if (next != null && previous != null) {
            double toPrev = calculateDistance(candidate, previous);
            double toNext = calculateDistance(candidate, next);
            double balance = 1.0 - Math.abs(toPrev - toNext) / (toPrev + toNext + 0.1);
            score += balance * 2.0; // 최대 2점
        }

        // 3. 평점
        score += (candidate.rating() != null ? candidate.rating() : 3.0) * 0.6; // 최대 3점

        // 4. 시간대 적합도
        score += getTimeBlockCompatibility(candidate, timeBlock) * 1.0; // 최대 1점

        return score;
    }

    /**
     * 시간대별 카테고리 필터링
     */
    private List<TourPlace> filterByTimeBlock(List<TourPlace> candidates, String timeBlock) {
        return candidates.stream()
            .filter(place -> isCompatibleWithTimeBlock(place, timeBlock))
            .collect(Collectors.toList());
    }

    private boolean isCompatibleWithTimeBlock(TourPlace place, String timeBlock) {
        String category = place.category();
        if (category == null) return true;

        return switch (timeBlock) {
            case "BREAKFAST", "LUNCH", "DINNER" ->
                category.contains("맛집") || category.contains("음식") || category.contains("레스토랑");
            case "CAFE" ->
                category.contains("카페") || category.contains("디저트");
            case "MORNING_ACTIVITY", "AFTERNOON_ACTIVITY" ->
                category.contains("관광") || category.contains("문화") || category.contains("체험");
            case "EVENING_ACTIVITY" ->
                category.contains("야경") || category.contains("쇼핑") || category.contains("엔터");
            default -> true;
        };
    }

    private double getTimeBlockCompatibility(TourPlace place, String timeBlock) {
        if (isCompatibleWithTimeBlock(place, timeBlock)) {
            return 1.0;
        }
        return 0.5; // 부분 적합
    }

    private boolean isRestaurant(TourPlace place) {
        String category = place.category();
        return category != null && (
            category.contains("맛집") ||
            category.contains("음식") ||
            category.contains("레스토랑")
        );
    }

    private String adjustNonMealTimeBlock(String mealBlock, int index, List<String> timeBlocks) {
        // 식사 시간이 아닌 인접 시간대로 조정
        if (mealBlock.equals("LUNCH")) {
            return index > 0 ? timeBlocks.get(index - 1) : timeBlocks.get(index + 1);
        } else if (mealBlock.equals("DINNER")) {
            return index < timeBlocks.size() - 1 ? timeBlocks.get(index + 1) : timeBlocks.get(index - 1);
        }
        return mealBlock;
    }

    private TourPlace findStartingPoint(List<TourPlace> places) {
        // 가장 북쪽 또는 서쪽 장소를 시작점으로
        return places.stream()
            .min(Comparator.comparing((TourPlace p) -> p.latitude() + p.longitude()))
            .orElse(places.get(0));
    }

    private TourPlace findNearestPlace(TourPlace from, Set<TourPlace> candidates) {
        return candidates.stream()
            .min(Comparator.comparing(to -> calculateDistance(from, to)))
            .orElse(null);
    }

    private TourPlace findNextAnchor(String currentTimeBlock, Map<String, TourPlace> anchors, List<String> timeBlocks) {
        int currentIndex = timeBlocks.indexOf(currentTimeBlock);
        for (int i = currentIndex + 1; i < timeBlocks.size(); i++) {
            if (anchors.containsKey(timeBlocks.get(i))) {
                return anchors.get(timeBlocks.get(i));
            }
        }
        return null;
    }

    private double calculateDistance(TourPlace from, TourPlace to) {
        // 간단한 유클리드 거리 (실제로는 Haversine 공식 사용)
        double latDiff = from.latitude() - to.latitude();
        double lngDiff = from.longitude() - to.longitude();
        return Math.sqrt(latDiff * latDiff + lngDiff * lngDiff) * 111.0; // km 근사
    }

    private DayRoute createDayRoute(List<TourPlace> route, int dayNumber) {
        double totalDistance = 0.0;
        int totalDuration = 0;

        // 구간별 거리/시간 계산
        for (int i = 0; i < route.size() - 1; i++) {
            double distance = calculateDistance(route.get(i), route.get(i + 1));
            totalDistance += distance;
            totalDuration += (int)(distance * 3); // 대략 시속 20km 가정
        }

        return new DayRoute(dayNumber, route, totalDistance, totalDuration);
    }

    /**
     * 하루 동선 정보
     */
    public record DayRoute(
        int dayNumber,
        List<TourPlace> places,
        double totalDistance,  // km
        int totalDuration      // 분
    ) {}
}