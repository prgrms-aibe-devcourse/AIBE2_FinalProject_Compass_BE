package com.compass.domain.chat.service.impl;

import com.compass.domain.chat.dto.PromptRequest;
import com.compass.domain.chat.dto.PromptResponse;
import com.compass.domain.chat.prompt.PromptTemplate;
import com.compass.domain.chat.prompt.PromptTemplateRegistry;
import com.compass.domain.chat.service.PromptTemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Implementation of prompt template service
 */
@Service
public class PromptTemplateServiceImpl implements PromptTemplateService {
    
    private static final Logger logger = LoggerFactory.getLogger(PromptTemplateServiceImpl.class);
    
    private final PromptTemplateRegistry templateRegistry;
    
    // Keywords for template selection
    private static final Map<String, List<String>> TEMPLATE_KEYWORDS = Map.of(
        "travel_planning", List.of("plan", "itinerary", "trip", "vacation", "journey", "travel plan"),
        "travel_recommendation", List.of("recommend", "suggest", "advice", "tips", "what should", "where to"),
        "destination_discovery", List.of("where", "destination", "discover", "explore", "find place", "best place"),
        "local_experience", List.of("local", "authentic", "culture", "tradition", "hidden gem", "off the beaten"),
        "budget_optimization", List.of("budget", "save", "cheap", "affordable", "cost", "expense", "money")
    );
    
    public PromptTemplateServiceImpl(PromptTemplateRegistry templateRegistry) {
        this.templateRegistry = templateRegistry;
    }
    
    @Override
    public String buildPrompt(String templateName, Map<String, Object> parameters) {
        return templateRegistry.buildPrompt(templateName, parameters);
    }
    
    @Override
    public PromptResponse buildEnrichedPrompt(PromptRequest request) {
        try {
            // Add system context to parameters
            Map<String, Object> enrichedParams = new HashMap<>(request.getParameters());
            enrichedParams.put("timestamp", LocalDateTime.now().toString());
            enrichedParams.put("sessionId", request.getSessionId());
            
            // Build the prompt
            String prompt = buildPrompt(request.getTemplateName(), enrichedParams);
            
            // Create response with metadata
            PromptResponse response = new PromptResponse();
            response.setPrompt(prompt);
            response.setTemplateName(request.getTemplateName());
            response.setParameters(enrichedParams);
            response.setTimestamp(LocalDateTime.now());
            response.setSuccess(true);
            
            logger.info("Successfully built enriched prompt for template: {}", request.getTemplateName());
            return response;
            
        } catch (Exception e) {
            logger.error("Error building enriched prompt: ", e);
            
            PromptResponse errorResponse = new PromptResponse();
            errorResponse.setSuccess(false);
            errorResponse.setErrorMessage(e.getMessage());
            errorResponse.setTimestamp(LocalDateTime.now());
            return errorResponse;
        }
    }
    
    @Override
    public Set<String> getAvailableTemplates() {
        return templateRegistry.getTemplateNames();
    }
    
    @Override
    public Map<String, Object> getTemplateDetails(String templateName) {
        Optional<PromptTemplate> template = templateRegistry.getTemplate(templateName);
        
        if (template.isEmpty()) {
            throw new IllegalArgumentException("Template not found: " + templateName);
        }
        
        PromptTemplate t = template.get();
        Map<String, Object> details = new HashMap<>();
        details.put("name", t.getName());
        details.put("description", t.getDescription());
        details.put("requiredParameters", t.getRequiredParameters());
        details.put("optionalParameters", t.getOptionalParameters());
        details.put("template", t.getTemplate());
        
        return details;
    }
    
