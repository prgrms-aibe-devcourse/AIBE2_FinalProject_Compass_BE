package com.compass.domain.chat.controller;

import com.compass.domain.chat.service.TravelTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for travel template management
 * Implements REQ-AI-003: Basic travel templates API
 */
@Slf4j
@RestController
@RequestMapping("/api/chat/travel-templates")
@RequiredArgsConstructor
public class TravelTemplateController {
    
    private final TravelTemplateService templateService;
    
    /**
     * Get all available template summaries
     * GET /api/chat/travel-templates
     */
    @GetMapping
    public ResponseEntity<List<Map<String, String>>> getTemplateSummaries() {
        log.info("Fetching all template summaries");
        return ResponseEntity.ok(templateService.getTemplateSummaries());
    }
    
    /**
     * Get a specific template by ID
     * GET /api/chat/travel-templates/{templateId}
     */
    @GetMapping("/{templateId}")
    public ResponseEntity<Map<String, Object>> getTemplate(@PathVariable String templateId) {
        log.info("Fetching template: {}", templateId);
        return templateService.getTemplate(templateId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get template variables
     * GET /api/chat/travel-templates/{templateId}/variables
     */
    @GetMapping("/{templateId}/variables")
    public ResponseEntity<List<String>> getTemplateVariables(@PathVariable String templateId) {
        log.info("Fetching variables for template: {}", templateId);
        try {
            List<String> variables = templateService.getTemplateVariables(templateId);
            return ResponseEntity.ok(variables);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Recommend a template based on nights
     * GET /api/chat/travel-templates/recommend?nights=2
     */
    @GetMapping("/recommend")
    public ResponseEntity<Map<String, Object>> recommendTemplate(@RequestParam int nights) {
        log.info("Recommending template for {} nights", nights);
        return templateService.recommendTemplate(nights)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Fill a template with user values
     * POST /api/chat/travel-templates/{templateId}/fill
     */
    @PostMapping("/{templateId}/fill")
    public ResponseEntity<Map<String, Object>> fillTemplate(
            @PathVariable String templateId,
            @RequestBody Map<String, String> values) {
        log.info("Filling template {} with {} values", templateId, values.size());
        try {
            Map<String, Object> filled = templateService.fillTemplate(templateId, values);
            return ResponseEntity.ok(filled);
        } catch (IllegalArgumentException e) {
            log.error("Failed to fill template: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}