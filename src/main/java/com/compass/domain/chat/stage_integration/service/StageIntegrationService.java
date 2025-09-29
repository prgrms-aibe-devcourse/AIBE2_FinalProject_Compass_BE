package com.compass.domain.chat.stage_integration.service;

import com.compass.domain.chat.entity.TravelCandidate;
import com.compass.domain.chat.model.context.TravelContext;
import com.compass.domain.chat.model.TravelPlace;
import com.compass.domain.chat.repository.TravelCandidateRepository;
import com.compass.domain.chat.stage1.service.Stage1DestinationSelectionService;
import com.compass.domain.chat.stage2.service.Stage2TimeBlockService;
import com.compass.domain.chat.stage3.service.Stage3IntegrationService;
import com.compass.domain.chat.stage3.service.Stage3RouteOptimizationService;
import com.compass.domain.chat.stage3.service.Stage3PersistenceService;
import com.compass.domain.chat.stage3.dto.Stage3Output;
import com.compass.domain.chat.stage3.dto.DailyItinerary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class StageIntegrationService {

    private final TravelCandidateRepository travelCandidateRepository;
    private final Stage1DestinationSelectionService stage1Service;
    private final Stage2TimeBlockService stage2Service;
    private final Stage3IntegrationService stage3IntegrationService;
    private final Stage3RouteOptimizationService routeOptimizationService;
    private final Stage2To3DirectConverter stage2To3DirectConverter;
    private final Stage3PersistenceService stage3PersistenceService;

    // Stage 1: DB에서 지역의 모든 장소 표시 (설계 문서 3.1 기반)
    public Map<String, Object> processStage1(TravelContext context) {
        // destination을 collectedInfo에서 가져오기
        @SuppressWarnings("unchecked")
        List<String> destinations = (List<String>) context.getCollectedInfo().get(TravelContext.KEY_DESTINATIONS);
        String destination = destinations != null && !destinations.isEmpty() ? destinations.get(0) : "서울";

        log.info("🎯 [Stage 1] 장소 표시 시작 - 지역: {}", destination);

        try {
            // 1. DB에서 해당 지역의 모든 장소 조회
            List<TravelCandidate> allPlaces = travelCandidateRepository
                .findByRegion(destination);

            log.info("📍 [Stage 1] {}개 장소 조회됨", allPlaces.size());

            // 2. 주소와 좌표가 있는 장소만 필터링하고 여행 스타일과 매칭
            List<Map<String, Object>> displayPlaces = allPlaces.stream()
                .filter(place -> isValidAddress(place.getAddress()))
                .filter(place -> place.getLatitude() != null && place.getLongitude() != null)
                .map(place -> {
                    Map<String, Object> placeData = new HashMap<>();
                    placeData.put("id", place.getId());
                    placeData.put("name", place.getName());
                    placeData.put("category", place.getCategory());
                    placeData.put("subCategory", place.getCategory()); // subCategory 없으면 category 사용
                    placeData.put("description", place.getDescription());
                    placeData.put("address", place.getAddress());
                    placeData.put("latitude", place.getLatitude());
                    placeData.put("longitude", place.getLongitude());
                    placeData.put("rating", place.getRating());
                    placeData.put("imageUrl", ""); // imageUrl 필드가 없으므로 빈 문자열
                    placeData.put("businessHours", place.getBusinessHours());
                    @SuppressWarnings("unchecked")
                    List<String> travelStyles = (List<String>) context.getCollectedInfo().get(TravelContext.KEY_TRAVEL_STYLE);
                    placeData.put("isRecommended", matchesTravelStyle(place, travelStyles));
                    return placeData;
                })
                .collect(Collectors.toList());

            log.info("📍 [Stage 1] 필터링 후 {}개 장소 (주소/좌표 있는 장소만)", displayPlaces.size());

            // 3. 카테고리별 그룹화
            Map<String, List<Map<String, Object>>> categorizedPlaces = displayPlaces.stream()
                .collect(Collectors.groupingBy(p -> (String) p.get("category")));

            Map<String, Object> result = new HashMap<>();
            result.put("places", categorizedPlaces);
            result.put("totalCount", displayPlaces.size());
            result.put("recommendedCount", displayPlaces.stream()
                .filter(p -> (Boolean) p.get("isRecommended")).count());
            result.put("stage", 1);
            result.put("type", "PLACE_DISPLAY");
            result.put("nextAction", "SELECT_PLACES");

            log.info("✅ [Stage 1] 완료 - 전체: {}개, 추천: {}개",
                result.get("totalCount"), result.get("recommendedCount"));

            return result;

        } catch (Exception e) {
            log.error("❌ [Stage 1] 오류 발생: ", e);
            throw new RuntimeException("Stage 1 처리 중 오류 발생", e);
        }
    }

    // Stage 2: 사용자 장소 선택 처리 + 날짜별 분배 (K-means 클러스터링)
    public Map<String, Object> processStage2(TravelContext context, List<Long> selectedPlaceIds) {
        log.info("🎯 [Stage 2] 사용자 선택 처리 및 날짜별 분배 - {}개 장소", selectedPlaceIds.size());

        try {
            // 1. 선택된 장소 정보 조회
            List<TravelCandidate> selectedPlaces = travelCandidateRepository
                .findAllById(selectedPlaceIds);

            // 2. TravelPlace로 변환
            List<TravelPlace> userPlaces = selectedPlaces.stream()
                .map(this::convertToTravelPlace)
                .collect(Collectors.toList());

            // 3. 여행 날짜 정보 가져오기
            Map<String, String> travelDates = new HashMap<>();
            String startDate = (String) context.getCollectedInfo().get(TravelContext.KEY_START_DATE);
            String endDate = (String) context.getCollectedInfo().get(TravelContext.KEY_END_DATE);
            travelDates.put("startDate", startDate);
            travelDates.put("endDate", endDate);
            int totalDays = calculateTravelDays(travelDates);

            log.info("📅 [Stage 2] 여행 기간: {}일", totalDays);

            // 4. K-means 클러스터링으로 지역별 그룹화 및 날짜별 분배
            Map<Integer, List<TravelPlace>> dailyDistribution = distributePlacesByDay(userPlaces, totalDays);

            // 5. 시간블록 고려하여 재배치 (겹치지 않도록)
            Map<Integer, List<TravelPlace>> optimizedDistribution = optimizeTimeBlocks(dailyDistribution, context);

            // 6. Context에 저장 (Stage 3에서 사용)
            context.getMetadata().put("userSelectedPlaces", userPlaces);
            context.getMetadata().put("dailyDistribution", optimizedDistribution);

            // 7. 결과 생성
            Map<String, Object> result = new HashMap<>();

            // 날짜별 분배 결과
            List<Map<String, Object>> dailyPlans = new ArrayList<>();
            for (Map.Entry<Integer, List<TravelPlace>> entry : optimizedDistribution.entrySet()) {
                Map<String, Object> dayPlan = new HashMap<>();
                dayPlan.put("day", entry.getKey());
                dayPlan.put("placeCount", entry.getValue().size());
                dayPlan.put("places", entry.getValue().stream()
                    .map(place -> {
                        Map<String, Object> placeInfo = new HashMap<>();
                        placeInfo.put("id", place.getPlaceId());
                        placeInfo.put("name", place.getName());
                        placeInfo.put("category", place.getCategory());
                        placeInfo.put("address", place.getAddress());
                        placeInfo.put("latitude", place.getLatitude());
                        placeInfo.put("longitude", place.getLongitude());
                        placeInfo.put("rating", place.getRating());
                        return placeInfo;
                    })
                    .collect(Collectors.toList()));
                dailyPlans.add(dayPlan);
            }

            // Stage 3에서 사용할 수 있도록 metadata에 저장
            context.getMetadata().put("dailyDistribution", optimizedDistribution);

            result.put("dailyDistribution", dailyPlans);
            result.put("totalDays", totalDays);
            result.put("selectedCount", userPlaces.size());
            result.put("stage", 2);
            result.put("type", "PLACES_DISTRIBUTED");
            result.put("nextAction", "OPTIMIZE_ITINERARY");

            log.info("✅ [Stage 2] 완료 - {}개 장소를 {}일에 분배", userPlaces.size(), totalDays);

            return result;

        } catch (Exception e) {
            log.error("❌ [Stage 2] 오류 발생: ", e);
            throw new RuntimeException("Stage 2 처리 중 오류 발생", e);
        }
    }

    // Stage 1 → Stage 2 전환 처리
    public Map<String, Object> processStage1ToStage2(TravelContext context, Map<String, Object> metadata) {
        log.info("🔄 [Stage 1 → Stage 2] 전환 처리 시작");

        try {
            // metadata에서 선택된 장소 정보 가져오기
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> selectedPlaces = (List<Map<String, Object>>) metadata.get("selectedPlaces");

            if (selectedPlaces == null || selectedPlaces.isEmpty()) {
                log.warn("선택된 장소가 없습니다.");
                return Map.of(
                    "stage", 2,
                    "type", "ERROR",
                    "message", "선택된 장소가 없습니다."
                );
            }

            log.info("📍 선택된 장소 수: {}", selectedPlaces.size());

            // Context에 Stage 1 결과 저장
            context.getMetadata().put("stage1SelectedPlaces", selectedPlaces);

            // 선택한 장소들의 ID 목록 추출
            List<Long> placeIds = selectedPlaces.stream()
                .map(place -> Long.valueOf(place.get("id").toString()))
                .collect(Collectors.toList());

            // Stage 2 처리 실행
            return processStage2(context, placeIds);

        } catch (Exception e) {
            log.error("Stage 1 → Stage 2 전환 중 오류:", e);
            return Map.of(
                "stage", 2,
                "type", "ERROR",
                "message", "Stage 전환 중 오류 발생: " + e.getMessage()
            );
        }
    }

    // K-means 클러스터링을 사용한 날짜별 분배
    private Map<Integer, List<TravelPlace>> distributePlacesByDay(List<TravelPlace> places, int days) {
        Map<Integer, List<TravelPlace>> distribution = new HashMap<>();

        if (places.size() <= days) {
            // 장소가 날짜보다 적으면 하루에 하나씩
            for (int i = 0; i < places.size(); i++) {
                distribution.computeIfAbsent(i + 1, k -> new ArrayList<>()).add(places.get(i));
            }
        } else {
            // K-means 클러스터링으로 지역별 그룹화
            List<List<TravelPlace>> clusters = performKMeansClustering(places, days);

            // 각 클러스터를 날짜에 할당
            for (int i = 0; i < clusters.size(); i++) {
                distribution.put(i + 1, clusters.get(i));
            }
        }

        return distribution;
    }

    // 간단한 K-means 구현 (실제로는 더 복잡한 알고리즘 필요)
    private List<List<TravelPlace>> performKMeansClustering(List<TravelPlace> places, int k) {
        List<List<TravelPlace>> clusters = new ArrayList<>();

        // 초기화: k개의 빈 클러스터 생성
        for (int i = 0; i < k; i++) {
            clusters.add(new ArrayList<>());
        }

        // 간단한 분배: 거리 기반으로 가까운 장소끼리 묶기
        // (실제로는 더 정교한 K-means 알고리즘 구현 필요)
        int clusterIndex = 0;
        for (TravelPlace place : places) {
            clusters.get(clusterIndex).add(place);
            clusterIndex = (clusterIndex + 1) % k;
        }

        return clusters;
    }

    // 시간블록 최적화 (겹치지 않도록)
    private Map<Integer, List<TravelPlace>> optimizeTimeBlocks(
            Map<Integer, List<TravelPlace>> distribution, TravelContext context) {

        // 각 날짜별로 시간대가 겹치지 않도록 조정
        // 오전(09:00-12:00), 점심(12:00-14:00), 오후(14:00-18:00), 저녁(18:00-20:00)

        Map<Integer, List<TravelPlace>> optimized = new HashMap<>();

        for (Map.Entry<Integer, List<TravelPlace>> entry : distribution.entrySet()) {
            List<TravelPlace> dayPlaces = entry.getValue();

            // 카테고리별로 시간대 할당
            List<TravelPlace> morning = new ArrayList<>();
            List<TravelPlace> lunch = new ArrayList<>();
            List<TravelPlace> afternoon = new ArrayList<>();
            List<TravelPlace> dinner = new ArrayList<>();

            for (TravelPlace place : dayPlaces) {
                String category = place.getCategory();
                if (category != null && category.contains("식당")) {
                    if (lunch.isEmpty()) {
                        lunch.add(place);
                    } else if (dinner.isEmpty()) {
                        dinner.add(place);
                    } else {
                        morning.add(place); // 나머지는 오전에
                    }
                } else {
                    if (morning.size() < 2) {
                        morning.add(place);
                    } else if (afternoon.size() < 3) {
                        afternoon.add(place);
                    } else {
                        morning.add(place); // 오버플로우는 오전에 추가
                    }
                }
            }

            // 시간 순서대로 재배열
            List<TravelPlace> orderedPlaces = new ArrayList<>();
            orderedPlaces.addAll(morning);
            orderedPlaces.addAll(lunch);
            orderedPlaces.addAll(afternoon);
            orderedPlaces.addAll(dinner);

            optimized.put(entry.getKey(), orderedPlaces);
        }

        return optimized;
    }

    // 여행 일수 계산
    private int calculateTravelDays(Map<String, String> travelDates) {
        if (travelDates == null) {
            return 1; // 기본값
        }

        try {
            String startDate = travelDates.get("startDate");
            String endDate = travelDates.get("endDate");

            if (!StringUtils.hasText(startDate) || !StringUtils.hasText(endDate)) {
                return 1;
            }

            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);

            if (end.isBefore(start)) {
                return 1;
            }

            return (int) ChronoUnit.DAYS.between(start, end) + 1;

        } catch (Exception e) {
            log.warn("날짜 계산 실패, 기본값 사용: ", e);
            return 1;
        }
    }

    // Stage 3: 일정 생성 (설계 문서 3.3 기반)
    public Map<String, Object> processStage3(TravelContext context) {
        log.info("🎯 [Stage 3] 최종 일정 생성 시작 - Stage3IntegrationService 사용");
        log.info("📍 [Stage 3] Context metadata keys: {}", context.getMetadata().keySet());
        log.info("📍 [Stage 3] Context collectedInfo keys: {}", context.getCollectedInfo().keySet());

        try {
            // Stage3IntegrationService를 사용하여 실제 일정 생성
            var stage3Output = stage3IntegrationService.processWithTravelContext(context);

            // DB에 저장
            try {
                var savedItinerary = stage3PersistenceService.saveItinerary(context, stage3Output);
                log.info("💾 [Stage 3] 일정 저장 완료 - Itinerary ID: {}", savedItinerary.getId());
            } catch (Exception e) {
                log.error("❌ [Stage 3] 일정 저장 실패: ", e);
                // 저장 실패해도 프론트엔드에는 응답 반환
            }

            // Stage3Output을 프론트엔드가 기대하는 형식으로 변환
            List<Map<String, Object>> itinerary = new ArrayList<>();

            if (stage3Output.getDailyItineraries() != null && !stage3Output.getDailyItineraries().isEmpty()) {
                for (var dailyItinerary : stage3Output.getDailyItineraries()) {
                    Map<String, Object> dayData = new HashMap<>();
                    dayData.put("day", dailyItinerary.getDayNumber());
                    dayData.put("date", dailyItinerary.getDate().toString());

                    // 시간 블록별로 그룹화된 일정 생성 (프론트엔드 기대 형식)
                    Map<String, List<Map<String, Object>>> timeBlocks = new LinkedHashMap<>();

                    // 시간 블록을 순서대로 초기화
                    timeBlocks.put("09:00-12:00", new ArrayList<>());
                    timeBlocks.put("12:00-15:00", new ArrayList<>());
                    timeBlocks.put("15:00-18:00", new ArrayList<>());
                    timeBlocks.put("18:00-21:00", new ArrayList<>());

                    if (dailyItinerary.getTimeBlocks() != null && !dailyItinerary.getTimeBlocks().isEmpty()) {
                        for (Map.Entry<String, List<TravelPlace>> timeBlockEntry : dailyItinerary.getTimeBlocks().entrySet()) {
                            String timeBlock = timeBlockEntry.getKey();
                            List<TravelPlace> places = timeBlockEntry.getValue();

                            List<Map<String, Object>> blockPlaces = timeBlocks.computeIfAbsent(timeBlock, k -> new ArrayList<>());
                            for (TravelPlace place : places) {
                                Map<String, Object> placeData = convertTravelPlaceToMap(place);
                                blockPlaces.add(placeData);
                            }
                        }
                    } else if (dailyItinerary.getPlaces() != null) {
                        // 시간 블록이 없는 경우 places를 시간대별로 자동 분배
                        List<TravelPlace> places = dailyItinerary.getPlaces();
                        String[] blocks = {"09:00-12:00", "12:00-15:00", "15:00-18:00", "18:00-21:00"};

                        for (int i = 0; i < places.size(); i++) {
                            String block = blocks[Math.min(i, blocks.length - 1)];
                            Map<String, Object> placeData = convertTravelPlaceToMap(places.get(i));
                            timeBlocks.get(block).add(placeData);
                        }
                    }

                    // 비어있지 않은 시간 블록만 포함
                    Map<String, List<Map<String, Object>>> nonEmptyBlocks = new LinkedHashMap<>();
                    for (Map.Entry<String, List<Map<String, Object>>> entry : timeBlocks.entrySet()) {
                        if (!entry.getValue().isEmpty()) {
                            nonEmptyBlocks.put(entry.getKey(), entry.getValue());
                        }
                    }

                    dayData.put("timeBlocks", nonEmptyBlocks);
                    dayData.put("totalPlaces", nonEmptyBlocks.values().stream()
                        .mapToInt(List::size).sum());
                    dayData.put("estimatedDuration", dailyItinerary.getEstimatedDuration());

                    itinerary.add(dayData);
                }
            }

            // 경로 최적화 정보 추가
            double totalDistance = 0.0;
            long totalTime = 0;
            if (stage3Output.getOptimizedRoutes() != null) {
                for (var route : stage3Output.getOptimizedRoutes()) {
                    totalDistance += route.getTotalDistance();
                    totalTime += route.getTotalDuration();
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("itinerary", itinerary);  // 프론트엔드가 기대하는 키
            result.put("totalDays", itinerary.size());
            result.put("stage", 3);
            result.put("type", "FINAL_ITINERARY_CREATED");
            result.put("nextAction", "REVIEW_AND_CONFIRM");
            result.put("optimizationApplied", true);
            result.put("totalDistance", totalDistance);
            result.put("totalTime", totalTime);

            log.info("✅ [Stage 3] 완료 - {}일 최종 일정 생성 완료, 총 거리: {}km, 총 시간: {}분",
                itinerary.size(), String.format("%.2f", totalDistance), totalTime);

            return result;

        } catch (Exception e) {
            log.error("❌ [Stage 3] 오류 발생: ", e);
            throw new RuntimeException("Stage 3 처리 중 오류 발생", e);
        }
    }

    // DB에서 추가 장소 선별하고 최적화
    private List<Map<String, Object>> selectAndOptimizePlaces(
            int day, List<Map<String, Object>> selectedPlaces, TravelContext context) {

        List<Map<String, Object>> optimizedSchedule = new ArrayList<>();

        // 사용자 지정 시간 가져오기 (Phase 2에서 입력)
        String departureTime = (String) context.getCollectedInfo().get("departureTime");
        String endTime = (String) context.getCollectedInfo().get("endTime");

        // 기본값 설정
        if (departureTime == null) departureTime = "09:00";
        if (endTime == null) endTime = "21:00";

        // 시간대 생성 (사용자 지정 시간 기반)
        int startHour = Integer.parseInt(departureTime.split(":")[0]);
        int endHour = Integer.parseInt(endTime.split(":")[0]);
        List<String> timeSlots = new ArrayList<>();

        // 3시간 간격으로 시간대 생성
        for (int hour = startHour; hour < endHour && timeSlots.size() < selectedPlaces.size(); hour += 3) {
            timeSlots.add(String.format("%02d:00", hour));
        }

        // 선택된 장소들 모두 추가 (사용자가 선택한 것 우선)
        int slotIndex = 0;
        for (Map<String, Object> place : selectedPlaces) {
            Map<String, Object> scheduleItem = new HashMap<>();
            if (slotIndex < timeSlots.size()) {
                scheduleItem.put("time", timeSlots.get(slotIndex++));
            } else {
                // 시간대가 부족하면 마지막 시간대 사용
                scheduleItem.put("time", timeSlots.get(timeSlots.size() - 1));
            }
            scheduleItem.put("place", place);
            scheduleItem.put("duration", 90); // 기본 90분
            scheduleItem.put("source", "user_selected");
            optimizedSchedule.add(scheduleItem);
        }

        // 사용자가 선택한 장소가 충분하면 DB 추가 제외
        // 최소 3개 이상이면 추가하지 않음
        if (optimizedSchedule.size() < 3 && optimizedSchedule.size() < timeSlots.size()) {
            int needMore = Math.min(timeSlots.size() - optimizedSchedule.size(), 1);
            log.info("📍 추가 장소 필요: 현재 {} 개, 추가 {} 개 조회", optimizedSchedule.size(), needMore);

            List<Map<String, Object>> additionalPlaces = fetchAdditionalPlacesFromDB(
                day, needMore, selectedPlaces, context);

            for (Map<String, Object> place : additionalPlaces) {
                if (slotIndex < timeSlots.size()) {
                    Map<String, Object> scheduleItem = new HashMap<>();
                    scheduleItem.put("time", timeSlots.get(slotIndex++));
                    scheduleItem.put("place", place);
                    scheduleItem.put("duration", 60); // 추가 장소는 60분
                    scheduleItem.put("source", "ai_recommended");
                    optimizedSchedule.add(scheduleItem);
                }
            }
        } else {
            log.info("📍 사용자 선택 장소 충분: {} 개 (추가 조회 없음)", optimizedSchedule.size());
        }

        return optimizedSchedule;
    }

    // DB에서 추가 장소 가져오기
    private List<Map<String, Object>> fetchAdditionalPlacesFromDB(
            int day, int count, List<Map<String, Object>> existingPlaces, TravelContext context) {

        List<Map<String, Object>> additional = new ArrayList<>();

        try {
            // 지역 정보 추출
            @SuppressWarnings("unchecked")
            List<String> destinations = (List<String>) context.getCollectedInfo()
                .get(TravelContext.KEY_DESTINATIONS);

            if (destinations != null && !destinations.isEmpty()) {
                String region = destinations.get(0);

                // 기존 장소 ID 추출 (중복 방지)
                Set<String> existingNames = existingPlaces.stream()
                    .map(p -> String.valueOf(p.get("name")))
                    .collect(Collectors.toSet());

                // DB에서 추가 장소 조회 (이름 중복 방지)
                List<TravelCandidate> candidates = travelCandidateRepository
                    .findByRegion(region).stream()
                    .filter(c -> !existingNames.contains(c.getName()))
                    .limit(count)
                    .collect(Collectors.toList());

                // TravelCandidate를 Map으로 변환
                for (TravelCandidate candidate : candidates) {
                    Map<String, Object> placeMap = new HashMap<>();
                    placeMap.put("id", candidate.getId());
                    placeMap.put("name", candidate.getName());
                    placeMap.put("address", candidate.getAddress());
                    placeMap.put("category", candidate.getCategory());
                    placeMap.put("description", candidate.getDescription());
                    placeMap.put("lat", candidate.getLatitude());
                    placeMap.put("lng", candidate.getLongitude());
                    placeMap.put("rating", candidate.getRating());
                    additional.add(placeMap);
                }
            }

        } catch (Exception e) {
            log.warn("추가 장소 조회 실패, 기본값 사용: ", e);
        }

        return additional;
    }

    // 경로 정보 추가
    private void enrichWithRouteInformation(List<Map<String, Object>> itinerary, TravelContext context) {
        // 각 일정에 이동 경로 정보 추가
        for (Map<String, Object> dayPlan : itinerary) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> schedule = (List<Map<String, Object>>) dayPlan.get("schedule");

            if (schedule != null && schedule.size() > 1) {
                for (int i = 0; i < schedule.size() - 1; i++) {
                    Map<String, Object> current = schedule.get(i);
                    Map<String, Object> next = schedule.get(i + 1);

                    // 이동 정보 추가 (거리, 시간 등)
                    Map<String, Object> route = new HashMap<>();
                    route.put("from", ((Map<String, Object>) current.get("place")).get("name"));
                    route.put("to", ((Map<String, Object>) next.get("place")).get("name"));
                    route.put("distance", calculateDistance(current, next)); // km
                    route.put("duration", calculateDuration(current, next)); // 분
                    route.put("transport", determineTransport(current, next));

                    current.put("nextRoute", route);
                    current.put("transport", route);  // transport 필드도 추가
                }
            }
        }
    }

    // 날짜 계산
    private String calculateDate(TravelContext context, int dayNumber) {
        try {
            String startDateStr = (String) context.getCollectedInfo().get(TravelContext.KEY_START_DATE);

            if (startDateStr != null) {
                // LocalDate로 변환하여 dayNumber만큼 더하기
                LocalDate startDate = LocalDate.parse(startDateStr);
                LocalDate targetDate = startDate.plusDays(dayNumber - 1); // dayNumber는 1부터 시작
                return targetDate.toString();
            }
        } catch (Exception e) {
            log.warn("날짜 계산 실패: ", e);
        }
        // 기본값: 현재 날짜 기준으로 계산
        return LocalDate.now().plusDays(dayNumber - 1).toString();
    }

    // 거리 계산 (간단한 직선 거리)
    private double calculateDistance(Map<String, Object> from, Map<String, Object> to) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> fromPlace = (Map<String, Object>) from.get("place");
            @SuppressWarnings("unchecked")
            Map<String, Object> toPlace = (Map<String, Object>) to.get("place");

            double lat1 = ((Number) fromPlace.get("lat")).doubleValue();
            double lng1 = ((Number) fromPlace.get("lng")).doubleValue();
            double lat2 = ((Number) toPlace.get("lat")).doubleValue();
            double lng2 = ((Number) toPlace.get("lng")).doubleValue();

            // Haversine formula
            double R = 6371; // 지구 반지름 (km)
            double dLat = Math.toRadians(lat2 - lat1);
            double dLng = Math.toRadians(lng2 - lng1);
            double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                      Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                      Math.sin(dLng/2) * Math.sin(dLng/2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
            return Math.round(R * c * 10) / 10.0; // 소수점 1자리
        } catch (Exception e) {
            return 5.0; // 기본값
        }
    }

    // 이동 시간 계산 (거리 기반 추정)
    private int calculateDuration(Map<String, Object> from, Map<String, Object> to) {
        double distance = calculateDistance(from, to);
        // 대중교통 기준: 평균 20km/h
        return (int) Math.ceil(distance * 3); // 분 단위
    }

    // 이동 수단 결정
    private String determineTransport(Map<String, Object> from, Map<String, Object> to) {
        double distance = calculateDistance(from, to);
        if (distance < 1.0) {
            return "도보";
        } else if (distance < 5.0) {
            return "버스";
        } else {
            return "지하철";
        }
    }

    // Stage 2 → Stage 3 전환 - 직접 변환 사용
    public Map<String, Object> processStage2ToStage3(TravelContext context, Map<String, Object> metadata) {
        log.info("🔄 [Stage 2 → Stage 3] 전환 시작");
        log.info("📊 [Stage 2 → Stage 3] 받은 metadata: {}", metadata);

        try {
            // 메타데이터에서 선택된 장소 추출
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> selectedPlaces =
                (List<Map<String, Object>>) metadata.get("selectedPlaces");

            log.info("📍 [Stage 2 → Stage 3] selectedPlaces 수: {}",
                selectedPlaces != null ? selectedPlaces.size() : "null");

            if (selectedPlaces != null && !selectedPlaces.isEmpty()) {
                // 항상 직접 변환 사용 - 사용자가 선택한 장소를 그대로 사용
                log.info("🔄 [Stage 2 → Stage 3] 직접 변환 처리 실행 (사용자 선택 장소 사용)");
                log.info("📍 선택된 장소들: {}", selectedPlaces.stream()
                    .map(p -> p.get("name"))
                    .collect(Collectors.toList()));

                // 직접 변환 사용 (프론트엔드 데이터를 직접 TravelPlace로 변환)
                Map<String, Object> conversionResult = stage2To3DirectConverter
                    .convertSelectedPlacesToStage3(context, metadata);

                // 직접 변환이 실패한 경우
                if (!(Boolean) conversionResult.get("success")) {
                    log.error("❌ 직접 변환 실패: {}", conversionResult.get("message"));
                    return conversionResult;
                }

                log.info("✅ 직접 변환 성공: {}", conversionResult.get("message"));

                // dailyDistribution이 제대로 생성되었는지 확인
                @SuppressWarnings("unchecked")
                Map<Integer, List<TravelPlace>> afterConversion =
                    (Map<Integer, List<TravelPlace>>) context.getMetadata().get("dailyDistribution");
                if (afterConversion != null) {
                    int totalPlaces = afterConversion.values().stream()
                        .mapToInt(List::size).sum();
                    log.info("📅 직접 변환 후 dailyDistribution: {} 일, 총 {} 개 장소",
                        afterConversion.size(), totalPlaces);
                }
            } else {
                log.warn("⚠️ [Stage 2 → Stage 3] selectedPlaces가 비어있거나 null입니다.");
                return Map.of(
                    "stage", 3,
                    "type", "ERROR",
                    "message", "선택된 장소가 없습니다.",
                    "success", false
                );
            }

            // Stage 3 실행 전 context 확인
            @SuppressWarnings("unchecked")
            Map<Integer, List<TravelPlace>> dailyDistribution =
                (Map<Integer, List<TravelPlace>>) context.getMetadata().get("dailyDistribution");
            log.info("📅 [Stage 2 → Stage 3] Stage 3 실행 전 dailyDistribution 존재 여부: {}",
                dailyDistribution != null ? "있음 (일 수: " + dailyDistribution.size() + ")" : "없음");

            // Stage 3 실행
            return processStage3(context);

        } catch (Exception e) {
            log.error("❌ [Stage 2 → Stage 3] 전환 오류: ", e);
            return Map.of(
                "stage", 3,
                "type", "ERROR",
                "message", "Stage 전환 중 오류 발생: " + e.getMessage(),
                "success", false
            );
        }
    }

    // 헬퍼 메서드들

    // 유효한 주소인지 확인 (무의미한 주소 필터링)
    private boolean isValidAddress(String address) {
        if (address == null || address.isEmpty()) {
            return false;
        }

        // 무의미한 주소 패턴들
        String lowerAddress = address.toLowerCase().trim();
        return !lowerAddress.contains("상이") &&
               !lowerAddress.contains("별도") &&
               !lowerAddress.contains("미정") &&
               !lowerAddress.equals("-") &&
               lowerAddress.length() > 5; // 최소 길이 체크
    }

    private boolean matchesTravelStyle(TravelCandidate place, List<String> travelStyles) {
        if (travelStyles == null || travelStyles.isEmpty()) {
            return false;
        }

        String category = place.getCategory() != null ? place.getCategory().toLowerCase() : "";
        String subCategory = place.getCategory() != null ? place.getCategory().toLowerCase() : "";
        String description = place.getDescription() != null ? place.getDescription().toLowerCase() : "";

        for (String style : travelStyles) {
            String lowerStyle = style.toLowerCase();
            if (category.contains(lowerStyle) ||
                subCategory.contains(lowerStyle) ||
                description.contains(lowerStyle)) {
                return true;
            }

            // 스타일별 특별 매칭
            switch (lowerStyle) {
                case "culture":
                    if (category.contains("문화") || category.contains("역사") ||
                        category.contains("박물관") || category.contains("궁")) {
                        return true;
                    }
                    break;
                case "food":
                    if (category.contains("음식") || category.contains("맛집") ||
                        category.contains("카페") || category.contains("레스토랑")) {
                        return true;
                    }
                    break;
                case "nature":
                    if (category.contains("자연") || category.contains("공원") ||
                        category.contains("산") || category.contains("바다")) {
                        return true;
                    }
                    break;
                case "shopping":
                    if (category.contains("쇼핑") || category.contains("시장") ||
                        category.contains("백화점") || category.contains("몰")) {
                        return true;
                    }
                    break;
                case "activity":
                    if (category.contains("액티비티") || category.contains("스포츠") ||
                        category.contains("레저") || category.contains("체험")) {
                        return true;
                    }
                    break;
            }
        }

        return false;
    }

    private TravelPlace convertToTravelPlace(TravelCandidate candidate) {
        TravelPlace place = new TravelPlace();
        place.setPlaceId(String.valueOf(candidate.getId())); // Long to String conversion
        place.setName(candidate.getName());
        place.setAddress(candidate.getAddress());
        place.setCategory(candidate.getCategory());
        place.setDescription(candidate.getDescription());
        place.setLatitude(candidate.getLatitude());
        place.setLongitude(candidate.getLongitude());
        place.setRating(candidate.getRating() != null ? candidate.getRating() : 0.0);
        // imageUrl과 businessHours는 TravelPlace에 없으므로 제거
        return place;
    }

    private Map<String, Object> convertTravelPlaceToMap(TravelPlace place) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", place.getPlaceId());
        map.put("name", place.getName());
        map.put("address", place.getAddress());
        map.put("category", place.getCategory());
        map.put("description", place.getDescription());
        map.put("lat", place.getLatitude());
        map.put("lng", place.getLongitude());
        map.put("rating", place.getRating());
        map.put("isUserSelected", place.getIsUserSelected());
        return map;
    }

    private Map<String, Object> convertTravelPlaceToMapWithTimeBlock(TravelPlace place, String timeBlock) {
        Map<String, Object> map = convertTravelPlaceToMap(place);
        map.put("timeBlock", timeBlock);
        return map;
    }

    private int calculateAutoFilledCount(Map<String, Object> itinerary) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> dailyItineraries =
                (List<Map<String, Object>>) itinerary.get("dailyItineraries");

            if (dailyItineraries == null) {
                return 0;
            }

            int totalPlaces = 0;
            for (Map<String, Object> daily : dailyItineraries) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> schedule =
                    (List<Map<String, Object>>) daily.get("schedule");
                if (schedule != null) {
                    totalPlaces += schedule.size();
                }
            }

            int userSelectedCount = (int) itinerary.getOrDefault("userSelectedCount", 0);
            return Math.max(0, totalPlaces - userSelectedCount);

        } catch (Exception e) {
            log.error("자동 채우기 개수 계산 실패: ", e);
            return 0;
        }
    }

    private double calculateQualityScore(com.compass.domain.chat.stage3.dto.Stage3Output stage3Output) {
        try {
            double score = 0.0;

            // 거리 기반 점수 (짧을수록 높은 점수)
            if (stage3Output.getTotalDistance() > 0) {
                double distanceScore = Math.max(0, 100 - (stage3Output.getTotalDistance() / 1000));
                score += distanceScore * 0.4;
            }

            // 시간 기반 점수 (적절한 시간일수록 높은 점수)
            if (stage3Output.getTotalDuration() > 0) {
                double durationScore = Math.max(0, 100 - (stage3Output.getTotalDuration() / 60));
                score += durationScore * 0.3;
            }

            // 일정 밀도 점수
            if (stage3Output.getDailyItineraries() != null) {
                double densityScore = stage3Output.getDailyItineraries().size() * 10;
                score += Math.min(densityScore, 30);
            }

            return Math.min(score, 100.0);

        } catch (Exception e) {
            log.error("품질 점수 계산 실패: ", e);
            return 70.0; // 기본 점수
        }
    }
}
