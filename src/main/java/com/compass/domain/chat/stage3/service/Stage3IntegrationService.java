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

        // OCR 확정 일정 활용
        List<ConfirmedSchedule> confirmedSchedules = context.getOcrConfirmedSchedules();

        // Stage3Input 생성
        Stage3Input input = Stage3Input.fromPhase2Output(
            destination, startDate, endDate, travelStyle, companions, transportMode
        );

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
            scoredPlaces,
            input.startDate(),
            input.endDate(),
            input.userSelectedPlaces()
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
            scoredPlaces,
            input.startDate(),
            input.endDate(),
            input.userSelectedPlaces(),
            confirmedSchedules
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

    // 날짜별 일정 생성
    private List<DailyItinerary> createDailyItineraries(
            List<TravelPlace> places,
            LocalDate startDate,
            LocalDate endDate,
            List<SelectedSchedule> userSelected) {

        List<DailyItinerary> itineraries = new ArrayList<>();
        long days = endDate.toEpochDay() - startDate.toEpochDay() + 1;

        // K-means 클러스터링으로 지역별 그룹화
        Map<Integer, List<TravelPlace>> clusteredPlaces = clusterPlacesByLocation(places, (int)days);

        for (int day = 0; day < days; day++) {
            LocalDate currentDate = startDate.plusDays(day);
            List<TravelPlace> dayPlaces = clusteredPlaces.getOrDefault(day, new ArrayList<>());

            // 사용자 선택 장소 추가
            if (userSelected != null && !userSelected.isEmpty()) {
                dayPlaces.addAll(convertUserSelectedPlaces(userSelected, currentDate));
            }

            // 시간대별 배치
            DailyItinerary itinerary = DailyItinerary.builder()
                .date(currentDate)
                .dayNumber(day + 1)
                .places(arrangeByTimeBlock(dayPlaces))
                .estimatedDuration(calculateDayDuration(dayPlaces))
                .build();

            itineraries.add(itinerary);
        }

        return itineraries;
    }

    // K-means 클러스터링
    private Map<Integer, List<TravelPlace>> clusterPlacesByLocation(List<TravelPlace> places, int days) {
        // 간단한 K-means 구현 (실제로는 더 정교한 알고리즘 필요)
        Map<Integer, List<TravelPlace>> clusters = new HashMap<>();

        int placesPerDay = Math.max(places.size() / days, 10);
        for (int i = 0; i < places.size(); i++) {
            int dayIndex = Math.min(i / placesPerDay, days - 1);
            clusters.computeIfAbsent(dayIndex, k -> new ArrayList<>()).add(places.get(i));
        }

        return clusters;
    }

    // 시간대별 장소 배치
    private List<TravelPlace> arrangeByTimeBlock(List<TravelPlace> places) {
        // 카테고리별로 시간대 배치
        return places.stream()
            .sorted((a, b) -> {
                // 아침: 카페, 조식
                // 오전: 관광지
                // 점심: 맛집
                // 오후: 관광지, 쇼핑
                // 저녁: 맛집
                // 야간: 야경, 바
                int timeA = getTimeBlockPriority(a.getCategory());
                int timeB = getTimeBlockPriority(b.getCategory());
                return Integer.compare(timeA, timeB);
            })
            .collect(Collectors.toList());
    }

    private int getTimeBlockPriority(String category) {
        return switch (category.toLowerCase()) {
            case "카페", "cafe", "breakfast" -> 1;
            case "관광지", "attraction", "museum" -> 2;
            case "맛집", "restaurant" -> 3;
            case "쇼핑", "shopping" -> 4;
            case "야경", "bar", "nightlife" -> 5;
            default -> 3;
        };
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
            List<TravelPlace> places,
            LocalDate startDate,
            LocalDate endDate,
            List<SelectedSchedule> userSelected,
            List<ConfirmedSchedule> confirmedSchedules) {

        List<DailyItinerary> itineraries = new ArrayList<>();
        long days = endDate.toEpochDay() - startDate.toEpochDay() + 1;

        // K-means 클러스터링으로 지역별 그룹화
        Map<Integer, List<TravelPlace>> clusteredPlaces = clusterPlacesByLocation(places, (int)days);

        for (int day = 0; day < days; day++) {
            LocalDate currentDate = startDate.plusDays(day);
            List<TravelPlace> dayPlaces = new ArrayList<>(clusteredPlaces.getOrDefault(day, new ArrayList<>()));

            // OCR 확정 일정 추가 (항공, 호텔, 공연 등)
            List<TravelPlace> fixedPlaces = extractFixedPlacesForDay(confirmedSchedules, currentDate);
            dayPlaces.addAll(0, fixedPlaces); // 고정 일정을 먼저 배치

            // 사용자 선택 장소 추가
            if (userSelected != null && !userSelected.isEmpty()) {
                dayPlaces.addAll(convertUserSelectedPlaces(userSelected, currentDate));
            }

            // 시간대별 배치 (고정 일정 시간을 고려)
            DailyItinerary itinerary = DailyItinerary.builder()
                .date(currentDate)
                .dayNumber(day + 1)
                .places(arrangeByTimeBlockWithConstraints(dayPlaces, confirmedSchedules, currentDate))
                .estimatedDuration(calculateDayDuration(dayPlaces))
                .hasFixedSchedules(!fixedPlaces.isEmpty())
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
}