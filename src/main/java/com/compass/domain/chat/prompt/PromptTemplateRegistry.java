package com.compass.domain.chat.prompt;

import com.compass.domain.chat.prompt.travel.*;
import com.compass.domain.chat.prompt.templates.*;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Registry for managing and accessing prompt templates
 */
@Component
public class PromptTemplateRegistry {
    
    private final Map<String, PromptTemplate> templates = new HashMap<>();
    
    @PostConstruct
    public void initialize() {
        // Register travel-related prompt templates
        register(new TravelPlanningPrompt());
        register(new TravelRecommendationPrompt());
        register(new DestinationDiscoveryPrompt());
        register(new LocalExperiencePrompt());
        register(new BudgetOptimizationPrompt());
        register(new DailyItineraryPrompt()); // TripDetail 엔티티용 일별 상세 템플릿
        
        // Register basic itinerary templates (REQ-AI-003)
        register(new DayTripTemplate());           // 당일치기
        register(new OneNightTwoDaysTemplate());   // 1박 2일
        register(new TwoNightsThreeDaysTemplate()); // 2박 3일
        register(new ThreeNightsFourDaysTemplate()); // 3박 4일
        
        // Register scenario-specific templates (REQ-PROMPT-003)
        register(new FamilyTripTemplate());        // 가족 여행
        register(new CoupleTripTemplate());        // 커플 여행
        register(new BusinessTripTemplate());      // 비즈니스 출장
        register(new BackpackingTemplate());       // 배낭 여행
        register(new LuxuryTravelTemplate());      // 럭셔리 여행
        register(new AdventureTravelTemplate());   // 모험 여행
        register(new CulturalTourTemplate());      // 문화 탐방
        register(new FoodTourTemplate());          // 미식 여행
        register(new RelaxationTemplate());        // 휴양 여행
        register(new SoloTravelTemplate());        // 솔로 여행
    }
    
    /**
     * Register a prompt template
     */
    public void register(PromptTemplate template) {
        templates.put(template.getName(), template);
    }
    
    /**
     * Get a prompt template by name
     */
    public Optional<PromptTemplate> getTemplate(String name) {
        return Optional.ofNullable(templates.get(name));
    }
    
    /**
     * Get all registered template names
     */
    public Set<String> getTemplateNames() {
        return templates.keySet();
    }
    
    /**
     * Check if a template exists
     */
    public boolean hasTemplate(String name) {
        return templates.containsKey(name);
    }
    
    /**
     * Build a prompt using a template
     * @param templateName The name of the template to use
     * @param parameters The parameters to fill in the template
     * @return The built prompt string
     * @throws IllegalArgumentException if template not found or parameters invalid
     */
    public String buildPrompt(String templateName, Map<String, Object> parameters) {
        PromptTemplate template = templates.get(templateName);
        if (template == null) {
            throw new IllegalArgumentException("Template not found: " + templateName);
        }
        return template.buildPrompt(parameters);
    }
}