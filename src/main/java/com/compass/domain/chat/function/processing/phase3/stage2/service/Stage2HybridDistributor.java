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
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

// Stage2 하이브리드 분산 처리 서비스 - 알고리즘 기반 클러스터링 + LLM 검수
@Slf4j
@Service
@RequiredArgsConstructor
public class Stage2HybridDistributor {

    private final MockDataGenerator mockDataGenerator;
    private final KMeansClusteringService clusteringService;
    private final ChatModel chatModel;  // Spring AI ChatModel
    private final ContextManager contextManager;
    private final ObjectMapper objectMapper;

    // 최대 이동 거리 임계값 (km)
    private static final double MAX_DISTANCE_THRESHOLD = 5.0;  // 5km 이내는 같은 지역
    private static final double COMFORTABLE_DAILY_DISTANCE = 15.0;  // 하루 적정 이동거리
    private static final double WARNING_DAILY_DISTANCE = 20.0;  // LLM 검수 필요

    // Stage2 메인 처리 메서드
    public Stage2Output processDistribution(String threadId, int tripDays) {
        log.info("Stage2 처리 시작: threadId={}, tripDays={}", threadId, tripDays);

        try {
            // 1. Stage 1 결과 조회 (Mock 데이터 사용)
            Stage1Result stage1Result = mockDataGenerator.generateStage1Result(threadId, tripDays);
            List<TourPlace> allPlaces = stage1Result.getPlaces();
            log.info("Stage1 결과 로드 완료: {}개 장소", allPlaces.size());

            // 2. 알고리즘으로 클러스터링 및 일정 생성
            Map<Integer, DailyItinerary> dailyItineraries =
                performAlgorithmicDistribution(allPlaces, tripDays);
            log.info("알고리즘 클러스터링 완료");

            // 3. LLM 검수 (필요시에만)
            if (needsLLMReview(dailyItineraries)) {
                log.info("LLM 검수 필요 - 일정 검토 요청");
                dailyItineraries = requestLLMReview(dailyItineraries, threadId);
            } else {
                log.info("알고리즘 결과 양호 - LLM 검수 생략");
            }

            // 4. 최종 출력 생성
            return buildStage2Output(dailyItineraries);

        } catch (Exception e) {
            log.error("Stage2 처리 중 오류 발생", e);
            throw new RuntimeException("Stage2 처리 실패", e);
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
                .filter(p -> timeBlock.equals(p.getTimeBlock()))
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
                .max(Comparator.comparing(p -> p.getRating() != null ? p.getRating() : 0.0))
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
                if (!place.getCategory().equals(lastPlace.getCategory())) {
                    score += 3.0;
                }

                // 평점 점수
                if (place.getRating() != null) {
                    score += place.getRating();
                }

                // 트렌디한 장소 보너스
                if (Boolean.TRUE.equals(place.getIsTrendy())) {
                    score += 2.0;
                }

                return score;
            }))
            .orElse(candidates.get(0));
    }

    // LLM 검수가 필요한지 판단
    private boolean needsLLMReview(Map<Integer, DailyItinerary> itineraries) {

        for (DailyItinerary itinerary : itineraries.values()) {
            // 1. 하루 이동거리가 너무 긴 경우
            if (itinerary.getTotalDistance() > WARNING_DAILY_DISTANCE) {
                log.warn("Day {} 이동거리 {}km - LLM 검수 필요",
                    itinerary.getDayNumber(), itinerary.getTotalDistance());
                return true;
            }

            // 2. 필수 시간 블록이 누락된 경우
            Set<String> timeBlocks = itinerary.getPlaces().stream()
                .map(TourPlace::getTimeBlock)
                .collect(Collectors.toSet());

            if (!timeBlocks.contains("LUNCH") || !timeBlocks.contains("DINNER")) {
                log.warn("Day {} 식사 시간 누락 - LLM 검수 필요",
                    itinerary.getDayNumber());
                return true;
            }

            // 3. 장소가 너무 적거나 많은 경우
            int placeCount = itinerary.getPlaces().size();
            if (placeCount < 4 || placeCount > 10) {
                log.warn("Day {} 장소 수 {} - LLM 검수 필요",
                    itinerary.getDayNumber(), placeCount);
                return true;
            }
        }

        return false;
    }

    // LLM 검수 요청 및 적용
    private Map<Integer, DailyItinerary> requestLLMReview(
            Map<Integer, DailyItinerary> itineraries,
            String threadId) {

        try {
            // TravelContext에서 여행 정보 조회 (Mock userId 사용)
            Optional<TravelContext> contextOpt = contextManager.getContext(threadId, "mockUser");
            TravelContext context = contextOpt.orElseGet(() -> createMockContext(threadId));

            // LLM 검수 프롬프트 생성
            String prompt = buildLLMReviewPrompt(itineraries, context);

            // Spring AI ChatModel 호출
            ChatResponse response = chatModel.call(new Prompt(prompt));
            String responseContent = response.getResult().getOutput().getContent();
            log.debug("LLM 응답: {}", responseContent);

            // JSON 파싱
            LLMReviewResponse reviewResponse = parseLLMResponse(responseContent);

            // 조정이 필요한 경우 적용
            if (reviewResponse.isNeedsAdjustment()) {
                log.info("LLM 제안 적용: {}", reviewResponse.getReason());
                return applyLLMAdjustments(itineraries, reviewResponse);
            }

            return itineraries;

        } catch (Exception e) {
            log.error("LLM 검수 중 오류 발생, 원본 일정 유지", e);
            return itineraries;
        }
    }

    // LLM 응답 파싱
    private LLMReviewResponse parseLLMResponse(String responseContent) {
        try {
            // JSON 부분만 추출
            int startIdx = responseContent.indexOf("{");
            int endIdx = responseContent.lastIndexOf("}") + 1;
            if (startIdx >= 0 && endIdx > startIdx) {
                String jsonPart = responseContent.substring(startIdx, endIdx);
                return objectMapper.readValue(jsonPart, LLMReviewResponse.class);
            }
        } catch (Exception e) {
            log.warn("LLM 응답 파싱 실패: {}", e.getMessage());
        }

        // 파싱 실패시 조정 불필요로 반환
        return LLMReviewResponse.builder()
            .needsAdjustment(false)
            .reason("파싱 실패")
            .suggestions(new ArrayList<>())
            .build();
    }

    // Mock 컨텍스트 생성
    private TravelContext createMockContext(String threadId) {
        TravelContext context = TravelContext.builder()
            .threadId(threadId)
            .userId("mockUser")
            .build();

        // Mock 여행 정보 설정
        context.updateCollectedInfo(TravelContext.KEY_DESTINATIONS, List.of("서울"));
        context.updateCollectedInfo(TravelContext.KEY_TRAVEL_STYLE, List.of("편안한", "문화"));
        context.updateCollectedInfo(TravelContext.KEY_COMPANIONS, "친구");

        return context;
    }

    // LLM 검수 프롬프트 생성
    @SuppressWarnings("unchecked")
    private String buildLLMReviewPrompt(Map<Integer, DailyItinerary> itineraries,
                                       TravelContext context) {

        StringBuilder prompt = new StringBuilder();
        prompt.append("여행 일정을 검토하고 문제가 있으면 조정 제안을 해주세요.\n\n");

        prompt.append("[여행 정보]\n");

        // collectedInfo에서 목적지 정보 가져오기
        List<String> destinations = (List<String>) context.getCollectedInfo()
            .getOrDefault(TravelContext.KEY_DESTINATIONS, List.of("서울"));
        prompt.append("- 목적지: ").append(destinations).append("\n");

        prompt.append("- 기간: ").append(itineraries.size()).append("일\n");

        // collectedInfo에서 여행 스타일 정보 가져오기
        List<String> travelStyle = (List<String>) context.getCollectedInfo()
            .getOrDefault(TravelContext.KEY_TRAVEL_STYLE, List.of("편안한"));
        prompt.append("- 여행 스타일: ").append(travelStyle).append("\n\n");

        prompt.append("[생성된 일정]\n");
        prompt.append(formatItinerariesForLLM(itineraries)).append("\n\n");

        prompt.append("[체크 포인트]\n");
        prompt.append("1. 하루 이동거리가 20km를 넘지 않는가?\n");
        prompt.append("2. 점심(12-14시)/저녁(18-20시) 시간에 식당이 있는가?\n");
        prompt.append("3. 같은 카테고리가 연속으로 나오지 않는가?\n");
        prompt.append("4. 실제 동선이 자연스러운가?\n");
        prompt.append("5. 지역 간 이동이 효율적인가?\n\n");

        prompt.append("[응답 형식]\n");
        prompt.append("{\n");
        prompt.append("  \"needsAdjustment\": true/false,\n");
        prompt.append("  \"reason\": \"조정이 필요한 이유\",\n");
        prompt.append("  \"suggestions\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"day\": 1,\n");
        prompt.append("      \"type\": \"MOVE/REMOVE/SWAP\",\n");
        prompt.append("      \"place\": \"장소명\",\n");
        prompt.append("      \"action\": \"구체적인 조정 내용\"\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n");

        return prompt.toString();
    }

    // 일정을 LLM용 텍스트로 포맷팅
    private String formatItinerariesForLLM(Map<Integer, DailyItinerary> itineraries) {
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<Integer, DailyItinerary> entry : itineraries.entrySet()) {
            DailyItinerary itinerary = entry.getValue();
            sb.append("\nDay ").append(itinerary.getDayNumber())
              .append(" (이동거리: ").append(String.format("%.1f", itinerary.getTotalDistance()))
              .append("km):\n");

            for (TourPlace place : itinerary.getPlaces()) {
                sb.append("- ").append(place.getRecommendTime())
                  .append(" [").append(place.getTimeBlock()).append("] ")
                  .append(place.getName())
                  .append(" (").append(place.getCategory()).append(")\n");
            }
        }

        return sb.toString();
    }

    // LLM 제안 적용
    private Map<Integer, DailyItinerary> applyLLMAdjustments(
            Map<Integer, DailyItinerary> itineraries,
            LLMReviewResponse response) {

        for (LLMReviewResponse.LLMSuggestion suggestion : response.getSuggestions()) {
            DailyItinerary itinerary = itineraries.get(suggestion.getDay());
            if (itinerary == null) continue;

            switch (suggestion.getType()) {
                case "MOVE":
                    movePlace(itineraries, suggestion);
                    break;
                case "REMOVE":
                    removePlace(itinerary, suggestion.getPlace());
                    break;
                case "SWAP":
                    swapPlaces(itinerary, suggestion);
                    break;
            }
        }

        // 조정 후 거리 재계산
        var updatedItineraries = new HashMap<Integer, DailyItinerary>();
        for (var entry : itineraries.entrySet()) {
            var itinerary = entry.getValue();
            double newDistance = DistanceUtils.calculateTotalDistance(itinerary.getPlaces());
            var updatedItinerary = itinerary.withTotalDistance(newDistance);
            updatedItineraries.put(entry.getKey(), updatedItinerary);
            log.info("Day {} 조정 완료: 새 이동거리 {}km",
                updatedItinerary.getDayNumber(), String.format("%.1f", newDistance));
        }
        itineraries.putAll(updatedItineraries);

        return itineraries;
    }

    // 장소 이동
    private void movePlace(Map<Integer, DailyItinerary> itineraries,
                          LLMReviewResponse.LLMSuggestion suggestion) {

        DailyItinerary sourceDay = itineraries.get(suggestion.getDay());
        DailyItinerary targetDay = itineraries.get(suggestion.getTargetDay());

        if (sourceDay != null && targetDay != null) {
            TourPlace placeToMove = sourceDay.getPlaces().stream()
                .filter(p -> p.getName().equals(suggestion.getPlace()))
                .findFirst()
                .orElse(null);

            if (placeToMove != null) {
                sourceDay.getPlaces().remove(placeToMove);
                targetDay.getPlaces().add(placeToMove);
                log.info("장소 이동: {} (Day {} → Day {})",
                    placeToMove.getName(), suggestion.getDay(), suggestion.getTargetDay());
            }
        }
    }

    // 장소 제거
    private void removePlace(DailyItinerary itinerary, String placeName) {
        itinerary.getPlaces().removeIf(p -> p.getName().equals(placeName));
        log.info("장소 제거: {}", placeName);
    }

    // 장소 순서 변경
    private void swapPlaces(DailyItinerary itinerary,
                           LLMReviewResponse.LLMSuggestion suggestion) {

        List<TourPlace> places = itinerary.getPlaces();
        int idx1 = -1, idx2 = -1;

        for (int i = 0; i < places.size(); i++) {
            if (places.get(i).getName().equals(suggestion.getPlace())) idx1 = i;
            if (places.get(i).getName().equals(suggestion.getSwapWith())) idx2 = i;
        }

        if (idx1 >= 0 && idx2 >= 0) {
            Collections.swap(places, idx1, idx2);
            log.info("장소 순서 변경: {} <-> {}",
                suggestion.getPlace(), suggestion.getSwapWith());
        }
    }

    // 최종 Stage2 출력 생성
    private Stage2Output buildStage2Output(Map<Integer, DailyItinerary> dailyItineraries) {
        TravelSummary summary = createSummary(dailyItineraries);

        return Stage2Output.builder()
            .dailyItineraries(dailyItineraries)
            .summary(summary)
            .build();
    }

    // 요약 정보 생성
    private TravelSummary createSummary(Map<Integer, DailyItinerary> dailyItineraries) {
        int totalPlaces = dailyItineraries.values().stream()
            .mapToInt(d -> d.getPlaces().size())
            .sum();

        double totalDistance = dailyItineraries.values().stream()
            .mapToDouble(DailyItinerary::getTotalDistance)
            .sum();

        Set<String> allRegions = dailyItineraries.values().stream()
            .flatMap(d -> d.getRegions().stream())
            .collect(Collectors.toSet());

        Map<String, Integer> categoryDistribution = dailyItineraries.values().stream()
            .flatMap(d -> d.getPlaces().stream())
            .collect(Collectors.groupingBy(
                TourPlace::getCategory,
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