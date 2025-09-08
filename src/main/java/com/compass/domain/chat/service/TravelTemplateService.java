package com.compass.domain.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing and loading travel itinerary templates
 * Implements REQ-AI-003: Basic travel templates
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TravelTemplateService {
    
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    
    // Cache for loaded templates
    private final Map<String, Map<String, Object>> templateCache = new HashMap<>();
    
    // Template file mappings
    private static final Map<String, String> TEMPLATE_FILES = Map.of(
        "day_trip", "classpath:templates/travel/day_trip.json",
        "one_night", "classpath:templates/travel/one_night.json",
        "two_nights", "classpath:templates/travel/two_nights.json",
        "three_nights", "classpath:templates/travel/three_nights.json"
    );
    
    @PostConstruct
    public void loadTemplates() {
        log.info("Loading travel templates...");
        
        for (Map.Entry<String, String> entry : TEMPLATE_FILES.entrySet()) {
            try {
                Resource resource = resourceLoader.getResource(entry.getValue());
                Map<String, Object> template = objectMapper.readValue(
                    resource.getInputStream(), 
                    Map.class
                );
                templateCache.put(entry.getKey(), template);
                log.info("Successfully loaded template: {}", entry.getKey());
            } catch (IOException e) {
                log.error("Failed to load template {}: {}", entry.getKey(), e.getMessage());
            }
        }
        
        log.info("Loaded {} templates", templateCache.size());
    }
    
    /**
     * Get a specific template by ID
     */
    public Optional<Map<String, Object>> getTemplate(String templateId) {
        return Optional.ofNullable(templateCache.get(templateId));
    }
    
    /**
     * Get all available templates
     */
    public List<Map<String, Object>> getAllTemplates() {
        return new ArrayList<>(templateCache.values());
    }
    
    /**
     * Get template summary information
     */
    public List<Map<String, String>> getTemplateSummaries() {
        return templateCache.values().stream()
            .map(template -> {
                Map<String, String> summary = new HashMap<>();
                summary.put("templateId", (String) template.get("templateId"));
                summary.put("templateName", (String) template.get("templateName"));
                summary.put("duration", (String) template.get("duration"));
                summary.put("description", (String) template.get("description"));
                return summary;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Recommend a template based on trip duration
     */
    public Optional<Map<String, Object>> recommendTemplate(int nights) {
        String templateId = switch (nights) {
            case 0 -> "day_trip";
            case 1 -> "one_night";
            case 2 -> "two_nights";
            case 3 -> "three_nights";
            default -> nights < 0 ? "day_trip" : "three_nights";
        };
        
        return getTemplate(templateId);
    }
    
    /**
     * Fill template with user-provided values
     */
    public Map<String, Object> fillTemplate(String templateId, Map<String, String> values) {
        Map<String, Object> template = getTemplate(templateId)
            .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));
        
        // Deep copy the template
        Map<String, Object> filled = deepCopy(template);
        
        // Replace placeholders
        replaceVariables(filled, values);
        
        return filled;
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> deepCopy(Map<String, Object> original) {
        Map<String, Object> copy = new HashMap<>();
        for (Map.Entry<String, Object> entry : original.entrySet()) {
            if (entry.getValue() instanceof Map) {
                copy.put(entry.getKey(), deepCopy((Map<String, Object>) entry.getValue()));
            } else if (entry.getValue() instanceof List) {
                copy.put(entry.getKey(), new ArrayList<>((List<?>) entry.getValue()));
            } else {
                copy.put(entry.getKey(), entry.getValue());
            }
        }
        return copy;
    }
    
    @SuppressWarnings("unchecked")
    private void replaceVariables(Object obj, Map<String, String> values) {
        if (obj instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) obj;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (entry.getValue() instanceof String) {
                    String str = (String) entry.getValue();
                    for (Map.Entry<String, String> var : values.entrySet()) {
                        str = str.replace("{{" + var.getKey() + "}}", var.getValue());
                    }
                    map.put(entry.getKey(), str);
                } else {
                    replaceVariables(entry.getValue(), values);
                }
            }
        } else if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            for (Object item : list) {
                replaceVariables(item, values);
            }
        }
    }
    
    /**
     * Get template variables that need to be filled
     */
    @SuppressWarnings("unchecked")
    public List<String> getTemplateVariables(String templateId) {
        Map<String, Object> template = getTemplate(templateId)
            .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));
        
        Object variables = template.get("variables");
        if (variables instanceof List) {
            return (List<String>) variables;
        }
        
        return Collections.emptyList();
    }
}