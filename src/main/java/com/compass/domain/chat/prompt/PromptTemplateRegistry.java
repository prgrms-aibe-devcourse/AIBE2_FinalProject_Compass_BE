package com.compass.domain.chat.prompt;

import com.compass.domain.chat.prompt.travel.*;
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