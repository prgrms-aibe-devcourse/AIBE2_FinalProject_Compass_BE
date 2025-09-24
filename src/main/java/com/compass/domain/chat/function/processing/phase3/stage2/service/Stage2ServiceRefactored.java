package com.compass.domain.chat.function.processing.phase3.stage2.service;

import com.compass.domain.chat.function.processing.phase3.stage2.mock.MockDataGenerator;
import com.compass.domain.chat.function.processing.phase3.stage2.model.*;
import com.compass.domain.chat.function.processing.phase3.stage2.model.cluster.Cluster;
import com.compass.domain.chat.function.processing.phase3.stage2.model.cluster.RegionCluster;
import com.compass.domain.chat.function.processing.phase3.stage2.model.llm.LLMReviewResponse;
import com.compass.domain.chat.function.processing.phase3.stage2.util.DistanceUtils;
import com.compass.domain.chat.model.context.TravelContext;
import com.compass.domain.chat.orchestrator.ContextManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

// Stage2 리팩토링된 서비스 - CLAUDE.md 원칙 적용
@Slf4j
@Service
@RequiredArgsConstructor
public class Stage2ServiceRefactored {

    private final MockDataGenerator mockDataGenerator;
    private final KMeansClusteringService clusteringService;
    private final ChatModel chatModel;
    private final ContextManager contextManager;
    private final ObjectMapper objectMapper;

    // 거리 임계값 상수
    private static final double MAX_DISTANCE_THRESHOLD = 5.0;
    private static final double WARNING_DAILY_DISTANCE = 20.0;

    // 메인 처리 메서드 - 20줄 이내로 단순화
    public Stage2Output processDistribution(String threadId, int tripDays) {
        MDC.put("threadId", threadId);
        MDC.put("tripDays", String.valueOf(tripDays));

        try {
            log.info("Stage2 처리 시작");

            var stage1Result = loadStage1Data(threadId, tripDays);
            var dailyItineraries = processItineraries(stage1Result.getPlaces(), tripDays);
            var reviewedItineraries = applyLLMReviewIfNeeded(dailyItineraries, threadId);

            return createOutput(reviewedItineraries);

        } catch (Exception e) {
            log.error("Stage2 처리 실패", e);
            throw new RuntimeException("Stage2 처리 실패", e);
        } finally {
            MDC.clear();
        }
    }

    // Stage1 데이터 로드
    private Stage1Result loadStage1Data(String threadId, int tripDays) {
        log.debug("Stage1 데이터 로드 중");
        var result = mockDataGenerator.generateStage1Result(threadId, tripDays);
        log.info("Stage1 데이터 로드 완료: {}개 장소", result.getPlaces().size());
        return result;
    }

    // 일정 처리 로직
    private Map<Integer, DailyItinerary> processItineraries(List<TourPlace> places, int tripDays) {
        var clusters = performClustering(places, tripDays);
        var dayAssignment = assignClustersToDay(clusters, tripDays);
        return createItineraries(dayAssignment);
    }

    // 클러스터링 수행
    private List<RegionCluster> performClustering(List<TourPlace> places, int tripDays) {
        var optimalK = clusteringService.determineOptimalClusters(places, tripDays);
        var clusters = clusteringService.performKMeansClustering(places, optimalK);
        return clusteringService.convertToRegionClusters(clusters);
    }

    // 클러스터를 일자별로 할당
    private Map<Integer, List<RegionCluster>> assignClustersToDay(
            List<RegionCluster> regions, int tripDays) {

        var dayAssignment = initializeDayAssignment(tripDays);
        regions.sort((a, b) -> Integer.compare(b.getPlaceCount(), a.getPlaceCount()));

        for (var region : regions) {
            assignRegionToDay(dayAssignment, region, tripDays);
        }

        logDayAssignment(dayAssignment);
        return dayAssignment;
    }

    // 일자별 할당 초기화
    private Map<Integer, List<RegionCluster>> initializeDayAssignment(int tripDays) {
        var assignment = new HashMap<Integer, List<RegionCluster>>();
        for (int day = 1; day <= tripDays; day++) {
            assignment.put(day, new ArrayList<>());
        }
        return assignment;
    }

    // 지역을 특정 일자에 할당
    private void assignRegionToDay(Map<Integer, List<RegionCluster>> dayAssignment,
                                  RegionCluster region, int tripDays) {
        var placeCount = region.getPlaceCount();

        if (placeCount >= 20) {
            assignToNextDay(dayAssignment, region, tripDays);
        } else if (placeCount >= 10) {
            assignToOptimalDay(dayAssignment, region, tripDays);
        } else {
            assignToNearestRegion(dayAssignment, region);
        }
    }