    @Override
    public String selectTemplate(String userQuery, Map<String, Object> context) {
        if (userQuery == null || userQuery.trim().isEmpty()) {
            return "travel_recommendation"; // Default template
        }
        
        String queryLower = userQuery.toLowerCase();
        Map<String, Integer> scores = new HashMap<>();
        
        // Score each template based on keyword matches
        for (Map.Entry<String, List<String>> entry : TEMPLATE_KEYWORDS.entrySet()) {
            String templateName = entry.getKey();
            List<String> keywords = entry.getValue();
            
            int score = 0;
            for (String keyword : keywords) {
                if (queryLower.contains(keyword)) {
                    score += keyword.split(" ").length; // Multi-word keywords get higher scores
                }
            }
            
            scores.put(templateName, score);
        }
        
        // Check context for additional hints
        if (context != null) {
            if (context.containsKey("needsItinerary") && Boolean.TRUE.equals(context.get("needsItinerary"))) {
                scores.merge("travel_planning", 5, Integer::sum);
            }
            if (context.containsKey("budgetConcern") && Boolean.TRUE.equals(context.get("budgetConcern"))) {
                scores.merge("budget_optimization", 5, Integer::sum);
            }
            if (context.containsKey("exploringOptions") && Boolean.TRUE.equals(context.get("exploringOptions"))) {
                scores.merge("destination_discovery", 5, Integer::sum);
            }
        }
        
        // Find the template with the highest score
        String selectedTemplate = scores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .filter(e -> e.getValue() > 0)
            .map(Map.Entry::getKey)
            .orElse("travel_recommendation");
        
        logger.debug("Selected template '{}' for query: {}", selectedTemplate, userQuery);
        return selectedTemplate;
    }
    
    @Override
    public Map<String, Object> extractParameters(String templateName, String userInput, Map<String, Object> context) {
        Map<String, Object> parameters = new HashMap<>();
        
        // Add context parameters if available
        if (context != null) {
            parameters.putAll(context);
        }
        
        // Extract common travel parameters from user input
        parameters.put("userQuery", userInput);
        
        // Extract destination if mentioned
        String destination = extractDestination(userInput);
        if (destination != null) {
            parameters.put("destination", destination);
        }
        
        // Extract duration if mentioned
        String duration = extractDuration(userInput);
        if (duration != null) {
            parameters.put("duration", duration);
            // Duration을 기반으로 startDate와 endDate 계산
            calculateDatesFromDuration(duration, parameters);
        }
        
        // Extract specific dates if mentioned
        extractDates(userInput, parameters);
        
        // Extract budget if mentioned
        String budget = extractBudget(userInput);
        if (budget != null) {
            parameters.put("budgetRange", budget);
            // Budget string을 integer로 변환
            Integer budgetAmount = parseBudgetAmount(budget);
            if (budgetAmount != null) {
                parameters.put("totalBudget", budgetAmount);
            }
        }
        
        // Extract number of travelers
        String travelers = extractTravelers(userInput);
        if (travelers != null) {
            parameters.put("numberOfPeople", Integer.parseInt(travelers));
        }
        
        // Add default values for required parameters if not present
        addDefaultParameters(templateName, parameters);
        
        return parameters;
    }
    
    private String extractDestination(String input) {
        // Simple extraction - in production, use NLP or more sophisticated parsing
        String[] destinationKeywords = {"visit", "going to", "traveling to", "to", "in", "at"};
        
        for (String keyword : destinationKeywords) {
            String pattern = keyword + "\\s+([A-Z][a-zA-Z]+(?:\\s+[A-Z][a-zA-Z]+)*)";
            var matcher = Pattern.compile(pattern).matcher(input);
            if (matcher.find()) {
                String destination = matcher.group(1);
                // Stop at common prepositions or time indicators
                String[] stopWords = {"for", "with", "during", "in", "at", "on"};
                for (String stopWord : stopWords) {
                    int stopIndex = destination.toLowerCase().indexOf(" " + stopWord + " ");
                    if (stopIndex > 0) {
                        destination = destination.substring(0, stopIndex);
                    }
                }
                return destination.trim();
            }
        }
        
        // Fallback: look for capitalized words (proper nouns)
        Pattern properNounPattern = Pattern.compile("\\b([A-Z][a-zA-Z]+(?:\\s+[A-Z][a-zA-Z]+)*)\\b");
        var matcher = properNounPattern.matcher(input);
        if (matcher.find()) {
            String destination = matcher.group(1);
            // Validate it's likely a place name
            if (!destination.equals("I") && destination.length() > 2) {
                return destination;
            }
        }
        
        return null;
    }
    
