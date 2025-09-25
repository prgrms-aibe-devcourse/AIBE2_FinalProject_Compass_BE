package com.compass.domain.chat.function.processing.phase3.date_selection.service;

import com.compass.domain.chat.function.processing.phase3.date_selection.model.*;
import com.compass.domain.chat.function.processing.phase3.date_selection.model.cluster.Cluster;
import com.compass.domain.chat.function.processing.phase3.date_selection.model.cluster.RegionCluster;
import com.compass.domain.chat.function.processing.phase3.date_selection.util.DistanceUtils;
import com.compass.domain.chat.model.context.TravelContext;
import com.compass.domain.chat.orchestrator.ContextManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

// 날짜별 선별 분산 처리 서비스 - 알고리즘 기반 클러스터링 + LLM 검수
@Slf4j
@Service
@RequiredArgsConstructor
public class DateSelectionDistributor {

    private final KMeansClusteringService clusteringService;
    private final ContextManager contextManager;
    private final OCRScheduleIntegrationService ocrIntegrationService;
    private final LLMReviewService llmReviewService;
    private final ObjectMapper objectMapper;

    // 최대 이동 거리 임계값 (km)
    private static final double MAX_DISTANCE_THRESHOLD = 5.0;  // 5km 이내는 같은 지역
    private static final double COMFORTABLE_DAILY_DISTANCE = 15.0;  // 하루 적정 이동거리
    private static final double WARNING_DAILY_DISTANCE = 20.0;  // LLM 검수 필요

    // 날짜별 선별 메인 처리 메서드 - 기존 호환성을 위한 오버로드
    public DateSelectionOutput processDistribution(String threadId, int tripDays) {
        // ContextManager에서 OCR 일정과 여행 시작일 가져오기
        Optional<TravelContext> contextOpt = contextManager.getContext(threadId, "mockUser");

        List<com.compass.domain.chat.model.dto.ConfirmedSchedule> ocrSchedules =
            contextOpt.map(TravelContext::getOcrConfirmedSchedules).orElse(new ArrayList<>());

        LocalDate tripStartDate = contextOpt
            .map(ctx -> ctx.getCollectedInfo().get(TravelContext.KEY_START_DATE))
            .filter(obj -> obj != null)
            .map(obj -> LocalDate.parse(obj.toString()))
            .orElse(LocalDate.now());

        return processDistribution(threadId, tripDays, ocrSchedules, tripStartDate);
    }

    // 날짜별 선별 메인 처리 메서드 (OCR 일정 포함)
    public DateSelectionOutput processDistribution(
            String threadId,
            int tripDays,
            List<com.compass.domain.chat.model.dto.ConfirmedSchedule> ocrSchedules,
            LocalDate tripStartDate) {
        log.info("날짜별 선별 처리 시작: threadId={}, tripDays={}, OCR일정={}개",
            threadId, tripDays, ocrSchedules != null ? ocrSchedules.size() : 0);

        try {
            // 1. 여행지 선별 결과 조회
            // TODO: Stage 1 구현 후 실제 데이터 로드 로직 추가
            List<TourPlace> allPlaces = new ArrayList<>();
            log.info("여행지 선별 결과 로드 완료: {}개 장소", allPlaces.size());

            // 빈 데이터인 경우 기본 결과 반환
            if (allPlaces.isEmpty()) {
                log.warn("Stage 1 데이터 없음 - 빈 일정 반환");
                Map<Integer, DailyItinerary> emptyItineraries = new HashMap<>();
                for (int day = 1; day <= tripDays; day++) {
                    emptyItineraries.put(day, DailyItinerary.builder()
                        .dayNumber(day)
                        .date(tripStartDate.plusDays(day - 1))
                        .regions(new ArrayList<>())
                        .places(new ArrayList<>())
                        .totalDistance(0.0)
                        .build());
                }
                return buildDateSelectionOutput(emptyItineraries);
            }

            // 2. 알고리즘으로 클러스터링 및 일정 생성
            Map<Integer, DailyItinerary> dailyItineraries =
                performAlgorithmicDistribution(allPlaces, tripDays);
            log.info("알고리즘 클러스터링 완료");

            // 3. OCR 확정 일정 통합 (최우선순위)
            if (ocrSchedules != null && !ocrSchedules.isEmpty()) {
                log.info("OCR 확정 일정 통합 시작");
                dailyItineraries = ocrIntegrationService.integrateOCRSchedules(
                    dailyItineraries, ocrSchedules, tripStartDate);
            }

            // 4. LLM 검수 (필요시에만)
            if (llmReviewService.needsReview(dailyItineraries)) {
                log.info("LLM 검수 필요 - 일정 검토 요청");
                dailyItineraries = llmReviewService.reviewAndAdjust(dailyItineraries, threadId);
            } else {
                log.info("알고리즘 결과 양호 - LLM 검수 생략");
            }

            // 4. 최종 출력 생성
            return buildDateSelectionOutput(dailyItineraries);

        } catch (Exception e) {
            log.error("날짜별 선별 처리 중 오류 발생", e);
            throw new RuntimeException("날짜별 선별 처리 실패", e);
        }
    }