    // 다음 날에 할당
    private void assignToNextDay(Map<Integer, List<RegionCluster>> dayAssignment,
                                RegionCluster region, int tripDays) {
        var currentDay = findLeastLoadedDay(dayAssignment, tripDays);
        dayAssignment.get(currentDay).add(region);
    }

    // 최적의 날 찾기 (가장 적은 장소 수)
    private int findLeastLoadedDay(Map<Integer, List<RegionCluster>> dayAssignment, int tripDays) {
        return dayAssignment.entrySet().stream()
            .min(Comparator.comparing(e -> countPlacesForDay(e.getValue())))
            .map(Map.Entry::getKey)
            .orElse(1);
    }

    // 일자의 총 장소 수 계산
    private int countPlacesForDay(List<RegionCluster> regions) {
        return regions.stream()
            .mapToInt(RegionCluster::getPlaceCount)
            .sum();
    }

    // 최적의 날에 할당
    private void assignToOptimalDay(Map<Integer, List<RegionCluster>> dayAssignment,
                                   RegionCluster region, int tripDays) {
        var optimalDay = findLeastLoadedDay(dayAssignment, tripDays);
        dayAssignment.get(optimalDay).add(region);
    }

    // 가장 가까운 지역에 할당
    private void assignToNearestRegion(Map<Integer, List<RegionCluster>> dayAssignment,
                                      RegionCluster region) {
        var nearestDay = findNearestRegionDay(dayAssignment, region);
        dayAssignment.get(nearestDay).add(region);
    }

    // 가장 가까운 지역이 있는 날 찾기
    private int findNearestRegionDay(Map<Integer, List<RegionCluster>> dayAssignment,
                                    RegionCluster region) {
        var bestDay = 1;
        var minDistance = Double.MAX_VALUE;

        for (var entry : dayAssignment.entrySet()) {
            for (var existingRegion : entry.getValue()) {
                var distance = calculateDistance(region, existingRegion);
                if (distance < minDistance) {
                    minDistance = distance;
                    bestDay = entry.getKey();
                }
            }
        }

        return minDistance < MAX_DISTANCE_THRESHOLD ? bestDay :
               findLeastLoadedDay(dayAssignment, dayAssignment.size());
    }

    // 두 지역 간 거리 계산
    private double calculateDistance(RegionCluster r1, RegionCluster r2) {
        return DistanceUtils.calculateDistance(
            r1.getCenter().getLat(), r1.getCenter().getLng(),
            r2.getCenter().getLat(), r2.getCenter().getLng()
        );
    }

    // 일별 일정 생성
    private Map<Integer, DailyItinerary> createItineraries(
            Map<Integer, List<RegionCluster>> dayAssignment) {

        var itineraries = new HashMap<Integer, DailyItinerary>();

        for (var entry : dayAssignment.entrySet()) {
            var itinerary = createDayItinerary(entry.getKey(), entry.getValue());
            itineraries.put(entry.getKey(), itinerary);
        }

        return itineraries;
    }

    // 하루 일정 생성
    private DailyItinerary createDayItinerary(int day, List<RegionCluster> regions) {
        var places = extractPlaces(regions);
        var selectedPlaces = selectOptimalPlaces(places);
        var regionNames = extractRegionNames(regions);
        var totalDistance = DistanceUtils.calculateTotalDistance(selectedPlaces);

        log.info("Day {} 일정: 지역={}, 장소={}개, 이동거리={}km",
            day, regionNames, selectedPlaces.size(), String.format("%.1f", totalDistance));

        return DailyItinerary.builder()
            .dayNumber(day)
            .date(LocalDate.now().plusDays(day - 1))
            .regions(regionNames)
            .places(selectedPlaces)
            .totalDistance(totalDistance)
            .build();
    }

    // 지역에서 장소 추출
    private List<TourPlace> extractPlaces(List<RegionCluster> regions) {
        return regions.stream()
            .flatMap(r -> r.getPlaces().stream())
            .collect(Collectors.toList());
    }

    // 지역명 추출
    private List<String> extractRegionNames(List<RegionCluster> regions) {
        return regions.stream()
            .map(RegionCluster::getRegionName)
            .collect(Collectors.toList());
    }

