package com.compass.domain.chat.route_optimization.service;

import com.compass.domain.chat.function.processing.phase3.date_selection.model.TourPlace;
import com.compass.domain.chat.model.dto.ConfirmedSchedule;
import com.compass.domain.chat.model.context.TravelContext;
import com.compass.domain.chat.orchestrator.ContextManager;
import com.compass.domain.chat.repository.ChatThreadRepository;
import com.compass.domain.chat.route_optimization.client.KakaoMobilityClient;
import com.compass.domain.chat.route_optimization.model.RouteOptimizationRequest;
import com.compass.domain.chat.route_optimization.model.RouteOptimizationResponse;
import com.compass.domain.chat.route_optimization.model.RouteOptimizationResponse.RouteInfo;
import com.compass.domain.chat.route_optimization.strategy.OptimizationStrategy;
import com.compass.domain.chat.route_optimization.strategy.OptimizationStrategyFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteOptimizationOrchestrationService {

    private final ChatThreadRepository threadRepository;
    private final ObjectMapper objectMapper;
    private final ContextManager contextManager;
    private final ItineraryPersistenceService persistenceService;
    private final OptimizationStrategyFactory strategyFactory;
    private final MultiPathOptimizationService multiPathOptimizationService;
    private final KakaoMobilityClient kakaoMobilityClient;

    @Transactional
    public RouteOptimizationResponse processRouteOptimization(Long sessionId, RouteOptimizationRequest request) {
        log.info("Route optimization 처리: Session={}", sessionId);

        // Stage2 출력에서 데이터 추출
        Map<Integer, List<TourPlace>> dailyCandidates = request.getDailyPlaceCandidates();
        List<ConfirmedSchedule> ocrSchedules = request.ocrConfirmedSchedules();

        log.info("Stage2에서 받은 데이터: {} days, 총 {} 장소, OCR 확정 일정: {} 개",
            dailyCandidates.size(),
            dailyCandidates.values().stream().mapToInt(List::size).sum(),
            ocrSchedules.size()
        );

        try {
            // 다중 경로 최적화 사용 여부 확인
            boolean useMultiPath = shouldUseMultiPath(request);

            Map<Integer, List<TourPlace>> aiRecommended;

            if (useMultiPath && multiPathOptimizationService != null) {
                // 다중 경로 최적화 사용
                log.info("다중 경로 최적화 사용");
                aiRecommended = optimizeWithMultiPath(dailyCandidates, ocrSchedules, request);
            } else {
                // 기존 단일 경로 최적화
                log.info("단일 경로 최적화 사용");
                aiRecommended = optimizeItinerary(dailyCandidates, ocrSchedules, request.startDate());
            }

            // 2. 경로 정보 계산 (카카오 API 활용)
            Map<Integer, RouteInfo> routes = calculateRoutesWithKakaoAPI(aiRecommended, request.transportMode());

            // 3. 응답 생성
            RouteOptimizationResponse response = RouteOptimizationResponse.success(aiRecommended, dailyCandidates, routes);

            // 4. DB 저장
            if (persistenceService != null) {
                persistenceService.saveItinerary(sessionId, request, response);
            }

            return response;

        } catch (Exception e) {
            log.error("Route optimization 처리 실패", e);
            return RouteOptimizationResponse.error(e.getMessage());
        }
    }

    // AI 추천 일정 최적화 (OCR 확정 일정 우선 처리)
    // AI 추천 일정 최적화 (OCR 확정 일정 우선 처리)
    private Map<Integer, List<TourPlace>> optimizeItinerary(
            Map<Integer, List<TourPlace>> candidates,
            List<ConfirmedSchedule> ocrSchedules,
            LocalDate startDate) {

        Map<Integer, List<TourPlace>> optimized = new HashMap<>();

        // 1. OCR 확정 일정을 먼저 배치
        Map<Integer, List<TourPlace>> fixedSchedules = convertOcrToTourPlaces(ocrSchedules, startDate);

        for (var entry : candidates.entrySet()) {
            int day = entry.getKey();
            List<TourPlace> places = entry.getValue();

            log.debug("Day {} 최적화: {} 장소 중 선택", day, places.size());

            List<TourPlace> selected = new ArrayList<>();

            // OCR 확정 일정 먼저 추가
            if (fixedSchedules.containsKey(day)) {
                selected.addAll(fixedSchedules.get(day));
                log.info("Day {}: OCR 확정 일정 {} 개 추가", day, fixedSchedules.get(day).size());
            }

            // OCR 일정과 겹치지 않는 시간대 확인
            Set<String> occupiedTimeBlocks = selected.stream()
                .map(TourPlace::timeBlock)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

            // 시간대별로 그룹화
            Map<String, List<TourPlace>> byTimeBlock = places.stream()
                .filter(p -> p.timeBlock() != null)
                .filter(p -> !occupiedTimeBlocks.contains(p.timeBlock()))  // OCR 일정과 겹치지 않는 것만
                .collect(Collectors.groupingBy(TourPlace::timeBlock));

            // 아침/점심/저녁 추가 (OCR과 겹치지 않으면)
            if (!occupiedTimeBlocks.contains("BREAKFAST"))
                addBestPlace(byTimeBlock.get("BREAKFAST"), selected, 1);
            if (!occupiedTimeBlocks.contains("LUNCH"))
                addBestPlace(byTimeBlock.get("LUNCH"), selected, 1);
            if (!occupiedTimeBlocks.contains("DINNER"))
                addBestPlace(byTimeBlock.get("DINNER"), selected, 1);

            // 활동 장소 선택 (OCR 일정 고려)
            int remainingSlots = Math.max(0, 6 - selected.size());  // 하루 최대 6개 일정
            List<TourPlace> activities = new ArrayList<>();

            if (byTimeBlock.get("MORNING_ACTIVITY") != null)
                activities.addAll(byTimeBlock.get("MORNING_ACTIVITY"));
            if (byTimeBlock.get("AFTERNOON_ACTIVITY") != null)
                activities.addAll(byTimeBlock.get("AFTERNOON_ACTIVITY"));
            if (byTimeBlock.get("EVENING_ACTIVITY") != null)
                activities.addAll(byTimeBlock.get("EVENING_ACTIVITY"));

            activities.stream()
                .sorted(Comparator.comparing((TourPlace p) ->
                    p.rating() != null ? p.rating() : 0.0).reversed())
                .limit(remainingSlots)
                .forEach(selected::add);

            // 카페 선택 (공간이 있으면)
            if (selected.size() < 8 && !occupiedTimeBlocks.contains("CAFE"))
                addBestPlace(byTimeBlock.get("CAFE"), selected, 1);

            log.info("Day {}: 총 {}개 장소 (OCR: {}, AI: {})",
                day, selected.size(),
                fixedSchedules.getOrDefault(day, List.of()).size(),
                selected.size() - fixedSchedules.getOrDefault(day, List.of()).size());

            optimized.put(day, selected);
        }

        return optimized;
    }

    // 평점 기준 최고 장소 선택
    private void addBestPlace(List<TourPlace> places, List<TourPlace> selected, int limit) {
        if (places != null && !places.isEmpty()) {
            places.stream()
                .sorted(Comparator.comparing((TourPlace p) ->
                    p.rating() != null ? p.rating() : 0.0).reversed())
                .limit(limit)
                .forEach(selected::add);
        }
    }

    // 다중 경로 사용 여부 결정
    private boolean shouldUseMultiPath(RouteOptimizationRequest request) {
        // 다중 경로 최적화를 사용할 조건
        // 1. 시간대별 후보가 많은 경우
        // 2. 사용자가 명시적으로 요청한 경우
        // 3. OCR 확정 일정이 적어서 선택지가 많은 경우

        int totalCandidates = request.getDailyPlaceCandidates().values().stream()
            .mapToInt(List::size).sum();
        int ocrCount = request.ocrConfirmedSchedules().size();

        // 후보가 20개 이상이고 OCR 확정 일정이 3개 미만인 경우
        boolean shouldUse = totalCandidates > 20 && ocrCount < 3;

        log.info("다중 경로 사용 결정: 총 후보={}, OCR 확정={}, 사용={}",
            totalCandidates, ocrCount, shouldUse);

        return shouldUse;
    }

    // 다중 경로 최적화를 사용한 일정 생성
    private Map<Integer, List<TourPlace>> optimizeWithMultiPath(
            Map<Integer, List<TourPlace>> dailyCandidates,
            List<ConfirmedSchedule> ocrSchedules,
            RouteOptimizationRequest request) {

        log.info("다중 경로 최적화 시작");
        Map<Integer, List<TourPlace>> optimized = new HashMap<>();

        // OCR 확정 일정을 TourPlace로 변환
        Map<Integer, List<TourPlace>> fixedSchedules = convertOcrToTourPlaces(ocrSchedules, request.startDate());

        for (var entry : dailyCandidates.entrySet()) {
            int day = entry.getKey();
            List<TourPlace> allCandidates = entry.getValue();

            // 시간대별로 후보 그룹화
            Map<String, List<TourPlace>> timeBlockCandidates = allCandidates.stream()
                .filter(p -> p.timeBlock() != null)
                .collect(Collectors.groupingBy(TourPlace::timeBlock));

            // OCR 확정 일정 처리
            List<TourPlace> fixedPlaces = fixedSchedules.getOrDefault(day, new ArrayList<>());
            Set<String> occupiedTimeBlocks = fixedPlaces.stream()
                .map(TourPlace::timeBlock)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

            // OCR과 겹치지 않는 시간대의 후보만 다중 경로 탐색
            Map<String, List<TourPlace>> availableTimeBlocks = new HashMap<>();
            for (var tbEntry : timeBlockCandidates.entrySet()) {
                if (!occupiedTimeBlocks.contains(tbEntry.getKey())) {
                    availableTimeBlocks.put(tbEntry.getKey(), tbEntry.getValue());
                }
            }

            // 다중 경로 최적화 실행
            List<TourPlace> optimalPath = multiPathOptimizationService.findOptimalPath(
                availableTimeBlocks,
                KakaoMobilityClient.TransportMode.valueOf(request.transportMode())
            );

            // OCR 확정 일정과 최적 경로 병합
            List<TourPlace> dayItinerary = new ArrayList<>();
            dayItinerary.addAll(fixedPlaces);
            dayItinerary.addAll(optimalPath);

            // 시간 순서대로 정렬
            dayItinerary.sort(Comparator.comparing(p -> getTimeBlockOrder(p.timeBlock())));

            log.info("Day {}: OCR 확정={}, 다중 경로 최적화={}, 총={}",
                day, fixedPlaces.size(), optimalPath.size(), dayItinerary.size());

            optimized.put(day, dayItinerary);
        }

        return optimized;
    }

    // 시간대별 순서 반환
    private int getTimeBlockOrder(String timeBlock) {
        if (timeBlock == null) return 999;
        return switch (timeBlock) {
            case "BREAKFAST" -> 1;
            case "MORNING_ACTIVITY" -> 2;
            case "LUNCH" -> 3;
            case "AFTERNOON_ACTIVITY" -> 4;
            case "CAFE" -> 5;
            case "DINNER" -> 6;
            case "EVENING_ACTIVITY" -> 7;
            default -> 999;
        };
    }

    // 카카오 API를 사용한 경로 계산
    private Map<Integer, RouteInfo> calculateRoutesWithKakaoAPI(
            Map<Integer, List<TourPlace>> itinerary,
            String transportMode) {

        Map<Integer, RouteInfo> routes = new HashMap<>();

        for (var entry : itinerary.entrySet()) {
            int day = entry.getKey();
            List<TourPlace> places = entry.getValue();

            if (places.isEmpty()) continue;

            // 순서 최적화 (TSP)
            List<TourPlace> orderedPlaces = optimizeVisitOrder(places);

            // 카카오 API 사용 가능한 경우
            if (kakaoMobilityClient != null) {
                try {
                    RouteInfo routeInfo = calculateWithKakaoAPI(orderedPlaces, transportMode);
                    routes.put(day, routeInfo);
                    log.info("Day {}: 카카오 API로 경로 계산 완료 - 거리: {:.2f}km, 시간: {}분",
                        day, routeInfo.totalDistance(), routeInfo.totalDuration());
                } catch (Exception e) {
                    log.warn("카카오 API 호출 실패, Haversine 공식 사용: {}", e.getMessage());
                    RouteInfo fallbackRoute = calculateWithHaversine(orderedPlaces, transportMode);
                    routes.put(day, fallbackRoute);
                }
            } else {
                // 카카오 API 사용 불가능한 경우 Haversine 공식 사용
                RouteInfo fallbackRoute = calculateWithHaversine(orderedPlaces, transportMode);
                routes.put(day, fallbackRoute);
            }
        }

        return routes;
    }

    // 카카오 API를 사용한 실제 경로 계산
    private RouteInfo calculateWithKakaoAPI(List<TourPlace> places, String transportMode) {
        if (places.size() < 2) {
            return new RouteInfo(
                places.stream().map(TourPlace::name).collect(Collectors.toList()),
                0.0, 0, transportMode, new ArrayList<>()
            );
        }

        // TourPlace를 Waypoint로 변환
        List<KakaoMobilityClient.Waypoint> waypoints = places.stream()
            .map(p -> new KakaoMobilityClient.Waypoint(
                p.name(),
                p.latitude() != null ? p.latitude() : 37.5665,  // 기본값: 서울시청
                p.longitude() != null ? p.longitude() : 126.9780
            ))
            .collect(Collectors.toList());

        // RouteRequest 생성
        KakaoMobilityClient.RouteRequest routeRequest = KakaoMobilityClient.RouteRequest.of(
            waypoints,
            KakaoMobilityClient.TransportMode.valueOf(transportMode)
        );

        // API 호출
        KakaoMobilityClient.RouteResponse response = kakaoMobilityClient.searchMultiDestinationRoute(routeRequest);

        if (response != null && response.sections() != null) {
            List<RouteInfo.Segment> segments = response.sections().stream()
                .map(section -> new RouteInfo.Segment(
                    section.from(),
                    section.to(),
                    section.distance(),
                    section.duration(),
                    transportMode
                ))
                .collect(Collectors.toList());

            return new RouteInfo(
                places.stream().map(TourPlace::name).collect(Collectors.toList()),
                response.totalDistance(),
                response.totalDuration(),
                transportMode,
                segments
            );
        }

        // API 호출 실패시 Haversine fallback
        log.warn("카카오 API 응답 없음, Haversine 공식 사용");
        return calculateWithHaversine(places, transportMode);
    }

    // Haversine 공식을 사용한 fallback 경로 계산
    private RouteInfo calculateWithHaversine(List<TourPlace> places, String transportMode) {
        double totalDistance = 0;
        int totalDuration = 0;
        List<RouteInfo.Segment> segments = new ArrayList<>();

        for (int i = 0; i < places.size() - 1; i++) {
            TourPlace from = places.get(i);
            TourPlace to = places.get(i + 1);

            double distance = calculateDistance(from, to);
            int duration = calculateDuration(distance, transportMode);

            segments.add(new RouteInfo.Segment(
                from.name(), to.name(),
                distance, duration,
                transportMode
            ));

            totalDistance += distance;
            totalDuration += duration;
        }

        return new RouteInfo(
            places.stream().map(TourPlace::name).collect(Collectors.toList()),
            totalDistance, totalDuration, transportMode, segments
        );
    }

    // 경로 계산 (실제 좌표 활용)
    private Map<Integer, RouteInfo> calculateRoutes(Map<Integer, List<TourPlace>> itinerary, String transportMode) {
        Map<Integer, RouteInfo> routes = new HashMap<>();

        for (var entry : itinerary.entrySet()) {
            int day = entry.getKey();
            List<TourPlace> places = entry.getValue();

            if (places.isEmpty()) continue;

            // TSP 근사로 순서 최적화
            List<TourPlace> orderedPlaces = optimizeVisitOrder(places);

            List<String> placeNames = orderedPlaces.stream()
                .map(TourPlace::name)
                .collect(Collectors.toList());

            // 실제 좌표로 거리 계산
            double totalDistance = 0;
            int totalDuration = 0;
            List<RouteInfo.Segment> segments = new ArrayList<>();

            for (int i = 0; i < orderedPlaces.size() - 1; i++) {
                TourPlace from = orderedPlaces.get(i);
                TourPlace to = orderedPlaces.get(i + 1);

                double distance = calculateDistance(from, to);
                int duration = calculateDuration(distance, transportMode);

                segments.add(new RouteInfo.Segment(
                    from.name(),
                    to.name(),
                    distance,
                    duration,
                    transportMode
                ));

                totalDistance += distance;
                totalDuration += duration;
            }

            log.debug("Day {}: 총 거리 {:.2f}km, 소요시간 {}분", day, totalDistance, totalDuration);
            routes.put(day, new RouteInfo(placeNames, totalDistance, totalDuration, transportMode, segments));
        }

        return routes;
    }

    // TSP 근사 알고리즘 (Strategy Pattern 사용)
    private List<TourPlace> optimizeVisitOrder(List<TourPlace> places) {
        if (places.size() <= 2) return new ArrayList<>(places);

        // 전략 팩토리가 없으면 기본 알고리즘 사용 (테스트 호환성)
        if (strategyFactory == null) {
            List<TourPlace> ordered = nearestNeighbor(places);
            ordered = twoOptImprovement(ordered);
            return ordered;
        }

        // 컨텍스트에서 최적화 전략 가져오기
        String strategyType = "BALANCED";  // 기본값
        if (contextManager != null) {
            Optional<TravelContext> context = contextManager.getContext("current", "user");
            if (context.isPresent()) {
                Object strategy = context.get().getCollectedInfo().get("optimizationStrategy");
                if (strategy != null) {
                    strategyType = strategy.toString();
                }
            }
        }

        // 전략 선택 및 실행
        OptimizationStrategy strategy = strategyFactory.getStrategy(strategyType);
        return strategy.optimize(places);
    }

    // Nearest Neighbor 알고리즘
    private List<TourPlace> nearestNeighbor(List<TourPlace> places) {
        List<TourPlace> ordered = new ArrayList<>();
        Set<TourPlace> unvisited = new HashSet<>(places);

        // 시작점: 첫 번째 장소 또는 가장 북쪽
        TourPlace current = places.stream()
            .filter(p -> p.latitude() != null)
            .max(Comparator.comparing(TourPlace::latitude))
            .orElse(places.get(0));

        ordered.add(current);
        unvisited.remove(current);

        // 가장 가까운 이웃 선택
        while (!unvisited.isEmpty()) {
            final TourPlace currentPlace = current;
            TourPlace nearest = unvisited.stream()
                .min(Comparator.comparing(p -> calculateDistance(currentPlace, p)))
                .orElse(unvisited.iterator().next());

            ordered.add(nearest);
            unvisited.remove(nearest);
            current = nearest;
        }

        return ordered;
    }

    // 2-opt 알고리즘으로 경로 개선
    private List<TourPlace> twoOptImprovement(List<TourPlace> route) {
        if (route.size() < 4) return route;  // 최소 4개 노드 필요

        List<TourPlace> improved = new ArrayList<>(route);
        boolean improvement = true;
        int iterations = 0;
        int maxIterations = 100;  // 무한 루프 방지

        while (improvement && iterations < maxIterations) {
            improvement = false;
            iterations++;

            for (int i = 1; i < improved.size() - 2; i++) {
                for (int j = i + 1; j < improved.size(); j++) {
                    // 현재 거리: (i-1 -> i) + (j -> j+1)
                    double currentDistance = 0;
                    if (i > 0) {
                        currentDistance += calculateDistance(improved.get(i-1), improved.get(i));
                    }
                    if (j < improved.size() - 1) {
                        currentDistance += calculateDistance(improved.get(j), improved.get((j+1) % improved.size()));
                    }

                    // 교체 후 거리: (i-1 -> j) + (i -> j+1)
                    double newDistance = 0;
                    if (i > 0) {
                        newDistance += calculateDistance(improved.get(i-1), improved.get(j));
                    }
                    if (j < improved.size() - 1) {
                        newDistance += calculateDistance(improved.get(i), improved.get((j+1) % improved.size()));
                    }

                    // 개선되면 경로 교체
                    if (newDistance < currentDistance) {
                        // i부터 j까지 역순으로 변경
                        reverseSubRoute(improved, i, j);
                        improvement = true;
                        log.debug("2-opt 개선: {}번째 반복, 구간 [{}, {}] 역순 처리", iterations, i, j);
                    }
                }
            }
        }

        log.info("2-opt 최적화 완료: {} 반복, 최종 경로 크기: {}", iterations, improved.size());
        return improved;
    }

    // 부분 경로 역순 처리
    private void reverseSubRoute(List<TourPlace> route, int start, int end) {
        while (start < end) {
            TourPlace temp = route.get(start);
            route.set(start, route.get(end));
            route.set(end, temp);
            start++;
            end--;
        }
    }

    // Haversine 공식으로 거리 계산
    private double calculateDistance(TourPlace from, TourPlace to) {
        if (from.latitude() == null || to.latitude() == null) return 5.0;

        double lat1 = from.latitude();
        double lon1 = from.longitude();
        double lat2 = to.latitude();
        double lon2 = to.longitude();

        double R = 6371; // km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                  Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                  Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }

    // 이동수단별 소요시간 계산
    private int calculateDuration(double distance, String transportMode) {
        double speedKmh = switch (transportMode) {
            case "CAR" -> 40;
            case "PUBLIC_TRANSPORT" -> 25;
            case "WALKING" -> 4;
            default -> 30;
        };
        return (int)(distance / speedKmh * 60);
    }



    // OCR 확정 일정을 TourPlace로 변환
    private Map<Integer, List<TourPlace>> convertOcrToTourPlaces(
            List<ConfirmedSchedule> ocrSchedules,
            LocalDate startDate) {

        Map<Integer, List<TourPlace>> result = new HashMap<>();

        if (ocrSchedules == null || ocrSchedules.isEmpty()) {
            return result;
        }

        for (ConfirmedSchedule schedule : ocrSchedules) {
            // 날짜 계산 (시작일로부터 며칠째인지)
            LocalDate scheduleDate = schedule.startTime().toLocalDate();
            int dayNumber = (int) java.time.temporal.ChronoUnit.DAYS.between(startDate, scheduleDate) + 1;

            if (dayNumber < 1) continue;  // 여행 시작 전 일정은 무시

            // TimeBlock 결정
            String timeBlock = determineTimeBlock(schedule.startTime());

            // TourPlace로 변환
            TourPlace tourPlace = TourPlace.builder()
                .id("ocr_" + schedule.hashCode())
                .name(schedule.title())
                .timeBlock(timeBlock)
                .category(mapDocumentTypeToCategory(schedule.documentType()))
                .address(schedule.address() != null ? schedule.address() : schedule.location())
                .latitude(null)  // OCR에서는 좌표 정보가 없을 수 있음
                .longitude(null)
                .rating(5.0)  // 확정 일정은 최고 우선순위
                .priceLevel("N/A")
                .isTrendy(false)
                .petAllowed(false)
                .parkingAvailable(false)
                .day(dayNumber)
                .build();

            result.computeIfAbsent(dayNumber, k -> new ArrayList<>()).add(tourPlace);
        }

        return result;
    }

    // 시간대로 TimeBlock 결정
    private String determineTimeBlock(LocalDateTime dateTime) {
        int hour = dateTime.getHour();

        if (hour >= 6 && hour < 10) return "BREAKFAST";
        else if (hour >= 10 && hour < 12) return "MORNING_ACTIVITY";
        else if (hour >= 12 && hour < 14) return "LUNCH";
        else if (hour >= 14 && hour < 17) return "AFTERNOON_ACTIVITY";
        else if (hour >= 17 && hour < 20) return "DINNER";
        else if (hour >= 20 && hour < 23) return "EVENING_ACTIVITY";
        else return "LATE_ACTIVITY";
    }

    // DocumentType을 카테고리로 매핑
    private String mapDocumentTypeToCategory(com.compass.domain.chat.model.enums.DocumentType type) {
        return switch (type) {
            case FLIGHT_RESERVATION -> "교통";
            case HOTEL_RESERVATION -> "숙박";
            case TRAIN_TICKET -> "교통";
            case EVENT_TICKET -> "공연/이벤트";
            case RESTAURANT_RESERVATION -> "맛집";
            case ATTRACTION_TICKET -> "관광지";
            case CAR_RENTAL -> "교통";
            default -> "기타";
        };
    }

    // 커스터마이징 요청 DTO
    public record CustomizationRequest(
        Map<Integer, List<String>> selectedPlaces,
        String optimizationStrategy,
        String transportMode
    ) {}

    @Transactional
    public RouteOptimizationResponse customizeItinerary(
        Long sessionId,
        Long itineraryId,
        CustomizationRequest request
    ) {
        log.info("커스터마이징: Session={}, Itinerary={}", sessionId, itineraryId);

        try {
            // Mock 데이터로 처리
            Map<Integer, List<TourPlace>> mockPlaces = createMockPlaces();

            // 사용자 선택 기반 새 일정 구성
            Map<Integer, List<TourPlace>> customItinerary = new HashMap<>();

            for (var entry : request.selectedPlaces().entrySet()) {
                int day = entry.getKey();
                List<String> names = entry.getValue();

                List<TourPlace> dayPlaces = mockPlaces.getOrDefault(day, List.of()).stream()
                    .filter(p -> names.contains(p.name()))
                    .collect(Collectors.toList());

                customItinerary.put(day, dayPlaces);
            }

            // 경로 재계산
            Map<Integer, RouteInfo> routes = calculateRoutes(customItinerary, request.transportMode());

            // DB 업데이트
            // Mock 데이터 사용
            Map<Integer, List<TourPlace>> allCandidates = mockPlaces;

            return RouteOptimizationResponse.success(customItinerary, allCandidates, routes);

        } catch (Exception e) {
            log.error("커스터마이징 실패", e);
            return RouteOptimizationResponse.error(e.getMessage());
        }
    }

    // Mock 데이터 생성
    private Map<Integer, List<TourPlace>> createMockPlaces() {
        Map<Integer, List<TourPlace>> mock = new HashMap<>();

        mock.put(1, List.of(
            TourPlace.builder()
                .id("1")
                .name("경복궁")
                .timeBlock("MORNING_ACTIVITY")
                .day(1)
                .latitude(37.5796)
                .longitude(126.9770)
                .address("서울 종로구")
                .category("관광지")
                .rating(4.5)
                .priceLevel("$$")
                .isTrendy(false)
                .build(),
            TourPlace.builder()
                .id("2")
                .name("북촌한옥마을")
                .timeBlock("AFTERNOON_ACTIVITY")
                .day(1)
                .latitude(37.5826)
                .longitude(126.9835)
                .address("서울 종로구")
                .category("관광지")
                .rating(4.3)
                .priceLevel("$")
                .isTrendy(false)
                .build()
        ));

        mock.put(2, List.of(
            TourPlace.builder()
                .id("3")
                .name("명동")
                .timeBlock("MORNING_ACTIVITY")
                .day(2)
                .latitude(37.5638)
                .longitude(126.9868)
                .address("서울 중구")
                .category("쇼핑")
                .rating(4.2)
                .priceLevel("$$")
                .isTrendy(true)
                .build(),
            TourPlace.builder()
                .id("4")
                .name("남산타워")
                .timeBlock("AFTERNOON_ACTIVITY")
                .day(2)
                .latitude(37.5512)
                .longitude(126.9882)
                .address("서울 용산구")
                .category("관광지")
                .rating(4.5)
                .priceLevel("$$$")
                .isTrendy(false)
                .build()
        ));

        return mock;
    }
}