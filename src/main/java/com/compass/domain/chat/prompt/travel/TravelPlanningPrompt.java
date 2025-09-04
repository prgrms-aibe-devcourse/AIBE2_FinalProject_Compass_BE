package com.compass.domain.chat.prompt.travel;

import com.compass.domain.chat.helper.TripDurationHelper;
import com.compass.domain.chat.prompt.AbstractPromptTemplate;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * Dynamic prompt template for travel planning with duration-specific adjustments
 */
public class TravelPlanningPrompt extends AbstractPromptTemplate {
    
    private static final String BASE_TEMPLATE = """
        You are a professional travel planner assistant helping to create personalized itineraries.
        
        Trip Information:
        - Destination: {{destination}}
        - Duration: {{duration}}
        - Travel Dates: {{travelDates}}
        - Number of Travelers: {{numberOfTravelers}}
        - Trip Purpose: {{tripPurpose}}
        - User Preferences: {{userPreferences}}
        - Travel Style: {{travelStyle}}
        - Budget Range: {{budgetRange}}
        {{specialRequirements}}
        
        {{durationGuidelines}}
        
        Please create a comprehensive travel plan that includes:
        {{planInclusions}}
        
        The plan should be tailored to the specified:
        - Travel style and preferences
        - Budget constraints
        - Trip purpose
        - Number and type of travelers
        
        Please ensure the itinerary is:
        - Realistic and achievable within the time frame
        - Well-balanced between activities and rest
        - Suitable for the specified travel dates and season
        - Inclusive of the user's stated preferences
        - Mindful of any special requirements mentioned
        {{durationSpecificRequirements}}
        
        Format the response in a clear, organized manner with sections for each day and category of information.
        """;
    
    public TravelPlanningPrompt() {
        super(
            "travel_planning",
            "Dynamic travel planning template with duration-specific customization",
            BASE_TEMPLATE,
            new String[] {
                "destination",
                "duration",
                "travelDates",
                "numberOfTravelers",
                "tripPurpose",
                "userPreferences",
                "travelStyle",
                "budgetRange"
            },
            new String[] {
                "specialRequirements",
                "durationGuidelines",
                "planInclusions",
                "durationSpecificRequirements"
            }
        );
    }
    
    /**
     * Generate a dynamic template based on trip duration
     * @param startDate Start date of the trip
     * @param endDate End date of the trip
     * @return Customized prompt template
     */
    public String generateDynamicTemplate(LocalDate startDate, LocalDate endDate) {
        String tripType = TripDurationHelper.determineTripType(startDate, endDate);
        Map<String, String> guidelines = TripDurationHelper.getDurationGuidelines(tripType);
        
        String template = BASE_TEMPLATE;
        
        // Add duration-specific guidelines
        String durationGuidelines = buildDurationGuidelines(tripType, guidelines);
        template = template.replace("{{durationGuidelines}}", durationGuidelines);
        
        // Customize plan inclusions based on trip type
        String planInclusions = buildPlanInclusions(tripType);
        template = template.replace("{{planInclusions}}", planInclusions);
        
        // Add duration-specific requirements
        String specificRequirements = buildDurationSpecificRequirements(tripType);
        template = template.replace("{{durationSpecificRequirements}}", specificRequirements);
        
        return template;
    }
    
    private String buildDurationGuidelines(String tripType, Map<String, String> guidelines) {
        StringBuilder sb = new StringBuilder();
        sb.append("Duration-Specific Guidelines (").append(tripType).append("):\n");
        sb.append("- Focus: ").append(guidelines.get("focus")).append("\n");
        sb.append("- Time Frame: ").append(guidelines.get("timeFrame")).append("\n");
        sb.append("- Pacing: ").append(guidelines.get("pacing")).append("\n");
        sb.append("- Activities: ").append(guidelines.get("activities"));
        return sb.toString();
    }
    