    // 최적 장소 선택 - 시간 블록별로
    private List<TourPlace> selectOptimalPlaces(List<TourPlace> candidates) {
        var timeBlocks = List.of(
            "BREAKFAST", "MORNING_ACTIVITY", "LUNCH", "CAFE",
            "AFTERNOON_ACTIVITY", "DINNER", "EVENING_ACTIVITY"
        );

        var selectedPlaces = new ArrayList<TourPlace>();

        for (var timeBlock : timeBlocks) {
            selectPlaceForTimeBlock(candidates, timeBlock, selectedPlaces);
        }

        return selectedPlaces;
    }

    // 특정 시간 블록의 장소 선택
    private void selectPlaceForTimeBlock(List<TourPlace> candidates, String timeBlock,
                                        List<TourPlace> selectedPlaces) {
        candidates.stream()
            .filter(p -> timeBlock.equals(p.getTimeBlock()))
            .max(Comparator.comparing(p -> calculatePlaceScore(p, selectedPlaces)))
            .ifPresent(selectedPlaces::add);
    }

    // 장소 점수 계산
    private double calculatePlaceScore(TourPlace place, List<TourPlace> currentSchedule) {
        if (currentSchedule.isEmpty()) {
            return place.rating() != null ? place.rating() : 0.0;
        }

        var lastPlace = currentSchedule.get(currentSchedule.size() - 1);
        var score = 0.0;

        // 거리 점수
        var distance = DistanceUtils.calculateDistance(lastPlace, place);
        score += (10.0 - Math.min(distance, 10.0));

        // 카테고리 다양성
        if (!place.category().equals(lastPlace.category())) {
            score += 3.0;
        }

        // 평점 점수
        if (place.rating() != null) {
            score += place.rating();
        }

        // 트렌디 보너스
        if (Boolean.TRUE.equals(place.isTrendy())) {
            score += 2.0;
        }

        return score;
    }

    // LLM 검수 적용 (필요시)
    private Map<Integer, DailyItinerary> applyLLMReviewIfNeeded(
            Map<Integer, DailyItinerary> itineraries, String threadId) {

        if (!needsLLMReview(itineraries)) {
            log.info("LLM 검수 불필요");
            return itineraries;
        }

        MDC.put("llmReview", "true");
        try {
            log.info("LLM 검수 시작");
            return requestLLMReview(itineraries, threadId);
        } finally {
            MDC.remove("llmReview");
        }
    }

    // LLM 검수 필요 여부 판단
    private boolean needsLLMReview(Map<Integer, DailyItinerary> itineraries) {
        return itineraries.values().stream()
            .anyMatch(this::checkItineraryProblems);
    }

    // 일정 문제 체크
    private boolean checkItineraryProblems(DailyItinerary itinerary) {
        // 이동거리 체크
        if (itinerary.totalDistance() > WARNING_DAILY_DISTANCE) {
            log.warn("Day {} 이동거리 초과: {}km", itinerary.dayNumber(), itinerary.totalDistance());
            return true;
        }

        // 필수 시간 블록 체크
        var timeBlocks = itinerary.places().stream()
            .map(TourPlace::getTimeBlock)
            .collect(Collectors.toSet());

        if (!timeBlocks.contains("LUNCH") || !timeBlocks.contains("DINNER")) {
            log.warn("Day {} 필수 식사 시간 누락", itinerary.dayNumber());
            return true;
        }

        // 장소 수 체크
        var placeCount = itinerary.places().size();
        if (placeCount < 4 || placeCount > 10) {
            log.warn("Day {} 장소 수 이상: {}개", itinerary.dayNumber(), placeCount);
            return true;
        }

        return false;
    }

    // LLM 검수 요청
    private Map<Integer, DailyItinerary> requestLLMReview(
            Map<Integer, DailyItinerary> itineraries, String threadId) {

        try {
            var context = loadOrCreateContext(threadId);
            var prompt = buildReviewPrompt(itineraries, context);
            var response = callLLM(prompt);
            return applyLLMSuggestions(itineraries, response);

        } catch (Exception e) {
            log.error("LLM 검수 실패, 원본 유지", e);
            return itineraries;
        }
    }

    // 컨텍스트 로드 또는 생성
    private TravelContext loadOrCreateContext(String threadId) {
        return contextManager.getContext(threadId, "mockUser")
            .orElseGet(() -> createMockContext(threadId));
    }