    // 알고리즘 기반 분산 처리
    private Map<Integer, DailyItinerary> performAlgorithmicDistribution(
            List<TourPlace> places, int tripDays) {

        // 1. Elbow Method로 최적 클러스터 수 결정
        int optimalClusters = clusteringService.determineOptimalClusters(places, tripDays);
        log.info("최적 클러스터 수: {}", optimalClusters);

        // 2. K-Means 클러스터링
        List<Cluster> clusters = clusteringService.performKMeansClustering(places, optimalClusters);
        List<RegionCluster> regions = clusteringService.convertToRegionClusters(clusters);
        log.info("{}개 지역 클러스터 생성 완료", regions.size());

        // 3. 일자별 지역 할당
        Map<Integer, List<RegionCluster>> dayRegionAssignment =
            assignRegionsToDay(regions, tripDays);

        // 4. 일정 구성
        return createDailyItineraries(dayRegionAssignment);
    }

    // 지역을 일자별로 할당
    private Map<Integer, List<RegionCluster>> assignRegionsToDay(
            List<RegionCluster> regions, int tripDays) {

        Map<Integer, List<RegionCluster>> dayAssignment = new HashMap<>();

        // 초기화
        for (int day = 1; day <= tripDays; day++) {
            dayAssignment.put(day, new ArrayList<>());
        }

        // 지역 크기별로 정렬 (큰 것부터)
        regions.sort((a, b) -> Integer.compare(b.getPlaceCount(), a.getPlaceCount()));

        int currentDay = 1;
        for (RegionCluster region : regions) {
            int placeCount = region.getPlaceCount();

            if (placeCount >= 20) {
                // 큰 지역: 단독 일자
                dayAssignment.get(currentDay).add(region);
                currentDay = (currentDay % tripDays) + 1;
            } else if (placeCount >= 10) {
                // 중간 지역: 가능하면 조합
                addToOptimalDay(dayAssignment, region, tripDays);
            } else {
                // 작은 지역: 가장 가까운 지역과 합치기
                addToNearestRegion(dayAssignment, region);
            }
        }

        logDayAssignment(dayAssignment);
        return dayAssignment;
    }

    // 최적의 일자에 지역 추가
    private void addToOptimalDay(Map<Integer, List<RegionCluster>> dayAssignment,
                                RegionCluster region, int tripDays) {

        int optimalDay = 1;
        int minPlaceCount = Integer.MAX_VALUE;

        // 가장 장소가 적은 날 찾기
        for (int day = 1; day <= tripDays; day++) {
            int dayPlaceCount = dayAssignment.get(day).stream()
                .mapToInt(RegionCluster::getPlaceCount)
                .sum();

            if (dayPlaceCount < minPlaceCount) {
                minPlaceCount = dayPlaceCount;
                optimalDay = day;
            }
        }

        dayAssignment.get(optimalDay).add(region);
    }

    // 가장 가까운 지역이 있는 날에 추가
    private void addToNearestRegion(Map<Integer, List<RegionCluster>> dayAssignment,
                                   RegionCluster region) {

        int bestDay = 1;
        double minDistance = Double.MAX_VALUE;

        for (Map.Entry<Integer, List<RegionCluster>> entry : dayAssignment.entrySet()) {
            for (RegionCluster existingRegion : entry.getValue()) {
                double distance = DistanceUtils.calculateDistance(
                    region.getCenter().getLat(), region.getCenter().getLng(),
                    existingRegion.getCenter().getLat(), existingRegion.getCenter().getLng()
                );

                if (distance < minDistance) {
                    minDistance = distance;
                    bestDay = entry.getKey();
                }
            }
        }

        if (minDistance < MAX_DISTANCE_THRESHOLD) {
            dayAssignment.get(bestDay).add(region);
        } else {
            // 거리가 너무 먼 경우 가장 적은 날에 추가
            addToOptimalDay(dayAssignment, region, dayAssignment.size());
        }
    }

    // 일별 여행 일정 생성
    private Map<Integer, DailyItinerary> createDailyItineraries(
            Map<Integer, List<RegionCluster>> dayRegionAssignment) {

        Map<Integer, DailyItinerary> itineraries = new HashMap<>();

        for (Map.Entry<Integer, List<RegionCluster>> entry : dayRegionAssignment.entrySet()) {
            int day = entry.getKey();
            List<RegionCluster> dayRegions = entry.getValue();

            // 해당 일자의 모든 장소 수집
            List<TourPlace> dayPlaces = new ArrayList<>();
            List<String> regionNames = new ArrayList<>();

            for (RegionCluster region : dayRegions) {
                dayPlaces.addAll(region.getPlaces());
                regionNames.add(region.getRegionName());
            }

            // 시간 블록별로 최적 장소 선택
            List<TourPlace> selectedPlaces = selectOptimalPlaces(dayPlaces);

            // 총 이동 거리 계산
            double totalDistance = DistanceUtils.calculateTotalDistance(selectedPlaces);

            DailyItinerary itinerary = DailyItinerary.builder()
                .dayNumber(day)
                .date(LocalDate.now().plusDays(day - 1))
                .regions(regionNames)
                .places(selectedPlaces)
                .totalDistance(totalDistance)
                .build();

            itineraries.put(day, itinerary);
            log.info("Day {} 일정 생성: 지역={}, 장소={}개, 이동거리={}km",
                day, regionNames, selectedPlaces.size(), String.format("%.1f", totalDistance));
        }

        return itineraries;
    }

