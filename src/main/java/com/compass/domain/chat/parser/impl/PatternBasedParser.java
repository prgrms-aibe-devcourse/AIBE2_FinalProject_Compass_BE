package com.compass.domain.chat.parser.impl;

import com.compass.domain.chat.dto.TripPlanningRequest;
import com.compass.domain.chat.parser.core.TripPlanningParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pattern-based implementation of TripPlanningParser.
 * Uses regex patterns to extract trip information from natural language.
 * This implementation has NO external dependencies and can run without AI services.
 * 
 * Perfect for:
 * - Unit testing without external dependencies
 * - Offline operation
 * - Fast processing of common patterns
 * - Fallback when AI services are unavailable
 */
@Slf4j
@Component("patternBasedParser")
public class PatternBasedParser implements TripPlanningParser {
    
    // Date patterns
    private static final Pattern DATE_PATTERN = Pattern.compile(
        "(\\d{4}[-/년]\\s?\\d{1,2}[-/월]\\s?\\d{1,2}[일]?)|" +
        "(\\d{4}\\.\\d{1,2}\\.\\d{1,2})|" +
        "(\\d{1,2}월\\s?\\d{1,2}일)|" +
        "(다음주|이번주|다음달|이번달|내일|모레)|" +
        "(\\d{1,2}박\\s?\\d{1,2}일)"
    );
    
    // Budget patterns
    private static final Pattern BUDGET_PATTERN = Pattern.compile(
        "([0-9,]+)\\s?(원|만원|천원|달러|USD|KRW)|" +
        "(예산|비용)\\s?([0-9,]+\\s?(원|만원|천원|달러|USD)?)|" +
        "(저렴|보통|고급|럭셔리)"
    );
    
    // People count patterns
    private static final Pattern PEOPLE_PATTERN = Pattern.compile(
        "([0-9]+)\\s?(명|인)|" +
        "(혼자|둘이|가족|친구|커플|단체)"
    );
    
    // Location patterns
    private static final Pattern LOCATION_PATTERN = Pattern.compile(
        "(서울|부산|제주도|제주|경주|강릉|전주|여수|춘천|속초|대구|대전|광주|인천)|" +
        "([가-힣]+시)|([가-힣]+도)"
    );
    
    @Override
    public TripPlanningRequest parse(String userInput) {
        log.info("Pattern-based parsing for input: {}", userInput);
        
        TripPlanningRequest request = new TripPlanningRequest();
        
        // Extract entities using patterns
        extractDestination(userInput, request);
        extractDates(userInput, request);
        extractBudget(userInput, request);
        extractTravelerCount(userInput, request);
        extractTravelStyle(userInput, request);
        extractInterests(userInput, request);
        
        // Set defaults for missing values
        setDefaults(request);
        
        log.info("Pattern-based parsing complete: {}", request);
        return request;
    }
    
    @Override
    public String getStrategyName() {
        return "pattern";
    }
    
    /**
     * Extract destination from user input
     */
    private void extractDestination(String input, TripPlanningRequest request) {
        Matcher matcher = LOCATION_PATTERN.matcher(input);
        if (matcher.find()) {
            String destination = matcher.group(0);
            request.setDestination(destination);
            log.debug("Extracted destination: {}", destination);
        }
    }
    
    /**
     * Extract and parse dates from user input
     */
    private void extractDates(String input, TripPlanningRequest request) {
        Matcher matcher = DATE_PATTERN.matcher(input);
        List<String> dateStrings = new ArrayList<>();
        Integer durationNights = null;
        
        while (matcher.find()) {
            String matched = matcher.group(0);
            // Check if this is a duration pattern (e.g., "3박4일")
            if (matched.contains("박") && matched.contains("일")) {
                Pattern durationPattern = Pattern.compile("(\\d+)박\\s?(\\d+)일");
                Matcher durationMatcher = durationPattern.matcher(matched);
                if (durationMatcher.find()) {
                    durationNights = Integer.parseInt(durationMatcher.group(1));
                }
            } else {
                dateStrings.add(matched);
            }
        }
        
        for (String dateStr : dateStrings) {
            LocalDate parsedDate = parseDateString(dateStr);
            if (parsedDate != null) {
                if (request.getStartDate() == null) {
                    request.setStartDate(parsedDate);
                } else if (request.getEndDate() == null) {
                    request.setEndDate(parsedDate);
                }
            }
        }
        
        // Apply duration if we found one and have a start date but no end date
        if (durationNights != null && request.getEndDate() == null) {
            // Store the duration for later application if start date is not set yet
            request.setPreferences(request.getPreferences() != null ? 
                request.getPreferences() : new HashMap<>());
            request.getPreferences().put("_duration_nights", String.valueOf(durationNights));
        }
    }
    
