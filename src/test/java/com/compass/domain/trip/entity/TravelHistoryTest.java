package com.compass.domain.trip.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TravelHistory 엔티티 단위 테스트
 * REQ-LLM-004: 여행 히스토리 엔티티 테스트
 */
@Tag("unit")
class TravelHistoryTest {
    
    private TravelHistory travelHistory;
    
    @BeforeEach
    void setUp() {
        travelHistory = TravelHistory.builder()
                .id(1L)
                .userId(100L)
                .destination("제주도")
                .startDate(LocalDate.of(2024, 1, 10))
                .endDate(LocalDate.of(2024, 1, 12))
                .travelType("가족")
                .companionCount(4)
                .totalBudget(2000000)
                .actualExpense(1800000)
                .rating(5)
                .travelStyle("휴양")
                .accommodationType("호텔")
                .transportationMode("렌터카")
                .weatherCondition("맑음")
                .season("겨울")
                .usedAiPlan(true)
                .aiSatisfaction(4)
                .build();
    }
    
    @Test
    @DisplayName("TravelHistory 엔티티 생성 테스트")
    void testCreateTravelHistory() {
        // then
        assertThat(travelHistory).isNotNull();
        assertThat(travelHistory.getId()).isEqualTo(1L);
        assertThat(travelHistory.getUserId()).isEqualTo(100L);
        assertThat(travelHistory.getDestination()).isEqualTo("제주도");
        assertThat(travelHistory.getTravelType()).isEqualTo("가족");
        assertThat(travelHistory.getRating()).isEqualTo(5);
    }
    
    @Test
    @DisplayName("여행 기간 계산 테스트 - 정상 케이스")
    void testGetTripDuration() {
        // when
        long duration = travelHistory.getTripDuration();
        
        // then
        assertThat(duration).isEqualTo(3); // 1/10 ~ 1/12 = 3일
    }
    
    @Test
    @DisplayName("여행 기간 계산 테스트 - 당일치기")
    void testGetTripDurationSameDay() {
        // given
        travelHistory.setStartDate(LocalDate.of(2024, 1, 10));
        travelHistory.setEndDate(LocalDate.of(2024, 1, 10));
        
        // when
        long duration = travelHistory.getTripDuration();
        
        // then
        assertThat(duration).isEqualTo(1); // 당일치기는 1일
    }
    
    @Test
    @DisplayName("여행 기간 계산 테스트 - null 날짜")
    void testGetTripDurationWithNullDates() {
        // given
        travelHistory.setStartDate(null);
        travelHistory.setEndDate(null);
        
        // when
        long duration = travelHistory.getTripDuration();
        
        // then
        assertThat(duration).isEqualTo(0);
    }
    
    @Test
    @DisplayName("예산 대비 실제 지출 비율 계산 테스트")
    void testGetExpenseRatio() {
        // when
        double ratio = travelHistory.getExpenseRatio();
        
        // then
        assertThat(ratio).isEqualTo(0.9); // 1,800,000 / 2,000,000 = 0.9
    }
    
    @Test
    @DisplayName("예산 대비 실제 지출 비율 - 예산 초과")
    void testGetExpenseRatioOverBudget() {
        // given
        travelHistory.setTotalBudget(1000000);
        travelHistory.setActualExpense(1200000);
        
        // when
        double ratio = travelHistory.getExpenseRatio();
        
        // then
        assertThat(ratio).isEqualTo(1.2); // 예산 초과
    }
    
    @Test
    @DisplayName("예산 대비 실제 지출 비율 - null 값 처리")
    void testGetExpenseRatioWithNullValues() {
        // given
        travelHistory.setTotalBudget(null);
        travelHistory.setActualExpense(null);
        
        // when
        double ratio = travelHistory.getExpenseRatio();
        
        // then
        assertThat(ratio).isEqualTo(0.0);
    }
    
    @Test
    @DisplayName("예산 대비 실제 지출 비율 - 예산 0")
    void testGetExpenseRatioWithZeroBudget() {
        // given
        travelHistory.setTotalBudget(0);
        travelHistory.setActualExpense(100000);
        
        // when
        double ratio = travelHistory.getExpenseRatio();
        
        // then
        assertThat(ratio).isEqualTo(0.0);
    }
    
