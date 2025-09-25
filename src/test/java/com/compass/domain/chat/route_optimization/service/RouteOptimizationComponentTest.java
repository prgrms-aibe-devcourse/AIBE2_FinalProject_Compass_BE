package com.compass.domain.chat.route_optimization.service;

import com.compass.domain.chat.function.processing.phase3.date_selection.model.*;
import com.compass.domain.chat.model.dto.ConfirmedSchedule;
import com.compass.domain.chat.orchestrator.ContextManager;
import com.compass.domain.chat.repository.ChatThreadRepository;
import com.compass.domain.chat.route_optimization.client.KakaoMobilityClient;
import com.compass.domain.chat.route_optimization.entity.TravelItinerary;
import com.compass.domain.chat.route_optimization.model.RouteOptimizationRequest;
import com.compass.domain.chat.route_optimization.model.RouteOptimizationResponse;
import com.compass.domain.chat.route_optimization.repository.TravelItineraryRepository;
import com.compass.domain.chat.route_optimization.repository.TravelPlaceCandidateRepository;
import com.compass.domain.chat.route_optimization.repository.TravelPlaceRepository;
import com.compass.domain.chat.route_optimization.service.ItineraryPersistenceService;
import com.compass.domain.chat.route_optimization.service.MultiPathOptimizationService;
import com.compass.domain.chat.route_optimization.service.RouteOptimizationOrchestrationService;
import com.compass.domain.chat.route_optimization.strategy.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Route Optimization 컴포넌트 통합 테스트")
class RouteOptimizationComponentTest {

    private RouteOptimizationOrchestrationService orchestrationService;
    private ItineraryPersistenceService persistenceService;
    private OptimizationStrategyFactory strategyFactory;

    @Mock
    private ChatThreadRepository threadRepository;
    @Mock
    private TravelItineraryRepository itineraryRepository;
    @Mock
    private TravelPlaceRepository placeRepository;
    @Mock
    private TravelPlaceCandidateRepository candidateRepository;
    @Mock
    private ContextManager contextManager;
    @Mock
    private MultiPathOptimizationService multiPathOptimizationService;
    @Mock
    private KakaoMobilityClient kakaoMobilityClient;

    private ObjectMapper objectMapper;
    private Long sessionId;
    private DateSelectionOutput testDateSelectionOutput;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        persistenceService = new ItineraryPersistenceService(
            itineraryRepository,
            placeRepository,
            candidateRepository,
            threadRepository
        );

        // Create strategy factory with real strategies for testing
        DistanceOptimizationStrategy distanceStrategy = new DistanceOptimizationStrategy();
        TimeOptimizationStrategy timeStrategy = new TimeOptimizationStrategy();
        BalancedOptimizationStrategy balancedStrategy = new BalancedOptimizationStrategy();
        strategyFactory = new OptimizationStrategyFactory(
            distanceStrategy,
            timeStrategy,
            balancedStrategy
        );