    /**
     * Parse date string to LocalDate
     */
    private LocalDate parseDateString(String dateStr) {
        LocalDate today = LocalDate.now();
        
        // Handle relative dates
        switch (dateStr) {
            case "오늘":
                return today;
            case "내일":
                return today.plusDays(1);
            case "모레":
                return today.plusDays(2);
            case "이번주":
                return today.plusDays((7 - today.getDayOfWeek().getValue()) % 7);
            case "다음주":
                return today.plusWeeks(1);
            case "이번달":
                return today.withDayOfMonth(today.lengthOfMonth());
            case "다음달":
                return today.plusMonths(1).withDayOfMonth(1);
        }
        
        // Try parsing various date formats
        String[] formats = {
            "yyyy-MM-dd", "yyyy/MM/dd", "yyyy년 M월 d일",
            "yyyy년MM월dd일", "yyyy년M월d일",
            "M월 d일", "M월d일", "yyyy.MM.dd", "yyyy.M.d"
        };
        
        for (String format : formats) {
            try {
                // Add year if not present
                String processedDate = dateStr;
                if (!dateStr.matches(".*\\d{4}.*")) {
                    processedDate = today.getYear() + "년 " + dateStr;
                }
                
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
                return LocalDate.parse(processedDate.replaceAll("\\s+", ""), formatter);
            } catch (DateTimeParseException e) {
                // Try next format
            }
        }
        
        return null;
    }
    
    /**
     * Extract and normalize budget from user input
     */
    private void extractBudget(String input, TripPlanningRequest request) {
        Matcher matcher = BUDGET_PATTERN.matcher(input);
        
        if (matcher.find()) {
            String budgetStr = matcher.group(0);
            Integer budget = parseBudget(budgetStr);
            
            if (budget != null) {
                request.setBudgetPerPerson(budget);
                log.debug("Extracted budget: {} KRW", budget);
            }
            
            // Extract travel style from budget keywords
            if (budgetStr.contains("저렴") || budgetStr.contains("싸게")) {
                request.setTravelStyle("budget");
            } else if (budgetStr.contains("고급") || budgetStr.contains("럭셔리")) {
                request.setTravelStyle("luxury");
            } else {
                request.setTravelStyle("moderate");
            }
        }
    }
    
    /**
     * Parse budget string to integer (in KRW)
     */
    private Integer parseBudget(String budgetStr) {
        try {
            // Keep spaces between number and unit but remove commas
            String cleaned = budgetStr.replaceAll(",", "");
            
            // Extract number with unit - look for patterns like "100만원" or "예산 100만원"
            Pattern numPattern = Pattern.compile("(\\d+)\\s*(만원|천원|원|달러|USD)");
            Matcher numMatcher = numPattern.matcher(cleaned);
            
            if (numMatcher.find()) {
                int amount = Integer.parseInt(numMatcher.group(1));
                String unit = numMatcher.group(2);
                
                // Convert to KRW based on unit
                if (unit != null) {
                    if (unit.equals("만원")) {
                        return amount * 10000;
                    } else if (unit.equals("천원")) {
                        return amount * 1000;
                    } else if (unit.equals("달러") || unit.equals("USD")) {
                        return amount * 1300; // Approximate USD to KRW conversion
                    } else if (unit.equals("원")) {
                        return amount;
                    }
                }
            }
        } catch (NumberFormatException e) {
            log.warn("Failed to parse budget: {}", budgetStr);
        }
        
        return null;
    }
    