    private String extractDuration(String input) {
        Pattern daysPattern = Pattern.compile("(\\d+)\\s*days?");
        Pattern weeksPattern = Pattern.compile("(\\d+)\\s*weeks?");
        Pattern monthsPattern = Pattern.compile("(\\d+)\\s*months?");
        
        var daysMatcher = daysPattern.matcher(input.toLowerCase());
        if (daysMatcher.find()) {
            return daysMatcher.group(1) + " days";
        }
        
        var weeksMatcher = weeksPattern.matcher(input.toLowerCase());
        if (weeksMatcher.find()) {
            return weeksMatcher.group(1) + " weeks";
        }
        
        var monthsMatcher = monthsPattern.matcher(input.toLowerCase());
        if (monthsMatcher.find()) {
            return monthsMatcher.group(1) + " months";
        }
        
        return null;
    }
    
    private String extractBudget(String input) {
        // Look for budget patterns with dollar sign or currency keywords
        Pattern[] budgetPatterns = {
            Pattern.compile("\\$([0-9,]+(?:\\.[0-9]{2})?)"),
            Pattern.compile("([0-9,]+(?:\\.[0-9]{2})?)\\s*(?:dollars?|usd|USD)"),
            Pattern.compile("budget\\s+(?:of\\s+)?\\$?([0-9,]+(?:\\.[0-9]{2})?)")
        };
        
        for (Pattern pattern : budgetPatterns) {
            var matcher = pattern.matcher(input);
            if (matcher.find()) {
                String amount = matcher.group(1).replaceAll(",", "");
                return "$" + amount;
            }
        }
        
        return null;
    }
    
    private String extractTravelers(String input) {
        Pattern travelersPattern = Pattern.compile("(\\d+)\\s*(people|persons?|travelers?|adults?|pax)");
        var matcher = travelersPattern.matcher(input.toLowerCase());
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        if (input.toLowerCase().contains("solo") || input.toLowerCase().contains("alone")) {
            return "1";
        }
        if (input.toLowerCase().contains("couple") || input.toLowerCase().contains("two of us")) {
            return "2";
        }
        if (input.toLowerCase().contains("family")) {
            return "4"; // Default family size
        }
        
        return null;
    }
    
    private void addDefaultParameters(String templateName, Map<String, Object> parameters) {
        // Add sensible defaults for missing required parameters
        switch (templateName) {
            case "travel_planning":
                parameters.putIfAbsent("destination", "Not specified");
                parameters.putIfAbsent("startDate", LocalDateTime.now().plusDays(30).toLocalDate().toString());
                parameters.putIfAbsent("endDate", LocalDateTime.now().plusDays(37).toLocalDate().toString());
                parameters.putIfAbsent("numberOfPeople", 2);
                parameters.putIfAbsent("totalBudget", 2000000); // 200만원 기본값
                parameters.putIfAbsent("tripPurpose", "Leisure");
                parameters.putIfAbsent("userPreferences", "General interests");
                parameters.putIfAbsent("travelStyle", "Balanced");
                break;
                
            case "daily_itinerary":
                parameters.putIfAbsent("destination", "Not specified");
                parameters.putIfAbsent("dayNumber", 1);
                parameters.putIfAbsent("activityDate", LocalDateTime.now().plusDays(30).toLocalDate().toString());
                parameters.putIfAbsent("numberOfPeople", 2);
                parameters.putIfAbsent("dailyBudget", 300000); // 30만원 일일 예산
                parameters.putIfAbsent("preferences", "General activities");
                break;
                
            case "travel_recommendation":
                parameters.putIfAbsent("userLocation", "Not specified");
                parameters.putIfAbsent("interests", "General travel");
                parameters.putIfAbsent("budgetLevel", "Moderate");
                break;
                
            case "destination_discovery":
                parameters.putIfAbsent("travelStyle", "Explorer");
                parameters.putIfAbsent("interests", "Culture, nature, food");
                parameters.putIfAbsent("budgetRange", "Flexible");
                parameters.putIfAbsent("travelSeason", "Any time");
                parameters.putIfAbsent("duration", "1-2 weeks");
                parameters.putIfAbsent("departureLocation", "To be determined");
                break;
                
            case "local_experience":
                parameters.putIfAbsent("destination", "Not specified");
                parameters.putIfAbsent("duration", "1 week");
                parameters.putIfAbsent("travelerProfile", "Cultural enthusiast");
                parameters.putIfAbsent("culturalInterests", "Local traditions and customs");
                parameters.putIfAbsent("foodPreferences", "Open to trying local cuisine");
                parameters.putIfAbsent("activityPreferences", "Mix of active and relaxed");
                break;
                
            case "budget_optimization":
                parameters.putIfAbsent("destination", "Not specified");
                parameters.putIfAbsent("duration", "1 week");
                parameters.putIfAbsent("totalBudget", "$2000");
                parameters.putIfAbsent("numberOfTravelers", "2");
                parameters.putIfAbsent("travelDates", "Flexible");
                parameters.putIfAbsent("priorityAreas", "Accommodation and experiences");
                parameters.putIfAbsent("mustHaveExperiences", "Main attractions");
                parameters.putIfAbsent("comfortLevel", "Mid-range");
                break;
        }
    }
    