    // 시간 블록별로 최적 장소 선택
    private List<TourPlace> selectOptimalPlaces(List<TourPlace> candidates) {
        String[] timeBlocks = {
            "BREAKFAST", "MORNING_ACTIVITY", "LUNCH", "CAFE",
            "AFTERNOON_ACTIVITY", "DINNER", "EVENING_ACTIVITY"
        };

        List<TourPlace> selectedPlaces = new ArrayList<>();

        for (String timeBlock : timeBlocks) {
            List<TourPlace> blockCandidates = candidates.stream()
                .filter(p -> timeBlock.equals(p.timeBlock()))
                .collect(Collectors.toList());

            if (!blockCandidates.isEmpty()) {
                TourPlace selected = selectOptimalPlace(blockCandidates, selectedPlaces);
                if (selected != null) {
                    selectedPlaces.add(selected);
                }
            }
        }

        return selectedPlaces;
    }

    // 최적 장소 선택 (거리 + 카테고리 + 평점)
    private TourPlace selectOptimalPlace(List<TourPlace> candidates,
                                        List<TourPlace> currentSchedule) {

        if (currentSchedule.isEmpty()) {
            // 첫 장소는 평점 기준
            return candidates.stream()
                .max(Comparator.comparing(p -> p.rating() != null ? p.rating() : 0.0))
                .orElse(candidates.get(0));
        }

        TourPlace lastPlace = currentSchedule.get(currentSchedule.size() - 1);

        return candidates.stream()
            .max(Comparator.comparing(place -> {
                double score = 0.0;

                // 거리 점수 (가까울수록 높은 점수)
                double distance = DistanceUtils.calculateDistance(lastPlace, place);
                score += (10.0 - Math.min(distance, 10.0)); // 최대 10점

                // 카테고리 다양성 (다른 카테고리면 +3점)
                if (!place.category().equals(lastPlace.category())) {
                    score += 3.0;
                }

                // 평점 점수
                if (place.rating() != null) {
                    score += place.rating();
                }

                // 트렌디한 장소 보너스
                if (Boolean.TRUE.equals(place.isTrendy())) {
                    score += 2.0;
                }

                return score;
            }))
            .orElse(candidates.get(0));
    }









    // 최종 날짜별 선별 출력 생성
    private DateSelectionOutput buildDateSelectionOutput(Map<Integer, DailyItinerary> dailyItineraries) {
        TravelSummary summary = createSummary(dailyItineraries);

        return DateSelectionOutput.builder()
            .dailyItineraries(dailyItineraries)
            .summary(summary)
            .build();
    }

    // 요약 정보 생성
    private TravelSummary createSummary(Map<Integer, DailyItinerary> dailyItineraries) {
        int totalPlaces = dailyItineraries.values().stream()
            .mapToInt(d -> d.places().size())
            .sum();

        double totalDistance = dailyItineraries.values().stream()
            .mapToDouble(DailyItinerary::totalDistance)
            .sum();

        Set<String> allRegions = dailyItineraries.values().stream()
            .flatMap(d -> d.regions().stream())
            .collect(Collectors.toSet());

        Map<String, Integer> categoryDistribution = dailyItineraries.values().stream()
            .flatMap(d -> d.places().stream())
            .collect(Collectors.groupingBy(
                TourPlace::category,
                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
            ));

        return TravelSummary.builder()
            .totalDays(dailyItineraries.size())
            .totalPlaces(totalPlaces)
            .totalRegions(allRegions.size())
            .averageRegionsPerDay(allRegions.size() / (double) dailyItineraries.size())
            .categoryDistribution(categoryDistribution)
            .estimatedTotalDistance(totalDistance)
            .llmReviewApplied(false) // 실제 검수 여부 추가 필요
            .build();
    }

    // 일자별 할당 로깅
    private void logDayAssignment(Map<Integer, List<RegionCluster>> dayAssignment) {
        for (Map.Entry<Integer, List<RegionCluster>> entry : dayAssignment.entrySet()) {
            int day = entry.getKey();
            List<String> regionNames = entry.getValue().stream()
                .map(RegionCluster::getRegionName)
                .collect(Collectors.toList());
            int totalPlaces = entry.getValue().stream()
                .mapToInt(RegionCluster::getPlaceCount)
                .sum();
            log.info("Day {} 지역 할당: {} (총 {}개 장소)", day, regionNames, totalPlaces);
        }
    }
}