    /**
     * Extract traveler count from user input
     */
    private void extractTravelerCount(String input, TripPlanningRequest request) {
        Matcher matcher = PEOPLE_PATTERN.matcher(input);
        
        if (matcher.find()) {
            String peopleStr = matcher.group(0);
            Integer count = parsePeopleCount(peopleStr);
            
            if (count != null) {
                request.setNumberOfTravelers(count);
                log.debug("Extracted traveler count: {}", count);
            }
        }
    }
    
    /**
     * Parse people count string to integer
     */
    private Integer parsePeopleCount(String peopleStr) {
        // Try to extract number directly
        Pattern numPattern = Pattern.compile("\\d+");
        Matcher numMatcher = numPattern.matcher(peopleStr);
        
        if (numMatcher.find()) {
            return Integer.parseInt(numMatcher.group());
        }
        
        // Handle descriptive terms
        if (peopleStr.contains("혼자")) {
            return 1;
        } else if (peopleStr.contains("둘이") || peopleStr.contains("커플")) {
            return 2;
        } else if (peopleStr.contains("가족")) {
            return 4; // Default family size
        } else if (peopleStr.contains("단체")) {
            return 10; // Default group size
        }
        
        return null;
    }
    
    /**
     * Extract travel style from user input
     */
    private void extractTravelStyle(String input, TripPlanningRequest request) {
        if (request.getTravelStyle() != null) {
            return; // Already set by budget extraction
        }
        
        if (input.contains("배낭") || input.contains("게스트하우스")) {
            request.setTravelStyle("budget");
        } else if (input.contains("호텔") || input.contains("리조트")) {
            request.setTravelStyle("luxury");
        } else if (input.contains("펜션") || input.contains("에어비엔비")) {
            request.setTravelStyle("moderate");
        }
    }
    
    /**
     * Extract interests from user input
     */
    private void extractInterests(String input, TripPlanningRequest request) {
        List<String> interests = new ArrayList<>();
        
        // Culture
        if (input.matches(".*(문화|박물관|미술관|역사|전통|궁|사찰).*")) {
            interests.add("culture");
        }
        
        // Food
        if (input.matches(".*(맛집|음식|먹거리|카페|레스토랑|요리).*")) {
            interests.add("food");
        }
        
        // Adventure
        if (input.matches(".*(모험|액티비티|스포츠|등산|서핑|다이빙).*")) {
            interests.add("adventure");
        }
        
        // Shopping
        if (input.matches(".*(쇼핑|면세|아울렛|시장|기념품).*")) {
            interests.add("shopping");
        }
        
        // Nature
        if (input.matches(".*(자연|산|바다|해변|공원|트레킹).*")) {
            interests.add("nature");
        }
        
        if (!interests.isEmpty()) {
            request.setInterests(interests.toArray(new String[0]));
            log.debug("Extracted interests: {}", interests);
        }
    }
    
    /**
     * Set default values for missing fields
     */
    private void setDefaults(TripPlanningRequest request) {
        if (request.getOrigin() == null) {
            request.setOrigin("서울");
        }
        
        if (request.getStartDate() == null) {
            request.setStartDate(LocalDate.now().plusDays(7));
        }
        
        // Check if we have a stored duration to apply
        if (request.getEndDate() == null) {
            if (request.getPreferences() != null && 
                request.getPreferences().containsKey("_duration_nights")) {
                try {
                    Object durationObj = request.getPreferences().get("_duration_nights");
                    int nights = Integer.parseInt(String.valueOf(durationObj));
                    request.setEndDate(request.getStartDate().plusDays(nights));
                    // Remove the temporary duration storage
                    request.getPreferences().remove("_duration_nights");
                } catch (NumberFormatException | NullPointerException e) {
                    // Fall back to default
                    request.setEndDate(request.getStartDate().plusDays(2));
                }
            } else {
                request.setEndDate(request.getStartDate().plusDays(2));
            }
        }
        
        if (request.getNumberOfTravelers() == null) {
            request.setNumberOfTravelers(1);
        }
        
        if (request.getTravelStyle() == null) {
            request.setTravelStyle("moderate");
        }
        
        if (request.getCurrency() == null || request.getCurrency().equals("USD")) {
            request.setCurrency("KRW");
        }
        
        if (request.getPreferences() == null) {
            request.setPreferences(new HashMap<>());
        }
    }
}