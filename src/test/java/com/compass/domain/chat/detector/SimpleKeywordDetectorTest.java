package com.compass.domain.chat.detector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SimpleKeywordDetector 단위 테스트
 * REQ-PROMPT-002: 키워드 기반 템플릿 선택 시스템 테스트
 */
@Tag("unit")
class SimpleKeywordDetectorTest {
    
    private SimpleKeywordDetector detector;
    
    @BeforeEach
    void setUp() {
        detector = new SimpleKeywordDetector();
    }
    
    @Test
    @DisplayName("일정 계획 템플릿 감지 테스트")
    void testDetectDailyItineraryTemplate() {
        // given
        String userMessage = "부산 2박3일 상세일정 좀 짜줘";
        
        // when
        String template = detector.detectTemplate(userMessage);
        
        // then
        assertThat(template).isEqualTo("daily_itinerary");
    }
    
    @Test
    @DisplayName("예산 최적화 템플릿 감지 테스트")
    void testDetectBudgetOptimizationTemplate() {
        // given
        String userMessage = "제주도 가성비 좋은 여행 추천해줘";
        
        // when
        String template = detector.detectTemplate(userMessage);
        
        // then
        assertThat(template).isEqualTo("budget_optimization");
    }
    
    @Test
    @DisplayName("현지 체험 템플릿 감지 테스트")
    void testDetectLocalExperienceTemplate() {
        // given
        String userMessage = "서울 현지인들이 가는 맛집 알려줘";
        
        // when
        String template = detector.detectTemplate(userMessage);
        
        // then
        assertThat(template).isEqualTo("local_experience");
    }
    
    @Test
    @DisplayName("목적지 탐색 템플릿 감지 테스트")
    void testDetectDestinationDiscoveryTemplate() {
        // given
        String userMessage = "겨울에 가볼만한 관광지 어디 있을까?";
        
        // when
        String template = detector.detectTemplate(userMessage);
        
        // then
        assertThat(template).isEqualTo("destination_discovery");
    }
    
    @Test
    @DisplayName("여행 추천 템플릿 감지 테스트")
    void testDetectTravelRecommendationTemplate() {
        // given
        String userMessage = "인기 있는 여행지 추천해줘";
        
        // when
        String template = detector.detectTemplate(userMessage);
        
        // then
        assertThat(template).isEqualTo("travel_recommendation");
    }
    
    @Test
    @DisplayName("기본 여행 계획 템플릿 감지 테스트")
    void testDetectTravelPlanningTemplate() {
        // given
        String userMessage = "여행 계획 도와줘";
        
        // when
        String template = detector.detectTemplate(userMessage);
        
        // then
        assertThat(template).isEqualTo("travel_planning");
    }
    
    @Test
    @DisplayName("빈 메시지 처리 테스트")
    void testDetectTemplateWithEmptyMessage() {
        // given
        String userMessage = "";
        
        // when
        String template = detector.detectTemplate(userMessage);
        
        // then
        assertThat(template).isEqualTo("travel_planning");
    }
    
    @Test
    @DisplayName("null 메시지 처리 테스트")
    void testDetectTemplateWithNullMessage() {
        // given
        String userMessage = null;
        
        // when
        String template = detector.detectTemplate(userMessage);
        
        // then
        assertThat(template).isEqualTo("travel_planning");
    }
    
    @Test
    @DisplayName("서울 목적지 감지 테스트")
    void testDetectSeoulDestination() {
        // given
        String userMessage = "서울 경복궁 가고 싶어";
        
        // when
        Optional<String> destination = detector.detectDestination(userMessage);
        
        // then
        assertThat(destination).isPresent();
        assertThat(destination.get()).isEqualTo("seoul");
    }
    
    @Test
    @DisplayName("부산 목적지 감지 테스트")
    void testDetectBusanDestination() {
        // given
        String userMessage = "해운대 바다 보러 가자";
        
        // when
        Optional<String> destination = detector.detectDestination(userMessage);
        
        // then
        assertThat(destination).isPresent();
        assertThat(destination.get()).isEqualTo("busan");
    }
    
    @Test
    @DisplayName("제주 목적지 감지 테스트")
    void testDetectJejuDestination() {
        // given
        String userMessage = "한라산 등산하고 싶어";
        
        // when
        Optional<String> destination = detector.detectDestination(userMessage);
        
        // then
        assertThat(destination).isPresent();
        assertThat(destination.get()).isEqualTo("jeju");
    }
    
    @Test
    @DisplayName("목적지 없는 메시지 처리 테스트")
    void testDetectNoDestination() {
        // given
        String userMessage = "여행 가고 싶다";
        
        // when
        Optional<String> destination = detector.detectDestination(userMessage);
        
        // then
        assertThat(destination).isEmpty();
    }
    
