package com.compass.domain.chat.route_optimization.service;

import com.compass.domain.chat.function.processing.phase3.date_selection.model.*;
import com.compass.domain.chat.route_optimization.config.RouteOptimizationTestConfig;
import com.compass.domain.chat.route_optimization.model.RouteOptimizationRequest;
import com.compass.domain.chat.route_optimization.model.RouteOptimizationResponse;
import com.compass.domain.chat.route_optimization.model.RouteOptimizationResponse.RouteInfo;
import com.compass.domain.chat.route_optimization.service.RouteOptimizationOrchestrationService.CustomizationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(RouteOptimizationTestConfig.class)
@ActiveProfiles("test")
class RouteOptimizationIntegrationTest {

    @Autowired
    private RouteOptimizationOrchestrationService orchestrationService;

    private Long sessionId;
    private DateSelectionOutput testDateSelectionOutput;

    @BeforeEach
    void setUp() {
        sessionId = 1L;
        testDateSelectionOutput = createTestDateSelectionOutput();
    }

    @Test
    @DisplayName("날짜별 선별 데이터로 Route Optimization AI 추천 일정 생성")
    void testOptimizeWithDateSelectionData() {
        // Given
        RouteOptimizationRequest request = RouteOptimizationRequest.fromDateSelectionOutput(testDateSelectionOutput);

        // When
        RouteOptimizationResponse response = orchestrationService.processRouteOptimization(sessionId, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.success()).isTrue();
        assertThat(response.aiRecommendedItinerary()).isNotEmpty();
        assertThat(response.dailyRoutes()).hasSize(3); // 3일 일정

        // 각 날짜별 검증
        response.aiRecommendedItinerary().forEach((day, places) -> {
            assertThat(places).isNotEmpty();
            assertThat(places.size()).isLessThanOrEqualTo(8); // 하루 최대 8개 장소

            // 시간대별 장소가 있는지 확인
            boolean hasMeal = places.stream()
                .anyMatch(p -> p.timeBlock() != null &&
                    (p.timeBlock().contains("BREAKFAST") ||
                     p.timeBlock().contains("LUNCH") ||
                     p.timeBlock().contains("DINNER")));
            assertThat(hasMeal).isTrue();
        });

        // 경로 정보 검증
        response.dailyRoutes().forEach((day, route) -> {
            assertThat(route.orderedPlaces()).isNotEmpty();
            assertThat(route.totalDistance()).isGreaterThan(0);
            assertThat(route.totalDuration()).isGreaterThan(0);
            assertThat(route.segments()).isNotEmpty();
        });

        // 통계 검증
        assertThat(response.statistics()).isNotNull();
        assertThat(response.statistics().totalDays()).isEqualTo(3);
        assertThat(response.statistics().totalDistance()).isGreaterThan(0);
    }

    @Test
    @DisplayName("이동수단별 경로 계산 차이")
    void testDifferentTransportModes() {
        // CAR로 테스트
        RouteOptimizationRequest carRequest = new RouteOptimizationRequest(
            testDateSelectionOutput, "BALANCED", "CAR", null, List.of(), null, LocalDate.now()
        );
        RouteOptimizationResponse carResponse = orchestrationService.processRouteOptimization(sessionId, carRequest);

        // PUBLIC_TRANSPORT로 테스트
        RouteOptimizationRequest publicRequest = new RouteOptimizationRequest(
            testDateSelectionOutput, "BALANCED", "PUBLIC_TRANSPORT", null, List.of(), null, LocalDate.now()
        );
        RouteOptimizationResponse publicResponse = orchestrationService.processRouteOptimization(sessionId, publicRequest);

        // 같은 장소여도 이동시간이 다름을 검증
        carResponse.dailyRoutes().forEach((day, carRoute) -> {
            RouteInfo publicRoute = publicResponse.dailyRoutes().get(day);
            assertThat(publicRoute).isNotNull();

            // 대중교통이 일반적으로 더 오래 걸림
            assertThat(publicRoute.totalDuration())
                .isGreaterThanOrEqualTo(carRoute.totalDuration());
        });
    }