    // Mock 컨텍스트 생성
    private TravelContext createMockContext(String threadId) {
        var context = TravelContext.builder()
            .threadId(threadId)
            .userId("mockUser")
            .build();

        context.updateCollectedInfo(TravelContext.KEY_DESTINATIONS, List.of("서울"));
        context.updateCollectedInfo(TravelContext.KEY_TRAVEL_STYLE, List.of("편안한", "문화"));
        context.updateCollectedInfo(TravelContext.KEY_COMPANIONS, "친구");

        return context;
    }

    // LLM 검수 프롬프트 생성
    @SuppressWarnings("unchecked")
    private String buildReviewPrompt(Map<Integer, DailyItinerary> itineraries,
                                    TravelContext context) {

        var destinations = (List<String>) context.getCollectedInfo()
            .getOrDefault(TravelContext.KEY_DESTINATIONS, List.of("서울"));
        var travelStyle = (List<String>) context.getCollectedInfo()
            .getOrDefault(TravelContext.KEY_TRAVEL_STYLE, List.of("편안한"));

        return String.format("""
            여행 일정 검토 요청

            [여행 정보]
            - 목적지: %s
            - 기간: %d일
            - 스타일: %s

            [생성된 일정]
            %s

            [체크 포인트]
            1. 하루 이동거리 20km 이내
            2. 점심/저녁 식사 시간 포함
            3. 카테고리 다양성
            4. 자연스러운 동선

            [응답 형식]
            {
              "needsAdjustment": true/false,
              "reason": "이유",
              "suggestions": []
            }
            """,
            destinations, itineraries.size(), travelStyle,
            formatItineraries(itineraries));
    }

    // 일정 포맷팅
    private String formatItineraries(Map<Integer, DailyItinerary> itineraries) {
        return itineraries.entrySet().stream()
            .map(this::formatDayItinerary)
            .collect(Collectors.joining("\n"));
    }

    // 하루 일정 포맷팅
    private String formatDayItinerary(Map.Entry<Integer, DailyItinerary> entry) {
        var it = entry.getValue();
        var places = it.places().stream()
            .map(p -> String.format("- %s [%s] %s (%s)",
                p.recommendTime(), p.timeBlock(), p.name(), p.category()))
            .collect(Collectors.joining("\n"));

        return String.format("Day %d (%.1fkm):\n%s",
            it.dayNumber(), it.totalDistance(), places);
    }

    // LLM 호출
    private LLMReviewResponse callLLM(String prompt) {
        var response = chatModel.call(new Prompt(prompt));
        var content = response.getResult().getOutput().getContent();
        return parseLLMResponse(content);
    }

    // LLM 응답 파싱
    private LLMReviewResponse parseLLMResponse(String content) {
        try {
            var jsonStart = content.indexOf("{");
            var jsonEnd = content.lastIndexOf("}") + 1;
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                var json = content.substring(jsonStart, jsonEnd);
                return objectMapper.readValue(json, LLMReviewResponse.class);
            }
        } catch (Exception e) {
            log.warn("LLM 응답 파싱 실패", e);
        }