    private String buildPlanInclusions(String tripType) {
        StringBuilder sb = new StringBuilder();
        
        if (tripType.equals("당일치기")) {
            sb.append("1. Compact day itinerary with specific times (morning to evening)\n");
            sb.append("2. Round-trip transportation options from origin city\n");
            sb.append("3. 3-4 main attractions within close proximity\n");
            sb.append("4. Lunch and dinner restaurant recommendations\n");
            sb.append("5. Efficient route planning to maximize time\n");
            sb.append("6. Quick snack and cafe stops\n");
            sb.append("7. Weather check and what to bring for the day\n");
            sb.append("8. Emergency contacts and day trip essentials");
        } else if (tripType.equals("1박2일")) {
            sb.append("1. Two-day itinerary with overnight stay\n");
            sb.append("2. One accommodation recommendation near attractions\n");
            sb.append("3. Transportation to/from destination and local options\n");
            sb.append("4. 5-6 main attractions including evening activities\n");
            sb.append("5. Restaurant recommendations (Day 1: lunch, dinner; Day 2: breakfast, lunch)\n");
            sb.append("6. Must-see highlights and quick photo spots\n");
            sb.append("7. Weekend getaway packing essentials\n");
            sb.append("8. Budget breakdown for short trip");
        } else if (tripType.equals("2박3일") || tripType.equals("3박4일")) {
            sb.append("1. Day-by-day itinerary with specific times and activities\n");
            sb.append("2. Accommodation suggestions appropriate to the travel style and budget\n");
            sb.append("3. Transportation options (both to/from destination and local transport)\n");
            sb.append("4. Recommended restaurants and dining experiences\n");
            sb.append("5. Must-see attractions and hidden gems\n");
            sb.append("6. Cultural tips and etiquette\n");
            sb.append("7. Budget breakdown and cost estimates\n");
            sb.append("8. Weather considerations and what to pack\n");
            sb.append("9. Emergency contacts and safety information");
        } else {
            // Long-term travel
            sb.append("1. Week-by-week overview with daily highlights\n");
            sb.append("2. Multiple accommodation options with location strategy\n");
            sb.append("3. Inter-city transportation and local transit passes\n");
            sb.append("4. Diverse dining experiences from street food to fine dining\n");
            sb.append("5. Comprehensive attraction list with priority levels\n");
            sb.append("6. Deep cultural immersion activities\n");
            sb.append("7. Detailed budget planning with contingency\n");
            sb.append("8. Seasonal considerations and extended stay logistics\n");
            sb.append("9. Health, safety, and administrative requirements\n");
            sb.append("10. Laundry, rest days, and sustainable travel tips");
        }
        
        return sb.toString();
    }
    
    private String buildDurationSpecificRequirements(String tripType) {
        StringBuilder sb = new StringBuilder();
        
        if (tripType.equals("당일치기")) {
            sb.append("\n- Optimized for single-day exploration without fatigue");
            sb.append("\n- Focused on accessible attractions with minimal travel time");
            sb.append("\n- Includes return transportation timing");
        } else if (tripType.equals("1박2일")) {
            sb.append("\n- Balanced between exploration and rest");
            sb.append("\n- Strategic accommodation location for easy access");
            sb.append("\n- Mix of popular and unique experiences");
        } else if (tripType.contains("박") && tripType.contains("일")) {
            long nights = extractNights(tripType);
            if (nights <= 7) {
                sb.append("\n- Comprehensive coverage without overwhelming schedule");
                sb.append("\n- Mix of structured activities and free exploration time");
                sb.append("\n- Progressive intensity (lighter first/last days)");
            } else {
                sb.append("\n- Sustainable pace for extended travel");
                sb.append("\n- Regular rest days and flexible scheduling");
                sb.append("\n- Consideration for travel fatigue and logistics");
            }
        }
        
        return sb.toString();
    }
    
    private long extractNights(String tripType) {
        if (tripType.equals("당일치기")) return 0;
        
        String[] parts = tripType.split("박");
        if (parts.length > 0) {
            try {
                return Long.parseLong(parts[0]);
            } catch (NumberFormatException e) {
                return 3; // Default
            }
        }
        return 3;
    }
}