package com.compass.domain.chat.prompt.travel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("TravelPlanningPrompt 테스트")
class TravelPlanningPromptTest {
    
    private TravelPlanningPrompt prompt;
    private Map<String, Object> parameters;
    
    @BeforeEach
    void setUp() {
        prompt = new TravelPlanningPrompt();
        parameters = new HashMap<>();
    }
    
    @Test
    @DisplayName("템플릿 기본 정보 확인")
    void testTemplateInfo() {
        assertThat(prompt.getName()).isEqualTo("travel_planning");
        assertThat(prompt.getDescription()).contains("travel planning", "duration-specific");
        assertThat(prompt.getTemplate()).contains(
            "professional travel planner",
            "{{planInclusions}}",
            "{{durationGuidelines}}",
            "{{durationSpecificRequirements}}"
        );
    }
    
    @Test
    @DisplayName("필수 파라미터 확인")
    void testRequiredParameters() {
        String[] required = prompt.getRequiredParameters();
        assertThat(required).containsExactlyInAnyOrder(
            "destination",
            "duration",
            "travelDates",
            "numberOfTravelers",
            "tripPurpose",
            "userPreferences",
            "travelStyle",
            "budgetRange"
        );
    }
    
    @Test
    @DisplayName("완전한 여행 계획 프롬프트 생성")
    void testBuildCompleteTravelPlanPrompt() {
        parameters.put("destination", "Tokyo, Japan");
        parameters.put("duration", "7 days");
        parameters.put("travelDates", "March 15-21, 2024");
        parameters.put("numberOfTravelers", "2");
        parameters.put("tripPurpose", "Honeymoon");
        parameters.put("userPreferences", "Cultural experiences, local food, photography");
        parameters.put("travelStyle", "Comfortable but authentic");
        parameters.put("budgetRange", "$3000-4000 per person");
        parameters.put("specialRequirements", "Vegetarian options needed, prefer ryokans over hotels");
        
        String result = prompt.buildPrompt(parameters);
        
        assertThat(result)
            .contains("Tokyo, Japan")
            .contains("7 days")
            .contains("March 15-21, 2024")
            .contains("Honeymoon")
            .contains("Cultural experiences, local food, photography")
            .contains("$3000-4000 per person")
            .contains("Vegetarian options needed");
    }
    
    @Test
    @DisplayName("특별 요구사항 없이 프롬프트 생성")
    void testBuildPromptWithoutSpecialRequirements() {
        parameters.put("destination", "Paris, France");
        parameters.put("duration", "5 days");
        parameters.put("travelDates", "June 1-5, 2024");
        parameters.put("numberOfTravelers", "4");
        parameters.put("tripPurpose", "Family vacation");
        parameters.put("userPreferences", "Museums, parks, kid-friendly activities");
        parameters.put("travelStyle", "Relaxed pace");
        parameters.put("budgetRange", "$2000 per person");
        
        String result = prompt.buildPrompt(parameters);
        
        assertThat(result)
            .contains("Paris, France")
            .contains("Family vacation")
            .contains("Museums, parks, kid-friendly activities");
    }
    
    @Test
    @DisplayName("필수 파라미터 누락 시 검증 실패")
    void testValidationFailsWithMissingParameters() {
        parameters.put("destination", "London");
        parameters.put("duration", "3 days");
        // Missing other required parameters
        
        assertThat(prompt.validateParameters(parameters)).isFalse();
    }
}