    @Test
    @DisplayName("방문한 장소 JSONB 필드 테스트")
    void testVisitedPlacesJsonb() {
        // given
        Map<String, Object> visitedPlaces = new HashMap<>();
        Map<String, Object> place1 = new HashMap<>();
        place1.put("name", "성산일출봉");
        place1.put("type", "attraction");
        place1.put("rating", 5);
        
        Map<String, Object> place2 = new HashMap<>();
        place2.put("name", "흑돼지거리");
        place2.put("type", "restaurant");
        place2.put("rating", 4);
        
        visitedPlaces.put("day1", place1);
        visitedPlaces.put("day2", place2);
        
        // when
        travelHistory.setVisitedPlaces(visitedPlaces);
        
        // then
        assertThat(travelHistory.getVisitedPlaces()).isNotNull();
        assertThat(travelHistory.getVisitedPlaces()).hasSize(2);
        assertThat(travelHistory.getVisitedPlaces().get("day1")).isNotNull();
    }
    
    @Test
    @DisplayName("메타데이터 JSONB 필드 테스트")
    void testMetadataJsonb() {
        // given
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("photos_count", 150);
        metadata.put("favorite_meal", "해물찜");
        metadata.put("weather_satisfaction", "매우 만족");
        
        // when
        travelHistory.setMetadata(metadata);
        
        // then
        assertThat(travelHistory.getMetadata()).isNotNull();
        assertThat(travelHistory.getMetadata().get("photos_count")).isEqualTo(150);
        assertThat(travelHistory.getMetadata().get("favorite_meal")).isEqualTo("해물찜");
    }
    
    @Test
    @DisplayName("onCreate 메서드 테스트")
    void testOnCreate() {
        // given
        TravelHistory newHistory = new TravelHistory();
        
        // when
        newHistory.onCreate();
        
        // then
        assertThat(newHistory.getCreatedAt()).isNotNull();
        assertThat(newHistory.getUpdatedAt()).isNotNull();
        // 타임스탬프 정밀도 문제로 나노초 단위 차이가 발생할 수 있으므로
        // 같은 초 내에 생성되었는지만 확인 (년월일시분초까지만 비교)
        assertThat(newHistory.getCreatedAt().withNano(0))
            .isEqualTo(newHistory.getUpdatedAt().withNano(0));
    }
    
    @Test
    @DisplayName("onUpdate 메서드 테스트")
    void testOnUpdate() throws InterruptedException {
        // given
        TravelHistory history = new TravelHistory();
        history.onCreate();
        LocalDateTime originalCreatedAt = history.getCreatedAt();
        LocalDateTime originalUpdatedAt = history.getUpdatedAt();
        
        // 시간 차이를 만들기 위해 잠시 대기
        Thread.sleep(10);
        
        // when
        history.onUpdate();
        
        // then
        assertThat(history.getCreatedAt()).isEqualTo(originalCreatedAt);
        assertThat(history.getUpdatedAt()).isAfter(originalUpdatedAt);
    }
    
    @Test
    @DisplayName("Builder 패턴 테스트")
    void testBuilderPattern() {
        // given & when
        TravelHistory history = TravelHistory.builder()
                .userId(200L)
                .destination("부산")
                .startDate(LocalDate.of(2024, 3, 1))
                .endDate(LocalDate.of(2024, 3, 3))
                .travelType("친구")
                .rating(4)
                .visitedPlaces(new HashMap<>())
                .metadata(new HashMap<>())
                .build();
        
        // then
        assertThat(history.getUserId()).isEqualTo(200L);
        assertThat(history.getDestination()).isEqualTo("부산");
        assertThat(history.getTravelType()).isEqualTo("친구");
        assertThat(history.getRating()).isEqualTo(4);
        assertThat(history.getVisitedPlaces()).isNotNull();
        assertThat(history.getMetadata()).isNotNull();
    }
    
    @Test
    @DisplayName("기본값 테스트")
    void testDefaultValues() {
        // given & when
        TravelHistory history = TravelHistory.builder()
                .userId(300L)
                .destination("서울")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now())
                .build();
        
        // then
        assertThat(history.getVisitedPlaces()).isNotNull();
        assertThat(history.getVisitedPlaces()).isEmpty();
        assertThat(history.getMetadata()).isNotNull();
        assertThat(history.getMetadata()).isEmpty();
    }
}