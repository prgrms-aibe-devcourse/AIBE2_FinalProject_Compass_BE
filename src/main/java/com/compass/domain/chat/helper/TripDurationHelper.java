package com.compass.domain.chat.helper;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for managing trip duration templates and guidelines
 * Provides duration-specific prompt templates for Function Calling integration
 */
@Slf4j
@UtilityClass
public class TripDurationHelper {
    
    /**
     * Determines trip type based on the duration
     * @param startDate Start date of the trip
     * @param endDate End date of the trip
     * @return Trip type string (e.g., "당일치기", "1박2일")
     */
    public static String determineTripType(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return "3박4일"; // Default
        }
        
        long nights = ChronoUnit.DAYS.between(startDate, endDate);
        
        if (nights <= 0) {
            return "당일치기";
        } else if (nights == 1) {
            return "1박2일";
        } else if (nights == 2) {
            return "2박3일";
        } else if (nights == 3) {
            return "3박4일";
        } else if (nights <= 6) {
            return nights + "박" + (nights + 1) + "일";
        } else {
            return "장기여행";
        }
    }
    
    /**
     * Gets duration-specific prompt guidelines
     * @param tripType The type of trip (e.g., "당일치기", "1박2일")
     * @return Map of prompt guidelines for the specific duration
     */
    public static Map<String, String> getDurationGuidelines(String tripType) {
        Map<String, String> guidelines = new HashMap<>();
        
        switch (tripType) {
            case "당일치기":
                guidelines.put("focus", "Compact itinerary with efficient travel routes");
                guidelines.put("accommodation", "Not needed - focus on day activities");
                guidelines.put("meals", "Breakfast (optional), lunch, dinner recommendations");
                guidelines.put("transportation", "Round-trip transportation from origin city");
                guidelines.put("activities", "3-4 main attractions within close proximity");
                guidelines.put("timeFrame", "Early morning departure (7-9 AM), evening return (7-10 PM)");
                guidelines.put("pacing", "Moderate pace with 2-3 hours per major attraction");
                break;
                
            case "1박2일":
                guidelines.put("focus", "Weekend getaway with must-see highlights");
                guidelines.put("accommodation", "1 night stay near main attractions");
                guidelines.put("meals", "Day 1: lunch, dinner; Day 2: breakfast, lunch");
                guidelines.put("transportation", "Consider local transport options");
                guidelines.put("activities", "5-6 main attractions, evening/night activities");
                guidelines.put("timeFrame", "Day 1: arrival by noon; Day 2: departure by evening");
                guidelines.put("pacing", "Balanced with rest time at accommodation");
                break;
                
            case "2박3일":
                guidelines.put("focus", "Comprehensive city exploration with hidden gems");
                guidelines.put("accommodation", "2 nights, consider location changes if exploring different areas");
                guidelines.put("meals", "Full meal recommendations including local specialties");
                guidelines.put("transportation", "Mix of walking, public transport, and occasional taxi");
                guidelines.put("activities", "8-10 attractions including popular and off-beaten paths");
                guidelines.put("timeFrame", "Full day activities with flexible morning starts");
                guidelines.put("pacing", "Relaxed pace with time for spontaneous exploration");
                break;
                
            case "3박4일":
                guidelines.put("focus", "In-depth exploration with day trips to nearby areas");
                guidelines.put("accommodation", "3 nights, strategic location or split stays");
                guidelines.put("meals", "Variety of dining experiences from street food to fine dining");
                guidelines.put("transportation", "Consider day pass or rental options");
                guidelines.put("activities", "12-15 attractions, possible day trip to suburbs");
                guidelines.put("timeFrame", "Flexible schedule with one free morning/afternoon");
                guidelines.put("pacing", "Comfortable pace with rest periods and shopping time");
                break;
                
            case "장기여행":
                guidelines.put("focus", "Immersive experience with multiple cities/regions");
                guidelines.put("accommodation", "Multiple accommodations, consider weekly rates");
                guidelines.put("meals", "Mix of dining out and self-catering options");
                guidelines.put("transportation", "Inter-city transport, consider rail passes");
                guidelines.put("activities", "Comprehensive coverage with flexibility for changes");
                guidelines.put("timeFrame", "Include rest days and laundry time");
                guidelines.put("pacing", "Sustainable pace avoiding burnout");
                break;
                
            default:
                // For custom durations like "5박6일"
                guidelines.put("focus", "Balanced exploration with flexibility");
                guidelines.put("accommodation", "Strategic locations based on itinerary");
                guidelines.put("meals", "Mix of planned and spontaneous dining");
                guidelines.put("transportation", "Optimal mix based on destination");
                guidelines.put("activities", "Proportional to trip duration");
                guidelines.put("timeFrame", "Flexible with buffer time");
                guidelines.put("pacing", "Adjusted to trip length and traveler preferences");
        }
        
        return guidelines;
    }
    
    /**
     * Generates a duration-specific prompt enhancement
     * @param tripType The type of trip
     * @return Enhanced prompt text for the specific duration
     */
    public static String generateDurationPrompt(String tripType) {
        Map<String, String> guidelines = getDurationGuidelines(tripType);
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("\n\n**Trip Duration Specific Guidelines for ").append(tripType).append(":**\n");
        prompt.append("- Focus: ").append(guidelines.get("focus")).append("\n");
        prompt.append("- Accommodation: ").append(guidelines.get("accommodation")).append("\n");
        prompt.append("- Meals: ").append(guidelines.get("meals")).append("\n");
        prompt.append("- Transportation: ").append(guidelines.get("transportation")).append("\n");
        prompt.append("- Activities: ").append(guidelines.get("activities")).append("\n");
        prompt.append("- Time Frame: ").append(guidelines.get("timeFrame")).append("\n");
        prompt.append("- Pacing: ").append(guidelines.get("pacing")).append("\n");
        
        return prompt.toString();
    }
    
    /**
     * Gets time allocation suggestions for different trip durations
     * @param tripType The type of trip
     * @return Map of time allocations for different activity types
     */
    public static Map<String, Integer> getTimeAllocations(String tripType) {
        Map<String, Integer> allocations = new HashMap<>();
        
        switch (tripType) {
            case "당일치기":
                allocations.put("majorAttractions", 180); // 3 hours per major site
                allocations.put("minorAttractions", 60);  // 1 hour for minor stops
                allocations.put("meals", 60);              // 1 hour per meal
                allocations.put("transportation", 30);     // 30 mins between sites
                allocations.put("shopping", 30);           // 30 mins if any
                allocations.put("buffer", 30);             // 30 mins buffer
                break;
                
            case "1박2일":
                allocations.put("majorAttractions", 150); // 2.5 hours
                allocations.put("minorAttractions", 60);
                allocations.put("meals", 90);              // 1.5 hours
                allocations.put("transportation", 30);
                allocations.put("shopping", 60);
                allocations.put("buffer", 60);
                break;
                
            case "2박3일":
                allocations.put("majorAttractions", 120); // 2 hours
                allocations.put("minorAttractions", 60);
                allocations.put("meals", 90);
                allocations.put("transportation", 30);
                allocations.put("shopping", 90);
                allocations.put("buffer", 90);
                break;
                
            case "3박4일":
                allocations.put("majorAttractions", 120);
                allocations.put("minorAttractions", 60);
                allocations.put("meals", 90);
                allocations.put("transportation", 30);
                allocations.put("shopping", 120);          // 2 hours
                allocations.put("buffer", 120);
                break;
                
            default:
                // Default allocations for longer trips
                allocations.put("majorAttractions", 120);
                allocations.put("minorAttractions", 60);
                allocations.put("meals", 90);
                allocations.put("transportation", 45);
                allocations.put("shopping", 90);
                allocations.put("buffer", 90);
        }
        
        return allocations;
    }
    
    /**
     * Suggests the optimal number of attractions based on trip duration
     * @param tripType The type of trip
     * @return Suggested number of attractions
     */
    public static Map<String, Integer> getSuggestedAttractionCount(String tripType) {
        Map<String, Integer> suggestions = new HashMap<>();
        
        switch (tripType) {
            case "당일치기":
                suggestions.put("major", 2);
                suggestions.put("minor", 2);
                suggestions.put("restaurants", 2);
                suggestions.put("cafes", 1);
                break;
                
            case "1박2일":
                suggestions.put("major", 3);
                suggestions.put("minor", 3);
                suggestions.put("restaurants", 4);
                suggestions.put("cafes", 2);
                break;
                
            case "2박3일":
                suggestions.put("major", 5);
                suggestions.put("minor", 5);
                suggestions.put("restaurants", 6);
                suggestions.put("cafes", 3);
                break;
                
            case "3박4일":
                suggestions.put("major", 7);
                suggestions.put("minor", 7);
                suggestions.put("restaurants", 8);
                suggestions.put("cafes", 4);
                break;
                
            default:
                // Calculate based on days
                int days = extractDaysFromTripType(tripType);
                suggestions.put("major", Math.min(days * 2, 14));
                suggestions.put("minor", Math.min(days * 2, 14));
                suggestions.put("restaurants", Math.min(days * 2 + 2, 20));
                suggestions.put("cafes", Math.min(days, 7));
        }
        
        return suggestions;
    }
    
    /**
     * Extracts the number of days from trip type string
     * @param tripType Trip type string like "5박6일"
     * @return Number of days, or 4 as default
     */
    private static int extractDaysFromTripType(String tripType) {
        if (tripType.equals("당일치기")) {
            return 1;
        }
        
        // Extract from format "X박Y일"
        String[] parts = tripType.split("[박일]");
        if (parts.length >= 2) {
            try {
                return Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                log.debug("Could not parse days from trip type: {}", tripType);
            }
        }
        
        return 4; // Default
    }
}