    @Test
    @DisplayName("당일치기 기간 감지 테스트")
    void testDetectDayTripDuration() {
        // given
        String userMessage = "서울 당일치기 코스";
        
        // when
        Optional<String> duration = detector.detectDuration(userMessage);
        
        // then
        assertThat(duration).isPresent();
        assertThat(duration.get()).isEqualTo("day_trip");
    }
    
    @Test
    @DisplayName("1박2일 기간 감지 테스트")
    void testDetect1Night2DaysDuration() {
        // given
        String userMessage = "부산 1박2일 여행";
        
        // when
        Optional<String> duration = detector.detectDuration(userMessage);
        
        // then
        assertThat(duration).isPresent();
        assertThat(duration.get()).isEqualTo("1night2days");
    }
    
    @Test
    @DisplayName("2박3일 기간 감지 테스트")
    void testDetect2Nights3DaysDuration() {
        // given
        String userMessage = "제주도 2박3일 일정";
        
        // when
        Optional<String> duration = detector.detectDuration(userMessage);
        
        // then
        assertThat(duration).isPresent();
        assertThat(duration.get()).isEqualTo("2nights3days");
    }
    
    @Test
    @DisplayName("일주일 기간 감지 테스트")
    void testDetectWeekDuration() {
        // given
        String userMessage = "일주일 동안 여행하려고";
        
        // when
        Optional<String> duration = detector.detectDuration(userMessage);
        
        // then
        assertThat(duration).isPresent();
        assertThat(duration.get()).isEqualTo("week");
    }
    
    @Test
    @DisplayName("가족 여행 유형 감지 테스트")
    void testDetectFamilyTravelType() {
        // given
        String userMessage = "가족이랑 함께 여행";
        
        // when
        Optional<String> travelType = detector.detectTravelType(userMessage);
        
        // then
        assertThat(travelType).isPresent();
        assertThat(travelType.get()).isEqualTo("family");
    }
    
    @Test
    @DisplayName("커플 여행 유형 감지 테스트")
    void testDetectCoupleTravelType() {
        // given
        String userMessage = "연인과 로맨틱한 여행";
        
        // when
        Optional<String> travelType = detector.detectTravelType(userMessage);
        
        // then
        assertThat(travelType).isPresent();
        assertThat(travelType.get()).isEqualTo("couple");
    }
    
    @Test
    @DisplayName("혼자 여행 유형 감지 테스트")
    void testDetectSoloTravelType() {
        // given
        String userMessage = "나홀로 자유여행";
        
        // when
        Optional<String> travelType = detector.detectTravelType(userMessage);
        
        // then
        assertThat(travelType).isPresent();
        assertThat(travelType.get()).isEqualTo("solo");
    }
    
    @Test
    @DisplayName("친구 여행 유형 감지 테스트")
    void testDetectFriendsTravelType() {
        // given
        String userMessage = "친구들과 우정여행";
        
        // when
        Optional<String> travelType = detector.detectTravelType(userMessage);
        
        // then
        assertThat(travelType).isPresent();
        assertThat(travelType.get()).isEqualTo("friends");
    }
    
    @Test
    @DisplayName("비즈니스 여행 유형 감지 테스트")
    void testDetectBusinessTravelType() {
        // given
        String userMessage = "출장 일정";
        
        // when
        Optional<String> travelType = detector.detectTravelType(userMessage);
        
        // then
        assertThat(travelType).isPresent();
        assertThat(travelType.get()).isEqualTo("business");
    }
    
    @Test
    @DisplayName("전체 정보 추출 통합 테스트")
    void testExtractAllInfo() {
        // given
        String userMessage = "부산 2박3일 가족여행 일정 짜줘";
        
        // when
        Map<String, Object> info = detector.extractAllInfo(userMessage);
        
        // then
        assertThat(info).isNotNull();
        assertThat(info.get("template")).isEqualTo("daily_itinerary");
        assertThat(info.get("destination")).isEqualTo("busan");
        assertThat(info.get("duration")).isEqualTo("2nights3days");
        assertThat(info.get("travelType")).isEqualTo("family");
        assertThat(info.get("originalMessage")).isEqualTo(userMessage);
    }
    
    @Test
    @DisplayName("복합 키워드 우선순위 테스트")
    void testKeywordPriority() {
        // given
        String userMessage = "제주도 예산 절약하면서 현지 맛집 탐방 일정";
        
        // when
        String template = detector.detectTemplate(userMessage);
        
        // then
        // 더 긴 키워드와 많은 매칭이 있는 템플릿이 선택되어야 함
        assertThat(template).isIn("budget_optimization", "local_experience", "daily_itinerary");
    }
    
    @Test
    @DisplayName("대소문자 구분 없이 감지 테스트")
    void testCaseInsensitiveDetection() {
        // given
        String userMessage = "SEOUL Travel BUDGET optimization";
        
        // when
        Map<String, Object> info = detector.extractAllInfo(userMessage);
        
        // then
        assertThat(info.get("template")).isEqualTo("budget_optimization");
        assertThat(info.get("destination")).isEqualTo("seoul");
    }
}