    /**
     * Duration 문자열로부터 시작일과 종료일 계산
     */
    private void calculateDatesFromDuration(String duration, Map<String, Object> parameters) {
        LocalDate startDate = LocalDate.now().plusDays(30); // 기본 30일 후 출발
        
        // Duration에서 일 수 추출
        Pattern pattern = Pattern.compile("(\\d+)\\s*(?:days?|일)");
        Matcher matcher = pattern.matcher(duration.toLowerCase());
        
        if (matcher.find()) {
            int days = Integer.parseInt(matcher.group(1));
            LocalDate endDate = startDate.plusDays(days - 1);
            
            parameters.putIfAbsent("startDate", startDate.toString());
            parameters.putIfAbsent("endDate", endDate.toString());
        }
    }
    
    /**
     * 텍스트에서 날짜 추출
     */
    private void extractDates(String input, Map<String, Object> parameters) {
        // MM월 DD일 형식
        Pattern koreanDatePattern = Pattern.compile("(\\d{1,2})월\\s*(\\d{1,2})일");
        Matcher matcher = koreanDatePattern.matcher(input);
        
        List<LocalDate> dates = new ArrayList<>();
        int currentYear = LocalDate.now().getYear();
        
        while (matcher.find()) {
            int month = Integer.parseInt(matcher.group(1));
            int day = Integer.parseInt(matcher.group(2));
            
            LocalDate date = LocalDate.of(currentYear, month, day);
            // 과거 날짜면 내년으로 설정
            if (date.isBefore(LocalDate.now())) {
                date = date.plusYears(1);
            }
            dates.add(date);
        }
        
        if (dates.size() >= 2) {
            parameters.put("startDate", dates.get(0).toString());
            parameters.put("endDate", dates.get(1).toString());
        } else if (dates.size() == 1) {
            parameters.put("startDate", dates.get(0).toString());
            // 종료일이 없으면 7일 후로 설정
            parameters.putIfAbsent("endDate", dates.get(0).plusDays(6).toString());
        }
    }
    
    /**
     * 예산 문자열을 정수로 변환
     */
    private Integer parseBudgetAmount(String budgetStr) {
        try {
            // $, 원, 만원 등 제거하고 숫자만 추출
            String numbers = budgetStr.replaceAll("[^0-9]", "");
            
            if (numbers.isEmpty()) {
                return null;
            }
            
            int amount = Integer.parseInt(numbers);
            
            // "만원" 단위 처리
            if (budgetStr.contains("만원") || budgetStr.contains("만")) {
                amount *= 10000;
            }
            // 1000 미만이면 만원 단위로 가정
            else if (amount < 1000) {
                amount *= 10000;
            }
            
            return amount;
            
        } catch (NumberFormatException e) {
            logger.debug("Failed to parse budget amount: {}", budgetStr);
            return null;
        }
    }
}