        return LLMReviewResponse.builder()
            .needsAdjustment(false)
            .build();
    }

    // LLM 제안 적용
    private Map<Integer, DailyItinerary> applyLLMSuggestions(
            Map<Integer, DailyItinerary> itineraries,
            LLMReviewResponse response) {

        if (!response.isNeedsAdjustment()) {
            return itineraries;
        }

        log.info("LLM 제안 적용: {}", response.getReason());

        var updatedItineraries = new HashMap<>(itineraries);
        for (var suggestion : response.getSuggestions()) {
            applySingleSuggestion(updatedItineraries, suggestion);
        }

        recalculateDistances(updatedItineraries);
        return updatedItineraries;
    }

    // 개별 제안 적용
    private void applySingleSuggestion(Map<Integer, DailyItinerary> itineraries,
                                      LLMReviewResponse.LLMSuggestion suggestion) {
        switch (suggestion.getType()) {
            case "MOVE" -> movePlaceBetweenDays(itineraries, suggestion);
            case "REMOVE" -> removePlace(itineraries, suggestion);
            case "SWAP" -> swapPlaces(itineraries, suggestion);
        }
    }

    // 장소를 다른 날로 이동
    private void movePlaceBetweenDays(Map<Integer, DailyItinerary> itineraries,
                                     LLMReviewResponse.LLMSuggestion suggestion) {
        var sourceDay = itineraries.get(suggestion.getDay());
        var targetDay = itineraries.get(suggestion.getTargetDay());

        if (sourceDay != null && targetDay != null) {
            sourceDay.places().stream()
                .filter(p -> p.name().equals(suggestion.getPlace()))
                .findFirst()
                .ifPresent(place -> {
                    var newSourcePlaces = new ArrayList<>(sourceDay.places());
                    newSourcePlaces.remove(place);
                    var newTargetPlaces = new ArrayList<>(targetDay.places());
                    newTargetPlaces.add(place);

                    itineraries.put(sourceDay.dayNumber(), sourceDay.withPlaces(newSourcePlaces));
                    itineraries.put(targetDay.dayNumber(), targetDay.withPlaces(newTargetPlaces));
                });
        }
    }

    // 장소 제거
    private void removePlace(Map<Integer, DailyItinerary> itineraries,
                           LLMReviewResponse.LLMSuggestion suggestion) {
        var itinerary = itineraries.get(suggestion.getDay());
        if (itinerary != null) {
            var newPlaces = itinerary.places().stream()
                .filter(p -> !p.name().equals(suggestion.getPlace()))
                .collect(Collectors.toList());
            itineraries.put(itinerary.dayNumber(), itinerary.withPlaces(newPlaces));
        }
    }

    // 장소 순서 변경
    private void swapPlaces(Map<Integer, DailyItinerary> itineraries,
                          LLMReviewResponse.LLMSuggestion suggestion) {
        var itinerary = itineraries.get(suggestion.getDay());
        if (itinerary != null) {
            var places = new ArrayList<>(itinerary.places());
            var idx1 = findPlaceIndex(places, suggestion.getPlace());
            var idx2 = findPlaceIndex(places, suggestion.getSwapWith());

            if (idx1 >= 0 && idx2 >= 0) {
                Collections.swap(places, idx1, idx2);
                itineraries.put(itinerary.dayNumber(), itinerary.withPlaces(places));
            }
        }
    }

    // 장소 인덱스 찾기
    private int findPlaceIndex(List<TourPlace> places, String name) {
        for (int i = 0; i < places.size(); i++) {
            if (places.get(i).name().equals(name)) {
                return i;
            }
        }
        return -1;
    }

    // 거리 재계산
    private void recalculateDistances(Map<Integer, DailyItinerary> itineraries) {
        itineraries.replaceAll((day, itinerary) -> {
            var newDistance = DistanceUtils.calculateTotalDistance(itinerary.places());
            return itinerary.withTotalDistance(newDistance);
        });
    }

    // 최종 출력 생성
    private Stage2Output createOutput(Map<Integer, DailyItinerary> itineraries) {
        var summary = createSummary(itineraries);
        return Stage2Output.builder()
            .dailyItineraries(itineraries)
            .summary(summary)
            .build();
    }

    // 요약 생성
    private TravelSummary createSummary(Map<Integer, DailyItinerary> itineraries) {
        var totalPlaces = itineraries.values().stream()
            .mapToInt(d -> d.places().size())
            .sum();

        var totalDistance = itineraries.values().stream()
            .mapToDouble(DailyItinerary::totalDistance)
            .sum();

        var allRegions = itineraries.values().stream()
            .flatMap(d -> d.regions().stream())
            .collect(Collectors.toSet());

        var categoryDistribution = calculateCategoryDistribution(itineraries);

        return TravelSummary.builder()
            .totalDays(itineraries.size())
            .totalPlaces(totalPlaces)
            .totalRegions(allRegions.size())
            .averageRegionsPerDay(allRegions.size() / (double) itineraries.size())
            .categoryDistribution(categoryDistribution)
            .estimatedTotalDistance(totalDistance)
            .llmReviewApplied(false)
            .build();
    }

    // 카테고리별 분포 계산
    private Map<String, Integer> calculateCategoryDistribution(Map<Integer, DailyItinerary> itineraries) {
        return itineraries.values().stream()
            .flatMap(d -> d.places().stream())
            .collect(Collectors.groupingBy(
                TourPlace::category,
                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
            ));
    }

    // 일자별 할당 로깅
    private void logDayAssignment(Map<Integer, List<RegionCluster>> dayAssignment) {
        dayAssignment.forEach((day, regions) -> {
            var regionNames = regions.stream()
                .map(RegionCluster::getRegionName)
                .collect(Collectors.toList());
            var totalPlaces = countPlacesForDay(regions);
            log.info("Day {} 할당: {} ({}개 장소)", day, regionNames, totalPlaces);
        });
    }
}