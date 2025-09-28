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

    // TravelContext를 활용한 Phase 2 → Stage 3 통합 처리
    @Transactional(readOnly = true)
    public Stage3Output processWithTravelContext(TravelContext context) {
        log.info("Processing Stage 3 with TravelContext for user: {}", context.getUserId());

        // TravelContext에서 Phase 2 정보 추출
        String destination = extractDestination(context);
        String departureLocation = (String) context.getCollectedInfo().get(TravelContext.KEY_DEPARTURE);

        // 날짜 안전하게 변환 (String 또는 LocalDate 모두 처리)
        LocalDate startDate = convertToLocalDate(context.getCollectedInfo().get(TravelContext.KEY_START_DATE));
        LocalDate endDate = convertToLocalDate(context.getCollectedInfo().get(TravelContext.KEY_END_DATE));

        // travelStyle 처리 (List 또는 String)
        String travelStyle = extractTravelStyle(context);
        String companions = (String) context.getCollectedInfo().get(TravelContext.KEY_COMPANIONS);
        String transportMode = (String) context.getCollectedInfo().get(TravelContext.KEY_TRANSPORTATION_TYPE);

        // 사용자 선택 장소 추출
        List<SelectedSchedule> userSelectedPlaces = extractUserSelectedPlaces(context);
        log.info("Extracted {} user selected places from context", userSelectedPlaces.size());

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

        return Stage3Output.builder()
            .dailyItineraries(dailyItineraries)
            .optimizedRoutes(optimizedRoutes)
            .totalDistance(calculateTotalDistance(optimizedRoutes))
            .totalDuration(calculateTotalDuration(optimizedRoutes))
            .generatedAt(LocalDateTime.now())
            .build();
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

        // 4. 경로 최적화 (출발지 정보 포함)
        List<OptimizedRoute> optimizedRoutes = optimizeRoutes(
            dailyItineraries,
            input.transportMode(),
            departureLocation
        );

        return Stage3Output.builder()
            .dailyItineraries(dailyItineraries)
            .optimizedRoutes(optimizedRoutes)
            .totalDistance(calculateTotalDistance(optimizedRoutes))
            .totalDuration(calculateTotalDuration(optimizedRoutes))
            .generatedAt(LocalDateTime.now())
            .build();
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

        List<DailyItinerary> itineraries = new ArrayList<>();
        long days = endDate.toEpochDay() - startDate.toEpochDay() + 1;

        // 1. 사용자 선택 장소들을 K-means로 클러스터링
        List<TravelPlace> userPlaces = convertAllUserSelectedPlaces(userSelected);
        log.info("User selected places count: {}, userSelected input count: {}",
                userPlaces.size(), userSelected != null ? userSelected.size() : 0);

        if (userPlaces.isEmpty()) {
            log.warn("No user selected places found. Creating empty itineraries.");
            for (int day = 0; day < days; day++) {
                LocalDate currentDate = startDate.plusDays(day);
                DailyItinerary itinerary = DailyItinerary.builder()
                    .date(currentDate)
                    .dayNumber(day + 1)
                    .places(new ArrayList<>())
                    .estimatedDuration(0L)
                    .timeBlocks(createTimeBlocks(new ArrayList<>()))
                    .build();
                itineraries.add(itinerary);
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

        String normalized1 = name1.toLowerCase().replaceAll("\\s+", "");
        String normalized2 = name2.toLowerCase().replaceAll("\\s+", "");

        // 완전히 같은 경우
        if (normalized1.equals(normalized2)) return true;

        // 핵심 장소명 추출하여 비교
        String core1 = extractCorePlaceName(name1);
        String core2 = extractCorePlaceName(name2);

        // 핵심 장소명이 같으면 유사한 장소로 판단
        if (core1.equals(core2)) {
            log.debug("Similar places detected: {} ({}) == {} ({})", name1, core1, name2, core2);
            return true;
        }

        // 한쪽이 다른 쪽을 포함하는 경우
        if (core1.contains(core2) || core2.contains(core1)) {
            log.debug("Containment detected: {} contains {} or vice versa", core1, core2);
            return true;
        }

        return false;
    }

    // 장소명에서 핵심 이름만 추출하는 메서드
    private String extractCorePlaceName(String name) {
        String cleaned = name.toLowerCase()
            // 공백 제거
            .replaceAll("\\s+", "")
            // 수식어구 제거
            .replaceAll("(야경|야간개장|전망대|플라자|어드벤처|야간|주간|특별|이벤트)", "")
            // N서울타워 변형 통일
            .replaceAll("(n서울타워|남산서울타워|서울타워|남산타워)", "서울타워")
            // 롯데월드 변형 통일
            .replaceAll("(롯데월드타워|롯데타워|롯데월드어드벤처|롯데월드)", "롯데월드")
            // 국립중앙박물관 변형 통일
            .replaceAll("(국립중앙박물관|중앙박물관)", "국립중앙박물관");

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

    // 시간 블록 정보 생성
    private Map<String, List<TravelPlace>> createTimeBlocks(List<TravelPlace> places) {
        Map<String, List<TravelPlace>> timeBlocks = new LinkedHashMap<>();
        timeBlocks.put("09:00-12:00", new ArrayList<>());
        timeBlocks.put("12:00-15:00", new ArrayList<>());
        timeBlocks.put("15:00-18:00", new ArrayList<>());
        timeBlocks.put("18:00-21:00", new ArrayList<>());

        // 각 블록에 최대 1개의 장소만 배치 (일반적인 여행 일정)
        int index = 0;
        for (String block : timeBlocks.keySet()) {
            if (index < places.size()) {
                timeBlocks.get(block).add(places.get(index++));
            }
        }

        // 추가 장소가 있으면 시간블록에 균등 배치 (블록당 최대 2개)
        if (index < places.size()) {
            int blockIndex = 0;
            String[] blocks = timeBlocks.keySet().toArray(new String[0]);

            while (index < places.size()) {
                String block = blocks[blockIndex % 4];
                // 각 블록에 최대 2개까지만 허용
                if (timeBlocks.get(block).size() < 2) {
                    timeBlocks.get(block).add(places.get(index++));
                }
                blockIndex++;

                // 모든 블록이 2개씩 차면 중단
                if (blockIndex % 4 == 0) {
                    boolean allBlocksFull = true;
                    for (List<TravelPlace> blockPlaces : timeBlocks.values()) {
                        if (blockPlaces.size() < 2) {
                            allBlocksFull = false;
                            break;
                        }
                    }
                    if (allBlocksFull) break;
                }
            }
        }

        return timeBlocks;
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

            // 클러스터 중심이 없으면 전체 데이터에서 검색
            if (centersForDay.isEmpty() && currentDayPlaces < minPlacesNeeded) {
                // 서울 중심 좌표 사용
                Stage3KMeansClusteringService.ClusterCenter seoulCenter =
                    new Stage3KMeansClusteringService.ClusterCenter(0, 37.5665, 126.9780, 1);
                centersForDay.add(seoulCenter);
                log.info("Day {} has no clusters, using Seoul center for AI recommendations", day);
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

    private boolean matchesTravelStyle(String categoryValue, String descriptionValue, String travelStyle) {
        if (travelStyle == null || travelStyle.isBlank()) {
            return true;
        }

        String normalizedStyle = travelStyle.trim().toLowerCase();
        String category = categoryValue != null ? categoryValue.toLowerCase() : "";
        String description = descriptionValue != null ? descriptionValue.toLowerCase() : "";

        if (category.contains(normalizedStyle) || description.contains(normalizedStyle)) {
            return true;
        }

        return switch (normalizedStyle) {
            case "relax", "휴양", "편안한" ->
                category.contains("스파") || category.contains("카페") || category.contains("공원");
            case "food", "미식" ->
                category.contains("맛집") || category.contains("음식") || category.contains("레스토랑") || category.contains("카페");
            case "culture", "문화" ->
                category.contains("문화") || category.contains("박물관") || category.contains("전통") || category.contains("역사");
            case "activity", "액티비티", "adventure" ->
                category.contains("체험") || category.contains("액티비티") || category.contains("스포츠");
            case "shopping", "쇼핑" ->
                category.contains("쇼핑") || category.contains("시장") || category.contains("백화점");
            case "nature", "자연" ->
                category.contains("자연") || category.contains("공원") || category.contains("산") || category.contains("바다");
            default -> true;
        };
    }

    // TravelContext에서 사용자 선택 장소 추출
    @SuppressWarnings("unchecked")
    private List<SelectedSchedule> extractUserSelectedPlaces(TravelContext context) {
        log.info("Extracting user selected places from context. CollectedInfo keys: {}",
                context.getCollectedInfo().keySet());

        Object placesObj = context.getCollectedInfo().get("userSelectedPlaces");
        if (placesObj == null) {
            log.warn("No userSelectedPlaces found in context");
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
