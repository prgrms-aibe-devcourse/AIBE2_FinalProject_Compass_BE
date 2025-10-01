package com.compass.domain.chat.stage3.service;

import com.compass.domain.chat.entity.TravelCandidate;
import com.compass.domain.chat.model.TravelPlace;
import com.compass.domain.chat.model.context.TravelContext;
import com.compass.domain.chat.model.dto.ConfirmedSchedule;
import com.compass.domain.chat.repository.TravelCandidateRepository;
import com.compass.domain.chat.stage2.dto.SelectedSchedule;
import com.compass.domain.chat.stage3.dto.Stage3Input;
import com.compass.domain.chat.stage3.dto.Stage3Output;
import com.compass.domain.chat.stage3.dto.OptimizedRoute;
import com.compass.domain.chat.stage3.dto.DailyItinerary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class Stage3IntegrationService {

    private final TravelCandidateRepository travelCandidateRepository;
    private final Stage3TravelCandidateEnrichmentService enrichmentService;
    private final PlaceScoreCalculationService scoreCalculationService;
    private final Stage3RouteOptimizationService routeOptimizationService;
    private final Stage3KMeansClusteringService kMeansClusteringService;
    private final TimeBlockRecommendationService timeBlockRecommendationService;

    // TravelContext를 활용한 Phase 2 → Stage 3 통합 처리
    @Transactional(readOnly = true)
    public Stage3Output processWithTravelContext(TravelContext context) {
        log.info("Processing Stage 3 with TravelContext for user: {}", context.getUserId());
        log.info("Context metadata keys: {}", context.getMetadata().keySet());
        log.info("Context collectedInfo keys: {}", context.getCollectedInfo().keySet());

        // TravelContext에서 Phase 2 정보 추출
        String destination = extractDestination(context);
        String departureLocation = (String) context.getCollectedInfo().get(TravelContext.KEY_DEPARTURE);

        // 날짜 안전하게 변환 (String 또는 LocalDate 모두 처리)
        LocalDate startDate = convertToLocalDate(context.getCollectedInfo().get(TravelContext.KEY_START_DATE));
        LocalDate endDate = convertToLocalDate(context.getCollectedInfo().get(TravelContext.KEY_END_DATE));

        log.info("Destination: {}, Start: {}, End: {}", destination, startDate, endDate);

        // travelStyle 처리 (List 또는 String)
        String travelStyle = extractTravelStyle(context);
        String companions = (String) context.getCollectedInfo().get(TravelContext.KEY_COMPANIONS);
        String transportMode = (String) context.getCollectedInfo().get(TravelContext.KEY_TRANSPORTATION_TYPE);

        // 사용자 선택 장소 추출
        List<SelectedSchedule> userSelectedPlaces = extractUserSelectedPlaces(context);
        log.info("Extracted {} user selected places from context", userSelectedPlaces.size());

        // 장소별 상세 로깅
        for (SelectedSchedule place : userSelectedPlaces) {
            log.info("User selected place: {} ({})", place.placeName(), place.placeId());
        }

        // OCR 확정 일정 활용
        List<ConfirmedSchedule> confirmedSchedules = context.getOcrConfirmedSchedules();

        // Stage3Input 생성 (사용자 선택 장소 포함)
        Stage3Input input = Stage3Input.fromPhase2Output(
            destination, startDate, endDate, travelStyle, companions, transportMode
        ).withUserSelectedPlaces(userSelectedPlaces);

        // OCR 확정 일정을 고려한 처리 (출발지 정보 포함)
        return processWithConfirmedSchedules(input, confirmedSchedules, departureLocation);
    }

    // Phase 2 Output → Stage 3 처리
    @Transactional(readOnly = true)
    public Stage3Output processPhase2Output(Stage3Input input) {
        log.info("Starting Stage 3 processing for destination: {}", input.destination());

        // Phase2에서는 출발지 정보가 없으므로 null 사용
        String departureLocation = null;

        // 1. travel_candidates에서 해당 지역의 장소들 조회
        List<TravelCandidate> candidates = fetchEnrichedCandidates(
            input.destination(),
            input.startDate(),
            input.endDate()
        );

        // 2. 사용자 선호도에 따른 스코어링
        List<TravelPlace> scoredPlaces = scoreCalculationService.calculateScores(
            candidates,
            input.travelStyle(),
            input.travelCompanion()
        );

        // 3. 날짜별 일정 생성
        List<DailyItinerary> dailyItineraries = createDailyItineraries(
            candidates,
            scoredPlaces,
            input.startDate(),
            input.endDate(),
            input.userSelectedPlaces(),
            input.travelStyle()
        );

        // 4. 경로 최적화 (출발지 정보 포함)
        List<OptimizedRoute> optimizedRoutes = optimizeRoutes(
            dailyItineraries,
            input.transportMode(),
            departureLocation
        );

        Stage3Output output = Stage3Output.builder()
            .dailyItineraries(dailyItineraries)
            .optimizedRoutes(optimizedRoutes)
            .totalDistance(calculateTotalDistance(optimizedRoutes))
            .totalDuration(calculateTotalDuration(optimizedRoutes))
            .generatedAt(LocalDateTime.now())
            .build();

        // 프론트엔드 콘솔용 상세 로깅
        logDetailedItineraryForFrontend(output);

        return output;
    }

    // OCR 확정 일정을 고려한 Stage 3 처리
    private Stage3Output processWithConfirmedSchedules(
            Stage3Input input,
            List<ConfirmedSchedule> confirmedSchedules,
            String departureLocation) {

        log.info("Processing with {} confirmed schedules from OCR, departing from {}",
                confirmedSchedules.size(), departureLocation);

        // 1. travel_candidates에서 해당 지역의 장소들 조회
        List<TravelCandidate> candidates = fetchEnrichedCandidates(
            input.destination(),
            input.startDate(),
            input.endDate()
        );

        // 2. 사용자 선호도 + OCR 확정 일정 고려한 스코어링
        List<TravelPlace> scoredPlaces = scoreCalculationService.calculateScoresWithConstraints(
            candidates,
            input.travelStyle(),
            input.travelCompanion(),
            confirmedSchedules
        );

        // 3. OCR 확정 일정을 고려한 날짜별 일정 생성
        List<DailyItinerary> dailyItineraries = createDailyItinerariesWithConfirmedSchedules(
            candidates,
            scoredPlaces,
            input.startDate(),
            input.endDate(),
            input.userSelectedPlaces(),
            confirmedSchedules,
            input.travelStyle()
        );

        // Day 1 첫번째에 출발지 추가
        if (departureLocation != null && !departureLocation.isEmpty() && !dailyItineraries.isEmpty()) {
            DailyItinerary day1 = dailyItineraries.get(0);
            List<TravelPlace> places = new ArrayList<>(day1.getPlaces());

            TravelPlace departure = TravelPlace.builder()
                .name(departureLocation + " 출발")
                .category("출발")
                .description("여행 출발지")
                .latitude(null)  // TODO: TravelContext에서 departureLat 가져오도록 수정 필요
                .longitude(null)
                .build();

            places.add(0, departure);

            DailyItinerary updatedDay1 = DailyItinerary.builder()
                .date(day1.getDate())
                .dayNumber(day1.getDayNumber())
                .places(places)
                .estimatedDuration(day1.getEstimatedDuration())
                .timeBlocks(day1.getTimeBlocks())
                .build();

            dailyItineraries.set(0, updatedDay1);
            log.info("Added departure location '{}' to Day 1", departureLocation);
        }

        // 4. 경로 최적화 (출발지 정보 포함)
        List<OptimizedRoute> optimizedRoutes = optimizeRoutes(
            dailyItineraries,
            input.transportMode(),
            departureLocation
        );

        Stage3Output output = Stage3Output.builder()
            .dailyItineraries(dailyItineraries)
            .optimizedRoutes(optimizedRoutes)
            .totalDistance(calculateTotalDistance(optimizedRoutes))
            .totalDuration(calculateTotalDuration(optimizedRoutes))
            .generatedAt(LocalDateTime.now())
            .build();

        // 프론트엔드 콘솔용 상세 로깅
        logDetailedItineraryForFrontend(output);

        return output;
    }

    // TravelContext에서 목적지 추출
    @SuppressWarnings("unchecked")
    private String extractDestination(TravelContext context) {
        Object destinations = context.getCollectedInfo().get(TravelContext.KEY_DESTINATIONS);
        if (destinations instanceof List) {
            List<String> destList = (List<String>) destinations;
            return !destList.isEmpty() ? destList.get(0) : null;
        }
        return destinations != null ? destinations.toString() : null;
    }

    // travel_candidates에서 enriched 데이터 조회
    private List<TravelCandidate> fetchEnrichedCandidates(
            String destination,
            LocalDate startDate,
            LocalDate endDate) {

        // Google Places Enhanced 데이터가 있는 장소들 우선 조회
        List<TravelCandidate> candidates = travelCandidateRepository
            .findByRegionAndIsActiveTrue(destination)
            .stream()
            .filter(c -> c.getGooglePlaceId() != null) // Google Places 데이터가 있는 것만
            .filter(c -> c.getQualityScore() >= 0.6) // 품질 점수 임계값
            .sorted((a, b) -> {
                // 품질 점수로 정렬
                int scoreCompare = Double.compare(
                    b.getQualityScore(),
                    a.getQualityScore()
                );
                if (scoreCompare != 0) return scoreCompare;

                // 리뷰 수로 정렬
                return Integer.compare(
                    b.getReviewCount() != null ? b.getReviewCount() : 0,
                    a.getReviewCount() != null ? a.getReviewCount() : 0
                );
            })
            .limit(45) // 더 현실적인 수: 일당 15개 * 3일
            .collect(Collectors.toList());

        log.info("Fetched {} enriched candidates for {}", candidates.size(), destination);
        return candidates;
    }

    // 날짜별 일정 생성 (문서 요구사항 반영)
    private List<DailyItinerary> createDailyItineraries(
            List<TravelCandidate> candidates,
            List<TravelPlace> scoredPlaces,
            LocalDate startDate,
            LocalDate endDate,
            List<SelectedSchedule> userSelected,
            String travelStyle) {

        log.info("=== Creating daily itineraries ===");
        log.info("Candidates: {}, ScoredPlaces: {}, UserSelected: {}, Days: {} to {}",
                candidates.size(), scoredPlaces.size(),
                userSelected != null ? userSelected.size() : 0,
                startDate, endDate);

        List<DailyItinerary> itineraries = new ArrayList<>();
        long days = endDate.toEpochDay() - startDate.toEpochDay() + 1;

        // 1. 사용자 선택 장소들을 K-means로 클러스터링
        List<TravelPlace> userPlaces = convertAllUserSelectedPlaces(userSelected);
        log.info("User selected places count: {}, userSelected input count: {}",
                userPlaces.size(), userSelected != null ? userSelected.size() : 0);

        // 각 사용자 선택 장소 로깅
        for (TravelPlace place : userPlaces) {
            log.info("User place: {} (id: {}, lat: {}, lon: {})",
                    place.getName(), place.getPlaceId(), place.getLatitude(), place.getLongitude());
        }

        if (userPlaces.isEmpty()) {
            log.warn("No user selected places found. Creating AI-only itineraries.");

            // 사용자 선택 장소가 없어도 AI 추천으로 일정 생성
            Set<String> globalUsedPlaceIds = new HashSet<>();

            for (int day = 0; day < days; day++) {
                LocalDate currentDate = startDate.plusDays(day);

                // 목적지 후보 장소들의 평균 좌표를 기본 클러스터 중심으로 사용
                double avgLat = candidates.stream()
                    .mapToDouble(c -> c.getLatitude() != null ? c.getLatitude() : 37.5665)
                    .average().orElse(37.5665);
                double avgLng = candidates.stream()
                    .mapToDouble(c -> c.getLongitude() != null ? c.getLongitude() : 126.9780)
                    .average().orElse(126.9780);
                Stage3KMeansClusteringService.ClusterCenter defaultCenter =
                    new Stage3KMeansClusteringService.ClusterCenter(0, avgLat, avgLng, 1);
                List<Stage3KMeansClusteringService.ClusterCenter> centers = List.of(defaultCenter);

                // AI 추천 장소 검색 (시간 블록당 최소 1개씩, 총 4-6개)
                List<TravelPlace> aiRecommended = searchNearbyPlacesWithGlobalTracking(
                    candidates,
                    centers,
                    globalUsedPlaceIds,
                    travelStyle,
                    scoredPlaces
                );

                // 부족하면 추가 검색
                if (aiRecommended.size() < 4) {
                    List<TravelPlace> additional = searchAdditionalPlaces(
                        candidates,
                        scoredPlaces,
                        globalUsedPlaceIds,
                        4 - aiRecommended.size()
                    );
                    aiRecommended.addAll(additional);
                }

                // 시간 블록에 따른 일정 배치
                List<TravelPlace> arrangedPlaces = arrangeByDetailedTimeBlocks(
                    aiRecommended, "09:00", "21:00", currentDate
                );

                // 출발지는 상위 메소드(processWithConfirmedSchedules)에서 처리

                DailyItinerary itinerary = DailyItinerary.builder()
                    .date(currentDate)
                    .dayNumber(day + 1)
                    .places(arrangedPlaces)
                    .estimatedDuration(calculateDayDuration(arrangedPlaces))
                    .timeBlocks(createTimeBlocks(arrangedPlaces))
                    .build();

                itineraries.add(itinerary);

                log.info("Created AI-only itinerary for Day {}: {} places", day + 1, arrangedPlaces.size());
            }
            return itineraries;
        }

        int k = Math.min((userPlaces.size() + 1) / 2, 3); // K = min(사용자선택장소수/2, 3)
        Map<Integer, List<TravelPlace>> userClusters = kMeansClusteringService.clusterPlaces(userPlaces, k);

        // 2. 클러스터 중심점 계산
        List<Stage3KMeansClusteringService.ClusterCenter> clusterCenters =
            kMeansClusteringService.getClusterCenters(userClusters);

        // 3. 날짜별 클러스터 할당
        Stage3KMeansClusteringService.ClusterAssignmentResult assignmentResult =
            kMeansClusteringService.assignClustersTodays(userClusters, (int) days);
        Map<Integer, List<TravelPlace>> dayAssignments = assignmentResult.getDayAssignments();
        Map<Integer, Integer> clusterToDayMap = assignmentResult.getClusterToDayMap();

        // 전체 여행 기간 동안 사용된 장소 추적
        Set<String> globalUsedPlaceIds = new HashSet<>();

        // 사용자 선택 장소들을 실제로 날짜에 배치할 때 중복 방지
        Map<Integer, List<TravelPlace>> actualDayAssignments = new HashMap<>();

        for (int day = 0; day < days; day++) {
            List<TravelPlace> plannedDayPlaces = dayAssignments.getOrDefault(day, new ArrayList<>());
            List<TravelPlace> dayUserPlaces = new ArrayList<>();

            // 이미 사용된 장소는 제외하고 추가
            for (TravelPlace place : plannedDayPlaces) {
                String placeKey = place.getName() != null ?
                    place.getName().toLowerCase().replaceAll("\\s+", "") :
                    place.getPlaceId();

                if (placeKey != null && !globalUsedPlaceIds.contains(placeKey)) {
                    dayUserPlaces.add(place);
                    globalUsedPlaceIds.add(placeKey);

                    // ID와 이름 모두 추적
                    if (place.getPlaceId() != null) {
                        globalUsedPlaceIds.add(place.getPlaceId());
                    }
                    if (place.getName() != null) {
                        globalUsedPlaceIds.add(place.getName());
                        globalUsedPlaceIds.add(place.getName().toLowerCase().replaceAll("\\s+", ""));
                    }
                }
            }

            actualDayAssignments.put(day, dayUserPlaces);

            // AI 추천 전에 위에서 이미 처리 완료
        }

        // 이제 실제로 날짜별로 처리
        for (int day = 0; day < days; day++) {
            LocalDate currentDate = startDate.plusDays(day);
            List<TravelPlace> dayUserPlaces = actualDayAssignments.getOrDefault(day, new ArrayList<>());

            // 4. 각 클러스터 중심점 주변에서 AI 추천 장소 검색
            List<Stage3KMeansClusteringService.ClusterCenter> centersForDay = filterCentersByDay(
                clusterCenters,
                clusterToDayMap,
                day
            );

            List<TravelPlace> aiRecommended = searchNearbyPlacesWithGlobalTracking(
                candidates,
                centersForDay,
                globalUsedPlaceIds,
                travelStyle,
                scoredPlaces
            );

            // 5. 사용자 선택 + AI 추천 장소 통합
            List<TravelPlace> allDayPlaces = new ArrayList<>();
            allDayPlaces.addAll(dayUserPlaces);
            allDayPlaces.addAll(aiRecommended);

            // 6. 시간 블록에 따른 일정 배치
            List<TravelPlace> arrangedPlaces = arrangeByDetailedTimeBlocks(
                allDayPlaces, "09:00", "21:00", currentDate
            );

            // 숙소 정보는 향후 사용자 입력 기반으로 처리 예정

            DailyItinerary itinerary = DailyItinerary.builder()
                .date(currentDate)
                .dayNumber(day + 1)
                .places(arrangedPlaces)
                .estimatedDuration(calculateDayDuration(arrangedPlaces))
                .timeBlocks(createTimeBlocks(arrangedPlaces))
                .build();

            itineraries.add(itinerary);
        }

        return itineraries;
    }

    // 모든 사용자 선택 장소 변환
    private List<TravelPlace> convertAllUserSelectedPlaces(List<SelectedSchedule> userSelected) {
        if (userSelected == null || userSelected.isEmpty()) {
            return new ArrayList<>();
        }

        return userSelected.stream()
            .map(s -> {
                log.info("🔍 [DEBUG] 사용자 선택 장소 변환: 이름={}, 주소={}, 좌표=({}, {})",
                    s.placeName(), s.address(), s.latitude(), s.longitude());

                TravelPlace place = TravelPlace.builder()
                    .placeId(s.placeId())
                    .name(s.placeName())
                    .category(s.category())
                    .latitude(s.latitude())
                    .longitude(s.longitude())
                    .address(s.address())
                    .rating(s.rating())
                    .isUserSelected(true)
                    .build();

                // 주소나 좌표가 없는 경우 DB에서 검색하여 보충
                if (needsEnrichment(place)) {
                    log.info("⚠️ [DEBUG] Enrichment 필요: {}", place.getName());
                    place = enrichPlaceFromDatabase(place);
                } else {
                    log.info("✅ [DEBUG] Enrichment 불필요 (주소와 좌표 모두 있음): {}", place.getName());
                }

                return place;
            })
            .collect(Collectors.toList());
    }

    // 장소 정보가 불완전한지 확인
    private boolean needsEnrichment(TravelPlace place) {
        return (place.getAddress() == null || place.getAddress().isEmpty()) ||
               (place.getLatitude() == null || place.getLongitude() == null);
    }

    // DB에서 장소 정보 보충
    private TravelPlace enrichPlaceFromDatabase(TravelPlace place) {
        try {
            // 이름으로 DB 검색
            List<TravelCandidate> candidates = travelCandidateRepository
                .findAll()
                .stream()
                .filter(c -> c.getName() != null &&
                            place.getName() != null &&
                            c.getName().toLowerCase().contains(place.getName().toLowerCase()))
                .limit(1)
                .collect(Collectors.toList());

            if (!candidates.isEmpty()) {
                TravelCandidate candidate = candidates.get(0);
                log.info("✅ DB에서 장소 정보 보충: {} → 주소: {}, 좌표: ({}, {})",
                    place.getName(), candidate.getAddress(),
                    candidate.getLatitude(), candidate.getLongitude());

                // 부족한 정보만 보충
                if (place.getAddress() == null || place.getAddress().isEmpty()) {
                    place.setAddress(candidate.getAddress());
                }
                if (place.getLatitude() == null || place.getLongitude() == null) {
                    place.setLatitude(candidate.getLatitude());
                    place.setLongitude(candidate.getLongitude());
                }
                if (place.getPlaceId() == null || place.getPlaceId().isEmpty()) {
                    place.setPlaceId(String.valueOf(candidate.getId()));
                }
                if (place.getRating() == null || place.getRating() == 0.0) {
                    place.setRating(candidate.getRating());
                }
            } else {
                log.warn("⚠️ DB에서 장소 정보를 찾을 수 없음: {}, 기본값 사용", place.getName());
                // 기본 좌표 설정 (서울 시청)
                if (place.getLatitude() == null || place.getLongitude() == null) {
                    place.setLatitude(37.5666805);
                    place.setLongitude(126.9784147);
                    log.info("📍 기본 좌표 적용: 서울 시청 (37.5666805, 126.9784147)");
                }
            }
        } catch (Exception e) {
            log.error("❌ DB 검색 중 오류 발생: {}", e.getMessage());
            // 실패해도 기본 좌표 설정
            if (place.getLatitude() == null || place.getLongitude() == null) {
                place.setLatitude(37.5666805);
                place.setLongitude(126.9784147);
            }
        }

        return place;
    }

    // 클러스터 중심점 주변에서 AI 추천 장소 검색 (전역 추적 포함 - 이름 기반)
    private List<TravelPlace> searchNearbyPlacesWithGlobalTracking(
            List<TravelCandidate> allCandidates,
            List<Stage3KMeansClusteringService.ClusterCenter> clusterCenters,
            Set<String> globalUsedPlaceIds,
            String travelStyle,
            List<TravelPlace> scoredFallback) {

        List<TravelPlace> recommended = new ArrayList<>();

        // 사용된 장소 이름들도 추적 (이름 기반 중복 방지 - 정규화)
        Set<String> usedPlaceNames = new HashSet<>();
        globalUsedPlaceIds.forEach(id -> {
            // ID가 실제 장소명일 수도 있음
            if (id != null && !id.startsWith("place_")) {
                // 공백을 제거하여 정규화
                usedPlaceNames.add(id.toLowerCase().replaceAll("\\s+", ""));
            }
        });

        for (Stage3KMeansClusteringService.ClusterCenter center : clusterCenters) {
            List<TravelPlace> nearbyPlaces = allCandidates.stream()
                .filter(candidate -> candidate.getLatitude() != null && candidate.getLongitude() != null)
                .filter(candidate -> {
                    // ID 기반 또는 이름 기반 중복 체크
                    if (candidate.getPlaceId() != null && globalUsedPlaceIds.contains(candidate.getPlaceId())) {
                        return false;
                    }
                    if (candidate.getName() != null) {
                        String normalizedName = candidate.getName().toLowerCase().replaceAll("\\s+", "");
                        if (globalUsedPlaceIds.contains(candidate.getName()) ||
                            usedPlaceNames.contains(normalizedName)) {
                            return false;
                        }

                        // 유사 장소명 체크
                        for (String usedName : usedPlaceNames) {
                            if (isSimilarPlace(normalizedName, usedName)) {
                                return false;
                            }
                        }
                    }
                    return true;
                })
                .filter(candidate -> candidate.getQualityScore() != null && candidate.getQualityScore() >= 0.6) // 낮춤
                .filter(candidate -> candidate.getRating() != null && candidate.getRating() >= 3.5) // 낮춤
                .filter(candidate -> matchesTravelStyle(candidate, travelStyle))
                .filter(candidate -> calculateDistance(
                    center.getLatitude(), center.getLongitude(),
                    candidate.getLatitude(), candidate.getLongitude()) < 10.0) // 거리 늘림
                .sorted((a, b) -> {
                    double distA = calculateDistance(
                        center.getLatitude(), center.getLongitude(),
                        a.getLatitude(), a.getLongitude());
                    double distB = calculateDistance(
                        center.getLatitude(), center.getLongitude(),
                        b.getLatitude(), b.getLongitude());

                    int distanceCompare = Double.compare(distA, distB);
                    if (distanceCompare != 0) {
                        return distanceCompare;
                    }

                    double qualityA = a.getQualityScore() != null ? a.getQualityScore() : 0.0;
                    double qualityB = b.getQualityScore() != null ? b.getQualityScore() : 0.0;
                    return Double.compare(qualityB, qualityA);
                })
                .limit(5) // 각 클러스터 중심점에서 더 많은 장소를 가져옴
                .map(candidate -> {
                    TravelPlace place = candidate.toTravelPlace();
                    place.setIsUserSelected(false);
                    return place;
                })
                .collect(Collectors.toList());

            if (nearbyPlaces.size() < 5 && scoredFallback != null) {
                List<TravelPlace> fallback = scoredFallback.stream()
                    .filter(place -> place.getPlaceId() != null || place.getName() != null)
                    .filter(place -> place.getLatitude() != null && place.getLongitude() != null)
                    .filter(place -> {
                        // ID 기반 또는 이름 기반 중복 체크
                        if (place.getPlaceId() != null && globalUsedPlaceIds.contains(place.getPlaceId())) {
                            return false;
                        }
                        if (place.getName() != null) {
                            String normalizedName = place.getName().toLowerCase().replaceAll("\\s+", "");
                            if (globalUsedPlaceIds.contains(place.getName()) ||
                                usedPlaceNames.contains(normalizedName)) {
                                return false;
                            }

                            // 유사 장소명 체크
                            for (String usedName : usedPlaceNames) {
                                if (isSimilarPlace(normalizedName, usedName)) {
                                    return false;
                                }
                            }
                        }
                        return true;
                    })
                    .filter(place -> matchesTravelStyle(place, travelStyle))
                    .filter(place -> calculateDistance(
                        center.getLatitude(), center.getLongitude(),
                        place.getLatitude(), place.getLongitude()) < 10.0) // fallback도 거리 늘림
                    .limit(5 - nearbyPlaces.size()) // fallback도 더 많이 가져옴
                    .collect(Collectors.toList());

                nearbyPlaces.addAll(fallback);
            }

            // 추가된 장소들을 전역 사용 목록에 추가 (ID와 이름 모두 추가)
            nearbyPlaces.forEach(place -> {
                if (place.getPlaceId() != null) {
                    globalUsedPlaceIds.add(place.getPlaceId());
                }
                if (place.getName() != null) {
                    globalUsedPlaceIds.add(place.getName());
                    usedPlaceNames.add(place.getName().toLowerCase().replaceAll("\\s+", ""));
                }
            });
            recommended.addAll(nearbyPlaces);
        }

        log.info("AI recommended {} places, global used places count: {}",
                recommended.size(), globalUsedPlaceIds.size());
        return recommended;
    }

    // 추가 장소 검색 (조건 완화)
    private List<TravelPlace> searchAdditionalPlaces(
            List<TravelCandidate> allCandidates,
            List<TravelPlace> scoredFallback,
            Set<String> globalUsedPlaceIds,
            int needed) {

        List<TravelPlace> additionalPlaces = new ArrayList<>();

        // 사용된 장소 이름들도 추적
        Set<String> usedPlaceNames = new HashSet<>();
        globalUsedPlaceIds.forEach(id -> {
            if (id != null && !id.startsWith("place_")) {
                usedPlaceNames.add(id.toLowerCase().replaceAll("\\s+", ""));
            }
        });

        // 조건을 완화하여 추가 장소 검색
        List<TravelPlace> candidates = allCandidates.stream()
            .filter(candidate -> candidate.getLatitude() != null && candidate.getLongitude() != null)
            .filter(candidate -> {
                // 중복 체크
                if (candidate.getPlaceId() != null && globalUsedPlaceIds.contains(candidate.getPlaceId())) {
                    return false;
                }
                if (candidate.getName() != null &&
                    (globalUsedPlaceIds.contains(candidate.getName()) ||
                     usedPlaceNames.contains(candidate.getName().toLowerCase().replaceAll("\\s+", "")))) {
                    return false;
                }
                return true;
            })
            .filter(candidate -> candidate.getQualityScore() != null && candidate.getQualityScore() >= 0.5) // 더 낮춤
            .filter(candidate -> candidate.getRating() != null && candidate.getRating() >= 3.0) // 더 낮춤
            .sorted((a, b) -> {
                // 품질 점수로 정렬
                double qualityA = a.getQualityScore() != null ? a.getQualityScore() : 0.0;
                double qualityB = b.getQualityScore() != null ? b.getQualityScore() : 0.0;
                return Double.compare(qualityB, qualityA);
            })
            .limit(needed)
            .map(candidate -> {
                TravelPlace place = candidate.toTravelPlace();
                place.setIsUserSelected(false);
                return place;
            })
            .collect(Collectors.toList());

        additionalPlaces.addAll(candidates);

        // 그래도 부족하면 scoredFallback에서 추가
        if (additionalPlaces.size() < needed && scoredFallback != null) {
            List<TravelPlace> fallback = scoredFallback.stream()
                .filter(place -> place.getPlaceId() != null || place.getName() != null)
                .filter(place -> {
                    if (place.getPlaceId() != null && globalUsedPlaceIds.contains(place.getPlaceId())) {
                        return false;
                    }
                    if (place.getName() != null &&
                        (globalUsedPlaceIds.contains(place.getName()) ||
                         usedPlaceNames.contains(place.getName().toLowerCase().replaceAll("\\s+", "")))) {
                        return false;
                    }
                    return true;
                })
                .limit(needed - additionalPlaces.size())
                .collect(Collectors.toList());

            additionalPlaces.addAll(fallback);
        }

        // 추가된 장소들을 전역 사용 목록에 추가
        additionalPlaces.forEach(place -> {
            if (place.getPlaceId() != null) {
                globalUsedPlaceIds.add(place.getPlaceId());
            }
            if (place.getName() != null) {
                globalUsedPlaceIds.add(place.getName());
                usedPlaceNames.add(place.getName().toLowerCase().replaceAll("\\s+", ""));
            }
        });

        log.info("Added {} additional places to fill minimum requirement", additionalPlaces.size());
        return additionalPlaces;
    }

    // 기존 메서드는 다른 곳에서 사용될 수 있으므로 유지
    private List<TravelPlace> searchNearbyPlaces(
            List<TravelCandidate> allCandidates,
            List<Stage3KMeansClusteringService.ClusterCenter> clusterCenters,
            List<TravelPlace> userPlaces,
            String travelStyle,
            List<TravelPlace> scoredFallback) {

        Set<String> usedPlaceIds = userPlaces.stream()
            .map(TravelPlace::getPlaceId)
            .collect(Collectors.toCollection(HashSet::new));

        return searchNearbyPlacesWithGlobalTracking(
            allCandidates,
            clusterCenters,
            usedPlaceIds,
            travelStyle,
            scoredFallback
        );
    }

    // Haversine 공식으로 거리 계산
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

    // 유사 장소명 체크 메서드 - 더 강력한 중복 검사
    private boolean isSimilarPlace(String name1, String name2) {
        if (name1 == null || name2 == null) return false;

        // 빠른 체크: 정규화된 이름이 같은지
        String normalized1 = name1.toLowerCase().replaceAll("\\s+", "");
        String normalized2 = name2.toLowerCase().replaceAll("\\s+", "");
        if (normalized1.equals(normalized2)) {
            log.debug("Exact match after normalization: {} == {}", name1, name2);
            return true;
        }

        // 핵심 장소명 추출하여 비교
        String core1 = extractCorePlaceName(name1);
        String core2 = extractCorePlaceName(name2);

        // 빈 문자열 체크
        if (core1.isEmpty() || core2.isEmpty()) {
            return false;
        }

        // 핵심 장소명이 같으면 유사한 장소로 판단
        if (core1.equals(core2)) {
            log.debug("Similar places detected: '{}' (core: {}) == '{}' (core: {})", name1, core1, name2, core2);
            return true;
        }

        // 길이가 충분히 긴 경우에만 포함 관계 체크 (너무 짧은 단어는 오탐 가능성)
        if (core1.length() >= 3 && core2.length() >= 3) {
            if (core1.contains(core2) || core2.contains(core1)) {
                log.debug("Containment detected: '{}' contains '{}' or vice versa", core1, core2);
                return true;
            }
        }

        return false;
    }

    // 장소명에서 핵심 이름만 추출하는 메서드
    private String extractCorePlaceName(String name) {
        if (name == null) return "";

        String cleaned = name.toLowerCase();

        // 1단계: 변형들을 먼저 통일 (공백이 있는 상태에서)
        // N서울타워 계열
        cleaned = cleaned.replaceAll("(n서울타워|남산서울타워|남산 서울타워|남산타워|남산 타워|n 서울타워)", "서울타워");

        // 롯데월드 계열
        cleaned = cleaned.replaceAll("(롯데월드타워|롯데타워|롯데 타워|롯데월드어드벤처|롯데월드 어드벤처|롯데 월드)", "롯데월드");

        // 국립중앙박물관 계열
        cleaned = cleaned.replaceAll("(국립중앙박물관|국립 중앙 박물관|중앙박물관|중앙 박물관)", "국립중앙박물관");

        // 2단계: 수식어구 제거 (공백 포함 패턴)
        cleaned = cleaned.replaceAll("\\s*(야경|야간개장|전망대|플라자|어드벤처|야간|주간|특별|이벤트|입장|체험|관람|투어|방문)\\s*", " ");

        // 3단계: 연속된 공백을 단일 공백으로 정리
        cleaned = cleaned.replaceAll("\\s+", " ").trim();

        // 4단계: 최종적으로 공백 제거
        cleaned = cleaned.replaceAll("\\s+", "");

        return cleaned;
    }

    // 상세 시간 블록별 장소 배치 (문서 요구사항 반영)
    private List<TravelPlace> arrangeByDetailedTimeBlocks(
            List<TravelPlace> places,
            String startTime,
            String endTime,
            LocalDate date) {

        // 시간 블록 정의
        Map<String, List<String>> timeBlockCategories = Map.of(
            "아침", List.of("카페", "cafe", "breakfast", "조식", "빵집", "bakery"),
            "오전", List.of("관광지", "attraction", "museum", "박물관", "궁전", "공원", "park"),
            "점심", List.of("맛집", "restaurant", "식당", "음식점"),
            "오후", List.of("관광지", "쇼핑", "shopping", "시장", "market", "체험"),
            "저녁", List.of("맛집", "restaurant", "저녁식사", "dinner"),
            "야간", List.of("야경", "bar", "nightlife", "클럽", "펍")
        );

        // 사용자 선택 장소와 AI 추천 장소 분리
        List<TravelPlace> userSelected = places.stream()
            .filter(p -> Boolean.TRUE.equals(p.getIsUserSelected()))
            .collect(Collectors.toList());
        List<TravelPlace> aiRecommended = places.stream()
            .filter(p -> !Boolean.TRUE.equals(p.getIsUserSelected()))
            .collect(Collectors.toList());

        List<TravelPlace> arranged = new ArrayList<>();

        // 시간 블록별 처리
        String[] timeBlocks = {"아침", "오전", "점심", "오후", "저녁", "야간"};
        for (String block : timeBlocks) {
            // 사용자 선택 우선 배치
            List<String> criteria = timeBlockCategories.getOrDefault(block, List.of());
            List<TravelPlace> blockUserPlaces = filterByTimeBlock(userSelected, criteria);
            arranged.addAll(blockUserPlaces);
            userSelected.removeAll(blockUserPlaces);

            // AI 추천 보충 (블록당 최대 2개)
            if (blockUserPlaces.size() < 2) {
                List<TravelPlace> blockAiPlaces = filterByTimeBlock(aiRecommended, criteria);
                int toAdd = Math.min(2 - blockUserPlaces.size(), blockAiPlaces.size());
                arranged.addAll(blockAiPlaces.subList(0, toAdd));
                aiRecommended.removeAll(blockAiPlaces.subList(0, toAdd));
            }
        }

        // 남은 장소 추가 (시간 블록에 맞지 않는 장소들)
        arranged.addAll(userSelected);
        if (!aiRecommended.isEmpty()) {
            arranged.addAll(aiRecommended.subList(0, Math.min(3, aiRecommended.size())));
        }

        return arranged;
    }

    // 시간 블록별 필터링
    private List<TravelPlace> filterByTimeBlock(List<TravelPlace> places, List<String> categories) {
        return places.stream()
            .filter(p -> {
                String category = p.getCategory() != null ? p.getCategory().toLowerCase() : "";
                String name = p.getName() != null ? p.getName().toLowerCase() : "";
                return categories.stream().anyMatch(c ->
                    category.contains(c.toLowerCase()) || name.contains(c.toLowerCase())
                );
            })
            .collect(Collectors.toList());
    }

    // 시간 블록 정보 생성 (개선된 버전 - 중복 완전 제거)
    private Map<String, List<TravelPlace>> createTimeBlocks(List<TravelPlace> places) {
        // 사용자 선택 장소와 AI 추천 장소 분리
        List<TravelPlace> userSelected = places.stream()
            .filter(p -> p.getIsUserSelected() != null && p.getIsUserSelected())
            .collect(Collectors.toList());

        List<TravelPlace> aiRecommended = places.stream()
            .filter(p -> p.getIsUserSelected() == null || !p.getIsUserSelected())
            .collect(Collectors.toList());

        // 시간 블록별 분배를 위한 기본 구조
        Map<String, List<TravelPlace>> timeBlocks = new LinkedHashMap<>();
        timeBlocks.put("09:00-12:00", new ArrayList<>());
        timeBlocks.put("12:00-15:00", new ArrayList<>());
        timeBlocks.put("15:00-18:00", new ArrayList<>());
        timeBlocks.put("18:00-21:00", new ArrayList<>());

        Map<String, List<TravelPlace>> result;

        // 카테고리 기반 스마트 분배 사용 가능 여부 체크
        if (timeBlockRecommendationService != null) {
            try {
                // AI 추천 장소를 시간대별로 적절히 분류
                Map<String, List<TravelPlace>> aiByTimeBlock = categorizeByTimeBlock(aiRecommended);

                // 사용자 선택 장소와 AI 추천 장소를 시간대별로 조합
                result = timeBlockRecommendationService.distributeWithUserSelection(
                    userSelected,
                    aiByTimeBlock
                );
            } catch (Exception e) {
                log.warn("Failed to use smart time block distribution, falling back to simple distribution", e);
                result = createSimpleTimeBlocks(places);
            }
        } else {
            // 폴백: 기존의 단순 분배 로직
            result = createSimpleTimeBlocks(places);
        }

        // 최종 중복 제거 및 검증
        return removeDuplicatesFromTimeBlocks(result);
    }

    // 단순 시간 블록 분배 (폴백용)
    private Map<String, List<TravelPlace>> createSimpleTimeBlocks(List<TravelPlace> places) {
        Map<String, List<TravelPlace>> timeBlocks = new LinkedHashMap<>();
        timeBlocks.put("09:00-12:00", new ArrayList<>());
        timeBlocks.put("12:00-15:00", new ArrayList<>());
        timeBlocks.put("15:00-18:00", new ArrayList<>());
        timeBlocks.put("18:00-21:00", new ArrayList<>());

        int index = 0;
        for (String block : timeBlocks.keySet()) {
            if (index < places.size()) {
                timeBlocks.get(block).add(places.get(index++));
            }
        }

        if (index < places.size()) {
            int blockIndex = 0;
            String[] blocks = timeBlocks.keySet().toArray(new String[0]);
            while (index < places.size()) {
                String block = blocks[blockIndex % 4];
                if (timeBlocks.get(block).size() < 2) {
                    timeBlocks.get(block).add(places.get(index++));
                }
                blockIndex++;
                if (blockIndex % 4 == 0) {
                    boolean allBlocksFull = timeBlocks.values().stream()
                        .allMatch(blockPlaces -> blockPlaces.size() >= 2);
                    if (allBlocksFull) break;
                }
            }
        }
        return timeBlocks;
    }

    // AI 추천 장소를 시간대별로 카테고리 분류
    private Map<String, List<TravelPlace>> categorizeByTimeBlock(List<TravelPlace> places) {
        Map<String, List<TravelPlace>> categorized = new LinkedHashMap<>();
        categorized.put("09:00-12:00", new ArrayList<>());
        categorized.put("12:00-15:00", new ArrayList<>());
        categorized.put("15:00-18:00", new ArrayList<>());
        categorized.put("18:00-21:00", new ArrayList<>());

        // 시간대별 카테고리 매칭
        Map<String, List<String>> timeBlockCategories = Map.of(
            "09:00-12:00", List.of("카페", "관광지", "고궁", "박물관", "공원"),
            "12:00-15:00", List.of("맛집", "식당", "쇼핑", "시장"),
            "15:00-18:00", List.of("관광지", "체험", "테마파크", "쇼핑"),
            "18:00-21:00", List.of("맛집", "야경", "전망대", "야간개장")
        );

        // 중복 방지: placeId 또는 정규화된 name 사용
        Set<String> usedPlaceIds = new HashSet<>();

        // 각 시간대별로 적합한 장소 찾기
        for (Map.Entry<String, List<String>> entry : timeBlockCategories.entrySet()) {
            String timeBlock = entry.getKey();
            List<String> categories = entry.getValue();

            List<TravelPlace> matching = places.stream()
                .filter(p -> {
                    String placeKey = getPlaceKey(p);
                    return !usedPlaceIds.contains(placeKey);
                })
                .filter(p -> matchesCategories(p, categories))
                .limit(2)  // 블록당 최대 2개
                .collect(Collectors.toList());

            categorized.get(timeBlock).addAll(matching);
            // 추가된 장소 ID 저장
            matching.forEach(p -> usedPlaceIds.add(getPlaceKey(p)));
        }

        // 남은 장소들을 빈 블록 우선으로 분배
        List<TravelPlace> remaining = places.stream()
            .filter(p -> {
                String placeKey = getPlaceKey(p);
                return !usedPlaceIds.contains(placeKey);
            })
            .collect(Collectors.toList());

        // 빈 블록 우선 채우기
        for (TravelPlace place : remaining) {
            // 장소가 0개인 블록 찾기
            Optional<String> emptyBlock = categorized.entrySet().stream()
                .filter(e -> e.getValue().isEmpty())
                .map(Map.Entry::getKey)
                .findFirst();

            if (emptyBlock.isPresent()) {
                categorized.get(emptyBlock.get()).add(place);
                usedPlaceIds.add(getPlaceKey(place));
                continue;
            }

            // 빈 블록이 없으면 1개만 있는 블록에 추가 (최대 2개까지)
            Optional<String> singleBlock = categorized.entrySet().stream()
                .filter(e -> e.getValue().size() == 1)
                .map(Map.Entry::getKey)
                .findFirst();

            if (singleBlock.isPresent()) {
                categorized.get(singleBlock.get()).add(place);
                usedPlaceIds.add(getPlaceKey(place));
            }
        }

        log.info("Categorized places: {}",
            categorized.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue().size())
                .collect(Collectors.joining(", ")));

        return categorized;
    }

    // 장소 고유 키 생성 (중복 방지용)
    private String getPlaceKey(TravelPlace place) {
        if (place.getPlaceId() != null && !place.getPlaceId().isEmpty()) {
            return place.getPlaceId();
        }
        if (place.getName() != null && !place.getName().isEmpty()) {
            // 정규화된 이름 사용 (공백 제거, 소문자)
            return extractCorePlaceName(place.getName());
        }
        // 최후의 수단: 좌표 기반
        if (place.getLatitude() != null && place.getLongitude() != null) {
            return String.format("%.6f_%.6f", place.getLatitude(), place.getLongitude());
        }
        return UUID.randomUUID().toString();
    }

    // 시간 블록에서 중복 제거 및 빈 공간 채우기
    private Map<String, List<TravelPlace>> removeDuplicatesFromTimeBlocks(
            Map<String, List<TravelPlace>> timeBlocks) {

        log.info("=== Removing duplicates from time blocks ===");

        // 전역적으로 사용된 장소 추적 (정규화된 키 사용)
        Set<String> globalUsedKeys = new HashSet<>();
        Map<String, List<TravelPlace>> cleanedBlocks = new LinkedHashMap<>();

        // 각 시간 블록별로 중복 제거
        for (Map.Entry<String, List<TravelPlace>> entry : timeBlocks.entrySet()) {
            String timeBlock = entry.getKey();
            List<TravelPlace> places = entry.getValue();

            List<TravelPlace> uniquePlaces = new ArrayList<>();

            for (TravelPlace place : places) {
                String placeKey = getPlaceKey(place);

                // 이미 사용된 장소인지 확인
                if (!globalUsedKeys.contains(placeKey)) {
                    uniquePlaces.add(place);
                    globalUsedKeys.add(placeKey);

                    // 추가 추적: ID와 이름 모두
                    if (place.getPlaceId() != null) {
                        globalUsedKeys.add(place.getPlaceId());
                    }
                    if (place.getName() != null) {
                        globalUsedKeys.add(place.getName());
                        globalUsedKeys.add(extractCorePlaceName(place.getName()));
                    }
                } else {
                    log.debug("Duplicate detected in {}: {} (key: {})", timeBlock, place.getName(), placeKey);
                }
            }

            cleanedBlocks.put(timeBlock, uniquePlaces);
            log.info("Time block {}: {} places after deduplication", timeBlock, uniquePlaces.size());
        }

        // 빈 블록 채우기 (최소 1개씩)
        return fillEmptyTimeBlocks(cleanedBlocks, globalUsedKeys);
    }

    // 빈 시간 블록 채우기
    private Map<String, List<TravelPlace>> fillEmptyTimeBlocks(
            Map<String, List<TravelPlace>> timeBlocks,
            Set<String> globalUsedKeys) {

        log.info("=== Filling empty time blocks ===");

        // 빈 블록 찾기
        List<String> emptyBlocks = timeBlocks.entrySet().stream()
            .filter(e -> e.getValue().isEmpty())
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        if (emptyBlocks.isEmpty()) {
            log.info("No empty blocks found");
            return timeBlocks;
        }

        log.info("Found {} empty blocks: {}", emptyBlocks.size(), emptyBlocks);

        // 다른 블록에서 여유 장소 재분배
        List<TravelPlace> sparePlaces = new ArrayList<>();

        // 2개 이상 있는 블록에서 여유 장소 수집
        for (Map.Entry<String, List<TravelPlace>> entry : timeBlocks.entrySet()) {
            List<TravelPlace> places = entry.getValue();
            if (places.size() > 2) {
                // 2개를 초과하는 장소들을 여유 장소로 수집
                List<TravelPlace> extras = places.subList(2, places.size());
                sparePlaces.addAll(new ArrayList<>(extras));
                // 원본 리스트에서는 2개만 유지
                entry.setValue(new ArrayList<>(places.subList(0, 2)));
            }
        }

        log.info("Collected {} spare places for redistribution", sparePlaces.size());

        // 빈 블록에 여유 장소 배분
        int spareIndex = 0;
        for (String emptyBlock : emptyBlocks) {
            if (spareIndex < sparePlaces.size()) {
                List<TravelPlace> blockPlaces = timeBlocks.get(emptyBlock);
                blockPlaces.add(sparePlaces.get(spareIndex));
                log.info("Filled empty block {} with place: {}", emptyBlock, sparePlaces.get(spareIndex).getName());
                spareIndex++;
            } else {
                log.warn("Not enough spare places to fill all empty blocks");
                break;
            }
        }

        // 여전히 빈 블록이 있으면 경고
        long stillEmpty = timeBlocks.values().stream()
            .filter(List::isEmpty)
            .count();

        if (stillEmpty > 0) {
            log.warn("Still {} empty blocks remaining after redistribution", stillEmpty);
        } else {
            log.info("✅ All time blocks filled successfully");
        }

        return timeBlocks;
    }

    // 카테고리 매칭 헬퍼 메서드
    private boolean matchesCategories(TravelPlace place, List<String> categories) {
        if (place.getCategory() == null && place.getName() == null) {
            return false;
        }

        String placeCategory = place.getCategory() != null ?
            place.getCategory().toLowerCase() : "";
        String placeName = place.getName() != null ?
            place.getName().toLowerCase() : "";

        return categories.stream().anyMatch(cat ->
            placeCategory.contains(cat.toLowerCase()) ||
            placeName.contains(cat.toLowerCase())
        );
    }

    // 사용자 선택 장소 변환
    private List<TravelPlace> convertUserSelectedPlaces(
            List<SelectedSchedule> userSelected,
            LocalDate date) {
        return userSelected.stream()
            .filter(s -> s.scheduledDateTime() == null ||
                        s.scheduledDateTime().toLocalDate().equals(date))
            .map(s -> TravelPlace.builder()
                .placeId(s.placeId())
                .name(s.placeName())
                .category(s.category())
                .latitude(s.latitude())
                .longitude(s.longitude())
                .address(s.address())
                .rating(s.rating())
                .isUserSelected(true)
                .build())
            .collect(Collectors.toList());
    }

    // 경로 최적화 (출발지 정보 포함)
    private List<OptimizedRoute> optimizeRoutes(
            List<DailyItinerary> itineraries,
            String transportMode,
            String departureLocation) {

        return itineraries.stream()
            .map(itinerary -> routeOptimizationService.optimize(
                itinerary.getPlaces(),
                transportMode,
                departureLocation
            ))
            .collect(Collectors.toList());
    }

    // 전체 거리 계산
    private double calculateTotalDistance(List<OptimizedRoute> routes) {
        return routes.stream()
            .mapToDouble(OptimizedRoute::getTotalDistance)
            .sum();
    }

    // 프론트엔드 콘솔용 상세 로깅
    private void logDetailedItineraryForFrontend(Stage3Output output) {
        log.info("===========================================");
        log.info("🎯 [Stage 3] 완성된 여행 일정 상세 정보");
        log.info("===========================================");
        log.info("📅 총 {}일 일정", output.getDailyItineraries().size());
        log.info("🚗 총 이동 거리: {}km", String.format("%.2f", output.getTotalDistance()));
        log.info("⏱️ 총 이동 시간: {}분", output.getTotalDuration());

        int dayCounter = 1;
        for (DailyItinerary day : output.getDailyItineraries()) {
            log.info("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("📆 Day {}: {}", day.getDayNumber(), day.getDate());
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            if (day.getTimeBlocks() != null && !day.getTimeBlocks().isEmpty()) {
                day.getTimeBlocks().forEach((timeSlot, placesList) -> {
                    log.info("\n⏰ 시간대: {}", timeSlot);
                    log.info("───────────────────────────────────");

                    placesList.forEach(place -> {
                        String marker = (place.getIsUserSelected() != null && place.getIsUserSelected()) ? "🎯" : "🤖";
                        log.info("  {} {} [{}]", marker, place.getName(), place.getCategory());
                        log.info("     📍 주소: {}", place.getAddress());
                        if (place.getRating() != null) {
                            log.info("     ⭐ 평점: {}", place.getRating());
                        }
                        if (place.getDescription() != null && !place.getDescription().isEmpty()) {
                            log.info("     📝 설명: {}", place.getDescription());
                        }
                        log.info("     📊 품질 점수: {}", String.format("%.2f", place.getQualityScore()));
                    });
                });
            }

            // 경로 최적화 정보
            final int currentDay = dayCounter;
            OptimizedRoute route = output.getOptimizedRoutes().stream()
                .filter(r -> output.getOptimizedRoutes().indexOf(r) == currentDay - 1)
                .findFirst()
                .orElse(null);

            if (route != null) {
                log.info("\n🗺️ 경로 최적화 정보:");
                log.info("  - 총 이동거리: {}km", String.format("%.2f", route.getTotalDistance()));
                log.info("  - 총 이동시간: {}분", route.getTotalDuration());
                if (route.getPlaces() != null && !route.getPlaces().isEmpty()) {
                    String routeStr = route.getPlaces().stream()
                        .map(TravelPlace::getName)
                        .reduce((a, b) -> a + " → " + b)
                        .orElse("");
                    log.info("  - 경로: {}", routeStr);
                }
            }

            dayCounter++;
        }

        log.info("\n===========================================");
        log.info("✅ Stage 3 여행 일정 생성 완료");
        log.info("  - 사용자 선택 장소: 🎯");
        log.info("  - AI 추천 장소: 🤖");
        log.info("===========================================\n");
    }

    // 전체 소요 시간 계산
    private long calculateTotalDuration(List<OptimizedRoute> routes) {
        return routes.stream()
            .mapToLong(OptimizedRoute::getTotalDuration)
            .sum();
    }

    // 일일 소요 시간 계산
    private long calculateDayDuration(List<TravelPlace> places) {
        // 각 장소당 평균 2시간 + 이동시간 30분
        return places.size() * 150L; // 분 단위
    }

    // OCR 확정 일정을 고려한 날짜별 일정 생성
    private List<DailyItinerary> createDailyItinerariesWithConfirmedSchedules(
            List<TravelCandidate> candidates,
            List<TravelPlace> scoredPlaces,
            LocalDate startDate,
            LocalDate endDate,
            List<SelectedSchedule> userSelected,
            List<ConfirmedSchedule> confirmedSchedules,
            String travelStyle) {

        List<DailyItinerary> itineraries = new ArrayList<>();
        long days = endDate.toEpochDay() - startDate.toEpochDay() + 1;

        // K-means 클러스터링으로 지역별 그룹화
        List<TravelPlace> userPlaces = convertAllUserSelectedPlaces(userSelected);
        int k = Math.min((userPlaces.size() + 1) / 2, 3);
        Map<Integer, List<TravelPlace>> clusteredPlaces = kMeansClusteringService.clusterPlaces(userPlaces, k);
        Stage3KMeansClusteringService.ClusterAssignmentResult assignmentResult =
            kMeansClusteringService.assignClustersTodays(clusteredPlaces, (int) days);
        Map<Integer, List<TravelPlace>> dayAssignments = assignmentResult.getDayAssignments();
        Map<Integer, Integer> clusterToDayMap = assignmentResult.getClusterToDayMap();
        List<Stage3KMeansClusteringService.ClusterCenter> clusterCenters =
            kMeansClusteringService.getClusterCenters(clusteredPlaces);

        // 전체 여행 기간 동안 사용된 장소 추적
        Set<String> globalUsedPlaceIds = new HashSet<>();

        // 사용자 선택 장소들을 실제로 날짜에 배치할 때 중복 방지
        Map<Integer, List<TravelPlace>> actualDayAssignments = new HashMap<>();

        for (int day = 0; day < days; day++) {
            List<TravelPlace> plannedDayPlaces = dayAssignments.getOrDefault(day, new ArrayList<>());
            List<TravelPlace> dayUserPlaces = new ArrayList<>();

            // 이미 사용된 장소는 제외하고 추가
            for (TravelPlace place : plannedDayPlaces) {
                String placeKey = place.getName() != null ?
                    place.getName().toLowerCase().replaceAll("\\s+", "") :
                    place.getPlaceId();

                if (placeKey != null && !globalUsedPlaceIds.contains(placeKey)) {
                    dayUserPlaces.add(place);
                    globalUsedPlaceIds.add(placeKey);
                    // ID와 이름 모두 추적
                    if (place.getPlaceId() != null) {
                        globalUsedPlaceIds.add(place.getPlaceId());
                    }
                    if (place.getName() != null) {
                        globalUsedPlaceIds.add(place.getName());
                        globalUsedPlaceIds.add(place.getName().toLowerCase().replaceAll("\\s+", ""));
                    }
                }
            }
            actualDayAssignments.put(day, dayUserPlaces);
        }

        for (int day = 0; day < days; day++) {
            LocalDate currentDate = startDate.plusDays(day);
            // actualDayAssignments를 사용하여 중복이 제거된 사용자 선택 장소들 가져오기
            List<TravelPlace> dayPlaces = new ArrayList<>(actualDayAssignments.getOrDefault(day, new ArrayList<>()));

            // 현재 날짜의 사용자 선택 장소들을 전역 사용 목록에 추가 (ID와 이름 모두)
            dayPlaces.forEach(place -> {
                if (place.getPlaceId() != null) {
                    globalUsedPlaceIds.add(place.getPlaceId());
                }
                if (place.getName() != null) {
                    globalUsedPlaceIds.add(place.getName());
                }
            });

            // OCR 확정 일정 추가 (항공, 호텔, 공연 등)
            List<TravelPlace> fixedPlaces = extractFixedPlacesForDay(confirmedSchedules, currentDate);
            dayPlaces.addAll(0, fixedPlaces); // 고정 일정을 먼저 배치
            fixedPlaces.forEach(place -> {
                if (place.getPlaceId() != null) {
                    globalUsedPlaceIds.add(place.getPlaceId());
                }
                if (place.getName() != null) {
                    globalUsedPlaceIds.add(place.getName());
                }
            });

            // 사용자 선택 장소 추가 (특정 날짜) - 이미 사용된 장소는 제외 (ID와 이름 기반)
            if (userSelected != null && !userSelected.isEmpty()) {
                List<TravelPlace> additionalUserPlaces = convertUserSelectedPlaces(userSelected, currentDate)
                    .stream()
                    .filter(place -> {
                        // 정규화된 이름으로 비교
                        String placeKey = place.getName() != null ?
                            place.getName().toLowerCase().replaceAll("\\s+", "") :
                            place.getPlaceId();

                        if (placeKey != null && globalUsedPlaceIds.contains(placeKey)) {
                            return false;
                        }
                        if (place.getPlaceId() != null && globalUsedPlaceIds.contains(place.getPlaceId())) {
                            return false;
                        }
                        if (place.getName() != null && globalUsedPlaceIds.contains(place.getName())) {
                            return false;
                        }
                        return true;
                    })
                    .collect(Collectors.toList());
                dayPlaces.addAll(additionalUserPlaces);
                additionalUserPlaces.forEach(place -> {
                    if (place.getPlaceId() != null) {
                        globalUsedPlaceIds.add(place.getPlaceId());
                    }
                    if (place.getName() != null) {
                        globalUsedPlaceIds.add(place.getName());
                        // 정규화된 이름도 추가
                        globalUsedPlaceIds.add(place.getName().toLowerCase().replaceAll("\\s+", ""));
                    }
                });
            }

            List<Stage3KMeansClusteringService.ClusterCenter> centersForDay = filterCentersByDay(
                clusterCenters,
                clusterToDayMap,
                day
            );

            // 각 날짜에 최소 4개 장소가 필요 (4개 시간 블록 채우기)
            int currentDayPlaces = dayPlaces.size();
            int minPlacesNeeded = 4;

            // 클러스터 중심이 없으면 목적지 장소들의 평균 좌표 사용
            if (centersForDay.isEmpty() && currentDayPlaces < minPlacesNeeded) {
                double avgLat = candidates.stream()
                    .mapToDouble(c -> c.getLatitude() != null ? c.getLatitude() : 37.5665)
                    .average().orElse(37.5665);
                double avgLng = candidates.stream()
                    .mapToDouble(c -> c.getLongitude() != null ? c.getLongitude() : 126.9780)
                    .average().orElse(126.9780);
                Stage3KMeansClusteringService.ClusterCenter destinationCenter =
                    new Stage3KMeansClusteringService.ClusterCenter(0, avgLat, avgLng, 1);
                centersForDay.add(destinationCenter);
                log.info("Day {} has no clusters, using destination center ({}, {}) for AI recommendations",
                    day, avgLat, avgLng);
            }

            List<TravelPlace> aiRecommended = searchNearbyPlacesWithGlobalTracking(
                candidates,
                centersForDay,
                globalUsedPlaceIds,
                travelStyle,
                scoredPlaces
            );

            // 최소 장소 수를 채우기 위해 추가 검색
            if (dayPlaces.size() + aiRecommended.size() < minPlacesNeeded) {
                log.info("Day {} needs more places. Current: {}, AI: {}, Need: {}",
                    day, dayPlaces.size(), aiRecommended.size(), minPlacesNeeded);

                // 거리와 품질 조건을 완화하여 추가 검색
                List<TravelPlace> additionalPlaces = searchAdditionalPlaces(
                    candidates,
                    scoredPlaces,
                    globalUsedPlaceIds,
                    minPlacesNeeded - dayPlaces.size() - aiRecommended.size()
                );
                aiRecommended.addAll(additionalPlaces);
            }

            dayPlaces.addAll(aiRecommended);

            // 시간대별 배치 (고정 일정 시간을 고려)
            List<TravelPlace> arrangedPlaces = arrangeByTimeBlockWithConstraints(dayPlaces, confirmedSchedules, currentDate);

            DailyItinerary itinerary = DailyItinerary.builder()
                .date(currentDate)
                .dayNumber(day + 1)
                .places(arrangedPlaces)
                .estimatedDuration(calculateDayDuration(arrangedPlaces))
                .hasFixedSchedules(!fixedPlaces.isEmpty())
                .timeBlocks(createTimeBlocks(arrangedPlaces))
                .build();

            itineraries.add(itinerary);
        }

        return itineraries;
    }

    // OCR 확정 일정에서 특정 날짜의 고정 장소 추출
    private List<TravelPlace> extractFixedPlacesForDay(
            List<ConfirmedSchedule> confirmedSchedules,
            LocalDate date) {

        return confirmedSchedules.stream()
            .filter(schedule -> schedule.startTime().toLocalDate().equals(date))
            .filter(ConfirmedSchedule::isFixed)
            .map(schedule -> TravelPlace.builder()
                .placeId("ocr_" + schedule.documentType().name())
                .name(schedule.title())
                .category(mapDocumentTypeToCategory(schedule.documentType()))
                .address(schedule.address())
                .isFixed(true)
                .fixedTime(schedule.startTime())
                .build())
            .collect(Collectors.toList());
    }

    // 문서 타입을 카테고리로 매핑
    private String mapDocumentTypeToCategory(com.compass.domain.chat.model.enums.DocumentType type) {
        return switch (type) {
            case FLIGHT_RESERVATION -> "항공";
            case HOTEL_RESERVATION -> "숙박";
            case EVENT_TICKET -> "공연/이벤트";
            case TRAIN_TICKET -> "교통";
            default -> "기타";
        };
    }

    // 고정 일정을 고려한 시간대별 배치
    private List<TravelPlace> arrangeByTimeBlockWithConstraints(
            List<TravelPlace> places,
            List<ConfirmedSchedule> confirmedSchedules,
            LocalDate date) {

        // 고정 일정과 자유 일정 분리
        List<TravelPlace> fixedPlaces = places.stream()
            .filter(p -> p.getIsFixed() != null && p.getIsFixed())
            .sorted((a, b) -> {
                if (a.getFixedTime() != null && b.getFixedTime() != null) {
                    return a.getFixedTime().compareTo(b.getFixedTime());
                }
                return 0;
            })
            .collect(Collectors.toList());

        List<TravelPlace> flexiblePlaces = places.stream()
            .filter(p -> p.getIsFixed() == null || !p.getIsFixed())
            .collect(Collectors.toList());

        // 고정 일정 사이에 자유 일정 배치
        List<TravelPlace> arranged = new ArrayList<>();
        int flexIndex = 0;

        for (int i = 0; i < fixedPlaces.size(); i++) {
            TravelPlace fixed = fixedPlaces.get(i);
            arranged.add(fixed);

            // 다음 고정 일정까지 시간이 있으면 자유 일정 추가
            if (i < fixedPlaces.size() - 1) {
                TravelPlace nextFixed = fixedPlaces.get(i + 1);
                long hoursBetween = java.time.Duration.between(
                    fixed.getFixedTime(),
                    nextFixed.getFixedTime()
                ).toHours();

                // 3시간 이상 간격이 있으면 자유 일정 1-2개 추가
                int placesToAdd = (int) Math.min(hoursBetween / 3, 2);
                for (int j = 0; j < placesToAdd && flexIndex < flexiblePlaces.size(); j++) {
                    arranged.add(flexiblePlaces.get(flexIndex++));
                }
            }
        }

        // 남은 자유 일정 추가
        while (flexIndex < flexiblePlaces.size()) {
            arranged.add(flexiblePlaces.get(flexIndex++));
        }

        return arranged;
    }

    // 날짜 타입 안전 변환 (String 또는 LocalDate 처리)
    private LocalDate convertToLocalDate(Object dateObj) {
        if (dateObj == null) {
            return LocalDate.now(); // 기본값
        }
        if (dateObj instanceof LocalDate) {
            return (LocalDate) dateObj;
        }
        if (dateObj instanceof String) {
            try {
                return LocalDate.parse((String) dateObj);
            } catch (Exception e) {
                log.warn("Failed to parse date string: {}", dateObj);
                return LocalDate.now();
            }
        }
        return LocalDate.now();
    }

    // travelStyle 추출 (List 또는 String 처리)
    @SuppressWarnings("unchecked")
    private String extractTravelStyle(TravelContext context) {
        Object styleObj = context.getCollectedInfo().get(TravelContext.KEY_TRAVEL_STYLE);
        if (styleObj == null) {
            return "culture"; // 기본값
        }
        if (styleObj instanceof List) {
            List<String> styles = (List<String>) styleObj;
            return styles.isEmpty() ? "culture" : styles.get(0);
        }
        if (styleObj instanceof String) {
            return (String) styleObj;
        }
        return "culture";
    }

    private List<Stage3KMeansClusteringService.ClusterCenter> filterCentersByDay(
            List<Stage3KMeansClusteringService.ClusterCenter> centers,
            Map<Integer, Integer> clusterToDayMap,
            int dayIndex) {

        return centers.stream()
            .filter(center -> clusterToDayMap.getOrDefault(center.getClusterId(), -1) == dayIndex)
            .collect(Collectors.toList());
    }

    private boolean matchesTravelStyle(TravelCandidate candidate, String travelStyle) {
        return matchesTravelStyle(candidate.getCategory(), candidate.getDescription(), travelStyle);
    }

    private boolean matchesTravelStyle(TravelPlace place, String travelStyle) {
        return matchesTravelStyle(place.getCategory(), place.getDescription(), travelStyle);
    }

    // 여행 스타일 매칭 (Stage1과 동일한 로직)
    private boolean matchesTravelStyle(String categoryValue, String descriptionValue, String travelStyle) {
        if (travelStyle == null || travelStyle.isBlank()) {
            return true;
        }

        String normalizedStyle = travelStyle.trim().toLowerCase();
        String category = categoryValue != null ? categoryValue.toLowerCase() : "";
        String description = descriptionValue != null ? descriptionValue.toLowerCase() : "";

        // 직접 매칭
        if (category.contains(normalizedStyle) || description.contains(normalizedStyle)) {
            return true;
        }

        // 한국어 여행 스타일 매칭 (Stage1과 동일)
        return switch (normalizedStyle) {
            case "관광" ->
                category.contains("관광") || category.contains("명소") || category.contains("랜드마크") ||
                category.contains("전망") || category.contains("야경");
            case "맛집" ->
                category.contains("맛집") || category.contains("음식") || category.contains("레스토랑") ||
                category.contains("식당");
            case "편안한" ->
                true; // 평점 기반 필터링은 다른 곳에서 처리
            case "활동적인" ->
                category.contains("액티비티") || category.contains("체험");
            case "문화" ->
                category.contains("문화") || category.contains("박물관") || category.contains("전통");
            case "미식" ->
                category.contains("맛집") || category.contains("음식");
            case "쇼핑" ->
                category.contains("쇼핑");
            // 영어 여행 스타일 매칭
            case "relax", "휴양" ->
                category.contains("스파") || category.contains("카페") || category.contains("공원");
            case "food" ->
                category.contains("맛집") || category.contains("음식") || category.contains("레스토랑") || category.contains("카페");
            case "culture" ->
                category.contains("문화") || category.contains("박물관") || category.contains("전통") || category.contains("역사");
            case "activity", "액티비티", "adventure" ->
                category.contains("체험") || category.contains("액티비티") || category.contains("스포츠");
            case "shopping" ->
                category.contains("쇼핑") || category.contains("시장") || category.contains("백화점");
            case "nature", "자연" ->
                category.contains("자연") || category.contains("공원") || category.contains("산") || category.contains("바다");
            default -> true;
        };
    }

    // TravelContext에서 사용자 선택 장소 추출
    @SuppressWarnings("unchecked")
    private List<SelectedSchedule> extractUserSelectedPlaces(TravelContext context) {
        log.info("Extracting user selected places from context. CollectedInfo keys: {}, Metadata keys: {}",
                context.getCollectedInfo().keySet(), context.getMetadata().keySet());

        // Try metadata first (from Stage2 integration)
        Object placesObj = context.getMetadata().get("userSelectedPlaces");
        if (placesObj == null) {
            // Fall back to collectedInfo
            placesObj = context.getCollectedInfo().get("userSelectedPlaces");
        }

        if (placesObj == null) {
            log.warn("No userSelectedPlaces found in context metadata or collectedInfo");
            return List.of();
        }

        if (placesObj instanceof List<?>) {
            List<?> placesList = (List<?>) placesObj;
            List<SelectedSchedule> result = new ArrayList<>();

            for (Object placeData : placesList) {
                if (placeData instanceof Map) {
                    Map<String, Object> placeMap = (Map<String, Object>) placeData;
                    SelectedSchedule schedule = SelectedSchedule.userSelected(
                        (String) placeMap.get("placeId"),
                        (String) placeMap.getOrDefault("placeName", placeMap.get("name")),
                        (String) placeMap.get("category"),
                        (String) placeMap.get("address"),
                        convertToDouble(placeMap.get("latitude")),
                        convertToDouble(placeMap.get("longitude")),
                        convertToDouble(placeMap.get("rating"))
                    );
                    result.add(schedule);
                } else if (placeData instanceof TravelPlace) {
                    TravelPlace place = (TravelPlace) placeData;
                    SelectedSchedule schedule = SelectedSchedule.userSelected(
                        place.getPlaceId(),
                        place.getName(),
                        place.getCategory(),
                        place.getAddress(),
                        place.getLatitude(),
                        place.getLongitude(),
                        place.getRating()
                    );
                    result.add(schedule);
                }
            }

            log.info("Extracted {} places from context", result.size());
            return result;
        }

        return List.of();
    }

    // Double 타입 변환 헬퍼 메서드
    private Double convertToDouble(Object value) {
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
