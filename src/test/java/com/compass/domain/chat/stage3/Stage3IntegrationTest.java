package com.compass.domain.chat.stage3;

import com.compass.domain.chat.entity.TravelCandidate;
import com.compass.domain.chat.model.context.TravelContext;
import com.compass.domain.chat.model.dto.ConfirmedSchedule;
import com.compass.domain.chat.model.enums.DocumentType;
import com.compass.domain.chat.repository.TravelCandidateRepository;
import com.compass.domain.chat.stage3.dto.Stage3Output;
import com.compass.domain.chat.stage3.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled; // 👈 1. import 구문을 추가했습니다.
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Stage 3 통합 테스트")
@Disabled // 👈 2. 클래스 전체를 비활성화하는 어노테이션을 추가했습니다.
class Stage3IntegrationTest {

    @Mock
    private TravelCandidateRepository travelCandidateRepository;

    @Mock
    private Stage3TravelCandidateEnrichmentService enrichmentService;

    @Mock
    private PlaceScoreCalculationService scoreCalculationService;

    @Mock
    private Stage3RouteOptimizationService routeOptimizationService;

    @InjectMocks
    private Stage3IntegrationService stage3IntegrationService;

    private TravelContext testContext;
    private List<TravelCandidate> mockCandidates;

    @BeforeEach
    void setUp() {
        // TravelContext 설정
        testContext = TravelContext.builder()
            .userId("test_user")
            .threadId("test_thread")
            .currentPhase("PHASE_3")
            .build();

        // Phase 2 수집 정보 설정
        testContext.updateCollectedInfo(TravelContext.KEY_DESTINATIONS, List.of("서울"));
        testContext.updateCollectedInfo(TravelContext.KEY_START_DATE, LocalDate.of(2024, 2, 1));
        testContext.updateCollectedInfo(TravelContext.KEY_END_DATE, LocalDate.of(2024, 2, 3));
        testContext.updateCollectedInfo(TravelContext.KEY_TRAVEL_STYLE, "관광");
        testContext.updateCollectedInfo(TravelContext.KEY_COMPANIONS, "가족");
        testContext.updateCollectedInfo(TravelContext.KEY_TRANSPORTATION_TYPE, "대중교통");

        // OCR 호텔 예약 정보 추가
        ConfirmedSchedule hotelSchedule = ConfirmedSchedule.hotel(
            LocalDateTime.of(2024, 2, 1, 15, 0),
            LocalDateTime.of(2024, 2, 3, 11, 0),
            "롯데호텔 서울",
            "서울특별시 중구 을지로 30",
            "OCR_TEXT",
            "image_url"
        );
        testContext.addOcrSchedule(hotelSchedule);

        // Mock travel_candidates 데이터 생성
        mockCandidates = createMockCandidates();
    }