    private DateSelectionOutput createTestDateSelectionOutput() {
        Map<Integer, DailyItinerary> dailyItineraries = new HashMap<>();

        // Day 1 - 서울 중심부
        dailyItineraries.put(1, DailyItinerary.builder()
            .dayNumber(1)
            .date(LocalDate.now())
            .regions(List.of("종로구", "중구"))
            .places(List.of(
                createPlace("1", "경복궁", "MORNING_ACTIVITY", "관광지", 37.579617, 126.977041, 4.5),
                createPlace("2", "토속촌삼계탕", "LUNCH", "맛집", 37.5755, 126.9726, 4.3),
                createPlace("3", "북촌한옥마을", "AFTERNOON_ACTIVITY", "관광지", 37.5826, 126.9835, 4.4),
                createPlace("4", "인사동", "AFTERNOON_ACTIVITY", "쇼핑", 37.5738, 126.9863, 4.2),
                createPlace("5", "명동교자", "DINNER", "맛집", 37.5636, 126.9869, 4.1),
                createPlace("6", "남산타워", "EVENING_ACTIVITY", "관광지", 37.5512, 126.9882, 4.5),
                createPlace("7", "스타벅스 명동", "CAFE", "카페", 37.5635, 126.9850, 3.9)
            ))
            .totalDistance(0.0)
            .build()
        );

        // Day 2 - 강남/잠실
        dailyItineraries.put(2, DailyItinerary.builder()
            .dayNumber(2)
            .date(LocalDate.now().plusDays(1))
            .regions(List.of("강남구", "송파구"))
            .places(List.of(
                createPlace("8", "가로수길", "MORNING_ACTIVITY", "쇼핑", 37.5205, 127.0230, 4.2),
                createPlace("9", "진미평양냉면", "LUNCH", "맛집", 37.5172, 127.0473, 4.3),
                createPlace("10", "코엑스", "AFTERNOON_ACTIVITY", "쇼핑", 37.5115, 127.0595, 4.1),
                createPlace("11", "봉은사", "AFTERNOON_ACTIVITY", "관광지", 37.5152, 127.0574, 4.0),
                createPlace("12", "롯데타워", "EVENING_ACTIVITY", "관광지", 37.5126, 127.1024, 4.4),
                createPlace("13", "교대이층집", "DINNER", "맛집", 37.4929, 127.0141, 4.5)
            ))
            .totalDistance(0.0)
            .build()
        );

        // Day 3 - 홍대/이태원
        dailyItineraries.put(3, DailyItinerary.builder()
            .dayNumber(3)
            .date(LocalDate.now().plusDays(2))
            .regions(List.of("마포구", "용산구"))
            .places(List.of(
                createPlace("14", "홍대입구", "MORNING_ACTIVITY", "쇼핑", 37.5563, 126.9235, 4.1),
                createPlace("15", "연남동", "LUNCH", "맛집", 37.5661, 126.9253, 4.2),
                createPlace("16", "이태원", "AFTERNOON_ACTIVITY", "쇼핑", 37.5346, 126.9946, 4.0),
                createPlace("17", "리움미술관", "AFTERNOON_ACTIVITY", "관광지", 37.5384, 126.9990, 4.3),
                createPlace("18", "한강공원", "EVENING_ACTIVITY", "관광지", 37.5283, 126.9340, 4.2)
            ))
            .totalDistance(0.0)
            .build()
        );

        TravelSummary summary = TravelSummary.builder()
            .totalDays(3)
            .totalPlaces(18)
            // .regions(List.of("종로구", "중구", "강남구", "송파구", "마포구", "용산구"))
            // .estimatedBudget("300000")
            .build();

        return DateSelectionOutput.builder()
            .dailyItineraries(dailyItineraries)
            .summary(summary)
            .build();
    }

    private TourPlace createPlace(String id, String name, String timeBlock,
                                 String category, double lat, double lng, double rating) {
        return TourPlace.builder()
            .id(id)
            .name(name)
            .timeBlock(timeBlock)
            .category(category)
            .address(name + " 주소")
            .latitude(lat)
            .longitude(lng)
            .rating(rating)
            .priceLevel("$$")
            .isTrendy(rating > 4.2)
            .petAllowed(true)
            .parkingAvailable(true)
            .day(1)
            .build();
    }
}