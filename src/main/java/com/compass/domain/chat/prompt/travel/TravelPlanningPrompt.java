package com.compass.domain.chat.prompt.travel;

import com.compass.domain.chat.prompt.AbstractPromptTemplate;

/**
 * Prompt template for general travel planning
 */
public class TravelPlanningPrompt extends AbstractPromptTemplate {
    
    private static final String TEMPLATE = """
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
        
        Please create a comprehensive travel plan that includes:
        1. Day-by-day itinerary with specific times and activities
        2. Accommodation suggestions appropriate to the travel style and budget
        3. Transportation options (both to/from destination and local transport)
        4. Recommended restaurants and dining experiences
        5. Must-see attractions and hidden gems
        6. Cultural tips and etiquette
        7. Budget breakdown and cost estimates
        8. Weather considerations and what to pack
        9. Emergency contacts and safety information
        
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
        
        Format the response in a clear, organized manner with sections for each day and category of information.
        """;
    
    public TravelPlanningPrompt() {
        super(
            "travel_planning",
            "Comprehensive travel planning template for creating personalized itineraries",
            TEMPLATE,
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
            new String[] {"specialRequirements"}
        );
    }
}