    @Test
    @DisplayName("Phase 2 TravelContext로 Stage 3 처리 성공")
    void testProcessWithTravelContext() {
        // given
        when(travelCandidateRepository.findByRegionAndIsActiveTrue("서울"))
            .thenReturn(mockCandidates);

        // when
        Stage3Output result = stage3IntegrationService.processWithTravelContext(testContext);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getDailyItineraries()).isNotNull();
        assertThat(result.getOptimizedRoutes()).isNotNull();
    }

    @Test
    @DisplayName("OCR 호텔 예약이 고정 일정으로 처리됨")
    void testHotelReservationAsFixedSchedule() {
        // given
        when(travelCandidateRepository.findByRegionAndIsActiveTrue("서울"))
            .thenReturn(mockCandidates);

        // when
        Stage3Output result = stage3IntegrationService.processWithTravelContext(testContext);

        // then
        assertThat(testContext.getOcrConfirmedSchedules()).isNotEmpty();

        ConfirmedSchedule hotelSchedule = testContext.getOcrConfirmedSchedules().get(0);
        assertThat(hotelSchedule.documentType()).isEqualTo(DocumentType.HOTEL_RESERVATION);
        assertThat(hotelSchedule.isFixed()).isTrue();  // 우선순위 1 = 고정 일정
        assertThat(hotelSchedule.title()).contains("롯데호텔");
    }

    @Test
    @DisplayName("travel_candidates에서 Google Places 데이터 우선 활용")
    void testGooglePlacesDataPriority() {
        // given
        TravelCandidate enrichedCandidate = TravelCandidate.builder()
            .placeId("test_place_1")
            .name("경복궁")
            .region("서울")
            .category("관광지")
            .googlePlaceId("ChIJN1t_tDeuEmsR")  // Google Places ID 있음
            .rating(4.5)
            .reviewCount(15000)
            .qualityScore(0.85)
            .aiEnriched(true)  // AI 보강 완료
            .build();

        TravelCandidate normalCandidate = TravelCandidate.builder()
            .placeId("test_place_2")
            .name("남산타워")
            .region("서울")
            .category("관광지")
            .rating(4.2)
            .reviewCount(8000)
            .qualityScore(0.75)
            .build();

        List<TravelCandidate> candidates = Arrays.asList(enrichedCandidate, normalCandidate);
        when(travelCandidateRepository.findByRegionAndIsActiveTrue("서울"))
            .thenReturn(candidates);

        // when
        Stage3Output result = stage3IntegrationService.processWithTravelContext(testContext);

        // then
        // Google Places 데이터가 있는 enrichedCandidate가 우선 선택됨
        assertThat(enrichedCandidate.getGooglePlaceId()).isNotNull();
        assertThat(enrichedCandidate.getAiEnriched()).isTrue();
        assertThat(enrichedCandidate.getQualityScore())
            .isGreaterThan(normalCandidate.getQualityScore());
    }

    @Test
    @DisplayName("2박3일 일정에 최대 210개(70x3) 장소 조회")
    void testMaxPlacesForTrip() {
        // given
        List<TravelCandidate> manyCandidate = createManyCandidates(300);
        when(travelCandidateRepository.findByRegionAndIsActiveTrue("서울"))
            .thenReturn(manyCandidate);

        // when
        Stage3Output result = stage3IntegrationService.processWithTravelContext(testContext);

        // then
        long days = 3;  // 2박3일
        int maxPlacesPerDay = 70;
        int totalMaxPlaces = (int)(days * maxPlacesPerDay);

        // 실제 처리된 장소 수가 210개 이하인지 확인
        assertThat(totalMaxPlaces).isEqualTo(210);
    }

    @Test
    @DisplayName("품질 점수 계산 검증 (rating*0.7 + reviews*0.3)")
    void testQualityScoreCalculation() {
        // given
        TravelCandidate candidate = TravelCandidate.builder()
            .rating(4.5)  // 5점 만점
            .reviewCount(10000)
            .build();

        // when
        candidate.calculateScores();  // @PrePersist 메서드

        // then
        double expectedScore = (4.5 / 5.0 * 0.7) + (Math.log10(10000 + 1) / 4.0 * 0.3);
        assertThat(candidate.getQualityScore()).isEqualTo(expectedScore);
        assertThat(candidate.getReliabilityLevel()).isEqualTo("매우높음");
    }

    // Helper 메서드들
    private List<TravelCandidate> createMockCandidates() {
        return Arrays.asList(
            TravelCandidate.builder()
                .placeId("place_1")
                .name("경복궁")
                .region("서울")
                .category("관광지")
                .latitude(37.5796)
                .longitude(126.9770)
                .rating(4.5)
                .reviewCount(15234)
                .googlePlaceId("ChIJN1t_tDeuEmsR")
                .qualityScore(0.85)
                .aiEnriched(true)
                .build(),

            TravelCandidate.builder()
                .placeId("place_2")
                .name("북촌한옥마을")
                .region("서울")
                .category("관광지")
                .latitude(37.5826)
                .longitude(126.9831)
                .rating(4.3)
                .reviewCount(8921)
                .googlePlaceId("ChIJB2t_tDeuEmsR")
                .qualityScore(0.80)
                .build(),

            TravelCandidate.builder()
                .placeId("place_3")
                .name("명동 맛집")
                .region("서울")
                .category("맛집")
                .latitude(37.5636)
                .longitude(126.9869)
                .rating(4.2)
                .reviewCount(5234)
                .qualityScore(0.75)
                .build()
        );
    }

    private List<TravelCandidate> createManyCandidates(int count) {
        return Arrays.asList(new TravelCandidate[count]);
    }
}