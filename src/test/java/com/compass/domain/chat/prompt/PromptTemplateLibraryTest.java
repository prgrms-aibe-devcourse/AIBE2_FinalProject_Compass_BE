package com.compass.domain.chat.prompt;

import com.compass.domain.chat.prompt.templates.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * REQ-PROMPT-003: 템플릿 라이브러리 테스트
 * 20+ 여행 시나리오별 템플릿 검증
 */
@Tag("unit")
class PromptTemplateLibraryTest {

    private PromptTemplateRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new PromptTemplateRegistry();
        registry.initialize();
    }

    @Test
    @DisplayName("20개 이상의 템플릿이 등록되어 있어야 한다")
    void shouldHaveAtLeast20Templates() {
        // when
        var templateNames = registry.getTemplateNames();
        
        // then
        assertThat(templateNames).hasSizeGreaterThanOrEqualTo(20);
    }

    @Test
    @DisplayName("가족 여행 템플릿이 정상 작동해야 한다")
    void familyTripTemplateShouldWork() {
        // given
        var template = new FamilyTripTemplate();
        Map<String, Object> params = new HashMap<>();
        params.put("destination", "제주도");
        params.put("start_date", "2025-10-01");
        params.put("end_date", "2025-10-03");
        params.put("family_members", "4");
        params.put("children_ages", "8, 12");
        params.put("budget", "200만원");
        params.put("accommodation_type", "리조트");
        
        // when
        String prompt = template.buildPrompt(params);
        
        // then
        assertThat(prompt).contains("가족 여행 계획");
        assertThat(prompt).contains("제주도");
        assertThat(prompt).contains("아이들이 즐길 수 있는 활동");
    }

    @Test
    @DisplayName("커플 여행 템플릿이 정상 작동해야 한다")
    void coupleTripTemplateShouldWork() {
        // given
        var template = new CoupleTripTemplate();
        Map<String, Object> params = new HashMap<>();
        params.put("destination", "파리");
        params.put("start_date", "2025-09-15");
        params.put("end_date", "2025-09-20");
        params.put("budget", "500만원");
        params.put("occasion", "honeymoon");
        params.put("romance_level", "high");
        
        // when
        String prompt = template.buildPrompt(params);
        
        // then
        assertThat(prompt).contains("커플을 위한 로맨틱한 여행");
        assertThat(prompt).contains("파리");
        assertThat(prompt).contains("로맨틱한 숙소");
    }

    @Test
    @DisplayName("배낭여행 템플릿이 정상 작동해야 한다")
    void backpackingTemplateShouldWork() {
        // given
        var template = new BackpackingTemplate();
        Map<String, Object> params = new HashMap<>();
        params.put("destination", "태국");
        params.put("start_date", "2025-08-01");
        params.put("end_date", "2025-08-14");
        params.put("daily_budget", "5만원");
        params.put("travel_style", "adventure");
        params.put("accommodation_preference", "hostel");
        
        // when
        String prompt = template.buildPrompt(params);
        
        // then
        assertThat(prompt).contains("배낭여행 계획");
        assertThat(prompt).contains("저렴한 숙소");
        assertThat(prompt).contains("호스텔");
    }

    @Test
    @DisplayName("템플릿 키워드 감지가 작동해야 한다")
    void templateKeywordDetectionShouldWork() {
        // given
        var familyTemplate = new FamilyTripTemplate();
        var coupleTemplate = new CoupleTripTemplate();
        var luxuryTemplate = new LuxuryTravelTemplate();
        
        // then
        assertThat(familyTemplate.supports("가족과 함께 여행")).isTrue();
        assertThat(familyTemplate.supports("아이들과 제주도")).isTrue();
        assertThat(familyTemplate.supports("비즈니스 출장")).isFalse();
        
        assertThat(coupleTemplate.supports("신혼여행 계획")).isTrue();
        assertThat(coupleTemplate.supports("로맨틱한 여행")).isTrue();
        
        assertThat(luxuryTemplate.supports("럭셔리 호텔")).isTrue();
        assertThat(luxuryTemplate.supports("5성급 리조트")).isTrue();
    }

    @Test
    @DisplayName("필수 파라미터 누락 시 예외가 발생해야 한다")
    void shouldThrowExceptionWhenRequiredParametersMissing() {
        // given
        var template = new BusinessTripTemplate();
        Map<String, Object> params = new HashMap<>();
        params.put("destination", "도쿄");
        // meeting_locations 누락
        
        // then
        assertThatThrownBy(() -> template.buildPrompt(params))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing required parameters");
    }

    @Test
    @DisplayName("모든 시나리오별 템플릿이 등록되어야 한다")
    void allScenarioTemplatesShouldBeRegistered() {
        // then
        assertThat(registry.hasTemplate("family_trip")).isTrue();
        assertThat(registry.hasTemplate("couple_trip")).isTrue();
        assertThat(registry.hasTemplate("business_trip")).isTrue();
        assertThat(registry.hasTemplate("backpacking")).isTrue();
        assertThat(registry.hasTemplate("luxury_travel")).isTrue();
        assertThat(registry.hasTemplate("adventure_travel")).isTrue();
        assertThat(registry.hasTemplate("cultural_tour")).isTrue();
        assertThat(registry.hasTemplate("food_tour")).isTrue();
        assertThat(registry.hasTemplate("relaxation")).isTrue();
    }

    @Test
    @DisplayName("템플릿 파라미터 확인이 작동해야 한다")
    void templateParametersVerification() {
        // given
        var template = new AdventureTravelTemplate();
        
        // when
        String[] requiredParams = template.getRequiredParameters();
        String[] optionalParams = template.getOptionalParameters();
        
        // then
        assertThat(requiredParams).contains("destination", "adventure_types", "fitness_level");
        assertThat(optionalParams).contains("special_requirements");
    }

    @Test
    @DisplayName("PromptTemplateRegistry를 통한 템플릿 빌드가 작동해야 한다")
    void buildPromptThroughRegistryShouldWork() {
        // given
        Map<String, Object> params = new HashMap<>();
        params.put("destination", "발리");
        params.put("start_date", "2025-11-01");
        params.put("end_date", "2025-11-07");
        params.put("relaxation_type", "spa");
        params.put("budget", "300만원");
        params.put("accommodation_style", "resort");
        
        // when
        String prompt = registry.buildPrompt("relaxation", params);
        
        // then
        assertThat(prompt).contains("휴양과 힐링");
        assertThat(prompt).contains("발리");
        assertThat(prompt).contains("스파");
    }
}