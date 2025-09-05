package com.compass.domain.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for TravelTemplateService
 * Validates REQ-AI-003: Basic travel templates functionality
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Travel Template Service Tests")
class TravelTemplateServiceTest {
    
    @Autowired
    private TravelTemplateService templateService;
    
    @BeforeEach
    void setUp() {
        // Templates are loaded via @PostConstruct
    }
    
    @Test
    @DisplayName("Should load all templates on initialization")
    void shouldLoadAllTemplates() {
        List<Map<String, Object>> templates = templateService.getAllTemplates();
        
        assertThat(templates).isNotNull();
        assertThat(templates).hasSize(4);
    }
    
    @Test
    @DisplayName("Should get template by ID")
    void shouldGetTemplateById() {
        Optional<Map<String, Object>> template = templateService.getTemplate("day_trip");
        
        assertTrue(template.isPresent());
        Map<String, Object> dayTrip = template.get();
        
        assertEquals("day_trip", dayTrip.get("templateId"));
        assertEquals("당일치기", dayTrip.get("templateName"));
        assertEquals("0박1일", dayTrip.get("duration"));
        assertNotNull(dayTrip.get("defaultSchedule"));
    }
    
    @Test
    @DisplayName("Should return empty for non-existent template")
    void shouldReturnEmptyForNonExistentTemplate() {
        Optional<Map<String, Object>> template = templateService.getTemplate("invalid_id");
        
        assertFalse(template.isPresent());
    }
    
    @Test
    @DisplayName("Should get template summaries")
    void shouldGetTemplateSummaries() {
        List<Map<String, String>> summaries = templateService.getTemplateSummaries();
        
        assertThat(summaries).hasSize(4);
        
        Map<String, String> firstSummary = summaries.get(0);
        assertThat(firstSummary).containsKeys("templateId", "templateName", "duration", "description");
    }
    
    @Test
    @DisplayName("Should recommend correct template based on nights")
    void shouldRecommendCorrectTemplate() {
        // Test day trip
        Optional<Map<String, Object>> dayTrip = templateService.recommendTemplate(0);
        assertTrue(dayTrip.isPresent());
        assertEquals("day_trip", dayTrip.get().get("templateId"));
        
        // Test 1 night
        Optional<Map<String, Object>> oneNight = templateService.recommendTemplate(1);
        assertTrue(oneNight.isPresent());
        assertEquals("one_night", oneNight.get().get("templateId"));
        
        // Test 2 nights
        Optional<Map<String, Object>> twoNights = templateService.recommendTemplate(2);
        assertTrue(twoNights.isPresent());
        assertEquals("two_nights", twoNights.get().get("templateId"));
        
        // Test 3 nights
        Optional<Map<String, Object>> threeNights = templateService.recommendTemplate(3);
        assertTrue(threeNights.isPresent());
        assertEquals("three_nights", threeNights.get().get("templateId"));
        
        // Test more than 3 nights (should return 3 nights template)
        Optional<Map<String, Object>> moreThanThree = templateService.recommendTemplate(5);
        assertTrue(moreThanThree.isPresent());
        assertEquals("three_nights", moreThanThree.get().get("templateId"));
    }
    
    @Test
    @DisplayName("Should get template variables")
    void shouldGetTemplateVariables() {
        List<String> variables = templateService.getTemplateVariables("day_trip");
        
        assertThat(variables).isNotEmpty();
        assertThat(variables).contains(
            "origin", 
            "destination", 
            "startDate",
            "attraction1",
            "attraction2",
            "lunch",
            "dinner"
        );
    }
    
    @Test
    @DisplayName("Should fill template with values")
    void shouldFillTemplateWithValues() {
        Map<String, String> values = Map.of(
            "origin", "서울",
            "destination", "부산",
            "startDate", "2024-12-25",
            "attraction1", "해운대",
            "lunch", "돼지국밥",
            "dinner", "회"
        );
        
        Map<String, Object> filled = templateService.fillTemplate("day_trip", values);
        
        assertNotNull(filled);
        
        // Check if values were replaced
        String json = filled.toString();
        assertThat(json).contains("서울");
        assertThat(json).contains("부산");
        assertThat(json).contains("2024-12-25");
        assertThat(json).contains("해운대");
        assertThat(json).doesNotContain("{{origin}}");
        assertThat(json).doesNotContain("{{destination}}");
    }
    
    @Test
    @DisplayName("Should throw exception for invalid template ID when filling")
    void shouldThrowExceptionForInvalidTemplateId() {
        Map<String, String> values = Map.of("origin", "서울");
        
        assertThrows(IllegalArgumentException.class, () -> {
            templateService.fillTemplate("invalid_id", values);
        });
    }
}