package com.compass.domain.chat.parser.impl;

import com.compass.domain.chat.dto.TripPlanningRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for PatternBasedParser.
 * Tests pattern matching logic without any external dependencies.
 * This test runs without Spring context or AI services.
 */
@DisplayName("PatternBasedParser 단위 테스트")
class PatternBasedParserTest {
    
    private PatternBasedParser parser;
    
    @BeforeEach
    void setUp() {
        // Pure unit test - no Spring context or mocks needed
        parser = new PatternBasedParser();
    }
    
    @Test
    @DisplayName("기본 여행 정보 파싱 테스트")
    void testBasicTripParsing() {
        // Given
        String input = "제주도로 3박4일 여행 가고 싶어요";
        
        // When
        TripPlanningRequest result = parser.parse(input);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDestination()).isEqualTo("제주도");
        // Duration should be applied to dates since no start date was specified
        assertThat(result.getStartDate()).isNotNull();
        assertThat(result.getEndDate()).isNotNull();
        // End date should be 3 days after start date
        assertThat(result.getEndDate()).isEqualTo(result.getStartDate().plusDays(3));
    }
    
    @Test
    @DisplayName("예산 정보 파싱 테스트")
    void testBudgetParsing() {
        // Given
        String input = "예산 100만원으로 부산 여행";
        
        // When
        TripPlanningRequest result = parser.parse(input);
        
        // Then
        assertThat(result.getDestination()).isEqualTo("부산");
        assertThat(result.getBudgetPerPerson()).isEqualTo(1000000);
        assertThat(result.getCurrency()).isEqualTo("KRW");
    }
    
    @Test
    @DisplayName("인원수 파싱 테스트")
    void testTravelerCountParsing() {
        // Given
        String input = "2명이서 강릉 여행";
        
        // When
        TripPlanningRequest result = parser.parse(input);
        
        // Then
        assertThat(result.getDestination()).isEqualTo("강릉");
        assertThat(result.getNumberOfTravelers()).isEqualTo(2);
    }
    
    @Test
    @DisplayName("상대적 날짜 파싱 테스트")
    void testRelativeDateParsing() {
        // Given
        String input = "다음주에 경주 여행";  // Simpler test case
        LocalDate today = LocalDate.now();
        
        // When
        TripPlanningRequest result = parser.parse(input);
        
        // Then
        assertThat(result.getDestination()).isEqualTo("경주");
        assertThat(result.getOrigin()).isEqualTo("서울");  // Default origin
        assertThat(result.getStartDate()).isAfter(today);
        assertThat(result.getStartDate()).isBefore(today.plusDays(15));
    }
    
    @Test
    @DisplayName("관심사 추출 테스트")
    void testInterestExtraction() {
        // Given
        String input = "제주도에서 맛집 탐방하고 자연 경관 구경하고 싶어요";
        
        // When
        TripPlanningRequest result = parser.parse(input);
        
        // Then
        assertThat(result.getInterests()).contains("food", "nature");
    }
    
    @Test
    @DisplayName("여행 스타일 추출 테스트")
    void testTravelStyleExtraction() {
        // Given
        String input = "럭셔리 호텔에서 묵으면서 제주도 여행";
        
        // When
        TripPlanningRequest result = parser.parse(input);
        
        // Then
        assertThat(result.getTravelStyle()).isEqualTo("luxury");
    }
    
    @Test
    @DisplayName("복합 정보 파싱 테스트")
    void testComplexParsing() {
        // Given
        String input = "다음달 15일부터 3박4일로 제주도 여행을 가려고 해요. 예산은 100만원입니다.";
        
        // When
        TripPlanningRequest result = parser.parse(input);
        
        // Then
        assertThat(result.getDestination()).isEqualTo("제주도");
        assertThat(result.getBudgetPerPerson()).isEqualTo(1000000);
        assertThat(result.getStartDate()).isNotNull();
        assertThat(result.getEndDate()).isNotNull();
        assertThat(result.getEndDate()).isAfter(result.getStartDate());
    }
    
    @Test
    @DisplayName("기본값 설정 테스트")
    void testDefaultValues() {
        // Given
        String input = "여행 가고 싶어요";  // Minimal input
        
        // When
        TripPlanningRequest result = parser.parse(input);
        
        // Then
        assertThat(result.getOrigin()).isEqualTo("서울");  // Default origin
        assertThat(result.getNumberOfTravelers()).isEqualTo(1);  // Default travelers
        assertThat(result.getTravelStyle()).isEqualTo("moderate");  // Default style
        assertThat(result.getCurrency()).isEqualTo("KRW");  // Default currency
        assertThat(result.getStartDate()).isNotNull();  // Default start date
        assertThat(result.getEndDate()).isNotNull();  // Default end date
    }
    
    @Test
    @DisplayName("날짜 형식 파싱 테스트")
    void testDateFormatParsing() {
        // Given
        String input = "2024년 12월 25일부터 2024년 12월 28일까지 제주도";
        
        // When
        TripPlanningRequest result = parser.parse(input);
        
        // Then
        assertThat(result.getStartDate()).isEqualTo(LocalDate.of(2024, 12, 25));
        assertThat(result.getEndDate()).isEqualTo(LocalDate.of(2024, 12, 28));
    }
    
    @Test
    @DisplayName("예산 키워드 여행 스타일 추출 테스트")
    void testBudgetKeywordTravelStyle() {
        // Given
        String inputBudget = "저렴하게 제주도 여행";
        String inputLuxury = "고급스럽게 부산 여행";
        
        // When
        TripPlanningRequest budgetResult = parser.parse(inputBudget);
        TripPlanningRequest luxuryResult = parser.parse(inputLuxury);
        
        // Then
        assertThat(budgetResult.getTravelStyle()).isEqualTo("budget");
        assertThat(luxuryResult.getTravelStyle()).isEqualTo("luxury");
    }
    
    @Test
    @DisplayName("가족/커플 인원 추출 테스트")
    void testDescriptiveTravelerCount() {
        // Given
        String inputSolo = "혼자 여행";
        String inputCouple = "커플 여행";
        String inputFamily = "가족 여행";
        
        // When
        TripPlanningRequest soloResult = parser.parse(inputSolo);
        TripPlanningRequest coupleResult = parser.parse(inputCouple);
        TripPlanningRequest familyResult = parser.parse(inputFamily);
        
        // Then
        assertThat(soloResult.getNumberOfTravelers()).isEqualTo(1);
        assertThat(coupleResult.getNumberOfTravelers()).isEqualTo(2);
        assertThat(familyResult.getNumberOfTravelers()).isEqualTo(4);
    }
    
    @Test
    @DisplayName("전략 이름 확인 테스트")
    void testStrategyName() {
        // When
        String strategy = parser.getStrategyName();
        
        // Then
        assertThat(strategy).isEqualTo("pattern");
    }
    
    @Test
    @DisplayName("당일치기 여행 파싱 테스트")
    void testDayTripParsing() {
        // Given
        String input1 = "성수로 당일치기 여행";
        String input2 = "제주도 당일 여행 가고 싶어";
        String input3 = "부산으로 하루 여행";
        
        // When
        TripPlanningRequest result1 = parser.parse(input1);
        TripPlanningRequest result2 = parser.parse(input2);
        TripPlanningRequest result3 = parser.parse(input3);
        
        // Then
        assertThat(result1.getDestination()).isEqualTo("성수");
        assertThat(result1.getStartDate()).isNotNull();
        assertThat(result1.getEndDate()).isEqualTo(result1.getStartDate()); // Same day
        assertThat(result1.getPreferences()).containsEntry("trip_type", "당일치기");
        
        assertThat(result2.getDestination()).isEqualTo("제주도");
        assertThat(result2.getStartDate()).isNotNull();
        assertThat(result2.getEndDate()).isEqualTo(result2.getStartDate());
        assertThat(result2.getPreferences()).containsEntry("trip_type", "당일치기");
        
        assertThat(result3.getDestination()).isEqualTo("부산");
        assertThat(result3.getStartDate()).isNotNull();
        assertThat(result3.getEndDate()).isEqualTo(result3.getStartDate());
        assertThat(result3.getPreferences()).containsEntry("trip_type", "당일치기");
    }
    
    @Test
    @DisplayName("한국어 기간 표현 파싱 테스트")
    void testKoreanDurationParsing() {
        // Given
        String input1 = "제주도로 이틀동안 여행";
        String input2 = "사흘간 부산 여행";
        String input3 = "나흘 정도 강릉 여행";
        
        // When
        TripPlanningRequest result1 = parser.parse(input1);
        TripPlanningRequest result2 = parser.parse(input2);
        TripPlanningRequest result3 = parser.parse(input3);
        
        // Then
        assertThat(result1.getDestination()).isEqualTo("제주도");
        assertThat(result1.getEndDate()).isEqualTo(result1.getStartDate().plusDays(1)); // 이틀 = 2 days
        
        assertThat(result2.getDestination()).isEqualTo("부산");
        assertThat(result2.getEndDate()).isEqualTo(result2.getStartDate().plusDays(2)); // 사흘 = 3 days
        
        assertThat(result3.getDestination()).isEqualTo("강릉");
        assertThat(result3.getEndDate()).isEqualTo(result3.getStartDate().plusDays(3)); // 나흘 = 4 days
    }
    
    @Test
    @DisplayName("날짜 범위 표현 파싱 테스트")
    void testDateRangeParsing() {
        // Given
        String input1 = "12월 28일부터 30일까지 제주도 여행";
        String input2 = "28일~29일 부산 여행";
        int currentYear = LocalDate.now().getYear();
        
        // When
        TripPlanningRequest result1 = parser.parse(input1);
        TripPlanningRequest result2 = parser.parse(input2);
        
        // Then
        assertThat(result1.getDestination()).isEqualTo("제주도");
        assertThat(result1.getStartDate()).isEqualTo(LocalDate.of(currentYear, 12, 28));
        assertThat(result1.getEndDate()).isEqualTo(LocalDate.of(currentYear, 12, 30));
        
        assertThat(result2.getDestination()).isEqualTo("부산");
        // Month should be current or next month based on current date
        assertThat(result2.getStartDate().getDayOfMonth()).isEqualTo(28);
        assertThat(result2.getEndDate().getDayOfMonth()).isEqualTo(29);
    }
    
    @Test
    @DisplayName("다양한 기간 표현 파싱 테스트")
    void testVariousDurationExpressions() {
        // Given
        String input1 = "2일동안 제주도 여행";
        String input2 = "3일간 부산 여행";
        String input3 = "오늘 하루 강릉 여행";
        
        // When
        TripPlanningRequest result1 = parser.parse(input1);
        TripPlanningRequest result2 = parser.parse(input2);
        TripPlanningRequest result3 = parser.parse(input3);
        
        // Then
        assertThat(result1.getDestination()).isEqualTo("제주도");
        assertThat(result1.getEndDate()).isEqualTo(result1.getStartDate().plusDays(1)); // 2일동안 = 2 days
        
        assertThat(result2.getDestination()).isEqualTo("부산");
        assertThat(result2.getEndDate()).isEqualTo(result2.getStartDate().plusDays(2)); // 3일간 = 3 days
        
        assertThat(result3.getDestination()).isEqualTo("강릉");
        assertThat(result3.getStartDate()).isEqualTo(LocalDate.now()); // 오늘
        assertThat(result3.getEndDate()).isEqualTo(LocalDate.now()); // 하루 = same day
        assertThat(result3.getPreferences()).containsEntry("trip_type", "당일치기");
    }
    
    @Test
    @DisplayName("복합 날짜 정보 파싱 테스트")
    void testComplexDateParsing() {
        // Given
        String input = "다음주 금요일부터 일요일까지 제주도 2박3일 여행";
        
        // When
        TripPlanningRequest result = parser.parse(input);
        
        // Then
        assertThat(result.getDestination()).isEqualTo("제주도");
        assertThat(result.getStartDate()).isNotNull();
        assertThat(result.getEndDate()).isNotNull();
        // Should be 2 nights difference
        assertThat(result.getEndDate()).isEqualTo(result.getStartDate().plusDays(2));
    }
}