package com.compass.domain.chat.prompt.templates;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for basic itinerary templates (REQ-AI-003)
 */
@Tag("unit")
@DisplayName("기본 여행 일정 템플릿 테스트")
class ItineraryTemplatesTest {
    
    private DayTripTemplate dayTripTemplate;
    private OneNightTwoDaysTemplate oneNightTwoDaysTemplate;
    private TwoNightsThreeDaysTemplate twoNightsThreeDaysTemplate;
    private ThreeNightsFourDaysTemplate threeNightsFourDaysTemplate;
    
    @BeforeEach
    void setUp() {
        dayTripTemplate = new DayTripTemplate();
        oneNightTwoDaysTemplate = new OneNightTwoDaysTemplate();
        twoNightsThreeDaysTemplate = new TwoNightsThreeDaysTemplate();
        threeNightsFourDaysTemplate = new ThreeNightsFourDaysTemplate();
    }
    
    @Test
    @DisplayName("당일치기 템플릿 - 키워드 인식 테스트")
    void testDayTripTemplateKeywordRecognition() {
        // Given
        String[] validInputs = {
            "당일치기 여행 계획 짜줘",
            "서울에서 당일 여행 어디가 좋을까?",
            "일일 투어 추천해줘",
            "day trip to Busan",
            "하루 여행 일정 만들어줘"
        };
        
        String[] invalidInputs = {
            "1박 2일 여행",
            "3일 여행 계획",
            "일주일 여행"
        };
        
        // When & Then
        for (String input : validInputs) {
            assertThat(dayTripTemplate.supports(input))
                .as("Should support input: " + input)
                .isTrue();
        }
        
        for (String input : invalidInputs) {
            assertThat(dayTripTemplate.supports(input))
                .as("Should not support input: " + input)
                .isFalse();
        }
    }
    
    @Test
    @DisplayName("당일치기 템플릿 - 프롬프트 생성 테스트")
    void testDayTripTemplatePromptGeneration() {
        // Given
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("destination", "제주도");
        parameters.put("travel_date", "2024-03-15");
        parameters.put("start_time", "09:00");
        parameters.put("end_time", "18:00");
        parameters.put("travel_style", "자연 경관 중심");
        parameters.put("budget", "50000");
        parameters.put("companions", "가족 (부모님, 초등학생)");
        parameters.put("special_requirements", "- 어린이 친화적 장소 위주");
        
        // When
        String prompt = dayTripTemplate.buildPrompt(parameters);
        
        // Then
        assertThat(prompt).contains("당일치기 여행 일정");
        assertThat(prompt).contains("제주도");
        assertThat(prompt).contains("2024-03-15");
        assertThat(prompt).contains("09:00");
        assertThat(prompt).contains("18:00");
        assertThat(prompt).contains("자연 경관 중심");
        assertThat(prompt).contains("50000원");
        assertThat(prompt).contains("가족 (부모님, 초등학생)");
        assertThat(prompt).contains("어린이 친화적 장소 위주");
    }
    
    @Test
    @DisplayName("1박 2일 템플릿 - 키워드 인식 테스트")
    void testOneNightTwoDaysTemplateKeywordRecognition() {
        // Given
        String[] validInputs = {
            "1박 2일 여행 계획",
            "1박2일 부산 여행",
            "일박 이일 제주도",
            "1n2d travel plan",
            "이틀 여행 일정"
        };
        
        // When & Then
        for (String input : validInputs) {
            assertThat(oneNightTwoDaysTemplate.supports(input))
                .as("Should support input: " + input)
                .isTrue();
        }
    }
    
    @Test
    @DisplayName("1박 2일 템플릿 - 필수 파라미터 검증")
    void testOneNightTwoDaysTemplateParameterValidation() {
        // Given
        Map<String, Object> incompleteParams = new HashMap<>();
        incompleteParams.put("destination", "부산");
        incompleteParams.put("start_date", "2024-03-15");
        // Missing: end_date, travel_style, budget, companions, accommodation_preference
        
        // When & Then
        assertThatThrownBy(() -> oneNightTwoDaysTemplate.buildPrompt(incompleteParams))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Missing required parameters");
    }
    