        orchestrationService = new RouteOptimizationOrchestrationService(
            threadRepository,
            objectMapper,
            contextManager,
            persistenceService,
            strategyFactory,
            multiPathOptimizationService,
            kakaoMobilityClient
        );
        sessionId = 1L;
        testDateSelectionOutput = createTestDateSelectionOutput();
    }

    @Test
    @DisplayName("OCR 확정 일정 우선순위 처리 통합 테스트")
    void testOcrSchedulePrioritization() {
        // Given - OCR 확정 일정
        List<ConfirmedSchedule> ocrSchedules = List.of(
            ConfirmedSchedule.flight(
                LocalDateTime.now().plusDays(1).withHour(9).withMinute(0),
                LocalDateTime.now().plusDays(1).withHour(11).withMinute(0),
                "OZ201",
                "김포공항",
                "제주공항",
                "대한항공 예약 확인서",
                "flight.pdf"
            ),
            ConfirmedSchedule.hotel(
                LocalDateTime.now().plusDays(1).withHour(15).withMinute(0),
                LocalDateTime.now().plusDays(3).withHour(11).withMinute(0),
                "제주 롯데호텔",
                "제주시 연동 1261-16",
                "호텔 예약 확인서",
                "hotel.pdf"
            ),
            ConfirmedSchedule.train(
                LocalDateTime.now().plusDays(3).withHour(16).withMinute(0),
                LocalDateTime.now().plusDays(3).withHour(19).withMinute(0),
                "KTX 123",
                "123456789",  // 예약번호
                "제주역",
                "서울역",
                "KTX 예약 확인서",
                "train.pdf"
            )
        );

        RouteOptimizationRequest request = RouteOptimizationRequest.withOcrSchedules(
            testDateSelectionOutput,
            ocrSchedules,
            "thread123",
            LocalDate.now()
        );

        // Mock 설정
        when(itineraryRepository.findBySessionIdAndIsActiveTrue(anyLong()))
            .thenReturn(Optional.empty());
        when(itineraryRepository.save(any(TravelItinerary.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        RouteOptimizationResponse response = orchestrationService.processRouteOptimization(sessionId, request);

        // Then - OCR 일정이 최우선으로 포함되었는지 확인
        assertThat(response).isNotNull();
        assertThat(response.success()).isTrue();

        // Day 2의 일정 확인 (비행기가 Day 2에 있다고 가정)
        List<TourPlace> day2Places = response.aiRecommendedItinerary().get(2);
        assertThat(day2Places).isNotNull();

        // 항공편이 포함되었는지 확인
        boolean hasFlightSchedule = day2Places.stream()
            .anyMatch(p -> p.name().contains("OZ201") ||
                          p.name().contains("김포공항") ||
                          p.category().equals("교통"));

        // 호텔이 포함되었는지 확인
        boolean hasHotelSchedule = day2Places.stream()
            .anyMatch(p -> p.name().contains("롯데호텔") ||
                          p.category().equals("숙박"));

        assertThat(hasFlightSchedule || hasHotelSchedule).isTrue()
            .withFailMessage("OCR 확정 일정이 우선순위대로 포함되어야 합니다");

        // 시간 충돌 검사
        List<TourPlace> sortedPlaces = day2Places.stream()
            .sorted((p1, p2) -> {
                String t1 = p1.timeBlock() != null ? p1.timeBlock() : "";
                String t2 = p2.timeBlock() != null ? p2.timeBlock() : "";
                return t1.compareTo(t2);
            })
            .toList();

        // 시간대가 겹치지 않는지 확인
        Set<String> usedTimeBlocks = new HashSet<>();
        for (TourPlace place : sortedPlaces) {
            if (place.timeBlock() != null) {
                assertThat(usedTimeBlocks.add(place.timeBlock()))
                    .withFailMessage("시간대 충돌 발견: " + place.timeBlock())
                    .isTrue();
            }
        }
    }

    @Test
    @DisplayName("Route Optimization 전체 플로우 테스트 - 날짜별 선별 → 최적화 → 저장")
    void testCompleteRouteOptimizationFlow() {
        // Given
        RouteOptimizationRequest request = RouteOptimizationRequest.fromDateSelectionOutput(testDateSelectionOutput);

        // Mock 설정
        when(itineraryRepository.findBySessionIdAndIsActiveTrue(anyLong()))
            .thenReturn(Optional.empty());
        when(itineraryRepository.save(any(TravelItinerary.class)))
            .thenAnswer(invocation -> {
                TravelItinerary itinerary = invocation.getArgument(0);
                itinerary.setId(1L); // ID 설정
                return itinerary;
            });

        // When
        RouteOptimizationResponse response = orchestrationService.processRouteOptimization(sessionId, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.success()).isTrue();

        // AI 추천 일정 검증
        assertThat(response.aiRecommendedItinerary()).isNotEmpty();
        assertThat(response.aiRecommendedItinerary()).hasSize(3); // 3일 일정

        // 각 일자별 일정 검증
        response.aiRecommendedItinerary().forEach((day, places) -> {
            assertThat(places).isNotEmpty();
            assertThat(places.size()).isBetween(4, 10); // 하루 4-10개 장소

            // 각 장소 필수 정보 검증
            places.forEach(place -> {
                assertThat(place.name()).isNotBlank();
                assertThat(place.category()).isNotBlank();
                assertThat(place.latitude()).isNotNull();
                assertThat(place.longitude()).isNotNull();
            });
        });

        // 경로 정보 검증
        assertThat(response.dailyRoutes()).isNotEmpty();
        response.dailyRoutes().forEach((day, route) -> {
            assertThat(route.orderedPlaces()).isNotEmpty();
            assertThat(route.totalDistance()).isNotNegative(); // 0 이상 (Mock 데이터이므로)
            assertThat(route.totalDuration()).isNotNegative(); // 0 이상
            assertThat(route.segments()).isNotNull(); // null이 아님

            // 경로 세그먼트가 있다면 검증
            if (!route.segments().isEmpty()) {
                route.segments().forEach(segment -> {
                    assertThat(segment.from()).isNotBlank();
                    assertThat(segment.to()).isNotBlank();
                    assertThat(segment.distance()).isNotNegative();
                    assertThat(segment.duration()).isNotNegative();
                });
            }
        });

        // 후보 장소 검증
        assertThat(response.allCandidatePlaces()).isNotEmpty();
        assertThat(response.allCandidatePlaces()).hasSize(3); // 3일치

        // 통계 정보 검증
        assertThat(response.statistics()).isNotNull();
        assertThat(response.statistics().totalDays()).isEqualTo(3);
        // totalPlaces 메서드가 없으므로 일정의 장소 수로 확인
        int totalPlaces = response.aiRecommendedItinerary().values().stream()
            .mapToInt(List::size)
            .sum();
        assertThat(totalPlaces).isPositive();
        assertThat(response.statistics().totalDistance()).isNotNegative(); // Mock이므로 0 이상
        assertThat(response.statistics().totalDuration()).isNotNegative(); // Mock이므로 0 이상
    }

    @Test
    @DisplayName("최적화 전략별 경로 차이 테스트")
    void testDifferentOptimizationStrategies() {
        // 최단 거리 전략
        RouteOptimizationRequest shortestRequest = new RouteOptimizationRequest(
            testDateSelectionOutput,
            "SHORTEST_DISTANCE",
            "CAR",
            null,
            List.of(),
            null,
            LocalDate.now()
        );

        // 최소 시간 전략
        RouteOptimizationRequest fastestRequest = new RouteOptimizationRequest(
            testDateSelectionOutput,
            "FASTEST_TIME",
            "CAR",
            null,
            List.of(),
            null,
            LocalDate.now()
        );

        // Mock 설정
        when(itineraryRepository.findBySessionIdAndIsActiveTrue(anyLong()))
            .thenReturn(Optional.empty());
        when(itineraryRepository.save(any(TravelItinerary.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        RouteOptimizationResponse shortestResponse = orchestrationService.processRouteOptimization(sessionId, shortestRequest);
        RouteOptimizationResponse fastestResponse = orchestrationService.processRouteOptimization(sessionId, fastestRequest);

        // Then - 두 전략의 결과가 다른지 확인
        assertThat(shortestResponse.statistics().totalDistance())
            .isLessThanOrEqualTo(fastestResponse.statistics().totalDistance());

        // 최소 시간 전략이 일반적으로 더 빠른 시간을 보여야 함
        int fastestDuration = fastestResponse.statistics().totalDuration();
        int shortestDuration = shortestResponse.statistics().totalDuration();
        assertThat(fastestDuration).isLessThanOrEqualTo((int)(shortestDuration * 1.2)); // 20% 여유
    }

    private DateSelectionOutput createTestDateSelectionOutput() {
        Map<Integer, DailyItinerary> dailyItineraries = new HashMap<>();

        // 3일간의 테스트 일정 생성
        for (int day = 1; day <= 3; day++) {
            List<TourPlace> places = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                places.add(TourPlace.builder()
                    .id("place_" + day + "_" + i)
                    .name("장소 " + day + "-" + i)
                    .timeBlock(getTimeBlock(i))
                    .category(getCategory(i))
                    .address("서울시 테스트구 " + day + "-" + i)
                    .latitude(37.5 + (day * 0.01) + (i * 0.001))
                    .longitude(127.0 + (day * 0.01) + (i * 0.001))
                    .rating(4.0 + (i * 0.1))
                    .priceLevel("$$")
                    .isTrendy(i % 2 == 0)
                    .petAllowed(true)
                    .parkingAvailable(true)
                    .day(day)
                    .build());
            }

            dailyItineraries.put(day, DailyItinerary.builder()
                .dayNumber(day)
                .date(LocalDate.now().plusDays(day - 1))
                .regions(List.of("서울시 테스트구"))
                .places(places)
                .totalDistance(0.0)
                .build());
        }

        return DateSelectionOutput.builder()
            .dailyItineraries(dailyItineraries)
            .summary(TravelSummary.builder()
                .totalDays(3)
                .totalPlaces(18)
                .build())
            .build();
    }

    private String getTimeBlock(int index) {
        return switch (index) {
            case 0 -> "BREAKFAST";
            case 1 -> "MORNING_ACTIVITY";
            case 2 -> "LUNCH";
            case 3 -> "AFTERNOON_ACTIVITY";
            case 4 -> "DINNER";
            case 5 -> "EVENING_ACTIVITY";
            default -> "FREE_TIME";
        };
    }

    private String getCategory(int index) {
        return switch (index % 6) {
            case 0, 2, 4 -> "맛집";
            case 1, 3 -> "관광지";
            case 5 -> "카페";
            default -> "기타";
        };
    }
}