    @Test
    @DisplayName("2박 3일 템플릿 - 프롬프트 생성 테스트")
    void testTwoNightsThreeDaysTemplatePromptGeneration() {
        // Given
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("destination", "경주");
        parameters.put("start_date", "2024-04-01");
        parameters.put("end_date", "2024-04-03");
        parameters.put("travel_style", "역사 문화 탐방");
        parameters.put("budget", "300000");
        parameters.put("companions", "친구 2명");
        parameters.put("accommodation_preference", "한옥 게스트하우스");
        
        // When
        String prompt = twoNightsThreeDaysTemplate.buildPrompt(parameters);
        
        // Then
        assertThat(prompt).contains("2박 3일 여행 일정");
        assertThat(prompt).contains("경주");
        assertThat(prompt).contains("2024-04-01");
        assertThat(prompt).contains("2024-04-03");
        assertThat(prompt).contains("역사 문화 탐방");
        assertThat(prompt).contains("300000원");
        assertThat(prompt).contains("한옥 게스트하우스");
        assertThat(prompt).contains("Day 1");
        assertThat(prompt).contains("Day 2");
        assertThat(prompt).contains("Day 3");
    }
    
    @Test
    @DisplayName("3박 4일 템플릿 - 키워드 인식 테스트")
    void testThreeNightsFourDaysTemplateKeywordRecognition() {
        // Given
        String[] validInputs = {
            "3박 4일 여행",
            "3박4일 제주도 여행",
            "삼박 사일 계획",
            "3n4d itinerary",
            "4일 여행 일정"
        };
        
        // When & Then
        for (String input : validInputs) {
            assertThat(threeNightsFourDaysTemplate.supports(input))
                .as("Should support input: " + input)
                .isTrue();
        }
    }
    
    @Test
    @DisplayName("3박 4일 템플릿 - 선택적 파라미터 테스트")
    void testThreeNightsFourDaysTemplateOptionalParameters() {
        // Given
        Map<String, Object> parameters = new HashMap<>();
        // Required parameters
        parameters.put("destination", "제주도");
        parameters.put("start_date", "2024-05-01");
        parameters.put("end_date", "2024-05-04");
        parameters.put("travel_style", "힐링 및 휴양");
        parameters.put("budget", "600000");
        parameters.put("companions", "연인");
        parameters.put("accommodation_preference", "리조트");
        // Optional parameter not provided
        
        // When
        String prompt = threeNightsFourDaysTemplate.buildPrompt(parameters);
        
        // Then
        assertThat(prompt).contains("3박 4일 여행 일정");
        assertThat(prompt).contains("제주도");
        assertThat(prompt).doesNotContain("special_requirements");
    }
    
    @Test
    @DisplayName("모든 템플릿 - 기본 정보 확인")
    void testAllTemplatesBasicInfo() {
        // When & Then
        assertThat(dayTripTemplate.getName()).isEqualTo("day_trip");
        assertThat(dayTripTemplate.getDescription()).contains("당일치기");
        
        assertThat(oneNightTwoDaysTemplate.getName()).isEqualTo("one_night_two_days");
        assertThat(oneNightTwoDaysTemplate.getDescription()).contains("1박 2일");
        
        assertThat(twoNightsThreeDaysTemplate.getName()).isEqualTo("two_nights_three_days");
        assertThat(twoNightsThreeDaysTemplate.getDescription()).contains("2박 3일");
        
        assertThat(threeNightsFourDaysTemplate.getName()).isEqualTo("three_nights_four_days");
        assertThat(threeNightsFourDaysTemplate.getDescription()).contains("3박 4일");
    }
    
    @Test
    @DisplayName("모든 템플릿 - 필수 파라미터 확인")
    void testAllTemplatesRequiredParameters() {
        // Day Trip
        String[] dayTripRequired = dayTripTemplate.getRequiredParameters();
        assertThat(dayTripRequired).contains(
            "destination", "travel_date", "start_time", "end_time",
            "travel_style", "budget", "companions"
        );
        
        // 1N2D
        String[] oneNightRequired = oneNightTwoDaysTemplate.getRequiredParameters();
        assertThat(oneNightRequired).contains(
            "destination", "start_date", "end_date", "travel_style",
            "budget", "companions", "accommodation_preference"
        );
        
        // 2N3D
        String[] twoNightsRequired = twoNightsThreeDaysTemplate.getRequiredParameters();
        assertThat(twoNightsRequired).contains(
            "destination", "start_date", "end_date", "travel_style",
            "budget", "companions", "accommodation_preference"
        );
        
        // 3N4D
        String[] threeNightsRequired = threeNightsFourDaysTemplate.getRequiredParameters();
        assertThat(threeNightsRequired).contains(
            "destination", "start_date", "end_date", "travel_style",
            "budget", "companions", "accommodation_preference"
        );
    }
}